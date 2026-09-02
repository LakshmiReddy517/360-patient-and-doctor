package com.pcare.config;

import com.pcare.casefile.domain.CasePriority;
import com.pcare.casefile.repo.CaseFileRepository;
import com.pcare.casefile.service.CaseService;
import com.pcare.casefile.web.dto.CaseDtos.CreateCaseRequest;
import com.pcare.identity.domain.Role;
import com.pcare.identity.domain.User;
import com.pcare.identity.repo.UserRepository;
import com.pcare.patient.domain.Gender;
import com.pcare.patient.domain.GuardianPermission;
import com.pcare.patient.repo.PatientRepository;
import com.pcare.patient.service.PatientService;
import com.pcare.patient.web.dto.PatientDtos.UpsertConsentRequest;
import com.pcare.patient.web.dto.PatientDtos.UpsertGuardianRequest;
import com.pcare.patient.web.dto.PatientDtos.UpsertMedicalProfileRequest;
import com.pcare.patient.web.dto.PatientDtos.UpsertPatientRequest;
import com.pcare.patient.domain.Patient;
import com.pcare.servicerequest.domain.ServiceType;
import com.pcare.servicerequest.repo.ServiceRequestRepository;
import com.pcare.servicerequest.service.ServiceRequestService;
import com.pcare.servicerequest.web.dto.ServiceRequestDtos.CreateRequest;
import com.pcare.billing.domain.PackageCategory;
import com.pcare.billing.repo.RateCardRepository;
import com.pcare.billing.repo.ServicePackageRepository;
import com.pcare.billing.service.CatalogService;
import com.pcare.billing.web.dto.BillingDtos.PackageItemDto;
import com.pcare.billing.web.dto.BillingDtos.UpsertPackageRequest;
import com.pcare.billing.web.dto.BillingDtos.UpsertRateCardRequest;
import com.pcare.dispatch.domain.Agent;
import com.pcare.dispatch.domain.AgentStatus;
import com.pcare.dispatch.repo.AgentRepository;
import com.pcare.dispatch.service.AgentService;
import com.pcare.dispatch.web.dto.DispatchDtos.UpsertAgentRequest;
import com.pcare.healthcare.domain.BedStatus;
import com.pcare.healthcare.domain.Hospital;
import com.pcare.healthcare.repo.HospitalRepository;
import com.pcare.healthcare.service.DoctorService;
import com.pcare.healthcare.service.HospitalService;
import com.pcare.healthcare.web.dto.HealthcareDtos.UpsertBedRequest;
import com.pcare.healthcare.web.dto.HealthcareDtos.UpsertDoctorRequest;
import com.pcare.healthcare.web.dto.HealthcareDtos.UpsertHospitalRequest;
import com.pcare.fleet.domain.Ambulance;
import com.pcare.fleet.domain.AmbulanceCategory;
import com.pcare.fleet.repo.AmbulanceRepository;
import com.pcare.fleet.service.EmtService;
import com.pcare.fleet.service.FleetService;
import com.pcare.fleet.web.dto.FleetDtos.ReadinessRequest;
import com.pcare.fleet.web.dto.FleetDtos.UpsertAmbulanceRequest;
import com.pcare.fleet.web.dto.FleetDtos.UpsertEmtRequest;
import com.pcare.care.repo.CaretakerRepository;
import com.pcare.care.service.CareService;
import com.pcare.care.web.dto.CareDtos.UpsertCaretakerRequest;
import com.pcare.notification.domain.NotificationEnums.Category;
import com.pcare.notification.domain.NotificationEnums.Channel;
import com.pcare.notification.repo.NotificationRepository;
import com.pcare.notification.service.NotificationService;
import com.pcare.notification.service.PreferenceService;
import com.pcare.notification.web.dto.NotificationDtos.SendRequest;
import com.pcare.notification.web.dto.NotificationDtos.UpsertPreferenceRequest;
import com.pcare.support.domain.SupportEnums.IncidentSeverity;
import com.pcare.support.domain.SupportEnums.IncidentType;
import com.pcare.support.domain.SupportEnums.Priority;
import com.pcare.support.repo.ComplaintRepository;
import com.pcare.support.repo.IncidentRepository;
import com.pcare.support.repo.SlaPolicyRepository;
import com.pcare.support.service.ComplaintService;
import com.pcare.support.service.IncidentService;
import com.pcare.support.web.dto.SupportDtos.CreateComplaintRequest;
import com.pcare.support.web.dto.SupportDtos.CreateIncidentRequest;
import com.pcare.support.web.dto.SupportDtos.UpsertSlaPolicyRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Seeds default accounts and a little sample data so the platform is demoable on localhost
 * immediately. Idempotent — only creates what is missing. Disable with app.seed.enabled=false.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CaseService caseService;
    private final CaseFileRepository caseRepository;
    private final PatientService patientService;
    private final PatientRepository patientRepository;
    private final ServiceRequestService requestService;
    private final ServiceRequestRepository requestRepository;
    private final CatalogService catalogService;
    private final RateCardRepository rateCardRepository;
    private final ServicePackageRepository packageRepository;
    private final AgentService agentService;
    private final AgentRepository agentRepository;
    private final HospitalService hospitalService;
    private final HospitalRepository hospitalRepository;
    private final DoctorService doctorService;
    private final FleetService fleetService;
    private final AmbulanceRepository ambulanceRepository;
    private final EmtService emtService;
    private final CareService careService;
    private final CaretakerRepository caretakerRepository;
    private final NotificationService notificationService;
    private final PreferenceService preferenceService;
    private final NotificationRepository notificationRepository;
    private final ComplaintService complaintService;
    private final IncidentService incidentService;
    private final ComplaintRepository complaintRepository;
    private final IncidentRepository incidentRepository;
    private final SlaPolicyRepository slaPolicyRepository;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder,
                      CaseService caseService, CaseFileRepository caseRepository,
                      PatientService patientService, PatientRepository patientRepository,
                      ServiceRequestService requestService, ServiceRequestRepository requestRepository,
                      CatalogService catalogService, RateCardRepository rateCardRepository,
                      ServicePackageRepository packageRepository,
                      AgentService agentService, AgentRepository agentRepository,
                      HospitalService hospitalService, HospitalRepository hospitalRepository,
                      DoctorService doctorService,
                      FleetService fleetService, AmbulanceRepository ambulanceRepository, EmtService emtService,
                      CareService careService, CaretakerRepository caretakerRepository,
                      NotificationService notificationService, PreferenceService preferenceService,
                      NotificationRepository notificationRepository,
                      ComplaintService complaintService, IncidentService incidentService,
                      ComplaintRepository complaintRepository, IncidentRepository incidentRepository,
                      SlaPolicyRepository slaPolicyRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.caseService = caseService;
        this.caseRepository = caseRepository;
        this.patientService = patientService;
        this.patientRepository = patientRepository;
        this.requestService = requestService;
        this.requestRepository = requestRepository;
        this.catalogService = catalogService;
        this.rateCardRepository = rateCardRepository;
        this.packageRepository = packageRepository;
        this.agentService = agentService;
        this.agentRepository = agentRepository;
        this.hospitalService = hospitalService;
        this.hospitalRepository = hospitalRepository;
        this.doctorService = doctorService;
        this.fleetService = fleetService;
        this.ambulanceRepository = ambulanceRepository;
        this.emtService = emtService;
        this.careService = careService;
        this.caretakerRepository = caretakerRepository;
        this.notificationService = notificationService;
        this.preferenceService = preferenceService;
        this.notificationRepository = notificationRepository;
        this.complaintService = complaintService;
        this.incidentService = incidentService;
        this.complaintRepository = complaintRepository;
        this.incidentRepository = incidentRepository;
        this.slaPolicyRepository = slaPolicyRepository;
    }

    @Override
    public void run(String... args) {
        seedUser("admin", "admin123", "Command Centre Admin", Set.of(Role.SUPER_ADMIN, Role.ADMIN));
        seedUser("finance", "finance123", "Finance Officer", Set.of(Role.FINANCE));
        seedUser("agent", "agent123", "Care Agent Ravi", Set.of(Role.AGENT));
        seedUser("emt", "emt123", "Ambulance EMT Suresh", Set.of(Role.EMT));
        seedUser("patient", "patient123", "Demo Patient", Set.of(Role.PATIENT));

        seedCatalog();
        seedAgents();
        seedHealthcare();
        seedFleet();
        seedCaretakers();
        seedNotifications();
        seedSupport();
        seedPatients();
        seedRequests();

        if (caseRepository.count() == 0) {
            caseService.createCase(new CreateCaseRequest(null, "Anita Sharma", "9876500011",
                    "Airport pickup + hospital admission assistance", "Patient arriving alone, needs wheelchair support.",
                    CasePriority.NORMAL, false));
            caseService.createCase(new CreateCaseRequest(null, "Rahul Verma", "9876500022",
                    "Ambulance transfer to cardiac unit", "Chest pain reported, requires ALS ambulance.",
                    CasePriority.EMERGENCY, true));
            caseService.createCase(new CreateCaseRequest(null, "Meena Iyer", "9876500033",
                    "Caretaker + accommodation for 5 days", "Post-surgery care coordination.",
                    CasePriority.HIGH, false));
            log.info("Seeded sample cases");
        }
    }

    private void seedPatients() {
        if (patientRepository.count() > 0) {
            return;
        }
        Patient anita = patientService.create(new UpsertPatientRequest(
                "Anita Sharma", LocalDate.of(1958, 4, 12), Gender.FEMALE, "9876500011", "anita@example.com",
                "12 MG Road", "Bengaluru", "Karnataka", "India", "560001",
                "Rohit Sharma", "9876500012", "Son", "English",
                "Aadhaar", "XXXX-XXXX-1234", "Star Health", "SH-99881"));
        patientService.upsertMedical(anita.getId(), new UpsertMedicalProfileRequest(
                "Post-operative knee replacement care", "Osteoarthritis", "Hypertension, Type 2 Diabetes",
                "Right knee replacement (2026)", "None", "Penicillin", "Metformin 500mg, Amlodipine 5mg",
                "B+", "Uses walker post-surgery", "Under orthopaedic follow-up"));
        patientService.addGuardian(anita.getId(), new UpsertGuardianRequest(
                "Rohit Sharma", "Son", "9876500012", "rohit@example.com",
                Set.of(GuardianPermission.VIEW_LOCATION, GuardianPermission.RECEIVE_NOTIFICATIONS,
                        GuardianPermission.MAKE_PAYMENTS)));
        patientService.addConsent(anita.getId(), new UpsertConsentRequest(
                "Medical records", "Assigned care agent", "Coordinate hospital admission",
                LocalDate.now(), LocalDate.now().plusMonths(6), null));

        patientService.create(new UpsertPatientRequest(
                "Rahul Verma", LocalDate.of(1971, 9, 3), Gender.MALE, "9876500022", "rahul@example.com",
                "45 Residency Road", "Bengaluru", "Karnataka", "India", "560025",
                "Priya Verma", "9876500023", "Wife", "Hindi",
                null, null, null, null));

        patientService.create(new UpsertPatientRequest(
                "Meena Iyer", LocalDate.of(1985, 1, 22), Gender.FEMALE, "9876500033", "meena@example.com",
                "8 Indiranagar", "Bengaluru", "Karnataka", "India", "560038",
                "Karthik Iyer", "9876500034", "Brother", "Tamil",
                null, null, null, null));

        log.info("Seeded {} demo patients", patientRepository.count());
    }

    private void seedCatalog() {
        if (rateCardRepository.count() == 0) {
            rate(ServiceType.PICKUP_DROP, "Pickup & Drop", "per trip", 1200);
            rate(ServiceType.AMBULANCE, "Ambulance (BLS)", "per trip", 3500);
            rate(ServiceType.HOSPITAL_ADMISSION, "Hospital Admission Assistance", "per case", 2500);
            rate(ServiceType.DOCTOR_APPOINTMENT, "Doctor Appointment Assistance", "per visit", 800);
            rate(ServiceType.CARETAKER, "Caretaker", "per day", 1500);
            rate(ServiceType.ACCOMMODATION, "Accommodation", "per night", 2200);
            rate(ServiceType.FOOD, "Food", "per day", 450);
            rate(ServiceType.LOCAL_TRANSPORT, "Local Transport", "per trip", 600);
            rate(ServiceType.COMPLETE_CARE, "Complete Care", "per day", 4500);
            rate(ServiceType.FOLLOW_UP, "Follow-up", "per visit", 500);
            log.info("Seeded {} rate cards", rateCardRepository.count());
        }
        if (packageRepository.count() == 0) {
            catalogService.createPackage(new UpsertPackageRequest(
                    "Pickup & Drop", "Door-to-door patient pickup and drop.", PackageCategory.PICKUP_AND_DROP, true,
                    List.of(new PackageItemDto(ServiceType.PICKUP_DROP, "Pickup & Drop", BigDecimal.ONE, new BigDecimal("1200")))));
            catalogService.createPackage(new UpsertPackageRequest(
                    "Hospital Assistance", "Pickup, admission help and one follow-up.", PackageCategory.HOSPITAL_ASSISTANCE, true,
                    List.of(
                            new PackageItemDto(ServiceType.PICKUP_DROP, "Pickup & Drop", BigDecimal.ONE, new BigDecimal("1200")),
                            new PackageItemDto(ServiceType.HOSPITAL_ADMISSION, "Admission Assistance", BigDecimal.ONE, new BigDecimal("2500")),
                            new PackageItemDto(ServiceType.FOLLOW_UP, "Follow-up", BigDecimal.ONE, new BigDecimal("500")))));
            catalogService.createPackage(new UpsertPackageRequest(
                    "Complete Patient Care (5 days)", "Caretaker, accommodation and food for 5 days.", PackageCategory.COMPLETE_PATIENT_CARE, true,
                    List.of(
                            new PackageItemDto(ServiceType.CARETAKER, "Caretaker (5 days)", new BigDecimal("5"), new BigDecimal("1500")),
                            new PackageItemDto(ServiceType.ACCOMMODATION, "Accommodation (5 nights)", new BigDecimal("5"), new BigDecimal("2200")),
                            new PackageItemDto(ServiceType.FOOD, "Food (5 days)", new BigDecimal("5"), new BigDecimal("450")))));
            log.info("Seeded {} packages", packageRepository.count());
        }
    }

    private void seedAgents() {
        if (agentRepository.count() > 0) {
            return;
        }
        Long agentUserId = userRepository.findByUsernameIgnoreCase("agent").map(u -> u.getId()).orElse(null);
        Agent ravi = agentService.create(new UpsertAgentRequest(
                agentUserId, "Ravi Kumar", "9800000001", "ravi@care.example", "AG-001",
                Set.of("Wheelchair", "Elderly Care"), Set.of("English", "Hindi", "Kannada"), "Day", true));
        agentService.updateLocation(ravi.getId(), 12.9716, 77.5946);
        agentService.setStatus(ravi.getId(), AgentStatus.AVAILABLE);

        Agent asha = agentService.create(new UpsertAgentRequest(
                null, "Asha Menon", "9800000002", "asha@care.example", "AG-002",
                Set.of("First Aid", "Airport Assistance"), Set.of("English", "Malayalam", "Tamil"), "Day", true));
        agentService.updateLocation(asha.getId(), 12.9352, 77.6245);
        agentService.setStatus(asha.getId(), AgentStatus.AVAILABLE);

        agentService.create(new UpsertAgentRequest(
                null, "Mohan Das", "9800000003", "mohan@care.example", "AG-003",
                Set.of("Stretcher", "Elderly Care"), Set.of("Hindi", "Kannada"), "Night", false));
        log.info("Seeded {} agents", agentRepository.count());
    }

    private void seedHealthcare() {
        if (hospitalRepository.count() > 0) {
            return;
        }
        Hospital apollo = hospitalService.create(new UpsertHospitalRequest(
                "Apollo Hospital", "154 Bannerghatta Road", "Bengaluru", "Karnataka",
                "080-40001000", "care@apollo.example", true, "OPD 9am–5pm", true, true,
                Set.of("Cardiology", "Orthopaedics", "General Medicine", "Neurology"),
                Set.of("Emergency", "ICU", "Pharmacy", "Diagnostics")));
        for (int i = 1; i <= 6; i++) {
            hospitalService.addBed(new UpsertBedRequest(apollo.getId(), "Ward A", "3", "3" + String.format("%02d", i),
                    "A-" + i, i <= 2 ? "ICU" : "GENERAL", i == 3 ? BedStatus.OCCUPIED : BedStatus.AVAILABLE));
        }
        Hospital fortis = hospitalService.create(new UpsertHospitalRequest(
                "Fortis Hospital", "14 Cunningham Road", "Bengaluru", "Karnataka",
                "080-66214444", "care@fortis.example", true, "OPD 8am–8pm", true, true,
                Set.of("Cardiology", "Oncology", "Nephrology"),
                Set.of("Emergency", "Dialysis", "Pharmacy")));

        doctorService.create(new UpsertDoctorRequest(null, "Dr. Suresh Rao", "Cardiology", "Cardiology",
                apollo.getId(), apollo.getName(), "MBBS, MD, DM (Cardiology)", "KMC-11223",
                "9800012345", "suresh@apollo.example", new java.math.BigDecimal("800"), true));
        doctorService.create(new UpsertDoctorRequest(null, "Dr. Anjali Nair", "Orthopaedics", "Orthopaedics",
                apollo.getId(), apollo.getName(), "MBBS, MS (Ortho)", "KMC-22334",
                "9800023456", "anjali@apollo.example", new java.math.BigDecimal("700"), true));
        doctorService.create(new UpsertDoctorRequest(null, "Dr. Vikram Shah", "Oncology", "Oncology",
                fortis.getId(), fortis.getName(), "MBBS, MD, DM (Oncology)", "KMC-33445",
                "9800034567", "vikram@fortis.example", new java.math.BigDecimal("1000"), true));
        log.info("Seeded {} hospitals and doctors", hospitalRepository.count());
    }

    private void seedFleet() {
        if (ambulanceRepository.count() > 0) {
            return;
        }
        emtService.create(new UpsertEmtRequest(null, "Suresh Yadav", "9700000001", "KA-DL-778812",
                LocalDate.now().plusYears(3), "EMT-Basic", LocalDate.now().plusYears(2), true, true, "Day"));
        emtService.create(new UpsertEmtRequest(null, "Imran Khan", "9700000002", "KA-DL-990034",
                LocalDate.now().plusYears(1), "EMT-Paramedic", LocalDate.now().plusYears(2), true, true, "Night"));

        Ambulance als = fleetService.create(new UpsertAmbulanceRequest("AMB-ALS-01", "KA01AB1234",
                AmbulanceCategory.ALS, "City Care EMS", LocalDate.now().plusMonths(8), LocalDate.now().plusMonths(10),
                LocalDate.now().plusMonths(11), 1L, "Suresh Yadav"));
        // Pass a full readiness check so the ALS unit is dispatch-ready.
        fleetService.submitReadiness(als.getId(), new ReadinessRequest(
                true, true, true, true, true, true, true, true, true, true, true, true, 95, 80, "Pre-shift check OK"));
        fleetService.updateLocation(als.getId(), 12.9611, 77.6387);

        Ambulance bls = fleetService.create(new UpsertAmbulanceRequest("AMB-BLS-02", "KA01CD5678",
                AmbulanceCategory.BLS, "City Care EMS", LocalDate.now().plusMonths(6), LocalDate.now().plusMonths(9),
                LocalDate.now().plusMonths(7), 2L, "Imran Khan"));
        fleetService.submitReadiness(bls.getId(), new ReadinessRequest(
                true, true, true, true, true, true, true, true, false, true, false, true, 60, 90, "Suction not required for BLS"));

        fleetService.create(new UpsertAmbulanceRequest("AMB-PTV-03", "KA01EF9012",
                AmbulanceCategory.PATIENT_TRANSPORT, "Metro Ambulance", LocalDate.now().plusMonths(4),
                LocalDate.now().plusMonths(5), LocalDate.now().plusMonths(6), null, null));
        log.info("Seeded {} ambulances", ambulanceRepository.count());
    }

    private void seedSupport() {
        if (slaPolicyRepository.count() == 0) {
            complaintService.upsertSla(new UpsertSlaPolicyRequest(Priority.URGENT, 30, 240));
            complaintService.upsertSla(new UpsertSlaPolicyRequest(Priority.HIGH, 60, 480));
            complaintService.upsertSla(new UpsertSlaPolicyRequest(Priority.MEDIUM, 240, 1440));
            complaintService.upsertSla(new UpsertSlaPolicyRequest(Priority.LOW, 480, 2880));
        }
        if (complaintRepository.count() == 0) {
            complaintService.create(new CreateComplaintRequest(1L, "Anita Sharma",
                    "Pickup was delayed by 40 minutes", "Agent arrived late causing missed appointment slot.",
                    "Transport", Priority.HIGH));
            complaintService.create(new CreateComplaintRequest(null, "Rahul Verma",
                    "Billing discrepancy on invoice", "Charged for accommodation that was not used.",
                    "Billing", Priority.MEDIUM));
        }
        if (incidentRepository.count() == 0) {
            incidentService.create(new CreateIncidentRequest(2L, IncidentType.PATIENT_DETERIORATION,
                    IncidentSeverity.CRITICAL, "Patient's condition worsened during transit — requires immediate escalation."));
            incidentService.create(new CreateIncidentRequest(null, IncidentType.VEHICLE_BREAKDOWN,
                    IncidentSeverity.MEDIUM, "Ambulance KA01CD5678 reported a tyre puncture."));
        }
        log.info("Seeded SLA policies, complaints and incidents");
    }

    private void seedNotifications() {
        if (notificationRepository.count() > 0) {
            return;
        }
        Long patientUserId = userRepository.findByUsernameIgnoreCase("patient").map(u -> u.getId()).orElse(null);
        if (patientUserId != null) {
            // Patient opts in: payment on SMS+PUSH, appointment on WhatsApp+SMS; marketing left opt-out.
            preferenceService.upsert(new UpsertPreferenceRequest(patientUserId, Category.PAYMENT, true,
                    Set.of(Channel.SMS, Channel.PUSH)));
            preferenceService.upsert(new UpsertPreferenceRequest(patientUserId, Category.APPOINTMENT, true,
                    Set.of(Channel.WHATSAPP, Channel.SMS)));
            preferenceService.upsert(new UpsertPreferenceRequest(patientUserId, Category.MARKETING, false, Set.of()));
        }
        notificationService.send(new SendRequest(null, "Anita Sharma", "9876500011", Channel.WHATSAPP,
                Category.APPOINTMENT, "Appointment confirmed", "Your appointment is confirmed for tomorrow 10:30 AM.", 1L));
        notificationService.send(new SendRequest(null, "Rahul Verma", "9876500022", Channel.SMS,
                Category.EMERGENCY, "Ambulance dispatched", "An ALS ambulance is en route to your location.", 2L));
        log.info("Seeded notifications and patient preferences");
    }

    private void seedCaretakers() {
        if (caretakerRepository.count() > 0) {
            return;
        }
        careService.createCaretaker(new UpsertCaretakerRequest(null, "Lakshmi Devi", "9600000001",
                Set.of("Elderly Care", "Post-op Care"), Set.of("Kannada", "Hindi", "English"), "Day",
                new BigDecimal("150"), true, true));
        careService.createCaretaker(new UpsertCaretakerRequest(null, "Joseph Thomas", "9600000002",
                Set.of("Physiotherapy Support", "Bedridden Care"), Set.of("Malayalam", "English"), "Night",
                new BigDecimal("180"), true, true));
        log.info("Seeded {} caretakers", caretakerRepository.count());
    }

    private void rate(ServiceType type, String label, String unit, int amount) {
        catalogService.upsertRateCard(new UpsertRateCardRequest(type, label, unit, new BigDecimal(amount), true));
    }

    private void seedRequests() {
        if (requestRepository.count() > 0) {
            return;
        }
        requestService.create(new CreateRequest(
                null, "Sunita Rao", "9876511001",
                Set.of(ServiceType.PICKUP_DROP, ServiceType.HOSPITAL_ADMISSION),
                "Patient arriving alone from Delhi, needs wheelchair and admission help.", false,
                null, null));
        requestService.create(new CreateRequest(
                null, "Vikram Nair", "9876511002",
                Set.of(ServiceType.AMBULANCE),
                "Severe breathing difficulty, needs ambulance urgently.", true,
                null, null));
        log.info("Seeded {} demo service requests", requestRepository.count());
    }

    private void seedUser(String username, String rawPassword, String fullName, Set<Role> roles) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        roles.forEach(user::addRole);
        userRepository.save(user);
        log.info("Seeded user '{}' with roles {}", username, roles);
    }
}
