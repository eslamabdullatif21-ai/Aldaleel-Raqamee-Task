package com.company.supportticketing.mapper;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.company.supportticketing.domain.entity.AppUser;
import com.company.supportticketing.domain.entity.StatusHistory;
import com.company.supportticketing.domain.entity.Ticket;
import com.company.supportticketing.domain.entity.TicketComment;
import com.company.supportticketing.domain.enums.HistoryEventType;
import com.company.supportticketing.domain.enums.TicketCategory;
import com.company.supportticketing.domain.enums.TicketPriority;
import com.company.supportticketing.domain.enums.TicketStatus;
import com.company.supportticketing.domain.enums.UserRole;
import com.company.supportticketing.dto.response.CommentResponse;
import com.company.supportticketing.dto.response.HistoryResponse;
import com.company.supportticketing.dto.response.TicketResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TicketMapperTest {

    private final TicketMapper mapper = new TicketMapper();

    @Test
    void toResponse_ticket_mapsAllFieldsCorrectly() {
        UUID customerId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();
        AppUser customer = AppUser.builder().id(customerId).build();
        AppUser agent = AppUser.builder().id(agentId).build();
        Instant now = Instant.now();

        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .title("Issue")
                .description("Detailed description")
                .category(TicketCategory.BILLING)
                .priority(TicketPriority.URGENT)
                .status(TicketStatus.IN_PROGRESS)
                .customer(customer)
                .assignedAgent(agent)
                .createdAt(now)
                .updatedAt(now)
                .build();

        TicketResponse response = mapper.toResponse(ticket);

        assertEquals(ticket.getId(), response.id());
        assertEquals("Issue", response.title());
        assertEquals("Detailed description", response.description());
        assertEquals(TicketCategory.BILLING, response.category());
        assertEquals(TicketPriority.URGENT, response.priority());
        assertEquals(TicketStatus.IN_PROGRESS, response.status());
        assertEquals(customerId, response.customerId());
        assertEquals(agentId, response.assignedAgentId());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());
    }

    @Test
    void toResponse_ticket_handlesNullAssignedAgent() {
        AppUser customer = AppUser.builder().id(UUID.randomUUID()).build();
        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .title("Issue")
                .description("Desc")
                .category(TicketCategory.GENERAL)
                .priority(TicketPriority.LOW)
                .status(TicketStatus.OPEN)
                .customer(customer)
                .assignedAgent(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        TicketResponse response = mapper.toResponse(ticket);

        assertNull(response.assignedAgentId());
    }

    @Test
    void toResponse_comment_mapsAllFields() {
        AppUser author = AppUser.builder().id(UUID.randomUUID()).name("Alice").role(UserRole.CUSTOMER).build();
        Ticket ticket = Ticket.builder().id(UUID.randomUUID()).build();
        Instant now = Instant.now();

        TicketComment comment = TicketComment.builder()
                .id(UUID.randomUUID())
                .ticket(ticket)
                .author(author)
                .body("Hello world")
                .createdAt(now)
                .build();

        CommentResponse response = mapper.toResponse(comment);

        assertEquals(comment.getId(), response.id());
        assertEquals(ticket.getId(), response.ticketId());
        assertEquals(author.getId(), response.authorId());
        assertEquals("Alice", response.authorName());
        assertEquals(UserRole.CUSTOMER, response.authorRole());
        assertEquals("Hello world", response.body());
        assertEquals(now, response.createdAt());
    }

    @Test
    void toResponse_history_mapsAllFields() {
        AppUser changedBy = AppUser.builder().id(UUID.randomUUID()).build();
        Ticket ticket = Ticket.builder().id(UUID.randomUUID()).build();
        Instant now = Instant.now();
        UUID fromAgent = UUID.randomUUID();
        UUID toAgent = UUID.randomUUID();

        StatusHistory history = StatusHistory.builder()
                .id(UUID.randomUUID())
                .ticket(ticket)
                .eventType(HistoryEventType.ASSIGNMENT)
                .fromStatus(TicketStatus.OPEN)
                .toStatus(TicketStatus.IN_PROGRESS)
                .fromAgentId(fromAgent)
                .toAgentId(toAgent)
                .changedBy(changedBy)
                .changedAt(now)
                .note("Assigned agent")
                .build();

        HistoryResponse response = mapper.toResponse(history);

        assertEquals(history.getId(), response.id());
        assertEquals(ticket.getId(), response.ticketId());
        assertEquals(HistoryEventType.ASSIGNMENT, response.eventType());
        assertEquals(TicketStatus.OPEN, response.fromStatus());
        assertEquals(TicketStatus.IN_PROGRESS, response.toStatus());
        assertEquals(fromAgent, response.fromAgentId());
        assertEquals(toAgent, response.toAgentId());
        assertEquals(changedBy.getId(), response.changedBy());
        assertEquals(now, response.changedAt());
        assertEquals("Assigned agent", response.note());
    }
}
