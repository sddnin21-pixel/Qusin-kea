package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.receiver.ScheduleAlarmReceiver
import com.example.ui.viewmodel.TimetableUiState
import com.example.ui.viewmodel.TimetableViewModel
import com.example.util.GoogleCalendarHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: TimetableUiState,
    viewModel: TimetableViewModel
) {
    val context = LocalContext.current
    var showClearConfirmation by remember { mutableStateOf(false) }

    // Calendar permissions launcher for direct sync
    val calendarPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.WRITE_CALENDAR] == true
        if (granted) {
            val (count, message) = GoogleCalendarHelper.syncDirectToGoogleCalendar(context, state.schedules)
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Cần cấp quyền Lịch để đồng bộ trực tiếp.", Toast.LENGTH_SHORT).show()
        }
    }

    // Notification permission launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleNotifications(true)
            Toast.makeText(context, "Đã cấp quyền thông báo nhắc nhở.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Chưa cấp quyền thông báo.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Gemini AI Key
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cấu hình Gemini AI API Key",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val isKeyConfigured = state.apiKey.isNotBlank()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isKeyConfigured) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isKeyConfigured) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isKeyConfigured) "Đã thiết lập API Key hợp lệ" else "Chưa có API Key. Cần nhập để dùng AI quét ảnh.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isKeyConfigured) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Bạn có thể lấy Gemini API Key miễn phí tại Google AI Studio (aistudio.google.com). Ứng dụng dùng mô hình Gemini 3.5 Flash tốc độ cao.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { viewModel.setShowApiKeyDialog(true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_open_api_key_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isKeyConfigured) "Thay Đổi API Key" else "Nhập API Key Của Bạn")
                }
            }
        }

        // Section 2: Google Calendar Integration
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Đồng Bộ Với Lịch Google",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Chuyển toàn bộ thời khóa biểu vào Google Calendar với chu kỳ lặp lại (hàng tuần, hàng tháng, hàng năm) và tự động nhận chuông báo trước giờ học trên mọi thiết bị.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        calendarPermissionsLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_CALENDAR,
                                Manifest.permission.WRITE_CALENDAR
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sync_all_google_cal_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đồng Bộ Trực Tiếp Vào Google Calendar")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        val shared = GoogleCalendarHelper.shareIcsExport(context, state.schedules)
                        if (!shared) {
                            Toast.makeText(context, "Chưa có môn học nào để xuất lịch.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_ics_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Xuất & Chia Sẻ File Lịch (.ics)")
                }
            }
        }

        // Section 3: Notification & Reminders
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Nhắc Nhở Giờ Học",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Switch(
                        checked = state.isNotificationsEnabled,
                        onCheckedChange = { checked ->
                            if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.toggleNotifications(checked)
                            }
                        },
                        modifier = Modifier.testTag("global_notification_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Thời gian nhắc nhở mặc định trước khi lớp học bắt đầu:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 15, 30, 60).forEach { mins ->
                        val label = if (mins == 60) "Trước 1 giờ" else "Trước $mins phút"
                        FilterChip(
                            selected = state.defaultReminderMinutes == mins,
                            onClick = { viewModel.setDefaultReminderMinutes(mins) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("reminder_chip_$mins")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = {
                        val testIntent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
                            putExtra(ScheduleAlarmReceiver.EXTRA_ITEM_ID, 99999L)
                            putExtra(ScheduleAlarmReceiver.EXTRA_TITLE, "Thử nghiệm: Lập Trình Di Động")
                            putExtra(ScheduleAlarmReceiver.EXTRA_ROOM, "Phòng B204")
                            putExtra(ScheduleAlarmReceiver.EXTRA_START_TIME, "08:00")
                            putExtra(ScheduleAlarmReceiver.EXTRA_MINUTES_BEFORE, 15)
                        }
                        context.sendBroadcast(testIntent)
                        Toast.makeText(context, "Đã gửi thông báo thử nghiệm!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_notification_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kiểm Tra Báo Chuông Ngay")
                }
            }
        }

        // Section 4: Data Management & Sample Data
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Dữ Liệu & Hỗ Trợ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { viewModel.seedSampleData() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("seed_sample_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nạp Lại Thời Khóa Biểu Mẫu")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showClearConfirmation = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clear_all_btn"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Xóa Toàn Bộ Lịch Học")
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Xác nhận xóa hết") },
            text = { Text("Bạn có chắc chắn muốn xóa toàn bộ lịch học và hủy tất cả thông báo nhắc nhở?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllSchedules()
                        showClearConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xác nhận xóa")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirmation = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}
