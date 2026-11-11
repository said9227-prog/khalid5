package com.example.ui.panels

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.StudioViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChromaKeyInspectorPanel(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isArabic by viewModel.isArabic.collectAsState()

    // Green Screen States
    val isVideoLoaded by viewModel.isGreenScreenVideoLoaded.collectAsState()
    val videoName by viewModel.greenScreenVideoName.collectAsState()
    val duration by viewModel.greenScreenDuration.collectAsState()
    val currentTime by viewModel.greenScreenCurrentTime.collectAsState()
    val isPlaying by viewModel.isGreenScreenPlaying.collectAsState()
    val playSpeed by viewModel.greenScreenPlaybackSpeed.collectAsState()
    val isLooping by viewModel.isGreenScreenLooping.collectAsState()
    val statusMsg by viewModel.greenScreenStatusMessage.collectAsState()

    // Chroma Key Shader States
    val keyColor by viewModel.chromaKeyColor.collectAsState()
    val similarity by viewModel.chromaSimilarity.collectAsState()
    val smoothness by viewModel.chromaSmoothness.collectAsState()
    val spill by viewModel.chromaSpill.collectAsState()
    val opacity by viewModel.chromaOpacity.collectAsState()
    val envTint by viewModel.chromaEnvTint.collectAsState()

    // Actor 3D Transform States
    val actorPosX by viewModel.actorPosX.collectAsState()
    val actorPosY by viewModel.actorPosY.collectAsState()
    val actorPosZ by viewModel.actorPosZ.collectAsState()
    val actorScale by viewModel.actorScale.collectAsState()
    val actorRotY by viewModel.actorRotY.collectAsState()
    val isGroundLocked by viewModel.isGroundLocked.collectAsState()
    val hasContactShadow by viewModel.hasContactShadow.collectAsState()
    val isBillboard by viewModel.isBillboardEnabled.collectAsState()

    // Video File Picker Launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val contentResolver = context.contentResolver
                var fileName = "actor_greenscreen.mp4"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIdx >= 0) {
                        fileName = cursor.getString(nameIdx) ?: fileName
                    }
                }

                val ext = if (fileName.endsWith(".webm", ignoreCase = true)) "webm" else "mp4"
                val cacheFile = File(context.cacheDir, "greenscreen_${System.currentTimeMillis()}.$ext")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val fileUrl = "file://" + cacheFile.absolutePath
                viewModel.importGreenScreenVideo(fileName, fileUrl)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MovieFilter, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isArabic) "Hollywood Green Screen 3D Composer" else "Hollywood Green Screen 3D Composer",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF4ADE80),
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF22C55E).copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22C55E))
            ) {
                Text(
                    text = "GLSL GPU 4K",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4ADE80),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Text(
            text = if (isArabic) "دمج فيديو حقيقي أمام كروما خضراء مباشرة داخل عوالم 3D مع إزالة اللون الأخضر بـ GLSL Shader وظلال واقعية أسفل القدمين"
            else "Composite real green screen actor video into 3D virtual worlds with GPU GLSL Chroma Key & contact shadows",
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray
        )

        if (statusMsg.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF14532D).copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22C55E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "✨ $statusMsg",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF86EFAC),
                    modifier = Modifier.padding(8.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // ==========================================
        // 1. VIDEO INPUT & PRESETS SECTION
        // ==========================================
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF18181D),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22C55E)),
            modifier = Modifier.fillMaxWidth().testTag("card_greenscreen_input")
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isArabic) "1. إدخال الفيديو والممثلين (Video Input)" else "1. Video Input & Actor Clips",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { videoPickerLauncher.launch("video/*") },
                        modifier = Modifier.weight(1.2f).testTag("btn_import_greenscreen_video"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabic) "استيراد فيديو من الهاتف" else "Import Video", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }

                    Button(
                        onClick = { viewModel.loadSampleGreenScreenActor("action_hero") },
                        modifier = Modifier.weight(1.0f).testTag("btn_sample_greenscreen_actor"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isArabic) "ممثل هوليوود جاهز" else "Sample Actor", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                }

                // Sample Actor Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Pair("action_hero", if (isArabic) "🎬 بطل أكشن" else "🎬 Action Hero"),
                        Pair("news_anchor", if (isArabic) "🎙️ مقدم برامج" else "🎙️ Presenter"),
                        Pair("performer", if (isArabic) "🕺 فنان مسرحي" else "🕺 Performer")
                    ).forEach { p ->
                        OutlinedButton(
                            onClick = { viewModel.loadSampleGreenScreenActor(p.first) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(p.second, style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                        }
                    }
                }

                // Video Player & Scrubber Timeline
                if (isVideoLoaded) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(videoName.take(24), style = MaterialTheme.typography.bodySmall, color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold)
                            Text(
                                String.format(Locale.US, "%02d:%02d / %02d:%02d",
                                    (currentTime / 60).toInt(), (currentTime % 60).toInt(),
                                    (duration / 60).toInt(), (duration % 60).toInt()
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }

                        // Scrubber Slider
                        Slider(
                            value = currentTime.coerceIn(0f, duration.coerceAtLeast(1f)),
                            onValueChange = { viewModel.seekGreenScreenVideo(it) },
                            valueRange = 0f..duration.coerceAtLeast(1f),
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF22C55E), activeTrackColor = Color(0xFF22C55E)),
                            modifier = Modifier.height(28.dp).testTag("slider_greenscreen_scrub")
                        )

                        // Playback Controls Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.toggleGreenScreenPlayPause() },
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF27272A))
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color(0xFF22C55E)
                                    )
                                }

                                // Playback Speed Chips
                                listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { spd ->
                                    val isSpdSel = playSpeed == spd
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isSpdSel) Color(0xFF22C55E) else Color(0xFF27272A))
                                            .clickable { viewModel.setGreenScreenSpeed(spd) }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${spd}x",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSpdSel) Color.Black else Color.LightGray,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Loop Toggle
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (isArabic) "تكرار 🔁" else "Loop 🔁", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Spacer(modifier = Modifier.width(4.dp))
                                Switch(
                                    checked = isLooping,
                                    onCheckedChange = { viewModel.toggleGreenScreenLoop(it) },
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. GLSL CHROMA KEY TUNING SECTION
        // ==========================================
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF18181D),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3F3F46)),
            modifier = Modifier.fillMaxWidth().testTag("card_chroma_key_shader")
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isArabic) "2. إعدادات معالج الكروما GLSL Shader" else "2. GLSL Chroma Key GPU Shader Controls",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                // Key Color Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isArabic) "لون الخلفية:" else "Key Color:", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)

                    listOf(
                        Pair("#00FF00", if (isArabic) "🟢 أخضر هوليوود" else "🟢 Green Screen"),
                        Pair("#0000FF", if (isArabic) "🔵 أزرق استوديو" else "🔵 Blue Screen")
                    ).forEach { (colorHex, label) ->
                        val isSel = keyColor == colorHex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) Color(0xFF27272A) else Color(0xFF141416))
                                .border(
                                    width = if (isSel) 2.dp else 1.dp,
                                    color = if (isSel) Color(0xFF22C55E) else Color(0xFF3F3F46),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { viewModel.updateChromaKeySettings(color = colorHex) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 1. Color Similarity / Tolerance Slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isArabic) "حساسية وتطابق اللون (Tolerance / Similarity)" else "Color Tolerance / Similarity", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                        Text(String.format(Locale.US, "%.2f", similarity), style = MaterialTheme.typography.bodySmall, color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = similarity,
                        onValueChange = { viewModel.updateChromaKeySettings(similarity = it) },
                        valueRange = 0.15f..0.75f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF22C55E), activeTrackColor = Color(0xFF22C55E))
                    )
                }

                // 2. Edge Smoothness Slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isArabic) "نعومة وتلاشي الحواف (Edge Smoothness)" else "Edge Smoothness", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                        Text(String.format(Locale.US, "%.2f", smoothness), style = MaterialTheme.typography.bodySmall, color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = smoothness,
                        onValueChange = { viewModel.updateChromaKeySettings(smoothness = it) },
                        valueRange = 0.01f..0.30f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF22C55E), activeTrackColor = Color(0xFF22C55E))
                    )
                }

                // 3. Spill Suppression (إزالة الانعكاس الأخضر من الشعر والملابس)
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isArabic) "إزالة الانعكاس الأخضر من الشعر والملابس (Spill Suppression)" else "Spill Suppression (Hair & Edges)", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                        Text(String.format(Locale.US, "%.2f", spill), style = MaterialTheme.typography.bodySmall, color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = spill,
                        onValueChange = { viewModel.updateChromaKeySettings(spill = it) },
                        valueRange = 0.0f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF22C55E), activeTrackColor = Color(0xFF22C55E))
                    )
                }

                // 4. Overall Opacity
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isArabic) "شفافية الممثل (Actor Transparency)" else "Actor Opacity", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                        Text(String.format(Locale.US, "%.2f", opacity), style = MaterialTheme.typography.bodySmall, color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = opacity,
                        onValueChange = { viewModel.updateChromaKeySettings(opacity = it) },
                        valueRange = 0.2f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF22C55E), activeTrackColor = Color(0xFF22C55E))
                    )
                }
            }
        }

        // ==========================================
        // 3. ACTOR 3D WORLD PLACEMENT & GROUNDING
        // ==========================================
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF18181D),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3F3F46)),
            modifier = Modifier.fillMaxWidth().testTag("card_actor_3d_transform")
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isArabic) "3. تثبيت وتحريك الممثل داخل العالم ثلاثي الأبعاد" else "3. 3D World Positioning & Grounding",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                // Actor Scale
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isArabic) "مقياس وحجم الممثل (Actor Scale)" else "Actor Scale", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                        Text(String.format(Locale.US, "%.2fx", actorScale), style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = actorScale,
                        onValueChange = { viewModel.updateActor3DTransform(scale = it) },
                        valueRange = 0.4f..2.5f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                    )
                }

                // Horizontal Pos X (يمين / يسار)
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isArabic) "الموضع الأفقي X (يمين / يسار)" else "Horizontal Position X", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                        Text(String.format(Locale.US, "%.1fm", actorPosX), style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = actorPosX,
                        onValueChange = { viewModel.updateActor3DTransform(posX = it) },
                        valueRange = -6.0f..6.0f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                    )
                }

                // Depth Pos Z (العمق)
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isArabic) "العمق في المشهد Z (قريب / بعيد)" else "Depth Position Z", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                        Text(String.format(Locale.US, "%.1fm", actorPosZ), style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = actorPosZ,
                        onValueChange = { viewModel.updateActor3DTransform(posZ = it) },
                        valueRange = -8.0f..8.0f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                    )
                }

                // Rotation Y
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isArabic) "تدوير الممثل (Rotation Y)" else "Actor Rotation Y", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                        Text("${actorRotY.toInt()}°", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = actorRotY,
                        onValueChange = { viewModel.updateActor3DTransform(rotY = it) },
                        valueRange = -180f..180f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                    )
                }

                // Switches Row (Ground Lock, Contact Shadow, Billboard)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isArabic) "تثبيت أسفل القدمين على الأرض" else "Lock Actor to Ground", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text(if (isArabic) "يضبط ارتفاع الممثل ليتطابق مع مستوى سطح الأرضية" else "Locks vertical pivot to ground plane", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Switch(
                        checked = isGroundLocked,
                        onCheckedChange = { viewModel.updateActor3DTransform(groundLock = it) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isArabic) "ظل اصطناعي واقعي أسفل القدمين" else "Soft Contact Shadow at Feet", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text(if (isArabic) "يرسم ظلاً ناعماً تحت الممثل لإعطاء إيحاء حقيقي بالوجود في البيئة" else "Renders soft ground contact shadow", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Switch(
                        checked = hasContactShadow,
                        onCheckedChange = { viewModel.updateActor3DTransform(contactShadow = it) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isArabic) "مواجهة الكاميرا دوماً (Billboard)" else "Always Face Camera (Billboard)", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text(if (isArabic) "يدور الممثل تلقائياً باتجاه عدسة الكاميرا أثناء حركتها" else "Auto-rotates towards camera position", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Switch(
                        checked = isBillboard,
                        onCheckedChange = { viewModel.updateActor3DTransform(billboard = it) }
                    )
                }
            }
        }

        // ==========================================
        // 4. ENVIRONMENT LIGHT MATCHING
        // ==========================================
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF18181D),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3F3F46)),
            modifier = Modifier.fillMaxWidth().testTag("card_environment_lighting_match")
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isArabic) "4. مطابقة إضاءة البيئة (Environment Lighting Match)" else "4. Environment Lighting & Color Blend",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (isArabic) "يمزج إضاءة البيئة ثلاثية الأبعاد (مثل حرارة رمال الصحراء أو برودة الفضاء أو ضوء الاستوديو) على حواف وجسم الممثل"
                    else "Blends ambient environment lighting onto actor's silhouette for seamless cinematic realism",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isArabic) "شدة امتزاج ضوء البيئة (Light Tint Intensity)" else "Light Tint Blend", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                        Text(String.format(Locale.US, "%.2f", envTint), style = MaterialTheme.typography.bodySmall, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = envTint,
                        onValueChange = { viewModel.updateChromaKeySettings(envTint = it) },
                        valueRange = 0.0f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
                    )
                }

                // Quick Environment Set Selector
                Text(
                    text = if (isArabic) "اختر بيئة سينمائية للممثل:" else "Select Cinematic Virtual Environment:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val envs = listOf(
                        Pair("desert_dunes", if (isArabic) "🏜️ صحراء الكثبان" else "🏜️ Desert Dunes"),
                        Pair("forest_floor", if (isArabic) "🌲 غابة سينمائية" else "🌲 Pine Forest"),
                        Pair("water_ocean", if (isArabic) "🌊 شاطئ وبحر" else "🌊 Ocean Beach"),
                        Pair("studio_stage", if (isArabic) "🎬 استوديو هوليوود" else "🎬 Soundstage"),
                        Pair("deep_space", if (isArabic) "🌌 الفضاء والنجوم" else "🌌 Deep Space")
                    )

                    items(envs) { (envKey, label) ->
                        OutlinedButton(
                            onClick = { viewModel.setTerrainPreset(envKey) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
