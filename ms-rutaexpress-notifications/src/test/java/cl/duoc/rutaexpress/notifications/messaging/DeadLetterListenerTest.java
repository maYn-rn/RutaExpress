package cl.duoc.rutaexpress.notifications.messaging;

import cl.duoc.rutaexpress.notifications.deadletter.DeadLetterRecord;
import cl.duoc.rutaexpress.notifications.deadletter.DeadLetterRepository;
import cl.duoc.rutaexpress.notifications.messaging.config.MessagingProperties;
import cl.duoc.rutaexpress.notifications.messaging.consumer.deadletter.DeadLetterListener;
import cl.duoc.rutaexpress.notifications.messaging.event.EventEnvelope;
import cl.duoc.rutaexpress.notifications.messaging.support.AcknowledgingMessageHandler;
import cl.duoc.rutaexpress.notifications.notification.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DeadLetterListenerTest {

    private static final long TAG = 9L;
    private static final String ENVELOPE = """
            {"type":"SHIPMENT_STATUS_CHANGED","eventId":"evt-9","timestamp":"2026-10-23T10:15:30Z",
             "traceId":"trace-9","correlationId":"shipment-3",
             "data":{"shipmentId":3,"clienteNombre":"Ana #fallo","clienteEmail":"ana@rutaexpress.cl",
                     "estadoAnterior":"ACEPTADO","estado":"EN_TRANSITO"}}""";

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final DeadLetterRepository deadLetters = mock(DeadLetterRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final Channel channel = mock(Channel.class);
    private final MessagingProperties properties = new MessagingProperties(
            new MessagingProperties.Exchanges("cmd.direct", "cmd.topic", "cmd.dead.dlx"), 3, "#fallo",
            new MessagingProperties.Route("q.cmd.email", "email.send", "email.*", "q.cmd.email.dlq", "email.dlq"),
            new MessagingProperties.Route("q.cmd.warehouse", "warehouse.ticket", "warehouse.#", "q.cmd.warehouse.dlq", "warehouse.dlq"),
            new MessagingProperties.Route("q.cmd.label", "label.gen", "label.*", "q.cmd.label.dlq", "label.dlq"));
    private final DeadLetterListener listener =
            new DeadLetterListener(properties, deadLetters, notificationService, objectMapper);

    private Message deadMessage(String queue, String body, String lastError) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(TAG);
        props.setConsumerQueue(queue);
        props.setHeader(AcknowledgingMessageHandler.RETRY_COUNT_HEADER, 3);
        if (lastError != null) {
            props.setHeader(AcknowledgingMessageHandler.LAST_ERROR_HEADER, lastError);
        }
        return new Message(body.getBytes(StandardCharsets.UTF_8), props);
    }

    @Test
    void emailDeadLetterIsRecordedAndMarksNotificationAsFailed() throws Exception {
        listener.onDeadLetter(deadMessage("q.cmd.email.dlq", ENVELOPE, "Proveedor caido"), channel);

        ArgumentCaptor<DeadLetterRecord> record = ArgumentCaptor.forClass(DeadLetterRecord.class);
        verify(deadLetters).save(record.capture());
        assertThat(record.getValue().getConsumer()).isEqualTo("EMAIL");
        assertThat(record.getValue().getEventId()).isEqualTo("evt-9");
        assertThat(record.getValue().getRetries()).isEqualTo(3);

        ArgumentCaptor<EventEnvelope> envelope = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(notificationService).recordFailure(envelope.capture(), eq("Proveedor caido"));
        assertThat(envelope.getValue().data().shipmentId()).isEqualTo(3L);
        verify(channel).basicAck(TAG, false);
    }

    @Test
    void warehouseDeadLetterDoesNotCreateCustomerNotification() throws Exception {
        listener.onDeadLetter(deadMessage("q.cmd.warehouse.dlq", ENVELOPE, null), channel);

        ArgumentCaptor<DeadLetterRecord> record = ArgumentCaptor.forClass(DeadLetterRecord.class);
        verify(deadLetters).save(record.capture());
        assertThat(record.getValue().getConsumer()).isEqualTo("WAREHOUSE");
        verify(notificationService, never()).recordFailure(any(), any());
        verify(channel).basicAck(TAG, false);
    }

    @Test
    void unparsableBodyIsStillRecordedAndAcked() throws Exception {
        listener.onDeadLetter(deadMessage("q.cmd.email.dlq", "esto no es json", null), channel);

        verify(deadLetters).save(any(DeadLetterRecord.class));
        verify(notificationService, never()).recordFailure(any(), any());
        verify(channel).basicAck(TAG, false);
    }

    @Test
    void databaseFailureStillAcksTheMessage() throws Exception {
        doThrow(new IllegalStateException("db caida")).when(deadLetters).save(any());

        listener.onDeadLetter(deadMessage("q.cmd.email.dlq", ENVELOPE, "x"), channel);

        verify(channel).basicAck(TAG, false);
    }

}
