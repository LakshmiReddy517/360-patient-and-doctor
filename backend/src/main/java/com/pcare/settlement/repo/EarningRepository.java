package com.pcare.settlement.repo;

import com.pcare.settlement.domain.Earning;
import com.pcare.settlement.domain.Settlement.PayeeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EarningRepository extends JpaRepository<Earning, Long> {
    List<Earning> findByPayeeTypeAndPayeeIdAndSettledFalse(PayeeType payeeType, Long payeeId);

    List<Earning> findBySettlementId(Long settlementId);

    List<Earning> findBySettledFalse();
}
