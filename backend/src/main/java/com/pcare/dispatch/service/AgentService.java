package com.pcare.dispatch.service;

import com.pcare.common.exception.NotFoundException;
import com.pcare.dispatch.domain.Agent;
import com.pcare.dispatch.domain.AgentStatus;
import com.pcare.dispatch.repo.AgentRepository;
import com.pcare.dispatch.web.dto.DispatchDtos.AgentDto;
import com.pcare.dispatch.web.dto.DispatchDtos.DispatchStats;
import com.pcare.dispatch.web.dto.DispatchDtos.UpsertAgentRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;

/** Agent registry, availability status and live location (blueprint points 19, 20, 30). */
@Service
public class AgentService {

    private final AgentRepository agentRepository;
    private final com.pcare.live.LiveHub liveHub;

    public AgentService(AgentRepository agentRepository, com.pcare.live.LiveHub liveHub) {
        this.agentRepository = agentRepository;
        this.liveHub = liveHub;
    }

    @Transactional
    public Agent create(UpsertAgentRequest req) {
        Agent a = new Agent();
        apply(a, req);
        return agentRepository.save(a);
    }

    @Transactional
    public Agent update(Long id, UpsertAgentRequest req) {
        Agent a = get(id);
        apply(a, req);
        return agentRepository.save(a);
    }

    private void apply(Agent a, UpsertAgentRequest req) {
        a.setUserId(req.userId());
        a.setFullName(req.fullName());
        a.setMobile(req.mobile());
        a.setEmail(req.email());
        a.setEmployeeCode(req.employeeCode());
        a.setSkills(req.skills() != null ? new HashSet<>(req.skills()) : new HashSet<>());
        a.setLanguages(req.languages() != null ? new HashSet<>(req.languages()) : new HashSet<>());
        a.setShift(req.shift());
        if (req.verified() != null) a.setVerified(req.verified());
        if (req.emtLevel() != null) a.setEmtLevel(req.emtLevel());
        a.setBloodGroup(req.bloodGroup());
        a.setLicenceNumber(req.licenceNumber());
        a.setCertificationExpiry(req.certificationExpiry());
    }

    @Transactional(readOnly = true)
    public Agent get(Long id) {
        return agentRepository.findById(id).orElseThrow(() -> NotFoundException.of("Agent", id));
    }

    @Transactional(readOnly = true)
    public Agent getByUserId(Long userId) {
        return agentRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("No agent profile linked to this account"));
    }

    @Transactional(readOnly = true)
    public List<AgentDto> listAll() {
        return agentRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AgentDto> listDispatchable() {
        return agentRepository.findByStatusIn(List.of(AgentStatus.ONLINE, AgentStatus.AVAILABLE))
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public Agent setStatus(Long id, AgentStatus status) {
        Agent a = get(id);
        a.setStatus(status);
        return agentRepository.save(a);
    }

    @Transactional
    public Agent updateLocation(Long id, double lat, double lng) {
        Agent a = get(id);
        a.setCurrentLatitude(lat);
        a.setCurrentLongitude(lng);
        a.setLocationUpdatedAt(Instant.now());
        Agent saved = agentRepository.save(a);
        liveHub.broadcast("location", java.util.Map.of(
                "kind", "agent", "id", saved.getId(), "name", saved.getFullName(),
                "lat", lat, "lng", lng, "status", saved.getStatus().name()));
        return saved;
    }

    @Transactional(readOnly = true)
    public DispatchStats stats() {
        long online = agentRepository.countByStatus(AgentStatus.ONLINE);
        long available = agentRepository.countByStatus(AgentStatus.AVAILABLE);
        long offline = agentRepository.countByStatus(AgentStatus.OFFLINE);
        long total = agentRepository.count();
        long onJob = total - online - available - offline;
        return new DispatchStats(online, available, onJob, offline);
    }

    public AgentDto toDto(Agent a) {
        return new AgentDto(a.getId(), a.getUserId(), a.getFullName(), a.getMobile(), a.getEmail(),
                a.getEmployeeCode(), a.getSkills(), a.getLanguages(), a.getStatus(), a.getCurrentLatitude(),
                a.getCurrentLongitude(), a.getLocationUpdatedAt(), a.getShift(), a.getRating(),
                a.getActiveAssignments(), a.isVerified(), a.getEmtLevel(), a.getBloodGroup(),
                a.getLicenceNumber(), a.getCertificationExpiry());
    }
}
