package com.pcare.shared

// ---- Auth ----
data class LoginRequest(val username: String, val password: String)
data class RegisterPatientRequest(
    val username: String,
    val password: String,
    val fullName: String,
    val fatherName: String? = null,
    val mobile: String? = null,
    val email: String? = null,
    val aadhaar: String? = null,
    val pan: String? = null,
)
data class User(
    val id: Long,
    val username: String,
    val fullName: String?,
    val roles: List<String> = emptyList(),
)
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String?,
    val expiresInMinutes: Long,
    val user: User,
)

// ---- Packages / requests (Patient app) ----
data class PackageItem(val label: String, val quantity: Double, val unitPrice: Double)
data class ServicePackage(
    val id: Long,
    val name: String,
    val description: String?,
    val indicativeTotal: Double,
    val items: List<PackageItem> = emptyList(),
)
data class CreateRequest(
    val patientName: String,
    val patientMobile: String?,
    val services: List<String>,
    val notes: String?,
    val emergency: Boolean,
)
data class ServiceRequestDto(
    val id: Long,
    val patientName: String,
    val services: List<String> = emptyList(),
    val status: String,
    val emergency: Boolean,
    val caseNumber: String?,
)

// ---- Agent / dispatch (Agent app) ----
data class AgentDto(
    val id: Long,
    val userId: Long?,
    val fullName: String,
    val status: String,
    val activeAssignments: Int,
    val skills: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
)
data class AssignmentDto(
    val id: Long,
    val caseId: Long,
    val caseNumber: String?,
    val patientName: String?,
    val agentId: Long,
    val agentName: String?,
    val status: String,
    val pickupOtp: String?,
    val handoverOtp: String?,
)
data class StatusRequest(val status: String)
data class LocationRequest(val latitude: Double, val longitude: Double)
data class AssignmentStatusRequest(val status: String, val note: String? = null)
data class VerifyOtpRequest(val otp: String)

// ---- Patient App: my cases / records / payments ----
data class CaseSummaryLite(
    val id: Long,
    val caseNumber: String?,
    val patientName: String?,
    val title: String?,
    val status: String,
    val priority: String,
    val emergency: Boolean = false,
    val createdAt: String?,
)
data class TimelineEvent(
    val type: String,
    val description: String,
    val source: String?,
    val createdBy: String?,
    val createdAt: String?,
)
data class QuoteLite(
    val id: Long,
    val status: String,
    val total: Double,
    val amountPaid: Double,
    val balance: Double,
    val currency: String = "INR",
)
data class AppointmentLite(
    val id: Long,
    val doctorName: String?,
    val department: String?,
    val hospitalName: String?,
    val scheduledAt: String?,
    val status: String,
    val prescription: String?,
)
data class MedicineLite(
    val name: String,
    val dose: String?,
    val frequency: String?,
    val foodInstruction: String?,
    val prescribedBy: String?,
)
data class MyCaseDetail(
    val caseFile: CaseSummaryLite,
    val services: List<String> = emptyList(),
    val timeline: List<TimelineEvent> = emptyList(),
    val quotes: List<QuoteLite> = emptyList(),
    val appointments: List<AppointmentLite> = emptyList(),
    val medicines: List<MedicineLite> = emptyList(),
    val balance: Double = 0.0,
)
data class MyRecordsDto(
    val appointments: List<AppointmentLite> = emptyList(),
    val medicines: List<MedicineLite> = emptyList(),
)
data class MyRequestLite(
    val id: Long,
    val status: String,
    val services: List<String> = emptyList(),
    val emergency: Boolean = false,
    val caseNumber: String?,
)
data class PayRequest(val quoteId: Long, val amount: Double)

// ---- Nearby hospitals & ambulances (GPS discovery) ----
data class NearbyHospitalDto(
    val id: Long, val name: String, val address: String?, val phone: String?,
    val emergency: Boolean, val lat: Double?, val lng: Double?, val distanceKm: Double,
)
data class NearbyAmbulanceDto(
    val id: Long, val registrationNo: String, val status: String, val oxygenLevelPercent: Int?,
    val lat: Double?, val lng: Double?, val distanceKm: Double,
)
data class NearbyResultDto(
    val hospitals: List<NearbyHospitalDto> = emptyList(),
    val ambulances: List<NearbyAmbulanceDto> = emptyList(),
)

// ---- Live ambulance tracking (Patient app map) ----
data class GeoPointDto(val lat: Double?, val lng: Double?, val label: String?)
data class DriverPointDto(
    val agentId: Long?,
    val name: String?,
    val lat: Double?,
    val lng: Double?,
    val status: String?,
)
data class CaseRouteDto(
    val caseId: Long,
    val caseNumber: String?,
    val patientName: String?,
    val pickup: GeoPointDto?,
    val destination: GeoPointDto?,
    val driver: DriverPointDto?,
    val assignmentStatus: String?,
    // Road-snapped geometry driver -> pickup -> hospital as [lat,lng] pairs (computed server-side).
    val polyline: List<List<Double>>? = null,
)
