package com.aaditx23.krazyalarm.domain.models

data class AppSettings(
    val isDarkMode: Boolean = false,
    val defaultSnoozeDuration: Int = 10 // in minutes
)

