package com.aaditx23.krazyalarm.di

import com.aaditx23.krazyalarm.presentation.screen.alarm_list.AlarmListViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module


val presentationModule = module {

    // ViewModels
    viewModelOf(::AlarmListViewModel)
}
