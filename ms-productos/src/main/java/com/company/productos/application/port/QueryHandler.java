package com.company.productos.application.port;
import reactor.core.publisher.Flux;

public interface QueryHandler<C, R> {
    Flux<R> handle(C query);
}
