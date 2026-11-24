package com.example

import com.example.data.model.PaymentConfig
import com.example.data.model.PaymentStatus
import com.example.util.CurrencyFormatter
import com.example.util.PasswordHasher
import com.example.util.SettlementCalculator
import com.example.util.TransfermovilHelper
import com.example.util.YearMonthKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettlementCalculatorTest {

    @Test
    fun calculateFromIncomeAndExpenses_appliesThirtyPercentProfitPlusSpaceRent() {
        // Ingresos $100,000.00 CUP - Gastos $60,000.00 CUP = $40,000.00 utilidad neta
        // 30% de $40,000 = $12,000.00 CUP + Alquiler $5,000.00 CUP = $17,000.00 CUP
        val breakdown = SettlementCalculator.calculateFromIncomeAndExpenses(
            grossIncomeCents = 10_000_000L,
            operatingExpensesCents = 6_000_000L,
            profitSharePercent = 30L,
            spaceRentCents = 500_000L,
            paidCents = 0L
        )

        assertEquals(4_000_000L, breakdown.netProfitCents)
        assertEquals(1_200_000L, breakdown.profitShareCents)
        assertEquals(500_000L, breakdown.spaceRentCents)
        assertEquals(1_700_000L, breakdown.totalDueCents)
        assertEquals(1_700_000L, breakdown.remainingCents)
        assertEquals(PaymentStatus.PENDIENTE, breakdown.paymentStatus)
    }

    @Test
    fun calculateFromNetProfit_chargesOnlyRentWhenProfitIsZeroOrNegative() {
        val breakdown = SettlementCalculator.calculateFromIncomeAndExpenses(
            grossIncomeCents = 2_000_000L,
            operatingExpensesCents = 2_500_000L,
            profitSharePercent = 30L,
            spaceRentCents = 500_000L,
            paidCents = 200_000L
        )

        assertEquals(-500_000L, breakdown.netProfitCents)
        assertEquals(0L, breakdown.profitShareCents)
        assertEquals(500_000L, breakdown.totalDueCents)
        assertEquals(300_000L, breakdown.remainingCents)
        assertEquals(PaymentStatus.PARCIAL, breakdown.paymentStatus)
    }

    @Test
    fun calculateFromNetProfit_marksPaidWhenPaidCoversTotalDue() {
        val breakdown = SettlementCalculator.calculateFromNetProfit(
            netProfitCents = 1_000_000L,
            profitSharePercent = 30L,
            spaceRentCents = 400_000L,
            paidCents = 700_000L
        )

        assertEquals(700_000L, breakdown.totalDueCents)
        assertEquals(0L, breakdown.remainingCents)
        assertEquals(PaymentStatus.PAGADO, breakdown.paymentStatus)
    }

    @Test
    fun currencyFormatter_formatsCupAndParsesDecimalInputs() {
        assertEquals(1_250_50L, CurrencyFormatter.parseAmountToCents("1250.50"))
        assertEquals(4200L, CurrencyFormatter.parseAmountToCents("42,0"))
        assertNull(CurrencyFormatter.parseAmountToCents("-50"))
        assertEquals("$1,250.50 CUP", CurrencyFormatter.format(125050L))
        assertEquals("-$500.00 CUP", CurrencyFormatter.format(-50000L))
    }

    @Test
    fun yearMonthKey_rollsAcrossYears() {
        assertEquals(YearMonthKey(2025, 12), YearMonthKey(2026, 1).previous())
        assertEquals(YearMonthKey(2027, 1), YearMonthKey(2026, 12).next())
    }

    @Test
    fun transfermovilHelper_buildsValidTransferQrPayload() {
        assertEquals(
            "TRANSFERMOVIL_ETECSA,TRANSFERENCIA,0618221336820012,5355551234",
            TransfermovilHelper.buildTransferQrPayload(
                cardOrAccount = PaymentConfig.DEFAULT_BAUTA_BANK_ACCOUNT,
                phone = "53 5555 1234"
            )
        )
    }

    @Test
    fun transfermovilHelper_parsesTransfermovilConfirmationSms() {
        val parsed = TransfermovilHelper.parseTransfermovilSms(
            "BANDEC: Transferencia recibida por 17000.00 CUP hacia 0618221336820012. Operación: TM-849201."
        )
        assertEquals(1_700_000L, parsed.amountCents)
        assertEquals("TM-849201", parsed.transactionReference)
    }

    @Test
    fun passwordHasher_verifiesCorrectPasswordAndRejectsWrongPassword() {
        val hashed = PasswordHasher.hashPassword("Bauta2026*")
        val hashedSecond = PasswordHasher.hashPassword("Bauta2026*")

        assertNotEquals(hashed.saltHex, hashedSecond.saltHex)
        assertTrue(PasswordHasher.verifyPassword("Bauta2026*", hashed.hashHex, hashed.saltHex))
        assertFalse(PasswordHasher.verifyPassword("claveIncorrecta", hashed.hashHex, hashed.saltHex))
    }

    @Test
    fun summarizeSettlements_computesMonthlyAndAccumulatedIncomeExpensesAndNetProfit() {
        val janActor1 = com.example.data.model.MonthlySettlement(
            id = "s1",
            actorId = "a1",
            actorName = "Panadería El Trigal",
            periodYear = 2026L,
            periodMonth = 1L,
            grossIncomeCents = 10_000_000L,      // $100,000.00
            operatingExpensesCents = 6_000_000L, // $60,000.00
            netProfitCents = 4_000_000L,         // $40,000.00
            profitSharePercent = 30L,
            profitShareCents = 1_200_000L,
            spaceRentCents = 500_000L,
            totalDueCents = 1_700_000L,
            paidCents = 1_700_000L
        )
        val janActor2 = com.example.data.model.MonthlySettlement(
            id = "s2",
            actorId = "a2",
            actorName = "Cafetería Sur",
            periodYear = 2026L,
            periodMonth = 1L,
            grossIncomeCents = 5_000_000L,       // $50,000.00
            operatingExpensesCents = 2_000_000L, // $20,000.00
            netProfitCents = 3_000_000L,         // $30,000.00
            profitSharePercent = 30L,
            profitShareCents = 900_000L,
            spaceRentCents = 300_000L,
            totalDueCents = 1_200_000L,
            paidCents = 600_000L
        )
        val febActor1 = com.example.data.model.MonthlySettlement(
            id = "s3",
            actorId = "a1",
            actorName = "Panadería El Trigal",
            periodYear = 2026L,
            periodMonth = 2L,
            grossIncomeCents = 8_000_000L,       // $80,000.00
            operatingExpensesCents = 5_500_000L, // $55,000.00
            netProfitCents = 2_500_000L,         // $25,000.00
            profitSharePercent = 30L,
            profitShareCents = 750_000L,
            spaceRentCents = 500_000L,
            totalDueCents = 1_250_000L,
            paidCents = 1_250_000L
        )

        val febSummary = SettlementCalculator.summarizeSettlements(
            settlements = listOf(janActor1, janActor2, febActor1),
            selectedYear = 2026,
            selectedMonth = 2
        )

        // February totals
        assertEquals(8_000_000L, febSummary.monthGrossIncomeCents)
        assertEquals(5_500_000L, febSummary.monthExpensesCents)
        assertEquals(2_500_000L, febSummary.monthNetProfitCents)
        assertEquals(1, febSummary.monthSettlementsCount)

        // Accumulated totals across Jan + Feb
        assertEquals(23_000_000L, febSummary.accumulatedGrossIncomeCents)
        assertEquals(13_500_000L, febSummary.accumulatedExpensesCents)
        assertEquals(9_500_000L, febSummary.accumulatedNetProfitCents)
        assertEquals(9_500_000L, febSummary.accumulatedNetProfitUpToSelectedMonthCents)
        assertEquals(2, febSummary.periodHistory.size)
        // Newest period first in periodHistory
        assertEquals(2, febSummary.periodHistory[0].period.month)
        assertEquals(9_500_000L, febSummary.periodHistory[0].accumulatedNetProfitUpToPeriodCents)
        assertEquals(1, febSummary.periodHistory[1].period.month)
        assertEquals(7_000_000L, febSummary.periodHistory[1].accumulatedNetProfitUpToPeriodCents)
    }
}
