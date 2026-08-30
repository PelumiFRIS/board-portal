package com.fris.boardportal.messaging;

import com.fris.boardportal.messaging.dto.ConversationSummary;
import com.fris.boardportal.messaging.dto.CreateConversationRequest;
import com.fris.boardportal.messaging.dto.MessageDto;
import com.fris.boardportal.messaging.dto.SendMessageRequest;
import com.fris.boardportal.messaging.dto.UnreadCountResponse;
import com.fris.boardportal.security.AppUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public List<ConversationSummary> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return conversationService.listConversations(principal);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal AppUserPrincipal principal) {
        return new UnreadCountResponse(conversationService.unreadCount(principal));
    }

    @PostMapping
    public ResponseEntity<ConversationSummary> create(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateConversationRequest request) {
        ConversationSummary created = conversationService.createConversation(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}/messages")
    public List<MessageDto> listMessages(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        return conversationService.listMessages(principal, id);
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageDto> sendMessage(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id, @Valid @RequestBody SendMessageRequest request) {
        MessageDto created = conversationService.sendMessage(principal, id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
