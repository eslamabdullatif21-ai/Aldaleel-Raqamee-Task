package com.company.supportticketing.domain.statemachine;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.company.supportticketing.domain.enums.TicketStatus;
import com.company.supportticketing.exception.InvalidTransitionException;

/** Single source of truth for the documented ticket-status transition table. */
@Component
public class TicketStateMachine {
    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED = Map.of(
            TicketStatus.OPEN, Set.of(TicketStatus.IN_PROGRESS),
            TicketStatus.IN_PROGRESS, Set.of(TicketStatus.RESOLVED, TicketStatus.OPEN),
            TicketStatus.RESOLVED, Set.of(TicketStatus.CLOSED, TicketStatus.REOPENED),
            TicketStatus.REOPENED, Set.of(TicketStatus.IN_PROGRESS),
            TicketStatus.CLOSED, Set.of());

    public boolean canTransition(TicketStatus from, TicketStatus to) {
        return from != null && to != null && ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }
    public void validateTransition(TicketStatus from, TicketStatus to) {
        if (!canTransition(from, to)) throw new InvalidTransitionException(from, to);
    }
}
