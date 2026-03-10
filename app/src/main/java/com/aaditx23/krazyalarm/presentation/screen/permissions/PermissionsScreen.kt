package com.aaditx23.krazyalarm.presentation.screen.permissions

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aaditx23.krazyalarm.data.util.PermissionUtils

// ---------------------------------------------------------------------------
// OEM helpers
// ---------------------------------------------------------------------------

private enum class OemType { MIUI, SAMSUNG, REALME_COLOROS, HUAWEI_EMUI, OTHER }

private fun detectOem(): OemType {
    val manufacturer = Build.MANUFACTURER.lowercase()
    val brand = Build.BRAND.lowercase()
    return when {
        manufacturer.contains("xiaomi") || brand.contains("xiaomi") ||
        brand.contains("redmi") || brand.contains("poco") -> OemType.MIUI
        manufacturer.contains("samsung") -> OemType.SAMSUNG
        manufacturer.contains("realme") || manufacturer.contains("oppo") ||
        brand.contains("realme") || brand.contains("oppo") -> OemType.REALME_COLOROS
        manufacturer.contains("huawei") || manufacturer.contains("honor") -> OemType.HUAWEI_EMUI
        else -> OemType.OTHER
    }
}

private data class OemAlarmInfo(
    val extraDescription: String,
    val buttonLabel: String,
    /** Returns null if the intent cannot be constructed on this device */
    val buildIntent: (packageName: String, pm: PackageManager) -> Intent?
)

