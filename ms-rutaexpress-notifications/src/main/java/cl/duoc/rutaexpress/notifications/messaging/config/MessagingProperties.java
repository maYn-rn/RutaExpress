package cl.duoc.rutaexpress.notifications.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Nombres y parametros de la mensajeria leidos desde application.yml
 * (app.messaging.*). Ninguna clase escribe nombres de colas a mano.
 */
@ConfigurationProperties(prefix = "app.messaging")
public record MessagingProperties(
        Exchanges exchanges,
        int maxRetries,
        String simulatedFailureKeyword,
        Route email,
        Route warehouse,
        Route label
) {

    public record Exchanges(String direct, String topic, String deadLetter) {
    }

    /** Un flujo: cola principal con sus dos bindings y su DLQ. */
    public record Route(String queue, String directKey, String topicPattern,
                        String deadLetterQueue, String deadLetterKey) {
    }

    public List<Route> routes() {
        return List.of(email, warehouse, label);
    }

}
