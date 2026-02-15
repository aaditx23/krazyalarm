package com.aaditx23.krazyalarm.di

import com.aaditx23.krazyalarm.data.local.database.AlarmDatabase
import com.aaditx23.krazyalarm.data.local.preferences.SettingsDataStore
import com.aaditx23.krazyalarm.data.repository.AlarmRepositoryImpl
import com.aaditx23.krazyalarm.data.repository.SettingsRepositoryImpl
import com.aaditx23.krazyalarm.domain.repository.AlarmRepository
import com.aaditx23.krazyalarm.domain.repository.SettingsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {

    // Database
    single { AlarmDatabase.getInstance(androidContext()) }
    single { get<AlarmDatabase>().alarmDao() }

    // Preferences
    single { SettingsDataStore(androidContext()) }

    // Repositories
    single<AlarmRepository> { AlarmRepositoryImpl(get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
}
