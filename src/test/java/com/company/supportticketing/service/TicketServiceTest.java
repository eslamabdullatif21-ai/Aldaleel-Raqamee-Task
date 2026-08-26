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
        when(tickets.save(any())).thenAnswer(inv -> inv.getArgument(0));
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
}
