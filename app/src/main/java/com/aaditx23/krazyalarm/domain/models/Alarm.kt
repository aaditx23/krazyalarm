package com.aaditx23.krazyalarm.domain.models

data class Alarm(
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val days: Int, // bitmask for days of week
    val enabled: Boolean,
    val label: String? = null,
    val ringtoneUri: String? = null,
    val flashPatternId: String? = null,
    val vibrationPatternId: String? = null,
    val vibrationIntensity: VibrationIntensity = VibrationIntensity.MEDIUM,
    val snoozeDurationMinutes: Int = 10,
    val alarmDurationMinutes: Int = 1, // How long the alarm will ring (1-5 minutes)
    val scheduledDate: Long? = null, // Specific date in millis for one-time alarms
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class AlarmInput(
    val hour: Int,
    val minute: Int,
    val days: Int,
    val enabled: Boolean,
    val label: String? = null,
    val ringtoneUri: String? = null,
    val flashPatternId: String? = null,
    val vibrationPatternId: String? = null,
    val vibrationIntensity: VibrationIntensity = VibrationIntensity.MEDIUM,
    val snoozeDurationMinutes: Int = 10,
    val alarmDurationMinutes: Int = 1,
    val scheduledDate: Long? = null
)

enum class VibrationIntensity {
    LIGHT, MEDIUM, STRONG
}

sealed class FlashPattern(val id: String, val displayName: String) {
    object None : FlashPattern("NONE", "No Flash")
    object AlwaysOn : FlashPattern("ALWAYS_ON", "Always On")
    object SosBlink : FlashPattern("SOS_BLINK", "SOS Blink")
    object Strobe : FlashPattern("STROBE", "Strobe")
    object Pulse : FlashPattern("PULSE", "Pulse")
    object Heartbeat : FlashPattern("HEARTBEAT", "Heartbeat")

    companion object {
        fun fromId(id: String?): FlashPattern {
            return when (id) {
                "NONE" -> None
                "ALWAYS_ON" -> AlwaysOn
                "SOS_BLINK" -> SosBlink
                "STROBE" -> Strobe
                "PULSE" -> Pulse
                "HEARTBEAT" -> Heartbeat
                else -> None
            }
        }

        fun getAll(): List<FlashPattern> = listOf(None, AlwaysOn, SosBlink, Strobe, Pulse, Heartbeat)
    }
}

sealed class VibrationPattern(val id: String, val displayName: String) {
    object Off : VibrationPattern("OFF", "Off")
    object Continuous : VibrationPattern("CONTINUOUS", "Continuous")
    object Pulse : VibrationPattern("PULSE", "Pulse")
    object Escalating : VibrationPattern("ESCALATING", "Escalating")
    object Heartbeat : VibrationPattern("HEARTBEAT", "Heartbeat")
    object Wave : VibrationPattern("WAVE", "Wave")

    companion object {
        fun fromId(id: String?): VibrationPattern {
            return when (id) {
                "OFF" -> Off
                "CONTINUOUS" -> Continuous
                "PULSE" -> Pulse
                "ESCALATING" -> Escalating
                "HEARTBEAT" -> Heartbeat
                "WAVE" -> Wave
                else -> Continuous
            }
        }

        fun getAll(): List<VibrationPattern> = listOf(Off, Continuous, Pulse, Escalating, Heartbeat, Wave)
    }
}
