package com.fris.boardportal.meeting;

import com.fris.boardportal.audit.AuditAction;
import com.fris.boardportal.audit.AuditEntityType;
import com.fris.boardportal.audit.AuditLogService;
import com.fris.boardportal.common.ApiException;
import com.fris.boardportal.meeting.dto.MeetingTypeSummary;
import com.fris.boardportal.security.AppUserPrincipal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeetingTypeService {

    private final MeetingTypeOptionRepository meetingTypeOptionRepository;
    private final MeetingRepository meetingRepository;
    private final AuditLogService auditLogService;

    public MeetingTypeService(MeetingTypeOptionRepository meetingTypeOptionRepository,
            MeetingRepository meetingRepository, AuditLogService auditLogService) {
        this.meetingTypeOptionRepository = meetingTypeOptionRepository;
        this.meetingRepository = meetingRepository;
        this.auditLogService = auditLogService;
    }

    public List<MeetingTypeSummary> listForOrganization(AppUserPrincipal principal) {
        return meetingTypeOptionRepository.findByOrganizationIdOrderByNameAsc(principal.getOrganizationId()).stream()
                .map(MeetingTypeSummary::from)
                .toList();
    }

    @Transactional
    public MeetingTypeSummary create(AppUserPrincipal admin, String name) {
        if (meetingTypeOptionRepository.existsByOrganizationIdAndNameIgnoreCase(admin.getOrganizationId(), name)) {
            throw ApiException.badRequest("A meeting type with this name already exists");
        }

        MeetingTypeOption option = MeetingTypeOption.create(admin.getOrganizationId(), name);
        meetingTypeOptionRepository.save(option);

        auditLogService.record(admin, AuditAction.MEETING_TYPE_CREATED, AuditEntityType.MEETING_TYPE, option.getId(),
                "Added meeting type \"" + option.getName() + "\"");

        return MeetingTypeSummary.from(option);
    }

    @Transactional
    public void delete(AppUserPrincipal admin, UUID id) {
        MeetingTypeOption option = meetingTypeOptionRepository.findByIdAndOrganizationId(id, admin.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Meeting type not found"));

        if (meetingRepository.existsByMeetingTypeId(option.getId())) {
            throw ApiException.badRequest("This meeting type is in use by one or more meetings and can't be deleted");
        }

        meetingTypeOptionRepository.delete(option);

        auditLogService.record(admin, AuditAction.MEETING_TYPE_DELETED, AuditEntityType.MEETING_TYPE, option.getId(),
                "Deleted meeting type \"" + option.getName() + "\"");
    }
}
