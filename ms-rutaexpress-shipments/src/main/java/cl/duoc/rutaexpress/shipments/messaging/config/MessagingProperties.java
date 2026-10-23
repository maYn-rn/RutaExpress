package cl.duoc.rutaexpress.shipments.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de la mensajeria leidos desde application.yml (app.messaging.*).
 * Es el unico lugar desde donde el codigo obtiene exchange y routing keys.
 */
@ConfigurationProperties(prefix = "app.messaging")
public record MessagingProperties(String exchange, RoutingKeys routingKeys) {

    public record RoutingKeys(String shipmentCreated, String shipmentStatusChanged) {
    }

}
