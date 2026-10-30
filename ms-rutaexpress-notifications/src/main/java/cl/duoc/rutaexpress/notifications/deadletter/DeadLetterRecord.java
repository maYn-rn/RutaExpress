package cl.duoc.rutaexpress.notifications.deadletter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Registro de un mensaje que llego a una DLQ. Permite calcular la tasa de DLQ
 * por cola y revisar despues por que fallo cada mensaje.
 */
@Entity
@Table(name = "dead_letters")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Consumidor/flujo de origen: EMAIL, WAREHOUSE o LABEL. */
    @Column(nullable = false, length = 30)
    private String consumer;

    @Column(name = "dead_letter_queue", nullable = false, length = 100)
    private String deadLetterQueue;

    @Column(name = "event_id", length = 64)
    private String eventId;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    private int retries;

    @Column(length = 2000)
    private String body;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

}
