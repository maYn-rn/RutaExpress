package cl.duoc.rutaexpress.notifications.messaging.consumer.label;

import cl.duoc.rutaexpress.notifications.label.LabelService;
import cl.duoc.rutaexpress.notifications.messaging.support.AcknowledgingMessageHandler;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * CONSUMIDOR de q.cmd.label: envios recien creados (label.gen) que
 * necesitan guia de despacho. Delega en LabelService.
 */
@Component
@RequiredArgsConstructor
public class LabelCommandListener {

    public static final String CONSUMER = "LABEL";

    private final AcknowledgingMessageHandler handler;
    private final LabelService labelService;

    @RabbitListener(queues = "${app.messaging.label.queue}")
    public void onLabelCommand(Message message, Channel channel) {
        handler.handle(message, channel, CONSUMER, labelService::generateLabel);
    }

}