private fun getOemAlarmInfo(oem: OemType): OemAlarmInfo? = when (oem) {
    OemType.MIUI -> OemAlarmInfo(
        extraDescription = "On Xiaomi/MIUI devices you also need to enable \"Autostart\" and allow background activity in the MIUI Security app.",
        buttonLabel = "Open MIUI Permissions",
        buildIntent = { pkg, pm ->
            val miuiIntent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            }
            val canResolveMiui = try {
                pm.resolveActivity(miuiIntent, PackageManager.MATCH_DEFAULT_ONLY) != null
            } catch (_: Exception) { false }

            if (canResolveMiui) miuiIntent
            else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$pkg")
            }
        }
    )
    OemType.SAMSUNG -> OemAlarmInfo(
        extraDescription = "On Samsung devices, also make sure \"Allow background activity\" is enabled in Battery settings for this app.",
        buttonLabel = "Open Battery Settings",
        buildIntent = { pkg, _ ->
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$pkg")
            }
        }
    )
    OemType.REALME_COLOROS -> OemAlarmInfo(
        extraDescription = "On Realme/OPPO devices, enable \"Autostart\" in Phone Manager → Privacy → Startup Manager.",
        buttonLabel = "Open App Settings",
        buildIntent = { pkg, _ ->
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$pkg")
            }
        }
    )
    OemType.HUAWEI_EMUI -> OemAlarmInfo(
        extraDescription = "On Huawei/Honor devices, enable \"Auto-launch\" in Phone Manager → App Launch.",
        buttonLabel = "Open App Settings",
        buildIntent = { pkg, _ ->
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$pkg")
            }
        }
    )
    OemType.OTHER -> null
}

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@Composable
fun PermissionsScreen(
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val oem = remember { detectOem() }
    val oemAlarmInfo = remember(oem) { getOemAlarmInfo(oem) }

    var notificationPermissionGranted by remember {
        mutableStateOf(PermissionUtils.hasNotificationPermission(context))
    }
    var alarmPermissionGranted by remember {
        mutableStateOf(PermissionUtils.canScheduleExactAlarms(context))
    }
    var cameraPermissionGranted by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var fullScreenIntentGranted by remember {
        mutableStateOf(PermissionUtils.canUseFullScreenIntent(context))
    }

    // Check if we should show rationale for notification permission
    var showNotificationRationale by remember { mutableStateOf(false) }
    var showCameraRationale by remember { mutableStateOf(false) }

    // Notification permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationPermissionGranted = isGranted
        if (!isGranted) {
            showNotificationRationale = true
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraPermissionGranted = isGranted
        if (!isGranted) {
            showCameraRationale = true
        }
    }

    // Settings launcher to recheck permissions when user returns
    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Recheck permissions when returning from settings
        notificationPermissionGranted = PermissionUtils.hasNotificationPermission(context)
        alarmPermissionGranted = PermissionUtils.canScheduleExactAlarms(context)
        cameraPermissionGranted = context.checkSelfPermission(Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        fullScreenIntentGranted = PermissionUtils.canUseFullScreenIntent(context)
    }

    // Check permissions when screen first appears
    LaunchedEffect(Unit) {
        notificationPermissionGranted = PermissionUtils.hasNotificationPermission(context)
        alarmPermissionGranted = PermissionUtils.canScheduleExactAlarms(context)
        cameraPermissionGranted = context.checkSelfPermission(Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        fullScreenIntentGranted = PermissionUtils.canUseFullScreenIntent(context)
    }

    // Recheck permissions when app comes to foreground
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationPermissionGranted = PermissionUtils.hasNotificationPermission(context)
                alarmPermissionGranted = PermissionUtils.canScheduleExactAlarms(context)
                cameraPermissionGranted = context.checkSelfPermission(Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
                fullScreenIntentGranted = PermissionUtils.canUseFullScreenIntent(context)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Auto-navigate when all permissions are granted
    LaunchedEffect(notificationPermissionGranted, alarmPermissionGranted, cameraPermissionGranted, fullScreenIntentGranted) {
        if (notificationPermissionGranted && alarmPermissionGranted && cameraPermissionGranted && fullScreenIntentGranted) {
            onPermissionsGranted()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App icon or illustration
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "Welcome to Krazy Alarm",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "To provide you with reliable alarms, we need a few permissions",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Alarm Permission Card — always shown first
            PermissionCard(
                icon = Icons.Default.Schedule,
                title = "Exact Alarm Permission",
                description = "Required to schedule alarms at precise times and ensure they ring exactly when you need them",
                isGranted = alarmPermissionGranted,
                onRequestClick = {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    settingsLauncher.launch(intent)
                },
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    settingsLauncher.launch(intent)
                },
                showRationale = false,
                oemExtraInfo = oemAlarmInfo?.let { info ->
                    OemExtraInfo(
                        message = info.extraDescription,
                        buttonLabel = info.buttonLabel,
                        onButtonClick = {
                            val intent = info.buildIntent(context.packageName, context.packageManager)
                            if (intent != null) settingsLauncher.launch(intent)
                        }
                    )
                }
            )

            // Notification and Camera cards only appear after alarm permission is granted
            if (alarmPermissionGranted) {
                Spacer(modifier = Modifier.height(16.dp))

                PermissionCard(
                    icon = Icons.Default.Notifications,
                    title = "Notification Permission",
                    description = "Required to show alarm notifications and alert you when alarms ring",
                    isGranted = notificationPermissionGranted,
                    onRequestClick = {
                        if (!notificationPermissionGranted) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        settingsLauncher.launch(intent)
                    },
                    showRationale = showNotificationRationale
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Full Screen Intent Permission (Android 14+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && notificationPermissionGranted) {
                    PermissionCard(
                        icon = Icons.Default.PhoneAndroid,
                        title = "Full Screen Alarm Permission",
                        description = "Required to show alarms on your lock screen when the phone is locked. This ensures you never miss an alarm.",
                        isGranted = fullScreenIntentGranted,
                        onRequestClick = {
                            PermissionUtils.openFullScreenIntentSettings(context)
                        },
                        onOpenSettings = {
                            PermissionUtils.openFullScreenIntentSettings(context)
                        },
                        showRationale = false
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                PermissionCard(
                    icon = Icons.Default.FlashOn,
                    title = "Camera Permission",
                    description = "Required to use LED flash patterns as visual alerts when alarms ring",
                    isGranted = cameraPermissionGranted,
                    onRequestClick = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        settingsLauncher.launch(intent)
                    },
                    showRationale = showCameraRationale
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Continue button - only shown when all permissions are granted
            if (notificationPermissionGranted && alarmPermissionGranted && cameraPermissionGranted && fullScreenIntentGranted) {
                Button(
                    onClick = onPermissionsGranted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = "Get Started",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "Please grant all permissions to continue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info text
            Text(
                text = "You can change these permissions later in Settings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------------------------------------------------------------------------
// OEM extra info data class
// ---------------------------------------------------------------------------

data class OemExtraInfo(
    val message: String,
    val buttonLabel: String,
    val onButtonClick: () -> Unit
)

// ---------------------------------------------------------------------------
// Permission Card
// ---------------------------------------------------------------------------

@Composable
fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onRequestClick: () -> Unit,
    onOpenSettings: () -> Unit,
    showRationale: Boolean,
    oemExtraInfo: OemExtraInfo? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (isGranted)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (isGranted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Granted",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!isGranted) {
                Spacer(modifier = Modifier.height(16.dp))

                if (showRationale) {
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Settings")
                    }
                } else {
                    Button(
                        onClick = onRequestClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Grant Permission")
                    }
                }

                // OEM-specific extra guidance shown below the main button
                if (oemExtraInfo != null) {
                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = oemExtraInfo.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = oemExtraInfo.onButtonClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(oemExtraInfo.buttonLabel)
                    }
                }
            }
        }
    }
}

