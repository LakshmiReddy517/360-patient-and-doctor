package com.pcare.patient

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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

private val Teal = Color(0xFF0F766E)
private val TealDark = Color(0xFF0B5D56)
private val TealLite = Color(0xFF15938A)
private val Bg = Color(0xFFF1F4F9)
private val CardBorder = Color(0xFFEAEEF5)
private val Ink = Color(0xFF14203A)
private val Muted = Color(0xFF667085)
private val Danger = Color(0xFFDC2626)
private val Amber = Color(0xFFD97706)
private val GreenC = Color(0xFF16A34A)
// Richer diagonal brand gradient for a more premium header.
private val HeaderBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF16A79A), Teal, Color(0xFF0A4F49)),
    start = androidx.compose.ui.geometry.Offset(0f, 0f),
    end = androidx.compose.ui.geometry.Offset(1000f, 620f),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = Session(this)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(primary = Teal, onPrimary = Color.White, background = Bg, surface = Color.White, onSurface = Ink)
            ) {
                Surface(Modifier.fillMaxSize(), color = Bg) {
                    var loggedIn by remember { mutableStateOf(session.isLoggedIn) }
                    if (loggedIn) MainShell(session) { session.clear(); loggedIn = false }
                    else LoginScreen(session) { loggedIn = true }
                }
            }
        }
    }
}

// ---------------- LOGIN ----------------
@Composable
private fun LoginScreen(session: Session, onLoggedIn: () -> Unit) {
    val scope = rememberCoroutineScope()
    var server by remember { mutableStateOf(session.baseUrl) }
    var username by remember { mutableStateOf("patient") }
    var password by remember { mutableStateOf("patient123") }
    var registering by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var aadhaar by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var aadhaarUri by remember { mutableStateOf<Uri?>(null) }
    var panUri by remember { mutableStateOf<Uri?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var showServer by remember { mutableStateOf(false) }
    var showLang by remember { mutableStateOf(false) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    if (showLang) LanguageDialog(session) { showLang = false }
    val aadhaarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { aadhaarUri = it }
    val panPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { panUri = it }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(248.dp).clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)).background(HeaderBrush), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.16f)) {
                    Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.padding(16.dp).size(34.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text("360° Patient Care", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Care, transport & coordination", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
            }
        }
        Column(Modifier.padding(24.dp)) {
            Text(if (registering) "Create your account" else "Welcome back", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(if (registering) "Register to book assistance" else "Sign in to continue", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(18.dp))
            ElevatedCard(shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
                Column(Modifier.padding(18.dp)) {
                    if (registering) {
                        Field(fullName, { fullName = it }, "Full name", Icons.Filled.Person); Spacer(Modifier.height(12.dp))
                        Field(fatherName, { fatherName = it }, "Father's name", Icons.Filled.Person); Spacer(Modifier.height(12.dp))
                        Field(username, { username = it }, "Username", Icons.Filled.AccountCircle); Spacer(Modifier.height(12.dp))
                        Field(mobile, { mobile = it }, "Mobile number", Icons.Filled.Phone, keyboard = androidx.compose.ui.text.input.KeyboardType.Phone); Spacer(Modifier.height(12.dp))
                        Field(email, { email = it }, "Email (optional)", Icons.Filled.Email, keyboard = androidx.compose.ui.text.input.KeyboardType.Email); Spacer(Modifier.height(12.dp))
                        Text("Government ID verification", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(8.dp))
                        Field(aadhaar, { if (it.length <= 12 && it.all { c -> c.isDigit() }) aadhaar = it }, "Aadhaar number (12 digits)", Icons.Filled.Badge, keyboard = androidx.compose.ui.text.input.KeyboardType.Number); Spacer(Modifier.height(12.dp))
                        Field(pan, { if (it.length <= 10) pan = it.uppercase() }, "PAN (ABCDE1234F)", Icons.Filled.CreditCard); Spacer(Modifier.height(12.dp))
                        UploadRow("Aadhaar photo", aadhaarUri != null) { aadhaarPicker.launch("image/*") }
                        Spacer(Modifier.height(8.dp))
                        UploadRow("PAN photo", panUri != null) { panPicker.launch("image/*") }
                        Spacer(Modifier.height(12.dp))
                        Field(password, { password = it }, "Password", Icons.Filled.Lock, password = true)
                    } else {
                        Field(username, { username = it }, "Username (email/mobile)", Icons.Filled.AccountCircle); Spacer(Modifier.height(12.dp))
                        Field(password, { password = it }, "Password", Icons.Filled.Lock, password = true)
                    }
                    error?.let { Spacer(Modifier.height(10.dp)); Text(it, color = Danger, fontSize = 12.5.sp) }
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = {
                        error = null
                        if (registering) {
                            when {
                                fullName.isBlank() || username.isBlank() || password.length < 6 ->
                                    { error = "Enter full name, username and a password (6+ chars)"; return@Button }
                                !aadhaar.matches(Regex("\\d{12}")) ->
                                    { error = "Enter a valid 12-digit Aadhaar number"; return@Button }
                                !pan.matches(Regex("[A-Z]{5}[0-9]{4}[A-Z]")) ->
                                    { error = "Enter a valid PAN (e.g. ABCDE1234F)"; return@Button }
                                aadhaarUri == null || panUri == null ->
                                    { error = "Upload photos of your Aadhaar and PAN"; return@Button }
                            }
                        }
                        loading = true; session.baseUrl = server; ApiClient.reset()
                        scope.launch {
                            try {
                                val api = ApiClient.service(session)
                                val resp = if (registering) api.registerWithDocs(
                                    username.trim(), password, fullName.trim(), fatherName.trim().ifBlank { null },
                                    mobile.trim().ifBlank { null }, email.trim().ifBlank { null },
                                    aadhaar.trim(), pan.trim().uppercase(),
                                    aadhaarUri?.let { u -> ctx.contentResolver.openInputStream(u)?.use { it.readBytes() } },
                                    panUri?.let { u -> ctx.contentResolver.openInputStream(u)?.use { it.readBytes() } })
                                else api.login(LoginRequest(username, password))
                                session.token = resp.accessToken; session.userId = resp.user.id
                                session.userName = resp.user.fullName ?: resp.user.username; onLoggedIn()
                            } catch (e: Exception) { error = e.message ?: "Failed" } finally { loading = false }
                        }
                    }, enabled = !loading, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text(if (registering) "Register" else "Sign in", fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = { registering = !registering; error = null }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (registering) "Have an account? Sign in" else "New here? Create an account", color = Teal)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { showLang = true }) {
                    Icon(Icons.Filled.Language, null, Modifier.size(16.dp), tint = Teal); Spacer(Modifier.width(6.dp)); Text("Language / भाषा", color = Teal, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showServer = !showServer }) {
                    Icon(Icons.Filled.Settings, null, Modifier.size(16.dp), tint = Muted); Spacer(Modifier.width(6.dp)); Text("Server settings", color = Muted, fontSize = 12.sp)
                }
            }
            if (showServer) Field(server, { server = it }, "Server URL", Icons.Filled.Cloud)
            Spacer(Modifier.height(18.dp))
            Text("Developed by pvalr", color = Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ---------------- MAIN SHELL (bottom nav) ----------------
@Composable
private fun MainShell(session: Session, onLogout: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                val navColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Teal, selectedTextColor = Teal, indicatorColor = Teal.copy(alpha = 0.14f),
                    unselectedIconColor = Muted, unselectedTextColor = Muted)
                NavigationBarItem(tab == 0, { tab = 0 }, icon = { Icon(Icons.Filled.Home, null) }, label = { Text("Home") }, colors = navColors)
                NavigationBarItem(tab == 1, { tab = 1 }, icon = { Icon(Icons.Filled.MedicalServices, null) }, label = { Text("My Care") }, colors = navColors)
                NavigationBarItem(tab == 2, { tab = 2 }, icon = { Icon(Icons.Filled.Person, null) }, label = { Text("Profile") }, colors = navColors)
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> BookTab(session)
                1 -> MyCareTab(session)
                else -> ProfileTab(session, onLogout)
            }
        }
    }
}

