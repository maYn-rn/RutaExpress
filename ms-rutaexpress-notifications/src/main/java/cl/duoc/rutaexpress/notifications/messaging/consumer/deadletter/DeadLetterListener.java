package cl.duoc.rutaexpress.notifications.messaging.consumer.deadletter;

import cl.duoc.rutaexpress.notifications.messaging.support.AcknowledgingMessageHandler;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * CONSUMIDOR de mensajes no entregados (DLQ). Registra en el log por que
 * murio cada mensaje (header x-death: motivo, cola de origen, veces) y lo
 * confirma con ACK para que no se acumule sin control.
 */
@Slf4j
@Component
public class DeadLetterListener {

    @RabbitListener(queues = {
            "${app.messaging.notifications.dead-letter-queue}",
            "${app.messaging.tickets.dead-letter-queue}"
    })
    public void onDeadLetter(Message message, Channel channel) throws IOException {
        MessageProperties props = message.getMessageProperties();
        Map<String, ?> death = firstDeath(props);

        log.error("[DLQ] Mensaje no entregado | dlq={} | colaOrigen={} | motivo={} | reintentos={} | body={}",
                props.getConsumerQueue(),
                death.get("queue"),
                death.get("reason"),
                props.getHeaders().getOrDefault(AcknowledgingMessageHandler.RETRY_COUNT_HEADER, 0),
                new String(message.getBody(), StandardCharsets.UTF_8));

        channel.basicAck(props.getDeliveryTag(), false);
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> firstDeath(MessageProperties props) {
        Object xDeath = props.getHeaders().get("x-death");
        if (xDeath instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> map) {
            return (Map<String, ?>) map;
        }
        return Map.of();
    }

}
