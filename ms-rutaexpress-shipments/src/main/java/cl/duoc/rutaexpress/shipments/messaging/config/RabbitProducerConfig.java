package cl.duoc.rutaexpress.shipments.messaging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de mensajeria del lado PRODUCTOR.
 *
 * Ruta de mensajeria:
 *   ms-rutaexpress-shipments --(shipment.created | shipment.status.changed)-->
 *   rutaexpress.shipments.exchange (topic)
 *
 * Este servicio solo declara el exchange donde publica. Las colas, DLQ y
 * bindings las declara el consumidor (ms-rutaexpress-notifications).
 */
@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class RabbitProducerConfig {

    /** Exchange durable donde se publican todos los eventos de envios. */
    @Bean
    public TopicExchange shipmentsExchange(MessagingProperties properties) {
        return new TopicExchange(properties.exchange(), true, false);
    }

    /** Serializa los eventos como JSON (legibles en el panel de RabbitMQ). */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

}