@Composable
private fun GradientHeader(title: String, subtitle: String, initial: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)).background(HeaderBrush)) {
        Row(Modifier.fillMaxWidth().padding(20.dp, 26.dp, 20.dp, 28.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.14f), border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.35f)), modifier = Modifier.size(46.dp)) {}
                Text(initial.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(Modifier.width(13.dp))
            Column {
                Text(subtitle, color = Color.White.copy(alpha = 0.82f), fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
            }
        }
    }
}

// ---------------- HOME / BOOK ----------------
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun BookTab(session: Session) {
    val scope = rememberCoroutineScope()
    val api = remember { ApiClient.service(session) }
    var packages by remember { mutableStateOf<List<ServicePackage>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var notes by remember { mutableStateOf("") }
    var emergency by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showNearby by remember { mutableStateOf(false) }
    var showRecords by remember { mutableStateOf(false) }
    var showSupport by remember { mutableStateOf(false) }
    val bookRequester = remember { BringIntoViewRequester() }
    val services = listOf(
        Triple("PICKUP_DROP", "Pickup & Drop", Icons.Filled.LocalTaxi), Triple("AMBULANCE", "Ambulance", Icons.Filled.LocalHospital),
        Triple("HOSPITAL_ADMISSION", "Admission", Icons.Filled.MedicalServices), Triple("DOCTOR_APPOINTMENT", "Doctor", Icons.Filled.Person),
        Triple("CARETAKER", "Caretaker", Icons.Filled.Favorite), Triple("ACCOMMODATION", "Stay", Icons.Filled.Hotel),
        Triple("FOOD", "Food", Icons.Filled.Restaurant), Triple("LOCAL_TRANSPORT", "Transport", Icons.Filled.DirectionsCar))
    LaunchedEffect(Unit) { try { packages = api.packages() } catch (_: Exception) {} finally { loading = false } }

    if (showNearby) { NearbyScreen(session) { showNearby = false }; return }
    if (showRecords) { RecordsScreen(session) { showRecords = false }; return }
    if (showSupport) { SupportScreen { showSupport = false }; return }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        GradientHeader(session.userName ?: "Patient", "Hello,", session.userName ?: "P")
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction(Icons.Filled.Warning, "Emergency", Danger, Modifier.weight(1f)) { emergency = true }
                QuickAction(Icons.Filled.Add, "Book", Teal, Modifier.weight(1f)) { scope.launch { bookRequester.bringIntoView() } }
                QuickAction(Icons.Filled.Folder, "Records", Color(0xFF2563EB), Modifier.weight(1f)) { showRecords = true }
                QuickAction(Icons.Filled.SupportAgent, "Support", Color(0xFF7C3AED), Modifier.weight(1f)) { showSupport = true }
            }
            Spacer(Modifier.height(14.dp))
            ElevatedCard(onClick = { showNearby = true }, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Danger.copy(alpha = 0.10f)) { Icon(Icons.Filled.LocalHospital, null, tint = Danger, modifier = Modifier.padding(11.dp).size(22.dp)) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) { Text("Nearby hospitals & ambulances", fontWeight = FontWeight.Bold, color = Ink); Text("Find help around you using GPS", color = Muted, fontSize = 12.sp) }
                    Icon(Icons.Filled.ChevronRight, null, tint = Muted)
                }
            }
            Spacer(Modifier.height(22.dp))
            Box(Modifier.bringIntoViewRequester(bookRequester)) { SectionTitle("Book assistance") }
            ElevatedCard(shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp)) {
                    services.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { (key, label, icon) -> ServiceChip(label, icon, selected.contains(key), Modifier.weight(1f)) { selected = if (selected.contains(key)) selected - key else selected + key } }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Checkbox(emergency, { emergency = it }, colors = CheckboxDefaults.colors(checkedColor = Danger)); Text("This is an emergency", color = if (emergency) Danger else Ink, fontSize = 13.sp)
                    }
                    Button(onClick = {
                        message = null
                        scope.launch {
                            try {
                                val req = api.createRequest(CreateRequest(session.userName ?: "Patient", null, selected.toList(), notes.ifBlank { null }, emergency))
                                message = "Request #${req.id} submitted — ${req.status}"; selected = emptySet(); notes = ""; emergency = false
                            } catch (e: Exception) { message = e.message ?: "Failed" }
                        }
                    }, enabled = selected.isNotEmpty(), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 6.dp)) {
                        Icon(Icons.Filled.Send, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Request assistance", fontWeight = FontWeight.SemiBold)
                    }
                    message?.let {
                        Spacer(Modifier.height(12.dp))
                        Surface(color = Teal.copy(alpha = 0.10f), shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.CheckCircle, null, tint = Teal, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(it, color = TealDark, fontSize = 13.sp) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp)); SectionTitle("Care packages")
            if (loading) Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Teal) }
            packages.forEach { p -> PackageCard(p) }
            Spacer(Modifier.height(16.dp))
            Text("Developed by pvalr", color = Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ---------------- MY CARE (cases) ----------------
@Composable
private fun MyCareTab(session: Session) {
    val api = remember { ApiClient.service(session) }
    val scope = rememberCoroutineScope()
    var cases by remember { mutableStateOf<List<CaseSummaryLite>>(emptyList()) }
    var requests by remember { mutableStateOf<List<MyRequestLite>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    fun load() { loading = true; scope.launch { try { requests = api.myRequests(); cases = api.myCases(); error = null } catch (e: Exception) { error = e.message } finally { loading = false } } }
    LaunchedEffect(Unit) { load() }
    // Dynamic sync: silently refresh requests + cases every 5s so approval/rejection shows live.
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(5000); if (selected == null) { try { requests = api.myRequests(); cases = api.myCases() } catch (_: Exception) {} } } }

    val sel = selected
    if (sel != null) {
        CaseDetailScreen(session, sel) { selected = null; load() }
    } else {
    Column(Modifier.fillMaxSize()) {
        GradientHeader("My Care", "Your active journeys", session.userName ?: "P")
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            error?.let { Text(it, color = Danger, fontSize = 12.sp) }
            if (loading) Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Teal) }
            if (!loading && cases.isEmpty() && requests.isEmpty()) EmptyState(Icons.Filled.Inbox, "Nothing yet", "Book a service from Home to raise a request")

            // My Requests: what the command centre is doing with each submitted request (live).
            if (requests.isNotEmpty()) {
                Text("My Requests", fontWeight = FontWeight.Bold, color = Ink, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
                requests.forEach { r ->
                    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(12.dp), color = Amber.copy(alpha = 0.12f)) { Icon(Icons.Filled.MedicalServices, null, tint = Amber, modifier = Modifier.padding(11.dp).size(22.dp)) }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Request #${r.id}", fontWeight = FontWeight.Bold, color = Ink)
                                val svc = if (r.services.isEmpty()) "Service request" else r.services.joinToString(", ") { it.replace('_', ' ') }
                                Text(r.caseNumber?.let { "$svc • $it" } ?: svc, color = Muted, fontSize = 12.sp, maxLines = 1)
                            }
                            StatusChip(r.status, r.emergency)
                        }
                    }
                }
                if (cases.isNotEmpty()) { Spacer(Modifier.height(6.dp)); Text("My Cases", fontWeight = FontWeight.Bold, color = Ink, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp)) }
            }

            cases.forEach { c ->
                ElevatedCard(onClick = { selected = c.id }, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(12.dp), color = Teal.copy(alpha = 0.10f)) { Icon(Icons.Filled.Assignment, null, tint = Teal, modifier = Modifier.padding(11.dp).size(22.dp)) }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.caseNumber ?: "Case ${c.id}", fontWeight = FontWeight.Bold, color = Ink)
                            Text(c.title ?: "Service case", color = Muted, fontSize = 12.sp, maxLines = 1)
                        }
                        StatusChip(c.status, c.emergency)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
    }
}

