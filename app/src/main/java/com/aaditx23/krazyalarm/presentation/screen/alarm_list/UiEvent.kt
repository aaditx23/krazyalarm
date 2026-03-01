package com.aaditx23.krazyalarm.presentation.screen.alarm_list

sealed class UiEvent {
    data class Error(val message: String) : UiEvent()
    data class Success(val message: String) : UiEvent()
}
