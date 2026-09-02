package com.pcare.casefile.web.dto;

import com.pcare.casefile.domain.CaseEventType;
import com.pcare.casefile.domain.CasePriority;
import com.pcare.casefile.domain.CaseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/** DTOs for the Case engine. */
public final class CaseDtos {

    private CaseDtos() {
    }

    public record CreateCaseRequest(
            Long patientId,
            @NotBlank @Size(max = 160) String patientName,
            @Size(max = 30) String patientMobile,
            @Size(max = 240) String title,
            @Size(max = 2000) String summary,
            CasePriority priority,
            boolean emergency) {
    }

    public record UpdateStatusRequest(
            @NotNull CaseStatus status,
            @Size(max = 500) String note) {
    }

    public record UpdatePriorityRequest(
            @NotNull CasePriority priority,
            @Size(max = 500) String note) {
    }

    public record AssignRequest(
            @NotNull Long userId,
            @Size(max = 160) String name,
            @Size(max = 500) String note) {
    }

    public record AddNoteRequest(
            @NotBlank @Size(max = 500) String note) {
    }

    public record CaseSummaryDto(
            Long id,
            String caseNumber,
            Long patientId,
            String patientName,
            String patientMobile,
            String title,
            CaseStatus status,
            CasePriority priority,
            boolean emergency,
            Long assignedToUserId,
            String assignedToName,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record TimelineEventDto(
            Long id,
            CaseEventType type,
            String description,
            String source,
            String createdBy,
            Instant createdAt) {
    }

    public record CaseDetailDto(
            CaseSummaryDto caseFile,
            String summary,
            List<TimelineEventDto> timeline) {
    }

    public record DashboardStats(
            long openCases,
            long inProgressCases,
            long onHoldCases,
            long emergencies,
            long closedCases,
            long totalCases) {
    }
}