@Composable
private fun CaseDetailScreen(session: Session, caseId: Long, onBack: () -> Unit) {
    val api = remember { ApiClient.service(session) }
    val scope = rememberCoroutineScope()
    var d by remember { mutableStateOf<MyCaseDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var msg by remember { mutableStateOf<String?>(null) }
    var tracking by remember { mutableStateOf(false) }
    fun load() { loading = true; scope.launch { try { d = api.myCase(caseId) } catch (_: Exception) {} finally { loading = false } } }
    LaunchedEffect(caseId) { load() }
    // Dynamic sync: silently refresh this case every 4s so status/balance/timeline update live.
    LaunchedEffect(caseId) { while (true) { kotlinx.coroutines.delay(4000); try { d = api.myCase(caseId) } catch (_: Exception) {} } }

    if (tracking) { TrackAmbulanceScreen(session, caseId) { tracking = false }; return }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)).background(HeaderBrush)) {
            Row(Modifier.fillMaxWidth().padding(12.dp, 20.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White) }
                Column { Text(d?.caseFile?.caseNumber ?: "Case", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("Case detail", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp) }
            }
        }
        val det = d
        if (loading || det == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (loading) CircularProgressIndicator(color = Teal) else Text("Case not found", color = Muted)
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            // status + balance
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoTile("Status", det.caseFile.status.replace('_', ' '), Modifier.weight(1f))
                InfoTile("Balance", "₹${det.balance.toInt()}", Modifier.weight(1f), if (det.balance > 0) Amber else GreenC)
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { tracking = true }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Teal)) {
                Icon(Icons.Filled.LocalTaxi, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text("Track my ambulance", fontWeight = FontWeight.SemiBold)
            }
            if (det.services.isNotEmpty()) {
                Spacer(Modifier.height(16.dp)); SectionTitle("Services")
                det.services.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { s ->
                            Surface(shape = RoundedCornerShape(10.dp), color = Teal.copy(alpha = 0.10f), modifier = Modifier.weight(1f)) {
                                Text(s.replace('_', ' '), color = TealDark, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(10.dp))
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            // quotes / pay
            if (det.quotes.isNotEmpty()) {
                Spacer(Modifier.height(16.dp)); SectionTitle("Payment")
                det.quotes.forEach { q ->
                    ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Quote #${q.id}", fontWeight = FontWeight.Bold, color = Ink); StatusChip(q.status, false)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("Total ₹${q.total.toInt()} · Paid ₹${q.amountPaid.toInt()} · Balance ₹${q.balance.toInt()}", color = Muted, fontSize = 12.5.sp)
                            if (q.balance > 0) {
                                Spacer(Modifier.height(10.dp))
                                Button(onClick = { scope.launch { try { if (q.status == "SENT" || q.status == "DRAFT") api.acceptMyQuote(q.id); api.payMyQuote(PayRequest(q.id, q.balance)); msg = "Paid ₹${q.balance.toInt()}"; load() } catch (e: Exception) { msg = e.message } } },
                                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Filled.Payment, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Pay ₹${q.balance.toInt()}", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
                msg?.let { Text(it, color = Teal, fontSize = 12.5.sp) }
            }
            // appointments
            if (det.appointments.isNotEmpty()) {
                Spacer(Modifier.height(16.dp)); SectionTitle("Appointments")
                det.appointments.forEach { a -> RowCard(Icons.Filled.EventNote, a.doctorName ?: "Doctor", "${a.department ?: ""} · ${a.status.replace('_',' ')}" + (a.prescription?.let { " · Rx: $it" } ?: "")) }
            }
            // medicines
            if (det.medicines.isNotEmpty()) {
                Spacer(Modifier.height(16.dp)); SectionTitle("Medicines")
                det.medicines.forEach { m -> RowCard(Icons.Filled.Medication, "${m.name} ${m.dose ?: ""}", "${m.frequency ?: ""} · ${m.foodInstruction ?: ""}") }
            }
            // timeline
            Spacer(Modifier.height(16.dp)); SectionTitle("Journey timeline")
            det.timeline.take(20).forEach { e ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Box(Modifier.padding(top = 5.dp).size(8.dp).clip(RoundedCornerShape(50)).background(if (e.type == "EMERGENCY_ESCALATED") Danger else Teal))
                    Spacer(Modifier.width(12.dp))
                    Column { Text(e.description, fontSize = 13.sp, color = Ink); Text(e.type.replace('_', ' '), fontSize = 10.5.sp, color = Muted) }
                }
            }
            Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ---------------- TRACK AMBULANCE (native OpenStreetMap map, live) ----------------
private fun mapDot(color: Int, sizePx: Int): android.graphics.drawable.Drawable =
    android.graphics.drawable.GradientDrawable().apply {
        shape = android.graphics.drawable.GradientDrawable.OVAL
        setColor(color)
        setStroke(6, android.graphics.Color.WHITE)
        setSize(sizePx, sizePx)
    }

private fun haversineKm(a: org.osmdroid.util.GeoPoint, b: org.osmdroid.util.GeoPoint): Double {
    val r = 6371.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLng = Math.toRadians(b.longitude - a.longitude)
    val s = Math.sin(dLat / 2).let { it * it } +
        Math.cos(Math.toRadians(a.latitude)) * Math.cos(Math.toRadians(b.latitude)) *
        Math.sin(dLng / 2).let { it * it }
    return r * 2 * Math.atan2(Math.sqrt(s), Math.sqrt(1 - s))
}

@Composable
private fun TrackAmbulanceScreen(session: Session, caseId: Long, onBack: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val api = remember { ApiClient.service(session) }
    var status by remember { mutableStateOf("Locating your ambulance…") }
    var eta by remember { mutableStateOf("") }

    val mapView = remember {
        org.osmdroid.config.Configuration.getInstance().userAgentValue = ctx.packageName
        org.osmdroid.views.MapView(ctx).apply {
            setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(12.0)
            controller.setCenter(org.osmdroid.util.GeoPoint(12.9716, 77.5946))
        }
    }
    val ambulance = remember {
        org.osmdroid.views.overlay.Marker(mapView).apply {
            icon = mapDot(0xFF2563EB.toInt(), 46); setAnchor(0.5f, 0.5f); title = "Ambulance"
        }
    }
    val routeLine = remember {
        org.osmdroid.views.overlay.Polyline(mapView).apply { outlinePaint.color = 0xFF2563EB.toInt(); outlinePaint.strokeWidth = 12f }
    }
    var staticAdded by remember { mutableStateOf(false) }
    var fitted by remember { mutableStateOf(false) }

    // Live: re-fetch the route every 2s and move the ambulance marker (true near-real-time).
    LaunchedEffect(caseId) {
        while (true) {
            try {
                val r = api.caseRoute(caseId)
                val pk = r.pickup?.takeIf { it.lat != null && it.lng != null }?.let { org.osmdroid.util.GeoPoint(it.lat!!, it.lng!!) }
                val hp = r.destination?.takeIf { it.lat != null && it.lng != null }?.let { org.osmdroid.util.GeoPoint(it.lat!!, it.lng!!) }
                if (!staticAdded) {
                    pk?.let { g -> mapView.overlays.add(org.osmdroid.views.overlay.Marker(mapView).apply { position = g; icon = mapDot(0xFF16A34A.toInt(), 42); setAnchor(0.5f, 0.5f); title = "Pickup" }) }
                    hp?.let { g -> mapView.overlays.add(org.osmdroid.views.overlay.Marker(mapView).apply { position = g; icon = mapDot(0xFFDC2626.toInt(), 42); setAnchor(0.5f, 0.5f); title = "Hospital" }) }
                    mapView.overlays.add(routeLine)
                    mapView.overlays.add(ambulance)
                    staticAdded = true
                }
                val d = r.driver
                if (d?.lat != null && d.lng != null) {
                    val dg = org.osmdroid.util.GeoPoint(d.lat!!, d.lng!!)
                    ambulance.position = dg
                    val st = (r.assignmentStatus ?: d.status ?: "EN_ROUTE")
                    ambulance.title = "${d.name} · ${st.replace('_', ' ')}"
                    status = "Ambulance ${st.replace('_', ' ')}"
                    val beforePickup = st in listOf("OFFERED", "ACCEPTED", "EN_ROUTE", "ARRIVED_AT_PICKUP", "PATIENT_VERIFIED")
                    val target = if (beforePickup) pk else hp
                    val label = if (beforePickup) "your pickup" else (r.destination?.label ?: "hospital")
                    if (target != null) eta = "Approx ${"%.1f".format(haversineKm(dg, target))} km from $label · ${d.name}"

                    // Road-snapped line comes ready from the backend (driver -> pickup -> hospital).
                    val road = r.polyline?.mapNotNull { p -> if (p.size >= 2) org.osmdroid.util.GeoPoint(p[0], p[1]) else null }
                        ?: buildList { add(dg); pk?.let { add(it) }; hp?.let { add(it) } }
                    if (road.size >= 2) {
                        routeLine.setPoints(road)
                        if (!fitted) {
                            mapView.zoomToBoundingBox(org.osmdroid.util.BoundingBox.fromGeoPoints(road).increaseByScale(1.4f), true)
                            fitted = true
                        }
                    }
                } else {
                    status = "Ambulance not dispatched yet"
                }
                mapView.invalidate()
            } catch (_: Exception) {}
            kotlinx.coroutines.delay(2000)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)).background(HeaderBrush)) {
            Row(Modifier.fillMaxWidth().padding(12.dp, 20.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White) }
                Column { Text("Track my ambulance", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("Live location", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp) }
            }
        }
        AndroidView(modifier = Modifier.weight(1f).fillMaxWidth(), factory = { mapView }, onRelease = { it.onDetach() })
        Row(Modifier.fillMaxWidth().background(Color.White).padding(16.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(50), color = Teal.copy(alpha = 0.12f)) { Icon(Icons.Filled.LocalTaxi, null, tint = Teal, modifier = Modifier.padding(10.dp).size(20.dp)) }
            Spacer(Modifier.width(12.dp))
            Column { Text(status, fontWeight = FontWeight.Bold, color = Teal, fontSize = 15.sp); if (eta.isNotEmpty()) Text(eta, color = Muted, fontSize = 12.5.sp) }
        }
    }
}

// ---------------- NEARBY (device GPS: hospitals & ambulances) ----------------
@android.annotation.SuppressLint("MissingPermission")
private fun getDeviceLocation(ctx: android.content.Context, onResult: (Double, Double) -> Unit) {
    try {
        val lm = ctx.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        for (p in listOf(android.location.LocationManager.GPS_PROVIDER, android.location.LocationManager.NETWORK_PROVIDER)) {
            try { lm.getLastKnownLocation(p)?.let { onResult(it.latitude, it.longitude); return } } catch (_: Exception) {}
        }
        val provider = if (lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER))
            android.location.LocationManager.GPS_PROVIDER else android.location.LocationManager.NETWORK_PROVIDER
        lm.getCurrentLocation(provider, null, ctx.mainExecutor) { loc ->
            if (loc != null) onResult(loc.latitude, loc.longitude) else onResult(17.39, 78.36)
        }
    } catch (_: Exception) { onResult(17.39, 78.36) }
}

@Composable
private fun NearbyScreen(session: Session, onBack: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val api = remember { ApiClient.service(session) }
    var loc by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var result by remember { mutableStateOf<NearbyResultDto?>(null) }
    var status by remember { mutableStateOf("Getting your location…") }
    var tab by remember { mutableStateOf(0) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) getDeviceLocation(ctx) { la, ln -> loc = la to ln }
        else { status = "Location off — showing your area approximately"; loc = 17.39 to 78.36 }
    }
    LaunchedEffect(Unit) {
        val ok = androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (ok) getDeviceLocation(ctx) { la, ln -> loc = la to ln } else permLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }
    LaunchedEffect(loc) {
        val l = loc ?: return@LaunchedEffect
        status = "Finding nearby…"
        try { result = api.nearby(l.first, l.second, 8); status = "" } catch (e: Exception) { status = "Couldn't load: ${e.message}" }
    }

    val mapView = remember {
        org.osmdroid.config.Configuration.getInstance().userAgentValue = ctx.packageName
        org.osmdroid.views.MapView(ctx).apply {
            setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK); setMultiTouchControls(true)
            controller.setZoom(13.0); controller.setCenter(org.osmdroid.util.GeoPoint(17.39, 78.36))
        }
    }
    LaunchedEffect(result, loc) {
        mapView.overlays.clear()
        loc?.let {
            mapView.overlays.add(org.osmdroid.views.overlay.Marker(mapView).apply { position = org.osmdroid.util.GeoPoint(it.first, it.second); icon = mapDot(0xFF2563EB.toInt(), 40); setAnchor(0.5f, 0.5f); title = "You" })
            mapView.controller.setCenter(org.osmdroid.util.GeoPoint(it.first, it.second))
        }
        result?.hospitals?.forEach { h -> if (h.lat != null && h.lng != null) mapView.overlays.add(org.osmdroid.views.overlay.Marker(mapView).apply { position = org.osmdroid.util.GeoPoint(h.lat!!, h.lng!!); icon = mapDot(0xFFDC2626.toInt(), 36); setAnchor(0.5f, 0.5f); title = "${h.name} · ${h.distanceKm} km" }) }
        result?.ambulances?.forEach { a -> if (a.lat != null && a.lng != null) mapView.overlays.add(org.osmdroid.views.overlay.Marker(mapView).apply { position = org.osmdroid.util.GeoPoint(a.lat!!, a.lng!!); icon = mapDot(0xFF0F766E.toInt(), 32); setAnchor(0.5f, 0.5f); title = "${a.registrationNo} · ${a.status}" }) }
        mapView.invalidate()
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)).background(HeaderBrush)) {
            Row(Modifier.fillMaxWidth().padding(12.dp, 20.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White) }
                Column { Text("Nearby care", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("Hospitals & ambulances around you", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp) }
            }
        }
        AndroidView(modifier = Modifier.weight(1f).fillMaxWidth(), factory = { mapView }, onRelease = { it.onDetach() })
        Row(Modifier.fillMaxWidth().padding(16.dp, 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TabChip("Hospitals (${result?.hospitals?.size ?: 0})", tab == 0, Modifier.weight(1f)) { tab = 0 }
            TabChip("Ambulances (${result?.ambulances?.size ?: 0})", tab == 1, Modifier.weight(1f)) { tab = 1 }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp, 0.dp, 16.dp, 16.dp)) {
            if (status.isNotEmpty()) { Text(status, color = Muted, fontSize = 13.sp); Spacer(Modifier.height(8.dp)) }
            if (tab == 0) result?.hospitals?.forEach { h -> HospitalRow(h) { phone -> ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) } }
            else result?.ambulances?.forEach { a -> AmbulanceRow(a) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable private fun TabChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(50), color = if (selected) Teal else Color(0xFFF1F5F9), modifier = modifier) {
        Text(label, color = if (selected) Color.White else Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 10.dp))
    }
}
@Composable private fun HospitalRow(h: NearbyHospitalDto, onCall: (String) -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(10.dp), color = Danger.copy(alpha = 0.10f)) { Icon(Icons.Filled.LocalHospital, null, tint = Danger, modifier = Modifier.padding(9.dp).size(20.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(h.name, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 14.sp, maxLines = 1)
                    if (h.emergency) { Spacer(Modifier.width(6.dp)); Surface(shape = RoundedCornerShape(50), color = Danger.copy(alpha = 0.12f)) { Text("24×7", color = Danger, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp, 2.dp)) } }
                }
                Text("${h.distanceKm} km · ${h.address ?: ""}", color = Muted, fontSize = 11.5.sp, maxLines = 1)
            }
            if (!h.phone.isNullOrBlank()) IconButton(onClick = { onCall(h.phone!!) }) { Icon(Icons.Filled.Call, "Call", tint = GreenC) }
        }
    }
}
@Composable private fun AmbulanceRow(a: NearbyAmbulanceDto) {
    ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(10.dp), color = Teal.copy(alpha = 0.10f)) { Icon(Icons.Filled.LocalTaxi, null, tint = Teal, modifier = Modifier.padding(9.dp).size(20.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(a.registrationNo, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 14.sp)
                Text("${a.distanceKm} km · O₂ ${a.oxygenLevelPercent ?: "-"}%", color = Muted, fontSize = 11.5.sp)
            }
            StatusChip(a.status, false)
        }
    }
}

