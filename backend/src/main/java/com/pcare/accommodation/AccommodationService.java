package com.pcare.accommodation;

import com.pcare.accommodation.AccommodationDtos.AccommodationDto;
import com.pcare.accommodation.AccommodationDtos.NearbyAccommodationDto;
import com.pcare.accommodation.AccommodationDtos.UpsertAccommodationRequest;
import com.pcare.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/** Accommodation master lookup and curation near treating hospitals (blueprint point 42). */
@Service
@Transactional
public class AccommodationService {

    private final AccommodationRepository repo;

    public AccommodationService(AccommodationRepository repo) {
        this.repo = repo;
    }

    public AccommodationDto create(UpsertAccommodationRequest req) {
        AccommodationOption o = new AccommodationOption();
        apply(o, req);
        return toDto(repo.save(o));
    }

    public AccommodationDto update(Long id, UpsertAccommodationRequest req) {
        AccommodationOption o = repo.findById(id).orElseThrow(() -> NotFoundException.of("AccommodationOption", id));
        apply(o, req);
        return toDto(repo.save(o));
    }

    @Transactional(readOnly = true)
    public AccommodationDto get(Long id) {
        return repo.findById(id).map(this::toDto)
                .orElseThrow(() -> NotFoundException.of("AccommodationOption", id));
    }

    @Transactional(readOnly = true)
    public List<AccommodationDto> list(String city) {
        List<AccommodationOption> options = (city == null || city.isBlank())
                ? repo.findByActiveTrueOrderByPricePerNightAsc()
                : repo.findByActiveTrueAndCityIgnoreCaseOrderByPricePerNightAsc(city);
        return options.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<NearbyAccommodationDto> nearby(double lat, double lng, double radiusKm) {
        return repo.findByActiveTrueOrderByPricePerNightAsc().stream()
                .filter(o -> o.getLatitude() != null && o.getLongitude() != null)
                .map(o -> new NearbyAccommodationDto(toDto(o),
                        haversineKm(lat, lng, o.getLatitude(), o.getLongitude())))
                .filter(n -> n.distanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(NearbyAccommodationDto::distanceKm))
                .toList();
    }

    private void apply(AccommodationOption o, UpsertAccommodationRequest req) {
        o.setName(req.name());
        o.setType(req.type());
        o.setAddressLine(req.addressLine());
        o.setCity(req.city());
        o.setLatitude(req.latitude());
        o.setLongitude(req.longitude());
        o.setRoomType(req.roomType());
        o.setPricePerNight(req.pricePerNight());
        o.setDistanceToHospitalKm(req.distanceToHospitalKm());
        o.setAvailable(req.available() == null || req.available());
        o.setRoomsAvailable(req.roomsAvailable());
        o.setContactPhone(req.contactPhone());
        o.setActive(req.active() == null || req.active());
    }

    private AccommodationDto toDto(AccommodationOption o) {
        return new AccommodationDto(o.getId(), o.getName(), o.getType(), o.getAddressLine(), o.getCity(),
                o.getLatitude(), o.getLongitude(), o.getRoomType(), o.getPricePerNight(),
                o.getDistanceToHospitalKm(), o.isAvailable(), o.getRoomsAvailable(), o.getContactPhone(),
                o.isActive());
    }

    private static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1), dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return Math.round(r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) * 10.0) / 10.0;
    }
}
