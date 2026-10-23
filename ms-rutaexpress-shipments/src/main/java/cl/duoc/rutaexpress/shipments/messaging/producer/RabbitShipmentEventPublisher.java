package cl.duoc.rutaexpress.shipments.messaging.producer;

import cl.duoc.rutaexpress.shipments.entity.Shipment;
import cl.duoc.rutaexpress.shipments.entity.ShipmentStatus;
import cl.duoc.rutaexpress.shipments.messaging.ShipmentEventPublisher;
import cl.duoc.rutaexpress.shipments.messaging.config.MessagingProperties;
import cl.duoc.rutaexpress.shipments.messaging.event.ShipmentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

/**
 * PRODUCTOR: publica los eventos de envios en el exchange. No conoce colas ni
 * consumidores, solo el exchange y la routing key.
 *
 * Los eventos se envian despues del commit de la transaccion (para no avisar
 * de un envio que finalmente no se guardo) y un fallo de RabbitMQ solo queda
 * en el log: la creacion o el cambio de estado del envio nunca falla por la
 * mensajeria.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitShipmentEventPublisher implements ShipmentEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties properties;

    @Override
    public void shipmentCreated(Shipment shipment) {
        ShipmentEvent event = toEvent(ShipmentEvent.SHIPMENT_CREATED, shipment, null);
        publishAfterCommit(properties.routingKeys().shipmentCreated(), event);
    }

    @Override
    public void shipmentStatusChanged(Shipment shipment, ShipmentStatus previousStatus) {
        ShipmentEvent event = toEvent(ShipmentEvent.SHIPMENT_STATUS_CHANGED, shipment, previousStatus);
        publishAfterCommit(properties.routingKeys().shipmentStatusChanged(), event);
    }

    private void publishAfterCommit(String routingKey, ShipmentEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(routingKey, event);
                }
            });
        } else {
            send(routingKey, event);
        }
    }

    private void send(String routingKey, ShipmentEvent event) {
        try {
            rabbitTemplate.convertAndSend(properties.exchange(), routingKey, event);
            log.info("[PRODUCER] Evento {} publicado (envio id={}, routingKey={})",
                    event.eventType(), event.shipmentId(), routingKey);
        } catch (AmqpException ex) {
            log.error("[PRODUCER] No se pudo publicar {} del envio id={}: {}",
                    event.eventType(), event.shipmentId(), ex.getMessage());
        }
    }

    private ShipmentEvent toEvent(String type, Shipment shipment, ShipmentStatus previousStatus) {
        return new ShipmentEvent(
                type,
                shipment.getId(),
                shipment.getOrigen(),
                shipment.getDestino(),
                shipment.getPesoKg(),
                shipment.getClienteNombre(),
                previousStatus != null ? previousStatus.name() : null,
                shipment.getEstado().name(),
                LocalDateTime.now()
        );
    }

}
