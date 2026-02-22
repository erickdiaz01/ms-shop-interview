package com.company.inventario.application.handler;

import com.company.inventario.application.command.PurchaseCommand;
import com.company.inventario.application.port.CommandHandler;
import com.company.inventario.application.query.PurchaseResult;
import com.company.inventario.domain.service.InventoryDomainService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.UUID;

@Component
@Slf4j
public class PurchaseCommandHandler implements CommandHandler<PurchaseCommand, PurchaseResult> {

    private final InventoryDomainService domainService;
    private final Counter purchasesTotal;
    private final Counter purchasesFailed;

    public PurchaseCommandHandler(InventoryDomainService domainService, MeterRegistry meterRegistry) {
        this.domainService = domainService;
        this.purchasesTotal = Counter.builder("inventory.purchases.total")
                .description("Total purchases processed").register(meterRegistry);
        this.purchasesFailed = Counter.builder("inventory.purchases.failed")
                .description("Failed purchases").register(meterRegistry);
    }

    @Override
    public Mono<PurchaseResult> handle(PurchaseCommand command) {
        String correlationId = command.correlationId() != null
                ? command.correlationId() : UUID.randomUUID().toString();

        log.info("Handling PurchaseCommand: productId={}, qty={}, correlationId={}",
                command.productId(), command.quantity(), correlationId);

        return domainService.processPurchase(command.productId(), command.quantity(), correlationId)
                .doOnSuccess(r -> {
                    purchasesTotal.increment();
                    log.info("Purchase completed: purchaseId={}, remainingStock={}",
                            r.purchaseId(), r.remainingStock());
                })
                .doOnError(e -> {
                    purchasesFailed.increment();
                    log.error("Purchase failed: productId={}, error={}", command.productId(), e.getMessage());
                })
                .map(r -> new PurchaseResult(
                        r.purchaseId(), r.productId(), r.productName(),
                        r.quantity(), r.unitPrice(), r.totalAmount(),
                        r.remainingStock(), Instant.now()
                ));
    }
}
