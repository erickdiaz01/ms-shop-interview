package com.company.inventario.application.handler;

import com.company.inventario.application.command.GetInventoryCommand;
import com.company.inventario.application.port.CommandHandler;
import com.company.inventario.application.query.InventoryResult;
import com.company.inventario.domain.exception.InventoryNotFoundException;
import com.company.inventario.domain.port.InventoryRepository;
import com.company.inventario.domain.port.ProductPort;
import com.company.inventario.domain.valueobject.ProductId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class GetInventoryQueryHandler implements CommandHandler<GetInventoryCommand, InventoryResult> {

    private final InventoryRepository inventoryRepository;
    private final ProductPort productPort;

    @Override
    public Mono<InventoryResult> handle(GetInventoryCommand command) {
        log.debug("Querying inventory for productId: {}", command.productId());
        return inventoryRepository.findByProductId(ProductId.of(command.productId()))
                .switchIfEmpty(Mono.error(new InventoryNotFoundException(command.productId())))
                .zipWith(productPort.getProduct(command.productId()))
                .map(tuple -> {
                    var inv = tuple.getT1();
                    var product = tuple.getT2();
                    return new InventoryResult(
                            inv.getId().toString(),
                            command.productId(),
                            product.name(),
                            product.description(),
                            product.price(),
                            inv.getQuantity().value(),
                            inv.getMinStock(),
                            inv.isLowStock(),
                            inv.getUpdatedAt()
                    );
                });
    }
}
