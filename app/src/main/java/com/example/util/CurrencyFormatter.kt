package com.example.util

import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

data class YearMonthKey(
    val year: Int,
    val month: Int // 1..12
) {
    fun previous(): YearMonthKey =
        if (month == 1) YearMonthKey(year - 1, 12) else YearMonthKey(year, month - 1)

    fun next(): YearMonthKey =
        if (month == 12) YearMonthKey(year + 1, 1) else YearMonthKey(year, month + 1)

    fun label(): String = "${monthName(month)} $year"

    companion object {
        fun current(): YearMonthKey {
            val cal = Calendar.getInstance()
            return YearMonthKey(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH) + 1
            )
        }

        fun monthName(month: Int): String = when (month) {
            1 -> "Enero"
            2 -> "Febrero"
            3 -> "Marzo"
            4 -> "Abril"
            5 -> "Mayo"
            6 -> "Junio"
            7 -> "Julio"
            8 -> "Agosto"
            9 -> "Septiembre"
            10 -> "Octubre"
            11 -> "Noviembre"
            12 -> "Diciembre"
            else -> "Mes $month"
        }
    }
}

object CurrencyFormatter {
    private val numberFormat: NumberFormat
        get() = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
            isGroupingUsed = true
        }

    fun format(cents: Long, symbol: String = "$", suffix: String = "CUP"): String {
        val negative = cents < 0
        val amount = abs(cents) / 100.0
        val formatted = numberFormat.format(amount)
        val tail = if (suffix.isNotBlank()) " $suffix" else ""
        return if (negative) "-$symbol$formatted$tail" else "$symbol$formatted$tail"
    }

    fun formatInputValue(cents: Long): String {
        if (cents == 0L) return ""
        val whole = cents / 100
        val rem = abs(cents % 100)
        return if (rem == 0L) "$whole.00" else String.format(Locale.US, "%d.%02d", whole, rem)
    }

    fun parseAmountToCents(raw: String, allowNegative: Boolean = false): Long? {
        val cleaned = raw.trim()
            .replace("$", "")
            .replace("CUP", "", ignoreCase = true)
            .replace(" ", "")
            .replace(",", ".")
        if (cleaned.isEmpty()) return null
        val value = cleaned.toDoubleOrNull() ?: return null
        if (!allowNegative && value < 0.0) return null
        return (value * 100.0).roundToLong()
    }
}
