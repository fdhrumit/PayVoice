package com.payvoice.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object BatteryOptimizationHelper {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    fun openBatteryOptimizationSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    data class BrandTip(
        val brandName: String,
        val steps: List<String>
    )

    fun getOemTips(): List<BrandTip> {
        return listOf(
            BrandTip(
                brandName = "Xiaomi / Redmi / POCO (MIUI / HyperOS)",
                steps = listOf(
                    "1. Open Phone Settings > Apps > Manage Apps > PayVoice.",
                    "2. Turn ON 'Autostart'.",
                    "3. Tap 'Battery saver' and select 'No restrictions'.",
                    "4. In Recent Apps tray, long-press PayVoice and tap the Lock icon."
                )
            ),
            BrandTip(
                brandName = "Oppo / Realme (ColorOS / Realme UI)",
                steps = listOf(
                    "1. Open Settings > Battery > More battery settings > App battery management.",
                    "2. Select PayVoice and enable 'Allow background activity' and 'Allow auto-launch'.",
                    "3. In Recent Apps screen, tap 3 dots on PayVoice and select 'Lock'."
                )
            ),
            BrandTip(
                brandName = "Vivo / iQOO (Funtouch OS)",
                steps = listOf(
                    "1. Open Settings > Battery > Background power consumption management.",
                    "2. Select PayVoice and choose 'Don't restrict background power usage'.",
                    "3. Open Settings > Applications and Permissions > Autostart and enable PayVoice."
                )
            ),
            BrandTip(
                brandName = "Samsung (One UI)",
                steps = listOf(
                    "1. Open Settings > Apps > PayVoice > Battery.",
                    "2. Select 'Unrestricted'.",
                    "3. Open Settings > Battery and device care > Battery > Background usage limits > Never sleeping apps > Add PayVoice."
                )
            )
        )
    }
}
