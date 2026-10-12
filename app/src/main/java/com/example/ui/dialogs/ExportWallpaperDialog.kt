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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ScheduleItem
import com.example.util.MotivationQuote
import com.example.util.MotivationQuotesHelper
import com.example.util.TimetableWallpaperGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportWallpaperDialog(
    schedules: List<ScheduleItem>,
    initialQuote: MotivationQuote? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTheme by remember { mutableStateOf(TimetableWallpaperGenerator.WallpaperTheme.CYBER_DARK) }
    var selectedScope by remember { mutableStateOf(TimetableWallpaperGenerator.WallpaperScope.ALL_WEEK) }

    // Nếu số tiết lớn (> 40 tiết), tự động đề xuất bố cục Ma Trận Tuần
    var selectedLayout by remember(schedules) {
        mutableStateOf(
            if (schedules.size > 35) TimetableWallpaperGenerator.WallpaperLayout.GRID_MATRIX
            else if (schedules.size > 16) TimetableWallpaperGenerator.WallpaperLayout.TWO_COLUMNS
            else TimetableWallpaperGenerator.WallpaperLayout.STANDARD_9_16
        )
    }
    var autoMergeConsecutive by remember { mutableStateOf(true) }

    var titleText by remember { mutableStateOf("THỜI KHÓA BIỂU") }
    var subtitleText by remember { mutableStateOf("Lịch Học Cá Nhân") }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Câu động lực từ 607 câu
    var currentQuote by remember {
        mutableStateOf(initialQuote ?: MotivationQuotesHelper.getTodayQuote(context))
    }
    var showQuotePicker by remember { mutableStateOf(false) }

    // Tính toán số lượng tiết và số ca sau gộp
    val mergedCount = remember(schedules, autoMergeConsecutive) {
        if (autoMergeConsecutive) {
            (1..7).sumOf { day ->
                val dayItems = schedules.filter { it.dayOfWeek == day }
                TimetableWallpaperGenerator.mergeConsecutivePeriods(dayItems).size
            }
        } else {
            schedules.size
        }
    }

    // Tự động sinh ảnh preview khi đổi cấu hình
    LaunchedEffect(
        selectedTheme,
        selectedScope,
        selectedLayout,
        autoMergeConsecutive,
        titleText,
        subtitleText,
        currentQuote,
        schedules
    ) {
        val quoteFormatted = "“${currentQuote.content}” — (Ngày ${String.format("%03d", currentQuote.dayNumber)}/607 • Chinh phục THPTQG)"
        val bmp = TimetableWallpaperGenerator.generateWallpaper(
            schedules = schedules,
            theme = selectedTheme,
            customTitle = titleText.ifBlank { "THỜI KHÓA BIỂU" },
            customSubtitle = subtitleText.ifBlank { "Lịch Học Cá Nhân" },
            scope = selectedScope,
            layout = selectedLayout,
            autoMergeConsecutive = autoMergeConsecutive,
            customMotivationalQuote = quoteFormatted,
            context = context
        )
        previewBitmap = bmp
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .heightIn(max = 760.dp)
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
                            text = "Xuất Ảnh Nền Màn Hình Khóa",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }

                // Smart Solution Banner for High-Period Schedules (116 periods)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tối Ưu Tỉ Lệ 9:16 Cho Lịch Học Dày Đặc (${schedules.size} Tiết)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (schedules.size > 40) {
                                "✨ Đang phát hiện lịch học dày đặc (${schedules.size} tiết). Ứng dụng đã kích hoạt chế độ 'Bảng Ma Trận Tuần' & 'Gộp Tiết Tự Động':\n" +
                                        "• Bảng Ma Trận: 7 ngày x 12 ca học vừa khít 100% không cắt xén chữ.\n" +
                                        "• Rút gọn từ ${schedules.size} tiết thành ${mergedCount} ca học lớn rõ nét.\n" +
                                        "• Hoặc chọn phạm vi 'Đầu Tuần (T2-T4)' & 'Cuối Tuần (T5-CN)' để tạo 2 ảnh riêng biệt."
                            } else if (autoMergeConsecutive && schedules.size > mergedCount) {
                                "💡 Đang kích hoạt Gộp Tiết Thông Minh: Rút gọn từ ${schedules.size} tiết thành ${mergedCount} ca học lớn rõ nét, chữ to dễ đọc!"
                            } else {
                                "💡 Bạn có thể linh hoạt chọn bố cục 9:16 co giãn, Chia 2 Cột hoặc Bảng Ma Trận để vừa khít màn hình khóa."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            lineHeight = 17.sp
                        )
                    }
                }

                // Preview Thumbnail
                previewBitmap?.let { bmp ->
                    val aspect = if (selectedLayout == TimetableWallpaperGenerator.WallpaperLayout.LONG_POSTER) {
                        bmp.width.toFloat() / bmp.height.toFloat()
                    } else {
                        9f / 16f
                    }
                    Box(
                        modifier = Modifier
                            .height(260.dp)
                            .aspectRatio(aspect)
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

                // Scope Selector: Cả tuần vs Nửa tuần
                Text(
                    text = "1. Phạm Vi Hiển Thị:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(TimetableWallpaperGenerator.WallpaperScope.values()) { scope ->
                        FilterChip(
                            selected = selectedScope == scope,
                            onClick = { selectedScope = scope },
                            label = { Text(scope.label, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Layout Selector: 9:16 chuẩn vs 2 Cột vs Bảng Ma Trận vs Ảnh Dài
                Text(
                    text = "2. Kiểu Bố Cục Khi Nhiều Tiết (Lên tới 116 tiết):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(TimetableWallpaperGenerator.WallpaperLayout.values()) { layout ->
                        FilterChip(
                            selected = selectedLayout == layout,
                            onClick = { selectedLayout = layout },
                            label = { Text(layout.label, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Auto Merge Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tự động gộp các tiết liên tiếp cùng môn",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "vd: 4 tiết Toán sáng 7h-10h gộp thành 1 ca học, tiết kiệm 70% không gian",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoMergeConsecutive,
                        onCheckedChange = { autoMergeConsecutive = it }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 3: Câu động lực học tập từ 607 câu
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.FormatQuote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Câu Động Lực In Dưới Ảnh Nền:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = currentQuote.displayDayTitle,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "“${currentQuote.content}”",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    currentQuote = MotivationQuotesHelper.getRandomQuote(context)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Đổi Câu", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    currentQuote = MotivationQuotesHelper.getTodayQuote(context)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Hôm Nay", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { showQuotePicker = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("607 Câu", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Theme Selection Chips
                Text(
                    text = "4. Phong Cách Màu Sắc:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(TimetableWallpaperGenerator.WallpaperTheme.values()) { theme ->
                        FilterChip(
                            selected = selectedTheme == theme,
                            onClick = { selectedTheme = theme },
                            label = { Text(theme.displayName, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

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

                // Action button
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

    if (showQuotePicker) {
        MotivationQuotesDialog(
            onDismiss = { showQuotePicker = false },
            onSelectQuoteForWallpaper = { quote ->
                currentQuote = quote
                showQuotePicker = false
            }
        )
    }
}
