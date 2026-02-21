package com.company.productos.application.handler;

import com.company.productos.application.command.CreateProductCommand;
import com.company.productos.application.port.CommandHandler;
import com.company.productos.application.query.ProductResult;
import com.company.productos.domain.port.OutboxRepository;
import com.company.productos.domain.service.ProductDomainService;
import com.company.productos.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateProductCommandHandler implements CommandHandler<CreateProductCommand, ProductResult> {

    private final ProductDomainService domainService;
    private final OutboxRepository outboxRepository;

    /**
     * Flujo transaccional:
     * 1. Crear producto en DB
     * 2. Guardar ProductCreatedEvent en outbox (MISMA transacción R2DBC)
     * 3. El OutboxPublisher (scheduler) lo enviará a Kafka → ms-inventario lo consumirá
     * <p>
     * Si Kafka está caído en este momento, el evento queda en outbox y se reintenta.
     * Si la DB falla, ningún evento queda guardado → consistencia garantizada.
     */
    @Override
//    @Transactional
    public Mono<ProductResult> handle(CreateProductCommand command) {
        log.info("Creating product: name={}", command.name());

        return domainService.createProduct(command.name(), Money.of(command.price()), command.description())
                .flatMap(product -> {
                    log.info("Número de eventos de dominio: {}", product.getDomainEvents().size());

                    // Guardar todos los domain events en outbox (en la misma tx)
                    var saveEvents = product.getDomainEvents().stream()
                            .map(domainEvent ->
                                    outboxRepository.save(domainEvent)
                                            .doOnSuccess(m -> log.info("Event saved in outbox: {}", m))
                            )
                            .toList();
                    log.info("eventos {}", saveEvents);

                    return reactor.core.publisher.Flux.fromIterable(saveEvents)
                            .flatMap(m -> m)
                            .then(Mono.just(product));
                })
                .map(product -> {
                    log.info("Product created: id={}, event queued in outbox", product.getId());
                    return new ProductResult(
                            product.getId().toString(),
                            product.getName(),
                            product.getPrice().amount(),
                            product.getDescription(),
                            product.isActive(),
                            product.getCreatedAt(),
                            product.getUpdatedAt()
                    );
                });
    }
}
