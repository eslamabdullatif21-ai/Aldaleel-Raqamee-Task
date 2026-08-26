package com.company.supportticketing.domain.entity;

import com.company.supportticketing.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets", indexes = {
        @Index(name = "idx_ticket_customer", columnList = "customer_id"),
        @Index(name = "idx_ticket_agent_status", columnList = "assigned_agent_id,status")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Ticket {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, columnDefinition = "text")
    private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private TicketCategory category;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private TicketPriority priority;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private TicketStatus status;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private AppUser customer;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id")
    private AppUser assignedAgent;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version private long version;

    @PrePersist void onCreate() { var now = Instant.now(); if (createdAt == null) createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
