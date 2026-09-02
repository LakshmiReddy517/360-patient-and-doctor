package com.pcare.care.repo;

import com.pcare.care.domain.Caretaker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CaretakerRepository extends JpaRepository<Caretaker, Long> {
    List<Caretaker> findByAvailableTrue();

    @Query("select c from Caretaker c where :q is null or lower(c.fullName) like lower(concat('%', :q, '%'))")
    Page<Caretaker> search(@Param("q") String q, Pageable pageable);
}
