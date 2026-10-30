package cl.duoc.rutaexpress.notifications.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messaging/metrics")
@RequiredArgsConstructor
public class MessagingMetricsController {

    private final MessagingMetricsService metricsService;

    @GetMapping
    public MessagingMetricsService.MessagingMetrics metrics() {
        return metricsService.metrics();
    }

}
