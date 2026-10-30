package cl.duoc.rutaexpress.notifications.notification;

import cl.duoc.rutaexpress.notifications.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API interna (solo la llama el BFF, que agrega el email del usuario
 * autenticado como clienteEmail).
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationQueryService queryService;

    @GetMapping
    public List<NotificationResponse> findByCustomer(@RequestParam String clienteEmail) {
        return queryService.findByCustomer(clienteEmail);
    }

    @PutMapping("/{id}/leida")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id, @RequestParam String clienteEmail) {
        return ResponseEntity.of(queryService.markAsRead(id, clienteEmail));
    }

}
