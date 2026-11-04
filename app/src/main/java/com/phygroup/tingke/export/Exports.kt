package com.phygroup.tingke.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.phygroup.tingke.domain.Labels
import com.phygroup.tingke.domain.Mark
import com.phygroup.tingke.domain.ReportHeader
import com.phygroup.tingke.domain.envMarks
import com.phygroup.tingke.domain.fmt
import com.phygroup.tingke.domain.isEvalOnly
import com.phygroup.tingke.domain.joinedTeacher
import com.phygroup.tingke.domain.reportCsv
import com.phygroup.tingke.domain.reportText
import com.phygroup.tingke.domain.shareData
import java.io.File

object Exports {

    private fun authority(ctx: Context) = ctx.packageName + ".fileprovider"

    private fun shareDir(ctx: Context): File =
        File(ctx.cacheDir, "share").apply { if (!exists()) mkdirs() }

    /* ---------------- 文本 ---------------- */

    fun copyText(ctx: Context, text: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("听课时间轴", text))
    }

    fun shareText(ctx: Context, subject: String, text: String) {
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        ctx.startActivity(
            Intent.createChooser(i, "分享听课记录").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /* ---------------- CSV ---------------- */

    private fun safeName(s: String): String {
        val t = s.trim().ifBlank { "听课" }
        return t.replace(Regex("""[\\/:*?"<>|\s]+"""), "_").take(40)
    }

    fun shareCsv(ctx: Context, header: ReportHeader, marks: List<Mark>, elapsed: Int) {
        val name = safeName(header.title) + "-时间轴.csv"
        val f = File(shareDir(ctx), name)
        // 已在 reportCsv 里带上 UTF-8 BOM，Excel 直接双击不乱码
        f.writeText(reportCsv(header, marks, elapsed), Charsets.UTF_8)
        shareFile(ctx, f, "text/csv", "听课时间轴 CSV")
    }

    fun shareText2(ctx: Context, header: ReportHeader, marks: List<Mark>, elapsed: Int) {
        shareText(ctx, "【听课记录】" + header.title, reportText(header, marks, elapsed))
    }

    /* ---------------- 占比长图 ---------------- */

    fun shareSharePng(ctx: Context, header: ReportHeader, marks: List<Mark>, elapsed: Int) {
        val bmp = buildShareBitmap(header, marks, elapsed)
        val name = safeName(header.title) + "-环节占比.png"
        val f = File(shareDir(ctx), name)
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        shareFile(ctx, f, "image/png", "环节占比长图")
    }

    private fun shareFile(ctx: Context, f: File, mime: String, subject: String) {
        val uri: Uri = FileProvider.getUriForFile(ctx, authority(ctx), f)
        val i = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(
            Intent.createChooser(i, subject).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /* ---------------- 录音分发 / 导出 ---------------- */

    /** 分享单段录音（也可在系统面板里选"保存到文件"） */
    fun shareAudio(ctx: Context, file: File, subject: String) {
        if (!file.exists() || file.length() <= 0) return
        shareFile(ctx, file, "audio/mp4", subject)
    }

    /** 一次分享多段录音 */
    fun shareAudioAll(ctx: Context, files: List<File>, subject: String) {
        val exists = files.filter { it.exists() && it.length() > 0 }
        if (exists.isEmpty()) return
        if (exists.size == 1) {
            shareAudio(ctx, exists.first(), subject)
            return
        }
        val uris = ArrayList<Uri>()
        exists.forEach { f ->
            runCatching { uris.add(FileProvider.getUriForFile(ctx, authority(ctx), f)) }
        }
        if (uris.isEmpty()) return
        val i = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "audio/mp4"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(
            Intent.createChooser(i, subject).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** 用 Canvas 画一张 900px 宽的占比长图，方便直接发教研组微信群 */
    fun buildShareBitmap(header: ReportHeader, marks: List<Mark>, elapsed: Int): Bitmap {
        val data = shareData(marks, elapsed)
        val rows = data.rows.size + if (data.unmarked > 0) 1 else 0

        val w = 900
        val headH = 200
        val rowH = 78
        val footH = 80
        val h = headH + rows * rowH + footH

        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(AColor.WHITE)

        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        p.color = AColor.rgb(0x25, 0x63, 0xEB)
        c.drawRect(0f, 0f, w.toFloat(), 132f, p)

        p.color = AColor.WHITE
        p.textSize = 38f
        c.drawText(header.title.ifBlank { "听课记录" }, 40f, 66f, p)

        p.typeface = Typeface.DEFAULT
        p.textSize = 21f
        p.color = AColor.rgb(0xDB, 0xEA, 0xFE)
        val sub = listOf(
            listOf(header.stage, header.subject).filter { it.isNotBlank() }.joinToString(""),
            header.cls,
            joinedTeacher(header.teacher, header.teacherOrg),
            header.date,
            header.period
        ).filter { it.isNotBlank() }.joinToString(" · ")
        c.drawText(sub.ifBlank { "—" }, 40f, 104f, p)

        p.color = AColor.rgb(0x1F, 0x29, 0x37)
        p.textSize = 24f
        val evalCount = marks.count { isEvalOnly(it.label) }
        c.drawText(
            "总时长 ${fmt(data.total)}　·　打标 ${envMarks(marks).size} 条" +
                if (evalCount > 0) "　·　评课 $evalCount 条（不计入占比）" else "",
            40f, 176f, p
        )

        var y = headH + 26f
        val bar = RectF(40f, 0f, (w - 40).toFloat(), 0f)

        fun row(name: String, dur: Int, pct: Double, color: Int) {
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            p.textSize = 24f
            p.color = AColor.rgb(0x1F, 0x29, 0x37)
            c.drawText(name, 40f, y, p)

            p.color = AColor.rgb(0x25, 0x63, 0xEB)
            p.textAlign = Paint.Align.RIGHT
            c.drawText("${fmt(dur)} · ${String.format("%.1f%%", pct)}", (w - 40).toFloat(), y, p)
            p.textAlign = Paint.Align.LEFT

            bar.top = y + 14f
            bar.bottom = y + 32f
            p.color = AColor.rgb(0xE8, 0xED, 0xF3)
            c.drawRoundRect(bar, 9f, 9f, p)
            p.color = color
            val wBar = (bar.width() * (pct.coerceIn(0.0, 100.0) / 100.0)).toFloat()
            if (wBar > 0f) {
                c.drawRoundRect(
                    RectF(bar.left, bar.top, bar.left + wBar.coerceAtLeast(4f), bar.bottom),
                    9f, 9f, p
                )
            }
            y += rowH
        }

        data.rows.forEach { r ->
            row(r.label, r.dur, r.pct, Labels.colorOf(r.label).toArgbInt())
        }
        if (data.unmarked > 0) {
            row("未打标", data.unmarked, data.unmarkedPct, AColor.rgb(0x9C, 0xA3, 0xAF))
        }

        p.typeface = Typeface.DEFAULT
        p.textSize = 19f
        p.color = AColor.rgb(0x94, 0xA3, 0xB8)
        c.drawText("由「听课打标」生成 · 占比 = 该类累计时长 ÷ 总计时", 40f, (h - 30).toFloat(), p)

        return bmp
    }
}

/** Compose Color → ARGB int，避免在 Canvas 里混用两套颜色类型 */
private fun androidx.compose.ui.graphics.Color.toArgbInt(): Int =
    android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
