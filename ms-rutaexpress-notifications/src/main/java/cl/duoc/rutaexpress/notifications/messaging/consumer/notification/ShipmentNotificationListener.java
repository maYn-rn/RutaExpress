package cl.duoc.rutaexpress.notifications.messaging.consumer.notification;

import cl.duoc.rutaexpress.notifications.messaging.support.AcknowledgingMessageHandler;
import cl.duoc.rutaexpress.notifications.notification.NotificationService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * CONSUMIDOR del dominio "notificaciones": escucha envios creados y cambios de
 * estado (routing key shipment.#) y delega en NotificationService.
 * La confirmacion (ACK/NACK/DLQ) la resuelve AcknowledgingMessageHandler.
 */
@Component
@RequiredArgsConstructor
public class ShipmentNotificationListener {

    private final AcknowledgingMessageHandler handler;
    private final NotificationService notificationService;

    @RabbitListener(queues = "${app.messaging.notifications.queue}")
    public void onShipmentEvent(Message message, Channel channel) {
        handler.handle(message, channel, "NOTIFICACIONES", notificationService::notifyCustomer);
    }

}
