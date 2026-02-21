package com.company.productos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // necesario para el Outbox polling publisher
public class MsProductosApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsProductosApplication.class, args);
    }
}
