package com.pcare.support.web.dto;

import com.pcare.support.domain.SupportEnums.ComplaintStatus;
import com.pcare.support.domain.SupportEnums.IncidentSeverity;
import com.pcare.support.domain.SupportEnums.IncidentStatus;
import com.pcare.support.domain.SupportEnums.IncidentType;
import com.pcare.support.domain.SupportEnums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

/** DTOs for the complaints / incidents / SLA module. */
public final class SupportDtos {

    private SupportDtos() {
    }

    // ---- Complaints ----
    public record CreateComplaintRequest(
            Long caseId, String patientName, @NotBlank String subject, String description,
            String category, Priority priority) {
    }

    public record AssignComplaintRequest(@NotNull Long userId, String name) {
    }

    public record ComplaintStatusRequest(@NotNull ComplaintStatus status, String note) {
    }

    public record ResolveComplaintRequest(@NotBlank String resolution) {
    }

    public record AddCommentRequest(@NotBlank String message, boolean internal) {
    }

    public record CommentDto(Long id, Long complaintId, String message, boolean internal,
                             String author, Instant createdAt) {
    }

    public record ComplaintDto(
            Long id, Long caseId, String caseNumber, String patientName, String subject, String description,
            String category, Priority priority, ComplaintStatus status, Long assignedToUserId, String assignedToName,
            String resolution, Instant slaResponseDueAt, Instant slaResolutionDueAt, Instant firstResponseAt,
            Instant resolvedAt, boolean responseBreached, boolean resolutionBreached, int reopenCount,
            Instant createdAt, List<CommentDto> comments) {
    }

    public record ComplaintStats(long open, long investigating, long resolved, long breached) {
    }

    // ---- SLA policy ----
    public record SlaPolicyDto(Long id, Priority priority, int responseMinutes, int resolutionMinutes) {
    }

    public record UpsertSlaPolicyRequest(@NotNull Priority priority, int responseMinutes, int resolutionMinutes) {
    }

    // ---- Incidents ----
    public record CreateIncidentRequest(
            Long caseId, @NotNull IncidentType type, IncidentSeverity severity, @NotBlank String description) {
    }

    public record IncidentStatusRequest(@NotNull IncidentStatus status, String resolution) {
    }

    public record IncidentDto(
            Long id, Long caseId, String caseNumber, IncidentType type, IncidentSeverity severity,
            String description, IncidentStatus status, String resolution, Instant resolvedAt,
            String reportedBy, Instant createdAt) {
    }

    public record IncidentStats(long open, long inProgress, long escalated, long resolved) {
    }
}
