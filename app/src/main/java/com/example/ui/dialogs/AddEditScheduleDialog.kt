package com.example.ui.dialogs

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DayOfWeekHelper
import com.example.data.model.RepeatType
import com.example.data.model.ScheduleItem

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditScheduleDialog(
    initialItem: ScheduleItem,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (ScheduleItem) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialItem.title) }
    var lecturer by remember { mutableStateOf(initialItem.lecturer) }
    var room by remember { mutableStateOf(initialItem.room) }
    var dayOfWeek by remember { mutableIntStateOf(initialItem.dayOfWeek) }
    var startTime by remember { mutableStateOf(initialItem.startTime) }
    var endTime by remember { mutableStateOf(initialItem.endTime) }
    var repeatType by remember { mutableStateOf(initialItem.repeatType) }
    var colorHex by remember { mutableStateOf(initialItem.colorHex) }
    var notes by remember { mutableStateOf(initialItem.notes) }
    var isReminderEnabled by remember { mutableStateOf(initialItem.isReminderEnabled) }
    var reminderMinutes by remember { mutableIntStateOf(initialItem.reminderMinutesBefore) }

    val presetColors = listOf(
        "#2563EB", "#10B981", "#8B5CF6", "#F59E0B",
        "#EC4899", "#06B6D4", "#EF4444", "#14B8A6"
    )

    fun showTimePicker(currentVal: String, onTimeSelected: (String) -> Unit) {
        val parts = currentVal.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                val formatted = String.format("%02d:%02d", selectedHour, selectedMinute)
                onTimeSelected(formatted)
            },
            hour,
            minute,
            true
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EditCalendar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isNew) "Thêm Buổi Học Mới" else "Chỉnh Sửa Lịch Học",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Tên môn học
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tên môn học / Sự kiện *") },
                    placeholder = { Text("Ví dụ: Toán Cao Cấp, Lập Trình...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("schedule_title_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Giảng viên & Phòng học
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = lecturer,
                        onValueChange = { lecturer = it },
                        label = { Text("Giảng viên") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("schedule_lecturer_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("Phòng học") },
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("schedule_room_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Thứ trong tuần
                Text(
                    text = "Ngày trong tuần",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    (1..7).forEach { dayNum ->
                        FilterChip(
                            selected = dayOfWeek == dayNum,
                            onClick = { dayOfWeek = dayNum },
                            label = { Text(DayOfWeekHelper.getShortDayNameVi(dayNum)) },
                            modifier = Modifier.testTag("day_chip_$dayNum")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Thời gian bắt đầu và kết thúc
                Text(
                    text = "Thời gian học (24h)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showTimePicker(startTime) { startTime = it } },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("start_time_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bắt đầu: $startTime")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { showTimePicker(endTime) { endTime = it } },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("end_time_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kết thúc: $endTime")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Chu kỳ lặp lại
                Text(
                    text = "Chu kỳ lặp lại sự kiện",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RepeatType.values().forEach { rType ->
                        FilterChip(
                            selected = repeatType == rType.name,
                            onClick = { repeatType = rType.name },
                            label = { Text(rType.labelVi) },
                            modifier = Modifier.testTag("repeat_chip_${rType.name}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bật thông báo nhắc nhở
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
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Thông báo nhắc nhở trước giờ học")
                    }
                    Switch(
                        checked = isReminderEnabled,
                        onCheckedChange = { isReminderEnabled = it },
                        modifier = Modifier.testTag("reminder_switch")
                    )
                }

                if (isReminderEnabled) {
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(10, 15, 30, 60).forEach { mins ->
                            val label = if (mins == 60) "Trước 1h" else "Trước $mins phút"
                            FilterChip(
                                selected = reminderMinutes == mins,
                                onClick = { reminderMinutes = mins },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Chọn màu sắc
                Text(
                    text = "Màu sắc môn học",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    presetColors.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = colorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Ghi chú
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Ghi chú khác") },
                    placeholder = { Text("Mã môn, link học online, bài tập...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val updated = initialItem.copy(
                            title = title.trim(),
                            lecturer = lecturer.trim(),
                            room = room.trim(),
                            dayOfWeek = dayOfWeek,
                            startTime = startTime,
                            endTime = endTime,
                            repeatType = repeatType,
                            colorHex = colorHex,
                            notes = notes.trim(),
                            reminderMinutesBefore = reminderMinutes,
                            isReminderEnabled = isReminderEnabled
                        )
                        onSave(updated)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("save_schedule_button")
            ) {
                Text(if (isNew) "Thêm Môn" else "Lưu Thay Đổi")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_schedule_button")
            ) {
                Text("Hủy")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
