package com.example.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.StudioTab
import com.example.ui.StudioViewModel

@Composable
fun ExportInspectorPanel(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val isArabic by viewModel.isArabic.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val progress by viewModel.exportProgress.collectAsState()
    val exportedPath by viewModel.exportedFilePath.collectAsState()
    val selectedFormat by viewModel.exportFormat.collectAsState()
    val selectedRes by viewModel.exportResolution.collectAsState()

    val formats = remember {
        listOf(
            Pair("mp4", "MP4 Video"),
            Pair("webm", "WebM Video"),
            Pair("gif", "Animated GIF"),
            Pair("png", "PNG Sequence")
        )
    }

    val resolutions = remember {
        listOf(
            Pair("480p", if (isArabic) "480p (اقتصادي)" else "480p Draft"),
            Pair("720p", if (isArabic) "720p (متوسط)" else "720p HD"),
            Pair("1080p", if (isArabic) "1080p (عالي)" else "1080p Full HD"),
            Pair("4K", if (isArabic) "4K (فائق)" else "4K Ultra")
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = if (isArabic) "تحديد دقة وجودة الفيديو للتصدير" else "Video Resolution & Quality Settings",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFFD700)
            )
            Text(
                text = if (isArabic) "هذه الصفحة مخصصة لتحديد الدقة والصيغة فقط. زر تصدير الفيديو موجود فقط في نافذة الكاميرا."
                else "This page is dedicated to resolution & format settings. Video export is performed in the Camera tab.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        // Resolution Selector
        Text(
            text = if (isArabic) "دقة الفيديو (Resolution)" else "Video Resolution",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            resolutions.forEach { res ->
                val isSel = selectedRes == res.first
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) Color(0xFF27272A) else Color(0xFF18181B))
                        .border(
                            width = if (isSel) 2.dp else 1.dp,
                            color = if (isSel) Color(0xFFFFD700) else Color(0xFF3F3F46),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.exportResolution.value = res.first }
                        .padding(vertical = 10.dp)
                        .testTag("res_${res.first}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = res.second,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSel) Color(0xFFFFD700) else Color.White
                    )
                }
            }
        }

        // Export Format Selector
        Text(
            text = if (isArabic) "صيغة الفيديو (Format)" else "Export Format",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            formats.forEach { fmt ->
                val isSel = selectedFormat == fmt.first
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) Color(0xFF27272A) else Color(0xFF18181B))
                        .border(
                            width = if (isSel) 2.dp else 1.dp,
                            color = if (isSel) Color(0xFFFFD700) else Color(0xFF3F3F46),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.exportFormat.value = fmt.first }
                        .padding(vertical = 10.dp)
                        .testTag("fmt_${fmt.first}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = fmt.second,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSel) Color(0xFFFFD700) else Color.White
                    )
                }
            }
        }

        // Direction Card: Pointing user to the Camera window for export
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF27272A)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "زر تصدير الفيديو في نافذة الكاميرا فقط" else "Export Video Button is in Camera Window",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                }
                Text(
                    text = if (isArabic) "تم تحديد الدقة على $selectedRes والصيغة على ${selectedFormat.uppercase()}. لتصدير الفيديو مع حركة الكاميرا، انتقل إلى نافذة الكاميرا واضغط على زر التصدير بعد تحريك الكاميرا."
                    else "Current preset: $selectedRes (${selectedFormat.uppercase()}). To export the video with motion, switch to the Camera window and click Export Video after camera movement.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
                Button(
                    onClick = { viewModel.activeTab.value = StudioTab.CAMERA },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_go_to_camera_tab")
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "الانتقال إلى نافذة الكاميرا (Camera)" else "Go to Camera Window",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.Black
                    )
                }
            }
        }

        // Export Progress or Saved Result Status Display
        if (isExporting) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isArabic) "جاري رندر وتصدير الفيديو: $progress%" else "Rendering Video: $progress%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFFD700)
                    )
                    LinearProgressIndicator(
                        progress = progress / 100f,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFFD700),
                        trackColor = Color(0xFF3F3F46)
                    )
                }
            }
        }

        if (!exportedPath.isNullOrEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF132A1C)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isArabic) "تم تصدير الفيديو بنجاح وحفظه في المعرض!" else "Video exported and saved to Gallery!",
                            style = MaterialTheme.typography.bodyMedium,
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
}
