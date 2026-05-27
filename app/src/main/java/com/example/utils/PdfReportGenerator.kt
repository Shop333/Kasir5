package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.TransactionWithItems
import com.example.ui.viewmodel.ProfitLossSummary
import java.io.File
import java.io.FileOutputStream

object PdfReportGenerator {

    fun generateAndShareReport(
        context: Context,
        transactions: List<TransactionWithItems>,
        summary: ProfitLossSummary
    ) {
        if (transactions.isEmpty()) {
            Toast.makeText(context, "Tidak ada data transaksi untuk diekspor", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val pdfDocument = PdfDocument()
            
            // Standard A4 width = 595, height = 842
            val pageWidth = 595
            val pageHeight = 842
            
            val paint = Paint()
            val textPaint = Paint().apply {
                isAntiAlias = true
                textSize = 9f
                color = Color.BLACK
            }
            
            val boldPaint = Paint().apply {
                isAntiAlias = true
                textSize = 9f
                color = Color.BLACK
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            
            val titlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 14f
                color = Color.rgb(0, 77, 64) // #004D40 Deep Teal
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            
            val subtitlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 9f
                color = Color.DKGRAY
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            }

            var currentPageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            
            // Draw Header Banner
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 55f, Paint().apply { color = Color.rgb(0, 77, 64) })
            
            paint.color = Color.WHITE
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("SEMBAKO MODERN POS", 24f, 24f, paint)
            
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Sistem Aplikasi Kasir & Manajemen Stok Offline Sembako", 24f, 40f, paint)
            
            paint.color = Color.BLACK
            canvas.drawText("LAPORAN KINERJA KEUANGAN & LABA RUGI TOKO", 24f, 85f, titlePaint)
            
            val formattedTime = FormatUtils.formatDateTime(System.currentTimeMillis())
            canvas.drawText("Dokumen laporan ini dicetak secara otomatis pada: $formattedTime", 24f, 102f, subtitlePaint)
            
            // Summary Stat Section background
            val statsY = 120f
            val statsHeight = 65f
            canvas.drawRoundRect(
                24f, statsY, (pageWidth - 24).toFloat(), statsY + statsHeight,
                8f, 8f,
                Paint().apply { color = Color.rgb(240, 244, 244) }
            )
            
            // Total Income
            val colWidth = (pageWidth - 48) / 3f
            
            // Draw labels
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.rgb(100, 110, 110)
            canvas.drawText("PENDAPATAN POS", 36f, statsY + 22f, paint)
            canvas.drawText("HPP (MODAL TOKO)", 36f + colWidth, statsY + 22f, paint)
            canvas.drawText("ESTIMASI LABA BERSIH", 36f + colWidth * 2f, statsY + 22f, paint)
            
            // Draw values
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            
            paint.color = Color.rgb(0, 77, 64) // primary Theme Color
            canvas.drawText(FormatUtils.formatRupiah(summary.revenue), 36f, statsY + 44f, paint)
            
            paint.color = Color.rgb(216, 67, 21) // Alert / Warning orange-red
            canvas.drawText(FormatUtils.formatRupiah(summary.costOfGoods), 36f + colWidth, statsY + 44f, paint)
            
            paint.color = Color.rgb(46, 125, 50) // Profit green
            canvas.drawText(FormatUtils.formatRupiah(summary.netProfit), 36f + colWidth * 2f, statsY + 44f, paint)
            
            // Table Header Title
            canvas.drawText("Log Riwayat Transaksi Penjualan", 24f, 215f, titlePaint.apply { textSize = 11f })
            
            // Table Headers
            val tableY = 230f
            val headerPaint = Paint().apply {
                color = Color.rgb(224, 230, 230)
            }
            canvas.drawRect(24f, tableY, (pageWidth - 24).toFloat(), tableY + 24f, headerPaint)
            
            val colX1 = 28f          // Nota ID
            val colX2 = 110f         // Waktu / Tanggal
            val colX3 = 240f         // Kasir Utama
            val colX4 = 370f         // Total Item
            val colX5 = 450f         // Total Belanja
            
            canvas.drawText("No. Nota", colX1, tableY + 15f, boldPaint)
            canvas.drawText("Tanggal & Waktu", colX2, tableY + 15f, boldPaint)
            canvas.drawText("Kasir Penanggungjawab", colX3, tableY + 15f, boldPaint)
            canvas.drawText("Kuantitas", colX4, tableY + 15f, boldPaint)
            canvas.drawText("Total Belanja (IDR)", colX5, tableY + 15f, boldPaint)
            
            var currentY = tableY + 24f
            val rowHeight = 24f
            
            for (idx in transactions.indices) {
                // If item exceeds current page, print footer, end page, create new page!
                if (currentY + rowHeight > pageHeight - 50f) {
                    // Draw Footer on current page
                    paint.textSize = 8f
                    paint.color = Color.GRAY
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                    canvas.drawText("Halaman $currentPageNumber • Dokumen Offline Sembako Modern", 24f, (pageHeight - 20).toFloat(), paint)
                    
                    pdfDocument.finishPage(page)
                    
                    currentPageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    
                    // Draw mini header on next page
                    canvas.drawRect(0f, 0f, pageWidth.toFloat(), 30f, Paint().apply { color = Color.rgb(0, 77, 64) })
                    paint.color = Color.WHITE
                    paint.textSize = 10f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("Laporan Riwayat Penjualan Sembako - Halaman $currentPageNumber", 24f, 19f, paint)
                    
                    // Re-draw Table Header on next page
                    val nextTableY = 50f
                    canvas.drawRect(24f, nextTableY, (pageWidth - 24).toFloat(), nextTableY + 20f, headerPaint)
                    canvas.drawText("No. Nota", colX1, nextTableY + 13f, boldPaint)
                    canvas.drawText("Tanggal & Waktu", colX2, nextTableY + 13f, boldPaint)
                    canvas.drawText("Kasir Penanggungjawab", colX3, nextTableY + 13f, boldPaint)
                    canvas.drawText("Kuantitas", colX4, nextTableY + 13f, boldPaint)
                    canvas.drawText("Total Belanja (IDR)", colX5, nextTableY + 13f, boldPaint)
                    
                    currentY = nextTableY + 20f
                }
                
                val txWithItems = transactions[idx]
                
                // Color strip for alternating rows
                if (idx % 2 == 1) {
                    canvas.drawRect(24f, currentY, (pageWidth - 24).toFloat(), currentY + rowHeight, Paint().apply { color = Color.rgb(247, 249, 249) })
                }
                
                // Draw horizontal small row divider lines
                canvas.drawLine(24f, currentY + rowHeight, (pageWidth - 24).toFloat(), currentY + rowHeight, Paint().apply { color = Color.rgb(235, 235, 235) })
                
                canvas.drawText("#SBM-${txWithItems.transaction.id}", colX1, currentY + 15f, textPaint)
                canvas.drawText(FormatUtils.formatDateTime(txWithItems.transaction.timestamp), colX2, currentY + 15f, textPaint)
                canvas.drawText(txWithItems.transaction.cashierName, colX3, currentY + 15f, textPaint)
                
                val totalQty = txWithItems.items.sumOf { it.quantity }
                canvas.drawText("$totalQty item", colX4, currentY + 15f, textPaint)
                canvas.drawText(FormatUtils.formatRupiah(txWithItems.transaction.totalPrice), colX5, currentY + 15f, boldPaint)
                
                currentY += rowHeight
            }
            
            // Draw last page footer
            paint.textSize = 8f
            paint.color = Color.GRAY
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("Halaman $currentPageNumber • Sembako Modern Offline POS • Laporan Keuangan Selesai", 24f, (pageHeight - 20).toFloat(), paint)
            
            pdfDocument.finishPage(page)
            
            // Save the PDF file to private cache directory, which does not require WRITE_EXTERNAL_STORAGE permissions
            val outputDir = context.cacheDir
            val pdfFile = File(outputDir, "Lap_Keuangan_Sembako_Modern.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()
            
            // Trigger sharing
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Keuangan Sembako Modern")
                putExtra(Intent.EXTRA_TEXT, "Terlampir Laporan Kinerja Keuangan Sembako Modern POS Offline. Dicetak terhitung seketika.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            val chooser = Intent.createChooser(intent, "Simpan / Cetak Laporan PDF via:")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membuat PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
