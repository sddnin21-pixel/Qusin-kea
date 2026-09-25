package com.example.data.service

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.data.model.RepeatType
import com.example.data.model.ScheduleItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    companion object {
        data class ModelOption(
            val id: String,
            val displayName: String,
            val badge: String,
            val description: String
        )

        val SUPPORTED_MODELS = listOf(
            ModelOption(
                id = "gemini-3.1-flash-lite-preview",
                displayName = "Gemini 3.1 Flash Lite",
                badge = "Khuyên dùng",
                description = "Tốc độ nhanh nhất, cực ít nghẽn tải, xử lý đa ảnh tức thì"
            ),
            ModelOption(
                id = "gemini-3.5-flash",
                displayName = "Gemini 3.5 Flash",
                badge = "Trí tuệ cao",
                description = "Khả năng suy luận & đọc hiểu bảng biểu biểu đồ xuất sắc"
            ),
            ModelOption(
                id = "gemini-3.5-flash-lite-preview",
                displayName = "Gemini 3.5 Flash Lite",
                badge = "Tối ưu",
                description = "Phiên bản rút gọn cân bằng giữa độ chính xác và độ trễ"
            ),
            ModelOption(
                id = "gemini-3.6-flash-preview",
                displayName = "Gemini 3.6 Flash",
                badge = "Mới nhất",
                description = "Thế hệ mô hình thị giác AI tiên tiến nhất hiện nay"
            )
        )
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    // Highly available fallback chain prioritizing user models
    fun getModelFallbackChain(preferredModel: String): List<String> {
        val list = mutableListOf<String>()
        if (preferredModel.isNotBlank()) {
            list.add(preferredModel)
        }
        val userModels = listOf(
            "gemini-3.1-flash-lite-preview",
            "gemini-3.5-flash",
            "gemini-3.5-flash-lite-preview",
            "gemini-3.6-flash-preview"
        )
        for (m in userModels) {
            if (!list.contains(m)) list.add(m)
        }
        // Background backup fallbacks
        for (m in listOf("gemini-flash-latest", "gemini-2.5-flash")) {
            if (!list.contains(m)) list.add(m)
        }
        return list
    }

    private val colorPalette = listOf(
        "#2563EB", "#10B981", "#8B5CF6", "#F59E0B",
        "#EC4899", "#06B6D4", "#EF4444", "#14B8A6",
        "#6366F1", "#84CC16", "#D946EF", "#0EA5E9"
    )

    private val extractionPrompt = """
Bạn là một trợ lý AI thông minh chuyên phân tích ảnh chụp màn hình thời khóa biểu học tập, lịch học thêm, lịch tự học hoặc thời gian biểu sinh hoạt/học tập cá nhân (kể cả ảnh chụp bảng biểu, danh sách giờ học, ghi chú lịch học theo thứ trong tuần).

Nhiệm vụ:
Phân tích kỹ lưỡng ảnh (hoặc các ảnh) được cung cấp và trích xuất TOÀN BỘ các môn học, buổi học, hoạt động học tập hoặc sinh hoạt có khung giờ.
(Ví dụ: Từ vựng Anh, Học trên lớp, Học thêm Toán, Lý bài tập, Đi học chiều, Anh - Nghe + Nói, Lập trình, v.v.).

Với mỗi hoạt động/môn học:
1. "title": Tên môn học hoặc hoạt động (ví dụ: "Từ vựng Anh", "Học thêm Toán (chiều)", "Lý (bài tập)", "Học trên lớp").
2. "lecturer": Tên giảng viên / giáo viên / ghi chú thêm (nếu không có thì để trống "").
3. "room": Phòng học hoặc hình thức học (ví dụ: "Trên lớp", "Học thêm", "Phòng A101", "Tự học").
4. "dayOfWeek": Thứ trong tuần từ 1 đến 7:
   - 1 = Thứ Hai
   - 2 = Thứ Ba
   - 3 = Thứ Tư
   - 4 = Thứ Năm
   - 5 = Thứ Sáu
   - 6 = Thứ Bảy
   - 7 = Chủ Nhật
   Nếu trên ảnh có ghi tiêu đề "THỨ 2", "THỨ 3",... thì hãy gán chính xác theo thứ đó.
5. "startTime": Giờ bắt đầu định dạng 24h "HH:mm" (ví dụ: "06:00", "06:45", "12:40", "14:00", "18:30"). Nếu chỉ có 1 mốc giờ (như "13:20"), hãy dùng làm startTime.
6. "endTime": Giờ kết thúc định dạng 24h "HH:mm" (ví dụ: "06:45", "11:15", "13:15", "16:25", "20:30"). Nếu không ghi giờ kết thúc, hãy tính endTime = startTime + 45 phút.
7. "repeatType": "WEEKLY" (lặp lại hàng tuần).
8. "notes": Ghi chú thêm nếu có (ví dụ: "Tự học", "Bài tập", "Nghe + Nói").

Trả về ĐÚNG MỘT MẢNG JSON chuẩn:
[
  {
    "title": "Từ vựng Anh",
    "lecturer": "",
    "room": "Tự học",
    "dayOfWeek": 1,
    "startTime": "12:40",
    "endTime": "13:15",
    "repeatType": "WEEKLY",
    "notes": "Học từ vựng"
  }
]
    """.trimIndent()

    suspend fun parseTimetableImage(
        apiKey: String,
        bitmap: Bitmap,
        defaultReminderMinutes: Int = 15,
        preferredModel: String = "gemini-3.1-flash-lite-preview"
    ): Result<List<ScheduleItem>> = parseMultipleTimetableImages(apiKey, listOf(bitmap), defaultReminderMinutes, preferredModel)

    suspend fun parseMultipleTimetableImages(
        apiKey: String,
        bitmaps: List<Bitmap>,
        defaultReminderMinutes: Int = 15,
        preferredModel: String = "gemini-3.1-flash-lite-preview"
    ): Result<List<ScheduleItem>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Chưa cấu hình Gemini API Key. Vui lòng nhập API Key trong phần Cài đặt.")
            )
        }
        if (bitmaps.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Không có ảnh nào để phân tích."))
        }

        try {
            // Compress all bitmaps to base64 JPEG
            val base64Images = bitmaps.map { bmp ->
                val scaled = if (bmp.width > 1200 || bmp.height > 1200) {
                    val ratio = 1200f / Math.max(bmp.width, bmp.height)
                    Bitmap.createScaledBitmap(bmp, (bmp.width * ratio).toInt(), (bmp.height * ratio).toInt(), true)
                } else {
                    bmp
                }
                val stream = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            }

            // Build Multimodal Request JSON containing all images
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            // Text instructions
                            put(JSONObject().apply { put("text", extractionPrompt) })

                            // All images in single request
                            for (base64Img in base64Images) {
                                put(JSONObject().apply {
                                    put("inlineData", JSONObject().apply {
                                        put("mimeType", "image/jpeg")
                                        put("data", base64Img)
                                    })
                                })
                            }
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("responseMimeType", "application/json")
                })
            }

            val requestBodyStr = requestJson.toString()
            var lastError = "Không thể kết nối đến Gemini API"

            val modelsToTry = getModelFallbackChain(preferredModel)
            Log.d("GeminiScheduleParser", "Starting parse with models: $modelsToTry")

            for (model in modelsToTry) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                    val requestBody = requestBodyStr.toRequestBody("application/json; charset=utf-8".toMediaType())

                    val httpRequest = Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build()

                    val response = okHttpClient.newCall(httpRequest).execute()
                    val responseBody = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        val parsedItems = parseGeminiResponse(responseBody, defaultReminderMinutes)
                        if (parsedItems.isNotEmpty()) {
                            Log.d("GeminiScheduleParser", "Successfully parsed ${parsedItems.size} items using model: $model")
                            return@withContext Result.success(parsedItems)
                        }
                    } else {
                        val errorMsg = try {
                            val errJson = JSONObject(responseBody)
                            errJson.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                        } catch (e: Exception) {
                            "HTTP ${response.code}: $responseBody"
                        }
                        Log.w("GeminiScheduleParser", "Model $model returned error: $errorMsg")
                        lastError = errorMsg

                        // If high demand or rate limit, wait 1.2s and try next fallback model
                        if (errorMsg.contains("demand", ignoreCase = true) ||
                            errorMsg.contains("quota", ignoreCase = true) ||
                            response.code == 429 || response.code == 503) {
                            delay(1200)
                        }
                    }
                } catch (e: Exception) {
                    Log.w("GeminiScheduleParser", "Exception with model $model: ${e.message}")
                    lastError = e.message ?: "Lỗi kết nối mạng"
                }
            }

            // Friendly error message for high demand
            val friendlyError = if (lastError.contains("demand", ignoreCase = true) || lastError.contains("overloaded", ignoreCase = true)) {
                "Máy chủ Gemini đang quá tải tạm thời từ Google. Hệ thống đã thử các mô hình dự phòng nhưng chưa được phản hồi. Bạn hãy nhấn 'Thử Lại' sau 10 giây hoặc chọn 'Thử nghiệm Demo' để xem kết quả."
            } else {
                "Lỗi Gemini API: $lastError"
            }

            return@withContext Result.failure(Exception(friendlyError))
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

            // Standardize format to HH:mm (e.g. 6:00 -> 06:00)
            val startParts = startTime.split(":")
            if (startParts.size == 2) {
                startTime = "%02d:%02d".format(startParts[0].toIntOrNull() ?: 8, startParts[1].toIntOrNull() ?: 0)
            }
            val endParts = endTime.split(":")
            if (endParts.size == 2) {
                endTime = "%02d:%02d".format(endParts[0].toIntOrNull() ?: 9, endParts[1].toIntOrNull() ?: 30)
            }

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
                    title = "Học trên lớp",
                    lecturer = "Giáo viên chủ nhiệm",
                    room = "Trường học",
                    dayOfWeek = 1,
                    startTime = "06:45",
                    endTime = "11:15",
                    repeatType = RepeatType.WEEKLY.name,
                    colorHex = "#2563EB",
                    notes = "Học chính khóa buổi sáng",
                    reminderMinutesBefore = defaultReminderMinutes
                ),
                ScheduleItem(
                    title = "Từ vựng Anh",
                    lecturer = "",
                    room = "Tự học",
                    dayOfWeek = 1,
                    startTime = "12:40",
                    endTime = "13:15",
                    repeatType = RepeatType.WEEKLY.name,
                    colorHex = "#10B981",
                    notes = "Ôn 30 từ mới hàng ngày",
                    reminderMinutesBefore = defaultReminderMinutes
                ),
                ScheduleItem(
                    title = "Học thêm Toán (chiều)",
                    lecturer = "Thầy dạy thêm",
                    room = "Lớp học thêm",
                    dayOfWeek = 1,
                    startTime = "14:00",
                    endTime = "16:25",
                    repeatType = RepeatType.WEEKLY.name,
                    colorHex = "#8B5CF6",
                    notes = "Luyện đề thi tuyển sinh",
                    reminderMinutesBefore = defaultReminderMinutes
                ),
                ScheduleItem(
                    title = "Lý (bài tập)",
                    lecturer = "",
                    room = "Góc học tập",
                    dayOfWeek = 1,
                    startTime = "17:00",
                    endTime = "18:00",
                    repeatType = RepeatType.WEEKLY.name,
                    colorHex = "#F59E0B",
                    notes = "Làm bài tập sách bài tập",
                    reminderMinutesBefore = defaultReminderMinutes
                ),
                ScheduleItem(
                    title = "Anh - Nghe + Nói",
                    lecturer = "Ms. Sarah",
                    room = "Online",
                    dayOfWeek = 1,
                    startTime = "18:30",
                    endTime = "20:30",
                    repeatType = RepeatType.WEEKLY.name,
                    colorHex = "#EC4899",
                    notes = "Luyện phản xạ giao tiếp",
                    reminderMinutesBefore = defaultReminderMinutes
                ),
                ScheduleItem(
                    title = "Toán (tự học)",
                    lecturer = "",
                    room = "Tự học",
                    dayOfWeek = 2,
                    startTime = "13:30",
                    endTime = "14:30",
                    repeatType = RepeatType.WEEKLY.name,
                    colorHex = "#06B6D4",
                    notes = "Luyện giải bài tập Toán hình",
                    reminderMinutesBefore = defaultReminderMinutes
                ),
                ScheduleItem(
                    title = "Lý (tự học)",
                    lecturer = "",
                    room = "Tự học",
                    dayOfWeek = 2,
                    startTime = "14:45",
                    endTime = "15:45",
                    repeatType = RepeatType.WEEKLY.name,
                    colorHex = "#14B8A6",
                    notes = "Ôn lý thuyết Quang học",
                    reminderMinutesBefore = defaultReminderMinutes
                ),
                ScheduleItem(
                    title = "Ngữ Văn",
                    lecturer = "Cô Mai",
                    room = "Bàn học",
                    dayOfWeek = 2,
                    startTime = "16:00",
                    endTime = "17:00",
                    repeatType = RepeatType.WEEKLY.name,
                    colorHex = "#EF4444",
                    notes = "Soạn bài và đọc tác phẩm",
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
