package com.payvoice.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.payvoice.app.PayVoiceApplication
import com.payvoice.app.service.SoundboxService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d("PayVoiceBootReceiver", "Boot or package update received, checking soundbox state")
            CoroutineScope(Dispatchers.IO).launch {
                val app = PayVoiceApplication.get()
                val settings = app.settingsDataStore.getSettingsSnapshot()
                if (settings.isSoundboxActive) {
                    SoundboxService.start(context)
                }
            }
        }
    }
}
