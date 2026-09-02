package com.pcare.support.domain;

import com.pcare.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One entry in a complaint's communication history (author/at come from BaseEntity audit). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "complaint_comment", indexes = {
        @Index(name = "ix_ccomment_complaint", columnList = "complaintId")
})
public class ComplaintComment extends BaseEntity {

    @Column(nullable = false)
    private Long complaintId;

    @Column(nullable = false, length = 1000)
    private String message;

    /** True when the comment marks an internal note vs a customer-facing reply. */
    @Column(nullable = false)
    private boolean internal = false;
}
