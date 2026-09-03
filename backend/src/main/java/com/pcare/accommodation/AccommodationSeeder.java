package com.pcare.accommodation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds a starter accommodation master near a Bengaluru hospital cluster (blueprint point 42),
 * so the patient app "stay near hospital" screen has data on a fresh database.
 */
@Component
public class AccommodationSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AccommodationSeeder.class);

    private final AccommodationRepository repo;

    public AccommodationSeeder(AccommodationRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) {
        if (repo.count() > 0) {
            return;
        }
        List<AccommodationOption> options = List.of(
                option("Comfort Stay Lodge", "Guest House", "MG Road, Bengaluru", "Bengaluru",
                        12.9750, 77.6060, "Twin Sharing", 1200, 0.8, 8, "9611110001"),
                option("CityCare Service Apartments", "Service Apartment", "Indiranagar, Bengaluru", "Bengaluru",
                        12.9719, 77.6412, "Studio", 2500, 1.5, 5, "9611110002"),
                option("Nirmala Dharamshala", "Dharamshala", "Near Victoria Hospital", "Bengaluru",
                        12.9600, 77.5850, "Dormitory", 400, 0.5, 20, "9611110003"),
                option("Grand Meridian Hotel", "Hotel", "Residency Road", "Bengaluru",
                        12.9700, 77.6030, "Deluxe", 4200, 1.1, 12, "9611110004"));
        repo.saveAll(options);
        log.info("Seeded {} accommodation options (blueprint point 42)", options.size());
    }

    private AccommodationOption option(String name, String type, String addressLine, String city,
                                       double latitude, double longitude, String roomType, long pricePerNight,
                                       double distanceToHospitalKm, int roomsAvailable, String contactPhone) {
        AccommodationOption o = new AccommodationOption();
        o.setName(name);
        o.setType(type);
        o.setAddressLine(addressLine);
        o.setCity(city);
        o.setLatitude(latitude);
        o.setLongitude(longitude);
        o.setRoomType(roomType);
        o.setPricePerNight(BigDecimal.valueOf(pricePerNight));
        o.setDistanceToHospitalKm(distanceToHospitalKm);
        o.setAvailable(true);
        o.setRoomsAvailable(roomsAvailable);
        o.setContactPhone(contactPhone);
        o.setActive(true);
        return o;
    }
}
