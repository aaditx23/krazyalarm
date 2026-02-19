package com.aaditx23.krazyalarm.di

import com.aaditx23.krazyalarm.presentation.screen.alarm_list.AlarmListViewModel
import com.aaditx23.krazyalarm.presentation.screen.DetailsModal.DetailsModalViewModel
import com.aaditx23.krazyalarm.presentation.screen.alarm_ringing.AlarmRingingViewModel
import com.aaditx23.krazyalarm.presentation.screen.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module


val presentationModule = module {

    // ViewModels
    viewModelOf(::AlarmListViewModel)
    viewModelOf(::DetailsModalViewModel)
    viewModelOf(::SettingsViewModel)

    // AlarmRingingViewModel with parameter
    viewModel { (alarmId: Long) ->
        AlarmRingingViewModel(
            alarmId = alarmId,
            alarmRepository = get(),
            settingsRepository = get()
        )
    }

}
