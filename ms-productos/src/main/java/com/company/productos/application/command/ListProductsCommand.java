package com.company.productos.application.command;
public record ListProductsCommand(int page, int size) {
    public ListProductsCommand {
        if (page < 0) throw new IllegalArgumentException("Page must be >= 0");
        if (size < 1 || size > 100) throw new IllegalArgumentException("Size must be between 1 and 100");
    }
}
