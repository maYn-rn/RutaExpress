package cl.duoc.rutaexpress.notifications.notification;

import cl.duoc.rutaexpress.notifications.messaging.config.MessagingProperties;
import cl.duoc.rutaexpress.notifications.messaging.event.ShipmentEvent;
import cl.duoc.rutaexpress.notifications.messaging.support.RecoverableProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Logica de negocio: arma y "envia" la notificacion al cliente. No sabe nada
 * de RabbitMQ; solo recibe el evento ya deserializado.
 *
 * El envio real (correo, SMS) se simula con un log. Si el nombre del cliente
 * contiene app.messaging.simulated-failure-keyword, se simula una caida del
 * proveedor para demostrar reintentos y DLQ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final MessagingProperties properties;

    public String notifyCustomer(ShipmentEvent event) {
        String cliente = event.clienteNombre() != null ? event.clienteNombre() : "cliente";

        if (cliente.contains(properties.simulatedFailureKeyword())) {
            throw new RecoverableProcessingException("Proveedor de notificaciones no disponible (simulado)");
        }

        String text = switch (event.eventType()) {
            case ShipmentEvent.SHIPMENT_CREATED -> "Hola %s, registramos tu envio #%d de %s a %s."
                    .formatted(cliente, event.shipmentId(), event.origen(), event.destino());
            case ShipmentEvent.SHIPMENT_STATUS_CHANGED -> "Hola %s, tu envio #%d cambio de %s a %s."
                    .formatted(cliente, event.shipmentId(), event.estadoAnterior(), event.estado());
            default -> "Hola %s, hay novedades en tu envio #%d."
                    .formatted(cliente, event.shipmentId());
        };

        log.info("[NOTIFICACION] Enviada -> {}", text);
        return text;
    }

}
