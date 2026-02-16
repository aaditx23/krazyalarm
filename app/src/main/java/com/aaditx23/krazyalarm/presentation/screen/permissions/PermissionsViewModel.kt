package com.aaditx23.krazyalarm.presentation.screen.permissions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aaditx23.krazyalarm.data.util.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PermissionsViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val _permissionsState = MutableStateFlow(PermissionsState())
    val permissionsState: StateFlow<PermissionsState> = _permissionsState.asStateFlow()

    init {
        checkPermissions()
    }

    fun checkPermissions() {
        viewModelScope.launch {
            val notificationGranted = PermissionUtils.hasNotificationPermission(context)
            val alarmGranted = PermissionUtils.canScheduleExactAlarms(context)

            _permissionsState.value = PermissionsState(
                hasNotificationPermission = notificationGranted,
                hasAlarmPermission = alarmGranted,
                allPermissionsGranted = notificationGranted && alarmGranted
            )
        }
    }
}

data class PermissionsState(
    val hasNotificationPermission: Boolean = false,
    val hasAlarmPermission: Boolean = false,
    val allPermissionsGranted: Boolean = false
)

