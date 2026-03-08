package com.aaditx23.krazyalarm.presentation.screen.alarm_list

import com.aaditx23.krazyalarm.domain.models.Alarm

sealed class UiEvent {
    data class Error(val message: String) : UiEvent()
    data class Success(val message: String) : UiEvent()
    /** Fired after a swipe-to-delete. UI shows "Alarm deleted · Undo". */
    data class AlarmDeleted(val alarm: Alarm) : UiEvent()
}
