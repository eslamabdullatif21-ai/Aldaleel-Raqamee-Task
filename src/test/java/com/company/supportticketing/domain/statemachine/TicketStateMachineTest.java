package com.company.supportticketing.domain.statemachine;

import com.company.supportticketing.domain.enums.TicketStatus;
import com.company.supportticketing.exception.InvalidTransitionException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class TicketStateMachineTest {
    private final TicketStateMachine machine = new TicketStateMachine();
    private static final Set<String> ALLOWED = Set.of("OPEN->IN_PROGRESS", "IN_PROGRESS->RESOLVED",
            "IN_PROGRESS->OPEN", "RESOLVED->CLOSED", "RESOLVED->REOPENED", "REOPENED->IN_PROGRESS");
    static Stream<Arguments> allPairs() {
        return Arrays.stream(TicketStatus.values()).flatMap(from -> Arrays.stream(TicketStatus.values()).map(to -> new Arguments(from, to)));
    }
    @ParameterizedTest @MethodSource("allPairs")
    void enforcesCompleteTransitionTable(Arguments pair) {
        boolean expected = ALLOWED.contains(pair.from + "->" + pair.to);
        assertEquals(expected, machine.canTransition(pair.from, pair.to));
        if (expected) assertDoesNotThrow(() -> machine.validateTransition(pair.from, pair.to));
        else assertThrows(InvalidTransitionException.class, () -> machine.validateTransition(pair.from, pair.to));
    }
    @Test void closedIsTerminal() {
        for (TicketStatus target : TicketStatus.values()) assertFalse(machine.canTransition(TicketStatus.CLOSED, target));
    }
    record Arguments(TicketStatus from, TicketStatus to) { }
}
