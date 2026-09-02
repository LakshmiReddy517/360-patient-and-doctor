package com.pcare.patient.service;

import com.pcare.common.exception.NotFoundException;
import com.pcare.patient.domain.Consent;
import com.pcare.patient.domain.ConsentStatus;
import com.pcare.patient.domain.Guardian;
import com.pcare.patient.domain.MedicalDocument;
import com.pcare.patient.domain.MedicalProfile;
import com.pcare.patient.domain.Patient;
import com.pcare.patient.repo.ConsentRepository;
import com.pcare.patient.repo.GuardianRepository;
import com.pcare.patient.repo.MedicalDocumentRepository;
import com.pcare.patient.repo.MedicalProfileRepository;
import com.pcare.patient.repo.PatientRepository;
import com.pcare.patient.web.dto.PatientDtos.ConsentDto;
import com.pcare.patient.web.dto.PatientDtos.GuardianDto;
import com.pcare.patient.web.dto.PatientDtos.MedicalDocumentDto;
import com.pcare.patient.web.dto.PatientDtos.MedicalProfileDto;
import com.pcare.patient.web.dto.PatientDtos.PatientDto;
import com.pcare.patient.web.dto.PatientDtos.PatientSummaryDto;
import com.pcare.patient.web.dto.PatientDtos.RegistrationWizardRequest;
import com.pcare.patient.web.dto.PatientDtos.UpsertConsentRequest;
import com.pcare.patient.web.dto.PatientDtos.UpsertGuardianRequest;
import com.pcare.patient.web.dto.PatientDtos.UpsertMedicalProfileRequest;
import com.pcare.patient.web.dto.PatientDtos.UpsertPatientRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

