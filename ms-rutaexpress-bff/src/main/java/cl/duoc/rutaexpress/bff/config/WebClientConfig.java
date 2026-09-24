package cl.duoc.rutaexpress.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${app.services.catalog-url}")
    private String catalogServiceUrl;

    @Value("${app.services.shipments-url}")
    private String shipmentsServiceUrl;

    @Bean
    public WebClient catalogWebClient(WebClient.Builder builder) {
        return builder.baseUrl(catalogServiceUrl).build();
    }

    @Bean
    public WebClient shipmentsWebClient(WebClient.Builder builder) {
        return builder.baseUrl(shipmentsServiceUrl).build();
    }

}
