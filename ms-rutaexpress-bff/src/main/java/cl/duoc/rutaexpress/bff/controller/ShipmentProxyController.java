package cl.duoc.rutaexpress.bff.controller;

import cl.duoc.rutaexpress.bff.security.AccessRules;
import cl.duoc.rutaexpress.bff.security.CustomerScope;
import cl.duoc.rutaexpress.bff.service.ProxyErrors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.rutaexpress.bff.service.ShipmentProxyService;

import java.util.Optional;

/**
 * Reenvia peticiones ya autenticadas hacia ms-rutaexpress-shipments.
 * Autorizacion por endpoint (ver AccessRules): lectura requiere el scope
 * del API, crear envios requiere rol Admin, Operador o Cliente, cambiar el
 * estado requiere Admin u Operador y eliminar solo Admin.
 *
 * Quien no es Admin ni Operador (ver CustomerScope) unicamente ve y crea
 * envios a su nombre: el email se toma del token.
 */
@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentProxyController {

    private static final String NO_EMAIL_MESSAGE = "El token no incluye el email del cliente";

    private final ShipmentProxyService shipmentProxyService;
    private final CustomerScope customerScope;

    @GetMapping
    @PreAuthorize(AccessRules.CAN_READ)
    public ResponseEntity<String> getAll(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        if (customerScope.canSeeAllShipments(authentication)) {
            return shipmentProxyService.getAllShipments();
        }
        return customerEmail(jwt)
                .map(shipmentProxyService::getShipmentsByCustomer)
                .orElseGet(() -> ProxyErrors.error(HttpStatus.FORBIDDEN, NO_EMAIL_MESSAGE));
    }

    @GetMapping("/{id}")
    @PreAuthorize(AccessRules.CAN_READ)
    public ResponseEntity<String> getById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt,
                                          Authentication authentication) {
        ResponseEntity<String> response = shipmentProxyService.getShipmentById(id);
        if (customerScope.canSeeAllShipments(authentication) || !response.getStatusCode().is2xxSuccessful()) {
            return response;
        }
        // Un envio ajeno responde igual que uno inexistente para no revelar que existe.
        boolean owns = customerEmail(jwt)
                .map(email -> customerScope.ownsShipment(response.getBody(), email))
                .orElse(false);
        return owns ? response : ProxyErrors.error(HttpStatus.NOT_FOUND, "Envio no encontrado con id: " + id);
    }

    @PostMapping
    @PreAuthorize(AccessRules.CAN_CREATE_SHIPMENT)
    public ResponseEntity<String> create(@RequestBody String body, @AuthenticationPrincipal Jwt jwt,
                                         Authentication authentication) {
        if (customerScope.canSeeAllShipments(authentication)) {
            return shipmentProxyService.createShipment(body);
        }
        return customerEmail(jwt)
                .map(email -> shipmentProxyService.createShipment(
                        customerScope.assignToCustomer(body, email, jwt.getClaimAsString("name"))))
                .orElseGet(() -> ProxyErrors.error(HttpStatus.FORBIDDEN, NO_EMAIL_MESSAGE));
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

    private Optional<String> customerEmail(Jwt jwt) {
        return jwt != null ? customerScope.email(jwt) : Optional.empty();
    }

}
