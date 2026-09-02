package com.pcare.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pcare.shared.*
import kotlinx.coroutines.launch

private val Navy = Color(0xFF0B1220)
private val Navy2 = Color(0xFF16233B)
private val Teal = Color(0xFF14B8A6)
private val TealDeep = Color(0xFF0F766E)
private val Bg = Color(0xFFF1F4F9)
private val CardBorder = Color(0xFFEAEEF5)
private val Ink = Color(0xFF14203A)
private val Muted = Color(0xFF667085)
private val Green = Color(0xFF16A34A)
private val Amber = Color(0xFFD97706)
// Richer diagonal navy gradient for a more premium header.
private val NavyBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF24406B), Navy2, Navy),
    start = androidx.compose.ui.geometry.Offset(0f, 0f),
    end = androidx.compose.ui.geometry.Offset(1000f, 640f),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = Session(this)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(primary = TealDeep, onPrimary = Color.White, background = Bg, surface = Color.White, onSurface = Ink)
            ) {
                Surface(Modifier.fillMaxSize(), color = Bg) {
                    var loggedIn by remember { mutableStateOf(session.isLoggedIn) }
                    if (loggedIn) JobsScreen(session) { session.clear(); loggedIn = false }
                    else LoginScreen(session) { loggedIn = true }
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(session: Session, onLoggedIn: () -> Unit) {
    val scope = rememberCoroutineScope()
    var server by remember { mutableStateOf(session.baseUrl) }
    var username by remember { mutableStateOf("agent") }
    var password by remember { mutableStateOf("agent123") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var showServer by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(
            Modifier.fillMaxWidth().height(248.dp)
                .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                .background(NavyBrush),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = RoundedCornerShape(50), color = Teal.copy(alpha = 0.18f)) {
                    Icon(Icons.Filled.LocalShipping, null, tint = Teal, modifier = Modifier.padding(16.dp).size(34.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text("360° Care Agent", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Field operations", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            }
        }
        Column(Modifier.padding(24.dp)) {
            Text("Agent sign in", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(18.dp))
            ElevatedCard(shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
                Column(Modifier.padding(18.dp)) {
                    Field(username, { username = it }, "Username", Icons.Filled.AccountCircle)
                    Spacer(Modifier.height(12.dp))
                    Field(password, { password = it }, "Password", Icons.Filled.Lock, password = true)
                    error?.let { Spacer(Modifier.height(10.dp)); Text(it, color = Color(0xFFDC2626), fontSize = 12.5.sp) }
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            error = null; loading = true
                            session.baseUrl = server; ApiClient.reset()
                            scope.launch {
                                try {
                                    val resp = ApiClient.service(session).login(LoginRequest(username, password))
                                    session.token = resp.accessToken; session.userId = resp.user.id
                                    session.userName = resp.user.fullName ?: resp.user.username
                                    onLoggedIn()
                                } catch (e: Exception) { error = e.message ?: "Login failed" } finally { loading = false }
                            }
                        },
                        enabled = !loading, shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Sign in", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = { showServer = !showServer }) {
                Icon(Icons.Filled.Settings, null, Modifier.size(16.dp), tint = Muted)
                Spacer(Modifier.width(6.dp)); Text("Server settings", color = Muted, fontSize = 12.sp)
            }
            if (showServer) Field(server, { server = it }, "Server URL", Icons.Filled.Cloud)
            Spacer(Modifier.height(18.dp))
            Text("Developed by pvalr", color = Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun JobsScreen(session: Session, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    val api = remember { ApiClient.service(session) }
    var agent by remember { mutableStateOf<AgentDto?>(null) }
    var jobs by remember { mutableStateOf<List<AssignmentDto>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var navCase by remember { mutableStateOf<Long?>(null) }

    fun refresh() {
        scope.launch {
            try { val a = api.myAgent(); agent = a; jobs = api.agentAssignments(a.id); error = null }
            catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { refresh() }
    // Dynamic sync: poll every 4s so jobs dispatched from the Command Centre appear automatically.
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(4000)
            try { val a = api.myAgent(); agent = a; jobs = api.agentAssignments(a.id) } catch (_: Exception) {}
        }
    }

    val navTo = navCase
    val agentId = agent?.id
    if (navTo != null && agentId != null) {
        AgentRouteScreen(session, agentId, navTo) { navCase = null }
        return
    }

    val online = agent?.status == "AVAILABLE" || agent?.status == "ONLINE"

    // Live GPS: while Available, post location every few seconds so the Command Centre map moves.
    LaunchedEffect(agent?.id, online) {
        val a = agent ?: return@LaunchedEffect
        if (!online) return@LaunchedEffect
        var lat = 12.9716; var lng = 77.5946
        while (true) {
            kotlinx.coroutines.delay(4000)
            lat += (kotlin.random.Random.nextDouble() - 0.5) * 0.004
            lng += (kotlin.random.Random.nextDouble() - 0.5) * 0.004
            try { api.postLocation(a.id, LocationRequest(lat, lng)) } catch (_: Exception) {}
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)).background(NavyBrush)) {
            Column(Modifier.padding(20.dp, 24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(50), color = Teal.copy(alpha = 0.20f)) {
                        Text((agent?.fullName ?: "A").take(1).uppercase(), color = Teal, fontWeight = FontWeight.Bold,
                            fontSize = 18.sp, modifier = Modifier.padding(14.dp, 10.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(agent?.fullName ?: session.userName ?: "Agent", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(50), color = if (online) Green else Muted, modifier = Modifier.size(8.dp)) {}
                            Spacer(Modifier.width(6.dp))
                            Text(agent?.status?.replace('_', ' ') ?: "—", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        }
                    }
                    IconButton(onClick = onLogout) { Icon(Icons.Filled.Logout, "Logout", tint = Color.White) }
                }
                Spacer(Modifier.height(16.dp))
                agent?.let { a ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DutyButton("Available", Icons.Filled.CheckCircle, online, Modifier.weight(1f)) {
                            scope.launch { try { api.setAgentStatus(a.id, StatusRequest("AVAILABLE")); refresh() } catch (_: Exception) {} }
                        }
                        DutyButton("Offline", Icons.Filled.DoNotDisturbOn, !online, Modifier.weight(1f)) {
                            scope.launch { try { api.setAgentStatus(a.id, StatusRequest("OFFLINE")); refresh() } catch (_: Exception) {} }
                        }
                        DutyButton("GPS", Icons.Filled.MyLocation, false, Modifier.weight(1f)) {
                            scope.launch { try { api.postLocation(a.id, LocationRequest(12.9716, 77.5946)); refresh() } catch (_: Exception) {} }
                        }
                    }
                }
            }
        }

        Column(Modifier.padding(16.dp)) {
            val completed = jobs.count { it.status == "COMPLETED" }
            val active = jobs.count { it.status != "COMPLETED" && it.status != "CANCELLED" }
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Assigned", "${jobs.size}", TealDeep, Modifier.weight(1f))
                StatCard("Active", "$active", Amber, Modifier.weight(1f))
                StatCard("Completed", "$completed", Green, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("My jobs", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(50), color = TealDeep.copy(alpha = 0.12f)) {
                    Text("${jobs.size}", color = TealDeep, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(10.dp, 4.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            error?.let { Text(it, color = Color(0xFFDC2626), fontSize = 12.sp); Spacer(Modifier.height(8.dp)) }
            if (loading) Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = TealDeep) }
            if (!loading && jobs.isEmpty()) EmptyJobs()
            jobs.forEach { job -> JobCard(job, api, scope, onNavigate = { navCase = job.caseId }) { refresh() } }
            Spacer(Modifier.height(18.dp))
            Text("Developed by pvalr", color = Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier) {
    Card(shape = RoundedCornerShape(16.dp), modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(15.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontSize = 23.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
            Spacer(Modifier.height(2.dp))
            Text(label.uppercase(), color = Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
private fun EmptyJobs() {
    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Inbox, null, tint = Muted, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(10.dp))
            Text("No jobs assigned yet", color = Muted, fontSize = 13.sp)
            Text("Go Available to receive dispatches", color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun DutyButton(label: String, icon: ImageVector, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp),
        color = if (active) Teal else Color.White.copy(alpha = 0.12f), modifier = modifier) {
        Column(Modifier.padding(vertical = 10.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = if (active) Navy else Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, color = if (active) Navy else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ---------------- ROUTE MAP (native OpenStreetMap, live) ----------------
private fun mapDot(color: Int, sizePx: Int): android.graphics.drawable.Drawable =
    android.graphics.drawable.GradientDrawable().apply {
        shape = android.graphics.drawable.GradientDrawable.OVAL
        setColor(color); setStroke(6, android.graphics.Color.WHITE); setSize(sizePx, sizePx)
    }

private fun haversineKm(a: org.osmdroid.util.GeoPoint, b: org.osmdroid.util.GeoPoint): Double {
    val r = 6371.0
    val dLat = Math.toRadians(b.latitude - a.latitude); val dLng = Math.toRadians(b.longitude - a.longitude)
    val s = Math.sin(dLat / 2).let { it * it } +
        Math.cos(Math.toRadians(a.latitude)) * Math.cos(Math.toRadians(b.latitude)) * Math.sin(dLng / 2).let { it * it }
    return r * 2 * Math.atan2(Math.sqrt(s), Math.sqrt(1 - s))
}

@Composable
private fun AgentRouteScreen(session: Session, agentId: Long, caseId: Long, onBack: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val api = remember { ApiClient.service(session) }
    var status by remember { mutableStateOf("Loading route…") }
    var eta by remember { mutableStateOf("") }

    val mapView = remember {
        org.osmdroid.config.Configuration.getInstance().userAgentValue = ctx.packageName
        org.osmdroid.views.MapView(ctx).apply {
            setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
            setMultiTouchControls(true); controller.setZoom(12.0)
            controller.setCenter(org.osmdroid.util.GeoPoint(12.9716, 77.5946))
        }
    }
    val me = remember {
        org.osmdroid.views.overlay.Marker(mapView).apply { icon = mapDot(0xFF2563EB.toInt(), 46); setAnchor(0.5f, 0.5f); title = "You" }
    }
    val routeLine = remember {
        org.osmdroid.views.overlay.Polyline(mapView).apply { outlinePaint.color = 0xFF2563EB.toInt(); outlinePaint.strokeWidth = 12f }
    }
    var staticAdded by remember { mutableStateOf(false) }
    var fitted by remember { mutableStateOf(false) }

    LaunchedEffect(caseId) {
        while (true) {
            try {
                val r = api.agentCaseRoute(agentId, caseId)
                val pk = r.pickup?.takeIf { it.lat != null && it.lng != null }?.let { org.osmdroid.util.GeoPoint(it.lat!!, it.lng!!) }
                val hp = r.destination?.takeIf { it.lat != null && it.lng != null }?.let { org.osmdroid.util.GeoPoint(it.lat!!, it.lng!!) }
                if (!staticAdded) {
                    pk?.let { g -> mapView.overlays.add(org.osmdroid.views.overlay.Marker(mapView).apply { position = g; icon = mapDot(0xFF16A34A.toInt(), 42); setAnchor(0.5f, 0.5f); title = "Patient pickup" }) }
                    hp?.let { g -> mapView.overlays.add(org.osmdroid.views.overlay.Marker(mapView).apply { position = g; icon = mapDot(0xFFDC2626.toInt(), 42); setAnchor(0.5f, 0.5f); title = "Hospital" }) }
                    mapView.overlays.add(routeLine); mapView.overlays.add(me); staticAdded = true
                }
                val d = r.driver
                if (d?.lat != null && d.lng != null) {
                    val dg = org.osmdroid.util.GeoPoint(d.lat!!, d.lng!!)
                    me.position = dg
                    val st = (r.assignmentStatus ?: d.status ?: "EN_ROUTE")
                    status = "You · ${st.replace('_', ' ')}"
                    val beforePickup = st in listOf("OFFERED", "ACCEPTED", "EN_ROUTE", "ARRIVED_AT_PICKUP", "PATIENT_VERIFIED")
                    val target = if (beforePickup) pk else hp
                    val label = if (beforePickup) "patient pickup" else (r.destination?.label ?: "hospital")
                    if (target != null) eta = "Approx ${"%.1f".format(haversineKm(dg, target))} km to $label"
                    val road = r.polyline?.mapNotNull { p -> if (p.size >= 2) org.osmdroid.util.GeoPoint(p[0], p[1]) else null }
                        ?: buildList { add(dg); pk?.let { add(it) }; hp?.let { add(it) } }
                    if (road.size >= 2) {
                        routeLine.setPoints(road)
                        if (!fitted) { mapView.zoomToBoundingBox(org.osmdroid.util.BoundingBox.fromGeoPoints(road).increaseByScale(1.4f), true); fitted = true }
                    }
                }
                mapView.invalidate()
            } catch (_: Exception) {}
            kotlinx.coroutines.delay(2000)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)).background(NavyBrush)) {
            Row(Modifier.fillMaxWidth().padding(12.dp, 20.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White) }
                Column { Text("Route to patient", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("Live navigation", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp) }
            }
        }
        AndroidView(modifier = Modifier.weight(1f).fillMaxWidth(), factory = { mapView }, onRelease = { it.onDetach() })
        Row(Modifier.fillMaxWidth().background(Color.White).padding(16.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(50), color = TealDeep.copy(alpha = 0.12f)) { Icon(Icons.Filled.DirectionsCar, null, tint = TealDeep, modifier = Modifier.padding(10.dp).size(20.dp)) }
            Spacer(Modifier.width(12.dp))
            Column { Text(status, fontWeight = FontWeight.Bold, color = TealDeep, fontSize = 15.sp); if (eta.isNotEmpty()) Text(eta, color = Muted, fontSize = 12.5.sp) }
        }
    }
}

@Composable
private fun JobCard(job: AssignmentDto, api: ApiService, scope: kotlinx.coroutines.CoroutineScope, onNavigate: () -> Unit, onChanged: () -> Unit) {
    var otp by remember { mutableStateOf("") }
    fun run(block: suspend () -> Unit) = scope.launch { try { block(); onChanged() } catch (_: Exception) {} }

    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = TealDeep.copy(alpha = 0.10f)) {
                    Icon(Icons.Filled.Assignment, null, tint = TealDeep, modifier = Modifier.padding(10.dp).size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(job.caseNumber ?: "Case ${job.caseId}", fontWeight = FontWeight.Bold, color = Ink, fontSize = 15.sp)
                    Text(job.patientName ?: "Patient", color = Muted, fontSize = 12.sp)
                }
                StatusPill(job.status)
            }
            Spacer(Modifier.height(14.dp))

            if (job.status != "COMPLETED" && job.status != "CANCELLED") {
                OutlinedButton(onClick = onNavigate, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Map, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Navigate — view route")
                }
                Spacer(Modifier.height(10.dp))
            }

            when (job.status) {
                "OFFERED" -> PrimaryCta("Accept job", Icons.Filled.Check) { run { api.acceptAssignment(job.id) } }
                "ACCEPTED" -> PrimaryCta("Start — En route", Icons.Filled.DirectionsCar) { run { api.updateAssignmentStatus(job.id, AssignmentStatusRequest("EN_ROUTE")) } }
                "EN_ROUTE" -> PrimaryCta("Arrived at pickup", Icons.Filled.LocationOn) { run { api.updateAssignmentStatus(job.id, AssignmentStatusRequest("ARRIVED_AT_PICKUP")) } }
                "ARRIVED_AT_PICKUP" -> OtpBlock("Pickup OTP", otp, { otp = it }) { run { api.verifyPickup(job.id, VerifyOtpRequest(otp)) } }
                "PATIENT_PICKED" -> PrimaryCta("Reached hospital", Icons.Filled.LocalHospital) { run { api.updateAssignmentStatus(job.id, AssignmentStatusRequest("HOSPITAL_ARRIVED")) } }
                "HOSPITAL_ARRIVED" -> OtpBlock("Handover OTP", otp, { otp = it }) { run { api.verifyHandover(job.id, VerifyOtpRequest(otp)) } }
                "COMPLETED" -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Green, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp)); Text("Completed", color = Green, fontWeight = FontWeight.Bold)
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val c = when (status) {
        "COMPLETED" -> Green; "OFFERED" -> Amber; "CANCELLED" -> Color(0xFFDC2626); else -> TealDeep
    }
    Surface(shape = RoundedCornerShape(50), color = c.copy(alpha = 0.12f)) {
        Text(status.replace('_', ' '), color = c, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(10.dp, 5.dp))
    }
}

@Composable
private fun PrimaryCta(label: String, icon: ImageVector, onClick: () -> Unit) {
    Button(onClick = onClick, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun OtpBlock(label: String, otp: String, onChange: (String) -> Unit, onVerify: () -> Unit) {
    Column {
        OutlinedTextField(otp, onChange, label = { Text(label) }, singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Pin, null, tint = Muted) },
            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Button(onClick = onVerify, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Icon(Icons.Filled.VerifiedUser, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text("Verify $label", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String, icon: ImageVector, password: Boolean = false) {
    OutlinedTextField(
        value, onChange, label = { Text(label) }, singleLine = true,
        leadingIcon = { Icon(icon, null, tint = Muted) },
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
    )
}
