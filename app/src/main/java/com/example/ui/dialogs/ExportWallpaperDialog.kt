package com.example.ui.dialogs

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ScheduleItem
import com.example.util.TimetableWallpaperGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportWallpaperDialog(
    schedules: List<ScheduleItem>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTheme by remember { mutableStateOf(TimetableWallpaperGenerator.WallpaperTheme.CYBER_DARK) }
    var titleText by remember { mutableStateOf("THỜI KHÓA BIỂU TUẦN") }
    var subtitleText by remember { mutableStateOf("Học Kỳ Mới • Lịch Học Cá Nhân") }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Tự động sinh ảnh preview khi đổi theme hoặc nội dung
    LaunchedEffect(selectedTheme, titleText, subtitleText, schedules) {
        val bmp = TimetableWallpaperGenerator.generateWallpaper(
            schedules = schedules,
            theme = selectedTheme,
            customTitle = titleText.ifBlank { "THỜI KHÓA BIỂU" },
            customSubtitle = subtitleText.ifBlank { "Lịch Học Cá Nhân" }
        )
        previewBitmap = bmp
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 700.dp)
                .clip(RoundedCornerShape(24.dp))
                .testTag("export_wallpaper_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Wallpaper,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Ảnh Nền TKB 9:16",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }

                Text(
                    text = "Xuất ảnh tỷ lệ 9:16 sắc nét để cài hình nền khóa hoặc chia sẻ lên Story",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Preview Thumbnail
                previewBitmap?.let { bmp ->
                    Box(
                        modifier = Modifier
                            .height(280.dp)
                            .aspectRatio(9f / 16f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .testTag("wallpaper_preview_box"),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Xem trước ảnh nền",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Theme Selection Chips
                Text(
                    text = "Chọn Phong Cách Màu Sắc:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TimetableWallpaperGenerator.WallpaperTheme.values().forEach { theme ->
                        FilterChip(
                            selected = selectedTheme == theme,
                            onClick = { selectedTheme = theme },
                            label = { Text(theme.displayName, style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Text inputs
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Tiêu đề ảnh") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = subtitleText,
                    onValueChange = { subtitleText = it },
                    label = { Text("Phụ đề / Lớp học") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Button(
                    onClick = {
                        previewBitmap?.let { bmp ->
                            TimetableWallpaperGenerator.saveWallpaperToCacheAndShare(context, bmp)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_share_wallpaper"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Lưu Ảnh / Chia Sẻ Màn Hình Khóa",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
