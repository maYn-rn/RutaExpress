package cl.duoc.rutaexpress.notifications.notification;

import cl.duoc.rutaexpress.notifications.notification.dto.NotificationResponse;
import cl.duoc.rutaexpress.notifications.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Lo que el cliente consulta desde la app. Siempre se filtra por el email del
 * cliente: el BFF lo toma del token, nunca del navegador.
 */
@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    private final NotificationRepository repository;

    @Transactional(readOnly = true)
    public List<NotificationResponse> findByCustomer(String clienteEmail) {
        return repository.findByClienteEmailIgnoreCaseOrderByCreadaEnDesc(clienteEmail).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    /** Vacio si no existe o no pertenece a ese cliente (mismo resultado: 404). */
    @Transactional
    public Optional<NotificationResponse> markAsRead(Long id, String clienteEmail) {
        return repository.findByIdAndClienteEmailIgnoreCase(id, clienteEmail)
                .map(notification -> {
                    notification.setLeida(true);
                    Notification saved = repository.save(notification);
                    return NotificationResponse.from(saved);
                });
    }

}
