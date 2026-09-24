package cl.duoc.rutaexpress.shipments.repository;

import cl.duoc.rutaexpress.shipments.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
}
