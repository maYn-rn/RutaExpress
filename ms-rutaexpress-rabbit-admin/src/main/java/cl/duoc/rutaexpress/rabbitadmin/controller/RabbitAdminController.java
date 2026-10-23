package cl.duoc.rutaexpress.rabbitadmin.controller;

import cl.duoc.rutaexpress.rabbitadmin.dto.BindingRequest;
import cl.duoc.rutaexpress.rabbitadmin.dto.CreateExchangeRequest;
import cl.duoc.rutaexpress.rabbitadmin.dto.CreateQueueRequest;
import cl.duoc.rutaexpress.rabbitadmin.dto.NamingRules;
import cl.duoc.rutaexpress.rabbitadmin.dto.QueueInfoResponse;
import cl.duoc.rutaexpress.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * API REST para administrar RabbitMQ.
 *
 * | Metodo | Ruta                              | Accion                  | Respuesta |
 * |--------|-----------------------------------|-------------------------|-----------|
 * | POST   | /api/admin/rabbit/queues          | Crear cola              | 201 / 409 |
 * | GET    | /api/admin/rabbit/queues/{name}   | Ver cola (mensajes)     | 200 / 404 |
 * | DELETE | /api/admin/rabbit/queues/{name}   | Eliminar cola           | 204 / 404 |
 * | POST   | /api/admin/rabbit/exchanges       | Crear exchange          | 201 / 409 |
 * | DELETE | /api/admin/rabbit/exchanges/{name}| Eliminar exchange       | 204       |
 * | POST   | /api/admin/rabbit/bindings        | Crear binding           | 201 / 404 |
 * | DELETE | /api/admin/rabbit/bindings        | Eliminar binding        | 204       |
 *
 * Datos invalidos -> 400 con el detalle de cada campo. Broker caido -> 503.
 * Servicio interno: solo se expone a traves del BFF, que exige rol Admin.
 */
@Validated
@RestController
@RequestMapping("/api/admin/rabbit")
@RequiredArgsConstructor
public class RabbitAdminController {

    private final RabbitAdminService rabbitAdminService;

    @PostMapping("/queues")
    public ResponseEntity<QueueInfoResponse> createQueue(@Valid @RequestBody CreateQueueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rabbitAdminService.createQueue(request));
    }

    @GetMapping("/queues/{name}")
    public QueueInfoResponse getQueue(@PathVariable @Pattern(regexp = NamingRules.RESOURCE_NAME,
            message = "El nombre de la cola " + NamingRules.RESOURCE_NAME_MESSAGE) String name) {
        return rabbitAdminService.getQueue(name);
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Void> deleteQueue(@PathVariable @Pattern(regexp = NamingRules.RESOURCE_NAME,
            message = "El nombre de la cola " + NamingRules.RESOURCE_NAME_MESSAGE) String name) {
        rabbitAdminService.deleteQueue(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/exchanges")
    public ResponseEntity<Map<String, String>> createExchange(@Valid @RequestBody CreateExchangeRequest request) {
        rabbitAdminService.createExchange(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("name", request.name(), "type", request.type().name().toLowerCase()));
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> deleteExchange(@PathVariable @Pattern(regexp = NamingRules.RESOURCE_NAME,
            message = "El nombre del exchange " + NamingRules.RESOURCE_NAME_MESSAGE) String name) {
        rabbitAdminService.deleteExchange(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bindings")
    public ResponseEntity<BindingRequest> createBinding(@Valid @RequestBody BindingRequest request) {
        rabbitAdminService.createBinding(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> deleteBinding(@Valid @RequestBody BindingRequest request) {
        rabbitAdminService.deleteBinding(request);
        return ResponseEntity.noContent().build();
    }

}
