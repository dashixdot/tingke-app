package com.phygroup.tingke.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
import java.io.File

/**
 * 分段录音器。
 *
 * 设计要点（对应设计方案第十二节 A4 / A5）：
 *  - 每一段独立成文件，停止时把该段追加进结果列表，绝不覆盖上一段；
 *  - MediaRecorder.stop() 是同步的，但会抛 RuntimeException（录制过短），必须兜住；
 *  - 权限缺失或初始化失败时降级为「模拟录音」，只计时不产文件，并在 UI 明确告知。
 */
class AudioRecorder(private val ctx: Context) {

    data class Segment(
        val file: File,
        val startSec: Int,
        val endSec: Int,
        val mime: String,
        val sim: Boolean
    )

    private var recorder: MediaRecorder? = null
    private var current: File? = null
    private var currentStart = 0
    private var mime = "audio/mp4"

    /** 当前是否处于录制（含模拟） */
    var active = false
        private set

    /** 本次录音是否为模拟（无文件） */
    var simulated = false
        private set

    private val dir: File
        get() = File(ctx.filesDir, "recordings").apply { if (!exists()) mkdirs() }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    /** 开始一段；返回 true 表示真实录制 */
    fun start(startSec: Int): Boolean {
        if (active) return !simulated
        currentStart = startSec
        if (!hasPermission()) {
            simulated = true
            active = true
            current = null
            return false
        }
        val f = File(dir, "rec_" + System.currentTimeMillis() + ".m4a")
        val mr: MediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(ctx)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        return try {
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(96_000)
            mr.setAudioSamplingRate(44_100)
            mr.setOutputFile(f.absolutePath)
            mr.prepare()
            mr.start()
            recorder = mr
            current = f
            mime = "audio/mp4"
            simulated = false
            active = true
            true
        } catch (t: Throwable) {
            runCatching { mr.release() }
            runCatching { if (f.exists()) f.delete() }
            recorder = null
            current = null
            simulated = true
            active = true
            false
        }
    }

    fun pause() {
        if (active && !simulated) runCatching { recorder?.pause() }
    }

    fun resume() {
        if (active && !simulated) runCatching { recorder?.resume() }
    }

    /** 结束当前段并返回该段；真实录制失败时返回 null（UI 会据此提示无音频） */
    fun stop(endSec: Int): Segment? {
        if (!active) return null
        active = false
        val start = currentStart
        if (simulated) {
            current = null
            return Segment(File(""), start, endSec, "", true)
        }
        val f = current
        val mr = recorder
        recorder = null
        current = null
        var ok = true
        if (mr != null) {
            try {
                mr.stop()
            } catch (t: Throwable) {
                ok = false
            } finally {
                runCatching { mr.release() }
            }
        } else {
            ok = false
        }
        return if (ok && f != null && f.exists() && f.length() > 0) {
            Segment(f, start, endSec, mime, false)
        } else {
            runCatching { f?.delete() }
            null
        }
    }

    /** 放弃当前段（结束本节异常时用） */
    fun cancel() {
        val mr = recorder
        recorder = null
        active = false
        if (mr != null) {
            runCatching { mr.stop() }
            runCatching { mr.release() }
        }
        current = null
    }
}
