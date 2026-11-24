package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.model.InvoiceRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VersatSarasolaExporter {

    private const val UTF8_BOM = "\uFEFF"

    fun suggestedCsvFileName(year: Int, month: Int): String {
        val monthStr = month.toString().padStart(2, '0')
        return "Versat_Sarasola_Facturas_${year}_$monthStr.csv"
    }

    fun suggestedXmlFileName(year: Int, month: Int): String {
        val monthStr = month.toString().padStart(2, '0')
        return "Versat_Sarasola_Facturas_${year}_$monthStr.xml"
    }

    fun writeCsvToUri(
        context: Context,
        uri: Uri,
        invoices: List<InvoiceRecord>
    ): Result<Unit> {
        return try {
            val content = buildVersatCsv(invoices)
            val stream = context.contentResolver.openOutputStream(uri)
                ?: return Result.failure(IllegalStateException("No se pudo escribir el archivo CSV para Versat Sarasola."))
            stream.use { output ->
                output.write(content.toByteArray(Charsets.UTF_8))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun writeXmlToUri(
        context: Context,
        uri: Uri,
        invoices: List<InvoiceRecord>
    ): Result<Unit> {
        return try {
            val content = buildVersatXml(invoices)
            val stream = context.contentResolver.openOutputStream(uri)
                ?: return Result.failure(IllegalStateException("No se pudo escribir el archivo XML para Versat Sarasola."))
            stream.use { output ->
                output.write(content.toByteArray(Charsets.UTF_8))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun buildVersatCsv(invoices: List<InvoiceRecord>): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        val sb = StringBuilder()
        sb.append(UTF8_BOM)
        sb.appendLine(
            "No_Factura;Fecha_Emision;Periodo;Cliente_Actor;NIT_Cliente;Concepto;" +
                "Cuenta_Debito_Banco;Cuenta_Credito_Ingreso;Centro_Costo;" +
                "Aporte_Utilidad_CUP;Alquiler_Espacio_CUP;Importe_Total_CUP;Moneda;" +
                "Via_Pago;Referencia_Transfermovil;Emitida_Por"
        )

        for (inv in invoices) {
            val dateStr = dateFormat.format(Date(inv.issueDateMillis.coerceAtLeast(1L)))
            val periodStr = "${inv.periodYear}-${inv.periodMonth.toString().padStart(2, '0')}"
            val fields = listOf(
                inv.invoiceNumber,
                dateStr,
                periodStr,
                inv.actorName,
                inv.actorTaxId.ifBlank { "SIN-NIT" },
                inv.concept,
                inv.debitAccountCode.ifBlank { InvoiceRecord.DEFAULT_VERSAT_DEBIT_ACCOUNT },
                inv.creditAccountCode.ifBlank { InvoiceRecord.DEFAULT_VERSAT_CREDIT_ACCOUNT },
                inv.costCenter.ifBlank { InvoiceRecord.DEFAULT_VERSAT_COST_CENTER },
                formatDecimalCents(inv.profitShareCents),
                formatDecimalCents(inv.spaceRentCents),
                formatDecimalCents(inv.totalAmountCents),
                "CUP",
                inv.channelEnum().label,
                inv.paymentReference,
                inv.issuedByUsername
            )
            sb.appendLine(fields.joinToString(separator = ";") { csvEscape(it) })
        }
        return sb.toString()
    }

    fun buildVersatXml(invoices: List<InvoiceRecord>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<VersatSarasolaComprobantes entidad="Empresa Municipal de Comercio Bauta" moneda="CUP">""")
        for (inv in invoices) {
            val dateStr = dateFormat.format(Date(inv.issueDateMillis.coerceAtLeast(1L)))
            val periodStr = "${inv.periodYear}-${inv.periodMonth.toString().padStart(2, '0')}"
            sb.appendLine("  <Comprobante>")
            sb.appendLine("    <NoFactura>${xmlEscape(inv.invoiceNumber)}</NoFactura>")
            sb.appendLine("    <FechaEmision>$dateStr</FechaEmision>")
            sb.appendLine("    <Periodo>$periodStr</Periodo>")
            sb.appendLine("    <ClienteActor>${xmlEscape(inv.actorName)}</ClienteActor>")
            sb.appendLine("    <NITCliente>${xmlEscape(inv.actorTaxId.ifBlank { "SIN-NIT" })}</NITCliente>")
            sb.appendLine("    <Concepto>${xmlEscape(inv.concept)}</Concepto>")
            sb.appendLine("    <CuentaDebito>${xmlEscape(inv.debitAccountCode.ifBlank { InvoiceRecord.DEFAULT_VERSAT_DEBIT_ACCOUNT })}</CuentaDebito>")
            sb.appendLine("    <CuentaCredito>${xmlEscape(inv.creditAccountCode.ifBlank { InvoiceRecord.DEFAULT_VERSAT_CREDIT_ACCOUNT })}</CuentaCredito>")
            sb.appendLine("    <CentroCosto>${xmlEscape(inv.costCenter.ifBlank { InvoiceRecord.DEFAULT_VERSAT_COST_CENTER })}</CentroCosto>")
            sb.appendLine("    <AporteUtilidadCUP>${formatDecimalCents(inv.profitShareCents)}</AporteUtilidadCUP>")
            sb.appendLine("    <AlquilerEspacioCUP>${formatDecimalCents(inv.spaceRentCents)}</AlquilerEspacioCUP>")
            sb.appendLine("    <ImporteTotalCUP>${formatDecimalCents(inv.totalAmountCents)}</ImporteTotalCUP>")
            sb.appendLine("    <Moneda>CUP</Moneda>")
            sb.appendLine("    <ViaPago>${xmlEscape(inv.channelEnum().label)}</ViaPago>")
            sb.appendLine("    <ReferenciaTransfermovil>${xmlEscape(inv.paymentReference)}</ReferenciaTransfermovil>")
            sb.appendLine("    <EmitidaPor>${xmlEscape(inv.issuedByUsername)}</EmitidaPor>")
            sb.appendLine("  </Comprobante>")
        }
        sb.appendLine("</VersatSarasolaComprobantes>")
        return sb.toString()
    }

    fun formatDecimalCents(cents: Long): String {
        val negative = cents < 0L
        val abs = if (negative) -cents else cents
        val pesos = abs / 100L
        val rem = (abs % 100L).toString().padStart(2, '0')
        return if (negative) "-$pesos.$rem" else "$pesos.$rem"
    }

    private fun csvEscape(raw: String): String {
        val needsQuotes = raw.contains(';') || raw.contains('"') || raw.contains('\n') || raw.contains('\r')
        if (!needsQuotes) return raw
        return "\"${raw.replace("\"", "\"\"")}\""
    }

    private fun xmlEscape(raw: String): String =
        raw.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
