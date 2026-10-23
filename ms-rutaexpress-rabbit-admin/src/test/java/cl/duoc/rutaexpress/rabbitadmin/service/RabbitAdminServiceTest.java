package cl.duoc.rutaexpress.rabbitadmin.service;

import cl.duoc.rutaexpress.rabbitadmin.dto.BindingRequest;
import cl.duoc.rutaexpress.rabbitadmin.dto.CreateExchangeRequest;
import cl.duoc.rutaexpress.rabbitadmin.dto.CreateQueueRequest;
import cl.duoc.rutaexpress.rabbitadmin.dto.ExchangeType;
import cl.duoc.rutaexpress.rabbitadmin.exception.ResourceConflictException;
import cl.duoc.rutaexpress.rabbitadmin.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RabbitAdminServiceTest {

    private final AmqpAdmin amqpAdmin = mock(AmqpAdmin.class);
    private final RabbitAdminService service = new RabbitAdminService(amqpAdmin);

    @Test
    void createsQueueWithDeadLetterAndTtl() {
        service.createQueue(new CreateQueueRequest("pagos.queue", null, "pagos.dlx", "pagos.dead", 60000));

        ArgumentCaptor<Queue> queue = ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin).declareQueue(queue.capture());
        assertThat(queue.getValue().getName()).isEqualTo("pagos.queue");
        assertThat(queue.getValue().isDurable()).isTrue();
        assertThat(queue.getValue().getArguments())
                .containsEntry("x-dead-letter-exchange", "pagos.dlx")
                .containsEntry("x-dead-letter-routing-key", "pagos.dead")
                .containsEntry("x-message-ttl", 60000);
    }

    @Test
    void creatingExistingQueueIsConflict() {
        when(amqpAdmin.getQueueInfo("pagos.queue")).thenReturn(new QueueInformation("pagos.queue", 0, 0));

        assertThatThrownBy(() -> service.createQueue(new CreateQueueRequest("pagos.queue", true, null, null, null)))
                .isInstanceOf(ResourceConflictException.class);
        verify(amqpAdmin, never()).declareQueue(any());
    }

    @Test
    void deletingMissingQueueIsNotFound() {
        assertThatThrownBy(() -> service.deleteQueue("no.existe"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(amqpAdmin, never()).deleteQueue(any());
    }

    @Test
    void deletesExistingQueue() {
        when(amqpAdmin.getQueueInfo("pagos.queue")).thenReturn(new QueueInformation("pagos.queue", 3, 1));

        service.deleteQueue("pagos.queue");

        verify(amqpAdmin).deleteQueue("pagos.queue");
    }

    @Test
    void createsExchangeOfRequestedType() {
        service.createExchange(new CreateExchangeRequest("pagos.exchange", ExchangeType.FANOUT, null));

        ArgumentCaptor<Exchange> exchange = ArgumentCaptor.forClass(Exchange.class);
        verify(amqpAdmin).declareExchange(exchange.capture());
        assertThat(exchange.getValue().getType()).isEqualTo(ExchangeTypes.FANOUT);
        assertThat(exchange.getValue().isDurable()).isTrue();
    }

    @Test
    void bindingToMissingQueueIsNotFound() {
        assertThatThrownBy(() -> service.createBinding(new BindingRequest("pagos.exchange", "no.existe", "pago.#")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(amqpAdmin, never()).declareBinding(any());
    }

    @Test
    void createsAndRemovesBinding() {
        when(amqpAdmin.getQueueInfo("pagos.queue")).thenReturn(new QueueInformation("pagos.queue", 0, 0));
        BindingRequest request = new BindingRequest("pagos.exchange", "pagos.queue", "pago.#");

        service.createBinding(request);
        service.deleteBinding(request);

        ArgumentCaptor<Binding> binding = ArgumentCaptor.forClass(Binding.class);
        verify(amqpAdmin).declareBinding(binding.capture());
        assertThat(binding.getValue().getExchange()).isEqualTo("pagos.exchange");
        assertThat(binding.getValue().getRoutingKey()).isEqualTo("pago.#");
        verify(amqpAdmin).removeBinding(any(Binding.class));
    }

}
