package com.pcare.healthcare.repo;

import com.pcare.healthcare.domain.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByCaseIdOrderByScheduledAtDesc(Long caseId);

    List<Appointment> findByDoctorIdOrderByScheduledAtDesc(Long doctorId);
}
