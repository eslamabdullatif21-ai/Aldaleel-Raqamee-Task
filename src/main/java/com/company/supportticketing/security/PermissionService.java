package com.company.supportticketing.security;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.company.supportticketing.domain.entity.AppUser;
import com.company.supportticketing.domain.entity.Ticket;
import com.company.supportticketing.domain.enums.TicketStatus;
import com.company.supportticketing.domain.enums.UserRole;
import com.company.supportticketing.exception.PermissionDeniedException;

/** Central resource-ownership policy for ticket operations. */
@Service
public class PermissionService {
    public void assertCanView(AppUser actor, Ticket ticket) {
        if (isCustomerOwner(actor, ticket) || isAssignedAgent(actor, ticket)) {
            return;
        }
        deny("You do not have access to this ticket");
    }

    public void assertCanAssign(AppUser actor) {
        if (actor.getRole() != UserRole.AGENT) {
            deny("Only support agents may assign tickets");
        }
    }

    public void assertCanRouteAssignment(AppUser actor, Ticket ticket, UUID targetAgentId) {
        boolean selfClaim = ticket.getAssignedAgent() == null
                && Objects.equals(actor.getId(), targetAgentId);
        if (!selfClaim && !isAssignedAgent(actor, ticket)) {
            deny("Agents may only claim unassigned tickets for themselves or reassign tickets assigned to them");
        }
    }

    public void assertCanUpdatePriority(AppUser actor, Ticket ticket) {
        if (!isAssignedAgent(actor, ticket)) {
            deny("Only the assigned agent may update priority");
        }
    }

    public void assertCanComment(AppUser actor, Ticket ticket) {
        if (isCustomerOwner(actor, ticket) || isAssignedAgent(actor, ticket)) {
            return;
        }
        deny("Only the ticket customer or assigned agent may access comments");
    }

    public void assertCanViewHistory(AppUser actor, Ticket ticket) {
        assertCanComment(actor, ticket);
    }

    public void assertCanUpdateStatus(AppUser actor, Ticket ticket, TicketStatus target) {
        if (isAssignedAgent(actor, ticket)) {
            return;
        }
        boolean customerResolutionAction = isCustomerOwner(actor, ticket)
                && ticket.getStatus() == TicketStatus.RESOLVED
                && (target == TicketStatus.CLOSED || target == TicketStatus.REOPENED);
        if (!customerResolutionAction) {
            deny("You cannot perform this status transition");
        }
    }

    boolean isCustomerOwner(AppUser actor, Ticket ticket) {
        return actor.getRole() == UserRole.CUSTOMER
                && Objects.equals(actor.getId(), ticket.getCustomer().getId());
    }

    boolean isAssignedAgent(AppUser actor, Ticket ticket) {
        return actor.getRole() == UserRole.AGENT
                && ticket.getAssignedAgent() != null
                && Objects.equals(actor.getId(), ticket.getAssignedAgent().getId());
    }

    private void deny(String message) {
        throw new PermissionDeniedException(message);
    }
}
