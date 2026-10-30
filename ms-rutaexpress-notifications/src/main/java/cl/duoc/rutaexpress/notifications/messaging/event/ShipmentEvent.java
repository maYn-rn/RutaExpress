package cl.duoc.rutaexpress.notifications.messaging.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** Datos del envio dentro del EventEnvelope (campo "data"). */
@JsonIgnoreProperties(ignoreUnknown = true)
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
