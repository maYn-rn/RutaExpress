package cl.duoc.rutaexpress.notifications.messaging.support;

import cl.duoc.rutaexpress.notifications.messaging.config.MessagingProperties;
import cl.duoc.rutaexpress.notifications.messaging.event.EventEnvelope;
import cl.duoc.rutaexpress.notifications.messaging.idempotency.ProcessedMessage;
import cl.duoc.rutaexpress.notifications.messaging.idempotency.ProcessedMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

/**
 * Confirmacion explicita de mensajes (acknowledge-mode: manual), comun a todos
 * los consumidores. Cada mensaje termina en uno de estos caminos:
 *
 * | Resultado                              | Accion                       | Destino              |
 * |----------------------------------------|------------------------------|----------------------|
 * | Procesado correctamente                | basicAck                     | se elimina de la cola|
 * | Duplicado (eventId ya procesado)       | basicAck sin procesar        | se elimina de la cola|
 * | Mensaje invalido (JSON, datos)         | basicReject(requeue=false)   | DLQ, sin reintentos  |
 * | Error recuperable, quedan intentos     | reenvio con x-retry-count+1  | misma cola (reintento)|
 * |                                        | y basicAck del original      |                      |
 * | Error recuperable, intentos agotados   | basicNack(requeue=false)     | DLQ                  |
 * | Error inesperado                       | basicNack(requeue=false)     | DLQ                  |
 *
 * Nunca se usa requeue=true sobre el mismo mensaje: evitaria la DLQ y podria
 * generar un ciclo infinito de reprocesos. Los reintentos se cuentan en el
 * header x-retry-count.
 *
 * Idempotencia: cada consumidor registra los eventId que proceso con exito
 * (tabla processed_messages). Si RabbitMQ reentrega un mensaje ya procesado,
 * se confirma sin volver a ejecutar la logica (no se envia dos veces el email).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AcknowledgingMessageHandler {

    public static final String RETRY_COUNT_HEADER = "x-retry-count";
    /** Ultimo error recuperable; la DLQ lo usa para explicar por que fallo. */
    public static final String LAST_ERROR_HEADER = "x-last-error";

    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties properties;
    private final ProcessedMessageRepository processedMessages;

    public void handle(Message message, Channel channel, String consumerName, Consumer<EventEnvelope> processor) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            EventEnvelope envelope = parse(message);
            if (processedMessages.existsById(ProcessedMessage.key(consumerName, envelope.eventId()))) {
                channel.basicAck(deliveryTag, false);
                log.info("[{}] Duplicado ignorado, ACK eventId={}", consumerName, envelope.eventId());
                return;
            }

            processor.accept(envelope);
            processedMessages.save(ProcessedMessage.of(consumerName, envelope.eventId()));
            channel.basicAck(deliveryTag, false);
            log.info("[{}] ACK envio id={} ({}) eventId={} traceId={}", consumerName,
                    envelope.data().shipmentId(), envelope.type(), envelope.eventId(), envelope.traceId());

        } catch (InvalidMessageException ex) {
            log.warn("[{}] Mensaje invalido, REJECT -> DLQ: {}", consumerName, ex.getMessage());
            reject(channel, deliveryTag);

        } catch (RecoverableProcessingException ex) {
            int attempt = retryCount(message);
            if (attempt < properties.maxRetries()) {
                retry(message, channel, deliveryTag, consumerName, attempt + 1, ex);
            } else {
                log.error("[{}] Reintentos agotados ({}), NACK -> DLQ: {}",
                        consumerName, properties.maxRetries(), ex.getMessage());
                nack(channel, deliveryTag, false);
            }

        } catch (RuntimeException ex) {
            log.error("[{}] Error inesperado, NACK -> DLQ: {}", consumerName, ex.toString());
            nack(channel, deliveryTag, false);

        } catch (IOException ex) {
            // El canal se cerro antes de confirmar: el broker reentregara el
            // mensaje (no se pierde), por eso solo se registra.
            log.error("[{}] No se pudo enviar el ACK, RabbitMQ reentregara el mensaje: {}",
                    consumerName, ex.getMessage());
        }
    }

    private EventEnvelope parse(Message message) {
        EventEnvelope envelope;
        try {
            envelope = objectMapper.readValue(message.getBody(), EventEnvelope.class);
        } catch (IOException ex) {
            throw new InvalidMessageException("JSON no valido: " + ex.getMessage(), ex);
        }
        if (envelope == null || envelope.type() == null || envelope.eventId() == null
                || envelope.data() == null || envelope.data().shipmentId() == null) {
            throw new InvalidMessageException("Faltan campos obligatorios (type, eventId, data.shipmentId)");
        }
        return envelope;
    }

    /**
     * Reintento: publica una copia en la misma cola con el contador
     * incrementado y confirma el original. Si ni siquiera se puede reenviar,
     * el original se devuelve a la cola para no perderlo.
     */
    private void retry(Message message, Channel channel, long deliveryTag, String consumerName,
                       int nextAttempt, RecoverableProcessingException cause) {
        MessageProperties original = message.getMessageProperties();
        try {
            Message copy = MessageBuilder.withBody(message.getBody())
                    .copyProperties(original)
                    .setHeader(RETRY_COUNT_HEADER, nextAttempt)
                    .setHeader(LAST_ERROR_HEADER, cause.getMessage())
                    .build();
            rabbitTemplate.send("", original.getConsumerQueue(), copy);
            channel.basicAck(deliveryTag, false);
            log.warn("[{}] Error recuperable, reintento {}/{}: {}",
                    consumerName, nextAttempt, properties.maxRetries(), cause.getMessage());
        } catch (AmqpException | IOException ex) {
            log.error("[{}] No se pudo reenviar para reintento, NACK con requeue: {}", consumerName, ex.getMessage());
            nack(channel, deliveryTag, true);
        }
    }

    private int retryCount(Message message) {
        Object value = message.getMessageProperties().getHeaders().get(RETRY_COUNT_HEADER);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private void reject(Channel channel, long deliveryTag) {
        try {
            channel.basicReject(deliveryTag, false);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private void nack(Channel channel, long deliveryTag, boolean requeue) {
        try {
            channel.basicNack(deliveryTag, false, requeue);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

}