// ---------------- MEDICAL RECORDS ----------------
@Composable
private fun RecordsScreen(session: Session, onBack: () -> Unit) {
    val api = remember { ApiClient.service(session) }
    var records by remember { mutableStateOf<MyRecordsDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { try { records = api.myRecords() } catch (_: Exception) {} finally { loading = false } }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)).background(HeaderBrush)) {
            Row(Modifier.fillMaxWidth().padding(12.dp, 20.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White) }
                Column { Text("Medical records", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("Your appointments & medicines", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp) }
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (loading) Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Teal) }
            val r = records
            if (!loading && r != null) {
                if (r.appointments.isEmpty() && r.medicines.isEmpty())
                    EmptyState(Icons.Filled.Folder, "No records yet", "Appointments and prescriptions will appear here")
                if (r.appointments.isNotEmpty()) {
                    SectionTitle("Appointments")
                    r.appointments.forEach { a -> RowCard(Icons.Filled.EventNote, a.doctorName ?: "Doctor", "${a.department ?: ""} · ${a.status.replace('_', ' ')}" + (a.prescription?.let { " · Rx: $it" } ?: "")) }
                }
                if (r.medicines.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp)); SectionTitle("Medicines")
                    r.medicines.forEach { m -> RowCard(Icons.Filled.Medication, "${m.name} ${m.dose ?: ""}", "${m.frequency ?: ""} · ${m.foodInstruction ?: ""}") }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ---------------- SUPPORT ----------------
