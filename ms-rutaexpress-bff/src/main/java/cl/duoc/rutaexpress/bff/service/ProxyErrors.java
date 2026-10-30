package cl.duoc.rutaexpress.bff.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

/**
 * Respuestas de error propias del BFF cuando el microservicio destino no
 * responde (caido, timeout, DNS). Mantiene el mismo formato JSON que
 * RestAuthenticationEntryPoint y RestAccessDeniedHandler.
 */
public final class ProxyErrors {

    private ProxyErrors() {
    }

    static ResponseEntity<String> serviceUnavailable(String serviceName) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "El servicio " + serviceName + " no esta disponible en este momento");
    }

    public static ResponseEntity<String> error(HttpStatus status, String message) {
        String body = """
                {"timestamp":"%s","status":%d,"error":"%s","message":"%s"}"""
                .formatted(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

}
