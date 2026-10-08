package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.GradeRecord
import com.example.data.model.ScheduleItem
import com.example.ui.viewmodel.TimetableUiState
import com.example.ui.viewmodel.TimetableViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeAttendanceDialog(
    state: TimetableUiState,
    viewModel: TimetableViewModel,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Bảng Điểm & GPA, 1: Điểm Danh & Vắng Học
    var editingGradeForSubject by remember { mutableStateOf<GradeRecord?>(null) }

    // Distinct subjects from schedule
    val uniqueSubjects = remember(state.schedules) {
        state.schedules.distinctBy { it.title.trim() }
    }

    // Compute Overall GPA
    val totalCredits = state.gradeRecords.filter { it.calculateAverage10() != null }.sumOf { it.credits }
    val weightedAvg10 = if (totalCredits > 0) {
        state.gradeRecords.mapNotNull { g ->
            val avg = g.calculateAverage10()
            if (avg != null) avg * g.credits else null
        }.sum() / totalCredits
    } else 0f

    val weightedGpa4 = if (totalCredits > 0) {
        state.gradeRecords.mapNotNull { g ->
            val gpa = g.getGpa4()
            if (gpa != null) gpa * g.credits else null
        }.sum() / totalCredits
    } else 0f

    val overallClassification = when {
        weightedAvg10 >= 9.0f -> "Xuất sắc 🌟"
        weightedAvg10 >= 8.0f -> "Giỏi 🎯"
        weightedAvg10 >= 6.5f -> "Khá 👍"
        weightedAvg10 >= 5.0f -> "Trung bình ⚖️"
        weightedAvg10 > 0f -> "Cần cố gắng ⚠️"
        else -> "Chưa nhập điểm"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .heightIn(max = 720.dp)
                .clip(RoundedCornerShape(24.dp))
                .testTag("grade_attendance_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Grade,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Học Tập & Điểm Số",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Overall GPA Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "ĐIỂM TRUNG BÌNH HỌC KỲ (GPA)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = if (totalCredits > 0) String.format(Locale.US, "%.2f", weightedAvg10) else "--",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = " / 10",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = "GPA: " + if (totalCredits > 0) String.format(Locale.US, "%.2f", weightedGpa4) else "--",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                            Text(
                                text = "Xếp loại: $overallClassification ($totalCredits tín chỉ)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Switcher
                SecondaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Bảng Điểm Môn", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Sổ Điểm Danh & Vắng", fontWeight = FontWeight.SemiBold) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    // TAB 0: GRADES LIST
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uniqueSubjects.isEmpty()) {
                            item {
                                Text(
                                    text = "Chưa có môn học nào trong TKB để tính điểm.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp)
                                )
                            }
                        } else {
                            items(uniqueSubjects) { subject ->
                                val existingGrade = state.gradeRecords.find { it.scheduleId == subject.id || it.subjectTitle == subject.title }
                                    ?: GradeRecord(scheduleId = subject.id, subjectTitle = subject.title)
                                val avg10 = existingGrade.calculateAverage10()

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { editingGradeForSubject = existingGrade },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = subject.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            val scoreSummary = buildString {
                                                append("${existingGrade.credits} tín chỉ")
                                                existingGrade.regularScore1?.let { append(" • CC: $it") }
                                                existingGrade.midtermScore?.let { append(" • GK: $it") }
                                                existingGrade.finalScore?.let { append(" • CK: $it") }
                                            }
                                            Text(
                                                text = scoreSummary,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (avg10 != null) {
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = String.format(Locale.US, "%.1f", avg10),
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = "Điểm ${existingGrade.getLetterGrade()} (${existingGrade.getGpa4()})",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                            } else {
                                                OutlinedButton(
                                                    onClick = { editingGradeForSubject = existingGrade },
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("+ Nhập điểm", style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // TAB 1: ATTENDANCE TRACKER
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uniqueSubjects) { subject ->
                            val absents = state.attendanceRecords.count { it.scheduleId == subject.id && it.status == "ABSENT" }
                            val excused = state.attendanceRecords.count { it.scheduleId == subject.id && it.status == "EXCUSED" }
                            val presents = state.attendanceRecords.count { it.scheduleId == subject.id && it.status == "PRESENT" }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = subject.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )

                                        if (absents >= 3) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .background(Color(0xFFEF4444).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Cảnh báo cấm thi ($absents buổi vắng)",
                                                    color = Color(0xFFEF4444),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "Vắng: $absents | Phép: $excused",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Quick Attendance Buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.markAttendance(subject.id, subject.title, AttendanceStatus.PRESENT)
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("✓ Có mặt", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.markAttendance(subject.id, subject.title, AttendanceStatus.EXCUSED)
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Có phép", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.markAttendance(subject.id, subject.title, AttendanceStatus.ABSENT)
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("❌ Vắng", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        if (state.attendanceRecords.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Lịch sử điểm danh gần đây:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(state.attendanceRecords.take(10)) { record ->
                                val statusColor = when (record.status) {
                                    "PRESENT" -> Color(0xFF10B981)
                                    "EXCUSED" -> Color(0xFFF59E0B)
                                    else -> Color(0xFFEF4444)
                                }
                                val statusLabel = when (record.status) {
                                    "PRESENT" -> "Có mặt"
                                    "EXCUSED" -> "Có phép"
                                    else -> "Vắng mặt"
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(statusColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${record.date}: ${record.subjectTitle} ($statusLabel)",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteAttendance(record.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Xóa",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog Edit Grade
    editingGradeForSubject?.let { grade ->
        EditGradeSubDialog(
            initialGrade = grade,
            onDismiss = { editingGradeForSubject = null },
            onSave = { updated ->
                viewModel.saveGrade(updated)
                editingGradeForSubject = null
            }
        )
    }
}

@Composable
private fun EditGradeSubDialog(
    initialGrade: GradeRecord,
    onDismiss: () -> Unit,
    onSave: (GradeRecord) -> Unit
) {
    var creditsText by remember { mutableStateOf(initialGrade.credits.toString()) }
    var reg1Text by remember { mutableStateOf(initialGrade.regularScore1?.toString() ?: "") }
    var reg2Text by remember { mutableStateOf(initialGrade.regularScore2?.toString() ?: "") }
    var midtermText by remember { mutableStateOf(initialGrade.midtermScore?.toString() ?: "") }
    var finalText by remember { mutableStateOf(initialGrade.finalScore?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Nhập Điểm: ${initialGrade.subjectTitle}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = creditsText,
                    onValueChange = { creditsText = it },
                    label = { Text("Số tín chỉ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reg1Text,
                    onValueChange = { reg1Text = it },
                    label = { Text("Điểm 15p / Chuyên cần (HS1)") },
                    placeholder = { Text("vd: 8.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reg2Text,
                    onValueChange = { reg2Text = it },
                    label = { Text("Điểm 1 tiết / Bài tập (HS1)") },
                    placeholder = { Text("vd: 9.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = midtermText,
                    onValueChange = { midtermText = it },
                    label = { Text("Điểm Giữa Kỳ (HS2)") },
                    placeholder = { Text("vd: 8.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = finalText,
                    onValueChange = { finalText = it },
                    label = { Text("Điểm Thi Cuối Kỳ (HS3)") },
                    placeholder = { Text("vd: 8.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val credits = creditsText.toIntOrNull() ?: 3
                    val reg1 = reg1Text.toFloatOrNull()
                    val reg2 = reg2Text.toFloatOrNull()
                    val mid = midtermText.toFloatOrNull()
                    val fin = finalText.toFloatOrNull()
                    val updated = initialGrade.copy(
                        credits = credits,
                        regularScore1 = reg1,
                        regularScore2 = reg2,
                        midtermScore = mid,
                        finalScore = fin
                    )
                    onSave(updated)
                }
            ) {
                Text("Lưu Điểm")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
