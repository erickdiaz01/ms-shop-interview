package com.company.productos.domain.exception;
public class DuplicateProductException extends RuntimeException {
    public DuplicateProductException(String name) {
        super("Product already exists with name: " + name);
    }
}
