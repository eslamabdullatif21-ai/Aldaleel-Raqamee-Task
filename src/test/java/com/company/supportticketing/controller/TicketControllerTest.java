package com.company.supportticketing.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.company.supportticketing.domain.entity.AppUser;
import com.company.supportticketing.domain.enums.HistoryEventType;
import com.company.supportticketing.domain.enums.TicketCategory;
import com.company.supportticketing.domain.enums.TicketPriority;
import com.company.supportticketing.domain.enums.TicketStatus;
import com.company.supportticketing.domain.enums.UserRole;
import com.company.supportticketing.dto.request.AssignTicketRequest;
import com.company.supportticketing.dto.request.CreateTicketRequest;
import com.company.supportticketing.dto.request.UpdatePriorityRequest;
import com.company.supportticketing.dto.request.UpdateStatusRequest;
import com.company.supportticketing.dto.response.HistoryResponse;
import com.company.supportticketing.dto.response.TicketResponse;
import com.company.supportticketing.service.TicketService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketControllerTest {

    @Mock TicketService service;

    private TicketController controller;
    private AppUser customer;
    private AppUser agent;

    @BeforeEach
    void setUp() {
        controller = new TicketController(service);
        customer = AppUser.builder().id(UUID.randomUUID()).role(UserRole.CUSTOMER).build();
        agent = AppUser.builder().id(UUID.randomUUID()).role(UserRole.AGENT).build();
    }

    @Test
    void create_returnsCreatedStatusAndDelegatesToService() {
        CreateTicketRequest request = new CreateTicketRequest("Title", "Desc", TicketCategory.TECHNICAL, TicketPriority.HIGH);
        TicketResponse response = new TicketResponse(UUID.randomUUID(), "Title", "Desc", TicketCategory.TECHNICAL, TicketPriority.HIGH, TicketStatus.OPEN, customer.getId(), null, Instant.now(), Instant.now());
        when(service.create(customer, request)).thenReturn(response);

        ResponseEntity<TicketResponse> result = controller.create(customer, request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertSame(response, result.getBody());
        verify(service).create(customer, request);
    }

    @Test
    void get_delegatesToService() {
        UUID id = UUID.randomUUID();
        TicketResponse response = new TicketResponse(id, "Title", "Desc", TicketCategory.TECHNICAL, TicketPriority.MEDIUM, TicketStatus.OPEN, customer.getId(), null, Instant.now(), Instant.now());
        when(service.get(customer, id)).thenReturn(response);

        TicketResponse result = controller.get(customer, id);

        assertSame(response, result);
        verify(service).get(customer, id);
    }

    @Test
    void list_delegatesToService() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<TicketResponse> page = new PageImpl<>(List.of());
        when(service.list(customer, TicketStatus.OPEN, TicketPriority.HIGH, TicketCategory.TECHNICAL, pageable)).thenReturn(page);

        Page<TicketResponse> result = controller.list(customer, TicketStatus.OPEN, TicketPriority.HIGH, TicketCategory.TECHNICAL, pageable);

        assertSame(page, result);
        verify(service).list(customer, TicketStatus.OPEN, TicketPriority.HIGH, TicketCategory.TECHNICAL, pageable);
    }

    @Test
    void unassigned_delegatesToService() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<TicketResponse> page = new PageImpl<>(List.of());
        when(service.unassigned(
                agent,
                TicketStatus.OPEN,
                TicketPriority.HIGH,
                TicketCategory.TECHNICAL,
                pageable)).thenReturn(page);

        Page<TicketResponse> result = controller.unassigned(
                agent,
                TicketStatus.OPEN,
                TicketPriority.HIGH,
                TicketCategory.TECHNICAL,
                pageable);

        assertSame(page, result);
        verify(service).unassigned(
                agent,
                TicketStatus.OPEN,
                TicketPriority.HIGH,
                TicketCategory.TECHNICAL,
                pageable);
    }

    @Test
    void assign_delegatesToService() {
        UUID id = UUID.randomUUID();
        AssignTicketRequest request = new AssignTicketRequest(agent.getId());
        TicketResponse response = new TicketResponse(id, "Title", "Desc", TicketCategory.TECHNICAL, TicketPriority.MEDIUM, TicketStatus.OPEN, customer.getId(), agent.getId(), Instant.now(), Instant.now());
        when(service.assign(agent, id, request)).thenReturn(response);

        TicketResponse result = controller.assign(agent, id, request);

        assertSame(response, result);
        verify(service).assign(agent, id, request);
    }

    @Test
    void status_delegatesToService() {
        UUID id = UUID.randomUUID();
        UpdateStatusRequest request = new UpdateStatusRequest(TicketStatus.IN_PROGRESS, "Starting");
        TicketResponse response = new TicketResponse(id, "Title", "Desc", TicketCategory.TECHNICAL, TicketPriority.MEDIUM, TicketStatus.IN_PROGRESS, customer.getId(), agent.getId(), Instant.now(), Instant.now());
        when(service.updateStatus(agent, id, request)).thenReturn(response);

        TicketResponse result = controller.status(agent, id, request);

        assertSame(response, result);
        verify(service).updateStatus(agent, id, request);
    }

    @Test
    void priority_delegatesToService() {
        UUID id = UUID.randomUUID();
        UpdatePriorityRequest request = new UpdatePriorityRequest(TicketPriority.URGENT);
        TicketResponse response = new TicketResponse(id, "Title", "Desc", TicketCategory.TECHNICAL, TicketPriority.URGENT, TicketStatus.OPEN, customer.getId(), agent.getId(), Instant.now(), Instant.now());
        when(service.updatePriority(agent, id, request)).thenReturn(response);

        TicketResponse result = controller.priority(agent, id, request);

        assertSame(response, result);
        verify(service).updatePriority(agent, id, request);
    }

    @Test
    void history_delegatesToService() {
        UUID id = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        HistoryResponse item = new HistoryResponse(UUID.randomUUID(), id, HistoryEventType.STATUS_CHANGE, null, TicketStatus.OPEN, null, null, customer.getId(), Instant.now(), "Created");
        Page<HistoryResponse> page = new PageImpl<>(List.of(item));
        when(service.history(customer, id, pageable)).thenReturn(page);

        Page<HistoryResponse> result = controller.history(customer, id, pageable);

        assertSame(page, result);
        verify(service).history(customer, id, pageable);
    }
}
