package cl.duoc.rutaexpress.notifications.notification;

import cl.duoc.rutaexpress.notifications.label.LabelService;
import cl.duoc.rutaexpress.notifications.messaging.config.MessagingProperties;
import cl.duoc.rutaexpress.notifications.messaging.event.EventEnvelope;
import cl.duoc.rutaexpress.notifications.messaging.event.ShipmentEvent;
import cl.duoc.rutaexpress.notifications.messaging.support.InvalidMessageException;
import cl.duoc.rutaexpress.notifications.messaging.support.RecoverableProcessingException;
import cl.duoc.rutaexpress.notifications.notification.entity.Notification;
import cl.duoc.rutaexpress.notifications.notification.entity.NotificationStatus;
import cl.duoc.rutaexpress.notifications.ticket.TicketService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BusinessServicesTest {

    private final MessagingProperties properties = new MessagingProperties(null, 3, "#fallo", null, null, null);
    private final EmailSender emailSender = mock(EmailSender.class);
    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final NotificationService notificationService = new NotificationService(properties, emailSender, repository);
    private final TicketService ticketService = new TicketService();
    private final LabelService labelService = new LabelService();

    {
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private EventEnvelope envelope(String type, String cliente, String email, String estado, String motivo) {
        ShipmentEvent data = new ShipmentEvent(5L, "Santiago", "Temuco", BigDecimal.TEN, cliente, email,
                "EN_TRANSITO", estado, motivo);
        return new EventEnvelope(type, "evt-1", Instant.now(), "trace-1", "shipment-5", data);
    }

    @Test
    void createdShipmentSendsEmailAndStoresSentNotification() {
        Notification saved = notificationService.notifyCustomer(
                envelope(EventEnvelope.SHIPMENT_CREATED, "Ana", "ana@rutaexpress.cl", "CREADO", null));

        verify(emailSender).send("ana@rutaexpress.cl", "Envio #5 registrado", saved.getMensaje());
        assertThat(saved.getMensaje()).contains("registramos tu envio #5");
        assertThat(saved.getEstado()).isEqualTo(NotificationStatus.ENVIADA);
        assertThat(saved.getEventId()).isEqualTo("evt-1");
        assertThat(saved.isLeida()).isFalse();
    }

    @Test
    void messageDependsOnNewStatus() {
        assertThat(notificationService.notifyCustomer(envelope(EventEnvelope.SHIPMENT_STATUS_CHANGED,
                "Ana", "ana@rutaexpress.cl", "ENTREGADO", null)).getMensaje())
                .contains("fue entregado en Temuco");
        assertThat(notificationService.notifyCustomer(envelope(EventEnvelope.SHIPMENT_STATUS_CHANGED,
                "Ana", "ana@rutaexpress.cl", "FALLIDO", "Destinatario ausente")).getMensaje())
                .contains("no pudimos entregar").contains("Motivo: Destinatario ausente");
    }

    @Test
    void simulatedFailureKeywordRaisesRecoverableErrorAndSendsNothing() {
        assertThatThrownBy(() -> notificationService.notifyCustomer(
                envelope(EventEnvelope.SHIPMENT_CREATED, "Ana #fallo", "ana@rutaexpress.cl", "CREADO", null)))
                .isInstanceOf(RecoverableProcessingException.class);
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
        verify(repository, never()).save(any());
    }

    @Test
    void shipmentWithoutEmailIsInvalid() {
        assertThatThrownBy(() -> notificationService.notifyCustomer(
                envelope(EventEnvelope.SHIPMENT_CREATED, "Ana", null, "CREADO", null)))
                .isInstanceOf(InvalidMessageException.class);
    }

    @Test
    void recordFailureStoresFailedNotificationWithReason() {
        notificationService.recordFailure(
                envelope(EventEnvelope.SHIPMENT_STATUS_CHANGED, "Ana", "Ana@RutaExpress.cl", "EN_TRANSITO", null),
                "Proveedor de email no disponible");

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getEstado()).isEqualTo(NotificationStatus.FALLIDA);
        assertThat(saved.getValue().getDetalleError()).isEqualTo("Proveedor de email no disponible");
        assertThat(saved.getValue().getClienteEmail()).isEqualTo("ana@rutaexpress.cl");
        assertThat(saved.getValue().getMensaje()).contains("va en ruta");
    }

    @Test
    void recordFailureIsIdempotent() {
        when(repository.existsByEventId("evt-1")).thenReturn(true);

        notificationService.recordFailure(
                envelope(EventEnvelope.SHIPMENT_CREATED, "Ana", "ana@rutaexpress.cl", "CREADO", null), "x");

        verify(repository, never()).save(any());
    }

    @Test
    void ticketNumberIncludesShipmentId() {
        assertThat(ticketService.generateTicket(
                envelope(EventEnvelope.SHIPMENT_STATUS_CHANGED, "Ana", "ana@rutaexpress.cl", "ACEPTADO", null)))
                .startsWith("TKT-").endsWith("-000005");
    }

    @Test
    void labelNumberIncludesShipmentId() {
        assertThat(labelService.generateLabel(
                envelope(EventEnvelope.SHIPMENT_CREATED, "Ana", "ana@rutaexpress.cl", "CREADO", null)))
                .startsWith("GD-").endsWith("-000005");
    }

    @Test
    void ticketWithInvalidWeightIsNotRecoverable() {
        ShipmentEvent data = new ShipmentEvent(5L, "Santiago", "Temuco", BigDecimal.ZERO, "Ana",
                "ana@rutaexpress.cl", null, "ACEPTADO", null);
        assertThatThrownBy(() -> ticketService.generateTicket(
                new EventEnvelope(EventEnvelope.SHIPMENT_STATUS_CHANGED, "evt-2", Instant.now(), "t", "c", data)))
                .isInstanceOf(InvalidMessageException.class);
    }

}
