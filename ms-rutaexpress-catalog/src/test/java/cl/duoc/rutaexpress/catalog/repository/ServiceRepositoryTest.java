package cl.duoc.rutaexpress.catalog.repository;

import cl.duoc.rutaexpress.catalog.entity.Service;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ServiceRepositoryTest {

    @Autowired
    private ServiceRepository serviceRepository;

    @Test
    void shouldSaveAndRetrieveService() {
        Service service = Service.builder()
                .nombre("Envío Express")
                .tipo("EXPRESS")
                .tarifaBase(new BigDecimal("5000.00"))
                .capacidadTotal(100)
                .capacidadDisponible(80)
                .build();

        Service saved = serviceRepository.save(service);

        Optional<Service> found = serviceRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getNombre()).isEqualTo("Envío Express");
        assertThat(found.get().getTipo()).isEqualTo("EXPRESS");
        assertThat(found.get().getTarifaBase()).isEqualByComparingTo("5000.00");
        assertThat(found.get().getCapacidadTotal()).isEqualTo(100);
        assertThat(found.get().getCapacidadDisponible()).isEqualTo(80);
    }

}
