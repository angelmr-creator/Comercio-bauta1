package com.example.util

import com.example.data.model.PaymentStatus
import kotlin.math.max

data class SettlementBreakdown(
    val grossIncomeCents: Long,
    val operatingExpensesCents: Long,
    val netProfitCents: Long,
    val profitSharePercent: Long,
    val profitShareCents: Long,
    val spaceRentCents: Long,
    val totalDueCents: Long,
    val paidCents: Long,
    val remainingCents: Long,
    val paymentStatus: PaymentStatus
)

object SettlementCalculator {

    fun calculateFromIncomeAndExpenses(
        grossIncomeCents: Long,
        operatingExpensesCents: Long,
        profitSharePercent: Long = 30L,
        spaceRentCents: Long,
        paidCents: Long = 0L
    ): SettlementBreakdown {
        val safeGross = max(0L, grossIncomeCents)
        val safeExpenses = max(0L, operatingExpensesCents)
        val netProfit = safeGross - safeExpenses
        return calculateFromNetProfit(
            grossIncomeCents = safeGross,
            operatingExpensesCents = safeExpenses,
            netProfitCents = netProfit,
            profitSharePercent = profitSharePercent,
            spaceRentCents = spaceRentCents,
            paidCents = paidCents
        )
    }

    fun calculateFromNetProfit(
        grossIncomeCents: Long = 0L,
        operatingExpensesCents: Long = 0L,
        netProfitCents: Long,
        profitSharePercent: Long = 30L,
        spaceRentCents: Long,
        paidCents: Long = 0L
    ): SettlementBreakdown {
        val clampedPercent = profitSharePercent.coerceIn(0L, 100L)
        val safeRent = max(0L, spaceRentCents)
        val profitShareCents = if (netProfitCents > 0L) {
            ((netProfitCents * clampedPercent) + 50L) / 100L
        } else {
            0L
        }
        val totalDue = profitShareCents + safeRent
        val safePaid = paidCents.coerceIn(0L, max(totalDue, paidCents))
        val remaining = max(0L, totalDue - safePaid)
        val status = determinePaymentStatus(totalDueCents = totalDue, paidCents = safePaid)

        return SettlementBreakdown(
            grossIncomeCents = max(0L, grossIncomeCents),
            operatingExpensesCents = max(0L, operatingExpensesCents),
            netProfitCents = netProfitCents,
            profitSharePercent = clampedPercent,
            profitShareCents = profitShareCents,
            spaceRentCents = safeRent,
            totalDueCents = totalDue,
            paidCents = safePaid,
            remainingCents = remaining,
            paymentStatus = status
        )
    }

    fun determinePaymentStatus(totalDueCents: Long, paidCents: Long): PaymentStatus = when {
        totalDueCents <= 0L -> PaymentStatus.PAGADO
        paidCents <= 0L -> PaymentStatus.PENDIENTE
        paidCents < totalDueCents -> PaymentStatus.PARCIAL
        else -> PaymentStatus.PAGADO
    }

