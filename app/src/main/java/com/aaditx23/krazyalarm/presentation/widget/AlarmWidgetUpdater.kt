package com.aaditx23.krazyalarm.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.aaditx23.krazyalarm.MainActivity
import com.aaditx23.krazyalarm.R
import com.aaditx23.krazyalarm.domain.repository.AlarmRepository
import com.aaditx23.krazyalarm.domain.util.AlarmTimeCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AlarmWidgetUpdater : KoinComponent {

    private val alarmRepository: AlarmRepository by inject()

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        val digitalWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, DigitalClockWidgetProvider::class.java)
        )
        if (digitalWidgetIds.isNotEmpty()) {
            updateDigitalWidgets(context, appWidgetManager, digitalWidgetIds)
        }

        val analogWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, AnalogClockWidgetProvider::class.java)
        )
        if (analogWidgetIds.isNotEmpty()) {
            updateAnalogWidgets(context, appWidgetManager, analogWidgetIds)
        }
    }

    fun updateDigitalWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateWidgets(
            context = context,
            appWidgetManager = appWidgetManager,
            appWidgetIds = appWidgetIds,
            layoutId = R.layout.widget_digital_clock
        )
    }

    fun updateAnalogWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateWidgets(
            context = context,
            appWidgetManager = appWidgetManager,
            appWidgetIds = appWidgetIds,
            layoutId = R.layout.widget_analog_clock
        )
    }

    private fun updateWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        layoutId: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val nextAlarmText = buildNextAlarmText()
            val clickPendingIntent = createOpenAppPendingIntent(context)
            val hasUpcomingAlarm = !nextAlarmText.isNullOrEmpty()

            appWidgetIds.forEach { appWidgetId ->
                val views = RemoteViews(context.packageName, layoutId).apply {
                    if (hasUpcomingAlarm) {
                        setTextViewText(R.id.widget_next_alarm_text, nextAlarmText)
                        setViewVisibility(R.id.widget_next_alarm_row, View.VISIBLE)
                    } else {
                        setViewVisibility(R.id.widget_next_alarm_row, View.GONE)
                    }
                    setOnClickPendingIntent(R.id.widget_root, clickPendingIntent)
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    private suspend fun buildNextAlarmText(): String? {
        val enabledAlarms = alarmRepository.getEnabledAlarms()
        val nextAlarm = enabledAlarms.minByOrNull { AlarmTimeCalculator.getNextTriggerTime(it) }
            ?: return null

        val triggerTime = AlarmTimeCalculator.getNextTriggerTime(nextAlarm)
        val formatter = SimpleDateFormat("EEE h:mm a", Locale.getDefault())
        return formatter.format(Date(triggerTime))
    }

    private fun createOpenAppPendingIntent(context: Context): PendingIntent {
        val launchIntent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

