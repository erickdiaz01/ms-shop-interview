package com.company.inventario.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    // Topics en los que ms-inventario es PRODUCTOR (outbox de compras)
    @Value("${app.kafka.topics.inventory-events}")  private String inventoryEventsTopic;
    @Value("${app.kafka.topics.purchase-completed}") private String purchaseCompletedTopic;
    @Value("${app.kafka.topics.inventory-alerts}")   private String inventoryAlertsTopic;

    // Topic en el que ms-inventario es CONSUMIDOR (creación de productos)
    @Value("${app.kafka.topics.product-events:product.events}") private String productEventsTopic;

    @Bean public NewTopic inventoryEventsTopic() {
        return TopicBuilder.name(inventoryEventsTopic).partitions(3).replicas(1).build();
    }
    @Bean public NewTopic purchaseCompletedTopic() {
        return TopicBuilder.name(purchaseCompletedTopic).partitions(3).replicas(1).build();
    }
    @Bean public NewTopic inventoryAlertsTopic() {
        return TopicBuilder.name(inventoryAlertsTopic).partitions(1).replicas(1).build();
    }
    @Bean public NewTopic productEventsTopic() {
        return TopicBuilder.name(productEventsTopic).partitions(3).replicas(1).build();
    }
    /** Dead Letter Topic para mensajes que fallaron 3 veces */
    @Bean public NewTopic productEventsDlt() {
        return TopicBuilder.name(productEventsTopic + ".DLT").partitions(1).replicas(1).build();
    }
}
