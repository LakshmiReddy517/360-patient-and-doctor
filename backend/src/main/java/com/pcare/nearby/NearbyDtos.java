package com.pcare.nearby;

import java.util.List;

/** DTOs for GPS-based "find nearby hospitals & ambulances" (patient app). */
public final class NearbyDtos {

    private NearbyDtos() {
    }

    public record NearbyHospital(Long id, String name, String address, String phone, boolean emergency,
                                 Double lat, Double lng, double distanceKm) {
    }

    public record NearbyAmbulance(Long id, String registrationNo, String status, Integer oxygenLevelPercent,
                                  Double lat, Double lng, double distanceKm) {
    }

    public record NearbyResult(List<NearbyHospital> hospitals, List<NearbyAmbulance> ambulances) {
    }
}
