package com.pcare.billing.web;

import com.pcare.billing.service.CatalogService;
import com.pcare.billing.web.dto.BillingDtos.PackageDto;
import com.pcare.billing.web.dto.BillingDtos.RateCardDto;
import com.pcare.billing.web.dto.BillingDtos.UpsertPackageRequest;
import com.pcare.billing.web.dto.BillingDtos.UpsertRateCardRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Catalog", description = "Configurable rate cards and service packages (pricing inputs)")
@RestController
@RequestMapping("/api/v1")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Operation(summary = "List rate cards")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE')")
    @GetMapping("/rate-cards")
    public List<RateCardDto> rateCards() {
        return catalogService.listRateCards();
    }

    @Operation(summary = "Create or update a rate card")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE')")
    @PutMapping("/rate-cards")
    public RateCardDto upsertRateCard(@Valid @RequestBody UpsertRateCardRequest req) {
        return catalogService.upsertRateCard(req);
    }

    @Operation(summary = "List packages")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE','PATIENT')")
    @GetMapping("/packages")
    public List<PackageDto> packages() {
        return catalogService.listPackages();
    }

    @Operation(summary = "Create a package")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE')")
    @PostMapping("/packages")
    public PackageDto createPackage(@Valid @RequestBody UpsertPackageRequest req) {
        return catalogService.createPackage(req);
    }

    @Operation(summary = "Update a package")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE')")
    @PutMapping("/packages/{id}")
    public PackageDto updatePackage(@PathVariable Long id, @Valid @RequestBody UpsertPackageRequest req) {
        return catalogService.updatePackage(id, req);
    }
}
