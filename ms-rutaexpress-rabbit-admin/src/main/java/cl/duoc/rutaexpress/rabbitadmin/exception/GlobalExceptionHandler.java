package cl.duoc.rutaexpress.rabbitadmin.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Traduce los errores a respuestas JSON claras:
 * 400 datos invalidos, 404 recurso inexistente, 409 conflicto con lo que ya
 * existe en el broker y 503 si RabbitMQ no responde.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidBody(MethodArgumentNotValidException ex,
                                                                 HttpServletRequest request) {
        List<String> errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Datos invalidos", errors, request);
    }

    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class})
    public ResponseEntity<Map<String, Object>> handleInvalidPath(Exception ex, HttpServletRequest request) {
        List<String> errors = ex instanceof ConstraintViolationException cve
                ? cve.getConstraintViolations().stream().map(v -> v.getMessage()).toList()
                : ((HandlerMethodValidationException) ex).getAllErrors().stream()
                        .map(error -> error.getDefaultMessage()).toList();
        return build(HttpStatus.BAD_REQUEST, "Parametros invalidos", errors, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex,
                                                                HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "JSON invalido o tipo de exchange no soportado (use direct, topic o fanout)",
                List.of(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), List.of(), request);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ResourceConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), List.of(), request);
    }

    @ExceptionHandler(AmqpConnectException.class)
    public ResponseEntity<Map<String, Object>> handleBrokerDown(AmqpConnectException ex, HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "RabbitMQ no esta disponible", List.of(), request);
    }

    /** Por ejemplo, redeclarar un exchange con otro tipo o enlazar a un exchange inexistente. */
    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<Map<String, Object>> handleBrokerRejected(AmqpException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT,
                "RabbitMQ rechazo la operacion (el recurso no existe o ya existe con otra configuracion)",
                List.of(), request);
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message, List<String> errors,
                                                      HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        if (!errors.isEmpty()) {
            body.put("errors", errors);
        }
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

}
