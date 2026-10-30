package cl.duoc.rutaexpress.shipments.service;

import cl.duoc.rutaexpress.shipments.dto.ShipmentRequestDTO;
import cl.duoc.rutaexpress.shipments.dto.ShipmentResponseDTO;
import cl.duoc.rutaexpress.shipments.dto.ShipmentStatusUpdateDTO;
import cl.duoc.rutaexpress.shipments.entity.Shipment;
import cl.duoc.rutaexpress.shipments.entity.ShipmentStatus;
import cl.duoc.rutaexpress.shipments.exception.InvalidStatusTransitionException;
import cl.duoc.rutaexpress.shipments.exception.ResourceNotFoundException;
import cl.duoc.rutaexpress.shipments.messaging.ShipmentEventPublisher;
import cl.duoc.rutaexpress.shipments.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventPublisher shipmentEventPublisher;

    /** Sin email devuelve todos; con email, solo los envios de ese cliente. */
    @Transactional(readOnly = true)
    public List<ShipmentResponseDTO> findAll(String clienteEmail) {
        List<Shipment> shipments = (clienteEmail == null || clienteEmail.isBlank())
                ? shipmentRepository.findAll()
                : shipmentRepository.findByClienteEmailIgnoreCase(clienteEmail.trim());
        return shipments.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ShipmentResponseDTO findById(Long id) {
        return toResponseDTO(getShipmentOrThrow(id));
    }

    @Transactional
    public ShipmentResponseDTO create(ShipmentRequestDTO request) {
        Shipment shipment = Shipment.builder()
                .origen(request.getOrigen())
                .destino(request.getDestino())
                .pesoKg(request.getPesoKg())
                .clienteNombre(request.getClienteNombre())
                .clienteEmail(request.getClienteEmail().trim().toLowerCase())
                .estado(ShipmentStatus.CREADO)
                .build();
        Shipment saved = shipmentRepository.save(shipment);
        shipmentEventPublisher.shipmentCreated(saved);
        return toResponseDTO(saved);
    }

    @Transactional
    public ShipmentResponseDTO updateStatus(Long id, ShipmentStatusUpdateDTO request) {
        Shipment shipment = getShipmentOrThrow(id);
        ShipmentStatus newStatus = parseStatus(request.getEstado());

        if (!shipment.getEstado().canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(
                    "No se puede cambiar el estado de " + shipment.getEstado() + " a " + newStatus);
        }

        ShipmentStatus previousStatus = shipment.getEstado();
        shipment.setEstado(newStatus);
        Shipment saved = shipmentRepository.save(shipment);
        shipmentEventPublisher.shipmentStatusChanged(saved, previousStatus, request.getMotivo());
        return toResponseDTO(saved);
    }

    @Transactional
    public void delete(Long id) {
        Shipment shipment = getShipmentOrThrow(id);
        shipmentRepository.delete(shipment);
    }

    private Shipment getShipmentOrThrow(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Envio no encontrado con id: " + id));
    }

    private ShipmentStatus parseStatus(String rawStatus) {
        try {
            return ShipmentStatus.valueOf(rawStatus);
        } catch (IllegalArgumentException ex) {
            throw new InvalidStatusTransitionException("Estado invalido: " + rawStatus);
        }
    }

    private ShipmentResponseDTO toResponseDTO(Shipment shipment) {
        return ShipmentResponseDTO.builder()
                .id(shipment.getId())
                .origen(shipment.getOrigen())
                .destino(shipment.getDestino())
                .pesoKg(shipment.getPesoKg())
                .estado(shipment.getEstado())
                .clienteNombre(shipment.getClienteNombre())
                .clienteEmail(shipment.getClienteEmail())
                // Hibernate guarda la fecha en la zona de la JVM; se convierte a instante absoluto.
                .fechaCreacion(shipment.getFechaCreacion() != null
                        ? shipment.getFechaCreacion().atZone(ZoneId.systemDefault()).toInstant()
                        : null)
                .build();
    }

}
