package com.company.supportticketing.service;

import com.company.supportticketing.domain.entity.*;
import com.company.supportticketing.domain.enums.*;
import com.company.supportticketing.domain.statemachine.TicketStateMachine;
import com.company.supportticketing.dto.request.*;
import com.company.supportticketing.dto.response.*;
import com.company.supportticketing.exception.*;
import com.company.supportticketing.mapper.TicketMapper;
import com.company.supportticketing.repository.*;
import com.company.supportticketing.security.PermissionService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class TicketService {
    private static final int MAX_PAGE_SIZE = 100;

    private final TicketRepository tickets;
    private final UserRepository users;
    private final CommentRepository comments;
    private final StatusHistoryRepository history;
    private final TicketMapper mapper;
    private final TicketStateMachine stateMachine;
    private final PermissionService permissions;

    @Transactional
    public TicketResponse create(AppUser customer, CreateTicketRequest request) {
        if (customer.getRole() != UserRole.CUSTOMER) throw new PermissionDeniedException("Only customers may create tickets");
        Ticket ticket = tickets.save(Ticket.builder().title(request.title().trim()).description(request.description().trim())
                .category(request.category()).priority(request.priority() == null ? TicketPriority.MEDIUM : request.priority())
                .status(TicketStatus.OPEN).customer(customer).build());
        history.save(StatusHistory.builder().ticket(ticket).eventType(HistoryEventType.STATUS_CHANGE)
                .toStatus(TicketStatus.OPEN).changedBy(customer).note("Ticket created").build());
        return mapper.toResponse(ticket);
    }

    @Transactional(readOnly = true)
    public TicketResponse get(AppUser actor, UUID id) {
        Ticket ticket = requireTicket(id); permissions.assertCanView(actor, ticket); return mapper.toResponse(ticket);
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> list(AppUser actor, TicketStatus status, TicketPriority priority,
                                     TicketCategory category, Pageable pageable) {
        Specification<Ticket> spec = scopedTo(actor).and(filter(status, priority, category));
        return tickets.findAll(spec, cappedPage(pageable)).map(mapper::toResponse);
    }

    @Transactional
    public TicketResponse assign(AppUser actor, UUID id, AssignTicketRequest request) {
        permissions.assertCanAssign(actor);
        Ticket ticket = requireTicket(id);
        AppUser agent = request.agentId() == null ? actor : users.findById(request.agentId()).orElseThrow(() -> new UserNotFoundException(request.agentId()));
        if (agent.getRole() != UserRole.AGENT) throw new PermissionDeniedException("Tickets may only be assigned to support agents");
        UUID previous = ticket.getAssignedAgent() == null ? null : ticket.getAssignedAgent().getId();
        ticket.setAssignedAgent(agent);
        history.save(StatusHistory.builder().ticket(ticket).eventType(HistoryEventType.ASSIGNMENT)
                .fromAgentId(previous).toAgentId(agent.getId()).changedBy(actor).note("Ticket assigned").build());
        return mapper.toResponse(tickets.save(ticket));
    }

    @Transactional
    public TicketResponse updateStatus(AppUser actor, UUID id, UpdateStatusRequest request) {
        Ticket ticket = requireTicket(id);
        permissions.assertCanUpdateStatus(actor, ticket, request.status());
        TicketStatus previous = ticket.getStatus();
        stateMachine.validateTransition(previous, request.status());
        ticket.setStatus(request.status());
        history.save(StatusHistory.builder().ticket(ticket).eventType(HistoryEventType.STATUS_CHANGE)
                .fromStatus(previous).toStatus(request.status()).changedBy(actor).note(request.note()).build());
        return mapper.toResponse(tickets.save(ticket));
    }

    @Transactional
    public TicketResponse updatePriority(AppUser actor, UUID id, UpdatePriorityRequest request) {
        Ticket ticket = requireTicket(id); permissions.assertCanUpdatePriority(actor, ticket);
        ticket.setPriority(request.priority()); return mapper.toResponse(tickets.save(ticket));
    }

    @Transactional
    public CommentResponse addComment(AppUser actor, UUID id, AddCommentRequest request) {
        Ticket ticket = requireTicket(id); permissions.assertCanComment(actor, ticket);
        return mapper.toResponse(comments.save(TicketComment.builder().ticket(ticket).author(actor).body(request.body().trim()).build()));
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> comments(AppUser actor, UUID id, Pageable pageable) {
        Ticket ticket = requireTicket(id); permissions.assertCanComment(actor, ticket);
        return comments.findByTicketId(id, chronologicalPage(pageable, "createdAt")).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<HistoryResponse> history(AppUser actor, UUID id, Pageable pageable) {
        Ticket ticket = requireTicket(id); permissions.assertCanViewHistory(actor, ticket);
        return history.findByTicketId(id, chronologicalPage(pageable, "changedAt")).map(mapper::toResponse);
    }

    private Ticket requireTicket(UUID id) { return tickets.findById(id).orElseThrow(() -> new TicketNotFoundException(id)); }

    private Specification<Ticket> scopedTo(AppUser actor) {
        return (root, query, cb) -> actor.getRole() == UserRole.CUSTOMER
                ? cb.equal(root.get("customer").get("id"), actor.getId())
                : cb.equal(root.get("assignedAgent").get("id"), actor.getId());
    }

    private Specification<Ticket> filter(TicketStatus status, TicketPriority priority, TicketCategory category) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (priority != null) predicates.add(cb.equal(root.get("priority"), priority));
            if (category != null) predicates.add(cb.equal(root.get("category"), category));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Pageable chronologicalPage(Pageable pageable, String timestampProperty) {
        return PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), MAX_PAGE_SIZE),
                Sort.by(Sort.Order.asc(timestampProperty), Sort.Order.asc("id")));
    }

    private Pageable cappedPage(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), MAX_PAGE_SIZE), pageable.getSort());
    }
}
