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
final class ProxyErrors {

    private ProxyErrors() {
    }

    static ResponseEntity<String> serviceUnavailable(String serviceName) {
        String body = """
                {"timestamp":"%s","status":503,"error":"Service Unavailable","message":"El servicio %s no esta disponible en este momento"}"""
                .formatted(LocalDateTime.now(), serviceName);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

}
