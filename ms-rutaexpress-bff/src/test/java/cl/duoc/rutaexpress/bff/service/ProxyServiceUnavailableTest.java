package cl.duoc.rutaexpress.bff.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Si el microservicio destino no responde (puerto cerrado), el BFF debe
 * devolver 503 con cuerpo JSON en lugar de propagar un 500 generico.
 */
class ProxyServiceUnavailableTest {

    // Puerto 1: no hay nada escuchando, la conexion es rechazada de inmediato.
    private final WebClient unreachable = WebClient.builder().baseUrl("http://127.0.0.1:1").build();

    @Test
    void catalogReturns503WhenServiceIsDown() {
        ResponseEntity<String> response = new CatalogProxyService(unreachable).getAllServices();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).contains("\"status\":503").contains("catalogo");
    }

    @Test
    void shipmentsReturns503WhenServiceIsDown() {
        ResponseEntity<String> response = new ShipmentProxyService(unreachable).getAllShipments();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).contains("\"status\":503").contains("envios");
    }

}
