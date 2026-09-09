package com.fris.boardportal.messaging;

import com.fris.boardportal.messaging.dto.ConversationSummary;
import com.fris.boardportal.messaging.dto.CreateConversationRequest;
import com.fris.boardportal.messaging.dto.MessageDto;
import com.fris.boardportal.messaging.dto.SendMessageRequest;
import com.fris.boardportal.messaging.dto.ToggleReactionRequest;
import com.fris.boardportal.messaging.dto.UnreadCountResponse;
import com.fris.boardportal.security.AppUserPrincipal;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping("/{id}/messages/{messageId}/reactions")
    public MessageDto toggleReaction(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id, @PathVariable UUID messageId, @Valid @RequestBody ToggleReactionRequest request) {
        return conversationService.toggleReaction(principal, id, messageId, request.emoji());
    }

    @PostMapping("/{id}/messages/{messageId}/important")
    public MessageDto toggleImportant(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id, @PathVariable UUID messageId) {
        return conversationService.toggleImportant(principal, id, messageId);
    }

    @PostMapping("/{id}/mute")
    public ConversationSummary toggleMute(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        return conversationService.toggleMute(principal, id);
    }

    @PostMapping("/{id}/messages/{messageId}/attachment")
    public MessageDto uploadAttachment(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id,
            @PathVariable UUID messageId, @RequestParam("file") MultipartFile file) {
        return conversationService.uploadAttachment(principal, id, messageId, file);
    }

    @GetMapping("/{id}/messages/{messageId}/attachment")
    public ResponseEntity<byte[]> downloadAttachment(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id, @PathVariable UUID messageId) {
        MessageAttachment attachment = conversationService.getAttachment(principal, id, messageId);
        String encodedName = URLEncoder.encode(attachment.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .body(attachment.getFileData());
    }
}
