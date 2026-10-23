package cl.duoc.rutaexpress.notifications.messaging;

import cl.duoc.rutaexpress.notifications.messaging.config.MessagingProperties;
import cl.duoc.rutaexpress.notifications.messaging.event.ShipmentEvent;
import cl.duoc.rutaexpress.notifications.messaging.support.AcknowledgingMessageHandler;
import cl.duoc.rutaexpress.notifications.messaging.support.InvalidMessageException;
import cl.duoc.rutaexpress.notifications.messaging.support.RecoverableProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AcknowledgingMessageHandlerTest {

    private static final long TAG = 42L;
    private static final String QUEUE = "notifications.shipment.queue";
    private static final String VALID_JSON = """
            {"eventType":"SHIPMENT_CREATED","shipmentId":7,"origen":"Santiago","destino":"Valparaiso",
             "pesoKg":12.5,"clienteNombre":"Ana","estado":"CREADO","occurredAt":"2026-10-23T10:15:30"}""";

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final Channel channel = mock(Channel.class);
    private final MessagingProperties properties = new MessagingProperties(
            "rutaexpress.shipments.exchange", "rutaexpress.shipments.dlx", 3, "#fallo",
            new MessagingProperties.Route(QUEUE, "shipment.#", "notifications.shipment.dlq", "notifications.dead"),
            new MessagingProperties.Route("tickets.shipment.queue", "shipment.created", "tickets.shipment.dlq", "tickets.dead"));
    private final AcknowledgingMessageHandler handler =
            new AcknowledgingMessageHandler(objectMapper, rabbitTemplate, properties);

    private Message message(String body, Integer retryCount) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(TAG);
        props.setConsumerQueue(QUEUE);
        if (retryCount != null) {
            props.setHeader(AcknowledgingMessageHandler.RETRY_COUNT_HEADER, retryCount);
        }
        return new Message(body.getBytes(StandardCharsets.UTF_8), props);
    }

    @Test
    void successfulProcessingIsAcked() throws Exception {
        AtomicReference<ShipmentEvent> received = new AtomicReference<>();

        handler.handle(message(VALID_JSON, null), channel, "TEST", received::set);

        assertThat(received.get().shipmentId()).isEqualTo(7L);
        verify(channel).basicAck(TAG, false);
    }

    @Test
    void malformedJsonIsRejectedWithoutRequeueSoItGoesToDlq() throws Exception {
        handler.handle(message("esto no es json", null), channel, "TEST", e -> { });

        verify(channel).basicReject(TAG, false);
        verify(channel, never()).basicAck(anyLong(), any(Boolean.class));
    }

    @Test
    void missingRequiredFieldsIsRejected() throws Exception {
        handler.handle(message("{\"eventType\":\"SHIPMENT_CREATED\"}", null), channel, "TEST", e -> { });

        verify(channel).basicReject(TAG, false);
    }

    @Test
    void invalidDataDetectedByBusinessLogicIsRejected() throws Exception {
        handler.handle(message(VALID_JSON, null), channel, "TEST", e -> {
            throw new InvalidMessageException("datos invalidos");
        });

        verify(channel).basicReject(TAG, false);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void recoverableErrorIsRetriedWithIncrementedCounter() throws Exception {
        handler.handle(message(VALID_JSON, 1), channel, "TEST", e -> {
            throw new RecoverableProcessingException("proveedor caido");
        });

        ArgumentCaptor<Message> retried = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(""), eq(QUEUE), retried.capture());
        assertThat((Integer) retried.getValue().getMessageProperties()
                .getHeader(AcknowledgingMessageHandler.RETRY_COUNT_HEADER)).isEqualTo(2);
        verify(channel).basicAck(TAG, false);
    }

    @Test
    void recoverableErrorAfterMaxRetriesIsNackedToDlq() throws Exception {
        handler.handle(message(VALID_JSON, 3), channel, "TEST", e -> {
            throw new RecoverableProcessingException("proveedor caido");
        });

        verify(channel).basicNack(TAG, false, false);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void unexpectedErrorIsNackedToDlq() throws Exception {
        handler.handle(message(VALID_JSON, null), channel, "TEST", e -> {
            throw new IllegalStateException("bug");
        });

        verify(channel).basicNack(TAG, false, false);
    }

}
