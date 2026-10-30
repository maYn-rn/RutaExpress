package cl.duoc.rutaexpress.shipments.messaging;

import cl.duoc.rutaexpress.shipments.entity.Shipment;
import cl.duoc.rutaexpress.shipments.entity.ShipmentStatus;
import cl.duoc.rutaexpress.shipments.messaging.config.MessagingProperties;
import cl.duoc.rutaexpress.shipments.messaging.event.EventEnvelope;
import cl.duoc.rutaexpress.shipments.messaging.producer.RabbitShipmentEventPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RabbitShipmentEventPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final MessagingProperties properties = new MessagingProperties(
            new MessagingProperties.Exchanges("cmd.direct", "cmd.topic"),
            new MessagingProperties.RoutingKeys("email.send", "email", "warehouse.ticket", "label.gen"));
    private final RabbitShipmentEventPublisher publisher = new RabbitShipmentEventPublisher(rabbitTemplate, properties);

    private Shipment shipment(ShipmentStatus status) {
        return Shipment.builder()
                .id(7L).origen("Santiago").destino("Valparaiso")
                .pesoKg(new BigDecimal("12.5")).clienteNombre("Juan Perez")
                .clienteEmail("juan@rutaexpress.cl")
                .estado(status).build();
    }

    private EventEnvelope captureSent(String exchange, String routingKey) {
        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq(exchange), eq(routingKey), body.capture(), any(MessagePostProcessor.class));
        return (EventEnvelope) body.getValue();
    }

    @Test
    void shipmentCreatedSendsEmailAndLabelCommandsWithSameTrace() {
        publisher.shipmentCreated(shipment(ShipmentStatus.CREADO));

        EventEnvelope email = captureSent("cmd.direct", "email.send");
        EventEnvelope label = captureSent("cmd.direct", "label.gen");

        assertThat(email.type()).isEqualTo(EventEnvelope.SHIPMENT_CREATED);
        assertThat(email.data().clienteEmail()).isEqualTo("juan@rutaexpress.cl");
        assertThat(email.correlationId()).isEqualTo("shipment-7");
        assertThat(email.traceId()).isEqualTo(label.traceId());
        assertThat(email.eventId()).isNotEqualTo(label.eventId());
    }

    @Test
    void statusChangeGoesToTopicWithStatusInRoutingKey() {
        publisher.shipmentStatusChanged(shipment(ShipmentStatus.FALLIDO), ShipmentStatus.EN_TRANSITO, "Destinatario ausente");

        EventEnvelope email = captureSent("cmd.topic", "email.fallido");
        assertThat(email.type()).isEqualTo(EventEnvelope.SHIPMENT_STATUS_CHANGED);
        assertThat(email.data().estadoAnterior()).isEqualTo("EN_TRANSITO");
        assertThat(email.data().motivo()).isEqualTo("Destinatario ausente");
        verify(rabbitTemplate, never()).convertAndSend(eq("cmd.direct"), eq("warehouse.ticket"), any(Object.class),
                any(MessagePostProcessor.class));
    }

    @Test
    void acceptedShipmentAlsoRequestsWarehouseTicket() {
        publisher.shipmentStatusChanged(shipment(ShipmentStatus.ACEPTADO), ShipmentStatus.CREADO, null);

        captureSent("cmd.topic", "email.aceptado");
        captureSent("cmd.direct", "warehouse.ticket");
        verify(rabbitTemplate, times(2)).convertAndSend(anyString(), anyString(), any(Object.class),
                any(MessagePostProcessor.class));
    }

    @Test
    void envelopeFieldsAreCopiedToAmqpProperties() {
        publisher.shipmentCreated(shipment(ShipmentStatus.CREADO));

        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<MessagePostProcessor> processor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(eq("cmd.direct"), eq("email.send"), body.capture(), processor.capture());
        EventEnvelope envelope = (EventEnvelope) body.getValue();

        Message processed = processor.getValue().postProcessMessage(new Message(new byte[0], new MessageProperties()));
        MessageProperties props = processed.getMessageProperties();
        assertThat(props.getMessageId()).isEqualTo(envelope.eventId());
        assertThat(props.getCorrelationId()).isEqualTo("shipment-7");
        assertThat(props.getType()).isEqualTo(EventEnvelope.SHIPMENT_CREATED);
        assertThat((String) props.getHeader(RabbitShipmentEventPublisher.TRACE_ID_HEADER)).isEqualTo(envelope.traceId());
    }

    @Test
    void brokerFailureDoesNotPropagateToBusinessLogic() {
        doThrow(new AmqpConnectException(new RuntimeException("broker caido")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class));

        assertThatCode(() -> publisher.shipmentCreated(shipment(ShipmentStatus.CREADO)))
                .doesNotThrowAnyException();
    }

}
