package com.company.supportticketing.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.company.supportticketing.domain.entity.AppUser;
import com.company.supportticketing.domain.entity.StatusHistory;
import com.company.supportticketing.domain.entity.Ticket;
import com.company.supportticketing.domain.entity.TicketComment;
import com.company.supportticketing.domain.enums.HistoryEventType;
import com.company.supportticketing.domain.enums.TicketCategory;
import com.company.supportticketing.domain.enums.TicketPriority;
import com.company.supportticketing.domain.enums.TicketStatus;
import com.company.supportticketing.domain.enums.UserRole;
import com.company.supportticketing.domain.statemachine.TicketStateMachine;
import com.company.supportticketing.dto.request.AddCommentRequest;
import com.company.supportticketing.dto.request.AssignTicketRequest;
import com.company.supportticketing.dto.request.CreateTicketRequest;
import com.company.supportticketing.dto.request.UpdatePriorityRequest;
import com.company.supportticketing.dto.request.UpdateStatusRequest;
import com.company.supportticketing.dto.response.CommentResponse;
import com.company.supportticketing.dto.response.TicketResponse;
import com.company.supportticketing.exception.InvalidTransitionException;
import com.company.supportticketing.exception.InvalidSortPropertyException;
import com.company.supportticketing.exception.PermissionDeniedException;
import com.company.supportticketing.exception.TicketNotFoundException;
import com.company.supportticketing.exception.UserNotFoundException;
import com.company.supportticketing.mapper.TicketMapper;
import com.company.supportticketing.repository.CommentRepository;
import com.company.supportticketing.repository.StatusHistoryAppender;
import com.company.supportticketing.repository.StatusHistoryRepository;
import com.company.supportticketing.repository.TicketRepository;
import com.company.supportticketing.repository.UserRepository;
import com.company.supportticketing.security.PermissionService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock TicketRepository tickets;
    @Mock UserRepository users;
    @Mock CommentRepository comments;
    @Mock StatusHistoryRepository history;
    @Mock StatusHistoryAppender historyAppender;

    private TicketService service;
    private AppUser customer, otherCustomer, agent, otherAgent;

    @BeforeEach
    void setUp() {
        service = new TicketService(
                tickets,
                users,
                comments,
                history,
                historyAppender,
                new TicketMapper(),
                new TicketStateMachine(),
                new PermissionService());
        customer = AppUser.builder().id(UUID.randomUUID()).role(UserRole.CUSTOMER).name("Customer").build();
        otherCustomer = AppUser.builder().id(UUID.randomUUID()).role(UserRole.CUSTOMER).name("Other Customer").build();
        agent = AppUser.builder().id(UUID.randomUUID()).role(UserRole.AGENT).name("Agent").build();
        otherAgent = AppUser.builder().id(UUID.randomUUID()).role(UserRole.AGENT).name("Other Agent").build();
        lenient().when(tickets.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void create_appliesDefaultsAndWritesInitialHistory() {
        var result = service.create(customer, new CreateTicketRequest("  Help  ", "  Details  ", TicketCategory.TECHNICAL, null));
        assertEquals(TicketStatus.OPEN, result.status());
        assertEquals(TicketPriority.MEDIUM, result.priority());
        assertEquals("Help", result.title());
        assertEquals("Details", result.description());

        ArgumentCaptor<StatusHistory> event = ArgumentCaptor.forClass(StatusHistory.class);
        verify(historyAppender).append(event.capture());
        assertNull(event.getValue().getFromStatus());
        assertEquals(TicketStatus.OPEN, event.getValue().getToStatus());
        assertEquals(HistoryEventType.STATUS_CHANGE, event.getValue().getEventType());
    }

    @Test
    void create_throwsPermissionDenied_whenUserNotCustomer() {
        assertThrows(PermissionDeniedException.class, () ->
                service.create(agent, new CreateTicketRequest("Help", "Details", TicketCategory.TECHNICAL, TicketPriority.HIGH)));
    }

    @Test
    void get_success_whenOwnerCustomerOrAssignedAgent() {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = Ticket.builder().id(ticketId).customer(customer).assignedAgent(agent).status(TicketStatus.OPEN).build();
        when(tickets.findById(ticketId)).thenReturn(Optional.of(ticket));

        TicketResponse respCust = service.get(customer, ticketId);
        assertNotNull(respCust);
        assertEquals(ticketId, respCust.id());

        TicketResponse respAgent = service.get(agent, ticketId);
        assertNotNull(respAgent);
        assertEquals(ticketId, respAgent.id());
    }

    @Test
    void get_throwsTicketNotFound_whenMissing() {
        UUID ticketId = UUID.randomUUID();
        when(tickets.findById(ticketId)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> service.get(customer, ticketId));
    }

    @Test
    void get_throwsPermissionDenied_whenUnrelatedCustomerOrAgent() {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = Ticket.builder().id(ticketId).customer(customer).assignedAgent(agent).status(TicketStatus.OPEN).build();
        when(tickets.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(PermissionDeniedException.class, () -> service.get(otherCustomer, ticketId));
        assertThrows(PermissionDeniedException.class, () -> service.get(otherAgent, ticketId));
    }

    @Test
    void list_returnsPaginatedResults() {
        Ticket ticket = Ticket.builder().id(UUID.randomUUID()).customer(customer).status(TicketStatus.OPEN).priority(TicketPriority.MEDIUM).category(TicketCategory.TECHNICAL).build();
        when(tickets.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ticket), PageRequest.of(0, 20), 1));

        Page<TicketResponse> result = service.list(customer, TicketStatus.OPEN, TicketPriority.MEDIUM, TicketCategory.TECHNICAL, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals(ticket.getId(), result.getContent().get(0).id());
    }

    @Test
    void list_rejectsUnsupportedSortProperty_beforeQueryingDatabase() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("customer.passwordHash"));

        InvalidSortPropertyException exception = assertThrows(
                InvalidSortPropertyException.class,
                () -> service.list(customer, null, null, null, pageable));

        assertEquals(
                "Unsupported sort property 'customer.passwordHash'. Allowed properties: "
                        + "createdAt, updatedAt, status, priority, category",
                exception.getMessage());
        verify(tickets, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void unassigned_returnsFilteredPaginatedResultsForAgent() {
        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.MEDIUM)
                .category(TicketCategory.TECHNICAL)
                .build();
        when(tickets.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ticket), PageRequest.of(0, 100), 1));

        Page<TicketResponse> result = service.unassigned(
                agent,
                TicketStatus.OPEN,
                TicketPriority.MEDIUM,
                TicketCategory.TECHNICAL,
                PageRequest.of(0, 500));

        assertEquals(1, result.getTotalElements());
        assertNull(result.getContent().get(0).assignedAgentId());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(tickets).findAll(any(Specification.class), pageable.capture());
        assertEquals(100, pageable.getValue().getPageSize());
    }

    @Test
    void unassigned_rejectsCustomerBeforeQueryingDatabase() {
        assertThrows(
                PermissionDeniedException.class,
                () -> service.unassigned(
                        customer,
                        null,
                        null,
                        null,
                        PageRequest.of(0, 20)));

        verify(tickets, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void assign_selfAssigns_whenAgentIdNull() {
        Ticket ticket = Ticket.builder().id(UUID.randomUUID()).customer(customer).status(TicketStatus.OPEN).priority(TicketPriority.MEDIUM).build();
        when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        TicketResponse response = service.assign(agent, ticket.getId(), new AssignTicketRequest(null));

        assertEquals(TicketStatus.OPEN, response.status());
        assertEquals(agent.getId(), response.assignedAgentId());
        ArgumentCaptor<StatusHistory> event = ArgumentCaptor.forClass(StatusHistory.class);
        verify(historyAppender).append(event.capture());
        assertEquals(HistoryEventType.ASSIGNMENT, event.getValue().getEventType());
        assertNull(event.getValue().getFromAgentId());
        assertEquals(agent.getId(), event.getValue().getToAgentId());
    }

    @Test
    void assign_isIdempotent_whenTicketAlreadyBelongsToActor() {
        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .assignedAgent(agent)
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.MEDIUM)
                .build();
        when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        TicketResponse implicitResult = service.assign(
                agent,
                ticket.getId(),
                new AssignTicketRequest(null));
        TicketResponse explicitResult = service.assign(
                agent,
                ticket.getId(),
                new AssignTicketRequest(agent.getId()));

        assertEquals(agent.getId(), implicitResult.assignedAgentId());
        assertEquals(agent.getId(), explicitResult.assignedAgentId());
        verify(users, never()).findById(any());
        verify(tickets, never()).save(any());
        verify(historyAppender, never()).append(any());
    }

    @Test
    void assign_reassignsTicket_andRecordsPreviousAgent() {
        Ticket ticket = Ticket.builder().id(UUID.randomUUID()).customer(customer).assignedAgent(agent).status(TicketStatus.OPEN).priority(TicketPriority.MEDIUM).build();
        when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(users.findById(otherAgent.getId())).thenReturn(Optional.of(otherAgent));

        TicketResponse response = service.assign(agent, ticket.getId(), new AssignTicketRequest(otherAgent.getId()));

        assertEquals(otherAgent.getId(), response.assignedAgentId());
        ArgumentCaptor<StatusHistory> event = ArgumentCaptor.forClass(StatusHistory.class);
        verify(historyAppender).append(event.capture());
        assertEquals(agent.getId(), event.getValue().getFromAgentId());
        assertEquals(otherAgent.getId(), event.getValue().getToAgentId());
    }

    @Test
    void assign_throwsUserNotFound_whenTargetUserNotFound() {
        UUID ticketId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        Ticket ticket = Ticket.builder().id(ticketId).customer(customer).assignedAgent(agent).build();
        when(tickets.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(users.findById(targetId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.assign(agent, ticketId, new AssignTicketRequest(targetId)));
    }

    @Test
    void assign_throwsPermissionDenied_whenTargetUserIsCustomer() {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = Ticket.builder().id(ticketId).customer(customer).assignedAgent(agent).build();
        when(tickets.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(users.findById(customer.getId())).thenReturn(Optional.of(customer));

        assertThrows(PermissionDeniedException.class, () -> service.assign(agent, ticketId, new AssignTicketRequest(customer.getId())));
    }

    @Test
    void assign_throwsPermissionDenied_whenActorIsCustomer() {
        UUID ticketId = UUID.randomUUID();
        assertThrows(PermissionDeniedException.class, () -> service.assign(customer, ticketId, new AssignTicketRequest(null)));
    }

    @Test
    void assign_rejectsAssigningUnassignedTicketToAnotherAgent() {
        Ticket ticket = Ticket.builder().id(UUID.randomUUID()).customer(customer).build();
        when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThrows(
                PermissionDeniedException.class,
                () -> service.assign(agent, ticket.getId(), new AssignTicketRequest(otherAgent.getId())));
    }

    @Test
    void assign_rejectsReassignmentByUnrelatedAgent() {
        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .assignedAgent(agent)
                .build();
        when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThrows(
                PermissionDeniedException.class,
                () -> service.assign(otherAgent, ticket.getId(), new AssignTicketRequest(null)));
    }

    @Test
    void updateStatus_validTransition_writesMatchingEvent() {
        Ticket ticket = Ticket.builder().id(UUID.randomUUID()).customer(customer).assignedAgent(agent).status(TicketStatus.OPEN).priority(TicketPriority.MEDIUM).build();
        when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        service.updateStatus(agent, ticket.getId(), new UpdateStatusRequest(TicketStatus.IN_PROGRESS, "Starting"));

        ArgumentCaptor<StatusHistory> event = ArgumentCaptor.forClass(StatusHistory.class);
        verify(historyAppender).append(event.capture());
        assertEquals(TicketStatus.OPEN, event.getValue().getFromStatus());
        assertEquals(TicketStatus.IN_PROGRESS, event.getValue().getToStatus());
        assertEquals("Starting", event.getValue().getNote());
    }

    @Test
    void updateStatus_throwsInvalidTransition_whenIllegal() {
        Ticket ticket = Ticket.builder().id(UUID.randomUUID()).customer(customer).assignedAgent(agent).status(TicketStatus.OPEN).build();
        when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThrows(InvalidTransitionException.class, () ->
                service.updateStatus(agent, ticket.getId(), new UpdateStatusRequest(TicketStatus.RESOLVED, "Invalid directly to resolved")));
    }

    @Test
    void updatePriority_success_whenAssignedAgent() {
        Ticket ticket = Ticket.builder().id(UUID.randomUUID()).customer(customer).assignedAgent(agent).status(TicketStatus.OPEN).priority(TicketPriority.LOW).build();
        when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        TicketResponse response = service.updatePriority(agent, ticket.getId(), new UpdatePriorityRequest(TicketPriority.URGENT));

        assertEquals(TicketPriority.URGENT, response.priority());
    }

    @Test
    void updatePriority_throwsPermissionDenied_whenCustomerOrUnassignedAgent() {
        Ticket ticket = Ticket.builder().id(UUID.randomUUID()).customer(customer).assignedAgent(agent).status(TicketStatus.OPEN).priority(TicketPriority.LOW).build();
        when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThrows(PermissionDeniedException.class, () ->
                service.updatePriority(customer, ticket.getId(), new UpdatePriorityRequest(TicketPriority.HIGH)));
        assertThrows(PermissionDeniedException.class, () ->
                service.updatePriority(otherAgent, ticket.getId(), new UpdatePriorityRequest(TicketPriority.HIGH)));
    }

    @Test
    void addComment_success_whenParticipant() {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = Ticket.builder().id(ticketId).customer(customer).assignedAgent(agent).status(TicketStatus.OPEN).build();
        when(tickets.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(comments.save(any(TicketComment.class))).thenAnswer(inv -> {
            TicketComment c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CommentResponse response = service.addComment(customer, ticketId, new AddCommentRequest("  Need an update  "));

        assertNotNull(response);
        assertEquals("Need an update", response.body());
    }

    @Test
    void addComment_throwsPermissionDenied_whenUnrelatedActor() {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = Ticket.builder().id(ticketId).customer(customer).assignedAgent(agent).status(TicketStatus.OPEN).build();
        when(tickets.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(PermissionDeniedException.class, () ->
                service.addComment(otherCustomer, ticketId, new AddCommentRequest("Spam")));
        assertThrows(PermissionDeniedException.class, () ->
                service.addComment(otherAgent, ticketId, new AddCommentRequest("Spam")));
    }

    @Test
    void commentsArePagedWithStableChronologicalOrdering() {
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

    @Test
    void historyIsPagedWithStableChronologicalOrdering() {
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
