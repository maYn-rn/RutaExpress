package cl.duoc.rutaexpress.bff.controller;

import cl.duoc.rutaexpress.bff.security.AccessRules;
import cl.duoc.rutaexpress.bff.security.CustomerScope;
import cl.duoc.rutaexpress.bff.service.NotificationProxyService;
import cl.duoc.rutaexpress.bff.service.ProxyErrors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Avisos del usuario autenticado (cambios de estado de sus envios, enviados o
 * fallidos) y metricas de la mensajeria. El email se toma del token, asi
 * cada usuario solo ve sus propias notificaciones.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NotificationProxyController {

    private final NotificationProxyService notificationProxyService;
    private final CustomerScope customerScope;

    @GetMapping("/notifications/me")
    @PreAuthorize(AccessRules.CAN_READ)
    public ResponseEntity<String> myNotifications(@AuthenticationPrincipal Jwt jwt) {
        return customerScope.email(jwt)
                .map(notificationProxyService::getCustomerNotifications)
                .orElseGet(this::noEmail);
    }

    @PutMapping("/notifications/{id}/leida")
    @PreAuthorize(AccessRules.CAN_READ)
    public ResponseEntity<String> markAsRead(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return customerScope.email(jwt)
                .map(email -> notificationProxyService.markAsRead(id, email))
                .orElseGet(this::noEmail);
    }

    /** Procesados vs DLQ por cola y notificaciones enviadas/fallidas: Admin u Operador. */
    @GetMapping("/messaging/metrics")
    @PreAuthorize(AccessRules.CAN_WRITE)
    public ResponseEntity<String> messagingMetrics() {
        return notificationProxyService.getMessagingMetrics();
    }

    private ResponseEntity<String> noEmail() {
        return ProxyErrors.error(HttpStatus.FORBIDDEN, "El token no incluye el email del usuario");
    }

}
