package com.aaditx23.krazyalarm

import android.app.Application
import com.aaditx23.krazyalarm.di.dataModule
import com.aaditx23.krazyalarm.di.domainModule
import com.aaditx23.krazyalarm.di.presentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class KrazyAlarmApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Koin
        startKoin {
            androidContext(this@KrazyAlarmApplication)
            modules(
                dataModule,
                domainModule,
                presentationModule
            )
        }
    }
}
