package cl.duoc.rutaexpress.notifications.notification;

/**
 * Canal de salida del email. NotificationService no sabe como se envia:
 * hoy LogEmailSender lo escribe en el log; un envio SMTP real solo requiere
 * otra implementacion.
 */
public interface EmailSender {

    /**
     * @throws cl.duoc.rutaexpress.notifications.messaging.support.RecoverableProcessingException
     *         si el proveedor no esta disponible (se reintenta y luego va a la DLQ)
     */
    void send(String to, String subject, String body);

}
