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
public class RabbitAdminProxyService {

    private static final String BASE_PATH = "/api/admin/rabbit";

    private final WebClient rabbitAdminWebClient;

    public ResponseEntity<String> createQueue(String body) {
        return forward(HttpMethod.POST, BASE_PATH + "/queues", body);
    }

    public ResponseEntity<String> getQueue(String name) {
        return forward(HttpMethod.GET, BASE_PATH + "/queues/" + name, null);
    }

    public ResponseEntity<String> deleteQueue(String name) {
        return forward(HttpMethod.DELETE, BASE_PATH + "/queues/" + name, null);
    }

    public ResponseEntity<String> createExchange(String body) {
        return forward(HttpMethod.POST, BASE_PATH + "/exchanges", body);
    }

    public ResponseEntity<String> deleteExchange(String name) {
        return forward(HttpMethod.DELETE, BASE_PATH + "/exchanges/" + name, null);
    }

    public ResponseEntity<String> createBinding(String body) {
        return forward(HttpMethod.POST, BASE_PATH + "/bindings", body);
    }

    public ResponseEntity<String> deleteBinding(String body) {
        return forward(HttpMethod.DELETE, BASE_PATH + "/bindings", body);
    }

    private ResponseEntity<String> forward(HttpMethod method, String uri, String body) {
        WebClient.RequestBodySpec requestSpec = rabbitAdminWebClient.method(method).uri(uri);
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
            return ProxyErrors.serviceUnavailable("de administracion de RabbitMQ");
        }
    }

}
