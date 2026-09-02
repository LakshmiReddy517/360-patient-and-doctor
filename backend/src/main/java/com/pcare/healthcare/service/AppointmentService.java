package com.pcare.healthcare.service;

import com.pcare.casefile.domain.CaseEventType;
import com.pcare.casefile.domain.CaseFile;
import com.pcare.casefile.service.CaseService;
import com.pcare.common.exception.BadRequestException;
import com.pcare.common.exception.NotFoundException;
import com.pcare.healthcare.domain.Appointment;
import com.pcare.healthcare.domain.AppointmentStatus;
import com.pcare.healthcare.domain.Doctor;
import com.pcare.healthcare.repo.AppointmentRepository;
import com.pcare.healthcare.web.dto.HealthcareDtos.AppointmentDto;
import com.pcare.healthcare.web.dto.HealthcareDtos.ConsultationRequest;
import com.pcare.healthcare.web.dto.HealthcareDtos.CreateAppointmentRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Doctor appointment workflow (blueprint point 41) with optional case-timeline linkage. */
@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorService doctorService;
    private final CaseService caseService;

    public AppointmentService(AppointmentRepository appointmentRepository, DoctorService doctorService,
                              CaseService caseService) {
        this.appointmentRepository = appointmentRepository;
        this.doctorService = doctorService;
        this.caseService = caseService;
    }

    @Transactional
    public Appointment create(CreateAppointmentRequest req) {
        Doctor doctor = doctorService.get(req.doctorId());
        Appointment a = new Appointment();
        a.setDoctorId(doctor.getId());
        a.setDoctorName(doctor.getFullName());
        a.setHospitalId(doctor.getHospitalId());
        a.setHospitalName(doctor.getHospitalName());
        a.setDepartment(req.department() != null ? req.department() : doctor.getDepartment());
        a.setPatientId(req.patientId());
        a.setPatientName(req.patientName());
        a.setScheduledAt(req.scheduledAt());
        a.setReason(req.reason());
        a.setStatus(AppointmentStatus.REQUESTED);

        if (req.caseId() != null) {
            CaseFile caseFile = caseService.get(req.caseId());
            a.setCaseId(caseFile.getId());
            a.setCaseNumber(caseFile.getCaseNumber());
            if (a.getPatientName() == null) a.setPatientName(caseFile.getPatientName());
        }
        Appointment saved = appointmentRepository.save(a);
        if (saved.getCaseId() != null) {
            caseService.addEvent(saved.getCaseId(), CaseEventType.APPOINTMENT_BOOKED,
                    "Appointment with " + doctor.getFullName()
                            + (doctor.getSpecialty() != null ? " (" + doctor.getSpecialty() + ")" : "")
                            + " on " + saved.getScheduledAt(), "HEALTHCARE");
        }
        return saved;
    }

    @Transactional
    public Appointment updateStatus(Long id, AppointmentStatus status, String note) {
        Appointment a = get(id);
        if (a.getStatus().isClosed()) {
            throw new BadRequestException("Appointment is already " + a.getStatus());
        }
        a.setStatus(status);
        appointmentRepository.save(a);
        if (a.getCaseId() != null && (status == AppointmentStatus.CONFIRMED || status == AppointmentStatus.COMPLETED)) {
            caseService.addEvent(a.getCaseId(), CaseEventType.APPOINTMENT_BOOKED,
                    "Appointment #" + a.getId() + " " + status, "HEALTHCARE");
        }
        return a;
    }

    @Transactional
    public Appointment recordConsultation(Long id, ConsultationRequest req) {
        Appointment a = get(id);
        a.setConsultationNotes(req.consultationNotes());
        a.setPrescription(req.prescription());
        a.setFollowUpAt(req.followUpAt());
        a.setStatus(req.followUpAt() != null ? AppointmentStatus.FOLLOW_UP : AppointmentStatus.PRESCRIBED);
        appointmentRepository.save(a);
        if (a.getCaseId() != null) {
            caseService.addEvent(a.getCaseId(), CaseEventType.CARE_ACTIVITY,
                    "Consultation recorded for appointment #" + a.getId(), "HEALTHCARE");
        }
        return a;
    }

    @Transactional(readOnly = true)
    public Appointment get(Long id) {
        return appointmentRepository.findById(id).orElseThrow(() -> NotFoundException.of("Appointment", id));
    }

    @Transactional(readOnly = true)
    public List<AppointmentDto> listByCase(Long caseId) {
        return appointmentRepository.findByCaseIdOrderByScheduledAtDesc(caseId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentDto> listByDoctor(Long doctorId) {
        return appointmentRepository.findByDoctorIdOrderByScheduledAtDesc(doctorId).stream().map(this::toDto).toList();
    }

    public AppointmentDto toDto(Appointment a) {
        return new AppointmentDto(a.getId(), a.getCaseId(), a.getCaseNumber(), a.getPatientId(), a.getPatientName(),
                a.getDoctorId(), a.getDoctorName(), a.getHospitalId(), a.getHospitalName(), a.getDepartment(),
                a.getScheduledAt(), a.getStatus(), a.getReason(), a.getConsultationNotes(), a.getPrescription(),
                a.getFollowUpAt(), a.getCreatedAt());
    }
}
