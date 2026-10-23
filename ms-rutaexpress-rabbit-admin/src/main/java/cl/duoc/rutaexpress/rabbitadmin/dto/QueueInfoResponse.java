package cl.duoc.rutaexpress.rabbitadmin.dto;

/** Estado actual de una cola en el broker. */
public record QueueInfoResponse(String name, long messageCount, int consumerCount) {
}
