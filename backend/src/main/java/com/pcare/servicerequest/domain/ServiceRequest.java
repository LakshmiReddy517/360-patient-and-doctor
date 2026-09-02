package com.pcare.servicerequest.domain;

import com.pcare.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * A patient service request — the start of the journey. Captures the requested services, pickup
 * and destination, triage/emergency, and moves through the controlled {@link RequestStatus}
 * lifecycle. When actioned it is converted into a {@link com.pcare.casefile.domain.CaseFile}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "service_request", indexes = {
        @Index(name = "ix_sr_status", columnList = "status"),
        @Index(name = "ix_sr_patient", columnList = "patientId"),
        @Index(name = "ix_sr_case", columnList = "caseId")
})
public class ServiceRequest extends BaseEntity {

    private Long patientId;

    @Column(length = 160)
    private String patientName;

    @Column(length = 30)
    private String patientMobile;

    /** Set once this request has been converted into a case. */
    private Long caseId;

    @Column(length = 24)
    private String caseNumber;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "service_request_service", joinColumns = @JoinColumn(name = "request_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", length = 40)
    private Set<ServiceType> services = new HashSet<>();

    @Column(length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestStatus status = RequestStatus.NEW;

    @Column(nullable = false)
    private boolean emergency = false;

    @Column(length = 500)
    private String triageNotes;

    @Embedded
    private PickupInfo pickup = new PickupInfo();

    @Embedded
    private DestinationInfo destination = new DestinationInfo();
}
