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
import com.company.supportticketing.domain.enums.UserRole;
import com.company.supportticketing.dto.request.AddCommentRequest;
import com.company.supportticketing.dto.response.CommentResponse;
import com.company.supportticketing.service.TicketService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock TicketService service;

    private CommentController controller;
    private AppUser customer;

    @BeforeEach
    void setUp() {
        controller = new CommentController(service);
        customer = AppUser.builder().id(UUID.randomUUID()).role(UserRole.CUSTOMER).build();
    }

    @Test
    void add_returnsCreatedStatusAndDelegatesToService() {
        UUID ticketId = UUID.randomUUID();
        AddCommentRequest request = new AddCommentRequest("Need an update");
        CommentResponse response = new CommentResponse(UUID.randomUUID(), ticketId, customer.getId(), "Customer", UserRole.CUSTOMER, "Need an update", Instant.now());
        when(service.addComment(customer, ticketId, request)).thenReturn(response);

        ResponseEntity<CommentResponse> result = controller.add(customer, ticketId, request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertSame(response, result.getBody());
        verify(service).addComment(customer, ticketId, request);
    }

    @Test
    void list_delegatesToService() {
        UUID ticketId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        Page<CommentResponse> page = new PageImpl<>(List.of());
        when(service.comments(customer, ticketId, pageable)).thenReturn(page);

        Page<CommentResponse> result = controller.list(customer, ticketId, pageable);

        assertSame(page, result);
        verify(service).comments(customer, ticketId, pageable);
    }
}
