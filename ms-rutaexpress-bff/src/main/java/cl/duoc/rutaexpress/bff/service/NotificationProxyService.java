package cl.duoc.rutaexpress.bff.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/** Reenvia hacia ms-rutaexpress-notifications (notificaciones y metricas de mensajeria). */
@Service
@RequiredArgsConstructor
public class NotificationProxyService {

    private final WebClient notificationsWebClient;

    public ResponseEntity<String> getCustomerNotifications(String clienteEmail) {
        return forward(HttpMethod.GET, "/api/notifications?clienteEmail={email}", clienteEmail);
    }

    public ResponseEntity<String> markAsRead(Long id, String clienteEmail) {
        return forward(HttpMethod.PUT, "/api/notifications/{id}/leida?clienteEmail={email}", id, clienteEmail);
    }

    public ResponseEntity<String> getMessagingMetrics() {
        return forward(HttpMethod.GET, "/api/messaging/metrics");
    }

    private ResponseEntity<String> forward(HttpMethod method, String uri, Object... uriVariables) {
        try {
            return notificationsWebClient.method(method).uri(uri, uriVariables)
                    .retrieve().toEntity(String.class).block();
        } catch (WebClientResponseException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .headers(ex.getHeaders())
                    .body(ex.getResponseBodyAsString());
        } catch (WebClientRequestException ex) {
            return ProxyErrors.serviceUnavailable("de notificaciones");
        }
    }

}