@Composable
private fun SupportScreen(onBack: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val supportNumber = "+91 90000 00360"
    var showCall by remember { mutableStateOf(false) }
    fun open(intent: android.content.Intent) { try { ctx.startActivity(intent) } catch (_: Exception) { android.widget.Toast.makeText(ctx, "No app available for this action", android.widget.Toast.LENGTH_SHORT).show() } }

    if (showCall) {
        AlertDialog(
            onDismissRequest = { showCall = false },
            icon = { Icon(Icons.Filled.Call, null, tint = GreenC) },
            title = { Text("Call our support", fontWeight = FontWeight.Bold, color = Ink) },
            text = { Column { Text(supportNumber, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink); Spacer(Modifier.height(4.dp)); Text("Mon–Sun, 24×7", color = Muted, fontSize = 13.sp) } },
            confirmButton = { TextButton(onClick = { showCall = false; open(android.content.Intent(android.content.Intent.ACTION_DIAL, Uri.parse("tel:+919000000360"))) }) { Text("Call now", color = GreenC, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { showCall = false }) { Text("Close", color = Muted) } }
        )
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)).background(HeaderBrush)) {
            Row(Modifier.fillMaxWidth().padding(12.dp, 20.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White) }
                Column { Text("Support", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("We're here to help, 24×7", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp) }
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            SupportCard("Call us", supportNumber, Icons.Filled.Call, GreenC) { showCall = true }
            SupportCard("WhatsApp", "Chat with our team", Icons.Filled.Chat, GreenC) { open(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("https://wa.me/919000000360"))) }
            SupportCard("Email", "support@360patientcare.in", Icons.Filled.Email, Teal) { open(android.content.Intent(android.content.Intent.ACTION_SENDTO, Uri.parse("mailto:support@360patientcare.in"))) }
            SupportCard("Emergency helpline", "Call 108 (Ambulance)", Icons.Filled.Warning, Danger) { open(android.content.Intent(android.content.Intent.ACTION_DIAL, Uri.parse("tel:108"))) }
        }
    }
}
@Composable private fun SupportCard(title: String, sub: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.12f)) { Icon(icon, null, tint = color, modifier = Modifier.padding(11.dp).size(22.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, color = Ink); Text(sub, color = Muted, fontSize = 12.sp) }
            Icon(Icons.Filled.ChevronRight, null, tint = Muted)
        }
    }
}

