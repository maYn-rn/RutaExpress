package cl.duoc.rutaexpress.notifications.notification.entity;

public enum NotificationStatus {

    /** El email se envio correctamente. */
    ENVIADA,
    /** El email no se pudo enviar tras los reintentos (mensaje en la DLQ). */
    FALLIDA

}
