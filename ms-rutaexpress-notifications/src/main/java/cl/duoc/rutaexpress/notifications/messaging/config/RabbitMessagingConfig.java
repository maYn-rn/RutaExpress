package cl.duoc.rutaexpress.notifications.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara toda la infraestructura RabbitMQ que consume este servicio.
 * Spring la crea en el broker al arrancar si no existe.
 *
 * Rutas de mensajeria:
 *
 * 1) Notificaciones
 *    rutaexpress.shipments.exchange --(shipment.#)--> notifications.shipment.queue
 *    Recibe envio creado y cambios de estado; avisa al cliente.
 *
 * 2) Tickets / guia de despacho
 *    rutaexpress.shipments.exchange --(shipment.created)--> tickets.shipment.queue
 *    Recibe solo envios creados; genera el ticket del envio.
 *
 * 3) Dead Letter (mensajes no entregados)
 *    Cada cola principal tiene x-dead-letter-exchange = rutaexpress.shipments.dlx.
 *    Un mensaje rechazado (reject/nack sin requeue) se reenvia ahi y termina en:
 *      rutaexpress.shipments.dlx --(notifications.dead)--> notifications.shipment.dlq
 *      rutaexpress.shipments.dlx --(tickets.dead)-------> tickets.shipment.dlq
 *    DeadLetterListener los registra en el log.
 *
 * No se registra un MessageConverter JSON a proposito: los consumidores
 * reciben el Message crudo y AcknowledgingMessageHandler deserializa el
 * cuerpo. Asi un JSON invalido se maneja con REJECT explicito en vez de
 * fallar antes de llegar al consumidor.
 */
@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class RabbitMessagingConfig {

    // ---------- Exchanges ----------

    /** Exchange donde ms-rutaexpress-shipments publica los eventos. */
    @Bean
    public TopicExchange shipmentsExchange(MessagingProperties properties) {
        return new TopicExchange(properties.exchange(), true, false);
    }

    /** Exchange que recibe los mensajes rechazados de las colas principales. */
    @Bean
    public DirectExchange deadLetterExchange(MessagingProperties properties) {
        return new DirectExchange(properties.deadLetterExchange(), true, false);
    }

    // ---------- Ruta 1: notificaciones ----------

    @Bean
    public Queue notificationsQueue(MessagingProperties properties) {
        return mainQueue(properties, properties.notifications());
    }

    @Bean
    public Queue notificationsDeadLetterQueue(MessagingProperties properties) {
        return QueueBuilder.durable(properties.notifications().deadLetterQueue()).build();
    }

    @Bean
    public Binding notificationsBinding(Queue notificationsQueue, TopicExchange shipmentsExchange,
                                        MessagingProperties properties) {
        return BindingBuilder.bind(notificationsQueue).to(shipmentsExchange)
                .with(properties.notifications().routingKey());
    }

    @Bean
    public Binding notificationsDeadLetterBinding(Queue notificationsDeadLetterQueue, DirectExchange deadLetterExchange,
                                                  MessagingProperties properties) {
        return BindingBuilder.bind(notificationsDeadLetterQueue).to(deadLetterExchange)
                .with(properties.notifications().deadLetterRoutingKey());
    }

    // ---------- Ruta 2: tickets ----------

    @Bean
    public Queue ticketsQueue(MessagingProperties properties) {
        return mainQueue(properties, properties.tickets());
    }

    @Bean
    public Queue ticketsDeadLetterQueue(MessagingProperties properties) {
        return QueueBuilder.durable(properties.tickets().deadLetterQueue()).build();
    }

    @Bean
    public Binding ticketsBinding(Queue ticketsQueue, TopicExchange shipmentsExchange,
                                  MessagingProperties properties) {
        return BindingBuilder.bind(ticketsQueue).to(shipmentsExchange)
                .with(properties.tickets().routingKey());
    }

    @Bean
    public Binding ticketsDeadLetterBinding(Queue ticketsDeadLetterQueue, DirectExchange deadLetterExchange,
                                            MessagingProperties properties) {
        return BindingBuilder.bind(ticketsDeadLetterQueue).to(deadLetterExchange)
                .with(properties.tickets().deadLetterRoutingKey());
    }

    /** Cola principal durable que deriva sus mensajes rechazados a la DLX. */
    private Queue mainQueue(MessagingProperties properties, MessagingProperties.Route route) {
        return QueueBuilder.durable(route.queue())
                .deadLetterExchange(properties.deadLetterExchange())
                .deadLetterRoutingKey(route.deadLetterRoutingKey())
                .build();
    }

}
