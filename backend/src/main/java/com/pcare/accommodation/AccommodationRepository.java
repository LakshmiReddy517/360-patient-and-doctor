package com.pcare.accommodation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccommodationRepository extends JpaRepository<AccommodationOption, Long> {

    List<AccommodationOption> findByActiveTrueOrderByPricePerNightAsc();

    List<AccommodationOption> findByActiveTrueAndCityIgnoreCaseOrderByPricePerNightAsc(String city);
}
