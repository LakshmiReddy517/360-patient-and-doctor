package com.pcare.patient.domain;

import com.pcare.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An audit record of who accessed a patient's medical document (blueprint point 14).
 * Written every time a document file is downloaded/viewed, so access to sensitive records is traceable.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "document_access_log", indexes = {
        @Index(name = "ix_docaccess_document", columnList = "documentId"),
        @Index(name = "ix_docaccess_patient", columnList = "patientId")
})
public class DocumentAccessLog extends BaseEntity {

    @Column(nullable = false)
    private Long documentId;

    private Long patientId;

    /** VIEW or DOWNLOAD. */
    @Column(nullable = false, length = 20)
    private String action = "DOWNLOAD";

    private Long accessedByUserId;

    @Column(length = 120)
    private String accessedByName;

    /** The role/context the accessor acted in, for auditing (e.g. DOCTOR, AGENT). */
    @Column(length = 40)
    private String accessedByRole;
}
