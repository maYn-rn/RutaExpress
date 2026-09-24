package cl.duoc.rutaexpress.bff.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
public class ShipmentProxyService {

    private static final String SHIPMENTS_PATH = "/api/shipments";

    private final WebClient shipmentsWebClient;

    public ResponseEntity<String> getAllShipments() {
        return forward(HttpMethod.GET, SHIPMENTS_PATH, null);
    }

    public ResponseEntity<String> getShipmentById(Long id) {
        return forward(HttpMethod.GET, SHIPMENTS_PATH + "/" + id, null);
    }

    public ResponseEntity<String> createShipment(String body) {
        return forward(HttpMethod.POST, SHIPMENTS_PATH, body);
    }

    public ResponseEntity<String> updateShipmentStatus(Long id, String body) {
        return forward(HttpMethod.PUT, SHIPMENTS_PATH + "/" + id + "/estado", body);
    }

    public ResponseEntity<String> deleteShipment(Long id) {
        return forward(HttpMethod.DELETE, SHIPMENTS_PATH + "/" + id, null);
    }

    private ResponseEntity<String> forward(HttpMethod method, String uri, String body) {
        WebClient.RequestBodySpec requestSpec = shipmentsWebClient.method(method).uri(uri);
        WebClient.RequestHeadersSpec<?> headersSpec = (body != null)
                ? requestSpec.contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                : requestSpec;

        try {
            return headersSpec.retrieve().toEntity(String.class).block();
        } catch (WebClientResponseException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .headers(ex.getHeaders())
                    .body(ex.getResponseBodyAsString());
        } catch (WebClientRequestException ex) {
            return ProxyErrors.serviceUnavailable("de envios");
        }
    }

}
