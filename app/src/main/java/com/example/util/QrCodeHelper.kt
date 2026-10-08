package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.model.RepeatType
import com.example.data.model.ScheduleItem
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONArray
import org.json.JSONObject

object QrCodeHelper {

    private const val PREFIX_TIMETABLE = "SMART_TKB:"

    /**
     * Tạo Bitmap mã QR từ chuỗi dữ liệu
     */
    fun generateQrBitmap(
        content: String,
        width: Int = 600,
        height: Int = 600,
        fgColor: Int = Color.BLACK,
        bgColor: Int = Color.WHITE
    ): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height)
        val matrixWidth = bitMatrix.width
        val matrixHeight = bitMatrix.height
        val pixels = IntArray(matrixWidth * matrixHeight)

        for (y in 0 until matrixHeight) {
            val offset = y * matrixWidth
            for (x in 0 until matrixWidth) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) fgColor else bgColor
            }
        }

        val bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, matrixWidth, 0, 0, matrixWidth, matrixHeight)
        return bitmap
    }

    /**
     * Nén danh sách thời khóa biểu thành chuỗi mã QR gọn nhẹ
     */
    fun encodeTimetable(items: List<ScheduleItem>): String {
        val jsonArray = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("t", item.title)
            obj.put("l", item.lecturer)
            obj.put("r", item.room)
            obj.put("d", item.dayOfWeek)
            obj.put("s", item.startTime)
            obj.put("e", item.endTime)
            obj.put("c", item.colorHex)
            if (item.notes.isNotBlank()) obj.put("n", item.notes)
            jsonArray.put(obj)
        }
        return PREFIX_TIMETABLE + jsonArray.toString()
    }

    /**
     * Giải mã chuỗi QR thành danh sách môn học
     */
    fun decodeTimetable(rawText: String): List<ScheduleItem>? {
        val cleanText = rawText.trim()
        val jsonString = if (cleanText.startsWith(PREFIX_TIMETABLE)) {
            cleanText.removePrefix(PREFIX_TIMETABLE)
        } else if (cleanText.startsWith("[") && cleanText.endsWith("]")) {
            cleanText
        } else {
            return null
        }

        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<ScheduleItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val title = obj.optString("t", obj.optString("title", "Môn học"))
                val lecturer = obj.optString("l", obj.optString("lecturer", ""))
                val room = obj.optString("r", obj.optString("room", ""))
                val dayOfWeek = obj.optInt("d", obj.optInt("dayOfWeek", 1)).coerceIn(1, 7)
                val startTime = obj.optString("s", obj.optString("startTime", "08:00"))
                val endTime = obj.optString("e", obj.optString("endTime", "09:30"))
                val colorHex = obj.optString("c", obj.optString("colorHex", "#2563EB"))
                val notes = obj.optString("n", obj.optString("notes", ""))

                list.add(
                    ScheduleItem(
                        title = title,
                        lecturer = lecturer,
                        room = room,
                        dayOfWeek = dayOfWeek,
                        startTime = startTime,
                        endTime = endTime,
                        colorHex = colorHex,
                        notes = notes,
                        repeatType = RepeatType.WEEKLY.name
                    )
                )
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Quét và đọc mã QR từ Bitmap (ảnh chụp màn hình hoặc tải từ thư viện ảnh)
     */
    fun decodeQrFromBitmap(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val hints = mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true
            )
            val reader = MultiFormatReader()
            val result = reader.decode(binaryBitmap, hints)
            result.text
        } catch (e: Exception) {
            null
        }
    }
}