// ---------------- PROFILE ----------------
@Composable
private fun ProfileTab(session: Session, onLogout: () -> Unit) {
    var showLang by remember { mutableStateOf(false) }
    if (showLang) LanguageDialog(session) { showLang = false }
    Column(Modifier.fillMaxSize()) {
        GradientHeader(session.userName ?: "Patient", "Profile", session.userName ?: "P")
        Column(Modifier.padding(16.dp)) {
            ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp)) {
                    InfoRow("Name", session.userName ?: "—"); InfoRow("Account", "Patient")
                    InfoRow("Server", session.baseUrl)
                }
            }
            Spacer(Modifier.height(16.dp))
            ElevatedCard(onClick = { showLang = true }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Language, null, tint = Teal, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text("Language / भाषा", fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 14.sp); Text("English", color = Muted, fontSize = 12.sp) }
                    Icon(Icons.Filled.ChevronRight, null, tint = Muted)
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onLogout, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Icon(Icons.Filled.Logout, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Logout")
            }
            Spacer(Modifier.height(20.dp))
            Text("Developed by pvalr", color = Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }
}

// ---------------- SHARED UI ----------------
@Composable private fun InfoRow(k: String, v: String) { Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(k, color = Muted, fontSize = 13.sp); Text(v, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium) } }
@Composable private fun InfoTile(label: String, value: String, modifier: Modifier, color: Color = Teal) {
    ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = modifier, colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp)) { Text(label, color = Muted, fontSize = 11.5.sp); Spacer(Modifier.height(4.dp)); Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
    }
}
@Composable private fun RowCard(icon: ImageVector, title: String, sub: String) {
    ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Teal, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp))
            Column { Text(title, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 14.sp); Text(sub, color = Muted, fontSize = 12.sp) }
        }
    }
}
@Composable private fun StatusChip(status: String, emergency: Boolean) {
    val c = when {
        status == "REJECTED" || status == "CANCELLED" || status == "EXPIRED" -> Danger
        emergency || status == "EMERGENCY_ESCALATED" -> Danger
        status == "CLOSED" || status == "COMPLETED" || status == "PAID" || status == "CONFIRMED" -> GreenC
        status == "ON_HOLD" || status == "INFO_REQUIRED" -> Amber
        else -> Teal
    }
    Surface(shape = RoundedCornerShape(50), color = c.copy(alpha = 0.12f)) { Text(status.replace('_', ' '), color = c, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(10.dp, 5.dp)) }
}
@Composable private fun EmptyState(icon: ImageVector, title: String, sub: String) {
    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Muted, modifier = Modifier.size(40.dp)); Spacer(Modifier.height(10.dp)); Text(title, color = Muted, fontSize = 14.sp); Text(sub, color = Muted, fontSize = 11.5.sp)
        }
    }
}
@Composable private fun Field(value: String, onChange: (String) -> Unit, label: String, icon: ImageVector, password: Boolean = false,
                              keyboard: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text) {
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true, leadingIcon = { Icon(icon, null, tint = Muted) },
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = if (password) androidx.compose.ui.text.input.KeyboardType.Password else keyboard),
        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
}
@Composable
private fun LanguageDialog(session: Session, onDismiss: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var selected by remember { mutableStateOf(session.language) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = Teal) } },
        title = { Text("Choose language / भाषा चुनें", fontWeight = FontWeight.Bold, color = Ink) },
        text = {
            Column {
                LangRow("English", "Active", selected == "en", false) {
                    session.language = "en"; selected = "en"
                    android.widget.Toast.makeText(ctx, "Language set to English", android.widget.Toast.LENGTH_SHORT).show()
                }
                Spacer(Modifier.height(10.dp))
                LangRow("हिंदी", "Hindi", false, true) {
                    android.widget.Toast.makeText(ctx, "Hindi coming soon 🚧", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    )
}

@Composable
private fun LangRow(title: String, sub: String, selected: Boolean, comingSoon: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp),
        color = if (selected) Teal.copy(alpha = 0.10f) else Color(0xFFF1F5F9), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 15.sp)
                Text(sub, color = Muted, fontSize = 12.sp)
            }
            if (comingSoon) Surface(shape = RoundedCornerShape(50), color = Amber.copy(alpha = 0.15f)) {
                Text("Coming soon", color = Amber, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(9.dp, 4.dp))
            } else if (selected) Icon(Icons.Filled.CheckCircle, null, tint = GreenC)
        }
    }
}

