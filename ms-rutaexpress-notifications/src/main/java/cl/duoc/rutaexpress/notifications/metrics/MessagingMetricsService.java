package cl.duoc.rutaexpress.notifications.metrics;

import cl.duoc.rutaexpress.notifications.deadletter.DeadLetterRepository;
import cl.duoc.rutaexpress.notifications.messaging.config.MessagingProperties;
import cl.duoc.rutaexpress.notifications.messaging.consumer.email.EmailCommandListener;
import cl.duoc.rutaexpress.notifications.messaging.consumer.label.LabelCommandListener;
import cl.duoc.rutaexpress.notifications.messaging.consumer.warehouse.WarehouseCommandListener;
import cl.duoc.rutaexpress.notifications.messaging.idempotency.ProcessedMessageRepository;
import cl.duoc.rutaexpress.notifications.notification.NotificationRepository;
import cl.duoc.rutaexpress.notifications.notification.entity.NotificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Tasa de DLQ por flujo: mensajes muertos / (procesados + muertos).
 * Los procesados salen de processed_messages y los muertos de dead_letters.
 */
@Service
@RequiredArgsConstructor
public class MessagingMetricsService {

    private final MessagingProperties properties;
    private final ProcessedMessageRepository processedMessages;
    private final DeadLetterRepository deadLetters;
    private final NotificationRepository notifications;

    @Transactional(readOnly = true)
    public MessagingMetrics metrics() {
        List<QueueMetrics> queues = List.of(
                queue(EmailCommandListener.CONSUMER, properties.email()),
                queue(WarehouseCommandListener.CONSUMER, properties.warehouse()),
                queue(LabelCommandListener.CONSUMER, properties.label()));

        return new MessagingMetrics(queues,
                notifications.countByEstado(NotificationStatus.ENVIADA),
                notifications.countByEstado(NotificationStatus.FALLIDA));
    }

    private QueueMetrics queue(String consumer, MessagingProperties.Route route) {
        long processed = processedMessages.countByConsumer(consumer);
        long dead = deadLetters.countByConsumer(consumer);
        long total = processed + dead;
        BigDecimal rate = total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(dead * 100L).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        return new QueueMetrics(route.queue(), route.deadLetterQueue(), processed, dead, rate);
    }

    public record QueueMetrics(String cola, String dlq, long procesados, long enDlq, BigDecimal tasaDlqPorcentaje) {
    }

    public record MessagingMetrics(List<QueueMetrics> colas, long notificacionesEnviadas, long notificacionesFallidas) {
    }

}
