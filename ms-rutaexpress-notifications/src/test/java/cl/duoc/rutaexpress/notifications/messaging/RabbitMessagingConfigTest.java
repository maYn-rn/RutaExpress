package cl.duoc.rutaexpress.notifications.messaging;

import cl.duoc.rutaexpress.notifications.messaging.config.MessagingProperties;
import cl.duoc.rutaexpress.notifications.messaging.config.RabbitMessagingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las colas principales derivan sus mensajes rechazados a la DLX y cada
 * ruta queda unida con la routing key configurada.
 */
class RabbitMessagingConfigTest {

    private final RabbitMessagingConfig config = new RabbitMessagingConfig();
    private final MessagingProperties properties = new MessagingProperties(
            "rutaexpress.shipments.exchange", "rutaexpress.shipments.dlx", 3, "#fallo",
            new MessagingProperties.Route("notifications.shipment.queue", "shipment.#",
                    "notifications.shipment.dlq", "notifications.dead"),
            new MessagingProperties.Route("tickets.shipment.queue", "shipment.created",
                    "tickets.shipment.dlq", "tickets.dead"));

    @Test
    void mainQueuesAreDurableAndPointToDeadLetterExchange() {
        Queue notifications = config.notificationsQueue(properties);
        Queue tickets = config.ticketsQueue(properties);

        assertThat(notifications.isDurable()).isTrue();
        assertThat(notifications.getArguments())
                .containsEntry("x-dead-letter-exchange", "rutaexpress.shipments.dlx")
                .containsEntry("x-dead-letter-routing-key", "notifications.dead");
        assertThat(tickets.getArguments())
                .containsEntry("x-dead-letter-routing-key", "tickets.dead");
    }

    @Test
    void bindingsUseConfiguredRoutingKeys() {
        TopicExchange exchange = config.shipmentsExchange(properties);
        DirectExchange dlx = config.deadLetterExchange(properties);

        Binding notifications = config.notificationsBinding(config.notificationsQueue(properties), exchange, properties);
        Binding tickets = config.ticketsBinding(config.ticketsQueue(properties), exchange, properties);
        Binding ticketsDead = config.ticketsDeadLetterBinding(config.ticketsDeadLetterQueue(properties), dlx, properties);

        assertThat(notifications.getRoutingKey()).isEqualTo("shipment.#");
        assertThat(tickets.getRoutingKey()).isEqualTo("shipment.created");
        assertThat(ticketsDead.getExchange()).isEqualTo("rutaexpress.shipments.dlx");
        assertThat(ticketsDead.getDestination()).isEqualTo("tickets.shipment.dlq");
    }

}
