package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import com.example.bridge.WebAppInterface
import com.example.ui.components.StudioWebView
import com.example.ui.panels.*

data class StudioTabItem(
    val tab: StudioTab,
    val nameEn: String,
    val nameAr: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainStudioScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isArabic by viewModel.isArabic.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()

    val webAppInterface = remember {
        WebAppInterface(
            context = context,
            onProgress = { viewModel.onExportProgress(it) },
            onComplete = { viewModel.onExportComplete(it) },
            onTimelineUpdate = { viewModel.seekTimeline(it) },
            onCameraState = { status, count -> viewModel.onCameraRecordingState(status, count) }
        ).apply {
            onObjectSelectedListener = { id, x, y, z ->
                viewModel.selectAsset(id)
                viewModel.assetPosX.value = x
                viewModel.assetPosY.value = y
                viewModel.assetPosZ.value = z
            }
            onSurfacePaintUpdateListener = { count ->
                viewModel.totalPaintedCount.value = count
            }
            onFpsUpdateListener = { fps ->
                viewModel.onFpsUpdate(fps)
            }
            onModelImportedListener = { id, name, format, brushId ->
                viewModel.importModelStatus.value = if (viewModel.isArabic.value) "تم تجهيز النموذج $name بنجاح" else "Model $name ready"
            }
            onGreenScreenVideoStateListener = { cur, dur, playing ->
                viewModel.onGreenScreenVideoState(cur, dur, playing)
            }
        }
    }

    val tabs = remember {
        listOf(
            StudioTabItem(StudioTab.TEXT, "Text", "النص", Icons.Default.TextFields),
            StudioTabItem(StudioTab.TERRAIN, "3D Terrain", "البيئة والتضاريس 3D", Icons.Default.Terrain),
            StudioTabItem(StudioTab.SURFACE_PAINT, "3D Paint & Models", "الرسم ونماذج 3D", Icons.Default.Brush),
            StudioTabItem(StudioTab.GREEN_SCREEN, "Green Screen 3D", "كروما كي 3D", Icons.Default.MovieFilter),
            StudioTabItem(StudioTab.MATERIALS, "Materials", "المواد", Icons.Default.Palette),
            StudioTabItem(StudioTab.LIGHTING, "Lighting", "الإضاءة", Icons.Default.WbSunny),
            StudioTabItem(StudioTab.CAMERA, "Camera", "الكاميرا", Icons.Default.Videocam),
            StudioTabItem(StudioTab.ANIMATION, "Animation", "التحريك", Icons.Default.Movie),
            StudioTabItem(StudioTab.AUDIO, "Audio", "الصوت", Icons.Default.MusicNote),
            StudioTabItem(StudioTab.EXPORT, "Export", "التصدير", Icons.Default.IosShare),
            StudioTabItem(StudioTab.PROJECTS, "Projects", "المشاريع", Icons.Default.Folder)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SHOAIB 3D (شعيب 3D)",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFFFD700)
                        )
                        Text(
                            text = if (isArabic) "استوديو النصوص والسينما ثلاثي الأبعاد" else "Cinematic 3D Typography Studio",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    val batteryTemp by viewModel.batteryTempCelsius.collectAsState()
                    val cpuThermalStatus by viewModel.cpuThermalStatus.collectAsState()
                    val isThermalVisible by viewModel.isRamOverlayVisible.collectAsState()
                    val thermalBadgeColor = if (batteryTemp >= 43f || cpuThermalStatus == "SEVERE" || cpuThermalStatus == "CRITICAL") {
                        Color(0xFFEF4444)
                    } else if (batteryTemp >= 39f || cpuThermalStatus == "MODERATE") {
                        Color(0xFFF59E0B)
                    } else {
                        Color(0xFF10B981)
                    }

                    IconButton(
                        onClick = { viewModel.toggleRamOverlay(!isThermalVisible) },
                        modifier = Modifier.testTag("btn_top_thermal_hub")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Thermostat,
                            contentDescription = "Thermal & Performance Hub",
                            tint = if (isThermalVisible) thermalBadgeColor else Color.Gray
                        )
                    }

                    TextButton(
                        onClick = { viewModel.toggleLanguage() },
                        modifier = Modifier.testTag("btn_toggle_lang")
                    ) {
                        Text(
                            text = if (isArabic) "EN" else "العربية",
                            color = Color(0xFFFFD700),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121214))
            )
        },
        containerColor = Color(0xFF121214),
        modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 3D WebGL Viewport Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.1f)
                    .background(Color(0xFF121214))
            ) {
                StudioWebView(
                    modifier = Modifier.fillMaxSize(),
                    webAppInterface = webAppInterface,
                    onWebViewCreated = { webView ->
                        viewModel.webViewRef = webView
                    }
                )

                // Floating Real-Time Device Thermal & Performance Monitoring System (لوحة مراقبة حرارة الهاتف والأداء)
                val ramUsageMb by viewModel.ramUsageMb.collectAsState()
                val ramTotalMb by viewModel.ramTotalMb.collectAsState()
                val ramPercent by viewModel.ramPercent.collectAsState()
                val isRamOverlayVisible by viewModel.isRamOverlayVisible.collectAsState()
                val batteryTemp by viewModel.batteryTempCelsius.collectAsState()
                val cpuThermalStatus by viewModel.cpuThermalStatus.collectAsState()
                val fpsVal by viewModel.fpsVal.collectAsState()
                val systemHealth by viewModel.systemThermalHealth.collectAsState()
                val isSmartShield by viewModel.isSmartThermalProtectionEnabled.collectAsState()
                val isExpanded by viewModel.isThermalPanelExpanded.collectAsState()

                val tempStatusColor = when {
                    batteryTemp >= 43.0f || cpuThermalStatus in listOf("SEVERE", "CRITICAL", "EMERGENCY") -> Color(0xFFF87171)
                    batteryTemp >= 38.0f || cpuThermalStatus in listOf("LIGHT", "MODERATE") -> Color(0xFFFBBF24)
                    else -> Color(0xFF4ADE80)
                }

                if (isRamOverlayVisible) {
                    if (!isExpanded) {
                        // Compact HUD Chip
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { viewModel.toggleThermalPanelExpanded(true) }
                                .testTag("overlay_thermal_hud_chip"),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xEE18181B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, tempStatusColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Battery Temp
                                Icon(Icons.Default.Thermostat, contentDescription = null, tint = tempStatusColor, modifier = Modifier.size(15.dp))
                                Text(
                                    text = String.format(Locale.US, "%.1f°C", batteryTemp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tempStatusColor,
                                    fontWeight = FontWeight.Bold
                                )

                                Text("•", color = Color.DarkGray, style = MaterialTheme.typography.labelSmall)

                                // CPU status
                                Text(
                                    text = "CPU: $cpuThermalStatus",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (cpuThermalStatus == "NORMAL") Color(0xFFE4E4E7) else tempStatusColor
                                )

                                Text("•", color = Color.DarkGray, style = MaterialTheme.typography.labelSmall)

                                // FPS
                                Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(15.dp))
                                Text(
                                    text = "$fpsVal FPS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )

                                Text("•", color = Color.DarkGray, style = MaterialTheme.typography.labelSmall)

                                // RAM
                                Icon(Icons.Default.Memory, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(15.dp))
                                Text(
                                    text = "${ramUsageMb}MB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF38BDF8)
                                )

                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = "Expand",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        // Expanded Thermal & Performance Hub Card
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .widthIn(max = 380.dp)
                                .testTag("overlay_thermal_panel_expanded"),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xF518181C),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, tempStatusColor),
                            shadowElevation = 8.dp
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Thermostat, contentDescription = null, tint = tempStatusColor, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isArabic) "مراقبة حرارة وأداء الهاتف" else "Thermal & Performance Hub",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.toggleThermalPanelExpanded(false) },
                                        modifier = Modifier.size(28.dp).testTag("btn_close_thermal_panel")
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Metrics Grid 2x2
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF27272A)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = if (isArabic) "🌡️ حرارة البطارية" else "🌡️ Battery Temp",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = String.format(Locale.US, "%.1f °C", batteryTemp),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = tempStatusColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF27272A)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = if (isArabic) "🔥 حالة المعالج" else "🔥 CPU Thermal",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = cpuThermalStatus,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = tempStatusColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF27272A)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = if (isArabic) "🎮 سلاسة الرسوميات" else "🎮 Viewport FPS",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = "$fpsVal FPS",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Color(0xFFFFD700),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF27272A)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = if (isArabic) "🧠 الذاكرة RAM" else "🧠 System RAM",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = "${ramUsageMb}MB ($ramPercent%)",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Color(0xFF38BDF8),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Health status badge
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF27272A))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isArabic) "حالة الاستقرار: " else "System Status: ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.LightGray
                                    )
                                    Text(
                                        text = systemHealth,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tempStatusColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Smart Thermal Protection switch
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isArabic) "نظام الحماية الذكي التلقائي" else "Smart Thermal Shield",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (isArabic) "يخفض الظلال والمؤثرات تلقائياً عند السخونة" else "Auto reduces shadows & effects when warm",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                    Switch(
                                        checked = isSmartShield,
                                        onCheckedChange = { viewModel.toggleSmartThermalProtection(it) },
                                        modifier = Modifier.testTag("switch_smart_thermal")
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.triggerInstantCoolDown() },
                                        modifier = Modifier.weight(1f).testTag("btn_instant_cool_down"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 6.dp)
                                    ) {
                                        Icon(Icons.Default.AcUnit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isArabic) "تبريد فوري ❄️" else "Cool Down ❄️",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.resetBalancedThermal() },
                                        modifier = Modifier.weight(1f).testTag("btn_reset_balanced_thermal"),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isArabic) "إعادة التوازن ⚖️" else "Balanced ⚖️",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.LightGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Viewport Overlay Controls
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.toggleRamOverlay(!isRamOverlayVisible) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xCC1E1E22))
                            .testTag("btn_toggle_ram")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Thermostat,
                            contentDescription = null,
                            tint = if (isRamOverlayVisible) tempStatusColor else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.applyCameraPreset("orbit360") },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xCC1E1E22))
                    ) {
                        Icon(Icons.Default.Cameraswitch, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Studio Tool Selection Tab Bar
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF18181B))
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tabs) { tabItem ->
                    val isSelected = activeTab == tabItem.tab
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFFFFD700) else Color(0xFF27272A))
                            .clickable { viewModel.activeTab.value = tabItem.tab }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("tab_${tabItem.tab.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = tabItem.icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) tabItem.nameAr else tabItem.nameEn,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) Color.Black else Color.White
                        )
                    }
                }
            }

            // Bottom Inspector Content Area (Vertically Scrollable with Dynamic Weight)
            val panelScrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.9f)
                    .background(Color(0xFF1E1E22))
                    .verticalScroll(panelScrollState)
                    .padding(bottom = 24.dp)
            ) {
                when (activeTab) {
                    StudioTab.TEXT -> TextInspectorPanel(viewModel)
                    StudioTab.TERRAIN -> TerrainStudioInspectorPanel(viewModel)
                    StudioTab.SURFACE_PAINT -> SurfacePaintInspectorPanel(viewModel)
                    StudioTab.GREEN_SCREEN -> ChromaKeyInspectorPanel(viewModel)
                    StudioTab.MATERIALS -> MaterialsInspectorPanel(viewModel)
                    StudioTab.LIGHTING -> LightingInspectorPanel(viewModel)
                    StudioTab.CAMERA -> CameraInspectorPanel(viewModel)
                    StudioTab.ANIMATION -> TimelineInspectorPanel(viewModel)
                    StudioTab.AUDIO -> AudioInspectorPanel(viewModel)
                    StudioTab.EXPORT -> ExportInspectorPanel(viewModel)
                    StudioTab.PROJECTS -> ProjectsInspectorPanel(viewModel)
                }
            }
        }
    }
}
