package cl.duoc.rutaexpress.notifications.messaging.consumer.warehouse;

import cl.duoc.rutaexpress.notifications.messaging.support.AcknowledgingMessageHandler;
import cl.duoc.rutaexpress.notifications.ticket.TicketService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * CONSUMIDOR de q.cmd.warehouse: envios ACEPTADOS (warehouse.ticket) que
 * necesitan ticket de picking en bodega. Delega en TicketService.
 */
@Component
@RequiredArgsConstructor
public class WarehouseCommandListener {

    public static final String CONSUMER = "WAREHOUSE";

    private final AcknowledgingMessageHandler handler;
    private final TicketService ticketService;

    @RabbitListener(queues = "${app.messaging.warehouse.queue}")
    public void onWarehouseCommand(Message message, Channel channel) {
        handler.handle(message, channel, CONSUMER, ticketService::generateTicket);
    }

}
