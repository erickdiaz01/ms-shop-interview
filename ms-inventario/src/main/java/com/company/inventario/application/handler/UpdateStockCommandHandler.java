package com.company.inventario.application.handler;

import com.company.inventario.application.command.UpdateStockCommand;
import com.company.inventario.application.port.CommandHandler;
import com.company.inventario.application.query.InventoryResult;
import com.company.inventario.domain.exception.InventoryNotFoundException;
import com.company.inventario.domain.port.InventoryRepository;
import com.company.inventario.domain.port.OutboxRepository;
import com.company.inventario.domain.port.ProductPort;
import com.company.inventario.domain.valueobject.ProductId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateStockCommandHandler implements CommandHandler<UpdateStockCommand, InventoryResult> {

    private final InventoryRepository inventoryRepository;
    private final OutboxRepository outboxRepository;
    private final ProductPort productPort;

    @Override
    public Mono<InventoryResult> handle(UpdateStockCommand command) {
        String correlationId = command.correlationId() != null
                ? command.correlationId() : UUID.randomUUID().toString();

        return inventoryRepository.findByProductId(ProductId.of(command.productId()))
                .switchIfEmpty(Mono.error(new InventoryNotFoundException(command.productId())))
                .flatMap(inventory -> {
                    inventory.updateStock(command.quantity(), correlationId);
                    var events = inventory.getDomainEvents();
                    return inventoryRepository.save(inventory)
                            .flatMap(saved -> {
                                var saveEvents = events.stream()
                                        .map(outboxRepository::save)
                                        .toList();
                                return reactor.core.publisher.Flux.fromIterable(saveEvents)
                                        .flatMap(m -> m).then(Mono.just(saved));
                            });
                })
                .zipWith(productPort.getProduct(command.productId()))
                .map(tuple -> {
                    var inv = tuple.getT1();
                    var product = tuple.getT2();
                    return new InventoryResult(
                            inv.getId().toString(), command.productId(),
                            product.name(), product.description(), product.price(),
                            inv.getQuantity().value(), inv.getMinStock(),
                            inv.isLowStock(), inv.getUpdatedAt()
                    );
                });
    }
}
