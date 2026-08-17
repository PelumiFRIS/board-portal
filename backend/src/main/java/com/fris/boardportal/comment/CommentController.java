package com.fris.boardportal.comment;

import com.fris.boardportal.comment.dto.CommentDto;
import com.fris.boardportal.comment.dto.CreateCommentRequest;
import com.fris.boardportal.security.AppUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public List<CommentDto> list(@AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam CommentEntityType entityType, @RequestParam UUID entityId) {
        return commentService.listForEntity(principal, entityType, entityId);
    }

    @PostMapping
    public ResponseEntity<CommentDto> create(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateCommentRequest request) {
        CommentDto created = commentService.create(principal, request.entityType(), request.entityId(), request.body());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        commentService.delete(principal, id);
        return ResponseEntity.noContent().build();
    }
}
