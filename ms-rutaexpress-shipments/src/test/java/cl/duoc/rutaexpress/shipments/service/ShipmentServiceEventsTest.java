package cl.duoc.rutaexpress.shipments.service;

import cl.duoc.rutaexpress.shipments.dto.ShipmentRequestDTO;
import cl.duoc.rutaexpress.shipments.dto.ShipmentStatusUpdateDTO;
import cl.duoc.rutaexpress.shipments.entity.Shipment;
import cl.duoc.rutaexpress.shipments.entity.ShipmentStatus;
import cl.duoc.rutaexpress.shipments.exception.InvalidStatusTransitionException;
import cl.duoc.rutaexpress.shipments.messaging.ShipmentEventPublisher;
import cl.duoc.rutaexpress.shipments.repository.ShipmentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * La logica de negocio solo avisa al ShipmentEventPublisher; no conoce RabbitMQ.
 */
class ShipmentServiceEventsTest {

    private final ShipmentRepository repository = mock(ShipmentRepository.class);
    private final ShipmentEventPublisher publisher = mock(ShipmentEventPublisher.class);
    private final ShipmentService service = new ShipmentService(repository, publisher);

    private Shipment shipment(ShipmentStatus status) {
        return Shipment.builder()
                .id(1L).origen("Santiago").destino("Valparaiso")
                .pesoKg(new BigDecimal("10")).clienteNombre("Ana")
                .estado(status).build();
    }

    @Test
    void createPublishesShipmentCreated() {
        Shipment saved = shipment(ShipmentStatus.CREADO);
        when(repository.save(any(Shipment.class))).thenReturn(saved);

        service.create(new ShipmentRequestDTO("Santiago", "Valparaiso", new BigDecimal("10"), "Ana", "ana@rutaexpress.cl"));

        verify(publisher).shipmentCreated(saved);
    }

    @Test
    void updateStatusPublishesPreviousAndNewStatus() {
        Shipment existing = shipment(ShipmentStatus.CREADO);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        service.updateStatus(1L, new ShipmentStatusUpdateDTO("ACEPTADO"));

        verify(publisher).shipmentStatusChanged(existing, ShipmentStatus.CREADO, null);
    }

    @Test
    void failedDeliveryForwardsReasonToPublisher() {
        Shipment existing = shipment(ShipmentStatus.EN_TRANSITO);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        service.updateStatus(1L, new ShipmentStatusUpdateDTO("FALLIDO", "Destinatario ausente"));

        verify(publisher).shipmentStatusChanged(existing, ShipmentStatus.EN_TRANSITO, "Destinatario ausente");
    }

    @Test
    void createStoresNormalizedEmail() {
        when(repository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(new ShipmentRequestDTO("Santiago", "Valparaiso", new BigDecimal("10"), "Ana", " Ana@RutaExpress.cl "));

        ArgumentCaptor<Shipment> saved = ArgumentCaptor.forClass(Shipment.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getClienteEmail()).isEqualTo("ana@rutaexpress.cl");
    }

    @Test
    void findAllFiltersByCustomerEmailWhenGiven() {
        service.findAll("ana@rutaexpress.cl");
        service.findAll(null);

        verify(repository).findByClienteEmailIgnoreCase("ana@rutaexpress.cl");
        verify(repository).findAll();
    }

    @Test
    void invalidTransitionDoesNotPublishAnything() {
        when(repository.findById(1L)).thenReturn(Optional.of(shipment(ShipmentStatus.ENTREGADO)));

        assertThatThrownBy(() -> service.updateStatus(1L, new ShipmentStatusUpdateDTO("CREADO")))
                .isInstanceOf(InvalidStatusTransitionException.class);
        verifyNoInteractions(publisher);
    }

}
