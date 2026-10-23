package cl.duoc.rutaexpress.rabbitadmin.service;

import cl.duoc.rutaexpress.rabbitadmin.dto.BindingRequest;
import cl.duoc.rutaexpress.rabbitadmin.dto.CreateExchangeRequest;
import cl.duoc.rutaexpress.rabbitadmin.dto.CreateQueueRequest;
import cl.duoc.rutaexpress.rabbitadmin.dto.QueueInfoResponse;
import cl.duoc.rutaexpress.rabbitadmin.exception.ResourceConflictException;
import cl.duoc.rutaexpress.rabbitadmin.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.stereotype.Service;

/**
 * Toda la administracion de RabbitMQ (colas, exchanges y bindings) pasa por
 * aqui. El controlador solo llama a estos metodos y no conoce AmqpAdmin.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitAdminService {

    private final AmqpAdmin amqpAdmin;

    // ---------- Colas ----------

    public QueueInfoResponse createQueue(CreateQueueRequest request) {
        if (amqpAdmin.getQueueInfo(request.name()) != null) {
            throw new ResourceConflictException("La cola '" + request.name() + "' ya existe");
        }

        QueueBuilder builder = request.durableOrDefault()
                ? QueueBuilder.durable(request.name())
                : QueueBuilder.nonDurable(request.name());
        if (request.deadLetterExchange() != null) {
            builder.deadLetterExchange(request.deadLetterExchange());
        }
        if (request.deadLetterRoutingKey() != null && !request.deadLetterRoutingKey().isEmpty()) {
            builder.deadLetterRoutingKey(request.deadLetterRoutingKey());
        }
        if (request.messageTtlMs() != null) {
            builder.ttl(request.messageTtlMs());
        }

        Queue queue = builder.build();
        amqpAdmin.declareQueue(queue);
        log.info("[ADMIN] Cola creada: {} (durable={}, args={})", queue.getName(), queue.isDurable(), queue.getArguments());
        return new QueueInfoResponse(queue.getName(), 0, 0);
    }

    public QueueInfoResponse getQueue(String name) {
        QueueInformation info = amqpAdmin.getQueueInfo(name);
        if (info == null) {
            throw new ResourceNotFoundException("La cola '" + name + "' no existe");
        }
        return new QueueInfoResponse(info.getName(), info.getMessageCount(), info.getConsumerCount());
    }

    public void deleteQueue(String name) {
        getQueue(name);
        amqpAdmin.deleteQueue(name);
        log.info("[ADMIN] Cola eliminada: {}", name);
    }

    // ---------- Exchanges ----------

    public void createExchange(CreateExchangeRequest request) {
        ExchangeBuilder builder = switch (request.type()) {
            case DIRECT -> ExchangeBuilder.directExchange(request.name());
            case TOPIC -> ExchangeBuilder.topicExchange(request.name());
            case FANOUT -> ExchangeBuilder.fanoutExchange(request.name());
        };
        Exchange exchange = builder.durable(request.durableOrDefault()).build();
        amqpAdmin.declareExchange(exchange);
        log.info("[ADMIN] Exchange creado: {} ({})", exchange.getName(), exchange.getType());
    }

    public void deleteExchange(String name) {
        amqpAdmin.deleteExchange(name);
        log.info("[ADMIN] Exchange eliminado: {}", name);
    }

    // ---------- Bindings ----------

    public void createBinding(BindingRequest request) {
        getQueue(request.queue());
        amqpAdmin.declareBinding(toBinding(request));
        log.info("[ADMIN] Binding creado: {} --({})--> {}", request.exchange(), request.routingKey(), request.queue());
    }

    public void deleteBinding(BindingRequest request) {
        amqpAdmin.removeBinding(toBinding(request));
        log.info("[ADMIN] Binding eliminado: {} --({})--> {}", request.exchange(), request.routingKey(), request.queue());
    }

    private Binding toBinding(BindingRequest request) {
        return new Binding(request.queue(), Binding.DestinationType.QUEUE,
                request.exchange(), request.routingKey(), null);
    }

}
