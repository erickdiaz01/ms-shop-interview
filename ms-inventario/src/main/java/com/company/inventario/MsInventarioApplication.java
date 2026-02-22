package com.company.inventario;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@EnableScheduling
public class MsInventarioApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsInventarioApplication.class, args);
    }
}
