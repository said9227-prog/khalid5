package com.example.ui.panels

import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
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
import com.example.ui.Imported3DModelItem
import com.example.ui.StudioViewModel

data class BrushTypeItem(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val iconEmoji: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurfacePaintInspectorPanel(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isArabic by viewModel.isArabic.collectAsState()
    val isPaintEnabled by viewModel.isPaintModeEnabled.collectAsState()
    val activeBrushType by viewModel.activeBrushType.collectAsState()
    val activeBrushTool by viewModel.activeBrushTool.collectAsState()
    val brushRadius by viewModel.brushRadius.collectAsState()
    val brushDensity by viewModel.brushDensity.collectAsState()
    val totalCount by viewModel.totalPaintedCount.collectAsState()
    val importedModels by viewModel.imported3DModels.collectAsState()
    val modelStatus by viewModel.importModelStatus.collectAsState()

    // 3D Model File Picker for GLB, GLTF, OBJ
    val modelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val contentResolver = context.contentResolver
                var fileName = "model_" + System.currentTimeMillis()
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIdx >= 0) {
                        fileName = cursor.getString(nameIdx) ?: fileName
                    }
                }

                val format = when {
                    fileName.endsWith(".glb", ignoreCase = true) -> "GLB"
                    fileName.endsWith(".gltf", ignoreCase = true) -> "GLTF"
                    fileName.endsWith(".obj", ignoreCase = true) -> "OBJ"
                    else -> "GLB"
                }

                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null && bytes.isNotEmpty()) {
                    val base64Str = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    viewModel.import3DModelFile(fileName, format, base64Str)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val defaultBrushTypes = remember {
        listOf(
            BrushTypeItem("grass", "Grass Blades", "عشب أخضر 3D", "🌱", Color(0xFF22C55E)),
            BrushTypeItem("trees", "Pine Trees", "أشجار وغابات 3D", "🌲", Color(0xFF15803D)),
            BrushTypeItem("flowers", "Flowers", "زهور وحدائق 3D", "🌸", Color(0xFFEC4899)),
            BrushTypeItem("rocks", "Rocks & Boulders", "صخور وأحجار 3D", "🪨", Color(0xFF78716C)),
            BrushTypeItem("stars", "Glowing Stars", "نجوم ومجسمات مضيئة", "✨", Color(0xFFFBBF24))
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Panel Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isArabic) "3D Surface Painting Studio (الرسم بالفرشاة ونماذج 3D)" else "3D Surface Painting & 3D Models",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFFD700),
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (isArabic) "إجمالي الأجسام: $totalCount" else "Total Instances: $totalCount",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF10B981),
                fontWeight = FontWeight.Bold
            )
        }

        // Toggle Switch to Enable/Disable 3D Surface Painting Mode
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Brush, contentDescription = null, tint = Color(0xFFFFD700))
                Column {
                    Text(
                        text = if (isArabic) "تشغيل وضع الرسم بالفرشاة 3D (Object Paint)" else "Enable 3D Surface Object Painting",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isArabic) "اسحب إصبعك على الأرضية لغرس وتوزيع المجسمات والنماذج فورياً" else "Drag finger across ground to paint 3D objects live",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Switch(
                checked = isPaintEnabled,
                onCheckedChange = { viewModel.togglePaintMode(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFFFD700),
                    checkedTrackColor = Color(0xFF3F3F46)
                ),
                modifier = Modifier.testTag("switch_toggle_paint_mode")
            )
        }

        Divider(color = Color(0xFF27272A))

        // ==========================================
        // 3D MODEL IMPORT SECTION (GLB / GLTF / OBJ)
        // ==========================================
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1C1C22),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
            modifier = Modifier.fillMaxWidth().testTag("section_3d_model_import")
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Category, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "إستيراد نماذج 3D (GLB / GLTF / OBJ)" else "Import 3D Models (GLB / GLTF / OBJ)",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Format Badges
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FormatBadge("GLB", Color(0xFF38BDF8))
                        FormatBadge("GLTF", Color(0xFF34D399))
                        FormatBadge("OBJ", Color(0xFFFBBF24))
                    }
                }

                Text(
                    text = if (isArabic) "يمكنك إستيراد أي نموذج 3D واستخدامه كفرشاة رسم لتوزيع آلاف المجسمات أو إدراجه مباشرة في المشهد"
                    else "Import any 3D model to paint thousands of copies on the ground or place in the studio scene",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )

                // Import Button from Device Storage
                Button(
                    onClick = {
                        modelPickerLauncher.launch("*/*")
                    },
                    modifier = Modifier.fillMaxWidth().testTag("btn_import_3d_model"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "📥 اختيار واستيراد ملف 3D (.glb, .gltf, .obj)" else "📥 Import 3D Model File (.glb, .gltf, .obj)",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (modelStatus.isNotBlank()) {
                    Text(
                        text = "✨ $modelStatus",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4ADE80),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Preset Ready-to-Test 3D Models
                Text(
                    text = if (isArabic) "نماذج 3D سريعة جاهزة للتجربة والفرشاة:" else "Ready-to-use 3D Presets:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presets = listOf(
                        Imported3DModelItem("preset_spaceship", "🚀 سفينة (GLB)", "GLB", isPreset = true, presetKey = "spaceship"),
                        Imported3DModelItem("preset_robot", "🤖 روبوت (OBJ)", "OBJ", isPreset = true, presetKey = "robot"),
                        Imported3DModelItem("preset_car", "🏎️ سيارة (GLB)", "GLB", isPreset = true, presetKey = "car"),
                        Imported3DModelItem("preset_trophy", "🏆 كأس (OBJ)", "OBJ", isPreset = true, presetKey = "trophy")
                    )

                    items(presets) { p ->
                        OutlinedButton(
                            onClick = { viewModel.loadPreset3DModel(p) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                        ) {
                            Text(p.name, style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }

                // List of Imported Models
                if (importedModels.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isArabic) "النماذج المستوردة النشطة في الاستوديو:" else "Active Imported 3D Models:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFD700)
                        )

                        importedModels.take(4).forEach { item ->
                            val isBrushActive = activeBrushType == item.brushId || activeBrushType == item.id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isBrushActive) Color(0xFF27272A) else Color(0xFF141416),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isBrushActive) 1.5.dp else 1.dp,
                                    color = if (isBrushActive) Color(0xFFFFD700) else Color(0xFF3F3F46)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        FormatBadge(item.format, if (item.format == "OBJ") Color(0xFFFBBF24) else Color(0xFF38BDF8))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = item.name.take(20),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // Set as brush
                                        FilledTonalButton(
                                            onClick = { viewModel.setModelAsBrush(item.id) },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = if (isBrushActive) Color(0xFFFFD700) else Color(0xFF3F3F46)
                                            )
                                        ) {
                                            Text(
                                                text = if (isBrushActive) (if (isArabic) "الفرشاة النشطة ✅" else "Active Brush ✅")
                                                else (if (isArabic) "🖌️ تعيين كفرشاة" else "🖌️ Set Brush"),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isBrushActive) Color.Black else Color.White
                                            )
                                        }

                                        // Spawn into scene
                                        FilledTonalButton(
                                            onClick = { viewModel.spawnModelInScene(item.id) },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (isArabic) "➕ المشهد" else "➕ Scene",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White
                                            )
                                        }

                                        if (!item.isPreset) {
                                            IconButton(
                                                onClick = { viewModel.deleteImportedModel(item.id) },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tool Selector (Paint Brush 🖌️ vs Eraser 🧹)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isPaint = activeBrushTool == "paint"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isPaint) Color(0xFFFFD700) else Color(0xFF18181B))
                    .clickable { viewModel.setBrushTool("paint") }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Brush, contentDescription = null, tint = if (isPaint) Color.Black else Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isArabic) "فرشاة الرسم 🖌️" else "Paint Brush 🖌️", color = if (isPaint) Color.Black else Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }

            val isErase = activeBrushTool == "erase"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isErase) Color(0xFFEF4444) else Color(0xFF18181B))
                    .clickable { viewModel.setBrushTool("erase") }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoFixNormal, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isArabic) "ممحاة المسح 🧹" else "Eraser Tool 🧹", color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Brush Objects Selection Palette (نوع المجسمات 3D)
        Text(
            text = if (isArabic) "لوحة خيارات مجسمات الفرشاة (Object Brush Palette):" else "Object Brush Palette:",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(defaultBrushTypes) { b ->
                val isSel = activeBrushType == b.id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) Color(0xFF27272A) else Color(0xFF18181B))
                        .border(
                            width = if (isSel) 2.dp else 1.dp,
                            color = if (isSel) Color(0xFFFFD700) else Color(0xFF3F3F46),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.setBrushType(b.id) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(b.iconEmoji, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) b.nameAr else b.nameEn,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSel) Color(0xFFFFD700) else Color.White,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Brush Size / Radius Slider (حجم وقطر الفرشاة)
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (isArabic) "حجم وقطر الفرشاة (Brush Radius)" else "Brush Radius Size", style = MaterialTheme.typography.bodySmall, color = Color.White)
                Text(String.format("%.1fm", brushRadius), style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
            }
            Slider(
                value = brushRadius,
                onValueChange = { viewModel.setBrushRadius(it) },
                valueRange = 0.5f..8.0f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
            )
        }

        // Density Slider (كثافة التوزيع بالفرشاة)
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (isArabic) "كثافة التوزيع بالفرشاة (Density)" else "Brush Density Rate", style = MaterialTheme.typography.bodySmall, color = Color.White)
                Text("$brushDensity", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
            }
            Slider(
                value = brushDensity.toFloat(),
                onValueChange = { viewModel.setBrushDensity(it.toInt()) },
                valueRange = 1.0f..20.0f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
            )
        }

        // Instant Scatter & Populate Buttons (توزيع عشوائي آلي 3ds Max Scatter)
        Text(
            text = if (isArabic) "أداة التوزيع العشوائي الشامل (Scatter Generator):" else "Automated Scatter Generator:",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(100, 300, 800, 1500).forEach { count ->
                Button(
                    onClick = { viewModel.scatterSelectedBrush(count) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+$count", color = Color(0xFFFFD700), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Clear Surface Button
        Button(
            onClick = { viewModel.clearPaintedSurface() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
            modifier = Modifier.fillMaxWidth().testTag("btn_clear_painted_surface"),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isArabic) "مسح وتنظيف السطح بالكامل" else "Clear All Painted Surface Objects", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FormatBadge(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}
