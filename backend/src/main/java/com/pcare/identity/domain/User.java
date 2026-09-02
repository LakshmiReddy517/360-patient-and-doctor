package com.pcare.identity.domain;

import com.pcare.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
 * A platform account for any human actor (staff or patient/guardian). Authentication is by
 * username (email or mobile). A user may hold multiple roles.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "app_user", indexes = {
        @Index(name = "ux_user_username", columnList = "username", unique = true)
})
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 120)
    private String username;

    @Column(nullable = false, length = 120)
    private String passwordHash;

    @Column(length = 160)
    private String fullName;

    @Column(length = 160)
    private String fatherName;

    @Column(length = 30)
    private String mobile;

    @Column(length = 160)
    private String email;

    /** Government ID proof captured at registration (KYC). */
    @Column(length = 12)
    private String aadhaar;

    @Column(length = 10)
    private String pan;

    /** Stored file names of the uploaded Aadhaar / PAN document images. */
    @Column(length = 100)
    private String aadhaarImage;

    @Column(length = 100)
    private String panImage;

    /** Whether the government IDs passed format validation / verification. */
    @Column(nullable = false)
    private boolean idVerified = false;

    @Column(nullable = false)
    private boolean enabled = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 40)
    private Set<Role> roles = new HashSet<>();

    public void addRole(Role role) {
        roles.add(role);
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }
}
