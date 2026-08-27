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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.company.supportticketing.domain.entity.AppUser;
import com.company.supportticketing.dto.request.AddCommentRequest;
import com.company.supportticketing.dto.response.CommentResponse;
import com.company.supportticketing.service.TicketService;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
@RequiredArgsConstructor
public class CommentController {
    private final TicketService service;

    @PostMapping
    public ResponseEntity<CommentResponse> add(
            @AuthenticationPrincipal AppUser actor,
            @PathVariable UUID ticketId,
            @Valid @RequestBody AddCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.addComment(actor, ticketId, request));
    }

    @GetMapping
    public Page<CommentResponse> list(
            @AuthenticationPrincipal AppUser actor,
            @PathVariable UUID ticketId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC)
                    Pageable pageable) {
        return service.comments(actor, ticketId, pageable);
    }
}
