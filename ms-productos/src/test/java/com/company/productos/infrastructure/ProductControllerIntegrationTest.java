package com.company.productos.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
@ActiveProfiles("test")
class ProductControllerIntegrationTest {

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

    @Autowired
    WebTestClient webTestClient;

    @Test
    void createProduct_shouldReturn201_withJsonApiFormat() {
        webTestClient.post().uri("/api/v1/products")
                .header("X-Service-Api-Key", "test-api-key")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Integration Test Product", "price", 99.99))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.type").isEqualTo("products")
                .jsonPath("$.data.attributes.name").isEqualTo("Integration Test Product")
                .jsonPath("$.data.attributes.price").isEqualTo(99.99);
    }

    @Test
    void getProduct_shouldReturn404_whenNotFound() {
        webTestClient.get().uri("/api/v1/products/00000000-0000-0000-0000-000000000000")
                .header("X-Service-Api-Key", "test-api-key")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.errors[0].code").isEqualTo("PRODUCT_NOT_FOUND");
    }

    @Test
    void createProduct_shouldReturn400_whenNameIsBlank() {
        webTestClient.post().uri("/api/v1/products")
                .header("X-Service-Api-Key", "test-api-key")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "", "price", 10.0))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors[0].code").isEqualTo("VALIDATION_ERROR");
    }
}
