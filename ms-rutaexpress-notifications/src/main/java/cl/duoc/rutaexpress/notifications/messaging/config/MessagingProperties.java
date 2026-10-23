package cl.duoc.rutaexpress.notifications.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres y parametros de la mensajeria leidos desde application.yml
 * (app.messaging.*). Ninguna clase escribe nombres de colas a mano.
 */
@ConfigurationProperties(prefix = "app.messaging")
public record MessagingProperties(
        String exchange,
        String deadLetterExchange,
        int maxRetries,
        String simulatedFailureKeyword,
        Route notifications,
        Route tickets
) {

    /** Una ruta de mensajeria: cola principal, su binding y su DLQ. */
    public record Route(String queue, String routingKey, String deadLetterQueue, String deadLetterRoutingKey) {
    }

}
