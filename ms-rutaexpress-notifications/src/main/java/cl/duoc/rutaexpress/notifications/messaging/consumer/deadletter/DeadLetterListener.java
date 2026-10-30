package cl.duoc.rutaexpress.notifications.messaging.consumer.deadletter;

import cl.duoc.rutaexpress.notifications.deadletter.DeadLetterRecord;
import cl.duoc.rutaexpress.notifications.deadletter.DeadLetterRepository;
import cl.duoc.rutaexpress.notifications.messaging.config.MessagingProperties;
import cl.duoc.rutaexpress.notifications.messaging.consumer.email.EmailCommandListener;
import cl.duoc.rutaexpress.notifications.messaging.consumer.label.LabelCommandListener;
import cl.duoc.rutaexpress.notifications.messaging.consumer.warehouse.WarehouseCommandListener;
import cl.duoc.rutaexpress.notifications.messaging.event.EventEnvelope;
import cl.duoc.rutaexpress.notifications.messaging.support.AcknowledgingMessageHandler;
import cl.duoc.rutaexpress.notifications.notification.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * CONSUMIDOR de las 3 DLQ. Por cada mensaje no entregado:
 * 1. Lo registra en el log y en la tabla dead_letters (metricas de tasa de DLQ).
 * 2. Si viene de q.cmd.email.dlq, guarda la notificacion como FALLIDA: el
 *    cliente igual ve el cambio de estado en la app aunque el email no salio.
 * 3. Lo confirma con ACK para que no se acumule sin control.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterListener {

    private static final int MAX_BODY_LENGTH = 2000;

    private final MessagingProperties properties;
    private final DeadLetterRepository deadLetters;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = {
            "${app.messaging.email.dead-letter-queue}",
            "${app.messaging.warehouse.dead-letter-queue}",
            "${app.messaging.label.dead-letter-queue}"
    })
    public void onDeadLetter(Message message, Channel channel) throws IOException {
        MessageProperties props = message.getMessageProperties();
        String dlq = props.getConsumerQueue();
        String consumer = consumerFor(dlq);
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        String reason = reason(props);
        int retries = props.getHeaders().get(AcknowledgingMessageHandler.RETRY_COUNT_HEADER) instanceof Number n
                ? n.intValue() : 0;
        EventEnvelope envelope = tryParse(message);

        log.error("[DLQ] Mensaje no entregado | dlq={} | consumidor={} | motivo={} | reintentos={} | eventId={}",
                dlq, consumer, reason, retries, envelope != null ? envelope.eventId() : null);

        try {
            deadLetters.save(DeadLetterRecord.builder()
                    .consumer(consumer)
                    .deadLetterQueue(dlq)
                    .eventId(envelope != null ? envelope.eventId() : props.getMessageId())
                    .reason(truncate(reason, 500))
                    .retries(retries)
                    .body(truncate(body, MAX_BODY_LENGTH))
                    .receivedAt(Instant.now())
                    .build());

            if (EmailCommandListener.CONSUMER.equals(consumer) && envelope != null) {
                notificationService.recordFailure(envelope, reason);
            }
        } catch (RuntimeException ex) {
            // Sin base de datos no hay donde guardarlo; queda el log de arriba.
            log.error("[DLQ] No se pudo registrar el mensaje muerto: {}", ex.getMessage());
        }

        channel.basicAck(props.getDeliveryTag(), false);
    }

    String consumerFor(String deadLetterQueue) {
        if (properties.email().deadLetterQueue().equals(deadLetterQueue)) {
            return EmailCommandListener.CONSUMER;
        }
        if (properties.warehouse().deadLetterQueue().equals(deadLetterQueue)) {
            return WarehouseCommandListener.CONSUMER;
        }
        if (properties.label().deadLetterQueue().equals(deadLetterQueue)) {
            return LabelCommandListener.CONSUMER;
        }
        return "DESCONOCIDO";
    }

    /** El ultimo error recuperable si lo hubo; si no, el motivo del broker (x-death). */
    private String reason(MessageProperties props) {
        Object lastError = props.getHeaders().get(AcknowledgingMessageHandler.LAST_ERROR_HEADER);
        if (lastError != null) {
            return lastError.toString();
        }
        Object brokerReason = firstDeath(props).get("reason");
        return "rejected".equals(String.valueOf(brokerReason))
                ? "Mensaje rechazado por invalido o error inesperado"
                : String.valueOf(brokerReason);
    }

    private EventEnvelope tryParse(Message message) {
        try {
            EventEnvelope envelope = objectMapper.readValue(message.getBody(), EventEnvelope.class);
            return envelope != null && envelope.eventId() != null && envelope.data() != null
                    && envelope.data().shipmentId() != null ? envelope : null;
        } catch (IOException ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> firstDeath(MessageProperties props) {
        Object xDeath = props.getHeaders().get("x-death");
        if (xDeath instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> map) {
            return (Map<String, ?>) map;
        }
        return Map.of();
    }

    private static String truncate(String value, int max) {
        return value != null && value.length() > max ? value.substring(0, max) : value;
    }

}
