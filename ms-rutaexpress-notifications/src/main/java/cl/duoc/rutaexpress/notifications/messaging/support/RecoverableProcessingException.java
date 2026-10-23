package cl.duoc.rutaexpress.notifications.messaging.support;

/**
 * Error recuperable: un fallo temporal (por ejemplo, el proveedor de correo no
 * responde). Vale la pena reintentar; si se agotan los intentos, va a la DLQ.
 */
public class RecoverableProcessingException extends RuntimeException {

    public RecoverableProcessingException(String message) {
        super(message);
    }

}
