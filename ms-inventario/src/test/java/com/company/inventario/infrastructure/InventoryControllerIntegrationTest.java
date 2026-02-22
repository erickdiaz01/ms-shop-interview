package com.company.inventario.infrastructure;

import com.company.inventario.domain.port.ProductPort;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
@ActiveProfiles("test")
class InventoryControllerIntegrationTest {

    public static final String TEST_API_KEY = "test-api-key";
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
                postgres.getJdbcUrl().replace("jdbc:postgresql", "r2dbc:postgresql"));
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired WebTestClient webTestClient;
    @MockBean  ProductPort productPort;

    private static final String PRODUCT_ID = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"; // seeded

    @BeforeEach
    void mockProductPort() {
        when(productPort.getProduct(any())).thenReturn(Mono.just(
                new ProductPort.ProductInfo(PRODUCT_ID, "Laptop", BigDecimal.valueOf(1299.99),
                        "A laptop", true)
        ));
    }

    @Test
    void getInventory_shouldReturn200_withProductInfo() {
        webTestClient.get().uri("/api/v1/inventory/{id}", PRODUCT_ID)
                .header("X-Service-Api-Key", TEST_API_KEY)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.type").isEqualTo("inventories")
                .jsonPath("$.data.attributes.productName").isEqualTo("Laptop")
                .jsonPath("$.data.attributes.quantity").isNumber();
    }

    @Test
    void purchase_shouldReturn201_andReduceStock() {
        webTestClient.post().uri("/api/v1/inventory/purchase")
                .header("X-Service-Api-Key", TEST_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("productId", PRODUCT_ID, "quantity", 2))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.type").isEqualTo("purchases")
                .jsonPath("$.data.attributes.quantity").isEqualTo(2)
                .jsonPath("$.data.attributes.totalAmount").isNumber()
                .jsonPath("$.data.attributes.remainingStock").isNumber();
    }

    @Test
    void purchase_shouldReturn422_whenInsufficientStock() {
        webTestClient.post().uri("/api/v1/inventory/purchase")
                .header("X-Service-Api-Key", TEST_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("productId", PRODUCT_ID, "quantity", 9999))
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.errors[0].code").isEqualTo("INSUFFICIENT_STOCK");
    }

    @Test
    void purchase_shouldReturn400_whenQuantityIsZero() {
        webTestClient.post().uri("/api/v1/inventory/purchase")
                .header("X-Service-Api-Key", TEST_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("productId", PRODUCT_ID, "quantity", 0))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors[0].code").isEqualTo("VALIDATION_ERROR");
    }
}
