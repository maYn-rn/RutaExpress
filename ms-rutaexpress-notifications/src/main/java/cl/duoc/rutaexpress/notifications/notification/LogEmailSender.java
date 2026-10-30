package cl.duoc.rutaexpress.notifications.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Simula el envio del email registrandolo en el log. */
@Slf4j
@Component
public class LogEmailSender implements EmailSender {

    @Override
    public void send(String to, String subject, String body) {
        log.info("[EMAIL] Para: {} | Asunto: {} | {}", to, subject, body);
    }

}
