package cl.duoc.rutaexpress.notifications.messaging.consumer.ticket;

import cl.duoc.rutaexpress.notifications.messaging.support.AcknowledgingMessageHandler;
import cl.duoc.rutaexpress.notifications.ticket.TicketService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * CONSUMIDOR del dominio "tickets": escucha solo envios creados
 * (routing key shipment.created) y genera la guia de despacho.
 * La confirmacion (ACK/NACK/DLQ) la resuelve AcknowledgingMessageHandler.
 */
@Component
@RequiredArgsConstructor
public class ShipmentTicketListener {

    private final AcknowledgingMessageHandler handler;
    private final TicketService ticketService;

    @RabbitListener(queues = "${app.messaging.tickets.queue}")
    public void onShipmentCreated(Message message, Channel channel) {
        handler.handle(message, channel, "TICKETS", ticketService::generateTicket);
    }

}
