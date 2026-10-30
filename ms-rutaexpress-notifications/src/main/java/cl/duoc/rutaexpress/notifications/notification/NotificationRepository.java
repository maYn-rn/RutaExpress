package cl.duoc.rutaexpress.notifications.notification;

import cl.duoc.rutaexpress.notifications.notification.entity.Notification;
import cl.duoc.rutaexpress.notifications.notification.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByClienteEmailIgnoreCaseOrderByCreadaEnDesc(String clienteEmail);

    Optional<Notification> findByIdAndClienteEmailIgnoreCase(Long id, String clienteEmail);

    boolean existsByEventId(String eventId);

    long countByEstado(NotificationStatus estado);

}
