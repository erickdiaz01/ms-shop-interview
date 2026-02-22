package com.company.inventario.infrastructure.adapter.out.persistence.entity;
import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.Table;
import java.time.Instant;
import java.util.UUID;

@Table("outbox_events")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OutboxEventEntity {
    @Id private UUID id;
    private String aggregateId;
    private String eventType;
    private String payload;
    private boolean published;
    @CreatedDate private Instant createdAt;
}
