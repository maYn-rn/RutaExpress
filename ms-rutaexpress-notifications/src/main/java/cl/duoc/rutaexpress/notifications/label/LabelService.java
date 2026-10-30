package cl.duoc.rutaexpress.notifications.label;

import cl.duoc.rutaexpress.notifications.messaging.event.EventEnvelope;
import cl.duoc.rutaexpress.notifications.messaging.event.ShipmentEvent;
import cl.duoc.rutaexpress.notifications.messaging.support.InvalidMessageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Logica de negocio de etiquetas: genera la guia de despacho de un envio
 * recien creado. El documento se registra en el log.
 */
@Slf4j
@Service
public class LabelService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

    public String generateLabel(EventEnvelope envelope) {
        ShipmentEvent data = envelope.data();
        if (data.destino() == null || data.clienteNombre() == null) {
            throw new InvalidMessageException(
                    "Datos insuficientes para generar la guia del envio #" + data.shipmentId());
        }

        String guideNumber = "GD-%s-%06d".formatted(LocalDate.now().format(DATE), data.shipmentId());
        log.info("""
                [ETIQUETA] Guia de despacho generada
                  Numero      : {}
                  Destinatario: {} <{}>
                  Destino     : {}""",
                guideNumber, data.clienteNombre(), data.clienteEmail(), data.destino());
        return guideNumber;
    }

}
