package com.pcare.clinical.service;

import com.pcare.casefile.domain.CaseEventType;
import com.pcare.casefile.domain.CaseFile;
import com.pcare.casefile.service.CaseService;
import com.pcare.clinical.domain.MedicationIntake;
import com.pcare.clinical.domain.Medicine;
import com.pcare.clinical.domain.PatientCondition;
import com.pcare.clinical.repo.MedicationIntakeRepository;
import com.pcare.clinical.repo.MedicineRepository;
import com.pcare.clinical.repo.PatientConditionRepository;
import com.pcare.clinical.web.dto.ClinicalDtos.*;
import com.pcare.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Medicines, medication intake and patient condition/vitals — clinician-recorded, case-linked. */
@Service
public class ClinicalService {

    private final MedicineRepository medicineRepository;
    private final MedicationIntakeRepository intakeRepository;
    private final PatientConditionRepository conditionRepository;
    private final CaseService caseService;

    public ClinicalService(MedicineRepository medicineRepository, MedicationIntakeRepository intakeRepository,
                           PatientConditionRepository conditionRepository, CaseService caseService) {
        this.medicineRepository = medicineRepository;
        this.intakeRepository = intakeRepository;
        this.conditionRepository = conditionRepository;
        this.caseService = caseService;
    }

    // ---- Medicines ----
    @Transactional
    public Medicine prescribe(UpsertMedicineRequest r) {
        CaseFile caseFile = caseService.get(r.caseId());
        Medicine m = new Medicine();
        m.setCaseId(caseFile.getId());
        m.setPatientId(r.patientId() != null ? r.patientId() : caseFile.getPatientId());
        m.setName(r.name());
        m.setDose(r.dose());
        m.setRoute(r.route());
        m.setFrequency(r.frequency());
        m.setStartDate(r.startDate());
        m.setEndDate(r.endDate());
        m.setFoodInstruction(r.foodInstruction());
        m.setPrescribedBy(r.prescribedBy());
        m.setInstructions(r.instructions());
        Medicine saved = medicineRepository.save(m);
        caseService.addEvent(caseFile.getId(), CaseEventType.CARE_ACTIVITY,
                "Medicine prescribed: " + saved.getName() + (saved.getDose() != null ? " " + saved.getDose() : "")
                        + (saved.getFrequency() != null ? " (" + saved.getFrequency() + ")" : ""), "CLINICAL");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<MedicineDto> medicinesByCase(Long caseId) {
        return medicineRepository.findByCaseIdOrderByCreatedAtDesc(caseId).stream().map(this::toMedicineDto).toList();
    }

    @Transactional
    public MedicationIntake recordIntake(Long medicineId, RecordIntakeRequest r) {
        Medicine m = medicineRepository.findById(medicineId).orElseThrow(() -> NotFoundException.of("Medicine", medicineId));
        MedicationIntake i = new MedicationIntake();
        i.setMedicineId(medicineId);
        i.setCaseId(m.getCaseId());
        i.setMedicineName(m.getName());
        i.setStatus(r.status());
        i.setNote(r.note());
        MedicationIntake saved = intakeRepository.save(i);
        if (m.getCaseId() != null) {
            caseService.addEvent(m.getCaseId(), CaseEventType.CARE_ACTIVITY,
                    "Medication " + r.status() + ": " + m.getName(), "CLINICAL");
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<IntakeDto> intakeByCase(Long caseId) {
        return intakeRepository.findByCaseIdOrderByCreatedAtDesc(caseId).stream().map(this::toIntakeDto).toList();
    }

    // ---- Patient condition / vitals ----
    @Transactional
    public PatientCondition recordCondition(UpsertConditionRequest r) {
        CaseFile caseFile = caseService.get(r.caseId());
        PatientCondition c = new PatientCondition();
        c.setCaseId(caseFile.getId());
        c.setPatientId(r.patientId() != null ? r.patientId() : caseFile.getPatientId());
        c.setCondition(r.condition());
        c.setTemperature(r.temperature());
        c.setBloodPressure(r.bloodPressure());
        c.setPulse(r.pulse());
        c.setSpo2(r.spo2());
        c.setPainScore(r.painScore());
        c.setNotes(r.notes());
        PatientCondition saved = conditionRepository.save(c);
        caseService.addEvent(caseFile.getId(), CaseEventType.CARE_ACTIVITY,
                "Vitals recorded" + (r.bloodPressure() != null ? " BP " + r.bloodPressure() : "")
                        + (r.spo2() != null ? " SpO2 " + r.spo2() : ""), "CLINICAL");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ConditionDto> conditionsByCase(Long caseId) {
        return conditionRepository.findByCaseIdOrderByCreatedAtDesc(caseId).stream().map(this::toConditionDto).toList();
    }

    // ---- mapping ----
    public MedicineDto toMedicineDto(Medicine m) {
        return new MedicineDto(m.getId(), m.getPatientId(), m.getCaseId(), m.getName(), m.getDose(), m.getRoute(),
                m.getFrequency(), m.getStartDate(), m.getEndDate(), m.getFoodInstruction(), m.getPrescribedBy(),
                m.getInstructions(), m.isActive(), m.getCreatedAt());
    }

    public IntakeDto toIntakeDto(MedicationIntake i) {
        return new IntakeDto(i.getId(), i.getMedicineId(), i.getCaseId(), i.getMedicineName(), i.getStatus(),
                i.getNote(), i.getCreatedBy(), i.getCreatedAt());
    }

    public ConditionDto toConditionDto(PatientCondition c) {
        return new ConditionDto(c.getId(), c.getPatientId(), c.getCaseId(), c.getCondition(), c.getTemperature(),
                c.getBloodPressure(), c.getPulse(), c.getSpo2(), c.getPainScore(), c.getNotes(),
                c.getCreatedBy(), c.getCreatedAt());
    }
}
