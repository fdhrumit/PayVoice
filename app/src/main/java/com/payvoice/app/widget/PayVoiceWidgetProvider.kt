package com.payvoice.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.payvoice.app.MainActivity
import com.payvoice.app.R
import com.payvoice.app.data.local.AppDatabase
import com.payvoice.app.data.repository.PaymentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class PayVoiceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET || intent.action == ACTION_PAYMENT_RECEIVED) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, PayVoiceWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            updateWidgets(context, appWidgetManager, allWidgetIds)
        }
    }

    private fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val paymentDao = db.paymentDao()
                val startOfDay = PaymentRepository.getStartOfDayMillis()
                val todayPayments = paymentDao.getTodayPaymentsSync(startOfDay)
                val latest = paymentDao.getLatestPaymentSync()

                val totalAmount = todayPayments.sumOf { it.amount }
                val count = todayPayments.size

                val indianFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
                    maximumFractionDigits = 0
                }
                val formattedTotal = indianFormat.format(totalAmount).replace("₹", "₹ ")

                val latestText = if (latest != null) {
                    val amountText = if (latest.amount % 1.0 == 0.0) {
                        "₹${latest.amount.toInt()}"
                    } else {
                        "₹${String.format(Locale.US, "%.2f", latest.amount)}"
                    }
                    "Latest: ${latest.sender} • $amountText"
                } else {
                    "No payments received today"
                }

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_payvoice).apply {
                        setTextViewText(R.id.widget_today_amount, formattedTotal)
                        setTextViewText(R.id.widget_payment_count, "$count payments today")
                        setTextViewText(R.id.widget_latest_payment, latestText)

                        val intent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val pendingIntent = PendingIntent.getActivity(
                            context,
                            widgetId,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    }

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "android.appwidget.action.APPWIDGET_UPDATE"
        const val ACTION_PAYMENT_RECEIVED = "com.payvoice.app.ACTION_PAYMENT_RECEIVED"

        fun sendUpdateBroadcast(context: Context) {
            val intent = Intent(context, PayVoiceWidgetProvider::class.java).apply {
                action = ACTION_PAYMENT_RECEIVED
            }
            context.sendBroadcast(intent)
        }
    }
}
