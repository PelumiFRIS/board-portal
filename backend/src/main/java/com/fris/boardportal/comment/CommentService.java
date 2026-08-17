package com.fris.boardportal.comment;

import com.fris.boardportal.actionitem.ActionItemRepository;
import com.fris.boardportal.audit.AuditAction;
import com.fris.boardportal.audit.AuditEntityType;
import com.fris.boardportal.audit.AuditLogService;
import com.fris.boardportal.comment.dto.CommentDto;
import com.fris.boardportal.common.ApiException;
import com.fris.boardportal.document.DocumentRepository;
import com.fris.boardportal.meeting.MeetingRepository;
import com.fris.boardportal.resolution.ResolutionRepository;
import com.fris.boardportal.security.AppUserPrincipal;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final MeetingRepository meetingRepository;
    private final ResolutionRepository resolutionRepository;
    private final DocumentRepository documentRepository;
    private final ActionItemRepository actionItemRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public CommentService(CommentRepository commentRepository, MeetingRepository meetingRepository,
            ResolutionRepository resolutionRepository, DocumentRepository documentRepository,
            ActionItemRepository actionItemRepository, UserRepository userRepository,
            AuditLogService auditLogService) {
        this.commentRepository = commentRepository;
        this.meetingRepository = meetingRepository;
        this.resolutionRepository = resolutionRepository;
        this.documentRepository = documentRepository;
        this.actionItemRepository = actionItemRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public List<CommentDto> listForEntity(AppUserPrincipal principal, CommentEntityType entityType, UUID entityId) {
        return commentRepository
                .findByOrganizationIdAndEntityTypeAndEntityIdOrderByCreatedAtAsc(
                        principal.getOrganizationId(), entityType, entityId)
                .stream()
                .map(CommentDto::from)
                .toList();
    }

    @Transactional
    public CommentDto create(AppUserPrincipal principal, CommentEntityType entityType, UUID entityId, String body) {
        String entityLabel = describeEntity(principal, entityType, entityId);

        Comment comment = Comment.create(principal.getOrganizationId(), entityType, entityId, principal.getUserId(),
                authorName(principal), body);
        commentRepository.save(comment);

        auditLogService.record(principal, AuditAction.COMMENT_POSTED, AuditEntityType.COMMENT, comment.getId(),
                "Commented on " + entityLabel);

        return CommentDto.from(comment);
    }

    @Transactional
    public void delete(AppUserPrincipal principal, UUID id) {
        Comment comment = commentRepository.findByIdAndOrganizationId(id, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Comment not found"));

        boolean isAdmin = principal.getRole() == Role.ADMIN;
        boolean isAuthor = comment.getAuthorId().equals(principal.getUserId());
        if (!isAdmin && !isAuthor) {
            throw ApiException.forbidden("Only the author or an admin can delete this comment");
        }

        commentRepository.delete(comment);

        auditLogService.record(principal, AuditAction.COMMENT_DELETED, AuditEntityType.COMMENT, comment.getId(),
                "Deleted a comment on " + describeEntity(principal, comment.getEntityType(), comment.getEntityId()));
    }

    private String authorName(AppUserPrincipal principal) {
        return userRepository.findById(principal.getUserId())
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse(principal.getUsername());
    }

    private String describeEntity(AppUserPrincipal principal, CommentEntityType entityType, UUID entityId) {
        UUID orgId = principal.getOrganizationId();
        return switch (entityType) {
            case MEETING -> "meeting \"" + meetingRepository.findByIdAndOrganizationId(entityId, orgId)
                    .orElseThrow(() -> ApiException.notFound("Meeting not found"))
                    .getTitle() + "\"";
            case RESOLUTION -> "resolution \"" + resolutionRepository.findByIdAndOrganizationId(entityId, orgId)
                    .orElseThrow(() -> ApiException.notFound("Resolution not found"))
                    .getTitle() + "\"";
            case DOCUMENT -> "document \"" + documentRepository.findByIdAndOrganizationId(entityId, orgId)
                    .orElseThrow(() -> ApiException.notFound("Document not found"))
                    .getTitle() + "\"";
            case ACTION_ITEM -> "action item \"" + actionItemRepository.findByIdAndOrganizationId(entityId, orgId)
                    .orElseThrow(() -> ApiException.notFound("Action item not found"))
                    .getTitle() + "\"";
        };
    }
}
