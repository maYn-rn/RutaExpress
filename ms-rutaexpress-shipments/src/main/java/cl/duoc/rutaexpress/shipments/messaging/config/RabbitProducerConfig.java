package cl.duoc.rutaexpress.shipments.messaging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de mensajeria del lado PRODUCTOR.
 *
 * Rutas de mensajeria:
 *   envio creado      --(email.send, label.gen)--> cmd.direct (direct)
 *   envio ACEPTADO    --(warehouse.ticket)-------> cmd.direct (direct)
 *   cambio de estado  --(email.<estado>)---------> cmd.topic  (topic)
 *
 * Este servicio solo declara los exchanges donde publica. Las colas q.cmd.*,
 * sus DLQ y los bindings los declara el consumidor (ms-rutaexpress-notifications).
 */
@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class RabbitProducerConfig {

    /** Comandos punto a punto: la routing key identifica exactamente la cola. */
    @Bean
    public DirectExchange commandDirectExchange(MessagingProperties properties) {
        return new DirectExchange(properties.exchanges().direct(), true, false);
    }

    /** Comandos por patron: email.<estado> llega a q.cmd.email via email.*. */
    @Bean
    public TopicExchange commandTopicExchange(MessagingProperties properties) {
        return new TopicExchange(properties.exchanges().topic(), true, false);
    }

    /** Serializa los mensajes como JSON (legibles en el panel de RabbitMQ). */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

}
