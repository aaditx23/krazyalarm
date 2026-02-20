package com.aaditx23.krazyalarm.di


import com.aaditx23.krazyalarm.domain.usecase.CalculateNextAlarmTimeUseCase
import com.aaditx23.krazyalarm.domain.usecase.CreateAlarmUseCase
import com.aaditx23.krazyalarm.domain.usecase.DeleteAlarmUseCase
import com.aaditx23.krazyalarm.domain.usecase.GetAlarmByIdUseCase
import com.aaditx23.krazyalarm.domain.usecase.GetAlarmsUseCase
import com.aaditx23.krazyalarm.domain.usecase.ToggleAlarmUseCase
import com.aaditx23.krazyalarm.domain.usecase.UpdateAlarmUseCase
import org.koin.dsl.module

val domainModule = module {

    // Use cases
    factory { GetAlarmsUseCase(get()) }
    factory { GetAlarmByIdUseCase(get()) }
    factory { CreateAlarmUseCase(get(), get()) }
    factory { UpdateAlarmUseCase(get(), get()) }
    factory { DeleteAlarmUseCase(get(), get()) }
    factory { ToggleAlarmUseCase(get(), get()) }
    factory { CalculateNextAlarmTimeUseCase() }
}
