package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.ScheduleItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

object ScheduleBackupHelper {

    fun exportToJson(schedules: List<ScheduleItem>): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportTime", System.currentTimeMillis())
        val array = JSONArray()

        schedules.forEach { item ->
            val obj = JSONObject().apply {
                put("title", item.title)
                put("lecturer", item.lecturer)
                put("room", item.room)
                put("dayOfWeek", item.dayOfWeek)
                put("startTime", item.startTime)
                put("endTime", item.endTime)
                put("repeatType", item.repeatType)
                put("colorHex", item.colorHex)
                put("notes", item.notes)
                put("reminderMinutesBefore", item.reminderMinutesBefore)
                put("isReminderEnabled", item.isReminderEnabled)
            }
            array.put(obj)
        }
        root.put("schedules", array)
        return root.toString(2)
    }

    fun shareJsonBackup(context: Context, schedules: List<ScheduleItem>): Boolean {
        if (schedules.isEmpty()) return false
        try {
            val jsonContent = exportToJson(schedules)
            val fileName = "thoi_khoa_bieu_backup_${System.currentTimeMillis()}.json"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { it.write(jsonContent.toByteArray()) }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Sao lưu Thời Khóa Biểu")
                putExtra(Intent.EXTRA_TEXT, "File sao lưu thời khóa biểu của bạn.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Xuất & Lưu file Thời khóa biểu")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun parseJsonBackup(context: Context, uri: Uri): Result<List<ScheduleItem>> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("Không thể đọc file từ thiết bị."))

            val content = InputStreamReader(inputStream).use { it.readText() }
            parseJsonString(content)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Lỗi đọc file: ${e.message}"))
        }
    }

    fun parseJsonString(jsonContent: String): Result<List<ScheduleItem>> {
        return try {
            val clean = jsonContent.trim()
            val list = mutableListOf<ScheduleItem>()

            if (clean.startsWith("{")) {
                val root = JSONObject(clean)
                val array = root.optJSONArray("schedules")
                    ?: root.optJSONArray("items")
                    ?: JSONArray()

                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    parseItemFromJson(obj)?.let { list.add(it) }
                }
            } else if (clean.startsWith("[")) {
                val array = JSONArray(clean)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    parseItemFromJson(obj)?.let { list.add(it) }
                }
            } else {
                return Result.failure(Exception("Định dạng dữ liệu sao lưu không hợp lệ."))
            }

            if (list.isEmpty()) {
                Result.failure(Exception("Không tìm thấy môn học nào trong dữ liệu sao lưu."))
            } else {
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi phân tích cú pháp JSON: ${e.message}"))
        }
    }

    private fun parseItemFromJson(obj: JSONObject): ScheduleItem? {
        val title = obj.optString("title", "").trim()
        if (title.isBlank()) return null

        return ScheduleItem(
            title = title,
            lecturer = obj.optString("lecturer", ""),
            room = obj.optString("room", ""),
            dayOfWeek = obj.optInt("dayOfWeek", 1),
            startTime = obj.optString("startTime", "08:00"),
            endTime = obj.optString("endTime", "09:30"),
            repeatType = obj.optString("repeatType", "WEEKLY"),
            colorHex = obj.optString("colorHex", "#2563EB"),
            notes = obj.optString("notes", ""),
            reminderMinutesBefore = obj.optInt("reminderMinutesBefore", 15),
            isReminderEnabled = obj.optBoolean("isReminderEnabled", true)
        )
    }
}
