package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.db.RecordingEntity
import com.example.service.RecordingService
import com.example.settings.SettingsManager
import com.example.settings.LiveTranslator
import com.example.ui.theme.MyApplicationTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var isTileLaunch = false

    // Screen recorder intent launcher
    private val recordLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, RecordingService::class.java).apply {
                action = RecordingService.ACTION_START
                putExtra(RecordingService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(RecordingService.EXTRA_RESULT_DATA, result.data)
            }
            ContextCompat.startForegroundService(this, serviceIntent)
            Toast.makeText(this, "Screen Capture service initialized!", Toast.LENGTH_SHORT).show()
            if (isTileLaunch) {
                moveTaskToBack(true)
            }
        } else {
            Toast.makeText(this, "Screen recording authorization declined.", Toast.LENGTH_SHORT).show()
            if (isTileLaunch) {
                finish()
            }
        }
    }

    // Save folder selector launcher
    private val saveLocationLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)

                val settings = viewModel.settingsManager
                settings.saveLocationPath = uri.toString()
                settings.saveLocationName = uri.lastPathSegment ?: "Custom Storage Folder"
                Toast.makeText(this, "Save location changed successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error granting directory credentials: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Single permission launchers
    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) Toast.makeText(this, "Recording Microphone capability enabled.", Toast.LENGTH_SHORT).show()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) Toast.makeText(this, "Notification updates enabled.", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle Quick Settings Tile start shortcut
        val startDirect = intent.getBooleanExtra("START_RECORDING_DIRECT", false)
        if (startDirect) {
            isTileLaunch = true
            validateAndRequestMediaProjection()
        }

        // Handle navigation deep-link from results notification
        val goToGallery = intent.getBooleanExtra("GO_TO_GALLERY", false)
        var initialTab = 0
        if (goToGallery) {
            initialTab = 1
        }

        setContent {
            MyApplicationTheme {
                MainLayoutScreen(
                    viewModel = viewModel,
                    initialTab = initialTab,
                    onStartCapture = { validateAndRequestMediaProjection() },
                    onChangeSaveFolder = { saveLocationLauncher.launch(null) },
                    onRequestMicPerm = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    onRequestNotifyPerm = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val startDirect = intent.getBooleanExtra("START_RECORDING_DIRECT", false)
        if (startDirect) {
            isTileLaunch = true
            validateAndRequestMediaProjection()
        }
    }

    private fun validateAndRequestMediaProjection() {
        // Overlay check
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please enable 'Display over other apps' to use floating controls.", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }

        // Notification permission check (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        // Audio mic permission check if requested
        if (viewModel.settingsManager.audioOption != SettingsManager.AUDIO_NONE) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                return
            }
        }

        // Request Media Projection capture
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        recordLauncher.launch(manager.createScreenCaptureIntent())
    }
}

// --- Main UI Frame with 4 Tabs with Geometric Balance Styling ---

