package cl.duoc.rutaexpress.shipments.messaging.producer;

import cl.duoc.rutaexpress.shipments.entity.Shipment;
import cl.duoc.rutaexpress.shipments.entity.ShipmentStatus;
import cl.duoc.rutaexpress.shipments.messaging.ShipmentEventPublisher;
import cl.duoc.rutaexpress.shipments.messaging.config.MessagingProperties;
import cl.duoc.rutaexpress.shipments.messaging.event.EventEnvelope;
import cl.duoc.rutaexpress.shipments.messaging.event.ShipmentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * PRODUCTOR: traduce los hechos de negocio a comandos en RabbitMQ. No conoce
 * colas ni consumidores, solo exchanges y routing keys.
 *
 * | Hecho                 | Exchange   | Routing key       | Cola destino    |
 * |-----------------------|------------|-------------------|-----------------|
 * | Envio creado          | cmd.direct | email.send        | q.cmd.email     |
 * | Envio creado          | cmd.direct | label.gen         | q.cmd.label     |
 * | Cambio de estado      | cmd.topic  | email.<estado>    | q.cmd.email     |
 * | Cambio a ACEPTADO     | cmd.direct | warehouse.ticket  | q.cmd.warehouse |
 *
 * Los mensajes se envian despues del commit de la transaccion (para no avisar
 * de un envio que finalmente no se guardo) y un fallo de RabbitMQ solo queda
 * en el log: la creacion o el cambio de estado nunca falla por la mensajeria.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitShipmentEventPublisher implements ShipmentEventPublisher {

    public static final String TRACE_ID_HEADER = "x-trace-id";

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties properties;

    @Override
    public void shipmentCreated(Shipment shipment) {
        ShipmentEvent data = toEvent(shipment, null, null);
        String traceId = UUID.randomUUID().toString();
        MessagingProperties.RoutingKeys keys = properties.routingKeys();

        publishAfterCommit(List.of(
                command(properties.exchanges().direct(), keys.emailSend(), EventEnvelope.SHIPMENT_CREATED, traceId, data),
                command(properties.exchanges().direct(), keys.labelGen(), EventEnvelope.SHIPMENT_CREATED, traceId, data)
        ));
    }

    @Override
    public void shipmentStatusChanged(Shipment shipment, ShipmentStatus previousStatus, String motivo) {
        ShipmentEvent data = toEvent(shipment, previousStatus, motivo);
        String traceId = UUID.randomUUID().toString();
        MessagingProperties.RoutingKeys keys = properties.routingKeys();
        String emailKey = keys.emailStatusPrefix() + "." + shipment.getEstado().name().toLowerCase(Locale.ROOT);

        List<Command> commands = new ArrayList<>();
        commands.add(command(properties.exchanges().topic(), emailKey, EventEnvelope.SHIPMENT_STATUS_CHANGED, traceId, data));
        if (shipment.getEstado() == ShipmentStatus.ACEPTADO) {
            commands.add(command(properties.exchanges().direct(), keys.warehouseTicket(),
                    EventEnvelope.SHIPMENT_STATUS_CHANGED, traceId, data));
        }
        publishAfterCommit(commands);
    }

    private Command command(String exchange, String routingKey, String type, String traceId, ShipmentEvent data) {
        return new Command(exchange, routingKey, EventEnvelope.of(type, traceId, data));
    }

    private void publishAfterCommit(List<Command> commands) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    commands.forEach(RabbitShipmentEventPublisher.this::send);
                }
            });
        } else {
            commands.forEach(this::send);
        }
    }

    private void send(Command command) {
        EventEnvelope envelope = command.envelope();
        try {
            // Los campos del envelope tambien van como propiedades AMQP para
            // verlos en el panel de RabbitMQ sin abrir el cuerpo.
            rabbitTemplate.convertAndSend(command.exchange(), command.routingKey(), envelope, message -> {
                MessageProperties props = message.getMessageProperties();
                props.setMessageId(envelope.eventId());
                props.setCorrelationId(envelope.correlationId());
                props.setType(envelope.type());
                props.setHeader(TRACE_ID_HEADER, envelope.traceId());
                return message;
            });
            log.info("[PRODUCER] {} publicado | envio id={} | {} -> {} | eventId={} | traceId={}",
                    envelope.type(), envelope.data().shipmentId(), command.exchange(), command.routingKey(),
                    envelope.eventId(), envelope.traceId());
        } catch (AmqpException ex) {
            log.error("[PRODUCER] No se pudo publicar {} del envio id={} ({}): {}",
                    envelope.type(), envelope.data().shipmentId(), command.routingKey(), ex.getMessage());
        }
    }

    private ShipmentEvent toEvent(Shipment shipment, ShipmentStatus previousStatus, String motivo) {
        return new ShipmentEvent(
                shipment.getId(),
                shipment.getOrigen(),
                shipment.getDestino(),
                shipment.getPesoKg(),
                shipment.getClienteNombre(),
                shipment.getClienteEmail(),
                previousStatus != null ? previousStatus.name() : null,
                shipment.getEstado().name(),
                motivo
        );
    }

    private record Command(String exchange, String routingKey, EventEnvelope envelope) {
    }

}
