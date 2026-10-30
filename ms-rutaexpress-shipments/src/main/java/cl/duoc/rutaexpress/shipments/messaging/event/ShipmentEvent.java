package cl.duoc.rutaexpress.shipments.messaging.event;

import java.math.BigDecimal;

/**
 * Datos del envio que viajan dentro del EventEnvelope (campo "data").
 * estadoAnterior y motivo son null cuando el envio recien se crea.
 */
public record ShipmentEvent(
        Long shipmentId,
        String origen,
        String destino,
        BigDecimal pesoKg,
        String clienteNombre,
        String clienteEmail,
        String estadoAnterior,
        String estado,
        String motivo
) {
}
