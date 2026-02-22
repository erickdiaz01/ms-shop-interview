package com.company.inventario.application.port;
import reactor.core.publisher.Mono;
public interface CommandHandler<C, R> {
    Mono<R> handle(C command);
}
