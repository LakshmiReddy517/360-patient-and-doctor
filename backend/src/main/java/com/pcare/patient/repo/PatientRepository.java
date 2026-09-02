package com.pcare.patient.repo;

import com.pcare.patient.domain.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByUserId(Long userId);

    @Query("select p from Patient p where :q is null "
            + "or lower(p.fullName) like lower(concat('%', :q, '%')) "
            + "or p.mobile like concat('%', :q, '%')")
    Page<Patient> search(@Param("q") String q, Pageable pageable);
}
