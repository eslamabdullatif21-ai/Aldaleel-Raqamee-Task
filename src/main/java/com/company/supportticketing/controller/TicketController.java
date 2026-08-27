package com.company.supportticketing.controller;

import java.util.UUID;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.company.supportticketing.domain.entity.AppUser;
import com.company.supportticketing.domain.enums.TicketCategory;
import com.company.supportticketing.domain.enums.TicketPriority;
import com.company.supportticketing.domain.enums.TicketStatus;
import com.company.supportticketing.dto.request.AssignTicketRequest;
import com.company.supportticketing.dto.request.CreateTicketRequest;
import com.company.supportticketing.dto.request.UpdatePriorityRequest;
import com.company.supportticketing.dto.request.UpdateStatusRequest;
import com.company.supportticketing.dto.response.HistoryResponse;
import com.company.supportticketing.dto.response.TicketResponse;
import com.company.supportticketing.service.TicketService;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService service;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<TicketResponse> create(
            @AuthenticationPrincipal AppUser actor,
            @Valid @RequestBody CreateTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(actor, request));
    }

    @GetMapping("/{id}")
    public TicketResponse get(
            @AuthenticationPrincipal AppUser actor,
            @PathVariable UUID id) {
        return service.get(actor, id);
    }

    @GetMapping
    public Page<TicketResponse> list(
            @AuthenticationPrincipal AppUser actor,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) TicketCategory category,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return service.list(actor, status, priority, category, pageable);
    }

    @GetMapping("/unassigned")
    @PreAuthorize("hasRole('AGENT')")
    public Page<TicketResponse> unassigned(
            @AuthenticationPrincipal AppUser actor,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) TicketCategory category,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return service.unassigned(actor, status, priority, category, pageable);
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('AGENT')")
    public TicketResponse assign(
            @AuthenticationPrincipal AppUser actor,
            @PathVariable UUID id,
            @Valid @RequestBody AssignTicketRequest request) {
        return service.assign(actor, id, request);
    }

    @PatchMapping("/{id}/status")
    public TicketResponse status(
            @AuthenticationPrincipal AppUser actor,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request) {
        return service.updateStatus(actor, id, request);
    }

    @PatchMapping("/{id}/priority")
    @PreAuthorize("hasRole('AGENT')")
    public TicketResponse priority(
            @AuthenticationPrincipal AppUser actor,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePriorityRequest request) {
        return service.updatePriority(actor, id, request);
    }

    @GetMapping("/{id}/history")
    public Page<HistoryResponse> history(
            @AuthenticationPrincipal AppUser actor,
            @PathVariable UUID id,
            @PageableDefault(size = 20, sort = "changedAt", direction = Sort.Direction.ASC)
                    Pageable pageable) {
        return service.history(actor, id, pageable);
    }
}
