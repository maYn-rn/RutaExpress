package cl.duoc.rutaexpress.notifications.messaging;

import cl.duoc.rutaexpress.notifications.messaging.config.MessagingProperties;
import cl.duoc.rutaexpress.notifications.messaging.config.RabbitMessagingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * La topologia coincide con el diagrama: 6 colas (3 flujos + 3 DLQ), cada
 * cola principal con binding direct y topic, y derivando a cmd.dead.dlx.
 */
class RabbitMessagingConfigTest {

    private final RabbitMessagingConfig config = new RabbitMessagingConfig();
    private final MessagingProperties properties = new MessagingProperties(
            new MessagingProperties.Exchanges("cmd.direct", "cmd.topic", "cmd.dead.dlx"), 3, "#fallo",
            new MessagingProperties.Route("q.cmd.email", "email.send", "email.*", "q.cmd.email.dlq", "email.dlq"),
            new MessagingProperties.Route("q.cmd.warehouse", "warehouse.ticket", "warehouse.#", "q.cmd.warehouse.dlq", "warehouse.dlq"),
            new MessagingProperties.Route("q.cmd.label", "label.gen", "label.*", "q.cmd.label.dlq", "label.dlq"));

    private Declarables topology() {
        return config.commandQueues(properties,
                config.commandDirectExchange(properties),
                config.commandTopicExchange(properties),
                config.deadLetterExchange(properties));
    }

    @Test
    void declaresSixDurableQueues() {
        List<Queue> queues = topology().getDeclarablesByType(Queue.class);

        assertThat(queues).extracting(Queue::getName).containsExactlyInAnyOrder(
                "q.cmd.email", "q.cmd.email.dlq",
                "q.cmd.warehouse", "q.cmd.warehouse.dlq",
                "q.cmd.label", "q.cmd.label.dlq");
        assertThat(queues).allMatch(Queue::isDurable);
    }

    @Test
    void mainQueuesPointToDeadLetterExchange() {
        Queue email = topology().getDeclarablesByType(Queue.class).stream()
                .filter(q -> q.getName().equals("q.cmd.email")).findFirst().orElseThrow();

        assertThat(email.getArguments())
                .containsEntry("x-dead-letter-exchange", "cmd.dead.dlx")
                .containsEntry("x-dead-letter-routing-key", "email.dlq");
    }

    @Test
    void bindingsMatchDiagram() {
        List<Binding> bindings = topology().getDeclarablesByType(Binding.class);

        assertThat(bindings).extracting(Binding::getExchange, Binding::getRoutingKey, Binding::getDestination)
                .containsExactlyInAnyOrder(
                        tuple("cmd.direct", "email.send", "q.cmd.email"),
                        tuple("cmd.topic", "email.*", "q.cmd.email"),
                        tuple("cmd.dead.dlx", "email.dlq", "q.cmd.email.dlq"),
                        tuple("cmd.direct", "warehouse.ticket", "q.cmd.warehouse"),
                        tuple("cmd.topic", "warehouse.#", "q.cmd.warehouse"),
                        tuple("cmd.dead.dlx", "warehouse.dlq", "q.cmd.warehouse.dlq"),
                        tuple("cmd.direct", "label.gen", "q.cmd.label"),
                        tuple("cmd.topic", "label.*", "q.cmd.label"),
                        tuple("cmd.dead.dlx", "label.dlq", "q.cmd.label.dlq"));
    }

}
