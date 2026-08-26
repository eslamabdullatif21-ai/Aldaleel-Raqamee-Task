package com.company.supportticketing.domain.entity;

import com.company.supportticketing.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "status_history", indexes = @Index(name = "idx_history_ticket_changed", columnList = "ticket_id,changed_at"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
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
