package com.pcare.patient.domain;

import com.pcare.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Metadata for an uploaded medical document (blueprint point 14). The binary is stored on the
 * server file store; this row keeps type/date/uploader/source/description and access status so
 * documents are consent-gated and auditable.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "medical_document", indexes = {
        @Index(name = "ix_document_patient", columnList = "patientId")
})
public class MedicalDocument extends BaseEntity {

    @Column(nullable = false)
    private Long patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentType type = DocumentType.OTHER;

    @Column(length = 240)
    private String title;

    private LocalDate documentDate;

    @Column(length = 120)
    private String source;         // e.g. hospital / lab name

    @Column(length = 500)
    private String description;

    /** Stored file reference (relative path in the server file store). */
    @Column(length = 300)
    private String storedFileName;

    @Column(length = 240)
    private String originalFileName;

    @Column(length = 100)
    private String contentType;

    private Long sizeBytes;

    /** True once the patient has consented to this document being shared with care staff. */
    @Column(nullable = false)
    private boolean shareable = false;
}
