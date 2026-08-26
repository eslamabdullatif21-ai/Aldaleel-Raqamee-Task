package com.company.supportticketing.controller;

import com.company.supportticketing.domain.entity.AppUser;
import com.company.supportticketing.dto.request.AddCommentRequest;
import com.company.supportticketing.dto.response.CommentResponse;
import com.company.supportticketing.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/tickets/{ticketId}/comments") @RequiredArgsConstructor
public class CommentController {
    private final TicketService service;
    @PostMapping public ResponseEntity<CommentResponse> add(@AuthenticationPrincipal AppUser actor, @PathVariable UUID ticketId,
            @Valid @RequestBody AddCommentRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.addComment(actor, ticketId, request)); }
    @GetMapping public List<CommentResponse> list(@AuthenticationPrincipal AppUser actor, @PathVariable UUID ticketId) { return service.comments(actor, ticketId); }
}
