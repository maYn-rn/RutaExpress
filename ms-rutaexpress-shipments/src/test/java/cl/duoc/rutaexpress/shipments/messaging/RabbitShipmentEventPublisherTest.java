package cl.duoc.rutaexpress.shipments.messaging;

import cl.duoc.rutaexpress.shipments.entity.Shipment;
import cl.duoc.rutaexpress.shipments.entity.ShipmentStatus;
import cl.duoc.rutaexpress.shipments.messaging.config.MessagingProperties;
import cl.duoc.rutaexpress.shipments.messaging.event.ShipmentEvent;
import cl.duoc.rutaexpress.shipments.messaging.producer.RabbitShipmentEventPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitShipmentEventPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final MessagingProperties properties = new MessagingProperties(
            "rutaexpress.shipments.exchange",
            new MessagingProperties.RoutingKeys("shipment.created", "shipment.status.changed"));
    private final RabbitShipmentEventPublisher publisher = new RabbitShipmentEventPublisher(rabbitTemplate, properties);

    private Shipment shipment(ShipmentStatus status) {
        return Shipment.builder()
                .id(7L).origen("Santiago").destino("Valparaiso")
                .pesoKg(new BigDecimal("12.5")).clienteNombre("Juan Perez")
                .estado(status).build();
    }

    @Test
    void publishesShipmentCreatedToConfiguredExchangeAndRoutingKey() {
        publisher.shipmentCreated(shipment(ShipmentStatus.CREADO));

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("rutaexpress.shipments.exchange"), eq("shipment.created"), event.capture());
        ShipmentEvent sent = (ShipmentEvent) event.getValue();
        assertThat(sent.eventType()).isEqualTo(ShipmentEvent.SHIPMENT_CREATED);
        assertThat(sent.shipmentId()).isEqualTo(7L);
        assertThat(sent.estado()).isEqualTo("CREADO");
    }

    @Test
    void publishesStatusChangeWithPreviousStatus() {
        publisher.shipmentStatusChanged(shipment(ShipmentStatus.ACEPTADO), ShipmentStatus.CREADO);

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("rutaexpress.shipments.exchange"), eq("shipment.status.changed"), event.capture());
        ShipmentEvent sent = (ShipmentEvent) event.getValue();
        assertThat(sent.estadoAnterior()).isEqualTo("CREADO");
        assertThat(sent.estado()).isEqualTo("ACEPTADO");
    }

    @Test
    void brokerFailureDoesNotPropagateToBusinessLogic() {
        doThrow(new AmqpConnectException(new RuntimeException("broker caido")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        assertThatCode(() -> publisher.shipmentCreated(shipment(ShipmentStatus.CREADO)))
                .doesNotThrowAnyException();
    }

}
