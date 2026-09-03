package com.pcare.billing.service;

import com.pcare.billing.domain.PackageCategory;
import com.pcare.billing.domain.PackageItem;
import com.pcare.billing.domain.RateCard;
import com.pcare.billing.domain.ServicePackage;
import com.pcare.billing.repo.RateCardRepository;
import com.pcare.billing.repo.ServicePackageRepository;
import com.pcare.billing.web.dto.BillingDtos.PackageDto;
import com.pcare.billing.web.dto.BillingDtos.PackageItemDto;
import com.pcare.billing.web.dto.BillingDtos.RateCardDto;
import com.pcare.billing.web.dto.BillingDtos.UpsertPackageRequest;
import com.pcare.billing.web.dto.BillingDtos.UpsertRateCardRequest;
import com.pcare.billing.web.dto.BillingDtos.FareEstimate;
import com.pcare.common.exception.BadRequestException;
import com.pcare.common.exception.NotFoundException;
import com.pcare.servicerequest.domain.ServiceType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Rate-card and package management — the configurable inputs to the pricing engine. */
@Service
public class CatalogService {

    private final RateCardRepository rateCardRepository;
    private final ServicePackageRepository packageRepository;

    public CatalogService(RateCardRepository rateCardRepository, ServicePackageRepository packageRepository) {
        this.rateCardRepository = rateCardRepository;
        this.packageRepository = packageRepository;
    }

    // ---- Rate cards ----
    @Transactional(readOnly = true)
    public List<RateCardDto> listRateCards() {
        return rateCardRepository.findAll().stream().map(this::toRateDto).toList();
    }

    @Transactional
    public RateCardDto upsertRateCard(UpsertRateCardRequest req) {
        RateCard rc = rateCardRepository.findByServiceType(req.serviceType()).orElseGet(RateCard::new);
        rc.setServiceType(req.serviceType());
        rc.setLabel(req.label());
        rc.setUnit(req.unit());
        rc.setUnitRate(req.unitRate());
        if (req.baseFare() != null) rc.setBaseFare(req.baseFare());
        if (req.perKmRate() != null) rc.setPerKmRate(req.perKmRate());
        if (req.emergencyMultiplier() != null) rc.setEmergencyMultiplier(req.emergencyMultiplier());
        if (req.nightSurchargePercent() != null) rc.setNightSurchargePercent(req.nightSurchargePercent());
        rc.setActive(req.active() == null || req.active());
        return toRateDto(rateCardRepository.save(rc));
    }

    /**
     * Multi-factor fare estimate (blueprint point 17): base fare + per-km distance charge,
     * an emergency multiplier on the running subtotal, then a night-hours percentage surcharge.
     */
    @Transactional(readOnly = true)
    public FareEstimate estimate(ServiceType serviceType, double distanceKm, boolean emergency, boolean night) {
        RateCard rc = rateCardRepository.findByServiceType(serviceType)
                .orElseThrow(() -> new BadRequestException("No rate card configured for " + serviceType));
        BigDecimal base = rc.getBaseFare();
        BigDecimal distanceCharge = rc.getPerKmRate().multiply(BigDecimal.valueOf(Math.max(0, distanceKm)))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = base.add(distanceCharge).setScale(2, RoundingMode.HALF_UP);

        BigDecimal emergencySurcharge = BigDecimal.ZERO;
        if (emergency && rc.getEmergencyMultiplier().compareTo(BigDecimal.ONE) > 0) {
            emergencySurcharge = subtotal.multiply(rc.getEmergencyMultiplier().subtract(BigDecimal.ONE))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal afterEmergency = subtotal.add(emergencySurcharge);

        BigDecimal nightSurcharge = BigDecimal.ZERO;
        if (night && rc.getNightSurchargePercent().signum() > 0) {
            nightSurcharge = afterEmergency.multiply(rc.getNightSurchargePercent())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        BigDecimal total = afterEmergency.add(nightSurcharge).setScale(2, RoundingMode.HALF_UP);

        return new FareEstimate(serviceType, rc.getLabel(), distanceKm, emergency, night,
                base.setScale(2, RoundingMode.HALF_UP), distanceCharge, subtotal,
                emergencySurcharge, nightSurcharge, total, "INR");
    }

    // ---- Packages ----
    @Transactional(readOnly = true)
    public List<PackageDto> listPackages() {
        return packageRepository.findAll().stream().map(this::toPackageDto).toList();
    }

    @Transactional
    public PackageDto createPackage(UpsertPackageRequest req) {
        return toPackageDto(packageRepository.save(apply(new ServicePackage(), req)));
    }

    @Transactional
    public PackageDto updatePackage(Long id, UpsertPackageRequest req) {
        ServicePackage p = packageRepository.findById(id).orElseThrow(() -> NotFoundException.of("Package", id));
        return toPackageDto(packageRepository.save(apply(p, req)));
    }

    @Transactional(readOnly = true)
    public ServicePackage getPackage(Long id) {
        return packageRepository.findById(id).orElseThrow(() -> NotFoundException.of("Package", id));
    }

    private ServicePackage apply(ServicePackage p, UpsertPackageRequest req) {
        p.setName(req.name());
        p.setDescription(req.description());
        p.setCategory(req.category() != null ? req.category() : PackageCategory.CUSTOM);
        p.setActive(req.active() == null || req.active());
        p.getItems().clear();
        if (req.items() != null) {
            for (PackageItemDto d : req.items()) {
                PackageItem it = new PackageItem();
                it.setServiceType(d.serviceType());
                it.setLabel(d.label());
                it.setQuantity(d.quantity() != null ? d.quantity() : BigDecimal.ONE);
                it.setUnitPrice(d.unitPrice() != null ? d.unitPrice() : BigDecimal.ZERO);
                p.getItems().add(it);
            }
        }
        return p;
    }

    // ---- mapping ----
    private RateCardDto toRateDto(RateCard rc) {
        return new RateCardDto(rc.getId(), rc.getServiceType(), rc.getLabel(), rc.getUnit(), rc.getUnitRate(),
                rc.getBaseFare(), rc.getPerKmRate(), rc.getEmergencyMultiplier(), rc.getNightSurchargePercent(),
                rc.isActive());
    }

    private PackageDto toPackageDto(ServicePackage p) {
        BigDecimal total = p.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(i.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<PackageItemDto> items = p.getItems().stream()
                .map(i -> new PackageItemDto(i.getServiceType(), i.getLabel(), i.getQuantity(), i.getUnitPrice()))
                .toList();
        return new PackageDto(p.getId(), p.getName(), p.getDescription(), p.getCategory(), p.isActive(), items, total);
    }
}
