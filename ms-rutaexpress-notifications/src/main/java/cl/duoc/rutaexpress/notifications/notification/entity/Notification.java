package cl.duoc.rutaexpress.notifications.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Aviso al cliente sobre su envio. Se guarda siempre, tanto si el email salio
 * (ENVIADA) como si se agotaron los reintentos y el mensaje termino en la DLQ
 * (FALLIDA): asi el cliente ve el cambio de estado en la app aunque el
 * correo no haya llegado.
 */
@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notifications_cliente", columnList = "cliente_email"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** eventId del mensaje que la origino: una notificacion por evento. */
    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    private String eventId;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "shipment_id", nullable = false)
    private Long shipmentId;

    @Column(name = "cliente_email")
    private String clienteEmail;

    @Column(name = "cliente_nombre")
    private String clienteNombre;

    /** Estado del envio que se notifica (CREADO, ENTREGADO, FALLIDO...). */
    @Column(name = "estado_envio", length = 30)
    private String estadoEnvio;

    @Column(nullable = false)
    private String asunto;

    @Column(nullable = false, length = 1000)
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus estado;

    /** Por que no se pudo enviar el email (solo FALLIDA). */
    @Column(name = "detalle_error", length = 500)
    private String detalleError;

    @Column(nullable = false)
    private boolean leida;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

}
