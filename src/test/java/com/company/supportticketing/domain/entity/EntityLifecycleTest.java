package com.company.supportticketing.domain.entity;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.company.supportticketing.domain.enums.HistoryEventType;
import com.company.supportticketing.domain.enums.TicketCategory;
import com.company.supportticketing.domain.enums.TicketPriority;
import com.company.supportticketing.domain.enums.TicketStatus;
import com.company.supportticketing.domain.enums.UserRole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityLifecycleTest {

    @Test
    void appUser_prePersistAndAuthorities() {
        AppUser user = AppUser.builder()
                .email("test@example.com")
                .passwordHash("hash")
                .name("Test")
                .role(UserRole.CUSTOMER)
                .build();

        user.onCreate();

        assertNotNull(user.getCreatedAt());
        assertEquals("test@example.com", user.getUsername());
        assertEquals("hash", user.getPassword());
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());
        assertEquals(1, user.getAuthorities().size());
        assertEquals("ROLE_CUSTOMER", user.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void ticket_prePersistAndPreUpdate() {
        Ticket ticket = Ticket.builder()
                .title("Title")
                .description("Desc")
                .category(TicketCategory.TECHNICAL)
                .priority(TicketPriority.MEDIUM)
                .status(TicketStatus.OPEN)
                .customer(AppUser.builder().id(UUID.randomUUID()).build())
                .build();

        ticket.onCreate();
        assertNotNull(ticket.getCreatedAt());
        assertNotNull(ticket.getUpdatedAt());

        ticket.onUpdate();
        assertNotNull(ticket.getUpdatedAt());
    }

    @Test
    void ticketComment_prePersist() {
        TicketComment comment = TicketComment.builder()
                .ticket(Ticket.builder().id(UUID.randomUUID()).build())
                .author(AppUser.builder().id(UUID.randomUUID()).build())
                .body("Text")
                .build();

        comment.onCreate();
        assertNotNull(comment.getCreatedAt());
    }

    @Test
    void statusHistory_prePersist() {
        StatusHistory history = StatusHistory.builder()
                .ticket(Ticket.builder().id(UUID.randomUUID()).build())
                .eventType(HistoryEventType.STATUS_CHANGE)
                .changedBy(AppUser.builder().id(UUID.randomUUID()).build())
                .build();

        history.onCreate();
        assertNotNull(history.getChangedAt());
    }
}
