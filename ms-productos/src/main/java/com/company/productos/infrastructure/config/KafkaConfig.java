package com.company.productos.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${app.kafka.topics.product-events:product.events}")
    private String productEventsTopic;

    @Bean
    public NewTopic productEventsTopic() {
        return TopicBuilder.name(productEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
