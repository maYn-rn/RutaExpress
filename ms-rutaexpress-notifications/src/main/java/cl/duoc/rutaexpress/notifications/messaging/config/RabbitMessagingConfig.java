package cl.duoc.rutaexpress.notifications.messaging.config;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Declara toda la topologia RabbitMQ que consume este servicio.
 * Spring la crea en el broker al arrancar si no existe.
 *
 * Exchanges:
 *   cmd.direct   (direct) comandos punto a punto
 *   cmd.topic    (topic)  comandos por patron
 *   cmd.dead.dlx (direct) mensajes rechazados
 *
 * Por cada flujo (email, warehouse, label):
 *   cmd.direct --(email.send)--> q.cmd.email
 *   cmd.topic  --(email.*)-----> q.cmd.email
 *   q.cmd.email tiene x-dead-letter-exchange = cmd.dead.dlx, asi un mensaje
 *   rechazado (reject/nack sin requeue) termina en:
 *   cmd.dead.dlx --(email.dlq)--> q.cmd.email.dlq
 *
 * No se registra un MessageConverter JSON a proposito: los consumidores
 * reciben el Message crudo y AcknowledgingMessageHandler deserializa el
 * cuerpo. Asi un JSON invalido se maneja con REJECT explicito en vez de
 * fallar antes de llegar al consumidor.
 */
@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class RabbitMessagingConfig {

    @Bean
    public DirectExchange commandDirectExchange(MessagingProperties properties) {
        return new DirectExchange(properties.exchanges().direct(), true, false);
    }

    @Bean
    public TopicExchange commandTopicExchange(MessagingProperties properties) {
        return new TopicExchange(properties.exchanges().topic(), true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange(MessagingProperties properties) {
        return new DirectExchange(properties.exchanges().deadLetter(), true, false);
    }

    /** Las 6 colas (3 principales + 3 DLQ) y sus bindings. */
    @Bean
    public Declarables commandQueues(MessagingProperties properties, DirectExchange commandDirectExchange,
                                     TopicExchange commandTopicExchange, DirectExchange deadLetterExchange) {
        List<Declarable> declarables = new ArrayList<>();
        for (MessagingProperties.Route route : properties.routes()) {
            Queue main = QueueBuilder.durable(route.queue())
                    .deadLetterExchange(properties.exchanges().deadLetter())
                    .deadLetterRoutingKey(route.deadLetterKey())
                    .build();
            Queue dlq = QueueBuilder.durable(route.deadLetterQueue()).build();

            declarables.add(main);
            declarables.add(dlq);
            declarables.add(BindingBuilder.bind(main).to(commandDirectExchange).with(route.directKey()));
            declarables.add(BindingBuilder.bind(main).to(commandTopicExchange).with(route.topicPattern()));
            declarables.add(BindingBuilder.bind(dlq).to(deadLetterExchange).with(route.deadLetterKey()));
        }
        return new Declarables(declarables);
    }

}
