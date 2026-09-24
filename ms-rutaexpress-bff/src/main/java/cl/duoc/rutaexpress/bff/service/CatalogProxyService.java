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
public class CatalogProxyService {

    private static final String SERVICES_PATH = "/api/catalog/services";

    private final WebClient catalogWebClient;

    public ResponseEntity<String> getAllServices() {
        return forward(HttpMethod.GET, SERVICES_PATH, null);
    }

    public ResponseEntity<String> getServiceById(Long id) {
        return forward(HttpMethod.GET, SERVICES_PATH + "/" + id, null);
    }

    public ResponseEntity<String> createService(String body) {
        return forward(HttpMethod.POST, SERVICES_PATH, body);
    }

    public ResponseEntity<String> updateService(Long id, String body) {
        return forward(HttpMethod.PUT, SERVICES_PATH + "/" + id, body);
    }

    public ResponseEntity<String> deleteService(Long id) {
        return forward(HttpMethod.DELETE, SERVICES_PATH + "/" + id, null);
    }

    private ResponseEntity<String> forward(HttpMethod method, String uri, String body) {
        WebClient.RequestBodySpec requestSpec = catalogWebClient.method(method).uri(uri);
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
            return ProxyErrors.serviceUnavailable("de catalogo");
        }
    }

}
