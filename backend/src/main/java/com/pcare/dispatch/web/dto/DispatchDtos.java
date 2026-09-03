package com.pcare.dispatch.web.dto;

import com.pcare.dispatch.domain.AgentStatus;
import com.pcare.dispatch.domain.AssignmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Set;

/** DTOs for the dispatch / workforce module. */
public final class DispatchDtos {

    private DispatchDtos() {
    }

    // ---- Agents ----
    public record UpsertAgentRequest(
            Long userId,
            @NotBlank String fullName,
            String mobile,
            String email,
            String employeeCode,
            Set<String> skills,
            Set<String> languages,
            String shift,
            Boolean verified,
            com.pcare.dispatch.domain.EmtLevel emtLevel,
            String bloodGroup,
            String licenceNumber,
            java.time.LocalDate certificationExpiry) {
    }

    public record AgentDto(
            Long id, Long userId, String fullName, String mobile, String email, String employeeCode,
            Set<String> skills, Set<String> languages, AgentStatus status, Double currentLatitude,
            Double currentLongitude, Instant locationUpdatedAt, String shift, double rating,
            int activeAssignments, boolean verified,
            com.pcare.dispatch.domain.EmtLevel emtLevel, String bloodGroup, String licenceNumber,
            java.time.LocalDate certificationExpiry) {
    }

    public record StatusRequest(@NotNull AgentStatus status) {
    }

    public record LocationRequest(@NotNull Double latitude, @NotNull Double longitude) {
    }

    // ---- Assignments ----
    public record AssignRequest(@NotNull Long agentId, @NotNull Long caseId, String notes) {
    }

    public record AssignmentStatusRequest(@NotNull AssignmentStatus status, String note) {
    }

    public record VerifyOtpRequest(@NotBlank String otp) {
    }

    public record AssignmentDto(
            Long id, Long caseId, String caseNumber, String patientName, Long agentId, String agentName,
            AssignmentStatus status, String pickupOtp, String handoverOtp, String notes,
            Instant acceptedAt, Instant pickedAt, Instant completedAt, Instant createdAt) {
    }

    public record DispatchStats(long online, long available, long onJob, long offline) {
    }
}
