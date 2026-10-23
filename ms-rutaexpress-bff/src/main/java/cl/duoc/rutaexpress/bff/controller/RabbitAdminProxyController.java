package cl.duoc.rutaexpress.bff.controller;

import cl.duoc.rutaexpress.bff.security.AccessRules;
import cl.duoc.rutaexpress.bff.service.RabbitAdminProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reenvia hacia ms-rutaexpress-rabbit-admin. Solo el rol Admin puede
 * administrar colas, exchanges y bindings; la validacion de datos la hace
 * el microservicio administrador.
 */
@RestController
@RequestMapping("/api/admin/rabbit")
@RequiredArgsConstructor
@PreAuthorize(AccessRules.CAN_ADMINISTER_MESSAGING)
public class RabbitAdminProxyController {

    private final RabbitAdminProxyService rabbitAdminProxyService;

    @PostMapping("/queues")
    public ResponseEntity<String> createQueue(@RequestBody String body) {
        return rabbitAdminProxyService.createQueue(body);
    }

    @GetMapping("/queues/{name}")
    public ResponseEntity<String> getQueue(@PathVariable String name) {
        return rabbitAdminProxyService.getQueue(name);
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<String> deleteQueue(@PathVariable String name) {
        return rabbitAdminProxyService.deleteQueue(name);
    }

    @PostMapping("/exchanges")
    public ResponseEntity<String> createExchange(@RequestBody String body) {
        return rabbitAdminProxyService.createExchange(body);
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<String> deleteExchange(@PathVariable String name) {
        return rabbitAdminProxyService.deleteExchange(name);
    }

    @PostMapping("/bindings")
    public ResponseEntity<String> createBinding(@RequestBody String body) {
        return rabbitAdminProxyService.createBinding(body);
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<String> deleteBinding(@RequestBody String body) {
        return rabbitAdminProxyService.deleteBinding(body);
    }

}
