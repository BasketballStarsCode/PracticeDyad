package com.practicedyad.app.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.print.PrintAttributes
import android.print.pdf.PrintedPdfDocument
import androidx.core.content.FileProvider
import com.practicedyad.app.data.model.TrainingPlan
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    fun exportPlan(context: Context, plan: TrainingPlan): Uri {
        val document = PrintedPdfDocument(
            context,
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()
        )

        var page = document.startPage(0)
        val canvas = page.canvas
        val paint = android.graphics.Paint().apply {
            textSize = 24f
            color = android.graphics.Color.BLACK
        }

        var y = 60f
        canvas.drawText(plan.name, 40f, y, paint)
        y += 40f

        paint.textSize = 16f
        plan.workoutUnits.forEach { unit ->
            paint.textSize = 18f
            paint.isFakeBoldText = true
            if (y > 1000f) {
                document.finishPage(page)
                page = document.startPage(document.pages.size)
                y = 60f
            }
            canvas.drawText(unit.name, 40f, y, paint)
            y += 30f

            paint.textSize = 14f
            paint.isFakeBoldText = false
            unit.exercises.forEach { ex ->
                val repsOrTime = if (ex.durationSeconds > 0) "${ex.durationSeconds}s" else "${ex.reps} Wdh."
                canvas.drawText("  • ${ex.customName}  –  ${ex.sets}× $repsOrTime  |  Pause: ${ex.restSeconds}s", 50f, y, paint)
                y += 24f
                if (ex.customDescription.isNotEmpty()) {
                    paint.textSize = 11f
                    paint.color = android.graphics.Color.GRAY
                    canvas.drawText("    ${ex.customDescription.take(80)}", 50f, y, paint)
                    y += 20f
                    paint.textSize = 14f
                    paint.color = android.graphics.Color.BLACK
                }
            }
            y += 16f
        }

        document.finishPage(page)

        val dir = File(context.cacheDir, "pdf")
        dir.mkdirs()
        val file = File(dir, "${plan.name.replace(" ", "_")}.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
