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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.company.supportticketing.domain.enums.HistoryEventType;
import com.company.supportticketing.domain.enums.TicketStatus;

@Entity
@Table(name = "status_history", indexes = @Index(name = "idx_history_ticket_changed", columnList = "ticket_id,changed_at"))
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class StatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ticket_id", nullable = false) private Ticket ticket;
    @Enumerated(EnumType.STRING) @Column(name = "event_type", nullable = false, length = 30) private HistoryEventType eventType;
    @Enumerated(EnumType.STRING) @Column(name = "from_status", length = 30) private TicketStatus fromStatus;
    @Enumerated(EnumType.STRING) @Column(name = "to_status", length = 30) private TicketStatus toStatus;
    @Column(name = "from_agent_id") private UUID fromAgentId;
    @Column(name = "to_agent_id") private UUID toAgentId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "changed_by", nullable = false) private AppUser changedBy;
    @Column(name = "changed_at", nullable = false, updatable = false) private Instant changedAt;
    @Column(length = 1000) private String note;
    @PrePersist void onCreate() { if (changedAt == null) changedAt = Instant.now(); }
}
