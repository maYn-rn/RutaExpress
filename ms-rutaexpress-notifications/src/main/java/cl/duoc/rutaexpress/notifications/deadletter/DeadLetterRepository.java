package cl.duoc.rutaexpress.notifications.deadletter;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeadLetterRepository extends JpaRepository<DeadLetterRecord, Long> {

    long countByConsumer(String consumer);

}
