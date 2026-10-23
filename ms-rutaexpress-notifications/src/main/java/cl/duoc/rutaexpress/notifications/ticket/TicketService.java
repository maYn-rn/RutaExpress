package cl.duoc.rutaexpress.notifications.ticket;

import cl.duoc.rutaexpress.notifications.messaging.event.ShipmentEvent;
import cl.duoc.rutaexpress.notifications.messaging.support.InvalidMessageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Logica de negocio: genera el ticket (guia de despacho) de un envio recien
 * creado. El documento se registra en el log.
 */
@Slf4j
@Service
public class TicketService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

    public String generateTicket(ShipmentEvent event) {
        if (event.origen() == null || event.destino() == null
                || event.pesoKg() == null || event.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidMessageException(
                    "Datos insuficientes para generar el ticket del envio #" + event.shipmentId());
        }

        String ticketNumber = "TKT-%s-%06d".formatted(LocalDate.now().format(DATE), event.shipmentId());
        log.info("""
                [TICKET] Guia de despacho generada
                  Numero : {}
                  Cliente: {}
                  Ruta   : {} -> {}
                  Peso   : {} kg""",
                ticketNumber, event.clienteNombre(), event.origen(), event.destino(), event.pesoKg());
        return ticketNumber;
    }

}
