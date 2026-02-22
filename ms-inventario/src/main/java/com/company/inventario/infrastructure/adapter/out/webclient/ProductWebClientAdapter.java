package com.company.inventario.infrastructure.adapter.out.webclient;

import com.company.inventario.domain.exception.ProductServiceException;
import com.company.inventario.domain.port.ProductPort;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import java.math.BigDecimal;
import java.time.Duration;

@Component
@Slf4j
public class ProductWebClientAdapter implements ProductPort {

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final int maxRetries;

    public ProductWebClientAdapter(
            WebClient.Builder webClientBuilder,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.products-service.url}") String productsServiceUrl,
            @Value("${app.products-service.api-key}") String apiKey,
            @Value("${app.products-service.api-key-header}") String apiKeyHeader,
            @Value("${app.products-service.timeout-seconds:5}") int timeoutSeconds,
            @Value("${app.products-service.max-retries:3}") int maxRetries) {

        this.webClient = webClientBuilder
                .baseUrl(productsServiceUrl)
                .defaultHeader(apiKeyHeader, apiKey)
                .build();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("products-service");
        this.maxRetries = maxRetries;
    }

    @Override
    public Mono<ProductInfo> getProduct(String productId) {
        return webClient.get()
                .uri("/api/v1/products/{id}", productId)
                .retrieve()
                .bodyToMono(ProductApiResponse.class)
                .map(response -> new ProductInfo(
                        response.data().id(),
                        response.data().attributes().name(),
                        response.data().attributes().price(),
                        response.data().attributes().description(),
                        response.data().attributes().active()
                ))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .retryWhen(Retry.backoff(maxRetries, Duration.ofMillis(500))
                        .maxBackoff(Duration.ofSeconds(5))
                        .jitter(0.75)
                        .filter(this::isRetryable))
                .onErrorMap(WebClientResponseException.NotFound.class,
                        e -> new ProductServiceException("Product not found: " + productId))
                .onErrorMap(e -> !(e instanceof ProductServiceException),
                        e -> new ProductServiceException("Failed to fetch product: " + productId, e))
                .doOnError(err -> log.error("Error fetching product {}: {}", productId, err.getMessage()));
    }

    private boolean isRetryable(Throwable t) {
        return !(t instanceof WebClientResponseException.NotFound)
                && !(t instanceof WebClientResponseException.BadRequest);
    }

    // DTO for ms-productos JSON:API response
    record ProductApiResponse(ProductData data) {
        record ProductData(String id, String type, ProductAttr attributes) {
            record ProductAttr(String name, BigDecimal price, String description, boolean active) {}
        }
    }
}
