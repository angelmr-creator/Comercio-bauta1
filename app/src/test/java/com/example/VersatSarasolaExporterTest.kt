package com.example

import com.example.data.model.InvoiceRecord
import com.example.data.model.PaymentChannel
import com.example.data.model.PaymentConfig
import com.example.data.model.PaymentEvent
import com.example.data.model.PaymentVerificationStatus
import com.example.util.PdfReceiptGenerator
import com.example.util.VersatSarasolaExporter
import org.junit.Assert.assertTrue
import org.junit.Test

class VersatSarasolaExporterTest {

    @Test
    fun buildVersatCsv_formatsCubanFieldsAndDecimalCents() {
        val invoice = InvoiceRecord(
            id = "inv-1",
            invoiceNumber = "FAC-2026-0001",
            actorName = "Cafetería \"El Sol\"",
            actorTaxId = "20495811223",
            concept = "Aporte 30% + alquiler",
            periodYear = 2026L,
            periodMonth = 11L,
            profitShareCents = 1200000L,
            spaceRentCents = 500000L,
            totalAmountCents = 1700000L,
            paymentChannel = PaymentChannel.TRANSFERENCIA.name,
            paymentReference = "TM-849201",
            debitAccountCode = "111.01",
            creditAccountCode = "900.01",
            costCenter = "COM-BAUTA",
            issuedByUsername = "director",
            issueDateMillis = 1795305600000L
        )

        val csv = VersatSarasolaExporter.buildVersatCsv(listOf(invoice))
        assertTrue(csv.contains("No_Factura;Fecha_Emision;Periodo"))
        assertTrue(csv.contains("FAC-2026-0001"))
        assertTrue(csv.contains("2026-11"))
        assertTrue(csv.contains("\"Cafetería \"\"El Sol\"\"\""))
        assertTrue(csv.contains("12000.00;5000.00;17000.00;CUP"))
        assertTrue(csv.contains("TM-849201"))
    }

    @Test
    fun buildVersatXml_escapesSpecialChars() {
        val invoice = InvoiceRecord(
            id = "inv-2",
            invoiceNumber = "FAC-2026-0002",
            actorName = "Panadería A & B",
            actorTaxId = "11122233344",
            concept = "Cobro <Mensual>",
            periodYear = 2026L,
            periodMonth = 11L,
            profitShareCents = 300000L,
            spaceRentCents = 200000L,
            totalAmountCents = 500000L,
            paymentChannel = PaymentChannel.QR_LINK.name,
            paymentReference = "QR-9910",
            issueDateMillis = 1795305600000L
        )

        val xml = VersatSarasolaExporter.buildVersatXml(listOf(invoice))
        assertTrue(xml.contains("<VersatSarasolaComprobantes"))
        assertTrue(xml.contains("Panadería A &amp; B"))
        assertTrue(xml.contains("Cobro &lt;Mensual&gt;"))
        assertTrue(xml.contains("<ImporteTotalCUP>5000.00</ImporteTotalCUP>"))
    }

    @Test
    fun pdfReceiptData_populatesDefaultBautaAccount() {
        val event = PaymentEvent(
            id = "abc123def456",
            actorName = "Tienda Bauta",
            periodYear = 2026L,
            periodMonth = 11L,
            amountCents = 1700000L,
            totalPaidAfterCents = 1700000L,
            paymentChannel = PaymentChannel.TRANSFERENCIA.name,
            paymentReference = "TM-7711",
            verificationStatus = PaymentVerificationStatus.CONFIRMADO.name,
            createdAtMillis = 1795305600000L
        )
        val receipt = PdfReceiptGenerator.fromPaymentEvent(
            event = event,
            paymentConfig = PaymentConfig()
        )
        assertTrue(receipt.bankAccount == PaymentConfig.DEFAULT_BAUTA_BANK_ACCOUNT)
        assertTrue(receipt.receiptCode.startsWith("REC-"))
        assertTrue(PdfReceiptGenerator.suggestedFileName(receipt).endsWith(".pdf"))
    }
}
