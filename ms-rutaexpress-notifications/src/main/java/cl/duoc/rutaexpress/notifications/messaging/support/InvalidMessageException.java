package cl.duoc.rutaexpress.notifications.messaging.support;

/**
 * Error NO recuperable: el mensaje esta mal formado o le faltan datos.
 * Reintentarlo daria siempre el mismo resultado, asi que va directo a la DLQ.
 */
public class InvalidMessageException extends RuntimeException {

    public InvalidMessageException(String message) {
        super(message);
    }

    public InvalidMessageException(String message, Throwable cause) {
        super(message, cause);
    }

}
