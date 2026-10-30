package cl.duoc.rutaexpress.notifications.messaging.consumer.email;

import cl.duoc.rutaexpress.notifications.messaging.support.AcknowledgingMessageHandler;
import cl.duoc.rutaexpress.notifications.notification.NotificationService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * CONSUMIDOR de q.cmd.email: envio registrado (email.send) y cambios de
 * estado (email.<estado> via email.*). Delega en NotificationService.
 * La confirmacion (ACK/NACK/DLQ) la resuelve AcknowledgingMessageHandler.
 */
@Component
@RequiredArgsConstructor
public class EmailCommandListener {

    public static final String CONSUMER = "EMAIL";

    private final AcknowledgingMessageHandler handler;
    private final NotificationService notificationService;

    @RabbitListener(queues = "${app.messaging.email.queue}")
    public void onEmailCommand(Message message, Channel channel) {
        handler.handle(message, channel, CONSUMER, notificationService::notifyCustomer);
    }

}
