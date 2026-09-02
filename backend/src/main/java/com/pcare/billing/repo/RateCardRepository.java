package com.pcare.billing.repo;

import com.pcare.billing.domain.RateCard;
import com.pcare.servicerequest.domain.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RateCardRepository extends JpaRepository<RateCard, Long> {
    Optional<RateCard> findByServiceType(ServiceType serviceType);

    List<RateCard> findByActiveTrue();
}
