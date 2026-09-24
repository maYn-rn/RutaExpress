package cl.duoc.rutaexpress.catalog.repository;

import cl.duoc.rutaexpress.catalog.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<Service, Long> {
}
