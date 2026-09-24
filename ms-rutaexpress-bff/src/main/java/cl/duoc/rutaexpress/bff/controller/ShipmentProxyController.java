package cl.duoc.rutaexpress.bff.controller;

import cl.duoc.rutaexpress.bff.security.AccessRules;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.rutaexpress.bff.service.ShipmentProxyService;

/**
 * Reenvia peticiones ya autenticadas hacia ms-rutaexpress-shipments.
 * Autorizacion por endpoint (ver AccessRules): lectura requiere el scope
 * del API, crear/modificar requiere rol Admin u Operador y eliminar solo Admin.
 */
@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentProxyController {

    private final ShipmentProxyService shipmentProxyService;

    @GetMapping
    @PreAuthorize(AccessRules.CAN_READ)
    public ResponseEntity<String> getAll() {
        return shipmentProxyService.getAllShipments();
    }

    @GetMapping("/{id}")
    @PreAuthorize(AccessRules.CAN_READ)
    public ResponseEntity<String> getById(@PathVariable Long id) {
        return shipmentProxyService.getShipmentById(id);
    }

    @PostMapping
    @PreAuthorize(AccessRules.CAN_WRITE)
    public ResponseEntity<String> create(@RequestBody String body) {
        return shipmentProxyService.createShipment(body);
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize(AccessRules.CAN_WRITE)
    public ResponseEntity<String> updateStatus(@PathVariable Long id, @RequestBody String body) {
        return shipmentProxyService.updateShipmentStatus(id, body);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(AccessRules.CAN_DELETE)
    public ResponseEntity<String> delete(@PathVariable Long id) {
        return shipmentProxyService.deleteShipment(id);
    }

}
