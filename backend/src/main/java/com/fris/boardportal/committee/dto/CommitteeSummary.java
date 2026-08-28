package com.fris.boardportal.committee.dto;

import java.util.List;
import java.util.UUID;

public record CommitteeSummary(
        UUID id,
        String name,
        String description,
        UUID parentCommitteeId,
        String parentCommitteeName,
        List<CommitteeMemberDto> members,
        List<CommitteeSummary> subCommittees) {
}
