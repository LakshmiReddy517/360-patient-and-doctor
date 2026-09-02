package com.pcare.billing.repo;

import com.pcare.billing.domain.ServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServicePackageRepository extends JpaRepository<ServicePackage, Long> {
    List<ServicePackage> findByActiveTrue();
}
