package com.example.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.local.entity.PaymentEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    fun exportPaymentsToCsv(context: Context, payments: List<PaymentEntity>): Intent? {
        if (payments.isEmpty()) return null

        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val fileName = "payvoice_payments_${dateFormat.format(Date())}.csv"
            val file = File(exportDir, fileName)

            val displayFormat = SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.US)

            file.bufferedWriter().use { writer ->
                writer.write("ID,Date & Time,Amount (INR),Sender,Account (Last 4),Source,Spoken\n")
                for (p in payments) {
                    val dateStr = displayFormat.format(Date(p.timestamp))
                    val cleanSender = p.sender.replace("\"", "\"\"")
                    val cleanSource = p.source.replace("\"", "\"\"")
                    writer.write("${p.id},\"$dateStr\",${p.amount},\"$cleanSender\",\"${p.accountLast4}\",\"$cleanSource\",${p.isSpoken}\n")
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            return Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "PayVoice Payment Report")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
