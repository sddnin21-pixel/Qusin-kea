package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ScheduleItem
import com.example.data.repository.ScheduleRepository
import com.example.data.service.GeminiScheduleParser
import com.example.util.NotificationHelper
import com.example.util.PreferencesManager
import com.example.widget.TimetableCountdownWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TimetableUiState(
    val schedules: List<ScheduleItem> = emptyList(),
    val filteredSchedules: List<ScheduleItem> = emptyList(),
    val selectedDay: Int? = null, // null = All days, 1..7 = Monday..Sunday
    val apiKey: String = "",
    val customApiKeyOnly: String = "",
    val isScanning: Boolean = false,
    val scanError: String? = null,
    val extractedItems: List<ScheduleItem> = emptyList(),
    val previewBitmap: Bitmap? = null,
    val batchBitmaps: List<Bitmap> = emptyList(),
    val batchProgressText: String = "",
    val defaultReminderMinutes: Int = 15,
    val isNotificationsEnabled: Boolean = true,
    val snackbarMessage: String? = null,
    val isEditingOrAddingItem: ScheduleItem? = null,
    val isAddingNew: Boolean = false,
    val showApiKeyDialog: Boolean = false,
    val selectedModel: String = PreferencesManager.DEFAULT_MODEL
)

class TimetableViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ScheduleRepository(application)
    private val prefs = PreferencesManager(application)
    private val geminiParser = GeminiScheduleParser()
    private val notificationHelper = NotificationHelper(application)

    private val _uiState = MutableStateFlow(
        TimetableUiState(
            apiKey = prefs.getApiKey(),
            customApiKeyOnly = prefs.getCustomApiKeyOnly(),
            defaultReminderMinutes = prefs.getDefaultReminderMinutes(),
            isNotificationsEnabled = prefs.isNotificationsEnabled(),
            selectedModel = prefs.getSelectedModel()
        )
    )
    val uiState: StateFlow<TimetableUiState> = _uiState.asStateFlow()

    init {
        // Collect schedules from Room
        viewModelScope.launch {
            repository.allSchedules.collect { allItems ->
                _uiState.update { current ->
                    val filtered = if (current.selectedDay == null) {
                        allItems
                    } else {
                        allItems.filter { it.dayOfWeek == current.selectedDay }
                    }
                    current.copy(
                        schedules = allItems,
                        filteredSchedules = filtered
                    )
                }
                // Refresh widget whenever data changes
                TimetableCountdownWidgetProvider.updateAllWidgets(getApplication())
            }
        }

        // Initialize sample data on first launch
        viewModelScope.launch {
            if (prefs.isFirstLaunch()) {
                repository.seedSampleDataIfEmpty()
                prefs.setFirstLaunchCompleted()
                TimetableCountdownWidgetProvider.updateAllWidgets(getApplication())
            }
        }
    }

    fun selectDay(day: Int?) {
        _uiState.update { current ->
            val filtered = if (day == null) {
                current.schedules
            } else {
                current.schedules.filter { it.dayOfWeek == day }
            }
            current.copy(selectedDay = day, filteredSchedules = filtered)
        }
    }

    fun setCustomApiKey(key: String) {
        prefs.setCustomApiKey(key)
        _uiState.update {
            it.copy(
                customApiKeyOnly = key,
                apiKey = prefs.getApiKey(),
                showApiKeyDialog = false
            )
        }
        showSnackbar("Đã lưu API Key thành công!")
    }

    fun setShowApiKeyDialog(show: Boolean) {
        _uiState.update { it.copy(showApiKeyDialog = show) }
    }

    fun setSelectedModel(model: String) {
        prefs.setSelectedModel(model)
        _uiState.update { it.copy(selectedModel = model) }
        val modelName = when (model) {
            "gemini-3.1-flash-lite-preview" -> "Gemini 3.1 Flash Lite"
            "gemini-3.5-flash" -> "Gemini 3.5 Flash"
            "gemini-3.5-flash-lite-preview" -> "Gemini 3.5 Flash Lite"
            "gemini-3.6-flash-preview" -> "Gemini 3.6 Flash"
            else -> model
        }
        showSnackbar("Đã chọn mô hình: $modelName")
    }

    fun openAddDialog() {
        val currentDay = _uiState.value.selectedDay ?: 1
        _uiState.update {
            it.copy(
                isAddingNew = true,
                isEditingOrAddingItem = ScheduleItem(
                    title = "",
                    dayOfWeek = currentDay,
                    reminderMinutesBefore = prefs.getDefaultReminderMinutes()
                )
            )
        }
    }

    fun openEditDialog(item: ScheduleItem) {
        _uiState.update {
            it.copy(
                isAddingNew = false,
                isEditingOrAddingItem = item
            )
        }
    }

    fun closeAddEditDialog() {
        _uiState.update {
            it.copy(
                isEditingOrAddingItem = null,
                isAddingNew = false
            )
        }
    }

    fun saveSchedule(item: ScheduleItem) {
        val isNew = _uiState.value.isAddingNew
        viewModelScope.launch {
            if (isNew) {
                repository.insert(item)
                showSnackbar("Đã thêm môn học: ${item.title}")
            } else {
                repository.update(item)
                showSnackbar("Đã cập nhật môn học: ${item.title}")
            }
            closeAddEditDialog()
            TimetableCountdownWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun deleteSchedule(item: ScheduleItem) {
        viewModelScope.launch {
            repository.delete(item)
            showSnackbar("Đã xóa môn: ${item.title}")
            TimetableCountdownWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun setPreviewBitmapOnly(bitmap: Bitmap?) {
        _uiState.update {
            it.copy(
                previewBitmap = bitmap,
                batchBitmaps = if (bitmap != null) listOf(bitmap) else emptyList(),
                scanError = null
            )
        }
    }

    fun setBatchBitmaps(bitmaps: List<Bitmap>) {
        _uiState.update {
            it.copy(
                batchBitmaps = bitmaps,
                previewBitmap = bitmaps.firstOrNull(),
                scanError = null
            )
        }
    }

    fun scanImage(bitmap: Bitmap) {
        val apiKey = prefs.getApiKey()
        _uiState.update {
            it.copy(
                previewBitmap = bitmap,
                batchBitmaps = listOf(bitmap),
                isScanning = true,
                scanError = null,
                extractedItems = emptyList(),
                batchProgressText = "Đang kết nối Gemini AI Vision..."
            )
        }

        if (apiKey.isBlank()) {
            _uiState.update {
                it.copy(
                    isScanning = false,
                    scanError = "Bạn cần nhập Gemini API Key trước khi quét ảnh!",
                    showApiKeyDialog = true
                )
            }
            return
        }

        viewModelScope.launch {
            val result = geminiParser.parseTimetableImage(
                apiKey = apiKey,
                bitmap = bitmap,
                defaultReminderMinutes = prefs.getDefaultReminderMinutes(),
                preferredModel = _uiState.value.selectedModel
            )
            result.onSuccess { items ->
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        extractedItems = items,
                        batchProgressText = ""
                    )
                }
                showSnackbar("Gemini AI đã tìm thấy ${items.size} môn học!")
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanError = error.message ?: "Không thể phân tích ảnh.",
                        batchProgressText = ""
                    )
                }
            }
        }
    }

    fun scanMultipleImages(bitmaps: List<Bitmap>) {
        if (bitmaps.isEmpty()) return
        if (bitmaps.size == 1) {
            scanImage(bitmaps[0])
            return
        }

        val apiKey = prefs.getApiKey()
        _uiState.update {
            it.copy(
                batchBitmaps = bitmaps,
                previewBitmap = bitmaps.firstOrNull(),
                isScanning = true,
                scanError = null,
                extractedItems = emptyList(),
                batchProgressText = "Bắt đầu phân tích hàng loạt ${bitmaps.size} ảnh..."
            )
        }

        if (apiKey.isBlank()) {
            _uiState.update {
                it.copy(
                    isScanning = false,
                    scanError = "Bạn cần nhập Gemini API Key để quét ảnh!",
                    showApiKeyDialog = true
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    batchProgressText = "Gemini AI đang phân tích toàn bộ ${bitmaps.size} ảnh cùng lúc..."
                )
            }

            val result = geminiParser.parseMultipleTimetableImages(
                apiKey = apiKey,
                bitmaps = bitmaps,
                defaultReminderMinutes = prefs.getDefaultReminderMinutes(),
                preferredModel = _uiState.value.selectedModel
            )

            result.onSuccess { items ->
                val distinctItems = items.distinctBy {
                    "${it.title.lowercase().trim()}_${it.dayOfWeek}_${it.startTime}"
                }
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        extractedItems = distinctItems,
                        batchProgressText = "",
                        scanError = null
                    )
                }
                showSnackbar("Đã bóc tách thành công ${distinctItems.size} môn từ ${bitmaps.size} ảnh!")
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanError = err.message ?: "Không thể phân tích ảnh.",
                        batchProgressText = ""
                    )
                }
            }
        }
    }

    fun scanSampleScreenshotDemo(sampleIndex: Int, bitmap: Bitmap? = null) {
        _uiState.update {
            it.copy(
                previewBitmap = bitmap ?: it.previewBitmap,
                isScanning = true,
                scanError = null,
                extractedItems = emptyList(),
                batchProgressText = "Đang nhận dạng thời khóa biểu từ ảnh mẫu..."
            )
        }

        viewModelScope.launch {
            kotlinx.coroutines.delay(1200)
            val items = geminiParser.getDemoParsedItemsForScreenshot(
                sampleIndex = sampleIndex,
                defaultReminderMinutes = prefs.getDefaultReminderMinutes()
            )
            _uiState.update {
                it.copy(
                    isScanning = false,
                    extractedItems = items,
                    batchProgressText = ""
                )
            }
            showSnackbar("AI đã nhận dạng thành công ${items.size} môn học từ ảnh chụp màn hình!")
        }
    }

    fun confirmExtractedItems(selectedItems: List<ScheduleItem>) {
        if (selectedItems.isEmpty()) return
        viewModelScope.launch {
            repository.insertAll(selectedItems)
            _uiState.update {
                it.copy(
                    extractedItems = emptyList(),
                    previewBitmap = null,
                    batchBitmaps = emptyList()
                )
            }
            showSnackbar("Đã thêm thành công ${selectedItems.size} môn vào thời khóa biểu!")
            TimetableCountdownWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun clearExtractedItems() {
        _uiState.update {
            it.copy(
                extractedItems = emptyList(),
                previewBitmap = null,
                batchBitmaps = emptyList(),
                scanError = null,
                batchProgressText = ""
            )
        }
    }

    fun importBackup(items: List<ScheduleItem>, replaceExisting: Boolean) {
        viewModelScope.launch {
            if (replaceExisting) {
                repository.clearAll()
            }
            repository.insertAll(items)
            showSnackbar("Đã nhập thành công ${items.size} môn học vào thời khóa biểu!")
            TimetableCountdownWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun seedSampleData() {
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
            showSnackbar("Đã nạp thời khóa biểu mẫu.")
            TimetableCountdownWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun clearAllSchedules() {
        viewModelScope.launch {
            repository.clearAll()
            showSnackbar("Đã xóa toàn bộ thời khóa biểu.")
            TimetableCountdownWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun setDefaultReminderMinutes(minutes: Int) {
        prefs.setDefaultReminderMinutes(minutes)
        _uiState.update { it.copy(defaultReminderMinutes = minutes) }
        showSnackbar("Thời gian nhắc nhở: $minutes phút trước giờ học")
    }

    fun toggleNotifications(enabled: Boolean) {
        prefs.setNotificationsEnabled(enabled)
        _uiState.update { it.copy(isNotificationsEnabled = enabled) }
        showSnackbar(if (enabled) "Đã bật nhắc nhở thông báo" else "Đã tắt nhắc nhở thông báo")
    }

    fun triggerInstantTestNotification() {
        notificationHelper.showInstantTestNotification()
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
