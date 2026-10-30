package cl.duoc.rutaexpress.notifications.notification;

import cl.duoc.rutaexpress.notifications.messaging.config.MessagingProperties;
import cl.duoc.rutaexpress.notifications.messaging.event.EventEnvelope;
import cl.duoc.rutaexpress.notifications.messaging.event.ShipmentEvent;
import cl.duoc.rutaexpress.notifications.messaging.support.InvalidMessageException;
import cl.duoc.rutaexpress.notifications.messaging.support.RecoverableProcessingException;
import cl.duoc.rutaexpress.notifications.notification.entity.Notification;
import cl.duoc.rutaexpress.notifications.notification.entity.NotificationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Logica de negocio: arma el aviso al cliente, lo envia por email y lo guarda
 * para que el cliente lo vea en la app. No sabe nada de RabbitMQ; solo recibe
 * el envelope ya deserializado.
 *
 * Si el nombre del cliente contiene app.messaging.simulated-failure-keyword,
 * se simula una caida del proveedor de correo para demostrar reintentos, DLQ
 * y la notificacion FALLIDA.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int MAX_ERROR_LENGTH = 500;

    private final MessagingProperties properties;
    private final EmailSender emailSender;
    private final NotificationRepository repository;

    public Notification notifyCustomer(EventEnvelope envelope) {
        ShipmentEvent data = envelope.data();
        if (data.clienteEmail() == null || data.clienteEmail().isBlank()) {
            throw new InvalidMessageException("El envio #" + data.shipmentId() + " no tiene email de cliente");
        }

        Content content = buildContent(envelope);

        if (data.clienteNombre() != null && data.clienteNombre().contains(properties.simulatedFailureKeyword())) {
            throw new RecoverableProcessingException("Proveedor de email no disponible (simulado)");
        }
        emailSender.send(data.clienteEmail(), content.subject(), content.body());

        Notification saved = repository.save(toNotification(envelope, content, NotificationStatus.ENVIADA, null));
        log.info("[NOTIFICACION] ENVIADA envio #{} a {} ({})", data.shipmentId(), data.clienteEmail(), data.estado());
        return saved;
    }

    /**
     * El email no se pudo enviar y el mensaje termino en la DLQ: se guarda
     * igual el aviso como FALLIDA para que el cliente vea el cambio en la app
     * y el operador pueda medir los fallos.
     */
    public void recordFailure(EventEnvelope envelope, String reason) {
        if (repository.existsByEventId(envelope.eventId())) {
            return;
        }
        String detail = reason != null && reason.length() > MAX_ERROR_LENGTH
                ? reason.substring(0, MAX_ERROR_LENGTH) : reason;
        repository.save(toNotification(envelope, buildContent(envelope), NotificationStatus.FALLIDA, detail));
        log.warn("[NOTIFICACION] FALLIDA envio #{} ({}): {}", envelope.data().shipmentId(), envelope.data().estado(), detail);
    }

    Content buildContent(EventEnvelope envelope) {
        ShipmentEvent data = envelope.data();
        String cliente = data.clienteNombre() != null ? data.clienteNombre() : "cliente";
        long id = data.shipmentId();

        if (EventEnvelope.SHIPMENT_CREATED.equals(envelope.type())) {
            return new Content("Envio #%d registrado".formatted(id),
                    "Hola %s, registramos tu envio #%d de %s a %s. Te avisaremos cada cambio de estado."
                            .formatted(cliente, id, data.origen(), data.destino()));
        }

        String motivo = data.motivo() != null && !data.motivo().isBlank() ? " Motivo: " + data.motivo() + "." : "";
        String estado = data.estado() != null ? data.estado() : "";
        return switch (estado) {
            case "ACEPTADO" -> new Content("Envio #%d aceptado".formatted(id),
                    "Hola %s, tu envio #%d fue aceptado y se esta preparando en bodega.".formatted(cliente, id));
            case "EN_TRANSITO" -> new Content("Envio #%d en ruta".formatted(id),
                    "Hola %s, tu envio #%d va en ruta hacia %s.".formatted(cliente, id, data.destino()));
            case "ENTREGADO" -> new Content("Envio #%d entregado".formatted(id),
                    "Hola %s, tu envio #%d fue entregado en %s. Gracias por usar RutaExpress."
                            .formatted(cliente, id, data.destino()));
            case "FALLIDO" -> new Content("No pudimos entregar tu envio #%d".formatted(id),
                    "Hola %s, no pudimos entregar tu envio #%d.%s Lo intentaremos nuevamente o te contactaremos."
                            .formatted(cliente, id, motivo));
            case "CANCELADO" -> new Content("Envio #%d cancelado".formatted(id),
                    "Hola %s, tu envio #%d fue cancelado.%s".formatted(cliente, id, motivo));
            default -> new Content("Novedades de tu envio #%d".formatted(id),
                    "Hola %s, tu envio #%d cambio de %s a %s.".formatted(cliente, id, data.estadoAnterior(), estado));
        };
    }

    private Notification toNotification(EventEnvelope envelope, Content content, NotificationStatus status,
                                        String detail) {
        ShipmentEvent data = envelope.data();
        return Notification.builder()
                .eventId(envelope.eventId())
                .traceId(envelope.traceId())
                .shipmentId(data.shipmentId())
                .clienteEmail(data.clienteEmail() != null ? data.clienteEmail().toLowerCase() : null)
                .clienteNombre(data.clienteNombre())
                .estadoEnvio(data.estado())
                .asunto(content.subject())
                .mensaje(content.body())
                .estado(status)
                .detalleError(detail)
                .leida(false)
                .creadaEn(Instant.now())
                .build();
    }

    record Content(String subject, String body) {
    }

}
