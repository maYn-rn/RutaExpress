package cl.duoc.rutaexpress.notifications.messaging.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, String> {

    long countByConsumer(String consumer);

}
