package cl.duoc.rutaexpress.shipments.messaging.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Envelope comun de todos los mensajes que viajan por RabbitMQ.
 *
 * - type: hecho de negocio que origino el comando (SHIPMENT_CREATED, ...).
 * - eventId: unico por mensaje; el consumidor lo usa para idempotencia.
 * - timestamp: momento en que se publico (UTC).
 * - traceId: comun a todos los mensajes que genera una misma operacion
 *   (ej. crear un envio publica email.send y label.gen con el mismo traceId).
 * - correlationId: agrupa todos los mensajes de un mismo envio ("shipment-<id>").
 *
 * El contrato con los consumidores es este JSON, no la clase Java:
 * ms-rutaexpress-notifications tiene su propia copia.
 */
public record EventEnvelope(
        String type,
        String eventId,
        Instant timestamp,
        String traceId,
        String correlationId,
        ShipmentEvent data
) {

    public static final String SHIPMENT_CREATED = "SHIPMENT_CREATED";
    public static final String SHIPMENT_STATUS_CHANGED = "SHIPMENT_STATUS_CHANGED";

    public static EventEnvelope of(String type, String traceId, ShipmentEvent data) {
        return new EventEnvelope(type, UUID.randomUUID().toString(), Instant.now(), traceId,
                "shipment-" + data.shipmentId(), data);
    }

}
