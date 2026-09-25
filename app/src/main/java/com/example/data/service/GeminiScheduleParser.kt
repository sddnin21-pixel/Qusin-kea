package com.example.data.service

import android.graphics.Bitmap
import android.util.Base64
import com.example.data.model.RepeatType
import com.example.data.model.ScheduleItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiScheduleParser {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val colorPalette = listOf(
        "#2563EB", "#10B981", "#8B5CF6", "#F59E0B",
        "#EC4899", "#06B6D4", "#EF4444", "#14B8A6"
    )

    suspend fun parseTimetableImage(
        apiKey: String,
        bitmap: Bitmap,
        defaultReminderMinutes: Int = 15
    ): Result<List<ScheduleItem>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Chưa cấu hình Gemini API Key. Vui lòng nhập API Key trong phần Cài đặt.")
            )
        }

        try {
            // Compress bitmap to JPEG base64 (resize if too large to optimize speed)
            val scaledBitmap = if (bitmap.width > 1600 || bitmap.height > 1600) {
                val ratio = 1600f / Math.max(bitmap.width, bitmap.height)
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * ratio).toInt(),
                    (bitmap.height * ratio).toInt(),
                    true
                )
            } else {
                bitmap
            }

            val byteArrayOutputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
            val base64Image = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)

            val prompt = """
Bạn là một trợ lý AI thông minh chuyên phân tích ảnh thời khóa biểu học tập, lịch công tác, lịch giảng đường hoặc thời gian biểu sinh viên (kể cả ảnh chụp bảng, màn hình điện thoại/máy tính hoặc chữ viết tay).

Nhiệm vụ:
Phân tích kỹ lưỡng ảnh thời khóa biểu này và trích xuất tất cả các môn học, buổi học hoặc sự kiện định kỳ.
Với mỗi môn học/buổi học, hãy trích xuất:
1. "title": Tên môn học hoặc sự kiện (ví dụ: Toán Cao Cấp, Lập Trình Web, Giáo Dục Thể Chất, Tiếng Anh).
2. "lecturer": Tên giảng viên hoặc giáo viên (nếu tìm thấy, nếu không có để trống "").
3. "room": Phòng học hoặc giảng đường hoặc link học (ví dụ: A302, Hội trường B, Zoom, Lab 1...).
4. "dayOfWeek": Thứ trong tuần biểu diễn bằng số nguyên từ 1 đến 7:
   - 1 = Thứ Hai (Monday)
   - 2 = Thứ Ba (Tuesday)
   - 3 = Thứ Tư (Wednesday)
   - 4 = Thứ Năm (Thursday)
   - 5 = Thứ Sáu (Friday)
   - 6 = Thứ Bảy (Saturday)
   - 7 = Chủ Nhật (Sunday)
5. "startTime": Giờ bắt đầu định dạng 24h "HH:mm" (ví dụ: "07:30", "08:00", "13:30"). Nếu thời khóa biểu ghi theo tiết (tiết 1, tiết 2...), hãy quy đổi sang giờ học tiêu chuẩn (ví dụ Tiết 1: 07:00, Tiết 2: 07:50, Tiết 7: 12:30, Tiết 8: 13:20).
6. "endTime": Giờ kết thúc định dạng 24h "HH:mm" (ví dụ: "09:15", "11:30", "15:00").
7. "repeatType": Kiểu lặp lại: "WEEKLY" (mặc định hàng tuần), hoặc "MONTHLY" (hàng tháng), hoặc "YEARLY" (hàng năm).
8. "notes": Ghi chú thêm nếu có (ví dụ: tuần chẵn/lẻ, nhóm thực hành, tài liệu).

Hãy trả về một mảng JSON các đối tượng. Ví dụ:
[
  {
    "title": "Toán Giải Tích",
    "lecturer": "TS. Nguyễn Văn A",
    "room": "B204",
    "dayOfWeek": 1,
    "startTime": "07:30",
    "endTime": "09:30",
    "repeatType": "WEEKLY",
    "notes": "Tuần học 1-15"
  }
]
            """.trimIndent()

            // Build Gemini REST Request JSON
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            // Text prompt
                            put(JSONObject().apply { put("text", prompt) })
                            // Multimodal image part
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val httpRequest = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(responseBody)
                    errJson.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                } catch (e: Exception) {
                    "HTTP ${response.code}: $responseBody"
                }
                return@withContext Result.failure(Exception("Lỗi Gemini API: $errorMsg"))
            }

            val parsedItems = parseGeminiResponse(responseBody, defaultReminderMinutes)
            if (parsedItems.isEmpty()) {
                return@withContext Result.failure(
                    Exception("Không nhận diện được môn học nào từ ảnh. Hãy chụp rõ hơn hoặc cắt gọn phần thời khóa biểu.")
                )
            }

            Result.success(parsedItems)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Lỗi xử lý ảnh: ${e.localizedMessage ?: e.message}"))
        }
    }

    private fun parseGeminiResponse(
        responseJsonStr: String,
        defaultReminderMinutes: Int
    ): List<ScheduleItem> {
        val items = mutableListOf<ScheduleItem>()
        val responseObj = JSONObject(responseJsonStr)
        val candidates = responseObj.optJSONArray("candidates") ?: return emptyList()
        val firstCandidate = candidates.optJSONObject(0) ?: return emptyList()
        val content = firstCandidate.optJSONObject("content") ?: return emptyList()
        val parts = content.optJSONArray("parts") ?: return emptyList()
        val text = parts.optJSONObject(0)?.optString("text") ?: ""

        // Extract JSON array
        var cleanJson = text.trim()
        if (cleanJson.startsWith("```json")) {
            cleanJson = cleanJson.substring(7)
        } else if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.substring(3)
        }
        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.substring(0, cleanJson.length - 3)
        }
        cleanJson = cleanJson.trim()

        val jsonArray = try {
            if (cleanJson.startsWith("[")) {
                JSONArray(cleanJson)
            } else if (cleanJson.startsWith("{")) {
                val obj = JSONObject(cleanJson)
                // Might have a key like "schedules" or "classes"
                obj.optJSONArray("schedules")
                    ?: obj.optJSONArray("classes")
                    ?: obj.optJSONArray("items")
                    ?: JSONArray().apply { put(obj) }
            } else {
                return emptyList()
            }
        } catch (e: Exception) {
            return emptyList()
        }

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            val title = obj.optString("title", "").trim()
            if (title.isBlank()) continue

            val lecturer = obj.optString("lecturer", "").trim()
            val room = obj.optString("room", "").trim()

            var dayOfWeek = obj.optInt("dayOfWeek", 1)
            if (dayOfWeek !in 1..7) {
                // Try reading string day
                val dayStr = obj.optString("dayOfWeek", "").lowercase()
                dayOfWeek = when {
                    dayStr.contains("hai") || dayStr.contains("mon") || dayStr == "2" -> 1
                    dayStr.contains("ba") || dayStr.contains("tue") || dayStr == "3" -> 2
                    dayStr.contains("tư") || dayStr.contains("wed") || dayStr == "4" -> 3
                    dayStr.contains("năm") || dayStr.contains("thu") || dayStr == "5" -> 4
                    dayStr.contains("sáu") || dayStr.contains("fri") || dayStr == "6" -> 5
                    dayStr.contains("bảy") || dayStr.contains("sat") || dayStr == "7" -> 6
                    dayStr.contains("nhật") || dayStr.contains("sun") || dayStr == "8" -> 7
                    else -> 1
                }
            }

            var startTime = obj.optString("startTime", "08:00").trim()
            var endTime = obj.optString("endTime", "09:30").trim()

            if (!startTime.contains(":")) startTime = "08:00"
            if (!endTime.contains(":")) endTime = "09:30"

            val repeatTypeRaw = obj.optString("repeatType", RepeatType.WEEKLY.name).uppercase()
            val repeatType = when {
                repeatTypeRaw.contains("MONTH") -> RepeatType.MONTHLY.name
                repeatTypeRaw.contains("YEAR") -> RepeatType.YEARLY.name
                repeatTypeRaw.contains("NONE") -> RepeatType.NONE.name
                else -> RepeatType.WEEKLY.name
            }

            val notes = obj.optString("notes", "").trim()
            val colorHex = colorPalette[i % colorPalette.size]

            items.add(
                ScheduleItem(
                    title = title,
                    lecturer = lecturer,
                    room = room,
                    dayOfWeek = dayOfWeek,
                    startTime = startTime,
                    endTime = endTime,
                    repeatType = repeatType,
                    colorHex = colorHex,
                    notes = notes,
                    reminderMinutesBefore = defaultReminderMinutes,
                    isReminderEnabled = true
                )
            )
        }

        return items
    }

    fun getDemoParsedItemsForScreenshot(sampleIndex: Int, defaultReminderMinutes: Int): List<ScheduleItem> {
        return if (sampleIndex == 1) {
            listOf(
                ScheduleItem(
                    title = "Toán Cao Cấp & Giải Tích",
                    lecturer = "PGS.TS. Trần Đình Nam",
                    room = "B204",
                    dayOfWeek = 1,
                    startTime = "07:30",
                    endTime = "09:30",
                    repeatType = RepeatType.WEEKLY.name,
                    colorHex = "#2563EB",
                    notes = "Mang tài liệu bài giảng phần Đạo hàm",
                    reminderMinutesBefore = defaultReminderMinutes
                ),
                ScheduleItem(
                    title = "Lập Trình Di Động (Android Kotlin)",
                    lecturer = "ThS. Hoàng Quốc Bảo",
                    room = "Lab 4.2",
                    dayOfWeek = 2,
                    startTime = "09:45",
                    endTime = "11:45",
                    repeatType = RepeatType.WEEKLY.name,
                    colorHex = "#10B981",
                    notes = "Mang laptop đã cài Android Studio",
                    reminderMinutesBefore = defaultReminderMinutes
                ),
                ScheduleItem(
                    title = "Tiếng Anh Chuyên Ngành CNTT",
                    lecturer = "Ms. Sarah Jenkins",
                    room = "A101",
                    dayOfWeek = 3,
                    startTime = "13:30",
                    endTime = "15:30",
                    repeatType = RepeatType.WEEKLY.name,
                    colorHex = "#8B5CF6",
                    notes = "Thuyết trình nhóm Unit 4",
                    reminderMinutesBefore = defaultReminderMinutes
                ),
                ScheduleItem(
                    title = "Cấu Trúc Dữ Liệu & Giải Thuật",
                    lecturer = "TS. Lê Quang Huy",
                    room = "C302",
                    dayOfWeek = 4,
                    startTime = "08:00",
                    endTime = "10:30",
                    repeatType = RepeatType.WEEKLY.name,
                    colorHex = "#F59E0B",
                    notes = "Kiểm tra giữa kỳ phần Cây nhị phân",
                    reminderMinutesBefore = defaultReminderMinutes
                ),
                ScheduleItem(
                    title = "An Toàn & Bảo Mật Hệ Thống",
                    lecturer = "ThS. Vũ Hải Đăng",
                    room = "Hội trường 1",
                    dayOfWeek = 5,
                    startTime = "14:00",
                    endTime = "16:30",
                    repeatType = RepeatType.WEEKLY.name,
                    colorHex = "#EC4899",
                    notes = "Thực hành mã hóa AES & RSA",
                    reminderMinutesBefore = defaultReminderMinutes
                )
            )
        } else {
            listOf(
                ScheduleItem(
                    title = "Cơ Sở Dữ Liệu & SQL",
                    lecturer = "TS. Nguyễn Minh Châu",
                    room = "D3-201",
                    dayOfWeek = 1,
                    startTime = "08:00",
                    endTime = "10:15",
                    repeatType = RepeatType.WEEKLY.name,
                    colorHex = "#06B6D4",
                    notes = "Bài tập chuẩn hóa 3NF",
                    reminderMinutesBefore = defaultReminderMinutes
                ),
                ScheduleItem(
                    title = "Mạng Máy Tính & Viễn Thông",
                    lecturer = "ThS. Đỗ Thanh Tùng",
                    room = "D3-105",
                    dayOfWeek = 2,
                    startTime = "13:00",
                    endTime = "15:15",
                    repeatType = RepeatType.WEEKLY.name,
                    colorHex = "#14B8A6",
                    notes = "Cấu hình Router Cisco Packet Tracer",
                    reminderMinutesBefore = defaultReminderMinutes
                ),
                ScheduleItem(
                    title = "Kiến Trúc Máy Tính & HĐH",
                    lecturer = "PGS.TS. Phạm Văn Hòa",
                    room = "D1-302",
                    dayOfWeek = 4,
                    startTime = "07:30",
                    endTime = "09:45",
                    repeatType = RepeatType.WEEKLY.name,
                    colorHex = "#EF4444",
                    notes = "Lập trình đa luồng POSIX threads",
                    reminderMinutesBefore = defaultReminderMinutes
                ),
                ScheduleItem(
                    title = "Trí Tuệ Nhân Tạo Ứng Dụng",
                    lecturer = "TS. Bùi Gia Đức",
                    room = "Phòng AI Lab",
                    dayOfWeek = 5,
                    startTime = "09:30",
                    endTime = "11:45",
                    repeatType = RepeatType.WEEKLY.name,
                    colorHex = "#8B5CF6",
                    notes = "Tìm hiểu LLM Gemini & Prompt Engineering",
                    reminderMinutesBefore = defaultReminderMinutes
                )
            )
        }
    }
}
