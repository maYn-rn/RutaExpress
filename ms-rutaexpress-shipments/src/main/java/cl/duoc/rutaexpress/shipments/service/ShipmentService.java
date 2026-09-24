package cl.duoc.rutaexpress.shipments.service;

import cl.duoc.rutaexpress.shipments.dto.ShipmentRequestDTO;
import cl.duoc.rutaexpress.shipments.dto.ShipmentResponseDTO;
import cl.duoc.rutaexpress.shipments.dto.ShipmentStatusUpdateDTO;
import cl.duoc.rutaexpress.shipments.entity.Shipment;
import cl.duoc.rutaexpress.shipments.entity.ShipmentStatus;
import cl.duoc.rutaexpress.shipments.exception.InvalidStatusTransitionException;
import cl.duoc.rutaexpress.shipments.exception.ResourceNotFoundException;
import cl.duoc.rutaexpress.shipments.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;

    @Transactional(readOnly = true)
    public List<ShipmentResponseDTO> findAll() {
        return shipmentRepository.findAll().stream()
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
                .estado(ShipmentStatus.CREADO)
                .build();
        return toResponseDTO(shipmentRepository.save(shipment));
    }

    @Transactional
    public ShipmentResponseDTO updateStatus(Long id, ShipmentStatusUpdateDTO request) {
        Shipment shipment = getShipmentOrThrow(id);
        ShipmentStatus newStatus = parseStatus(request.getEstado());

        if (!shipment.getEstado().canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(
                    "No se puede cambiar el estado de " + shipment.getEstado() + " a " + newStatus);
        }

        shipment.setEstado(newStatus);
        return toResponseDTO(shipmentRepository.save(shipment));
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
                .fechaCreacion(shipment.getFechaCreacion())
                .build();
    }

}
