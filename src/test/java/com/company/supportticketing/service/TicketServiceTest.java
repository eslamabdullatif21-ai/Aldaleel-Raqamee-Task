package com.company.supportticketing.service;

import com.company.supportticketing.domain.entity.*;
import com.company.supportticketing.domain.enums.*;
import com.company.supportticketing.domain.statemachine.TicketStateMachine;
import com.company.supportticketing.dto.request.*;
import com.company.supportticketing.mapper.TicketMapper;
import com.company.supportticketing.repository.*;
import com.company.supportticketing.security.PermissionService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {
    @Mock TicketRepository tickets; @Mock UserRepository users; @Mock CommentRepository comments; @Mock StatusHistoryRepository history;
    private TicketService service;
    private AppUser customer, agent;
    @BeforeEach void setUp() {
        service = new TicketService(tickets, users, comments, history, new TicketMapper(), new TicketStateMachine(), new PermissionService());
        customer = AppUser.builder().id(UUID.randomUUID()).role(UserRole.CUSTOMER).build();
        agent = AppUser.builder().id(UUID.randomUUID()).role(UserRole.AGENT).build();
        lenient().when(tickets.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }
    @Test void creationAppliesDefaultsAndWritesInitialHistory() {
        var result = service.create(customer, new CreateTicketRequest("Help", "Details", TicketCategory.TECHNICAL, null));
        assertEquals(TicketStatus.OPEN, result.status()); assertEquals(TicketPriority.MEDIUM, result.priority());
        ArgumentCaptor<StatusHistory> event = ArgumentCaptor.forClass(StatusHistory.class);
        verify(history).save(event.capture());
        assertNull(event.getValue().getFromStatus()); assertEquals(TicketStatus.OPEN, event.getValue().getToStatus());
    }
    @Test void assignmentDoesNotImplicitlyChangeStatusAndWritesAssignmentEvent() {
        Ticket ticket = Ticket.builder().id(UUID.randomUUID()).customer(customer).status(TicketStatus.OPEN).priority(TicketPriority.MEDIUM).build();
        when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        service.assign(agent, ticket.getId(), new AssignTicketRequest(null));
        assertEquals(TicketStatus.OPEN, ticket.getStatus()); assertEquals(agent, ticket.getAssignedAgent());
        ArgumentCaptor<StatusHistory> event = ArgumentCaptor.forClass(StatusHistory.class); verify(history).save(event.capture());
        assertEquals(HistoryEventType.ASSIGNMENT, event.getValue().getEventType());
    }
    @Test void validStatusUpdateWritesMatchingEvent() {
        Ticket ticket = Ticket.builder().id(UUID.randomUUID()).customer(customer).assignedAgent(agent).status(TicketStatus.OPEN).priority(TicketPriority.MEDIUM).build();
        when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        service.updateStatus(agent, ticket.getId(), new UpdateStatusRequest(TicketStatus.IN_PROGRESS, "Starting"));
        ArgumentCaptor<StatusHistory> event = ArgumentCaptor.forClass(StatusHistory.class); verify(history).save(event.capture());
        assertEquals(TicketStatus.OPEN, event.getValue().getFromStatus()); assertEquals(TicketStatus.IN_PROGRESS, event.getValue().getToStatus());
    }
    @Test void commentsArePagedWithStableChronologicalOrdering() {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = Ticket.builder().id(ticketId).customer(customer).status(TicketStatus.OPEN).build();
        TicketComment comment = TicketComment.builder().id(UUID.randomUUID()).ticket(ticket).author(customer)
                .body("Update").createdAt(Instant.now()).build();
        when(tickets.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(comments.findByTicketId(eq(ticketId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(comment), PageRequest.of(1, 100), 101));

        Page<?> result = service.comments(customer, ticketId, PageRequest.of(1, 500, Sort.by("body")));

        assertEquals(1, result.getNumber());
        assertEquals(101, result.getTotalElements());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(comments).findByTicketId(eq(ticketId), pageable.capture());
        assertEquals(100, pageable.getValue().getPageSize());
        assertEquals(Sort.Direction.ASC, pageable.getValue().getSort().getOrderFor("createdAt").getDirection());
        assertEquals(Sort.Direction.ASC, pageable.getValue().getSort().getOrderFor("id").getDirection());
    }
    @Test void historyIsPagedWithStableChronologicalOrdering() {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = Ticket.builder().id(ticketId).customer(customer).status(TicketStatus.OPEN).build();
        StatusHistory event = StatusHistory.builder().id(UUID.randomUUID()).ticket(ticket).changedBy(customer)
                .eventType(HistoryEventType.STATUS_CHANGE).toStatus(TicketStatus.OPEN).changedAt(Instant.now()).build();
        when(tickets.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(history.findByTicketId(eq(ticketId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 20), 1));

        Page<?> result = service.history(customer, ticketId, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(history).findByTicketId(eq(ticketId), pageable.capture());
        assertEquals(Sort.Direction.ASC, pageable.getValue().getSort().getOrderFor("changedAt").getDirection());
        assertEquals(Sort.Direction.ASC, pageable.getValue().getSort().getOrderFor("id").getDirection());
    }
}
