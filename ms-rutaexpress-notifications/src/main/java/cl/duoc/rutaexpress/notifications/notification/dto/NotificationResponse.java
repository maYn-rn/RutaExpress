package cl.duoc.rutaexpress.notifications.notification.dto;

import cl.duoc.rutaexpress.notifications.notification.entity.Notification;
import cl.duoc.rutaexpress.notifications.notification.entity.NotificationStatus;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        Long shipmentId,
        String estadoEnvio,
        String asunto,
        String mensaje,
        NotificationStatus estado,
        String detalleError,
        boolean leida,
        Instant creadaEn
) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getShipmentId(), n.getEstadoEnvio(), n.getAsunto(),
                n.getMensaje(), n.getEstado(), n.getDetalleError(), n.isLeida(), n.getCreadaEn());
    }

}
