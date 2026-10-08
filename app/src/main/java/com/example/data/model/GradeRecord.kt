package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "grade_records")
data class GradeRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scheduleId: Long,
    val subjectTitle: String,
    val credits: Int = 3, // Số tín chỉ
    val regularScore1: Float? = null, // Điểm chuyên cần / 15p (hệ số 1)
    val regularScore2: Float? = null, // Điểm kiểm tra 1 tiết (hệ số 1)
    val midtermScore: Float? = null,  // Điểm giữa kỳ (hệ số 2)
    val finalScore: Float? = null,    // Điểm thi cuối kỳ (hệ số 3)
    val note: String = ""
) {
    /**
     * Tính điểm trung bình môn hệ 10:
     * Công thức thông dụng: (HS1*1 + HS1*1 + GiữaKỳ*2 + CuốiKỳ*3) / Tổng hệ số
     */
    fun calculateAverage10(): Float? {
        var totalWeighted = 0f
        var totalWeights = 0f

        regularScore1?.let {
            totalWeighted += it * 1f
            totalWeights += 1f
        }
        regularScore2?.let {
            totalWeighted += it * 1f
            totalWeights += 1f
        }
        midtermScore?.let {
            totalWeighted += it * 2f
            totalWeights += 2f
        }
        finalScore?.let {
            totalWeighted += it * 3f
            totalWeights += 3f
        }

        return if (totalWeights > 0) {
            String.format(Locale.US, "%.2f", totalWeighted / totalWeights).toFloat()
        } else {
            null
        }
    }

    /**
     * Quy đổi điểm chữ theo quy chế đại học / THPT
     */
    fun getLetterGrade(): String {
        val avg = calculateAverage10() ?: return "-"
        return when {
            avg >= 8.5f -> "A"
            avg >= 8.0f -> "B+"
            avg >= 7.0f -> "B"
            avg >= 6.5f -> "C+"
            avg >= 5.5f -> "C"
            avg >= 5.0f -> "D+"
            avg >= 4.0f -> "D"
            else -> "F"
        }
    }

    /**
     * Quy đổi sang thang điểm 4 (GPA đại học)
     */
    fun getGpa4(): Float? {
        val avg = calculateAverage10() ?: return null
        return when {
            avg >= 8.5f -> 4.0f
            avg >= 8.0f -> 3.5f
            avg >= 7.0f -> 3.0f
            avg >= 6.5f -> 2.5f
            avg >= 5.5f -> 2.0f
            avg >= 5.0f -> 1.5f
            avg >= 4.0f -> 1.0f
            else -> 0.0f
        }
    }

    fun getClassification(): String {
        val avg = calculateAverage10() ?: return "Chưa có điểm"
        return when {
            avg >= 9.0f -> "Xuất sắc 🌟"
            avg >= 8.0f -> "Giỏi 🎯"
            avg >= 6.5f -> "Khá 👍"
            avg >= 5.0f -> "Trung bình ⚖️"
            else -> "Cần cố gắng ⚠️"
        }
    }
}
