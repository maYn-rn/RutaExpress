package cl.duoc.rutaexpress.notifications.messaging.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * Envelope comun publicado por ms-rutaexpress-shipments. Es una copia del
 * contrato JSON a proposito: cada microservicio es independiente.
 *
 * eventId identifica cada mensaje (idempotencia), traceId agrupa los mensajes
 * de una misma operacion y correlationId los de un mismo envio.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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

}
