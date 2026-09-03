export type Role =
  | 'SUPER_ADMIN' | 'ADMIN' | 'FINANCE' | 'AGENT' | 'EMT'
  | 'DOCTOR' | 'HOSPITAL' | 'CARETAKER' | 'PATIENT';

export interface User {
  id: number;
  username: string;
  fullName: string;
  mobile?: string;
  email?: string;
  enabled: boolean;
  roles: Role[];
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresInMinutes: number;
  user: User;
}

export type CaseStatus = 'DRAFT' | 'OPEN' | 'IN_PROGRESS' | 'ON_HOLD' | 'COMPLETED' | 'CLOSED' | 'CANCELLED';
export type CasePriority = 'LOW' | 'NORMAL' | 'HIGH' | 'EMERGENCY';

export interface CaseSummary {
  id: number;
  caseNumber: string;
  patientId?: number;
  patientName: string;
  patientMobile?: string;
  title?: string;
  status: CaseStatus;
  priority: CasePriority;
  emergency: boolean;
  assignedToUserId?: number;
  assignedToName?: string;
  slaTargetAt?: string;
  slaMetAt?: string;
  slaBreached?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface TimelineEvent {
  id: number;
  type: string;
  description: string;
  source?: string;
  createdBy?: string;
  createdAt: string;
}

export interface CaseDetail {
  caseFile: CaseSummary;
  summary?: string;
  timeline: TimelineEvent[];
}

export interface DashboardStats {
  openCases: number;
  inProgressCases: number;
  onHoldCases: number;
  emergencies: number;
  closedCases: number;
  totalCases: number;
  slaBreached: number;
}

// ---- Patient module ----
export type Gender = 'MALE' | 'FEMALE' | 'OTHER' | 'UNDISCLOSED';
export type GuardianPermission =
  | 'VIEW_LOCATION' | 'VIEW_RECORDS' | 'RECEIVE_NOTIFICATIONS' | 'APPROVE_SERVICES' | 'MAKE_PAYMENTS';
export type ConsentStatus = 'GRANTED' | 'REVOKED' | 'EXPIRED';
export type DocumentType =
  | 'ID_PROOF' | 'PRESCRIPTION' | 'LAB_REPORT' | 'IMAGING' | 'DISCHARGE_SUMMARY'
  | 'MEDICAL_CERTIFICATE' | 'INSURANCE_DOCUMENT' | 'OTHER';

export interface PatientSummary {
  id: number;
  fullName: string;
  gender?: Gender;
  mobile?: string;
  city?: string;
  dateOfBirth?: string;
  createdAt: string;
}

export interface Patient {
  id: number;
  userId?: number;
  fullName: string;
  dateOfBirth?: string;
  gender?: Gender;
  mobile?: string;
  email?: string;
  addressLine?: string;
  city?: string;
  state?: string;
  country?: string;
  pincode?: string;
  emergencyContactName?: string;
  emergencyContactMobile?: string;
  emergencyContactRelation?: string;
  preferredLanguage?: string;
  identityType?: string;
  identityNumber?: string;
  insuranceProvider?: string;
  insuranceNumber?: string;
  createdAt: string;
  updatedAt: string;
}

export interface MedicalProfile {
  id: number;
  patientId: number;
  currentProblem?: string;
  diagnosisHistory?: string;
  chronicDiseases?: string;
  surgeries?: string;
  previousHospitalisations?: string;
  allergies?: string;
  currentMedicines?: string;
  bloodGroup?: string;
  disabilityOrMobility?: string;
  relevantHistory?: string;
}

export interface Guardian {
  id: number;
  patientId: number;
  fullName: string;
  relationship?: string;
  mobile?: string;
  email?: string;
  permissions: GuardianPermission[];
}

export interface Consent {
  id: number;
  patientId: number;
  scope: string;
  grantedTo: string;
  purpose?: string;
  validFrom?: string;
  validTo?: string;
  status: ConsentStatus;
  createdAt: string;
}

export interface MedicalDocument {
  id: number;
  patientId: number;
  type: DocumentType;
  title?: string;
  documentDate?: string;
  source?: string;
  description?: string;
  originalFileName?: string;
  contentType?: string;
  sizeBytes?: number;
  shareable: boolean;
  uploadedBy?: string;
  createdAt: string;
}

// ---- Service Request module ----
export type ServiceType =
  | 'PICKUP_DROP' | 'AMBULANCE' | 'HOSPITAL_ADMISSION' | 'DOCTOR_APPOINTMENT' | 'CARETAKER'
  | 'ACCOMMODATION' | 'FOOD' | 'LOCAL_TRANSPORT' | 'COMPLETE_CARE' | 'FOLLOW_UP' | 'OTHER';
export type PickupSource =
  | 'HOME' | 'RAILWAY_STATION' | 'AIRPORT' | 'BUS_STATION' | 'HOTEL' | 'CURRENT_LOCATION' | 'OTHER';
export type RequestStatus =
  | 'NEW' | 'UNDER_REVIEW' | 'INFO_REQUIRED' | 'TRIAGE' | 'QUOTE_PREPARED' | 'QUOTE_SENT'
  | 'ACCEPTED' | 'PAYMENT_PENDING' | 'CONFIRMED' | 'RESOURCE_ASSIGNMENT' | 'IN_PROGRESS'
  | 'COMPLETED' | 'CLOSED' | 'CANCELLED' | 'REJECTED' | 'EXPIRED' | 'ON_HOLD' | 'EMERGENCY_ESCALATED';

export interface PickupInfo {
  source?: PickupSource;
  address?: string;
  latitude?: number;
  longitude?: number;
  scheduledAt?: string;
  flightOrTrainNumber?: string;
  arrivalTime?: string;
  terminalOrCoach?: string;
  seatOrBerth?: string;
}

export interface DestinationInfo {
  destinationType?: string;
  hospitalId?: number;
  hospitalName?: string;
  department?: string;
  doctorName?: string;
  address?: string;
  latitude?: number;
  longitude?: number;
  appointmentAt?: string;
}

export interface RequestSummary {
  id: number;
  patientId?: number;
  patientName: string;
  patientMobile?: string;
  services: ServiceType[];
  status: RequestStatus;
  emergency: boolean;
  caseId?: number;
  caseNumber?: string;
  createdAt: string;
}

export interface RequestDetail extends RequestSummary {
  notes?: string;
  triageNotes?: string;
  pickup: PickupInfo;
  destination: DestinationInfo;
  updatedAt: string;
}

export interface RequestStats {
  newRequests: number;
  underReview: number;
  triage: number;
  confirmed: number;
  inProgress: number;
}

// ---- Billing module ----
export type PackageCategory = 'PICKUP_AND_DROP' | 'HOSPITAL_ASSISTANCE' | 'COMPLETE_PATIENT_CARE' | 'CUSTOM';
export type QuoteStatus = 'DRAFT' | 'SENT' | 'ACCEPTED' | 'REJECTED' | 'EXPIRED' | 'PAID';
export type PaymentType = 'ADVANCE' | 'BALANCE' | 'REFUND';
export type PaymentMethod = 'CASH' | 'UPI' | 'CARD' | 'BANK_TRANSFER' | 'WALLET';

export interface RateCard {
  id: number;
  serviceType: ServiceType;
  label: string;
  unit: string;
  unitRate: number;
  baseFare?: number;
  perKmRate?: number;
  emergencyMultiplier?: number;
  nightSurchargePercent?: number;
  active: boolean;
}

export interface PackageItem {
  serviceType?: ServiceType;
  label: string;
  quantity: number;
  unitPrice: number;
}

export interface ServicePackage {
  id: number;
  name: string;
  description?: string;
  category: PackageCategory;
  active: boolean;
  items: PackageItem[];
  indicativeTotal: number;
}

export interface QuoteItem {
  serviceType?: ServiceType;
  label: string;
  quantity: number;
  unitPrice: number;
  amount: number;
}

export interface Quote {
  id: number;
  caseId: number;
  caseNumber?: string;
  patientName?: string;
  status: QuoteStatus;
  currency: string;
  items: QuoteItem[];
  subtotal: number;
  taxPercent: number;
  taxAmount: number;
  discountAmount: number;
  total: number;
  advanceAmount: number;
  amountPaid: number;
  balance: number;
  terms?: string;
  validUntil?: string;
  acceptedAt?: string;
  createdAt: string;
}

export interface Payment {
  id: number;
  caseId: number;
  quoteId?: number;
  amount: number;
  type: PaymentType;
  method: PaymentMethod;
  reference?: string;
  note?: string;
  recordedBy?: string;
  createdAt: string;
}

// ---- Dispatch module ----
export type AgentStatus =
  | 'OFFLINE' | 'ONLINE' | 'AVAILABLE' | 'JOB_OFFERED' | 'ACCEPTED' | 'EN_ROUTE' | 'ARRIVED'
  | 'PATIENT_PICKED' | 'IN_TRANSIT' | 'HOSPITAL_ARRIVED' | 'JOB_COMPLETED' | 'ON_BREAK' | 'SUSPENDED';
export type AssignmentStatus =
  | 'OFFERED' | 'ACCEPTED' | 'EN_ROUTE' | 'ARRIVED_AT_PICKUP' | 'PATIENT_VERIFIED' | 'PATIENT_PICKED'
  | 'IN_TRANSIT' | 'HOSPITAL_ARRIVED' | 'HANDED_OVER' | 'COMPLETED' | 'CANCELLED';

export interface Agent {
  id: number;
  userId?: number;
  fullName: string;
  mobile?: string;
  email?: string;
  employeeCode?: string;
  skills: string[];
  languages: string[];
  status: AgentStatus;
  currentLatitude?: number;
  currentLongitude?: number;
  locationUpdatedAt?: string;
  shift?: string;
  rating: number;
  activeAssignments: number;
  verified: boolean;
  emtLevel?: EmtLevel;
  bloodGroup?: string;
  licenceNumber?: string;
  certificationExpiry?: string;
}

export type EmtLevel = 'NONE' | 'EMT_BASIC' | 'EMT_INTERMEDIATE' | 'EMT_PARAMEDIC';

export interface Assignment {
  id: number;
  caseId: number;
  caseNumber?: string;
  patientName?: string;
  agentId: number;
  agentName?: string;
  status: AssignmentStatus;
  pickupOtp?: string;
  handoverOtp?: string;
  notes?: string;
  acceptedAt?: string;
  pickedAt?: string;
  completedAt?: string;
  createdAt: string;
}

export interface DispatchStats {
  online: number;
  available: number;
  onJob: number;
  offline: number;
}

// ---- Healthcare module ----
export type BedStatus = 'AVAILABLE' | 'RESERVED' | 'OCCUPIED' | 'CLEANING' | 'MAINTENANCE' | 'BLOCKED';
export type AppointmentStatus =
  | 'REQUESTED' | 'SLOT_RESERVED' | 'CONFIRMED' | 'REMINDED' | 'ARRIVED' | 'IN_CONSULTATION'
  | 'PRESCRIBED' | 'COMPLETED' | 'FOLLOW_UP' | 'CANCELLED' | 'NO_SHOW';

export interface Hospital {
  id: number;
  name: string;
  addressLine?: string;
  city?: string;
  state?: string;
  phone?: string;
  email?: string;
  emergencyAvailable: boolean;
  opdTiming?: string;
  partner: boolean;
  active: boolean;
  departments: string[];
  services: string[];
  totalBeds: number;
  availableBeds: number;
}

export interface Bed {
  id: number;
  hospitalId: number;
  ward?: string;
  floor?: string;
  roomNumber?: string;
  bedNumber: string;
  type?: string;
  status: BedStatus;
}

export interface Doctor {
  id: number;
  userId?: number;
  fullName: string;
  specialty?: string;
  department?: string;
  hospitalId?: number;
  hospitalName?: string;
  qualification?: string;
  registrationNumber?: string;
  phone?: string;
  email?: string;
  consultationFee: number;
  available: boolean;
}

export interface Appointment {
  id: number;
  caseId?: number;
  caseNumber?: string;
  patientId?: number;
  patientName?: string;
  doctorId: number;
  doctorName?: string;
  hospitalId?: number;
  hospitalName?: string;
  department?: string;
  scheduledAt: string;
  status: AppointmentStatus;
  reason?: string;
  consultationNotes?: string;
  prescription?: string;
  followUpAt?: string;
  createdAt: string;
}

// ---- Fleet module ----
export type AmbulanceCategory = 'PATIENT_TRANSPORT' | 'BLS' | 'ALS';
export type AmbulanceStatus =
  | 'OFFLINE' | 'AVAILABLE' | 'EN_ROUTE' | 'AT_PICKUP' | 'TRANSPORTING' | 'AT_HOSPITAL'
  | 'MAINTENANCE' | 'OUT_OF_SERVICE';

export interface Ambulance {
  id: number;
  code: string;
  registrationNo: string;
  category: AmbulanceCategory;
  ownerName?: string;
  insuranceExpiry?: string;
  fitnessExpiry?: string;
  permitExpiry?: string;
  driverId?: number;
  driverName?: string;
  status: AmbulanceStatus;
  currentLatitude?: number;
  currentLongitude?: number;
  locationUpdatedAt?: string;
  oxygenLevelPercent: number;
  fuelPercent: number;
  ready: boolean;
  lastCheckAt?: string;
  currentCaseId?: number;
  currentCaseNumber?: string;
  currentPatientName?: string;
}

export interface Emt {
  id: number;
  fullName: string;
  mobile?: string;
  licenceNumber?: string;
  licenceExpiry?: string;
  certification?: string;
  certificationExpiry?: string;
  medicalFitnessValid: boolean;
  backgroundVerified: boolean;
  shift?: string;
  onDuty: boolean;
}

export interface FleetStats {
  total: number;
  available: number;
  onTrip: number;
  maintenance: number;
}

// ---- Care Services module ----
export type CareStatus = 'REQUESTED' | 'CONFIRMED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type CareServiceType = 'ACCOMMODATION' | 'FOOD' | 'LOCAL_TRANSPORT';
export type CareActivityType = 'MEDICINE' | 'MEAL' | 'VITALS' | 'DOCTOR_VISIT' | 'REST' | 'OBSERVATION' | 'HANDOVER';

export interface Caretaker {
  id: number;
  fullName: string;
  mobile?: string;
  skills: string[];
  languages: string[];
  shift?: string;
  hourlyRate: number;
  verified: boolean;
  available: boolean;
}

export interface CaretakerAssignment {
  id: number;
  caseId: number;
  caseNumber?: string;
  patientName?: string;
  caretakerId: number;
  caretakerName?: string;
  startAt?: string;
  endAt?: string;
  shift?: string;
  hourlyRate: number;
  responsibilities?: string;
  mealsProvided: boolean;
  accommodationProvided: boolean;
  status: CareStatus;
  createdAt: string;
}

export interface CareActivity {
  id: number;
  assignmentId: number;
  caseId?: number;
  type: CareActivityType;
  note: string;
  recordedBy?: string;
  createdAt: string;
}

export interface CareBooking {
  id: number;
  caseId: number;
  type: CareServiceType;
  provider?: string;
  description?: string;
  startAt?: string;
  endAt?: string;
  quantity: number;
  unitRate: number;
  total: number;
  roomType?: string;
  dietType?: string;
  meal?: string;
  tripType?: string;
  pickup?: string;
  destination?: string;
  distanceKm?: number;
  status: CareStatus;
  createdAt: string;
}

// ---- Notifications module ----
export type NotifChannel = 'WHATSAPP' | 'SMS' | 'EMAIL' | 'PUSH' | 'INTERNAL';
export type NotifCategory = 'APPOINTMENT' | 'PAYMENT' | 'PICKUP' | 'MEDICAL' | 'MEDICATION' | 'EMERGENCY' | 'MARKETING' | 'GENERAL';
export type DeliveryStatus = 'QUEUED' | 'SENT' | 'DELIVERED' | 'FAILED' | 'READ' | 'SUPPRESSED';

export interface Notification {
  id: number;
  recipientUserId?: number;
  recipientName?: string;
  recipientAddress?: string;
  channel: NotifChannel;
  category: NotifCategory;
  subject?: string;
  body?: string;
  caseId?: number;
  status: DeliveryStatus;
  sentAt?: string;
  deliveredAt?: string;
  readAt?: string;
  errorMessage?: string;
  createdAt: string;
}

export interface NotificationPreference {
  id?: number;
  userId: number;
  category: NotifCategory;
  enabled: boolean;
  channels: NotifChannel[];
}

export interface NotificationStats {
  total: number;
  delivered: number;
  failed: number;
  suppressed: number;
}

// ---- Support module (complaints / incidents / SLA) ----
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type ComplaintStatus = 'OPEN' | 'ASSIGNED' | 'INVESTIGATING' | 'RESOLVED' | 'CLOSED' | 'REOPENED';
export type IncidentType =
  | 'PATIENT_UNAVAILABLE' | 'WRONG_LOCATION' | 'VEHICLE_BREAKDOWN' | 'PATIENT_DETERIORATION'
  | 'ADMISSION_ISSUE' | 'APPOINTMENT_CANCELLATION' | 'PAYMENT_FAILURE' | 'DOCUMENT_GAP'
  | 'SERVICE_COMPLAINT' | 'OTHER';
export type IncidentSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type IncidentStatus = 'OPEN' | 'IN_PROGRESS' | 'ESCALATED' | 'RESOLVED' | 'CLOSED';

export interface ComplaintComment {
  id: number;
  complaintId: number;
  message: string;
  internal: boolean;
  author?: string;
  createdAt: string;
}

export interface Complaint {
  id: number;
  caseId?: number;
  caseNumber?: string;
  patientName?: string;
  subject: string;
  description?: string;
  category?: string;
  priority: Priority;
  status: ComplaintStatus;
  assignedToUserId?: number;
  assignedToName?: string;
  resolution?: string;
  slaResponseDueAt?: string;
  slaResolutionDueAt?: string;
  firstResponseAt?: string;
  resolvedAt?: string;
  responseBreached: boolean;
  resolutionBreached: boolean;
  reopenCount: number;
  createdAt: string;
  comments: ComplaintComment[];
}

export interface ComplaintStats { open: number; investigating: number; resolved: number; breached: number; }

export interface SlaPolicy { id: number; priority: Priority; responseMinutes: number; resolutionMinutes: number; }

export interface Incident {
  id: number;
  caseId?: number;
  caseNumber?: string;
  type: IncidentType;
  severity: IncidentSeverity;
  description: string;
  status: IncidentStatus;
  resolution?: string;
  resolvedAt?: string;
  reportedBy?: string;
  createdAt: string;
}

export interface IncidentStats { open: number; inProgress: number; escalated: number; resolved: number; }

// ---- Clinical module ----
export type IntakeStatus = 'TAKEN' | 'MISSED';

export interface Medicine {
  id: number;
  patientId?: number;
  caseId?: number;
  name: string;
  dose?: string;
  route?: string;
  frequency?: string;
  startDate?: string;
  endDate?: string;
  foodInstruction?: string;
  prescribedBy?: string;
  instructions?: string;
  active: boolean;
  createdAt: string;
}

export interface MedicationIntake {
  id: number;
  medicineId: number;
  caseId?: number;
  medicineName?: string;
  status: IntakeStatus;
  note?: string;
  recordedBy?: string;
  createdAt: string;
}

export interface PatientConditionReading {
  id: number;
  caseId?: number;
  condition?: string;
  temperature?: string;
  bloodPressure?: string;
  pulse?: string;
  spo2?: string;
  painScore?: number;
  notes?: string;
  recordedBy?: string;
  createdAt: string;
}

// ---- Settlement module ----
export type PayeeType = 'AGENT' | 'CARETAKER' | 'AMBULANCE_PARTNER' | 'HOSPITAL_PARTNER';
export type SettlementStatus = 'PENDING' | 'APPROVED' | 'PAID' | 'CANCELLED';

export interface Earning {
  id: number;
  payeeType: PayeeType;
  payeeId: number;
  payeeName?: string;
  caseId?: number;
  caseNumber?: string;
  description?: string;
  amount: number;
  settled: boolean;
  settlementId?: number;
  createdAt: string;
}

export interface Settlement {
  id: number;
  payeeType: PayeeType;
  payeeId: number;
  payeeName?: string;
  periodLabel?: string;
  grossAmount: number;
  deductions: number;
  netAmount: number;
  lineCount: number;
  status: SettlementStatus;
  paymentReference?: string;
  approvedAt?: string;
  paidAt?: string;
  createdAt: string;
}

export interface SettlementStats { pending: number; approved: number; paid: number; unsettledTotal: number; }

// ---- Service Plan (planned vs actual) ----
export interface ServicePlanItem {
  category: string;
  title: string;
  detail?: string;
  status: string;
  plannedAmount: number;
  actualAmount: number;
  delivered: boolean;
}

export interface ServicePlan {
  caseId: number;
  caseNumber?: string;
  patientName?: string;
  items: ServicePlanItem[];
  totalItems: number;
  deliveredItems: number;
  committedValue: number;
  plannedValue: number;
  deliveredValue: number;
  varianceValue: number;
}

// ---- Equipment inventory (point 27) ----
export type EquipmentCategory = 'OXYGEN' | 'MOBILITY' | 'MONITORING' | 'RESPIRATORY' | 'OTHER';

export interface EquipmentItem {
  id: number;
  name: string;
  category: EquipmentCategory;
  unit?: string;
  totalQuantity: number;
  availableQuantity: number;
  allocatedQuantity: number;
  active: boolean;
}

export interface EquipmentAllocation {
  id: number;
  equipmentItemId: number;
  equipmentName?: string;
  caseId?: number;
  caseNumber?: string;
  quantity: number;
  returned: boolean;
  returnedAt?: string;
  createdAt: string;
}

// ---- Accommodation master (point 42) ----
export interface Accommodation {
  id: number;
  name: string;
  type?: string;
  addressLine?: string;
  city?: string;
  latitude?: number;
  longitude?: number;
  roomType?: string;
  pricePerNight: number;
  distanceToHospitalKm?: number;
  available: boolean;
  roomsAvailable?: number;
  contactPhone?: string;
  active: boolean;
}

export interface NearbyAccommodation {
  option: Accommodation;
  distanceKm: number;
}

// ---- Cash handling (point 46) ----
export interface CashCollection {
  id: number;
  agentId: number;
  agentName?: string;
  caseId?: number;
  caseNumber?: string;
  amount: number;
  note?: string;
  deposited: boolean;
  depositedAt?: string;
  depositReference?: string;
  createdAt: string;
}

export interface CashInHandSummary {
  agentId: number;
  collected: number;
  deposited: number;
  inHand: number;
  totalCollections: number;
  pendingDeposits: number;
}

// ---- Unified Trip (points 44 & 76) ----
export interface TripLeg {
  label?: string;
  address?: string;
  latitude?: number;
  longitude?: number;
}

export interface Trip {
  caseId: number;
  caseNumber?: string;
  patientName?: string;
  phase: string;
  agentId?: number;
  agentName?: string;
  assignmentStatus?: string;
  pickupOtp?: string;
  handoverOtp?: string;
  ambulanceId?: number;
  ambulanceRegistration?: string;
  ambulanceCategory?: string;
  ambulanceStatus?: string;
  pickup?: TripLeg;
  destination?: TripLeg;
  distanceKm?: number;
  appointmentAt?: string;
  acceptedAt?: string;
  pickedAt?: string;
  completedAt?: string;
}

// ---- Fare estimate (point 17) ----
export interface FareEstimate {
  serviceType: ServiceType;
  label?: string;
  distanceKm: number;
  emergency: boolean;
  night: boolean;
  baseFare: number;
  distanceCharge: number;
  subtotal: number;
  emergencySurcharge: number;
  nightSurcharge: number;
  total: number;
  currency: string;
}

// ---- Document access history (point 14) ----
export interface DocumentAccess {
  id: number;
  documentId: number;
  patientId?: number;
  action: string;
  accessedByUserId?: number;
  accessedByName?: string;
  accessedByRole?: string;
  accessedAt: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