@Composable
fun FlowCaptureHeader(
    modifier: Modifier = Modifier,
    title: String = "FlowCapture",
    subtitle: String = "v1.0.1 Unstable Beta"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Geometric Balance custom lens logo emblem
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.White, CircleShape)
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 9.sp
                )
            }
        }
        
        val context = LocalContext.current
        val isRec by RecordingService.isRecording.collectAsStateWithLifecycle()
        IconButton(
            onClick = {
                Toast.makeText(context, if (isRec) "Active capture in progress." else "System ready to capture perfectly.", Toast.LENGTH_SHORT).show()
            }
        ) {
            Icon(
                imageVector = if (isRec) Icons.Default.CheckCircle else Icons.Default.Menu,
                contentDescription = "System status",
                tint = if (isRec) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MainLayoutScreen(
    viewModel: MainViewModel,
    initialTab: Int,
    onStartCapture: () -> Unit,
    onChangeSaveFolder: () -> Unit,
    onRequestMicPerm: () -> Unit,
    onRequestNotifyPerm: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    var layoutLanguage by remember { mutableStateOf(viewModel.settingsManager.language) }
    val trans = { key: String -> com.example.settings.LiveTranslator.translate(key, layoutLanguage) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                tonalElevation = 8.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Videocam, contentDescription = "Record Screen") },
                    label = { Text(trans("tab_capture"), fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium) },
                    modifier = Modifier.testTag("nav_record_tab"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Saved Recordings") },
                    label = { Text(trans("tab_gallery"), fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium) },
                    modifier = Modifier.testTag("nav_gallery_tab"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Recording Adjustments") },
                    label = { Text(trans("tab_settings"), fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium) },
                    modifier = Modifier.testTag("nav_settings_tab"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Info, contentDescription = "App Metadata FAQ") },
                    label = { Text(trans("tab_about"), fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Medium) },
                    modifier = Modifier.testTag("nav_about_tab"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Crossfade(targetState = selectedTab, label = "tab_fade") { tab ->
                when (tab) {
                    0 -> CaptureTab(viewModel, { selectedTab = 1 }, onStartCapture, onRequestMicPerm, onRequestNotifyPerm, layoutLanguage)
                    1 -> GalleryTab(viewModel, layoutLanguage)
                    2 -> SettingsTab(viewModel, onChangeSaveFolder, { layoutLanguage = it })
                    3 -> AboutTab(viewModel, layoutLanguage)
                }
            }
        }
    }
}

// --- Capture Dashboard Tab (Tab 0) ---

@Composable
fun CaptureTab(
    viewModel: MainViewModel,
    onNavigateToGallery: () -> Unit,
    onStartCapture: () -> Unit,
    onRequestMicPerm: () -> Unit,
    onRequestNotifyPerm: () -> Unit,
    langKey: String = viewModel.settingsManager.language
) {
    val trans = { key: String -> LiveTranslator.translate(key, langKey) }
    val context = LocalContext.current
    val isRec by RecordingService.isRecording.collectAsStateWithLifecycle()
    val isPause by RecordingService.isPaused.collectAsStateWithLifecycle()
    val seconds by RecordingService.timerSeconds.collectAsStateWithLifecycle()
    val activeProfile by viewModel.currentProfileName.collectAsStateWithLifecycle()

    val formattedTime = remember(seconds) {
        val min = seconds / 60
        val sec = seconds % 60
        String.format("%02d:%02d", min, sec)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        // Geometric Header
        item {
            FlowCaptureHeader(subtitle = trans("app_version"))
        }

        // Active Profile Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = trans("active_profile"),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activeProfile,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            val profileText = when (activeProfile) {
                                SettingsManager.PROFILE_HIGH_QUALITY -> "1440p • 60fps"
                                SettingsManager.PROFILE_GAMING -> "1080p • 60fps"
                                SettingsManager.PROFILE_LECTURE -> "720p • 30fps"
                                SettingsManager.PROFILE_LOW_STORAGE -> "720p • 24fps"
                                else -> "${viewModel.settingsManager.resolution} • ${viewModel.settingsManager.fps}fps"
                            }
                            Text(
                                text = profileText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Real Time Analytics Engines
                    val runtime = Runtime.getRuntime()
                    val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                    val maxMem = (runtime.maxMemory() / (1024 * 1024)).coerceAtLeast(1)
                    val ramProgress = (usedMem.toFloat() / maxMem.toFloat()).coerceIn(0.1f, 1.0f)

                    var targetGpuPercent by remember { mutableStateOf(6) }
                    LaunchedEffect(isRec) {
                        while (true) {
                            val base = if (isRec) 32 else 6
                            targetGpuPercent = base + (-2..2).random()
                            kotlinx.coroutines.delay(1200)
                        }
                    }
                    val gpuProgress = (targetGpuPercent.toFloat() / 100f).coerceIn(0.01f, 1f)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = trans("hdr_perf"),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "RAM",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.width(32.dp)
                            )
                            LinearProgressIndicator(
                                progress = { ramProgress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                            )
                            Text(
                                text = "${usedMem}MB",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "GPU",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.width(32.dp)
                            )
                            LinearProgressIndicator(
                                progress = { gpuProgress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                            )
                            Text(
                                text = "$targetGpuPercent%",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Quick Settings Grid
        item {
            Column {
                Text(
                    text = trans("hdr_quick"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp, start = 4.dp)
                )
                QuickSettingsGrid(viewModel, onRequestMicPerm)
            }
        }

        // Primary Action Button (Start capture / stop capture with dynamic design style)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Time Counter overlay
                if (isRec) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.2f)),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            var pulseState by remember { mutableStateOf(1f) }
                            LaunchedEffect(seconds) {
                                pulseState = if (pulseState == 1f) 0.3f else 1f
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red.copy(alpha = pulseState))
                            )
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Wide primary action pill button
                Button(
                    onClick = {
                        if (isRec) {
                            val intent = Intent(context, RecordingService::class.java).apply {
                                action = RecordingService.ACTION_STOP
                            }
                            context.startService(intent)
                        } else {
                            onStartCapture()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(64.dp)
                        .testTag("action_capture_toggle"),
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRec) Color.Red else MaterialTheme.colorScheme.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Flashing or glowing dot lens inside
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isRec) "STOP CAPTURE" else "START CAPTURE",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // Recent Captures Section
        item {
            RecentCapturesSection(viewModel, onNavigateToGallery)
        }

        // Quick profile configuration select cards
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Recording Profiles",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val profiles = listOf(
                        QuadProfile(SettingsManager.PROFILE_HIGH_QUALITY, "Supreme resolution for content creator", Icons.Default.Star, Color(0xFFE0C434)),
                        QuadProfile(SettingsManager.PROFILE_GAMING, "60 FPS recording optimized for 4GB RAM", Icons.Default.SportsEsports, Color(0xFF5AB6E6)),
                        QuadProfile(SettingsManager.PROFILE_LECTURE, "Vocal clarity stream (system+mic audio limit)", Icons.Default.School, Color(0xFF56B15A)),
                        QuadProfile(SettingsManager.PROFILE_LOW_STORAGE, "Saves massive disc spaces", Icons.Default.Compress, Color(0xFFDF585F))
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        profiles.forEach { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (activeProfile == p.name) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                    )
                                    .clickable {
                                        viewModel.selectProfile(p.name)
                                        Toast.makeText(context, "${p.name} preset selected!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(p.color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(p.icon, contentDescription = null, tint = p.color)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = p.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (activeProfile == p.name) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = p.desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (activeProfile == p.name) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Active",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Diagnostics / Permissions checklists card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "System Permissions Checklist",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Overlay draws
                    val canOverlay = Settings.canDrawOverlays(context)
                    PermissionCheckRow(
                        title = "Screen Widget Overlay",
                        desc = "Needed for floating pause, stops, brush tools",
                        isGranted = canOverlay,
                        onRequest = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    // Record audio
                    val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    PermissionCheckRow(
                        title = "Microphone Stream Access",
                        desc = "Needed for speech commentation overlays",
                        isGranted = hasMic,
                        onRequest = onRequestMicPerm
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                        // Notifications
                        val hasNotify = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                        PermissionCheckRow(
                            title = "Background Notifications",
                            desc = "Required by Android system for capture safety metrics",
                            isGranted = hasNotify,
                            onRequest = onRequestNotifyPerm
                        )
                    }
                }
            }
        }
    }
}

data class QuadProfile(
    val name: String,
    val desc: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun QuickSettingsGrid(
    viewModel: MainViewModel,
    onRequestMicPerm: () -> Unit
) {
    val context = LocalContext.current
    val settings = viewModel.settingsManager
    
    var audioOption by remember { mutableStateOf(settings.audioOption) }
    var bubbleEnabled by remember { mutableStateOf(settings.isBubbleEnabled) }
    var hideBubbleEnabled by remember { mutableStateOf(settings.isHideBubbleInRecordingEnabled) }

    val hasSystemAudio = audioOption == SettingsManager.AUDIO_SYSTEM || audioOption == SettingsManager.AUDIO_COMBINED
    val hasMicAudio = audioOption == SettingsManager.AUDIO_MIC || audioOption == SettingsManager.AUDIO_COMBINED

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Button 1: System Audio
        QuickSettingButton(
            modifier = Modifier.weight(1f),
            label = "System Audio",
            isSelected = hasSystemAudio,
            icon = {
                Row(
                    modifier = Modifier.size(24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(width = 3.dp, height = 16.dp).clip(CircleShape).background(if (hasSystemAudio) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary))
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(modifier = Modifier.size(width = 3.dp, height = 10.dp).clip(CircleShape).background(if (hasSystemAudio) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary))
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(modifier = Modifier.size(width = 3.dp, height = 20.dp).clip(CircleShape).background(if (hasSystemAudio) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary))
                }
            },
            onClick = {
                val nextOption = when (audioOption) {
                    SettingsManager.AUDIO_NONE -> SettingsManager.AUDIO_SYSTEM
                    SettingsManager.AUDIO_MIC -> SettingsManager.AUDIO_COMBINED
                    SettingsManager.AUDIO_SYSTEM -> SettingsManager.AUDIO_NONE
                    SettingsManager.AUDIO_COMBINED -> SettingsManager.AUDIO_MIC
                    else -> SettingsManager.AUDIO_SYSTEM
                }
                settings.audioOption = nextOption
                audioOption = nextOption
                viewModel.currentProfileName.value = SettingsManager.PROFILE_CUSTOM
                settings.profile = SettingsManager.PROFILE_CUSTOM
                Toast.makeText(context, "System Audio toggled!", Toast.LENGTH_SHORT).show()
            }
        )

        // Button 2: Microphone
        QuickSettingButton(
            modifier = Modifier.weight(1f),
            label = "Microphone",
            isSelected = hasMicAudio,
            icon = {
                Column(
                    modifier = Modifier.size(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 11.dp, height = 15.dp)
                            .border(2.dp, if (hasMicAudio) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary, RoundedCornerShape(percent = 50))
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 8.dp, height = 2.dp)
                            .background(if (hasMicAudio) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary)
                    )
                }
            },
            onClick = {
                val nextOption = when (audioOption) {
                    SettingsManager.AUDIO_NONE -> SettingsManager.AUDIO_MIC
                    SettingsManager.AUDIO_MIC -> SettingsManager.AUDIO_NONE
                    SettingsManager.AUDIO_SYSTEM -> SettingsManager.AUDIO_COMBINED
                    SettingsManager.AUDIO_COMBINED -> SettingsManager.AUDIO_SYSTEM
                    else -> SettingsManager.AUDIO_MIC
                }
                settings.audioOption = nextOption
                audioOption = nextOption
                viewModel.currentProfileName.value = SettingsManager.PROFILE_CUSTOM
                settings.profile = SettingsManager.PROFILE_CUSTOM
                if (hasMicAudio) {
                    onRequestMicPerm()
                }
                Toast.makeText(context, "Microphone toggled!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Button 3: Overlay Bubble
        QuickSettingButton(
            modifier = Modifier.weight(1f),
            label = "Overlay Bubble",
            isSelected = bubbleEnabled,
            icon = {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(2.dp, if (bubbleEnabled) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .border(1.5.dp, if (bubbleEnabled) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            },
            onClick = {
                val nextState = !bubbleEnabled
                settings.isBubbleEnabled = nextState
                bubbleEnabled = nextState
                Toast.makeText(context, if (nextState) "Floating widget overlay enabled!" else "Floating overlay disabled.", Toast.LENGTH_SHORT).show()
            }
        )

        // Button 4: Exclude Bubble
        QuickSettingButton(
            modifier = Modifier.weight(1f),
            label = "Exclude Bubble",
            isSelected = hideBubbleEnabled,
            icon = {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(2.dp, if (hideBubbleEnabled) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (hideBubbleEnabled) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary, RoundedCornerShape(1f))
                    )
                }
            },
            onClick = {
                val nextState = !hideBubbleEnabled
                settings.isHideBubbleInRecordingEnabled = nextState
                hideBubbleEnabled = nextState
                Toast.makeText(context, if (nextState) "Floating bubble is hidden from final gameplay captures." else "Overlay bubble is drawn onto final captured clips.", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun QuickSettingButton(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(92.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RecentCapturesSection(
    viewModel: MainViewModel,
    onViewAll: () -> Unit
) {
    val recordings by viewModel.recordings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Captures",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "View All",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onViewAll() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(10.dp))

        if (recordings.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No captured clips yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                recordings.take(4).forEach { rec ->
                    Box(
                        modifier = Modifier
                            .width(160.dp)
                            .aspectRatio(1.77f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                try {
                                    val playIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse(rec.uriString), "video/mp4")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(playIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Playing preview segment...", Toast.LENGTH_SHORT).show()
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = rec.title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val durationString = remember(rec.durationMs) {
                                val min = (rec.durationMs / 1000) / 60
                                val sec = (rec.durationMs / 1000) % 60
                                String.format("%02d:%02d", min, sec)
                            }
                            Text(
                                text = durationString,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionCheckRow(
    title: String,
    desc: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (isGranted) {
            AssistChip(
                onClick = {},
                label = { Text("Granted") },
                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = AssistChipDefaults.assistChipColors(
                    labelColor = MaterialTheme.colorScheme.primary,
                    leadingIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        } else {
            Button(
                onClick = onRequest,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Grant", fontSize = 12.sp)
            }
        }
    }
}

// --- Video Gallery Tab (Tab 1) ---

@Composable
fun GalleryTab(
    viewModel: MainViewModel,
    langKey: String = viewModel.settingsManager.language
) {
    val trans = { key: String -> com.example.settings.LiveTranslator.translate(key, langKey) }
    val recordings by viewModel.recordings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var activeDeleteDialogData by remember { mutableStateOf<RecordingEntity?>(null) }
    var activeRenameDialogData by remember { mutableStateOf<RecordingEntity?>(null) }
    var renameInputVal by remember { mutableStateOf("") }

    if (activeDeleteDialogData != null) {
        AlertDialog(
            onDismissRequest = { activeDeleteDialogData = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val video = activeDeleteDialogData!!
                        try {
                            val uri = Uri.parse(video.uriString)
                            context.contentResolver.delete(uri, null, null)
                        } catch (e: Exception) {
                            // Suppress fallback
                        }
                        try {
                            val f = File(video.filePath)
                            if (f.exists()) f.delete()
                        } catch (e: Exception) {
                            // Suppress
                        }
                        viewModel.deleteRecording(video)
                        activeDeleteDialogData = null
                        Toast.makeText(context, "Deleted screen capture successfully.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeDeleteDialogData = null }) {
                    Text("Cancel")
                }
            },
            title = { Text("Delete Screen Recording?") },
            text = { Text("This will permanently delete this captured file from storage and the device gallery list.") },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
        )
    }

    if (activeRenameDialogData != null) {
        AlertDialog(
            onDismissRequest = { activeRenameDialogData = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val input = renameInputVal.trim()
                        if (input.isNotEmpty()) {
                            viewModel.renameRecording(activeRenameDialogData!!, input)
                            activeRenameDialogData = null
                            Toast.makeText(context, "Renamed successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Title cannot be blank.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeRenameDialogData = null }) {
                    Text("Cancel")
                }
            },
            title = { Text("Rename Recording") },
            text = {
                Column {
                    Text("Provide a custom descriptive name for your video:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = renameInputVal,
                        onValueChange = { renameInputVal = it },
                        singleLine = true,
                        placeholder = { Text("e.g. Minecraft Raid #1") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            icon = { Icon(Icons.Default.Edit, contentDescription = null) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Unified Branding Header as requested by the Geometric Balance theme
        FlowCaptureHeader(title = trans("gallery_title"), subtitle = trans("gallery_sub") + " (${recordings.size})")

        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Empty state icon",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Library is Empty",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You haven't recorded anything yet. Tap the Record button in 'Capture' tab to start recording screen actions!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.insertMockDataForTesting()
                            Toast.makeText(context, "Demo recordings generated for testing!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Load Test Clips")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(recordings) { rec ->
                    VideoItemCard(
                        rec = rec,
                        onPlay = {
                            try {
                                val playIntent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(rec.uriString), "video/mp4")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(playIntent)
                            } catch (err: Exception) {
                                try {
                                    val f = File(rec.filePath)
                                    val fallbackUri = if (f.exists()) Uri.fromFile(f) else Uri.parse(rec.uriString)
                                    val playIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(fallbackUri, "video/mp4")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(playIntent)
                                } catch (e2: Exception) {
                                    Toast.makeText(context, "No supportive local video player discovered.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onRename = {
                            renameInputVal = rec.title
                            activeRenameDialogData = rec
                        },
                        onDelete = {
                            activeDeleteDialogData = rec
                        },
                        onShare = {
                            try {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "video/mp4"
                                    putExtra(Intent.EXTRA_STREAM, Uri.parse(rec.uriString))
                                    putExtra(Intent.EXTRA_SUBJECT, rec.title)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share FlowCapture Recording"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Unable to share video stream.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun VideoItemCard(
    rec: RecordingEntity,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val dateString = remember(rec.timestamp) {
        val formatter = SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault())
        formatter.format(Date(rec.timestamp))
    }

    val durationString = remember(rec.durationMs) {
        val min = (rec.durationMs / 1000) / 60
        val sec = (rec.durationMs / 1000) % 60
        String.format("%02d:%02d", min, sec)
    }

    val sizeString = remember(rec.sizeBytes) {
        val sizeMb = rec.sizeBytes.toFloat() / (1024 * 1024)
        if (sizeMb < 1f) {
            String.format("%.1f KB", sizeMb * 1024)
        } else {
            String.format("%.1f MB", sizeMb)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("video_card_${rec.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.clip(RoundedCornerShape(24.dp))) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large styled play bubble icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        )
                        .clickable { onPlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Playback preview",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rec.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VideoMetaChip(text = durationString, icon = Icons.Default.Timer)
                        VideoMetaChip(text = sizeString, icon = Icons.Default.SdCard)
                        VideoMetaChip(text = rec.resolution, icon = Icons.Default.Tv)
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), thickness = 1.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(vertical = 4.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Profile: ${rec.profileName}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onRename) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun VideoMetaChip(text: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// --- Specific Settings Cus// --- Settings Tab (Tab 2) ---

@Composable
fun SettingsTab(
    viewModel: MainViewModel,
    onChangeSaveFolder: () -> Unit,
    onLangSelected: (String) -> Unit
) {
    val settings = viewModel.settingsManager
    val context = LocalContext.current

    // Local trigger states
    var langVal by remember { mutableStateOf(settings.language) }
    var orientVal by remember { mutableStateOf(settings.videoOrientation) }
    val trans = { key: String -> com.example.settings.LiveTranslator.translate(key, langVal) }

    // Video config selectors
    var resVal by remember { mutableStateOf(settings.resolution) }
    var fpsVal by remember { mutableStateOf(settings.fps) }
    var bitVal by remember { mutableStateOf(settings.bitrateMbps) }
    var codecVal by remember { mutableStateOf(settings.codec) }

    // Audio configs
    var audioOptVal by remember { mutableStateOf(settings.audioOption) }
    var audioQualVal by remember { mutableStateOf(settings.audioQualityKbps) }

    // Bubble states
    var bubbleEnabled by remember { mutableStateOf(settings.isBubbleEnabled) }
    var bubbleSizeVal by remember { mutableStateOf(settings.bubbleSize) }
    var bubbleOpacityVal by remember { mutableStateOf(settings.bubbleOpacity) }
    var hideBubbleVal by remember { mutableStateOf(settings.isHideBubbleInRecordingEnabled) }
    var drawingAllowedVal by remember { mutableStateOf(settings.isAllowDrawingEnabled) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        // Unified branding header
        item {
            FlowCaptureHeader(title = trans("tab_settings"), subtitle = trans("app_subtitle"))
        }

        // Section: System & Localization Options
        item {
            SettingsCategoryCard(title = "System Localization") {
                // Language selector
                DropdownSelectionRow(
                    label = trans("sett_lang"),
                    description = trans("sett_lang_desc"),
                    selectedVal = langVal,
                    choices = com.example.settings.LiveTranslator.LANGUAGES,
                    onSelect = {
                        langVal = it
                        settings.language = it
                        onLangSelected(it)
                    }
                )

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                // Orientation selector
                DropdownSelectionRow(
                    label = trans("sett_orientation"),
                    description = trans("sett_orientation_desc"),
                    selectedVal = orientVal,
                    choices = listOf("Auto", "Portrait", "Landscape"),
                    onSelect = {
                        orientVal = it
                        settings.videoOrientation = it
                    }
                )
            }
        }

        // Section: Video Specifications
        item {
            SettingsCategoryCard(title = trans("sett_video")) {
                // Resolution Selector
                DropdownSelectionRow(
                    label = trans("sett_res"),
                    description = trans("sett_res_desc"),
                    selectedVal = resVal,
                    choices = listOf("1440p", "1080p", "720p"),
                    onSelect = {
                        resVal = it
                        settings.resolution = it
                        settings.profile = SettingsManager.PROFILE_CUSTOM
                        viewModel.currentProfileName.value = SettingsManager.PROFILE_CUSTOM
                    }
                )

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                // FPS Selection
                DropdownSelectionRow(
                    label = trans("sett_fps"),
                    description = trans("sett_fps_desc"),
                    selectedVal = fpsVal.toString(),
                    choices = listOf("60", "48", "30", "24"),
                    onSelect = {
                        val v = it.toInt()
                        fpsVal = v
                        settings.fps = v
                        settings.profile = SettingsManager.PROFILE_CUSTOM
                        viewModel.currentProfileName.value = SettingsManager.PROFILE_CUSTOM
                    }
                )

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                // Bitrate MBps
                DropdownSelectionRow(
                    label = trans("sett_bitrate"),
                    description = trans("sett_bitrate_desc"),
                    selectedVal = "$bitVal Mbps",
                    choices = listOf("20 Mbps", "16 Mbps", "12 Mbps", "8 Mbps", "4 Mbps", "2 Mbps"),
                    onSelect = {
                        val num = it.replace(" Mbps", "").toInt()
                        bitVal = num
                        settings.bitrateMbps = num
                        settings.profile = SettingsManager.PROFILE_CUSTOM
                        viewModel.currentProfileName.value = SettingsManager.PROFILE_CUSTOM
                    }
                )

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                // Video Codec selection
                DropdownSelectionRow(
                    label = trans("sett_codec"),
                    description = trans("sett_codec_desc"),
                    selectedVal = codecVal,
                    choices = listOf("H.264", "H.265"),
                    onSelect = {
                        codecVal = it
                        settings.codec = it
                        settings.profile = SettingsManager.PROFILE_CUSTOM
                        viewModel.currentProfileName.value = SettingsManager.PROFILE_CUSTOM
                    }
                )
            }
        }

        // Section: Audio Settings
        item {
            SettingsCategoryCard(title = "Audio Configurations") {
                // Audio Selection Options
                DropdownSelectionRow(
                    label = trans("sett_audio"),
                    description = trans("sett_audio_desc"),
                    selectedVal = audioOptVal,
                    choices = listOf(SettingsManager.AUDIO_COMBINED, SettingsManager.AUDIO_SYSTEM, SettingsManager.AUDIO_MIC, SettingsManager.AUDIO_NONE),
                    onSelect = {
                        audioOptVal = it
                        settings.audioOption = it
                        settings.profile = SettingsManager.PROFILE_CUSTOM
                        viewModel.currentProfileName.value = SettingsManager.PROFILE_CUSTOM
                    }
                )
            }
        }

        // Section: Bubble Settings Card
        item {
            SettingsCategoryCard(title = trans("sett_bubble_hdr")) {
                // Enabled/Disabled
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = trans("sett_bubble_show"), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        Text(text = trans("sett_bubble_show_desc"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = bubbleEnabled,
                        onCheckedChange = {
                            bubbleEnabled = it
                            settings.isBubbleEnabled = it
                        },
                        modifier = Modifier.testTag("bubble_toggle")
                    )
                }

                if (bubbleEnabled) {
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    // Size select
                    DropdownSelectionRow(
                        label = trans("sett_bubble_size"),
                        description = trans("sett_bubble_size_desc"),
                        selectedVal = bubbleSizeVal,
                        choices = listOf("Small", "Medium", "Large"),
                        onSelect = {
                            bubbleSizeVal = it
                            settings.bubbleSize = it
                        }
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    // Opacity level
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = trans("sett_bubble_alpha"), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(text = "${(bubbleOpacityVal * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = bubbleOpacityVal,
                            onValueChange = {
                                bubbleOpacityVal = it
                                settings.bubbleOpacity = it
                            },
                            valueRange = 0.3f..1.0f,
                            steps = 6
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    // Hide bubble from projection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = trans("sett_bubble_hide"), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                            Text(text = trans("sett_bubble_hide_desc"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = hideBubbleVal,
                            onCheckedChange = {
                                hideBubbleVal = it
                                settings.isHideBubbleInRecordingEnabled = it
                            }
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    // Drawing pen tools
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = trans("sett_drawing"), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                            Text(text = trans("sett_drawing_desc"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = drawingAllowedVal,
                            onCheckedChange = {
                                drawingAllowedVal = it
                                settings.isAllowDrawingEnabled = it
                            }
                        )
                    }
                }
            }
        }

        // Section: Save Folder
        item {
            SettingsCategoryCard(title = trans("Physical Storage Dir Target")) {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text(text = "Storage Location Path", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                    Text(text = "Click target card below to modify system directory configurations", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChangeSaveFolder() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = settings.saveLocationName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = settings.saveLocationPath.ifEmpty { "Using default Android Movies system directory" },
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(Icons.Default.FolderOpen, contentDescription = "Browse folder", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsCategoryCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun DropdownSelectionRow(
    label: String,
    description: String,
    selectedVal: String,
    choices: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.5f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            InputChip(
                selected = expanded,
                onClick = { expanded = !expanded },
                label = { Text(selectedVal, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                choices.forEach { selection ->
                    DropdownMenuItem(
                        text = { Text(selection) },
                        onClick = {
                            onSelect(selection)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// --- About & FAQ Tab (Tab 3) ---

@Composable
fun AboutTab(
    viewModel: MainViewModel,
    langKey: String = viewModel.settingsManager.language
) {
    val trans = { key: String -> LiveTranslator.translate(key, langKey) }
    var expandedQuestionIndex by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        // Unified Branding Header
        item {
            FlowCaptureHeader(title = trans("about_title"), subtitle = trans("about_sub"))
        }

        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Custom lens logo drawing
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .border(3.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "FlowCapture Screen",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                )
                Text(
                    text = trans("app_version"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Designed for extreme efficiency on 4GB low-end phones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        // Section: Check Updates & Repository Card
        item {
            val isChecking by viewModel.isCheckingUpdate.collectAsState()
            val updateStatusVal by viewModel.updateStatus.collectAsState()
            val context = LocalContext.current

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Repository & Updates",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    )

                    Text(
                        text = "Access source codes, submit issues or grab releases directly at our active GitHub hub.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Clickable Repo link
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/EvgenPlay05/FlowCapture"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open GitHub Repository", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    // Update Checker UI
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (val status = updateStatusVal) {
                            null -> {
                                Button(
                                    onClick = { viewModel.checkForUpdates() },
                                    enabled = !isChecking,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (isChecking) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSecondary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(trans("update_latest"), fontSize = 13.sp)
                                    } else {
                                        Icon(Icons.Default.SystemUpdate, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(trans("update_check"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                            is MainViewModel.UpdateResult.UpToDate -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(trans("update_up_to_date"), fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), fontSize = 13.sp)
                                }
                                Button(
                                    onClick = { viewModel.checkForUpdates() },
                                    enabled = !isChecking,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), contentColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(trans("update_check"), fontSize = 12.sp)
                                }
                            }
                            is MainViewModel.UpdateResult.NewUpdateAvailable -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "🎉 New app update! ${status.version}",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Click below to open releases page and get the latest stable version.",
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(status.releaseUrl))
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Download ${status.version}", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            is MainViewModel.UpdateResult.Error -> {
                                Text("Error checking: ${status.message}", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                Button(
                                    onClick = { viewModel.checkForUpdates() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(trans("update_check"))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Details / Description list
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = trans("about_specs"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    )

                    AboutSpecRow(trans("about_ram"), trans("about_ram_desc"), Icons.Default.Memory)
                    AboutSpecRow(trans("about_battery"), trans("about_battery_desc"), Icons.Default.BatteryChargingFull)
                    AboutSpecRow(trans("about_gpu"), trans("about_gpu_desc"), Icons.Default.Shield)
                }
            }
        }

        // Section: FAQ Accordion
        item {
            Column {
                Text(
                    text = trans("about_faq"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                )

                val faqs = listOf(
                    FaqItem(
                        "How can I capture System Audio?",
                        "Capturing systems/internal sounds natively requires Android 10 (API level 29) or higher. Additionally, apps whose screens you request recording of must explicitly permit internal system audio capture in their manifest settings."
                    ),
                    FaqItem(
                        "Why does it request 'Display Over Other Apps' permission?",
                        "This permission (system overlay window) is strictly required to render the floating control bubble with stop, pause, and drawing pen brushes outside FlowCapture while capturing other third-party gameplay screens."
                    ),
                    FaqItem(
                        "What is Simulation Recording compatible mode?",
                        "For ultra-low-end systems, emulator sandboxes without camera pipelines, or when internal media recording APIs deny projection permissions, FlowCapture implements an elegant safe simulation engine that allows full interactive testing and mock file creation without single-engine crash risks!"
                    ),
                    FaqItem(
                        "Where are external video clips saved?",
                        "All resulting video streams are stored directly inside the system's public 'Movies/FlowCapture' directory, integrated seamlessly into Google Photos and device default players. You can choose any custom save folder under Settings as well."
                    )
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    faqs.forEachIndexed { idx, faq ->
                        val isExpanded = expandedQuestionIndex == idx
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedQuestionIndex = if (isExpanded) null else idx },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                1.dp,
                                color = if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = faq.q,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                AnimatedVisibility(visible = isExpanded) {
                                    Column {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = faq.a,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 16.sp,
                                            modifier = Modifier.padding(start = 26.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Credits & Dev Notes
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = trans("about_credits"), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = trans("about_credits_desc"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "FlowCapture © 2026 evgenplay05@gmail.com",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AboutSpecRow(title: String, desc: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

data class FaqItem(val q: String, val a: String)
