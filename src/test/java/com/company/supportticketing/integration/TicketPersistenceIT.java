package com.company.supportticketing.integration;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.company.supportticketing.domain.entity.AppUser;
import com.company.supportticketing.domain.enums.TicketCategory;
import com.company.supportticketing.domain.enums.TicketStatus;
import com.company.supportticketing.domain.enums.UserRole;
import com.company.supportticketing.dto.request.AssignTicketRequest;
import com.company.supportticketing.dto.request.CreateTicketRequest;
import com.company.supportticketing.dto.request.UpdateStatusRequest;
import com.company.supportticketing.exception.InvalidTransitionException;
import com.company.supportticketing.exception.PermissionDeniedException;
import com.company.supportticketing.repository.UserRepository;
import com.company.supportticketing.service.TicketService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class TicketPersistenceIT {
    private static final String TEST_JWT_SECRET =
            "integration-test-secret-containing-more-than-thirty-two-bytes";

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> 4);
        registry.add("spring.datasource.hikari.minimum-idle", () -> 1);
        registry.add("spring.threads.virtual.enabled", () -> false);
        registry.add("app.jwt.secret", () -> TEST_JWT_SECRET);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired TicketService ticketService;
    @Autowired UserRepository users;

    @BeforeEach
    void clearDomainData() {
        jdbc.execute("TRUNCATE TABLE comments, status_history, tickets, users CASCADE");
    }

    @Test
    void flywayMigrationAndHibernateValidationSucceedOnPostgreSql() {
        Integer successfulMigrations = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success",
                Integer.class);
        String usersTable = jdbc.queryForObject(
                "SELECT to_regclass('public.users')::text",
                String.class);

        assertEquals(1, successfulMigrations);
        assertEquals("users", usersTable);
    }

    @Test
    void ticketWorkflowIsAtomicAndPermissionScopedOnPostgreSql() {
        AppUser customer = saveUser("customer-it@example.com", UserRole.CUSTOMER);
        AppUser otherCustomer = saveUser("other-it@example.com", UserRole.CUSTOMER);
        AppUser agent = saveUser("agent-it@example.com", UserRole.AGENT);

        var created = ticketService.create(
                customer,
                new CreateTicketRequest(
                        "PostgreSQL workflow",
                        "Real persistence verification",
                        TicketCategory.TECHNICAL,
                        null));

        assertEquals(TicketStatus.OPEN, created.status());
        assertEquals(1, historyCount(created.id()));
        assertThrows(
                PermissionDeniedException.class,
                () -> ticketService.get(otherCustomer, created.id()));

        ticketService.assign(agent, created.id(), new AssignTicketRequest(null));
        assertEquals(2, historyCount(created.id()));

        assertThrows(
                InvalidTransitionException.class,
                () -> ticketService.updateStatus(
                        agent,
                        created.id(),
                        new UpdateStatusRequest(TicketStatus.RESOLVED, "Invalid shortcut")));
        assertEquals(TicketStatus.OPEN.name(), persistedStatus(created.id()));
        assertEquals(2, historyCount(created.id()));

        ticketService.updateStatus(
                agent,
                created.id(),
                new UpdateStatusRequest(TicketStatus.IN_PROGRESS, "Investigation started"));

        assertEquals(TicketStatus.IN_PROGRESS.name(), persistedStatus(created.id()));
        assertEquals(3, historyCount(created.id()));
        assertTrue(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM status_history "
                        + "WHERE ticket_id = ? AND from_status = 'OPEN' "
                        + "AND to_status = 'IN_PROGRESS')",
                Boolean.class,
                created.id()));
    }

    @Test
    void unassignedQueueIsAgentOnlyAndRepeatedClaimIsIdempotent() {
        AppUser customer = saveUser("queue-customer-it@example.com", UserRole.CUSTOMER);
        AppUser agent = saveUser("queue-agent-it@example.com", UserRole.AGENT);

        var available = ticketService.create(
                customer,
                new CreateTicketRequest(
                        "Available ticket",
                        "Waiting for an agent",
                        TicketCategory.GENERAL,
                        null));
        var alreadyAssigned = ticketService.create(
                customer,
                new CreateTicketRequest(
                        "Assigned ticket",
                        "Already claimed",
                        TicketCategory.ACCOUNT,
                        null));
        ticketService.assign(agent, alreadyAssigned.id(), new AssignTicketRequest(null));

        var queue = ticketService.unassigned(
                agent,
                null,
                null,
                null,
                PageRequest.of(0, 20));

        assertEquals(1, queue.getTotalElements());
        assertEquals(available.id(), queue.getContent().getFirst().id());
        assertThrows(
                PermissionDeniedException.class,
                () -> ticketService.unassigned(
                        customer,
                        null,
                        null,
                        null,
                        PageRequest.of(0, 20)));

        ticketService.assign(agent, available.id(), new AssignTicketRequest(null));
        ticketService.assign(agent, available.id(), new AssignTicketRequest(null));

        assertEquals(2, historyCount(available.id()));
        assertEquals(0, ticketService.unassigned(
                agent,
                null,
                null,
                null,
                PageRequest.of(0, 20)).getTotalElements());
    }

    private AppUser saveUser(String email, UserRole role) {
        return users.save(AppUser.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Integration123!"))
                .name(email)
                .role(role)
                .build());
    }

    private int historyCount(UUID ticketId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM status_history WHERE ticket_id = ?",
                Integer.class,
                ticketId);
    }

    private String persistedStatus(UUID ticketId) {
        return jdbc.queryForObject(
                "SELECT status FROM tickets WHERE id = ?",
                String.class,
                ticketId);
    }
}
