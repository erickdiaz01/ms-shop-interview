package com.company.productos.domain;

import com.company.productos.domain.exception.DuplicateProductException;
import com.company.productos.domain.model.Product;
import com.company.productos.domain.port.ProductRepository;
import com.company.productos.domain.service.ProductDomainService;
import com.company.productos.domain.valueobject.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductDomainServiceTest {

    @Mock ProductRepository productRepository;
    @InjectMocks ProductDomainService domainService;

    @Test
    void createProduct_shouldSucceed_whenNameIsUnique() {
        when(productRepository.findByName("Laptop")).thenReturn(Mono.empty());
        when(productRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(domainService.createProduct("Laptop", Money.of(999.99), "A laptop"))
                .assertNext(p -> {
                    assert p.getName().equals("Laptop");
                    assert p.getPrice().amount().doubleValue() == 999.99;
                    assert !p.getDomainEvents().isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void createProduct_shouldFail_whenNameIsDuplicate() {
        Product existing = Product.create("Laptop", Money.of(100.0), null);
        when(productRepository.findByName("Laptop")).thenReturn(Mono.just(existing));

        StepVerifier.create(domainService.createProduct("Laptop", Money.of(999.99), null))
                .expectError(DuplicateProductException.class)
                .verify();
    }

    @Test
    void createProduct_shouldFail_whenNameIsBlank() {
        StepVerifier.create(domainService.createProduct("  ", Money.of(10.0), null))
                .expectError(IllegalArgumentException.class)
                .verify();
        // Verificar que nunca se llamo al repositorio
        verifyNoInteractions(productRepository);
    }

    @Test
    void product_shouldRaiseDomainEvent_onCreation() {
        Product p = Product.create("Mouse", Money.of(29.99), null);
        assert p.getDomainEvents().size() == 1;
    }

    @Test
    void money_shouldRejectNegativeAmount() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Money.of(-1.0));
    }
}
