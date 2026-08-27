package com.company.supportticketing.security;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.company.supportticketing.domain.entity.AppUser;
import com.company.supportticketing.domain.entity.Ticket;
import com.company.supportticketing.domain.enums.TicketStatus;
import com.company.supportticketing.domain.enums.UserRole;
import com.company.supportticketing.exception.PermissionDeniedException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PermissionServiceTest {
    private final PermissionService permissions = new PermissionService();
    private AppUser customer, otherCustomer, agent, otherAgent;
    private Ticket ticket;
    @BeforeEach void setUp() {
        customer = user(UserRole.CUSTOMER); otherCustomer = user(UserRole.CUSTOMER);
        agent = user(UserRole.AGENT); otherAgent = user(UserRole.AGENT);
        ticket = Ticket.builder().customer(customer).assignedAgent(agent).status(TicketStatus.OPEN).build();
    }
    @Test void ownerAndAssignedAgentCanView() {
        assertDoesNotThrow(() -> permissions.assertCanView(customer, ticket));
        assertDoesNotThrow(() -> permissions.assertCanView(agent, ticket));
    }
    @Test void unrelatedActorsCannotView() {
        assertThrows(PermissionDeniedException.class, () -> permissions.assertCanView(otherCustomer, ticket));
        assertThrows(PermissionDeniedException.class, () -> permissions.assertCanView(otherAgent, ticket));
    }
    @Test void onlyAgentsPassAssignmentRoleCheck() {
        assertDoesNotThrow(() -> permissions.assertCanAssign(otherAgent));
        assertThrows(PermissionDeniedException.class, () -> permissions.assertCanAssign(customer));
    }
    @Test void agentCanSelfClaimUnassignedTicket() {
        ticket.setAssignedAgent(null);
        assertDoesNotThrow(() -> permissions.assertCanRouteAssignment(agent, ticket, agent.getId()));
    }
    @Test void agentCannotAssignUnassignedTicketToAnotherAgent() {
        ticket.setAssignedAgent(null);
        assertThrows(
                PermissionDeniedException.class,
                () -> permissions.assertCanRouteAssignment(agent, ticket, otherAgent.getId()));
    }
    @Test void assignedAgentCanReassignTicketButUnrelatedAgentCannot() {
        assertDoesNotThrow(() -> permissions.assertCanRouteAssignment(agent, ticket, otherAgent.getId()));
        assertThrows(
                PermissionDeniedException.class,
                () -> permissions.assertCanRouteAssignment(otherAgent, ticket, otherAgent.getId()));
    }
    @Test void onlyAssignedAgentCanUpdatePriority() {
        assertDoesNotThrow(() -> permissions.assertCanUpdatePriority(agent, ticket));
        assertThrows(PermissionDeniedException.class, () -> permissions.assertCanUpdatePriority(otherAgent, ticket));
        assertThrows(PermissionDeniedException.class, () -> permissions.assertCanUpdatePriority(customer, ticket));
    }
    @Test void customerResolutionCarveOutIsNarrow() {
        ticket.setStatus(TicketStatus.RESOLVED);
        assertDoesNotThrow(() -> permissions.assertCanUpdateStatus(customer, ticket, TicketStatus.CLOSED));
        assertDoesNotThrow(() -> permissions.assertCanUpdateStatus(customer, ticket, TicketStatus.REOPENED));
        assertThrows(PermissionDeniedException.class, () -> permissions.assertCanUpdateStatus(otherCustomer, ticket, TicketStatus.CLOSED));
        ticket.setStatus(TicketStatus.OPEN);
        assertThrows(PermissionDeniedException.class, () -> permissions.assertCanUpdateStatus(customer, ticket, TicketStatus.IN_PROGRESS));
    }
    @Test void onlyParticipantsCanCommentAndReadHistory() {
        assertDoesNotThrow(() -> permissions.assertCanComment(customer, ticket));
        assertDoesNotThrow(() -> permissions.assertCanViewHistory(agent, ticket));
        assertThrows(PermissionDeniedException.class, () -> permissions.assertCanComment(otherAgent, ticket));
        assertThrows(PermissionDeniedException.class, () -> permissions.assertCanViewHistory(otherCustomer, ticket));
    }
    private AppUser user(UserRole role) { return AppUser.builder().id(UUID.randomUUID()).role(role).build(); }
}
