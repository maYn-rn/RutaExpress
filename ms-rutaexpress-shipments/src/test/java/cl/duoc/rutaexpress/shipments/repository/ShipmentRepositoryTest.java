package cl.duoc.rutaexpress.shipments.repository;

import cl.duoc.rutaexpress.shipments.entity.Shipment;
import cl.duoc.rutaexpress.shipments.entity.ShipmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ShipmentRepositoryTest {

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Test
    void shouldSaveAndRetrieveShipment() {
        Shipment shipment = Shipment.builder()
                .origen("Santiago")
                .destino("Valparaiso")
                .pesoKg(new BigDecimal("12.50"))
                .estado(ShipmentStatus.CREADO)
                .clienteNombre("Juan Perez")
                .build();

        Shipment saved = shipmentRepository.save(shipment);

        Optional<Shipment> found = shipmentRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getOrigen()).isEqualTo("Santiago");
        assertThat(found.get().getDestino()).isEqualTo("Valparaiso");
        assertThat(found.get().getPesoKg()).isEqualByComparingTo("12.50");
        assertThat(found.get().getEstado()).isEqualTo(ShipmentStatus.CREADO);
        assertThat(found.get().getClienteNombre()).isEqualTo("Juan Perez");
        assertThat(found.get().getFechaCreacion()).isNotNull();
    }

    @Test
    void shouldFindShipmentsByCustomerEmailIgnoringCase() {
        shipmentRepository.save(Shipment.builder()
                .origen("Santiago").destino("Temuco").pesoKg(BigDecimal.ONE)
                .estado(ShipmentStatus.CREADO).clienteNombre("Ana").clienteEmail("ana@rutaexpress.cl")
                .build());
        shipmentRepository.save(Shipment.builder()
                .origen("Santiago").destino("Arica").pesoKg(BigDecimal.ONE)
                .estado(ShipmentStatus.CREADO).clienteNombre("Luis").clienteEmail("luis@rutaexpress.cl")
                .build());

        assertThat(shipmentRepository.findByClienteEmailIgnoreCase("ANA@rutaexpress.cl"))
                .extracting(Shipment::getDestino)
                .containsExactly("Temuco");
    }

}
