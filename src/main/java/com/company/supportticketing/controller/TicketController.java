package com.company.supportticketing.controller;

import com.company.supportticketing.domain.entity.AppUser;
import com.company.supportticketing.domain.enums.*;
import com.company.supportticketing.dto.request.*;
import com.company.supportticketing.dto.response.*;
import com.company.supportticketing.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/tickets") @RequiredArgsConstructor
public class TicketController {
    private final TicketService service;
    @PostMapping @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<TicketResponse> create(@AuthenticationPrincipal AppUser actor, @Valid @RequestBody CreateTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(actor, request));
    }
    @GetMapping("/{id}") public TicketResponse get(@AuthenticationPrincipal AppUser actor, @PathVariable UUID id) { return service.get(actor, id); }
    @GetMapping public Page<TicketResponse> list(@AuthenticationPrincipal AppUser actor,
            @RequestParam(required=false) TicketStatus status, @RequestParam(required=false) TicketPriority priority,
            @RequestParam(required=false) TicketCategory category, @PageableDefault(size=20, sort="createdAt", direction=Sort.Direction.DESC) Pageable pageable) {
        return service.list(actor, status, priority, category, pageable);
    }
    @PatchMapping("/{id}/assign") @PreAuthorize("hasRole('AGENT')")
    public TicketResponse assign(@AuthenticationPrincipal AppUser actor, @PathVariable UUID id, @Valid @RequestBody AssignTicketRequest request) { return service.assign(actor, id, request); }
    @PatchMapping("/{id}/status")
    public TicketResponse status(@AuthenticationPrincipal AppUser actor, @PathVariable UUID id, @Valid @RequestBody UpdateStatusRequest request) { return service.updateStatus(actor, id, request); }
    @PatchMapping("/{id}/priority") @PreAuthorize("hasRole('AGENT')")
    public TicketResponse priority(@AuthenticationPrincipal AppUser actor, @PathVariable UUID id, @Valid @RequestBody UpdatePriorityRequest request) { return service.updatePriority(actor, id, request); }
    @GetMapping("/{id}/history") public List<HistoryResponse> history(@AuthenticationPrincipal AppUser actor, @PathVariable UUID id) { return service.history(actor, id); }
}
