package cl.duoc.rutaexpress.notifications.messaging.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento publicado por ms-rutaexpress-shipments. Es una copia del contrato
 * JSON a proposito: cada microservicio es independiente.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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
