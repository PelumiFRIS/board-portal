package com.fris.boardportal.resolution;

import com.fris.boardportal.audit.AuditAction;
import com.fris.boardportal.audit.AuditEntityType;
import com.fris.boardportal.audit.AuditLogService;
import com.fris.boardportal.common.ApiException;
import com.fris.boardportal.meeting.MeetingRepository;
import com.fris.boardportal.resolution.dto.CreateResolutionRequest;
import com.fris.boardportal.resolution.dto.ResolutionDetail;
import com.fris.boardportal.resolution.dto.ResolutionSummary;
import com.fris.boardportal.resolution.dto.VoteRecord;
import com.fris.boardportal.security.AppUserPrincipal;
import com.fris.boardportal.user.User;
import com.fris.boardportal.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResolutionService {

    private final ResolutionRepository resolutionRepository;
    private final VoteRepository voteRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public ResolutionService(ResolutionRepository resolutionRepository, VoteRepository voteRepository,
            MeetingRepository meetingRepository, UserRepository userRepository, AuditLogService auditLogService) {
        this.resolutionRepository = resolutionRepository;
        this.voteRepository = voteRepository;
        this.meetingRepository = meetingRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public List<ResolutionSummary> listForOrganization(AppUserPrincipal principal, UUID meetingId) {
        List<Resolution> resolutions = resolutionRepository.findByOrganizationIdOrderByCreatedAtDesc(
                principal.getOrganizationId());
        return resolutions.stream()
                .filter(r -> meetingId == null || meetingId.equals(r.getMeetingId()))
                .map(r -> toSummary(r, principal))
                .toList();
    }

    public List<ResolutionSummary> listForMeeting(AppUserPrincipal principal, UUID meetingId) {
        return resolutionRepository.findByMeetingIdOrderByCreatedAtDesc(meetingId).stream()
                .map(r -> toSummary(r, principal))
                .toList();
    }

    public ResolutionDetail getDetail(AppUserPrincipal principal, UUID id) {
        Resolution resolution = findInOrg(principal, id);
        List<Vote> votes = voteRepository.findByResolutionId(resolution.getId());
        return toDetail(resolution, votes, principal);
    }

    @Transactional
    public ResolutionSummary create(AppUserPrincipal admin, CreateResolutionRequest request) {
        meetingRepository.findByIdAndOrganizationId(request.meetingId(), admin.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Meeting not found"));

        Resolution resolution = Resolution.create(admin.getOrganizationId(), request.meetingId(), request.title(),
                request.description(), admin.getUserId());
        resolutionRepository.save(resolution);

        auditLogService.record(admin, AuditAction.RESOLUTION_CREATED, AuditEntityType.RESOLUTION, resolution.getId(),
                "Proposed resolution \"" + resolution.getTitle() + "\"");

        return toSummary(resolution, admin);
    }

    @Transactional
    public ResolutionSummary open(AppUserPrincipal admin, UUID id) {
        Resolution resolution = findInOrg(admin, id);
        if (resolution.getStatus() != ResolutionStatus.DRAFT) {
            throw ApiException.badRequest("Only a draft resolution can be opened for voting");
        }
        resolution.setStatus(ResolutionStatus.OPEN);
        resolution.setOpenedAt(Instant.now());
        resolutionRepository.save(resolution);

        auditLogService.record(admin, AuditAction.RESOLUTION_OPENED, AuditEntityType.RESOLUTION, resolution.getId(),
                "Opened \"" + resolution.getTitle() + "\" for voting");

        return toSummary(resolution, admin);
    }

    @Transactional
    public ResolutionSummary close(AppUserPrincipal admin, UUID id) {
        Resolution resolution = findInOrg(admin, id);
        if (resolution.getStatus() != ResolutionStatus.OPEN) {
            throw ApiException.badRequest("Only an open resolution can be closed");
        }
        List<Vote> votes = voteRepository.findByResolutionId(resolution.getId());
        long forCount = votes.stream().filter(v -> v.getChoice() == VoteChoice.FOR).count();
        long againstCount = votes.stream().filter(v -> v.getChoice() == VoteChoice.AGAINST).count();

        resolution.setStatus(ResolutionStatus.CLOSED);
        resolution.setOutcome(forCount > againstCount ? ResolutionOutcome.PASSED : ResolutionOutcome.FAILED);
        resolution.setClosedAt(Instant.now());
        resolutionRepository.save(resolution);

        auditLogService.record(admin, AuditAction.RESOLUTION_CLOSED, AuditEntityType.RESOLUTION, resolution.getId(),
                "Closed voting on \"" + resolution.getTitle() + "\" — " + resolution.getOutcome());

        return toSummary(resolution, admin);
    }

    @Transactional
    public ResolutionSummary castVote(AppUserPrincipal principal, UUID id, VoteChoice choice) {
        Resolution resolution = findInOrg(principal, id);
        if (resolution.getStatus() != ResolutionStatus.OPEN) {
            throw ApiException.badRequest("Voting is not open for this resolution");
        }

        Vote vote = voteRepository.findByResolutionIdAndUserId(resolution.getId(), principal.getUserId())
                .orElseGet(() -> Vote.create(resolution.getId(), principal.getUserId(), choice));
        vote.setChoice(choice);
        vote.setCastAt(Instant.now());
        voteRepository.save(vote);

        auditLogService.record(principal, AuditAction.VOTE_CAST, AuditEntityType.RESOLUTION, resolution.getId(),
                "Voted " + choice + " on \"" + resolution.getTitle() + "\"");

        return toSummary(resolution, principal);
    }

    @Transactional
    public void delete(AppUserPrincipal admin, UUID id) {
        Resolution resolution = findInOrg(admin, id);
        if (resolution.getStatus() != ResolutionStatus.DRAFT) {
            throw ApiException.badRequest("Only a draft resolution can be deleted");
        }
        resolutionRepository.delete(resolution);

        auditLogService.record(admin, AuditAction.RESOLUTION_DELETED, AuditEntityType.RESOLUTION, resolution.getId(),
                "Deleted draft resolution \"" + resolution.getTitle() + "\"");
    }

    private Resolution findInOrg(AppUserPrincipal principal, UUID id) {
        return resolutionRepository.findByIdAndOrganizationId(id, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Resolution not found"));
    }

    private ResolutionSummary toSummary(Resolution r, AppUserPrincipal principal) {
        List<Vote> votes = voteRepository.findByResolutionId(r.getId());
        long forCount = votes.stream().filter(v -> v.getChoice() == VoteChoice.FOR).count();
        long againstCount = votes.stream().filter(v -> v.getChoice() == VoteChoice.AGAINST).count();
        long abstainCount = votes.stream().filter(v -> v.getChoice() == VoteChoice.ABSTAIN).count();
        VoteChoice myVote = votes.stream()
                .filter(v -> v.getUserId().equals(principal.getUserId()))
                .map(Vote::getChoice)
                .findFirst()
                .orElse(null);
        return new ResolutionSummary(r.getId(), r.getMeetingId(), r.getTitle(), r.getDescription(), r.getStatus(),
                r.getOutcome(), forCount, againstCount, abstainCount, myVote, r.getCreatedAt(), r.getOpenedAt(),
                r.getClosedAt());
    }

    private ResolutionDetail toDetail(Resolution r, List<Vote> votes, AppUserPrincipal principal) {
        Map<UUID, User> usersById = userRepository.findByOrganizationId(r.getOrganizationId()).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        long forCount = votes.stream().filter(v -> v.getChoice() == VoteChoice.FOR).count();
        long againstCount = votes.stream().filter(v -> v.getChoice() == VoteChoice.AGAINST).count();
        long abstainCount = votes.stream().filter(v -> v.getChoice() == VoteChoice.ABSTAIN).count();
        VoteChoice myVote = votes.stream()
                .filter(v -> v.getUserId().equals(principal.getUserId()))
                .map(Vote::getChoice)
                .findFirst()
                .orElse(null);

        List<VoteRecord> voteRecords = votes.stream()
                .map(v -> {
                    User voter = usersById.get(v.getUserId());
                    String voterName = voter != null ? voter.getFirstName() + " " + voter.getLastName() : "Unknown";
                    return new VoteRecord(v.getUserId(), voterName, v.getChoice(), v.getCastAt());
                })
                .toList();

        return new ResolutionDetail(r.getId(), r.getMeetingId(), r.getTitle(), r.getDescription(), r.getStatus(),
                r.getOutcome(), forCount, againstCount, abstainCount, myVote, r.getCreatedAt(), r.getOpenedAt(),
                r.getClosedAt(), voteRecords);
    }
}
