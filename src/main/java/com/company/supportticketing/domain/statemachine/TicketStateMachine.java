package com.company.supportticketing.domain.statemachine;

import com.company.supportticketing.domain.enums.TicketStatus;
import com.company.supportticketing.exception.InvalidTransitionException;
import org.springframework.stereotype.Component;
import java.util.*;

/** Single source of truth for the transition table in 01-REQUIREMENTS.md TASK-010. */
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
