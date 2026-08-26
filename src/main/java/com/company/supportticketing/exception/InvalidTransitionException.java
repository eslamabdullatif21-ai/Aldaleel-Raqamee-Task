package com.company.supportticketing.exception;

import com.company.supportticketing.domain.enums.TicketStatus;
public class InvalidTransitionException extends RuntimeException {
    public InvalidTransitionException(TicketStatus from, TicketStatus to) { super("Cannot transition ticket from " + from + " to " + to); }
}
