package cl.duoc.rutaexpress.notifications.notification;

import cl.duoc.rutaexpress.notifications.messaging.config.MessagingProperties;
import cl.duoc.rutaexpress.notifications.messaging.event.ShipmentEvent;
import cl.duoc.rutaexpress.notifications.messaging.support.InvalidMessageException;
import cl.duoc.rutaexpress.notifications.messaging.support.RecoverableProcessingException;
import cl.duoc.rutaexpress.notifications.ticket.TicketService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BusinessServicesTest {

    private final MessagingProperties properties = new MessagingProperties(
            "x", "y", 3, "#fallo", null, null);
    private final NotificationService notificationService = new NotificationService(properties);
    private final TicketService ticketService = new TicketService();

    private ShipmentEvent event(String type, String cliente, BigDecimal peso) {
        return new ShipmentEvent(type, 5L, "Santiago", "Temuco", peso, cliente,
                "CREADO", "ACEPTADO", LocalDateTime.now());
    }

    @Test
    void notificationTextDependsOnEventType() {
        assertThat(notificationService.notifyCustomer(event(ShipmentEvent.SHIPMENT_CREATED, "Ana", BigDecimal.TEN)))
                .contains("registramos tu envio #5");
        assertThat(notificationService.notifyCustomer(event(ShipmentEvent.SHIPMENT_STATUS_CHANGED, "Ana", BigDecimal.TEN)))
                .contains("cambio de CREADO a ACEPTADO");
    }

    @Test
    void simulatedFailureKeywordRaisesRecoverableError() {
        assertThatThrownBy(() -> notificationService.notifyCustomer(
                event(ShipmentEvent.SHIPMENT_CREATED, "Ana #fallo", BigDecimal.TEN)))
                .isInstanceOf(RecoverableProcessingException.class);
    }

    @Test
    void ticketNumberIncludesShipmentId() {
        assertThat(ticketService.generateTicket(event(ShipmentEvent.SHIPMENT_CREATED, "Ana", BigDecimal.TEN)))
                .startsWith("TKT-").endsWith("-000005");
    }

    @Test
    void ticketWithInvalidWeightIsNotRecoverable() {
        assertThatThrownBy(() -> ticketService.generateTicket(
                event(ShipmentEvent.SHIPMENT_CREATED, "Ana", BigDecimal.ZERO)))
                .isInstanceOf(InvalidMessageException.class);
    }

}
