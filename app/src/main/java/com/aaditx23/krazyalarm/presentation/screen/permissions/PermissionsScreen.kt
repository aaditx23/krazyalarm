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
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aaditx23.krazyalarm.data.util.PermissionUtils
import androidx.core.net.toUri

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
                data = "package:$pkg".toUri()
            }
        }
    )
    OemType.REALME_COLOROS -> OemAlarmInfo(
        extraDescription = "On Realme/OPPO devices, enable \"Autostart\" in Phone Manager → Privacy → Startup Manager.",
        buttonLabel = "Open App Settings",
        buildIntent = { pkg, _ ->
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:$pkg".toUri()
            }
        }
    )
    OemType.HUAWEI_EMUI -> OemAlarmInfo(
        extraDescription = "On Huawei/Honor devices, enable \"Auto-launch\" in Phone Manager → App Launch.",
        buttonLabel = "Open App Settings",
        buildIntent = { pkg, _ ->
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:$pkg".toUri()
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
    requireExplicitContinue: Boolean = false,
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hasCameraHardware = remember { PermissionUtils.hasCamera(context) }

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
    val cameraPermissionSatisfied = !hasCameraHardware || cameraPermissionGranted

    // Notification permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationPermissionGranted = isGranted
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraPermissionGranted = isGranted
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
    LaunchedEffect(notificationPermissionGranted, alarmPermissionGranted, cameraPermissionGranted, fullScreenIntentGranted, hasCameraHardware) {
        if (!requireExplicitContinue && notificationPermissionGranted && alarmPermissionGranted && cameraPermissionSatisfied && fullScreenIntentGranted) {
            onPermissionsGranted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()

            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 24.dp)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Grant Permissions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Essential for reliable alarms",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Permission List
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Exact Alarm Permission
            CompactPermissionCard(
                icon = Icons.Default.Schedule,
                title = "Exact Alarms",
                isGranted = alarmPermissionGranted,
                onRequestClick = {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    settingsLauncher.launch(intent)
                }
            )
            // Full Screen Intent Permission (Android 14+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                CompactPermissionCard(
                    icon = Icons.Default.PhoneAndroid,
                    title = "Lock Screen Alarms",
                    isGranted = fullScreenIntentGranted,
                    onRequestClick = {
                        PermissionUtils.openFullScreenIntentSettings(context)
                    },
                    enabled = alarmPermissionGranted
                )
            }

            // Notification Permission
            CompactPermissionCard(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                isGranted = notificationPermissionGranted,
                onRequestClick = {
                    if (!notificationPermissionGranted) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                enabled = fullScreenIntentGranted
            )



            // Camera Permission
            if (hasCameraHardware) {
                CompactPermissionCard(
                    icon = Icons.Default.FlashOn,
                    title = "Camera (for Flash)",
                    isGranted = cameraPermissionGranted,
                    onRequestClick = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    enabled = fullScreenIntentGranted
                )
            }

            // OEM-specific guidance if needed
            if (oemAlarmInfo != null && alarmPermissionGranted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Device-specific setup",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Additional permissions may be needed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        TextButton(
                            onClick = {
                                val intent = oemAlarmInfo.buildIntent(context.packageName, context.packageManager)
                                if (intent != null) settingsLauncher.launch(intent)
                            }
                        ) {
                            Text("Open")
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(16.dp).padding(bottom= 16.dp)
        ) {
            if (notificationPermissionGranted && alarmPermissionGranted && cameraPermissionSatisfied && fullScreenIntentGranted) {
                Button(
                    onClick = onPermissionsGranted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (requireExplicitContinue) "Continue" else "Get Started",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Card(

                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),

                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Grant all permissions to continue",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Compact Permission Card
// ---------------------------------------------------------------------------

@Composable
fun CompactPermissionCard(
    icon: ImageVector,
    title: String,
    isGranted: Boolean,
    onRequestClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isGranted -> MaterialTheme.colorScheme.primaryContainer
                !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 72.dp)
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = when {
                    isGranted -> MaterialTheme.colorScheme.primary
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isGranted) FontWeight.Medium else FontWeight.Normal,
                color = when {
                    isGranted -> MaterialTheme.colorScheme.onPrimaryContainer
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.weight(1f)
            )

            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Button(
                    onClick = onRequestClick,
                    enabled = enabled,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Grant", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

