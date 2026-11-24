package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

data class ParsedTransfermovilSms(
    val amountCents: Long?,
    val transactionReference: String?
)

object TransfermovilHelper {
    const val TRANSFERMOVIL_PACKAGE = "cu.etecsa.cubacel.tr.tm"

    fun copyToClipboard(context: Context, label: String, text: String, toastMessage: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
    }

    fun cleanCardOrAccountForCopy(raw: String): String {
        val digitsOnly = raw.filter { it.isDigit() }
        return if (digitsOnly.length >= 12) digitsOnly else raw.trim()
    }

    fun buildTransferQrPayload(cardOrAccount: String, phone: String = ""): String {
        val digits = cardOrAccount.filter { it.isDigit() }
        val cleanPhone = phone.filter { it.isDigit() }
        return if (digits.isNotEmpty()) {
            "TRANSFERMOVIL_ETECSA,TRANSFERENCIA,$digits,$cleanPhone"
        } else {
            ""
        }
    }

    fun isWebUrl(value: String): Boolean {
        val trimmed = value.trim().lowercase()
        return trimmed.startsWith("https://") || trimmed.startsWith("http://")
    }

    fun openExternalLink(context: Context, url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return false
        return runCatching {
            val uri = Uri.parse(
                if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                    trimmed
                } else {
                    "https://$trimmed"
                }
            )
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun openTransfermovilApp(context: Context): Boolean {
        return runCatching {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(TRANSFERMOVIL_PACKAGE)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    /**
     * Parses pasted confirmation SMS text from Transfermóvil / BANDEC / BPA / Metropolitano
     * without requiring restricted Android SMS permissions.
     */
    fun parseTransfermovilSms(rawText: String): ParsedTransfermovilSms {
        val text = rawText.trim()
        if (text.isEmpty()) return ParsedTransfermovilSms(null, null)

        val amountRegex = Regex(
            pattern = """(?:monto|importe|transferencia\s+de|recibida?\s+de|por)\s*:?\s*\$?\s*([0-9][0-9.,]*)\s*(?:cup)?""",
            option = RegexOption.IGNORE_CASE
        )
        val fallbackCupRegex = Regex(
            pattern = """([0-9][0-9.,]*)\s*cup""",
            option = RegexOption.IGNORE_CASE
        )
        val rawAmount = amountRegex.find(text)?.groupValues?.getOrNull(1)
            ?: fallbackCupRegex.find(text)?.groupValues?.getOrNull(1)
        val amountCents = rawAmount?.let { CurrencyFormatter.parseAmountToCents(normalizeGroupedNumber(it)) }

        val refRegex = Regex(
            pattern = """(?:transacci[oó]n|operaci[oó]n|referencia|id|nr\.?|no\.?)\s*:?\s*([A-Z0-9\-]{4,24})""",
            option = RegexOption.IGNORE_CASE
        )
        val tmCodeRegex = Regex("""\b(TM[-_]?[A-Z0-9]{4,20})\b""", RegexOption.IGNORE_CASE)
        val reference = refRegex.find(text)?.groupValues?.getOrNull(1)?.uppercase()
            ?: tmCodeRegex.find(text)?.groupValues?.getOrNull(1)?.uppercase()

        return ParsedTransfermovilSms(amountCents = amountCents, transactionReference = reference)
    }

    private fun normalizeGroupedNumber(raw: String): String {
        val trimmed = raw.trim()
        // If both comma and dot exist, remove commas as thousands separators
        return if (trimmed.contains(',') && trimmed.contains('.')) {
            trimmed.replace(",", "")
        } else {
            trimmed
        }
    }
}
