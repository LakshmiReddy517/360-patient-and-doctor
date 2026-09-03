package com.pcare.identity.service;

import com.pcare.common.exception.BadRequestException;
import com.pcare.identity.domain.Role;
import com.pcare.identity.domain.User;
import com.pcare.identity.repo.UserRepository;
import com.pcare.identity.web.dto.AuthDtos.CreateStaffRequest;
import com.pcare.identity.web.dto.AuthDtos.RegisterPatientRequest;
import com.pcare.identity.web.dto.AuthDtos.UserDto;
import com.pcare.patient.domain.Patient;
import com.pcare.patient.repo.PatientRepository;
import com.pcare.patient.service.FileStorageService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/** Account creation and mapping. Patient self-registration and staff provisioning. */
@Service
public class UserService {

    private static final java.util.regex.Pattern AADHAAR = java.util.regex.Pattern.compile("\\d{12}");
    private static final java.util.regex.Pattern PAN = java.util.regex.Pattern.compile("[A-Z]{5}[0-9]{4}[A-Z]");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorage;
    private final PatientRepository patientRepository;
    private final org.springframework.context.ApplicationEventPublisher events;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, FileStorageService fileStorage,
                       PatientRepository patientRepository, org.springframework.context.ApplicationEventPublisher events) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileStorage = fileStorage;
        this.patientRepository = patientRepository;
        this.events = events;
    }

    /** Sends a welcome / ID-verified notification when a patient self-registers. */
    private void welcomeNotify(User user) {
        if (user.getMobile() == null && user.getEmail() == null) return;
        String msg = user.isIdVerified()
                ? "Welcome to 360 Patient Care! Your identity is verified — you can now book care services."
                : "Welcome to 360 Patient Care! Your account is ready.";
        events.publishEvent(new com.pcare.common.event.NotificationRequestedEvent(
                user.getId(), user.getFullName(), user.getMobile() != null ? user.getMobile() : user.getEmail(),
                "WHATSAPP", "GENERAL", "Welcome to 360 Patient Care", msg, null));
    }

    /**
     * Materializes a Patient master record for a self-registered patient account so that consent,
     * medical documents, appointments and clinical records can attach to the patient's own record.
     * Idempotent — no-op if a Patient already exists for this user.
     */
    private Patient linkPatient(User user) {
        return patientRepository.findByUserId(user.getId()).orElseGet(() -> {
            Patient p = new Patient();
            p.setUserId(user.getId());
            p.setFullName(user.getFullName() != null ? user.getFullName() : user.getUsername());
            p.setMobile(user.getMobile());
            p.setEmail(user.getEmail());
            return patientRepository.save(p);
        });
    }

    /** Patient self-registration that also stores the uploaded Aadhaar/PAN document images. */
    @Transactional
    public User registerPatientWithDocs(String username, String password, String fullName, String fatherName,
                                        String mobile, String email, String aadhaar, String pan,
                                        MultipartFile aadhaarImage, MultipartFile panImage) {
        if (aadhaar == null || !AADHAAR.matcher(aadhaar.trim()).matches()) {
            throw new BadRequestException("Aadhaar must be 12 digits");
        }
        if (pan == null || !PAN.matcher(pan.trim().toUpperCase()).matches()) {
            throw new BadRequestException("PAN must look like ABCDE1234F");
        }
        ensureUsernameFree(username);
        User user = new User();
        user.setUsername(username.trim());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setFatherName(fatherName);
        user.setMobile(mobile);
        user.setEmail(email);
        user.setAadhaar(aadhaar.trim());
        user.setPan(pan.trim().toUpperCase());
        if (aadhaarImage != null && !aadhaarImage.isEmpty()) user.setAadhaarImage(fileStorage.store(aadhaarImage));
        if (panImage != null && !panImage.isEmpty()) user.setPanImage(fileStorage.store(panImage));
        user.setIdVerified(true);
        user.addRole(Role.PATIENT);
        User saved = userRepository.save(user);
        linkPatient(saved);
        welcomeNotify(saved);
        return saved;
    }

    @Transactional
    public User registerPatient(RegisterPatientRequest req) {
        ensureUsernameFree(req.username());
        User user = new User();
        user.setUsername(req.username().trim());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setFullName(req.fullName());
        user.setFatherName(req.fatherName());
        user.setMobile(req.mobile());
        user.setEmail(req.email());
        user.setAadhaar(req.aadhaar());
        user.setPan(req.pan() == null ? null : req.pan().toUpperCase());
        // Aadhaar/PAN passed the format checks in the request DTO; mark as verified.
        // (Live UIDAI/NSDL verification can replace this flag once API access is provisioned.)
        user.setIdVerified(req.aadhaar() != null && req.pan() != null);
        user.addRole(Role.PATIENT);
        User saved = userRepository.save(user);
        linkPatient(saved);
        welcomeNotify(saved);
        return saved;
    }

    @Transactional
    public User createStaff(CreateStaffRequest req) {
        ensureUsernameFree(req.username());
        User user = new User();
        user.setUsername(req.username().trim());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setFullName(req.fullName());
        user.setMobile(req.mobile());
        user.setEmail(req.email());
        Set<Role> roles = (req.roles() == null || req.roles().isEmpty()) ? Set.of(Role.AGENT) : req.roles();
        roles.forEach(user::addRole);
        return userRepository.save(user);
    }

    private void ensureUsernameFree(String username) {
        if (userRepository.existsByUsernameIgnoreCase(username.trim())) {
            throw new BadRequestException("Username already registered: " + username);
        }
    }

    public UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getFullName(),
                user.getMobile(), user.getEmail(), user.isEnabled(), user.getRoles());
    }
}
