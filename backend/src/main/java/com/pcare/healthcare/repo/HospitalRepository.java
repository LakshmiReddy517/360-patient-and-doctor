package com.pcare.healthcare.repo;

import com.pcare.healthcare.domain.Hospital;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {
    @Query("select h from Hospital h where :q is null "
            + "or lower(h.name) like lower(concat('%', :q, '%')) "
            + "or lower(h.city) like lower(concat('%', :q, '%'))")
    Page<Hospital> search(@Param("q") String q, Pageable pageable);
}
