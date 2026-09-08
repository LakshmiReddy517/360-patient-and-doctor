package com.pcare.shared

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/** REST surface of the 360 Patient Care backend used by the mobile apps. */
interface ApiService {

    // Auth
    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("api/v1/auth/register")
    suspend fun register(@Body body: RegisterPatientRequest): AuthResponse

    @Multipart
    @POST("api/v1/auth/register")
    suspend fun registerKyc(
        @Part("username") username: RequestBody,
        @Part("password") password: RequestBody,
        @Part("fullName") fullName: RequestBody,
        @Part("fatherName") fatherName: RequestBody,
        @Part("mobile") mobile: RequestBody,
        @Part("email") email: RequestBody,
        @Part("aadhaar") aadhaar: RequestBody,
        @Part("pan") pan: RequestBody,
        @Part aadhaarImage: MultipartBody.Part?,
        @Part panImage: MultipartBody.Part?,
    ): AuthResponse

    @GET("api/v1/auth/me")
    suspend fun me(): User

    // Patient
    @GET("api/v1/packages")
    suspend fun packages(): List<ServicePackage>

    @POST("api/v1/requests")
    suspend fun createRequest(@Body body: CreateRequest): ServiceRequestDto

    // Patient App: my own cases / records / payments
    @GET("api/v1/me/cases")
    suspend fun myCases(): List<CaseSummaryLite>

    @GET("api/v1/me/requests")
    suspend fun myRequests(): List<MyRequestLite>

    @GET("api/v1/me/records")
    suspend fun myRecords(): MyRecordsDto

    @GET("api/v1/me/documents")
    suspend fun myDocuments(): List<MyDocumentDto>

    // Notification preferences (blueprint point 48)
    @GET("api/v1/notifications/preferences/{userId}")
    suspend fun notificationPreferences(@Path("userId") userId: Long): List<NotificationPreferenceDto>

    @PUT("api/v1/notifications/preferences")
    suspend fun updatePreference(@Body body: UpsertPreferenceRequest): NotificationPreferenceDto

    @GET("api/v1/me/cases/{id}")
    suspend fun myCase(@Path("id") id: Long): MyCaseDetail

    @GET("api/v1/me/cases/{id}/route")
    suspend fun caseRoute(@Path("id") id: Long): CaseRouteDto

    @GET("api/v1/nearby")
    suspend fun nearby(@Query("lat") lat: Double, @Query("lng") lng: Double, @Query("limit") limit: Int = 8): NearbyResultDto

    @POST("api/v1/me/quotes/{id}/accept")
    suspend fun acceptMyQuote(@Path("id") id: Long): QuoteLite

    @POST("api/v1/me/pay")
    suspend fun payMyQuote(@Body body: PayRequest): QuoteLite

    // Agent
    @GET("api/v1/agents/me")
    suspend fun myAgent(): AgentDto

    @GET("api/v1/agents/{id}/assignments")
    suspend fun agentAssignments(@Path("id") id: Long): List<AssignmentDto>

    @GET("api/v1/agents/{id}/earnings")
    suspend fun agentEarnings(@Path("id") id: Long): AgentEarningsDto

    @GET("api/v1/agents/{agentId}/cases/{caseId}/route")
    suspend fun agentCaseRoute(@Path("agentId") agentId: Long, @Path("caseId") caseId: Long): CaseRouteDto

    @PUT("api/v1/agents/{id}/status")
    suspend fun setAgentStatus(@Path("id") id: Long, @Body body: StatusRequest): AgentDto

    @PUT("api/v1/agents/{id}/location")
    suspend fun postLocation(@Path("id") id: Long, @Body body: LocationRequest): AgentDto

    @POST("api/v1/assignments/{id}/accept")
    suspend fun acceptAssignment(@Path("id") id: Long): AssignmentDto

    @PUT("api/v1/assignments/{id}/status")
    suspend fun updateAssignmentStatus(@Path("id") id: Long, @Body body: AssignmentStatusRequest): AssignmentDto

    @POST("api/v1/assignments/{id}/verify-pickup")
    suspend fun verifyPickup(@Path("id") id: Long, @Body body: VerifyOtpRequest): AssignmentDto

    @POST("api/v1/assignments/{id}/verify-handover")
    suspend fun verifyHandover(@Path("id") id: Long, @Body body: VerifyOtpRequest): AssignmentDto
}

/** Builds an [ApiService] bound to the session's base URL and bearer token. */
object ApiClient {

    @Volatile
    private var cached: Pair<String, ApiService>? = null

    fun service(session: Session): ApiService {
        val base = session.baseUrl
        cached?.let { if (it.first == base) return it.second }

        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val builder = chain.request().newBuilder()
                session.token?.let { builder.addHeader("Authorization", "Bearer $it") }
                chain.proceed(builder.build())
            }
            .addInterceptor(logging)
            .build()

        val service = Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        cached = base to service
        return service
    }

    fun reset() {
        cached = null
    }
}

/** Registers a patient with Aadhaar/PAN document images, building the multipart request. */
suspend fun ApiService.registerWithDocs(
    username: String, password: String, fullName: String, fatherName: String?, mobile: String?, email: String?,
    aadhaar: String, pan: String, aadhaarImage: ByteArray?, panImage: ByteArray?,
): AuthResponse {
    fun text(s: String?): RequestBody = (s ?: "").toRequestBody("text/plain".toMediaTypeOrNull())
    fun img(bytes: ByteArray?, name: String): MultipartBody.Part? = bytes?.let {
        MultipartBody.Part.createFormData(name, "$name.jpg", it.toRequestBody("image/*".toMediaTypeOrNull()))
    }
    return registerKyc(
        text(username), text(password), text(fullName), text(fatherName), text(mobile), text(email),
        text(aadhaar), text(pan), img(aadhaarImage, "aadhaarImage"), img(panImage, "panImage"))
}
