package cl.duoc.rutaexpress.notifications.notification;

import cl.duoc.rutaexpress.notifications.notification.dto.NotificationResponse;
import cl.duoc.rutaexpress.notifications.notification.entity.Notification;
import cl.duoc.rutaexpress.notifications.notification.entity.NotificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Cada cliente solo ve y marca como leidas sus propias notificaciones. */
@DataJpaTest
class NotificationQueryServiceTest {

    @Autowired
    private NotificationRepository repository;

    private NotificationQueryService service;

    @BeforeEach
    void setUp() {
        service = new NotificationQueryService(repository);
        repository.deleteAll();
    }

    private Notification save(String eventId, String email, Instant at) {
        return repository.save(Notification.builder()
                .eventId(eventId).shipmentId(1L).clienteEmail(email).estadoEnvio("CREADO")
                .asunto("a").mensaje("m").estado(NotificationStatus.ENVIADA).leida(false).creadaEn(at)
                .build());
    }

    @Test
    void returnsOnlyCustomerNotificationsNewestFirst() {
        save("e1", "ana@rutaexpress.cl", Instant.parse("2026-10-01T10:00:00Z"));
        save("e2", "ana@rutaexpress.cl", Instant.parse("2026-10-02T10:00:00Z"));
        save("e3", "luis@rutaexpress.cl", Instant.parse("2026-10-03T10:00:00Z"));

        List<NotificationResponse> result = service.findByCustomer("ANA@rutaexpress.cl");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).creadaEn()).isEqualTo(Instant.parse("2026-10-02T10:00:00Z"));
    }

    @Test
    void customerCanMarkOwnNotificationAsRead() {
        Notification own = save("e1", "ana@rutaexpress.cl", Instant.now());

        assertThat(service.markAsRead(own.getId(), "ana@rutaexpress.cl"))
                .hasValueSatisfying(n -> assertThat(n.leida()).isTrue());
    }

    @Test
    void customerCannotMarkSomeoneElsesNotification() {
        Notification other = save("e1", "luis@rutaexpress.cl", Instant.now());

        assertThat(service.markAsRead(other.getId(), "ana@rutaexpress.cl")).isEmpty();
        assertThat(repository.findById(other.getId()).orElseThrow().isLeida()).isFalse();
    }

}
