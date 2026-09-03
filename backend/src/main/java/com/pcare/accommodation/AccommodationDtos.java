package com.pcare.accommodation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** DTOs for the accommodation master near treating hospitals (blueprint point 42). */
public final class AccommodationDtos {

    private AccommodationDtos() {
    }

    public record UpsertAccommodationRequest(
            @NotBlank String name, String type, String addressLine, String city,
            Double latitude, Double longitude, String roomType, @NotNull BigDecimal pricePerNight,
            Double distanceToHospitalKm, Boolean available, Integer roomsAvailable, String contactPhone,
            Boolean active) {
    }

    public record AccommodationDto(
            Long id, String name, String type, String addressLine, String city,
            Double latitude, Double longitude, String roomType, BigDecimal pricePerNight,
            Double distanceToHospitalKm, boolean available, Integer roomsAvailable, String contactPhone,
            boolean active) {
    }

    /** An option with its computed distance from a query point (patient "near me" search). */
    public record NearbyAccommodationDto(AccommodationDto option, double distanceKm) {
    }
}
