package com.example.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.model.PaymentConfig
import com.example.data.model.PaymentEvent
import com.example.data.model.PaymentVerificationStatus
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PaymentReceiptData(
    val receiptCode: String,
    val actorName: String,
    val actorTaxId: String,
    val localeName: String,
    val periodLabel: String,
    val amountCents: Long,
    val totalPaidAfterCents: Long,
    val channelLabel: String,
    val paymentReference: String,
    val verificationStatus: PaymentVerificationStatus,
    val verifiedByUsername: String,
    val bankAccount: String,
    val bankName: String,
    val accountHolder: String,
    val issuedAtMillis: Long,
    val note: String
)

object PdfReceiptGenerator {

    fun fromPaymentEvent(
        event: PaymentEvent,
        paymentConfig: PaymentConfig,
        actorTaxId: String = "",
        fallbackLocaleName: String = ""
    ): PaymentReceiptData {
        val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(event.createdAtMillis))
        val shortId = event.id.replace("-", "").take(6).uppercase(Locale.US).ifBlank { "000001" }
        val periodLabel = YearMonthKey(event.periodYear.toInt(), event.periodMonth.toInt()).label()
        return PaymentReceiptData(
            receiptCode = "REC-$dateStamp-$shortId",
            actorName = event.actorName,
            actorTaxId = actorTaxId,
            localeName = event.localeName.ifBlank { fallbackLocaleName },
            periodLabel = periodLabel,
            amountCents = event.amountCents,
            totalPaidAfterCents = event.totalPaidAfterCents,
            channelLabel = event.channelEnum().label,
            paymentReference = event.paymentReference,
            verificationStatus = event.verificationStatusEnum(),
            verifiedByUsername = event.verifiedByUsername,
            bankAccount = paymentConfig.bankAccount.ifBlank { PaymentConfig.DEFAULT_BAUTA_BANK_ACCOUNT },
            bankName = paymentConfig.bankName,
            accountHolder = paymentConfig.accountHolder.ifBlank { "Empresa Municipal de Comercio Bauta" },
            issuedAtMillis = event.createdAtMillis,
            note = event.note
        )
    }

    fun suggestedFileName(data: PaymentReceiptData): String {
        val cleanActor = data.actorName
            .replace(Regex("[^A-Za-z0-9]+"), "_")
            .trim('_')
            .take(28)
            .ifBlank { "Actor" }
        val cleanPeriod = data.periodLabel
            .replace(Regex("[^A-Za-z0-9]+"), "_")
            .trim('_')
            .take(20)
        return "Recibo_${data.receiptCode}_${cleanActor}_$cleanPeriod.pdf"
    }

    fun writeReceiptToUri(
        context: Context,
        uri: Uri,
        data: PaymentReceiptData
    ): Result<Unit> {
        return try {
            val stream = context.contentResolver.openOutputStream(uri)
                ?: return Result.failure(IllegalStateException("No se pudo abrir el archivo de destino."))
            stream.use { output ->
                writeReceiptPdf(output, data)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun writeReceiptPdf(output: OutputStream, data: PaymentReceiptData) {
        val document = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val emeraldColor = Color.parseColor("#0B3B2E")
            val goldColor = Color.parseColor("#F5B731")
            val surfaceTint = Color.parseColor("#F3F7F5")
            val borderGray = Color.parseColor("#D5DDD9")
            val darkText = Color.parseColor("#18221E")
            val mutedText = Color.parseColor("#54645E")

            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.2f
                color = borderGray
            }

            // Header background
            fillPaint.color = emeraldColor
            canvas.drawRect(0f, 0f, 595f, 128f, fillPaint)

            // Gold accent strip
            fillPaint.color = goldColor
            canvas.drawRect(0f, 128f, 595f, 134f, fillPaint)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = goldColor
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val headerSmallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 11f
            }

            canvas.drawText("EMPRESA MUNICIPAL DE COMERCIO BAUTA", 42f, 44f, subHeaderPaint)
            canvas.drawText("RECIBO OFICIAL DE PAGO DIGITAL", 42f, 72f, titlePaint)
            canvas.drawText("Artemisa, Cuba · Moneda Nacional (CUP) · Pago 100% Bancarizado", 42f, 96f, headerSmallPaint)
            canvas.drawText("Folio: ${data.receiptCode}", 42f, 114f, headerSmallPaint)

            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = mutedText
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = darkText
                textSize = 12.5f
            }
            val valueBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = darkText
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val issuedDateText = dateFormatter.format(Date(data.issuedAtMillis))

            // Amount highlight card
            var y = 162f
            fillPaint.color = surfaceTint
            val amountBox = RectF(42f, y, 553f, y + 86f)
            canvas.drawRoundRect(amountBox, 12f, 12f, fillPaint)
            canvas.drawRoundRect(amountBox, 12f, 12f, strokePaint)

            canvas.drawText("IMPORTE PAGADO (CUP)", 60f, y + 28f, labelPaint)
            val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = emeraldColor
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(CurrencyFormatter.format(data.amountCents), 60f, y + 60f, amountPaint)

            val statusLabel = "Estado: ${data.verificationStatus.label}"
            canvas.drawText(statusLabel, 370f, y + 36f, valueBoldPaint)
            canvas.drawText("Fecha: $issuedDateText", 370f, y + 58f, valuePaint)

            // Actor section
            y += 116f
            canvas.drawText("DATOS DEL ACTOR ECONÓMICO", 42f, y, labelPaint)
            y += 10f
            val actorBox = RectF(42f, y, 553f, y + 118f)
            canvas.drawRoundRect(actorBox, 10f, 10f, strokePaint)

            var rowY = y + 26f
            canvas.drawText("Actor Económico:", 60f, rowY, labelPaint)
            canvas.drawText(data.actorName.take(46), 195f, rowY, valueBoldPaint)

            rowY += 24f
            canvas.drawText("NIT / Identificador:", 60f, rowY, labelPaint)
            canvas.drawText(data.actorTaxId.ifBlank { "No especificado" }, 195f, rowY, valuePaint)

            rowY += 24f
            canvas.drawText("Local Arrendado:", 60f, rowY, labelPaint)
            canvas.drawText(data.localeName.ifBlank { "Comercio Municipal Bauta" }.take(46), 195f, rowY, valuePaint)

            rowY += 24f
            canvas.drawText("Período Liquidado:", 60f, rowY, labelPaint)
            canvas.drawText(data.periodLabel, 195f, rowY, valueBoldPaint)

            // Digital Payment Details section
            y += 146f
            canvas.drawText("DETALLES DE LA OPERACIÓN TRANSFERMÓVIL / BANCARIA", 42f, y, labelPaint)
            y += 10f
            val bankBox = RectF(42f, y, 553f, y + 144f)
            canvas.drawRoundRect(bankBox, 10f, 10f, strokePaint)

            rowY = y + 26f
            canvas.drawText("Entidad Beneficiaria:", 60f, rowY, labelPaint)
            canvas.drawText(data.accountHolder.take(45), 195f, rowY, valueBoldPaint)

            rowY += 24f
            canvas.drawText("Cuenta Receptora CUP:", 60f, rowY, labelPaint)
            val bankSuffix = if (data.bankName.isNotBlank()) " (${data.bankName})" else ""
            canvas.drawText("${data.bankAccount}$bankSuffix", 195f, rowY, valueBoldPaint)

            rowY += 24f
            canvas.drawText("Canal de Pago Digital:", 60f, rowY, labelPaint)
            canvas.drawText(data.channelLabel, 195f, rowY, valuePaint)

            rowY += 24f
            canvas.drawText("Código de Operación:", 60f, rowY, labelPaint)
            canvas.drawText(data.paymentReference.ifBlank { "Sin referencia" }, 195f, rowY, valueBoldPaint)

            rowY += 24f
            canvas.drawText("Acumulado del Período:", 60f, rowY, labelPaint)
            canvas.drawText(
                CurrencyFormatter.format(data.totalPaidAfterCents.coerceAtLeast(data.amountCents)),
                195f,
                rowY,
                valuePaint
            )

            // Verification & Notes
            y += 174f
            canvas.drawText("CONCILIACIÓN Y OBSERVACIONES", 42f, y, labelPaint)
            y += 10f
            val noteBox = RectF(42f, y, 553f, y + 92f)
            canvas.drawRoundRect(noteBox, 10f, 10f, strokePaint)

            val verifierText = if (data.verifiedByUsername.isNotBlank()) {
                "Conciliación bancaria confirmada · Comercio Bauta"
            } else {
                "Pendiente de conciliación bancaria · Comercio Bauta"
            }
            canvas.drawText(verifierText, 60f, y + 28f, valueBoldPaint)
            val noteText = data.note.ifBlank { "Pago correspondiente al 30% de utilidad neta y alquiler de espacio municipal." }
            canvas.drawText(noteText.take(72), 60f, y + 54f, valuePaint)
            if (noteText.length > 72) {
                canvas.drawText(noteText.drop(72).take(72), 60f, y + 74f, valuePaint)
            }

            // Footer
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = mutedText
                textSize = 9.5f
            }
            canvas.drawLine(42f, 772f, 553f, 772f, strokePaint)
            canvas.drawText(
                "Documento digital emitido por Comercio Bauta · Control contable y compatibilidad Versat Sarasola.",
                42f,
                792f,
                footerPaint
            )
            canvas.drawText(
                "No válido sin respaldo bancario de transferencia Transfermóvil o Bulevar MiTransfer.",
                42f,
                808f,
                footerPaint
            )

            document.finishPage(page)
            document.writeTo(output)
        } finally {
            document.close()
        }
    }
}
