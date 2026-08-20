package com.fris.boardportal.committee;

import com.fris.boardportal.audit.AuditAction;
import com.fris.boardportal.audit.AuditEntityType;
import com.fris.boardportal.audit.AuditLogService;
import com.fris.boardportal.committee.dto.CommitteeMemberDto;
import com.fris.boardportal.committee.dto.CommitteeSummary;
import com.fris.boardportal.common.ApiException;
import com.fris.boardportal.security.AppUserPrincipal;
import com.fris.boardportal.user.User;
import com.fris.boardportal.user.UserRepository;
import com.fris.boardportal.user.dto.MemberCommitteeSummary;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommitteeService {

    private final CommitteeRepository committeeRepository;
    private final CommitteeMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public CommitteeService(CommitteeRepository committeeRepository, CommitteeMembershipRepository membershipRepository,
            UserRepository userRepository, AuditLogService auditLogService) {
        this.committeeRepository = committeeRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public List<CommitteeSummary> listForOrganization(AppUserPrincipal principal) {
        return committeeRepository.findByOrganizationIdOrderByNameAsc(principal.getOrganizationId()).stream()
                .map(this::toSummary)
                .toList();
    }

    public List<MemberCommitteeSummary> committeesForUser(UUID userId) {
        return membershipRepository.findByUserId(userId).stream()
                .map(m -> {
                    Committee committee = committeeRepository.findById(m.getCommitteeId()).orElse(null);
                    if (committee == null) return null;
                    return new MemberCommitteeSummary(committee.getId(), committee.getName(), m.isChair());
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional
    public CommitteeSummary create(AppUserPrincipal admin, String name, String description) {
        Committee committee = Committee.create(admin.getOrganizationId(), name, description);
        committeeRepository.save(committee);

        auditLogService.record(admin, AuditAction.COMMITTEE_CREATED, AuditEntityType.COMMITTEE, committee.getId(),
                "Created committee \"" + committee.getName() + "\"");

        return toSummary(committee);
    }

    @Transactional
    public CommitteeSummary update(AppUserPrincipal admin, UUID committeeId, String name, String description) {
        Committee committee = findInOrg(admin, committeeId);
        if (name != null) {
            committee.setName(name);
        }
        if (description != null) {
            committee.setDescription(description);
        }
        committee.setUpdatedAt(Instant.now());
        committeeRepository.save(committee);

        auditLogService.record(admin, AuditAction.COMMITTEE_UPDATED, AuditEntityType.COMMITTEE, committee.getId(),
                "Updated committee \"" + committee.getName() + "\"");

        return toSummary(committee);
    }

    @Transactional
    public void delete(AppUserPrincipal admin, UUID committeeId) {
        Committee committee = findInOrg(admin, committeeId);
        membershipRepository.deleteByCommitteeId(committee.getId());
        committeeRepository.delete(committee);

        auditLogService.record(admin, AuditAction.COMMITTEE_DELETED, AuditEntityType.COMMITTEE, committee.getId(),
                "Deleted committee \"" + committee.getName() + "\"");
    }

    @Transactional
    public CommitteeSummary addMember(AppUserPrincipal admin, UUID committeeId, UUID userId) {
        Committee committee = findInOrg(admin, committeeId);
        User user = userRepository.findByIdAndOrganizationId(userId, admin.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("User not found"));

        if (membershipRepository.findByCommitteeIdAndUserId(committee.getId(), user.getId()).isEmpty()) {
            membershipRepository.save(CommitteeMembership.create(committee.getId(), user.getId()));
            auditLogService.record(admin, AuditAction.COMMITTEE_MEMBERSHIP_CHANGED, AuditEntityType.COMMITTEE,
                    committee.getId(), "Added " + user.getFirstName() + " " + user.getLastName() + " to \""
                            + committee.getName() + "\"");
        }

        return toSummary(committee);
    }

    @Transactional
    public CommitteeSummary removeMember(AppUserPrincipal admin, UUID committeeId, UUID userId) {
        Committee committee = findInOrg(admin, committeeId);
        User user = userRepository.findByIdAndOrganizationId(userId, admin.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("User not found"));

        membershipRepository.deleteByCommitteeIdAndUserId(committee.getId(), user.getId());

        auditLogService.record(admin, AuditAction.COMMITTEE_MEMBERSHIP_CHANGED, AuditEntityType.COMMITTEE,
                committee.getId(), "Removed " + user.getFirstName() + " " + user.getLastName() + " from \""
                        + committee.getName() + "\"");

        return toSummary(committee);
    }

    @Transactional
    public CommitteeSummary setChair(AppUserPrincipal admin, UUID committeeId, UUID userId) {
        Committee committee = findInOrg(admin, committeeId);
        User user = userRepository.findByIdAndOrganizationId(userId, admin.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("User not found"));
        CommitteeMembership membership = membershipRepository.findByCommitteeIdAndUserId(committee.getId(), user.getId())
                .orElseThrow(() -> ApiException.notFound("This user is not a member of the committee"));

        membershipRepository.findByCommitteeId(committee.getId()).forEach(m -> {
            if (m.isChair() && !m.getUserId().equals(user.getId())) {
                m.setChair(false);
                membershipRepository.save(m);
            }
        });
        membership.setChair(true);
        membershipRepository.save(membership);

        auditLogService.record(admin, AuditAction.COMMITTEE_MEMBERSHIP_CHANGED, AuditEntityType.COMMITTEE,
                committee.getId(), "Made " + user.getFirstName() + " " + user.getLastName() + " chair of \""
                        + committee.getName() + "\"");

        return toSummary(committee);
    }

    private Committee findInOrg(AppUserPrincipal principal, UUID committeeId) {
        return committeeRepository.findByIdAndOrganizationId(committeeId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Committee not found"));
    }

    private CommitteeSummary toSummary(Committee committee) {
        List<CommitteeMembership> memberships = membershipRepository.findByCommitteeId(committee.getId());
        Map<UUID, User> usersById = userRepository
                .findByOrganizationId(committee.getOrganizationId()).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<CommitteeMemberDto> members = memberships.stream()
                .map(m -> {
                    User user = usersById.get(m.getUserId());
                    if (user == null) return null;
                    return new CommitteeMemberDto(user.getId(), user.getFirstName(), user.getLastName(), m.isChair());
                })
                .filter(Objects::nonNull)
                .toList();

        return new CommitteeSummary(committee.getId(), committee.getName(), committee.getDescription(), members);
    }
}
