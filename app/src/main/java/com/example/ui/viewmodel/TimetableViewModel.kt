package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ScheduleItem
import com.example.data.repository.ScheduleRepository
import com.example.data.service.GeminiScheduleParser
import com.example.util.PreferencesManager
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
    val defaultReminderMinutes: Int = 15,
    val isNotificationsEnabled: Boolean = true,
    val snackbarMessage: String? = null,
    val isEditingOrAddingItem: ScheduleItem? = null,
    val isAddingNew: Boolean = false,
    val showApiKeyDialog: Boolean = false
)

class TimetableViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ScheduleRepository(application)
    private val prefs = PreferencesManager(application)
    private val geminiParser = GeminiScheduleParser()

    private val _uiState = MutableStateFlow(
        TimetableUiState(
            apiKey = prefs.getApiKey(),
            customApiKeyOnly = prefs.getCustomApiKeyOnly(),
            defaultReminderMinutes = prefs.getDefaultReminderMinutes(),
            isNotificationsEnabled = prefs.isNotificationsEnabled()
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
            }
        }

        // Initialize sample data on first launch
        viewModelScope.launch {
            if (prefs.isFirstLaunch()) {
                repository.seedSampleDataIfEmpty()
                prefs.setFirstLaunchCompleted()
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
        }
    }

    fun deleteSchedule(item: ScheduleItem) {
        viewModelScope.launch {
            repository.delete(item)
            showSnackbar("Đã xóa môn: ${item.title}")
        }
    }

    fun scanImage(bitmap: Bitmap) {
        val apiKey = prefs.getApiKey()
        _uiState.update {
            it.copy(
                previewBitmap = bitmap,
                isScanning = true,
                scanError = null,
                extractedItems = emptyList()
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
                defaultReminderMinutes = prefs.getDefaultReminderMinutes()
            )
            result.onSuccess { items ->
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        extractedItems = items
                    )
                }
                showSnackbar("Gemini AI đã tìm thấy ${items.size} môn học!")
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanError = error.message ?: "Không thể phân tích ảnh."
                    )
                }
            }
        }
    }

    fun confirmExtractedItems(selectedItems: List<ScheduleItem>) {
        if (selectedItems.isEmpty()) return
        viewModelScope.launch {
            repository.insertAll(selectedItems)
            _uiState.update {
                it.copy(
                    extractedItems = emptyList(),
                    previewBitmap = null
                )
            }
            showSnackbar("Đã thêm thành công ${selectedItems.size} môn vào thời khóa biểu!")
        }
    }

    fun clearExtractedItems() {
        _uiState.update {
            it.copy(
                extractedItems = emptyList(),
                previewBitmap = null,
                scanError = null
            )
        }
    }

    fun seedSampleData() {
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
            showSnackbar("Đã nạp thời khóa biểu mẫu.")
        }
    }

    fun clearAllSchedules() {
        viewModelScope.launch {
            repository.clearAll()
            showSnackbar("Đã xóa toàn bộ thời khóa biểu.")
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

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
