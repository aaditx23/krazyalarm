package com.aaditx23.krazyalarm.data.scheduler

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Manages alarm queue to prevent overlapping alarms.
 * Ensures only one alarm rings at a time and queues others.
 */
object AlarmQueueManager {
    private const val TAG = "AlarmQueueManager"

    private val alarmQueue = ConcurrentLinkedQueue<Long>()
    private val mutex = Mutex()
    private var isProcessing = false
    private var currentAlarmId: Long? = null

    /**
     * Add an alarm to the queue and process it when ready
     */
    fun enqueueAlarm(context: Context, alarmId: Long) {
        Log.d(TAG, "Enqueuing alarm: $alarmId, current queue size: ${alarmQueue.size}, isProcessing: $isProcessing")
        alarmQueue.offer(alarmId)

        CoroutineScope(Dispatchers.Main).launch {
            processQueue(context)
        }
    }

    /**
     * Process the queue - start the next alarm if no alarm is currently ringing
     */
    private suspend fun processQueue(context: Context) {
        mutex.withLock {
            if (isProcessing) {
                Log.d(TAG, "Already processing an alarm, queue size: ${alarmQueue.size}")
                return
            }

            val nextAlarmId = alarmQueue.poll()
            if (nextAlarmId == null) {
                Log.d(TAG, "Queue is empty")
                return
            }

            isProcessing = true
            currentAlarmId = nextAlarmId

            Log.d(TAG, "Starting alarm from queue: $nextAlarmId, remaining in queue: ${alarmQueue.size}")

            // Start the alarm service
            val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
                putExtra(AlarmRingingService.EXTRA_ALARM_ID, nextAlarmId)
            }
            context.startForegroundService(serviceIntent)
        }
    }

    /**
     * Called when an alarm finishes (dismissed, snoozed, or auto-dismissed)
     */
    fun onAlarmFinished(context: Context, alarmId: Long) {
        Log.d(TAG, "Alarm finished: $alarmId, currentAlarmId: $currentAlarmId")

        CoroutineScope(Dispatchers.Main).launch {
            var shouldProcessNext = false

            mutex.withLock {
                if (currentAlarmId == alarmId) {
                    Log.d(TAG, "Resetting state for alarm: $alarmId")
                    currentAlarmId = null
                    isProcessing = false
                    shouldProcessNext = alarmQueue.isNotEmpty()
                } else {
                    Log.w(TAG, "Alarm $alarmId finished but currentAlarmId is $currentAlarmId - ignoring")
                }
            }

            // Small delay before processing next alarm (outside mutex to not block)
            if (shouldProcessNext) {
                delay(500)
                Log.d(TAG, "Processing next alarm in queue, queue size: ${alarmQueue.size}")
                processQueue(context)
            } else if (alarmQueue.isEmpty()) {
                Log.d(TAG, "Queue is empty, no more alarms to process")
            }
        }
    }

    /**
     * Check if an alarm is currently ringing
     */
    fun isAlarmRinging(): Boolean {
        return isProcessing && currentAlarmId != null
    }

    /**
     * Get the current alarm ID that is ringing
     */
    fun getCurrentAlarmId(): Long? {
        return currentAlarmId
    }

    /**
     * Get the number of alarms waiting in queue
     */
    fun getQueueSize(): Int {
        return alarmQueue.size
    }

    /**
     * Clear the entire queue (use with caution)
     */
    fun clearQueue() {
        Log.d(TAG, "Clearing alarm queue")
        alarmQueue.clear()
    }
}
