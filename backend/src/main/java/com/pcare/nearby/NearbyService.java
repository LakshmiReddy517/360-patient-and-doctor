package com.pcare.nearby;

import com.pcare.fleet.domain.Ambulance;
import com.pcare.fleet.repo.AmbulanceRepository;
import com.pcare.healthcare.domain.Hospital;
import com.pcare.healthcare.repo.HospitalRepository;
import com.pcare.nearby.NearbyDtos.NearbyAmbulance;
import com.pcare.nearby.NearbyDtos.NearbyHospital;
import com.pcare.nearby.NearbyDtos.NearbyResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Finds the hospitals and ambulances closest to a device location, sorted by distance. */
@Service
public class NearbyService {

    private final HospitalRepository hospitalRepository;
    private final AmbulanceRepository ambulanceRepository;

    public NearbyService(HospitalRepository hospitalRepository, AmbulanceRepository ambulanceRepository) {
        this.hospitalRepository = hospitalRepository;
        this.ambulanceRepository = ambulanceRepository;
    }

    @Transactional(readOnly = true)
    public NearbyResult nearby(double lat, double lng, int limit) {
        var hospitals = hospitalRepository.findAll().stream()
                .filter(h -> h.isActive() && h.getLatitude() != null && h.getLongitude() != null)
                .map(h -> new NearbyHospital(h.getId(), h.getName(), address(h), h.getPhone(), h.isEmergencyAvailable(),
                        h.getLatitude(), h.getLongitude(), km(lat, lng, h.getLatitude(), h.getLongitude())))
                .sorted(Comparator.comparingDouble(NearbyHospital::distanceKm))
                .limit(limit)
                .toList();

        var ambulances = ambulanceRepository.findAll().stream()
                .filter(a -> a.getCurrentLatitude() != null && a.getCurrentLongitude() != null)
                .map(a -> new NearbyAmbulance(a.getId(), a.getRegistrationNo(), a.getStatus().name(),
                        a.getOxygenLevelPercent(), a.getCurrentLatitude(), a.getCurrentLongitude(),
                        km(lat, lng, a.getCurrentLatitude(), a.getCurrentLongitude())))
                .sorted(Comparator.comparingDouble(NearbyAmbulance::distanceKm))
                .limit(limit)
                .toList();

        return new NearbyResult(hospitals, ambulances);
    }

    private String address(Hospital h) {
        return Stream.of(h.getAddressLine(), h.getCity(), h.getState())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(", "));
    }

    private double km(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1), dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return Math.round(r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) * 10.0) / 10.0;
    }
}
