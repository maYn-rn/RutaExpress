package cl.duoc.rutaexpress.shipments.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de la mensajeria leidos desde application.yml (app.messaging.*).
 * Es el unico lugar desde donde el codigo obtiene exchanges y routing keys.
 */
@ConfigurationProperties(prefix = "app.messaging")
public record MessagingProperties(Exchanges exchanges, RoutingKeys routingKeys) {

    public record Exchanges(String direct, String topic) {
    }

    public record RoutingKeys(String emailSend, String emailStatusPrefix, String warehouseTicket, String labelGen) {
    }

}
