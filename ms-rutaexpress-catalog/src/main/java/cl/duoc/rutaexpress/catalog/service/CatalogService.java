package cl.duoc.rutaexpress.catalog.service;

import cl.duoc.rutaexpress.catalog.dto.ServiceRequestDTO;
import cl.duoc.rutaexpress.catalog.dto.ServiceResponseDTO;
import cl.duoc.rutaexpress.catalog.entity.Service;
import cl.duoc.rutaexpress.catalog.exception.ResourceNotFoundException;
import cl.duoc.rutaexpress.catalog.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class CatalogService {

    private final ServiceRepository serviceRepository;

    @Transactional(readOnly = true)
    public List<ServiceResponseDTO> findAll() {
        return serviceRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServiceResponseDTO findById(Long id) {
        Service service = getServiceOrThrow(id);
        return toResponseDTO(service);
    }

    @Transactional
    public ServiceResponseDTO create(ServiceRequestDTO request) {
        Service service = Service.builder()
                .nombre(request.getNombre())
                .tipo(request.getTipo())
                .tarifaBase(request.getTarifaBase())
                .capacidadTotal(request.getCapacidadTotal())
                .capacidadDisponible(request.getCapacidadDisponible())
                .build();
        return toResponseDTO(serviceRepository.save(service));
    }

    @Transactional
    public ServiceResponseDTO update(Long id, ServiceRequestDTO request) {
        Service service = getServiceOrThrow(id);

        if (request.getTarifaBase() != null) {
            service.setTarifaBase(request.getTarifaBase());
        }
        if (request.getCapacidadTotal() != null) {
            service.setCapacidadTotal(request.getCapacidadTotal());
        }
        if (request.getCapacidadDisponible() != null) {
            service.setCapacidadDisponible(request.getCapacidadDisponible());
        }

        return toResponseDTO(serviceRepository.save(service));
    }

    @Transactional
    public void delete(Long id) {
        Service service = getServiceOrThrow(id);
        serviceRepository.delete(service);
    }

    private Service getServiceOrThrow(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));
    }

    private ServiceResponseDTO toResponseDTO(Service service) {
        return ServiceResponseDTO.builder()
                .id(service.getId())
                .nombre(service.getNombre())
                .tipo(service.getTipo())
                .tarifaBase(service.getTarifaBase())
                .capacidadTotal(service.getCapacidadTotal())
                .capacidadDisponible(service.getCapacidadDisponible())
                .build();
    }

}
