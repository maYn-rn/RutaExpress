package cl.duoc.rutaexpress.shipments.messaging;

import cl.duoc.rutaexpress.shipments.entity.Shipment;
import cl.duoc.rutaexpress.shipments.entity.ShipmentStatus;

/**
 * Lo unico que la logica de negocio conoce de la mensajeria. ShipmentService
 * no sabe que existe RabbitMQ: solo avisa que ocurrio algo.
 */
public interface ShipmentEventPublisher {

    void shipmentCreated(Shipment shipment);

    void shipmentStatusChanged(Shipment shipment, ShipmentStatus previousStatus, String motivo);

}