    fun summarizeSettlements(
        settlements: List<com.example.data.model.MonthlySettlement>,
        selectedYear: Int,
        selectedMonth: Int
    ): FirestoreFinancialSummary {
        val monthItems = settlements.filter {
            it.periodYear.toInt() == selectedYear && it.periodMonth.toInt() == selectedMonth
        }

        val monthGross = monthItems.sumOf { it.grossIncomeCents }
        val monthExpenses = monthItems.sumOf { it.operatingExpensesCents }
        val monthNetProfit = monthItems.sumOf { it.netProfitCents }
        val monthProfitShare = monthItems.sumOf { it.profitShareCents }
        val monthSpaceRent = monthItems.sumOf { it.spaceRentCents }
        val monthTotalDue = monthItems.sumOf { it.totalDueCents }
        val monthPaid = monthItems.sumOf { it.paidCents.coerceAtMost(it.totalDueCents) }

        val accumulatedGross = settlements.sumOf { it.grossIncomeCents }
        val accumulatedExpenses = settlements.sumOf { it.operatingExpensesCents }
        val accumulatedNetProfit = settlements.sumOf { it.netProfitCents }
        val accumulatedProfitShare = settlements.sumOf { it.profitShareCents }
        val accumulatedSpaceRent = settlements.sumOf { it.spaceRentCents }
        val accumulatedTotalDue = settlements.sumOf { it.totalDueCents }
        val accumulatedPaid = settlements.sumOf { it.paidCents.coerceAtMost(it.totalDueCents) }

        // Build chronological periods (ascending) to compute running accumulated net profit,
        // then return descending for display (newest month first).
        val groupedAsc = settlements
            .groupBy { YearMonthKey(it.periodYear.toInt(), it.periodMonth.toInt()) }
            .toSortedMap(compareBy<YearMonthKey> { it.year }.thenBy { it.month })

        var runningNet = 0L
        val periodsAsc = groupedAsc.map { (period, items) ->
            val pGross = items.sumOf { it.grossIncomeCents }
            val pExpenses = items.sumOf { it.operatingExpensesCents }
            val pNet = items.sumOf { it.netProfitCents }
            val pDue = items.sumOf { it.totalDueCents }
            val pPaid = items.sumOf { it.paidCents.coerceAtMost(it.totalDueCents) }
            runningNet += pNet
            FinancialPeriodSummary(
                period = period,
                settlementsCount = items.size,
                grossIncomeCents = pGross,
                operatingExpensesCents = pExpenses,
                netProfitCents = pNet,
                accumulatedNetProfitUpToPeriodCents = runningNet,
                totalDueCents = pDue,
                paidCents = pPaid
            )
        }

        val selectedPeriodAccumulatedNet = periodsAsc
            .lastOrNull {
                it.period.year < selectedYear ||
                    (it.period.year == selectedYear && it.period.month <= selectedMonth)
            }
            ?.accumulatedNetProfitUpToPeriodCents ?: 0L

        return FirestoreFinancialSummary(
            monthGrossIncomeCents = monthGross,
            monthExpensesCents = monthExpenses,
            monthNetProfitCents = monthNetProfit,
            monthProfitShareCents = monthProfitShare,
            monthSpaceRentCents = monthSpaceRent,
            monthTotalDueCents = monthTotalDue,
            monthPaidCents = monthPaid,
            monthSettlementsCount = monthItems.size,
            accumulatedGrossIncomeCents = accumulatedGross,
            accumulatedExpensesCents = accumulatedExpenses,
            accumulatedNetProfitCents = accumulatedNetProfit,
            accumulatedNetProfitUpToSelectedMonthCents = selectedPeriodAccumulatedNet,
            accumulatedProfitShareCents = accumulatedProfitShare,
            accumulatedSpaceRentCents = accumulatedSpaceRent,
            accumulatedTotalDueCents = accumulatedTotalDue,
            accumulatedPaidCents = accumulatedPaid,
            totalSettlementsCount = settlements.size,
            periodHistory = periodsAsc.reversed()
        )
    }
}

data class FinancialPeriodSummary(
    val period: YearMonthKey,
    val settlementsCount: Int,
    val grossIncomeCents: Long,
    val operatingExpensesCents: Long,
    val netProfitCents: Long,
    val accumulatedNetProfitUpToPeriodCents: Long,
    val totalDueCents: Long,
    val paidCents: Long
)

data class FirestoreFinancialSummary(
    val monthGrossIncomeCents: Long = 0L,
    val monthExpensesCents: Long = 0L,
    val monthNetProfitCents: Long = 0L,
    val monthProfitShareCents: Long = 0L,
    val monthSpaceRentCents: Long = 0L,
    val monthTotalDueCents: Long = 0L,
    val monthPaidCents: Long = 0L,
    val monthSettlementsCount: Int = 0,
    val accumulatedGrossIncomeCents: Long = 0L,
    val accumulatedExpensesCents: Long = 0L,
    val accumulatedNetProfitCents: Long = 0L,
    val accumulatedNetProfitUpToSelectedMonthCents: Long = 0L,
    val accumulatedProfitShareCents: Long = 0L,
    val accumulatedSpaceRentCents: Long = 0L,
    val accumulatedTotalDueCents: Long = 0L,
    val accumulatedPaidCents: Long = 0L,
    val totalSettlementsCount: Int = 0,
    val periodHistory: List<FinancialPeriodSummary> = emptyList()
)
