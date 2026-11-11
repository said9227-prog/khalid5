package com.example.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.StudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraInspectorPanel(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val isArabic by viewModel.isArabic.collectAsState()
    val isPreviewing by viewModel.isPreviewingCamera.collectAsState()
    val isPreviewComplete by viewModel.isCameraPreviewComplete.collectAsState()
    val isRecording by viewModel.isRecordingCamera.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val sequence by viewModel.cameraSequence.collectAsState()
    val isCameraPathVisible by viewModel.isCameraPathVisible.collectAsState()

    val progress by viewModel.exportProgress.collectAsState()
    val exportedPath by viewModel.exportedFilePath.collectAsState()
    val selectedFormat by viewModel.exportFormat.collectAsState()
    val selectedRes by viewModel.exportResolution.collectAsState()

    var selectedAddPreset by remember { mutableStateOf("top") }
    var selectedAddDuration by remember { mutableStateOf(4.0f) }

    // Custom Waypoint Start & End Point Editor State
    var startX by remember { mutableStateOf(0.0f) }
    var startY by remember { mutableStateOf(1.8f) }
    var startZ by remember { mutableStateOf(9.5f) }

    var endX by remember { mutableStateOf(0.0f) }
    var endY by remember { mutableStateOf(0.5f) }
    var endZ by remember { mutableStateOf(4.0f) }
    var customWayDuration by remember { mutableStateOf(5.0f) }

    val presetNames = remember {
        mapOf(
            "top" to Pair("كاميرا علوية High Top", "Top High Angle"),
            "bottom" to Pair("كاميرا سفلية Low Bottom", "Bottom Low Angle"),
            "left_side" to Pair("جانبية يسار Left Side", "Left Side Angle"),
            "right_side" to Pair("جانبية يمين Right Side", "Right Side Angle"),
            "close_up" to Pair("لقطة قريبة Close-up", "Close-up Shot"),
            "hero_wide" to Pair("لقطة واسعة Hero Wide", "Hero Wide Shot"),
            "orbit360" to Pair("دوران كامل 360°", "360 Orbit Shot"),
            "descent" to Pair("هبوط وتنزيل كاميرا", "Camera Descent"),
            "zoomOut" to Pair("تراجع وتكبير للخارج", "Zoom Out"),
            "zoomIn" to Pair("اقتراب ودخول للداخل", "Zoom In"),
            "droneFlyby" to Pair("تحليق طائرة درون", "Drone Flyby"),
            "craneReveal" to Pair("كشف رافعة Crane", "Crane Reveal")
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Title & Direct Video Export Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isArabic) "مخرج وإخراج حركات الكاميرات المتعددة 3D" else "Multi-Camera Director & Choreographer",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFFFD700)
                )
                val totalDurationSec = sequence.sumOf { it.durationSec.toDouble() }.toFloat()
                Text(
                    text = if (isArabic) "إجمالي زمن المشهد: ${totalDurationSec.toInt()}ث | أداء 60fps بدون تجميد"
                    else "Total Duration: ${totalDurationSec.toInt()}s | Smooth 60fps",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF10B981)
                )
            }
        }

        // Dedicated Video Preview & Export Card (زر تصدير الفيديو في نافذة الكاميرا فقط مع زر معاينة مسبق)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF27272A)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isArabic) "معاينة وتسجيل وتصدير فيديو المشهد" else "Preview & Export Scene Video",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFFFD700)
                        )
                        Text(
                            text = if (isArabic) "دقة التصدير المحددة: $selectedRes | الصيغة: ${selectedFormat.uppercase()}"
                            else "Quality: $selectedRes | Format: ${selectedFormat.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF38BDF8)
                        )
                    }
                    if (isPreviewing) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF0369A1)
                        ) {
                            Text(
                                text = if (isArabic) "جاري المعاينة..." else "Previewing...",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Step 1: Preview Camera Movement Section (زر معاينة قبل التصدير والتسجيل مع زر إيقاف فوري لحظي)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isPreviewing) {
                        Button(
                            onClick = { viewModel.stopCameraPreview() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_stop_camera_preview")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "⏹ إيقاف فوري الآن" else "⏹ Instant Stop",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    } else {
                        Button(
                            onClick = { viewModel.previewCameraSequence() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F3F46)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_preview_camera"),
                            enabled = !isRecording && !isExporting && sequence.isNotEmpty()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFFFFD700))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "1. معاينة حركة الكاميرا" else "1. Preview Camera Motion",
                                color = Color(0xFFFFD700)
                            )
                        }
                    }

                    // Step 2: Export Video Button (فقط في نافذة الكاميرا)
                    Button(
                        onClick = { viewModel.recordCameraSequenceAndExport() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPreviewComplete) Color(0xFF059669) else Color(0xFFDC2626)
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("btn_record_camera_export"),
                        enabled = !isRecording && !isExporting && !isPreviewing
                    ) {
                        Icon(Icons.Default.Movie, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "2. تصدير الفيديو بعد المعاينة" else "2. Export Video After Preview",
                            color = Color.White
                        )
                    }
                }

                // Quick Tip for Immediate Stop by Touching Screen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "يمكنك أيضاً لمس الشاشة ثلاثية الأبعاد في أي لحظة لإيقاف الحركة فوراً."
                        else "You can also tap the 3D screen at any moment to halt motion immediately.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Preview Complete Success Banner
                if (isPreviewComplete && !isPreviewing && !isExporting) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF064E3B).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "✅ تمت المعاينة بنجاح فائق! تم فحص النص العربي والإنجليزي بدقة بدون أي تقطع أو خطوط."
                            else "Preview completed smoothly! Arabic and English text rendering verified with zero tearing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF34D399)
                        )
                    }
                }

                // Export Progress Display (with instant cancel option)
                if (isExporting) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) "جاري رندر وتصدير الفيديو: $progress%" else "Rendering Video: $progress%",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFD700)
                            )
                            Button(
                                onClick = { viewModel.emergencyStopAll() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "إلغاء فوري" else "Cancel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                        LinearProgressIndicator(
                            progress = progress / 100f,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFFFD700),
                            trackColor = Color(0xFF3F3F46)
                        )
                    }
                }

                // Export Complete Saved Banner
                if (!exportedPath.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF132A1C), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isArabic) "تم تصدير الفيديو بنجاح وحفظه في المعرض!" else "Video exported and saved to Gallery!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF10B981)
                            )
                            Text(
                                text = exportedPath.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // 3D Path Curve Visibility Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Timeline, contentDescription = null, tint = Color(0xFF38BDF8))
                Column {
                    Text(
                        text = if (isArabic) "رسم وإظهار مسار الكاميرا ثلاثي الأبعاد (3D Path Curve)" else "Draw Visual 3D Camera Path",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (isArabic) "يتم إخفاء مسار الكاميرا واللمبة تلقائياً عند بدء تسجيل الفيديو" else "Path line & light bulb auto-hide during video export",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Switch(
                checked = isCameraPathVisible,
                onCheckedChange = { viewModel.toggleCameraPathCurve(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF38BDF8),
                    checkedTrackColor = Color(0xFF3F3F46)
                ),
                modifier = Modifier.testTag("switch_camera_path")
            )
        }

        Divider(color = Color(0xFF27272A))

        Text(
            text = if (isArabic) "قوالب تسلسل حركات الكاميرا المضافة (${sequence.size})" else "Active Shot Sequence (${sequence.size})",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White
        )

        // Active Sequence List Chips with Remove Option
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(sequence) { index, step ->
                val name = if (step.preset == "custom_waypoint") {
                    if (isArabic) "نقطة بداية ونهاية مخصصة" else "Custom Waypoint"
                } else {
                    presetNames[step.preset]?.let { if (isArabic) it.first else it.second } ?: step.preset
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.border(1.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}. $name (${step.durationSec.toInt()}ث)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { viewModel.removeCameraSequenceStep(step.id) }
                                .testTag("btn_remove_step_${step.id}")
                        )
                    }
                }
            }
        }

        // Custom Camera Start & End Point Editor (تحديد نقطة بداية كاميرا ونهايتها)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF27272A)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (isArabic) "تحديد نقطة بداية الكاميرا ونهايتها (Custom Waypoint)" else "Custom Camera Start & End Point Waypoint",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFFFFD700)
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Start Position XYZ
                Text(
                    text = if (isArabic) "نقطة بداية الكاميرا (X: $startX, Y: $startY, Z: $startZ)" else "Camera Start Position (X: $startX, Y: $startY, Z: $startZ)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF38BDF8)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start X", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Slider(value = startX, onValueChange = { startX = it }, valueRange = -15f..15f)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start Y", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Slider(value = startY, onValueChange = { startY = it }, valueRange = -5f..20f)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start Z", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Slider(value = startZ, onValueChange = { startZ = it }, valueRange = -10f..30f)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // End Position XYZ
                Text(
                    text = if (isArabic) "نقطة نهاية الكاميرا (X: $endX, Y: $endY, Z: $endZ)" else "Camera End Position (X: $endX, Y: $endY, Z: $endZ)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF10B981)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("End X", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Slider(value = endX, onValueChange = { endX = it }, valueRange = -15f..15f)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("End Y", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Slider(value = endY, onValueChange = { endY = it }, valueRange = -5f..20f)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("End Z", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Slider(value = endZ, onValueChange = { endZ = it }, valueRange = -10f..30f)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        viewModel.addCustomWaypointStep(
                            durationSec = customWayDuration,
                            startX = startX,
                            startY = startY,
                            startZ = startZ,
                            endX = endX,
                            endY = endY,
                            endZ = endZ
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_add_waypoint")
                ) {
                    Icon(Icons.Default.AddLocation, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isArabic) "إضافة قالب مسار مخصص (من نقطة البداية للنهاية)" else "Add Custom Waypoint Motion Step",
                        color = Color.Black
                    )
                }
            }
        }

        // Preset Camera Motion Templates Selection
        Text(
            text = if (isArabic) "إضافة قوالب زوايا كاميرا جاهزة:" else "Add Preset Motion Template:",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(presetNames.toList()) { p ->
                val isSel = selectedAddPreset == p.first
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSel) Color(0xFFFFD700) else Color(0xFF18181B))
                        .border(1.dp, if (isSel) Color(0xFFFFD700) else Color(0xFF3F3F46), RoundedCornerShape(6.dp))
                        .clickable {
                            selectedAddPreset = p.first
                            viewModel.applyCameraPreset(p.first)
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("preset_cam_${p.first}")
                ) {
                    Text(
                        text = if (isArabic) p.second.first else p.second.second,
                        color = if (isSel) Color.Black else Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Transition Duration Selection & Add Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isArabic) "مدة الحركة:" else "Duration:",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(listOf(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 10.0f, 15.0f, 30.0f)) { dur ->
                    val isSel = selectedAddDuration == dur
                    val label = "${dur.toInt()}ث"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) Color(0xFFFFD700) else Color(0xFF27272A))
                            .clickable { selectedAddDuration = dur }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(label, color = if (isSel) Color.Black else Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Button(
                onClick = { viewModel.addCameraSequenceStep(selectedAddPreset, selectedAddDuration) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_add_preset_step")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isArabic) "إضافة القالب" else "Add Template", color = Color.Black, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
