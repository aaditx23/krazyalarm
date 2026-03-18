package com.aaditx23.krazyalarm.domain.usecase

import com.aaditx23.krazyalarm.domain.models.Alarm
import com.aaditx23.krazyalarm.domain.models.AlarmInput
import com.aaditx23.krazyalarm.domain.repository.AlarmRepository
import com.aaditx23.krazyalarm.domain.repository.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CancelSnoozeUseCaseTest {

    @Test
    fun `cancel snooze clears snooze and restores schedule when alarm enabled`() = runBlocking {
        val alarm = Alarm(
            id = 10,
            hour = 6,
            minute = 45,
            days = 0b0111110,
            enabled = true,
            snoozedUntilMillis = System.currentTimeMillis() + 60_000L
        )
        val repo = FakeAlarmRepository(alarm)
        val scheduler = FakeAlarmScheduler()

        val result = CancelSnoozeUseCase(repo, scheduler).invoke(alarm.id)

        assertTrue(result.isSuccess)
        assertEquals(alarm.id, repo.updatedSnoozeId)
        assertEquals(null, repo.updatedSnoozeUntil)
        assertNotNull(scheduler.scheduledAlarm)
        assertEquals(alarm.id, scheduler.scheduledAlarm?.id)
        assertNull(scheduler.scheduledAlarm?.snoozedUntilMillis)
        assertNull(scheduler.canceledAlarmId)
    }

    @Test
    fun `cancel snooze clears snooze and cancels trigger when alarm disabled`() = runBlocking {
        val alarm = Alarm(
            id = 11,
            hour = 8,
            minute = 0,
            days = 0,
            enabled = false,
            snoozedUntilMillis = System.currentTimeMillis() + 60_000L
        )
        val repo = FakeAlarmRepository(alarm)
        val scheduler = FakeAlarmScheduler()

        val result = CancelSnoozeUseCase(repo, scheduler).invoke(alarm.id)

        assertTrue(result.isSuccess)
        assertEquals(alarm.id, repo.updatedSnoozeId)
        assertEquals(null, repo.updatedSnoozeUntil)
        assertNull(scheduler.scheduledAlarm)
        assertEquals(alarm.id, scheduler.canceledAlarmId)
    }

    private class FakeAlarmRepository(initialAlarm: Alarm?) : AlarmRepository {
        var alarm: Alarm? = initialAlarm
        var updatedSnoozeId: Long? = null
        var updatedSnoozeUntil: Long? = null

        override suspend fun createAlarm(input: AlarmInput): Result<Long> = Result.failure(UnsupportedOperationException())
        override suspend fun updateAlarm(id: Long, input: AlarmInput): Result<Unit> = Result.failure(UnsupportedOperationException())
        override suspend fun updateAlarmDirect(alarm: Alarm): Result<Unit> = Result.failure(UnsupportedOperationException())
        override suspend fun restoreAlarm(alarm: Alarm): Result<Unit> = Result.failure(UnsupportedOperationException())
        override suspend fun deleteAlarm(id: Long): Result<Unit> = Result.failure(UnsupportedOperationException())
        override fun observeAlarms(): Flow<List<Alarm>> = flowOf(listOfNotNull(alarm))
        override suspend fun getAlarm(id: Long): Alarm? = alarm?.takeIf { it.id == id }
        override suspend fun toggleAlarm(id: Long, enabled: Boolean): Result<Unit> = Result.failure(UnsupportedOperationException())
        override suspend fun updateSnoozedUntil(id: Long, snoozedUntilMillis: Long?): Result<Unit> {
            updatedSnoozeId = id
            updatedSnoozeUntil = snoozedUntilMillis
            alarm = alarm?.takeIf { it.id == id }?.copy(snoozedUntilMillis = snoozedUntilMillis)
            return Result.success(Unit)
        }
        override suspend fun getEnabledAlarms(): List<Alarm> = listOfNotNull(alarm?.takeIf { it.enabled })
    }

    private class FakeAlarmScheduler : AlarmScheduler {
        var scheduledAlarm: Alarm? = null
        var canceledAlarmId: Long? = null

        override suspend fun scheduleAlarm(alarm: Alarm): Result<Unit> {
            scheduledAlarm = alarm
            return Result.success(Unit)
        }

        override suspend fun cancelAlarm(alarmId: Long): Result<Unit> {
            canceledAlarmId = alarmId
            return Result.success(Unit)
        }

        override suspend fun rescheduleAllAlarms(): Result<Unit> = Result.success(Unit)
    }
}