@Composable private fun UploadRow(label: String, done: Boolean, onPick: () -> Unit) {
    OutlinedButton(onClick = onPick, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Icon(if (done) Icons.Filled.CheckCircle else Icons.Filled.CloudUpload, null, Modifier.size(18.dp), tint = if (done) GreenC else Muted)
        Spacer(Modifier.width(8.dp))
        Text(if (done) "$label — selected ✓" else "Upload $label", color = if (done) GreenC else Ink)
    }
}
@Composable private fun QuickAction(icon: ImageVector, label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(18.dp), modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(vertical = 15.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(14.dp), color = color.copy(alpha = 0.12f)) { Icon(icon, null, tint = color, modifier = Modifier.padding(10.dp).size(23.dp)) }
            Spacer(Modifier.height(9.dp)); Text(label, fontSize = 11.5.sp, color = Ink, fontWeight = FontWeight.SemiBold)
        }
    }
}
@Composable private fun SectionTitle(text: String) { Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(bottom = 10.dp)) }
@Composable private fun ServiceChip(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp), color = if (selected) Teal else Color(0xFFF1F5F9), border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = modifier) {
        Row(Modifier.padding(12.dp, 14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = if (selected) Color.White else Teal, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(label, color = if (selected) Color.White else Ink, fontSize = 12.5.sp, fontWeight = FontWeight.Medium) }
    }
}
@Composable private fun PackageCard(p: ServicePackage) {
    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = Teal.copy(alpha = 0.10f)) { Icon(Icons.Filled.Inventory2, null, tint = Teal, modifier = Modifier.padding(12.dp).size(24.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) { Text(p.name, fontWeight = FontWeight.Bold, color = Ink, fontSize = 15.sp); p.description?.let { Text(it, color = Muted, fontSize = 12.sp, maxLines = 2) } }
            Spacer(Modifier.width(10.dp))
            Surface(shape = RoundedCornerShape(50), color = Teal.copy(alpha = 0.12f)) { Text("₹${p.indicativeTotal.toInt()}", color = TealDark, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(12.dp, 6.dp)) }
        }
    }
}
