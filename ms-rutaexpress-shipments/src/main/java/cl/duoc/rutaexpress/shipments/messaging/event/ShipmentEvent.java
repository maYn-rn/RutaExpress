package cl.duoc.rutaexpress.shipments.messaging.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mensaje que viaja por RabbitMQ. El contrato con los consumidores es este
 * JSON, no la clase Java: ms-rutaexpress-notifications tiene su propia copia.
 */
public record ShipmentEvent(
        String eventType,
        Long shipmentId,
        String origen,
        String destino,
        BigDecimal pesoKg,
        String clienteNombre,
        String estadoAnterior,
        String estado,
        LocalDateTime occurredAt
) {

    public static final String SHIPMENT_CREATED = "SHIPMENT_CREATED";
    public static final String SHIPMENT_STATUS_CHANGED = "SHIPMENT_STATUS_CHANGED";

}
