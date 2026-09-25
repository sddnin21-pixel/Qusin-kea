package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.DayOfWeekHelper
import com.example.data.model.RepeatType
import com.example.data.model.ScheduleItem
import com.example.ui.viewmodel.TimetableUiState
import com.example.ui.viewmodel.TimetableViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScheduleScreen(
    state: TimetableUiState,
    viewModel: TimetableViewModel,
    onNavigateToTimetable: () -> Unit
) {
    val context = LocalContext.current
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(state.previewBitmap) }
    var selectedSampleIndex by remember { mutableStateOf<Int?>(null) }
    val selectedItemIndices = remember(state.extractedItems) {
        mutableStateListOf<Int>().apply {
            addAll(state.extractedItems.indices)
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            selectedBitmap = bitmap
            selectedSampleIndex = null
            viewModel.scanImage(bitmap)
        }
    }

    // Photo picker launcher (for Screenshots or any Gallery image)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                selectedBitmap = softwareBitmap
                selectedSampleIndex = null
                viewModel.setPreviewBitmapOnly(softwareBitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                viewModel.showSnackbar("Không thể mở ảnh: ${e.message}")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Banner / API Key status
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Gemini AI Vision Multimodal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (state.apiKey.isNotBlank()) "Đã kết nối API Key" else "Chưa có Gemini API Key",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.apiKey.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }

                Button(
                    onClick = { viewModel.setShowApiKeyDialog(true) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("config_api_key_btn")
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (state.apiKey.isNotBlank()) "Đổi Key" else "Nhập Key", fontSize = 12.sp)
                }
            }
        }

        // Upload and Screenshot Selection Area
        if (state.extractedItems.isEmpty() && !state.isScanning) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                // If an image is selected, show Preview & Action Card
                if (selectedBitmap != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Image(
                                    bitmap = selectedBitmap!!.asImageBitmap(),
                                    contentDescription = "Ảnh chụp màn hình đã chọn",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(230.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.Fit
                                )

                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            selectedBitmap = null
                                            selectedSampleIndex = null
                                            viewModel.setPreviewBitmapOnly(null)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Bỏ ảnh", modifier = Modifier.size(20.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Ảnh chụp màn hình thời khóa biểu đã sẵn sàng",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "AI sẽ đọc toàn bộ các cột Thứ, Tiết học, Phòng học và tự động tạo thời khóa biểu lặp lại.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action buttons for selected screenshot
                            Button(
                                onClick = {
                                    selectedBitmap?.let { bmp ->
                                        if (state.apiKey.isNotBlank()) {
                                            viewModel.scanImage(bmp)
                                        } else if (selectedSampleIndex != null) {
                                            viewModel.scanSampleScreenshotDemo(selectedSampleIndex!!, bmp)
                                        } else {
                                            viewModel.scanImage(bmp)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("analyze_screenshot_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (state.apiKey.isNotBlank()) "Phân Tích Bằng Gemini AI" else "Quét Ảnh Bằng Gemini AI",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (selectedSampleIndex != null || state.apiKey.isBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        val idx = selectedSampleIndex ?: 1
                                        viewModel.scanSampleScreenshotDemo(idx, selectedBitmap)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("demo_analyze_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.tertiary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Chạy Thử Nghiệm Nhận Dạng (Demo tức thì)")
                                }
                            }
                        }
                    }
                }

                // Screenshot Upload & Import Options
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "Tải Lên Ảnh Chụp Màn Hình Hoặc Ảnh Chụp",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Hỗ trợ ảnh chụp màn hình máy tính, điện thoại, cổng thông tin sinh viên hoặc giấy viết tay.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Primary Action Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Screenshot / Photo Picker Button
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("screenshot_picker_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Screenshot, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ảnh Màn Hình")
                            }

                            // Camera Button
                            OutlinedButton(
                                onClick = { cameraLauncher.launch(null) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("camera_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chụp Ảnh")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Gallery / Files Button
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("gallery_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mở Thư Viện Ảnh Thiết Bị")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Built-in Sample Timetable Screenshots Section (for instant 1-click test)
                Text(
                    text = "Hoặc thử ngay với ảnh chụp màn hình mẫu:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Sample 1: Mobile App Timetable Screenshot
                    SampleScreenshotCard(
                        title = "Mẫu 1: Ứng dụng điện thoại",
                        description = "Ảnh chụp màn hình app lịch học ĐH",
                        imageRes = R.drawable.sample_screenshot_1,
                        isSelected = selectedSampleIndex == 1,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedSampleIndex = 1
                            val bmp = BitmapFactory.decodeResource(context.resources, R.drawable.sample_screenshot_1)
                            selectedBitmap = bmp
                            viewModel.setPreviewBitmapOnly(bmp)
                        }
                    )

                    // Sample 2: Web Portal Timetable Table Screenshot
                    SampleScreenshotCard(
                        title = "Mẫu 2: Cổng đào tạo Web",
                        description = "Ảnh chụp bảng thời khóa biểu",
                        imageRes = R.drawable.sample_screenshot_2,
                        isSelected = selectedSampleIndex == 2,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedSampleIndex = 2
                            val bmp = BitmapFactory.decodeResource(context.resources, R.drawable.sample_screenshot_2)
                            selectedBitmap = bmp
                            viewModel.setPreviewBitmapOnly(bmp)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Scanning State
        if (state.isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(52.dp),
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Gemini AI đang phân tích ảnh chụp màn hình...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Đang nhận dạng các môn học, phòng học, giảng viên và khung giờ lặp lại...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Scan Error Display
        if (state.scanError != null && !state.isScanning) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Thông báo nhận diện",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.scanError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.setShowApiKeyDialog(true) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Nhập API Key")
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.scanSampleScreenshotDemo(selectedSampleIndex ?: 1, selectedBitmap)
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Thử nghiệm Demo")
                        }
                    }
                }
            }
        }

        // Extracted Items Review List
        if (state.extractedItems.isNotEmpty() && !state.isScanning) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI Nhận Diện Được (${state.extractedItems.size} môn)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Đã chọn: ${selectedItemIndices.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(state.extractedItems) { index, item ->
                        val isChecked = selectedItemIndices.contains(index)
                        ExtractedItemCard(
                            item = item,
                            isChecked = isChecked,
                            onToggle = {
                                if (isChecked) {
                                    selectedItemIndices.remove(index)
                                } else {
                                    selectedItemIndices.add(index)
                                }
                            }
                        )
                    }
                }

                // Bottom Action Buttons
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.clearExtractedItems()
                                selectedBitmap = null
                                selectedSampleIndex = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cancel_import_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Hủy bỏ")
                        }

                        Button(
                            onClick = {
                                val toSave = selectedItemIndices.map { state.extractedItems[it] }
                                viewModel.confirmExtractedItems(toSave)
                                onNavigateToTimetable()
                            },
                            enabled = selectedItemIndices.isNotEmpty(),
                            modifier = Modifier
                                .weight(2f)
                                .testTag("confirm_import_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lưu ${selectedItemIndices.size} môn vào Lịch")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SampleScreenshotCard(
    title: String,
    description: String,
    imageRes: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ExtractedItemCard(
    item: ScheduleItem,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isChecked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("item_checkbox_${item.title}")
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = DayOfWeekHelper.getDayNameVi(item.dayOfWeek),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = when (item.repeatType) {
                                RepeatType.WEEKLY.name -> "Hàng tuần"
                                RepeatType.MONTHLY.name -> "Hàng tháng"
                                RepeatType.YEARLY.name -> "Hàng năm"
                                else -> "Một lần"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (item.lecturer.isNotBlank()) {
                    Text(
                        text = "GV: ${item.lecturer}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${item.startTime} - ${item.endTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (item.room.isNotBlank()) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = item.room,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
