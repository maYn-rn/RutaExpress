package cl.duoc.rutaexpress.notifications.messaging.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Un mensaje que un consumidor ya proceso con exito. La clave es
 * consumidor + eventId: el mismo evento puede procesarse una vez por cola,
 * pero nunca dos veces en la misma (idempotencia ante reentregas).
 * Tambien sirve de contador de procesados para las metricas de DLQ.
 */
@Entity
@Table(name = "processed_messages")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedMessage {

    @Id
    @Column(length = 120)
    private String id;

    @Column(nullable = false, length = 30)
    private String consumer;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public static String key(String consumer, String eventId) {
        return consumer + ":" + eventId;
    }

    public static ProcessedMessage of(String consumer, String eventId) {
        return new ProcessedMessage(key(consumer, eventId), consumer, eventId, Instant.now());
    }

}
