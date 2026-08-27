package com.company.supportticketing.domain.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.company.supportticketing.domain.enums.TicketCategory;
import com.company.supportticketing.domain.enums.TicketPriority;
import com.company.supportticketing.domain.enums.TicketStatus;

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
