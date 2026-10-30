package cl.duoc.rutaexpress.notifications.ticket;

import cl.duoc.rutaexpress.notifications.messaging.event.EventEnvelope;
import cl.duoc.rutaexpress.notifications.messaging.event.ShipmentEvent;
import cl.duoc.rutaexpress.notifications.messaging.support.InvalidMessageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Logica de negocio de bodega: genera el ticket de picking cuando un envio es
 * ACEPTADO. El documento se registra en el log.
 */
@Slf4j
@Service
public class TicketService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

    public String generateTicket(EventEnvelope envelope) {
        ShipmentEvent data = envelope.data();
        if (data.origen() == null || data.destino() == null
                || data.pesoKg() == null || data.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidMessageException(
                    "Datos insuficientes para generar el ticket del envio #" + data.shipmentId());
        }

        String ticketNumber = "TKT-%s-%06d".formatted(LocalDate.now().format(DATE), data.shipmentId());
        log.info("""
                [BODEGA] Ticket de picking generado
                  Numero : {}
                  Cliente: {}
                  Ruta   : {} -> {}
                  Peso   : {} kg""",
                ticketNumber, data.clienteNombre(), data.origen(), data.destino(), data.pesoKg());
        return ticketNumber;
    }

}
