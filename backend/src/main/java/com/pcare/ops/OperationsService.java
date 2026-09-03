package com.pcare.ops;

import com.pcare.casefile.service.CaseService;
import com.pcare.casefile.web.dto.CaseDtos.DashboardStats;
import com.pcare.dispatch.domain.AssignmentStatus;
import com.pcare.dispatch.repo.AssignmentRepository;
import com.pcare.dispatch.service.AgentService;
import com.pcare.dispatch.web.dto.DispatchDtos.DispatchStats;
import com.pcare.fleet.service.FleetService;
import com.pcare.fleet.web.dto.FleetDtos.FleetStats;
import com.pcare.servicerequest.domain.RequestStatus;
import com.pcare.servicerequest.repo.ServiceRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** Aggregates per-domain stats into one operational Command-Centre dashboard (blueprint point 6). */
@Service
public class OperationsService {

    public record OpsDashboard(
            long newRequests, long openCases, long inProgressCases, long onHoldCases,
            long emergencies, long closedCases, long totalCases,
            long agentsOnline, long agentsAvailable, long agentsOnJob, long agentsOffline,
            long ambulancesTotal, long ambulancesAvailable, long ambulancesOnTrip, long ambulancesMaintenance,
            long activeDispatches, long pickupsToday) {
    }

    private final CaseService caseService;
    private final AgentService agentService;
    private final FleetService fleetService;
    private final ServiceRequestRepository requestRepository;
    private final AssignmentRepository assignmentRepository;

    public OperationsService(CaseService caseService, AgentService agentService, FleetService fleetService,
                             ServiceRequestRepository requestRepository, AssignmentRepository assignmentRepository) {
        this.caseService = caseService;
        this.agentService = agentService;
        this.fleetService = fleetService;
        this.requestRepository = requestRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public OpsDashboard dashboard() {
        DashboardStats c = caseService.dashboard();
        DispatchStats d = agentService.stats();
        FleetStats f = fleetService.stats();
        long newReq = requestRepository.countByStatus(RequestStatus.NEW);
        long active = assignmentRepository.countByStatusNotIn(
                List.of(AssignmentStatus.COMPLETED, AssignmentStatus.CANCELLED));
        var startOfDay = LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant();
        long pickupsToday = assignmentRepository.countByCreatedAtGreaterThanEqual(startOfDay);
        return new OpsDashboard(newReq, c.openCases(), c.inProgressCases(), c.onHoldCases(),
                c.emergencies(), c.closedCases(), c.totalCases(),
                d.online(), d.available(), d.onJob(), d.offline(),
                f.total(), f.available(), f.onTrip(), f.maintenance(),
                active, pickupsToday);
    }
}