/** Patient profile, medical profile, guardians, consent and documents. */
@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final MedicalProfileRepository medicalRepository;
    private final GuardianRepository guardianRepository;
    private final ConsentRepository consentRepository;
    private final MedicalDocumentRepository documentRepository;
    private final FileStorageService fileStorage;

    public PatientService(PatientRepository patientRepository, MedicalProfileRepository medicalRepository,
                          GuardianRepository guardianRepository, ConsentRepository consentRepository,
                          MedicalDocumentRepository documentRepository, FileStorageService fileStorage) {
        this.patientRepository = patientRepository;
        this.medicalRepository = medicalRepository;
        this.guardianRepository = guardianRepository;
        this.consentRepository = consentRepository;
        this.documentRepository = documentRepository;
        this.fileStorage = fileStorage;
    }

    // ---------- Patient ----------
    @Transactional
    public Patient create(UpsertPatientRequest req) {
        Patient p = new Patient();
        apply(p, req);
        return patientRepository.save(p);
    }

    @Transactional
    public Patient update(Long id, UpsertPatientRequest req) {
        Patient p = get(id);
        apply(p, req);
        return patientRepository.save(p);
    }

    @Transactional(readOnly = true)
    public Patient get(Long id) {
        return patientRepository.findById(id).orElseThrow(() -> NotFoundException.of("Patient", id));
    }

    @Transactional(readOnly = true)
    public Page<PatientSummaryDto> search(String q, Pageable pageable) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        return patientRepository.search(query, pageable).map(this::toSummary);
    }

    @Transactional
    public void archive(Long id) {
        Patient p = get(id);
        p.setArchived(true);
        patientRepository.save(p);
    }

    private void apply(Patient p, UpsertPatientRequest r) {
        p.setFullName(r.fullName());
        p.setDateOfBirth(r.dateOfBirth());
        p.setGender(r.gender());
        p.setMobile(r.mobile());
        p.setEmail(r.email());
        p.setAddressLine(r.addressLine());
        p.setCity(r.city());
        p.setState(r.state());
        p.setCountry(r.country());
        p.setPincode(r.pincode());
        p.setEmergencyContactName(r.emergencyContactName());
        p.setEmergencyContactMobile(r.emergencyContactMobile());
        p.setEmergencyContactRelation(r.emergencyContactRelation());
        p.setPreferredLanguage(r.preferredLanguage());
        p.setIdentityType(r.identityType());
        p.setIdentityNumber(r.identityNumber());
        p.setInsuranceProvider(r.insuranceProvider());
        p.setInsuranceNumber(r.insuranceNumber());
    }

    // ---------- Medical profile ----------
    @Transactional
    public MedicalProfile upsertMedical(Long patientId, UpsertMedicalProfileRequest r) {
        get(patientId); // ensure patient exists
        MedicalProfile m = medicalRepository.findByPatientId(patientId).orElseGet(() -> {
            MedicalProfile nm = new MedicalProfile();
            nm.setPatientId(patientId);
            return nm;
        });
        m.setCurrentProblem(r.currentProblem());
        m.setDiagnosisHistory(r.diagnosisHistory());
        m.setChronicDiseases(r.chronicDiseases());
        m.setSurgeries(r.surgeries());
        m.setPreviousHospitalisations(r.previousHospitalisations());
        m.setAllergies(r.allergies());
        m.setCurrentMedicines(r.currentMedicines());
        m.setBloodGroup(r.bloodGroup());
        m.setDisabilityOrMobility(r.disabilityOrMobility());
        m.setRelevantHistory(r.relevantHistory());
        return medicalRepository.save(m);
    }

    @Transactional(readOnly = true)
    public MedicalProfileDto getMedical(Long patientId) {
        return medicalRepository.findByPatientId(patientId).map(this::toMedicalDto).orElse(null);
    }

    // ---------- Guardians ----------
    @Transactional
    public Guardian addGuardian(Long patientId, UpsertGuardianRequest r) {
        get(patientId);
        Guardian g = new Guardian();
        g.setPatientId(patientId);
        g.setFullName(r.fullName());
        g.setRelationship(r.relationship());
        g.setMobile(r.mobile());
        g.setEmail(r.email());
        g.setPermissions(r.permissions() == null ? new HashSet<>() : new HashSet<>(r.permissions()));
        return guardianRepository.save(g);
    }

    @Transactional(readOnly = true)
    public List<GuardianDto> listGuardians(Long patientId) {
        return guardianRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream().map(this::toGuardianDto).toList();
    }

    @Transactional
    public void deleteGuardian(Long guardianId) {
        guardianRepository.deleteById(guardianId);
    }

    // ---------- Consent ----------
    @Transactional
    public Consent addConsent(Long patientId, UpsertConsentRequest r) {
        get(patientId);
        Consent c = new Consent();
        c.setPatientId(patientId);
        c.setScope(r.scope());
        c.setGrantedTo(r.grantedTo());
        c.setPurpose(r.purpose());
        c.setValidFrom(r.validFrom() != null ? r.validFrom() : LocalDate.now());
        c.setValidTo(r.validTo());
        c.setStatus(r.status() != null ? r.status() : ConsentStatus.GRANTED);
        return consentRepository.save(c);
    }

    @Transactional
    public Consent revokeConsent(Long consentId) {
        Consent c = consentRepository.findById(consentId).orElseThrow(() -> NotFoundException.of("Consent", consentId));
        c.setStatus(ConsentStatus.REVOKED);
        return consentRepository.save(c);
    }

    @Transactional(readOnly = true)
    public List<ConsentDto> listConsents(Long patientId) {
        return consentRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream().map(this::toConsentDto).toList();
    }

    // ---------- Documents ----------
    @Transactional
    public MedicalDocument addDocument(Long patientId, MedicalDocument meta, MultipartFile file) {
        get(patientId);
        meta.setPatientId(patientId);
        if (file != null && !file.isEmpty()) {
            meta.setStoredFileName(fileStorage.store(file));
            meta.setOriginalFileName(file.getOriginalFilename());
            meta.setContentType(file.getContentType());
            meta.setSizeBytes(file.getSize());
        }
        return documentRepository.save(meta);
    }

    @Transactional(readOnly = true)
    public List<MedicalDocumentDto> listDocuments(Long patientId) {
        return documentRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream().map(this::toDocumentDto).toList();
    }

    @Transactional(readOnly = true)
    public MedicalDocument getDocument(Long documentId) {
        return documentRepository.findById(documentId).orElseThrow(() -> NotFoundException.of("Document", documentId));
    }

    // ---------- Registration wizard ----------
    @Transactional
    public Patient register(RegistrationWizardRequest req) {
        Patient p = create(req.profile());
        if (req.medical() != null) {
            upsertMedical(p.getId(), req.medical());
        }
        if (req.guardians() != null) {
            req.guardians().forEach(g -> addGuardian(p.getId(), g));
        }
        if (req.consents() != null) {
            req.consents().forEach(c -> addConsent(p.getId(), c));
        }
        return p;
    }

    // ---------- Mapping ----------
    public PatientSummaryDto toSummary(Patient p) {
        return new PatientSummaryDto(p.getId(), p.getFullName(), p.getGender(), p.getMobile(),
                p.getCity(), p.getDateOfBirth(), p.getCreatedAt());
    }

    public PatientDto toDto(Patient p) {
        return new PatientDto(p.getId(), p.getUserId(), p.getFullName(), p.getDateOfBirth(), p.getGender(),
                p.getMobile(), p.getEmail(), p.getAddressLine(), p.getCity(), p.getState(), p.getCountry(),
                p.getPincode(), p.getEmergencyContactName(), p.getEmergencyContactMobile(),
                p.getEmergencyContactRelation(), p.getPreferredLanguage(), p.getIdentityType(),
                p.getIdentityNumber(), p.getInsuranceProvider(), p.getInsuranceNumber(),
                p.getCreatedAt(), p.getUpdatedAt());
    }

    private MedicalProfileDto toMedicalDto(MedicalProfile m) {
        return new MedicalProfileDto(m.getId(), m.getPatientId(), m.getCurrentProblem(), m.getDiagnosisHistory(),
                m.getChronicDiseases(), m.getSurgeries(), m.getPreviousHospitalisations(), m.getAllergies(),
                m.getCurrentMedicines(), m.getBloodGroup(), m.getDisabilityOrMobility(), m.getRelevantHistory());
    }

    private GuardianDto toGuardianDto(Guardian g) {
        return new GuardianDto(g.getId(), g.getPatientId(), g.getFullName(), g.getRelationship(),
                g.getMobile(), g.getEmail(), g.getPermissions());
    }

    private ConsentDto toConsentDto(Consent c) {
        return new ConsentDto(c.getId(), c.getPatientId(), c.getScope(), c.getGrantedTo(), c.getPurpose(),
                c.getValidFrom(), c.getValidTo(), c.getStatus(), c.getCreatedAt());
    }

    public MedicalDocumentDto toDocumentDto(MedicalDocument d) {
        return new MedicalDocumentDto(d.getId(), d.getPatientId(), d.getType(), d.getTitle(), d.getDocumentDate(),
                d.getSource(), d.getDescription(), d.getOriginalFileName(), d.getContentType(), d.getSizeBytes(),
                d.isShareable(), d.getCreatedBy(), d.getCreatedAt());
    }
}
