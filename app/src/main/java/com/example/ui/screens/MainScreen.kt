package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.dialogs.AddEditScheduleDialog
import com.example.ui.dialogs.ApiKeyDialog
import com.example.ui.viewmodel.TimetableViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: TimetableViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (currentTab) {
                                0 -> "Thời Khóa Biểu"
                                1 -> "Quét Lịch AI"
                                else -> "Cài Đặt & Đồng Bộ"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.setShowApiKeyDialog(true) },
                        modifier = Modifier.testTag("top_bar_key_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Cấu hình API Key",
                            tint = if (state.apiKey.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = "Lịch Học"
                        )
                    },
                    label = { Text("Lịch Học") },
                    modifier = Modifier.testTag("nav_timetable")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "Quét AI"
                        )
                    },
                    label = { Text("Quét AI") },
                    modifier = Modifier.testTag("nav_scan_ai")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Cài Đặt"
                        )
                    },
                    label = { Text("Cài Đặt") },
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        },
        floatingActionButton = {
            if (currentTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openAddDialog() },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Thêm Môn") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_schedule")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { tab ->
                when (tab) {
                    0 -> TimetableScreen(
                        state = state,
                        viewModel = viewModel,
                        onNavigateToScan = { currentTab = 1 }
                    )
                    1 -> ScanScheduleScreen(
                        state = state,
                        viewModel = viewModel,
                        onNavigateToTimetable = { currentTab = 0 }
                    )
                    2 -> SettingsScreen(
                        state = state,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // Add / Edit Schedule Dialog
    state.isEditingOrAddingItem?.let { item ->
        AddEditScheduleDialog(
            initialItem = item,
            isNew = state.isAddingNew,
            onDismiss = { viewModel.closeAddEditDialog() },
            onSave = { updated -> viewModel.saveSchedule(updated) }
        )
    }

    // API Key Dialog
    if (state.showApiKeyDialog) {
        ApiKeyDialog(
            initialKey = state.customApiKeyOnly,
            onDismiss = { viewModel.setShowApiKeyDialog(false) },
            onSave = { key -> viewModel.setCustomApiKey(key) }
        )
    }
}
