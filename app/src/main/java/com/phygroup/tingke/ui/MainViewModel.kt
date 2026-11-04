package com.phygroup.tingke.ui

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phygroup.tingke.RecorderHolder
import com.phygroup.tingke.TingKeApp
import com.phygroup.tingke.audio.AudioRecorder
import com.phygroup.tingke.audio.RecordingService
import com.phygroup.tingke.data.AudioSegmentEntity
import com.phygroup.tingke.data.SessionEntity
import com.phygroup.tingke.data.toDomain
import com.phygroup.tingke.data.toHeader
import com.phygroup.tingke.data.toInfo
import com.phygroup.tingke.data.withInfo
import com.phygroup.tingke.domain.Evals
import com.phygroup.tingke.domain.Info
import com.phygroup.tingke.domain.Meta
import com.phygroup.tingke.domain.Mark
import com.phygroup.tingke.domain.ReportHeader
import com.phygroup.tingke.domain.asHeader
import com.phygroup.tingke.domain.envMarks
import com.phygroup.tingke.domain.evalName
import com.phygroup.tingke.domain.fmt
import com.phygroup.tingke.domain.isEvalOnly
import com.phygroup.tingke.domain.reportCsv
import com.phygroup.tingke.domain.reportText
import com.phygroup.tingke.domain.segDur
import com.phygroup.tingke.domain.todayStr
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class Screen { Start, Clock, Report, History }
enum class RepMode { Share, Text, Csv }

data class ToastMsg(
    val id: Long,
    val text: String,
    val actionLabel: String? = null,
    val action: (() -> Unit)? = null
)

data class HistoryItem(
    val session: SessionEntity,
    val markCount: Int
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as TingKeApp).repo
    private val prefs = app.getSharedPreferences("tingke_prefs", Context.MODE_PRIVATE)
    private val recorder = RecorderHolder.get(app)

    /* ---------- 界面状态 ---------- */
    var screen by mutableStateOf(Screen.Start)
        private set

    var info by mutableStateOf(Info())
        private set

    /** 开始前默认勾选录音（对应需求：默认勾选，可取消） */
    var recEnabled by mutableStateOf(prefs.getBoolean(KEY_REC_ENABLED, true))
        private set

    var running by mutableStateOf(false)
        private set

    var paused by mutableStateOf(false)
        private set

    var elapsed by mutableStateOf(0)
        private set

    val marks = mutableStateListOf<Mark>()

    var sessionId by mutableStateOf<String?>(null)
        private set

    var recActive by mutableStateOf(false)
        private set

    /** true = 麦克风不可用，本次只计时不出音频 */
    var recSim by mutableStateOf(false)
        private set

    val segs = mutableStateListOf<AudioRecorder.Segment>()

    var lastMarkId by mutableStateOf<String?>(null)
        private set

    var toast by mutableStateOf<ToastMsg?>(null)
        private set

    /* ---------- 报告状态 ---------- */
    var reportHeader by mutableStateOf(ReportHeader("", "", "", "", ""))
        private set
    var reportMarks by mutableStateOf<List<Mark>>(emptyList())
        private set
    var reportElapsed by mutableStateOf(0)
        private set
    var reportAudio by mutableStateOf<List<AudioSegmentEntity>>(emptyList())
        private set
    var reportIsHistory by mutableStateOf(false)
        private set

    /** 报告页对应的那一节存档（历史页/结束页都用它回填"听课信息"表单） */
    var reportSession by mutableStateOf<SessionEntity?>(null)
        private set

    var repMode by mutableStateOf(RepMode.Share)

    /** 报告页/历史页"编辑信息"的初始值 */
    fun reportInfo(): Info = reportSession?.toInfo()
        ?: Info(
            title = reportHeader.title,
            cls = reportHeader.cls,
            teacher = reportHeader.teacher,
            date = reportHeader.date,
            period = reportHeader.period,
            teacherOrg = reportHeader.teacherOrg.ifBlank { Meta.DEFAULT_ORG },
            observerOrg = reportHeader.observerOrg.ifBlank { Meta.DEFAULT_ORG },
            observer = reportHeader.observer,
            stage = reportHeader.stage.ifBlank { Meta.DEFAULT_STAGE },
            subject = reportHeader.subject.ifBlank { Meta.DEFAULT_SUBJECT }
        )

    fun reportSessionId(): String? = reportSession?.id

    /* ---------- 历史 ---------- */
    var history by mutableStateOf<List<HistoryItem>>(emptyList())
        private set

    /* ---------- 计时内核 ---------- */
    private var baseSec = 0
    private var runAt = 0L
    private var ticker: Job? = null
    private var current: SessionEntity? = null
    private var toastSeq = 0L

    /** 保证 session 行先落库，再插 marks，否则会撞外键约束 */
    private var sessionJob: Job? = null

    init {
        viewModelScope.launch { restoreRunningIfAny() }
    }

    /* ===================== 工具 ===================== */

    fun tell(text: String, actionLabel: String? = null, action: (() -> Unit)? = null) {
        toastSeq += 1
        toast = ToastMsg(toastSeq, text, actionLabel, action)
    }

    fun clearToast() {
        toast = null
    }

    private fun computeElapsed(): Int {
        val extra = if (running && !paused && runAt > 0L) {
            ((SystemClock.elapsedRealtime() - runAt) / 1000L).toInt()
        } else 0
        return baseSec + extra
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                elapsed = computeElapsed()
                delay(250)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun sortMarks() {
        val sorted = marks.sortedBy { it.ts }
        marks.clear()
        marks.addAll(sorted)
    }

    private fun persistSnapshot() {
        val sid = sessionId ?: return
        val snap = (current ?: return).copy(
            title = info.title,
            teacher = info.teacher,
            cls = info.cls,
            observer = info.observer,
            date = info.date,
            period = info.period,
            count = info.count,
            place = info.place,
            teacherOrg = info.teacherOrg,
            observerOrg = info.observerOrg,
            stage = info.stage,
            subject = info.subject,
            elapsedSec = computeElapsed()
        )
        current = snap
        viewModelScope.launch { repo.saveSession(snap) }
    }

    /* ===================== 信息 ===================== */

    fun saveInfo(newInfo: Info) {
        info = newInfo
        persistSnapshot()
    }

    fun updateRecEnabled(v: Boolean) {
        recEnabled = v
        prefs.edit().putBoolean(KEY_REC_ENABLED, v).apply()
    }

    /* ===================== 开始 / 暂停 / 结束 ===================== */

    fun startSession() {
        val id = UUID.randomUUID().toString()
        sessionId = id
        baseSec = 0
        runAt = SystemClock.elapsedRealtime()
        running = true
        paused = false
        elapsed = 0
        marks.clear()
        segs.clear()
        lastMarkId = null

        val s = SessionEntity(
            id = id,
            title = info.title,
            teacher = info.teacher,
            cls = info.cls,
            observer = info.observer,
            date = if (info.date.isBlank()) todayStr() else info.date,
            period = info.period,
            count = info.count,
            place = info.place,
            startedAt = System.currentTimeMillis(),
            endedAt = null,
            elapsedSec = 0,
            hasAudio = false,
            teacherOrg = info.teacherOrg,
            observerOrg = info.observerOrg,
            stage = info.stage,
            subject = info.subject
        )
        current = s
        // 先插 session 行再插 marks，避免外键顺序问题
        sessionJob = viewModelScope.launch { repo.saveSession(s) }

        startTicker()

        if (recEnabled) {
            // 先起麦克风类型的前台服务，否则熄屏/切后台时录不到声音
            if (recorder.hasPermission()) RecordingService.start(getApplication())
            val real = recorder.start(0)
            recActive = true
            recSim = !real
            if (!real) RecordingService.stop(getApplication())
            tell(if (real) "已开始计时并同步录音" else "已开始计时；麦克风不可用，本节不生成音频")
        } else {
            recActive = false
            recSim = false
            tell("已开始计时（未录音）")
        }
        screen = Screen.Clock
    }

    fun togglePause() {
        if (!running) return
        if (paused) {
            runAt = SystemClock.elapsedRealtime()
            paused = false
            recorder.resume()
            tell("继续计时")
        } else {
            baseSec = computeElapsed()
            runAt = 0L
            paused = true
            elapsed = baseSec
            recorder.pause()
            tell("已暂停（录音同步暂停）")
        }
        persistSnapshot()
    }

    /** 结束本节：等录音段落下后再进报告页 */
    fun endSession() {
        val e = computeElapsed()
        baseSec = e
        runAt = 0L
        elapsed = e
        running = false
        paused = false
        stopTicker()

        val seg = recorder.stop(e)
        RecordingService.stop(getApplication())
        val all = segs.toList().toMutableList()
        if (seg != null) all.add(seg)
        segs.clear()
        segs.addAll(all)
        recActive = false

        val realSegs = all.filter { !it.sim && it.file.exists() }
        val sid = sessionId

        reportHeader = info.asHeader()
        reportMarks = marks.toList()
        reportElapsed = e
        reportIsHistory = false
        repMode = RepMode.Share

        current = current?.copy(
            endedAt = System.currentTimeMillis(),
            elapsedSec = e,
            hasAudio = realSegs.isNotEmpty()
        )
        reportSession = current

        viewModelScope.launch {
            if (sid != null) {
                current?.let { repo.saveSession(it) }
                if (realSegs.isNotEmpty()) {
                    repo.saveAudio(
                        sid,
                        realSegs.mapIndexed { i, s ->
                            AudioSegmentEntity(
                                sessionId = sid,
                                segIndex = i,
                                path = s.file.absolutePath,
                                startSec = s.startSec,
                                endSec = s.endSec,
                                mime = s.mime
                            )
                        }
                    )
                }
                reportAudio = repo.audio(sid)
            }
            screen = Screen.Report
        }
    }

    fun newSession() {
        stopTicker()
        recorder.cancel()
        RecordingService.stop(getApplication())
        sessionJob = null
        sessionId = null
        current = null
        marks.clear()
        segs.clear()
        lastMarkId = null
        running = false
        paused = false
        elapsed = 0
        baseSec = 0
        runAt = 0L
        recActive = false
        recSim = false
        info = Info(date = todayStr())
        reportSession = null
        reportIsHistory = false
        screen = Screen.Start
    }

    fun goStart() {
        screen = Screen.Start
    }

    fun backToClock() {
        if (running || marks.isNotEmpty() || elapsed > 0) screen = Screen.Clock
        else screen = Screen.Start
    }

    /* ===================== 打标 ===================== */

    fun quickTag(label: String) {
        if (!running) { tell("请先开始听课"); return }
        if (paused) { tell("计时已暂停，继续后再打标"); return }
        val m = Mark(UUID.randomUUID().toString(), computeElapsed(), label)
        marks.add(m)
        sortMarks()
        lastMarkId = m.id
        val sid = sessionId
        if (sid != null) viewModelScope.launch {
            sessionJob?.join()
            repo.saveMark(sid, m)
        }
        persistSnapshot()
        tell("已记录：$label ${fmt(m.ts)}", "撤销") { undoMark(m.id) }
    }

    fun undoMark(id: String) {
        val idx = marks.indexOfFirst { it.id == id }
        if (idx < 0) return
        marks.removeAt(idx)
        if (lastMarkId == id) lastMarkId = null
        viewModelScope.launch { repo.deleteMark(id) }
        persistSnapshot()
        tell("已撤销该打标")
    }

    /**
     * 新增或修改一条打标。
     * [ts] 必须是"打开弹窗那一刻"冻结的时刻，而不是保存时刻 —— 见设计方案 12.1 / A2。
     */
    fun commitMark(
        existingId: String?,
        ts: Int,
        label: String,
        sub: String,
        eval: String,
        note: String
    ) {
        if (existingId != null) {
            val idx = marks.indexOfFirst { it.id == existingId }
            if (idx >= 0) {
                val m = marks[idx].copy(label = label, sub = sub, eval = eval, note = note)
                marks[idx] = m
                val sid = sessionId
                if (sid != null) viewModelScope.launch {
                    sessionJob?.join()
                    repo.saveMark(sid, m)
                }
                persistSnapshot()
                tell("已更新")
                return
            }
        }
        val m = Mark(UUID.randomUUID().toString(), ts, label, sub, eval, note)
        marks.add(m)
        sortMarks()
        lastMarkId = m.id
        val sid = sessionId
        if (sid != null) viewModelScope.launch {
            sessionJob?.join()
            repo.saveMark(sid, m)
        }
        persistSnapshot()
        tell("已记录：$label ${fmt(ts)}", "撤销") { undoMark(m.id) }
    }

    /** 删除前的确认文案：必须说清"这段时间归到哪去" */
    fun deleteHint(id: String): String? {
        val i = marks.indexOfFirst { it.id == id }
        if (i < 0) return null
        val m = marks[i]
        // 独立评课标记不占时长，删掉不影响任何环节归属
        if (isEvalOnly(m.label)) {
            return "删除这条评课标记（${evalName(m.eval)} ${fmt(m.ts)}）？\n" +
                "只删除这条评课记录，环节时长与占比不受影响。"
        }
        val env = envMarks(marks)
        val ei = env.indexOfFirst { it.id == id }
        val prev = env.getOrNull(ei - 1)
        val next = env.getOrNull(ei + 1)
        val span = segDur(env, computeElapsed(), ei)
        return when {
            env.size == 1 ->
                "删除这条打标（${m.label} ${fmt(m.ts)}）？\n删除后这 ${fmt(span)} 将变为「未打标」。"
            prev != null ->
                "删除这条打标（${m.label} ${fmt(m.ts)}）？\n" +
                    "删除后 ${fmt(m.ts)}–${fmt(next?.ts ?: computeElapsed())} 这 ${fmt(span)} " +
                    "将并入上一环节「${prev.label}」。"
            else ->
                "删除第一条打标（${m.label} ${fmt(m.ts)}）？\n" +
                    "删除后这 ${fmt(span)} 将并入下一个环节「${next?.label ?: "（无）"}」。"
        }
    }

    fun deleteMark(id: String) {
        undoMark(id)
    }

    fun deleteHintText(m: Mark): String = "${m.label} · ${evalName(m.eval)}"

    /* ===================== 独立评课标记 ===================== */

    /**
     * 轻点评课按钮：立即记一条独立评课标记。
     * 它是时间轴上的点事件，**不参与环节时长与占比**（见 domain.envMarks / shareData）。
     */
    fun quickEval(eval: String) {
        if (!running) { tell("请先开始听课"); return }
        if (paused) { tell("计时已暂停，继续后再评课"); return }
        commitEval(null, computeElapsed(), eval, "")
    }

    /** 新增或修改一条独立评课标记（[ts] 同样是打开弹窗时冻结的时刻） */
    fun commitEval(existingId: String?, ts: Int, eval: String, note: String) {
        if (eval.isBlank()) { tell("请选择评课标记：亮点 / 不足 / 疑问"); return }
        if (existingId != null) {
            val idx = marks.indexOfFirst { it.id == existingId }
            if (idx >= 0) {
                val m = marks[idx].copy(
                    label = Evals.ONLY_LABEL, sub = "", eval = eval, note = note
                )
                marks[idx] = m
                val sid = sessionId
                if (sid != null) viewModelScope.launch {
                    sessionJob?.join()
                    repo.saveMark(sid, m)
                }
                persistSnapshot()
                tell("已更新评课标记")
                return
            }
        }
        val m = Mark(UUID.randomUUID().toString(), ts, Evals.ONLY_LABEL, "", eval, note)
        marks.add(m)
        sortMarks()
        lastMarkId = m.id
        val sid = sessionId
        if (sid != null) viewModelScope.launch {
            sessionJob?.join()
            repo.saveMark(sid, m)
        }
        persistSnapshot()
        tell("已记评课：${evalName(eval)} ${fmt(ts)}", "撤销") { undoMark(m.id) }
    }

    /** 本节的独立评课标记（时间升序） */
    fun evalMarksOf(): List<Mark> = marks.filter { isEvalOnly(it.label) }

    /* ===================== 历史 ===================== */

    fun openHistory() {
        screen = Screen.History
        viewModelScope.launch { loadHistory() }
    }

    /** 重新读一次历史列表（改完听课信息后要刷新列表与课时数） */
    private suspend fun loadHistory() {
        val list = mutableListOf<HistoryItem>()
        repo.sessionsOnce().forEach { s ->
            list.add(HistoryItem(s, repo.marks(s.id).size))
        }
        history = list
    }

    fun viewSession(id: String) {
        viewModelScope.launch {
            val s = repo.session(id) ?: return@launch
            val ms = repo.marks(id)
            reportHeader = s.toHeader()
            reportMarks = ms
            reportElapsed = s.elapsedSec
            reportAudio = repo.audio(id)
            reportSession = s
            reportIsHistory = true
            repMode = RepMode.Share
            screen = Screen.Report
        }
    }

    /**
     * 修改某一节的听课信息（历史记录里补录/改错）。
     * - 正在进行的本节：直接改 [info] 并随快照落库；
     * - 已存档的历史节：Upsert 该 session 行（@Upsert 不会删行，打标与录音分段都保留），
     *   同时刷新历史列表、报告页表头，导出的文本/CSV/占比图立即用新信息。
     */
    fun updateSessionInfo(id: String, newInfo: Info) {
        if (sessionId == id) {
            info = newInfo.copy(date = newInfo.date.ifBlank { info.date })
            persistSnapshot()
            if (!reportIsHistory) {
                reportHeader = info.asHeader()
            }
            viewModelScope.launch { loadHistory() }
            tell("听课信息已更新")
            return
        }
        viewModelScope.launch {
            val s = repo.session(id) ?: return@launch
            val upd = s.withInfo(newInfo)
            repo.saveSession(upd)
            if (reportSession?.id == id) {
                reportSession = upd
                reportHeader = upd.toHeader()
            }
            loadHistory()
            tell("听课信息已更新")
        }
    }

    /** 删除一节：连同该节的录音文件一起删掉（留着也没人听了，白占空间） */
    fun deleteSession(id: String) {
        viewModelScope.launch {
            val audioFiles = repo.audio(id).map { File(it.path) }
            repo.deleteSession(id)
            audioFiles.forEach { f -> runCatching { if (f.exists()) f.delete() } }
            if (reportSession?.id == id) reportSession = null
            openHistory()
            tell("已删除该节听课记录与录音")
        }
    }

    /* ===================== 报告导出 ===================== */

    fun currentReportText(): String = reportText(reportHeader, reportMarks, reportElapsed)

    fun currentReportCsv(): String = reportCsv(reportHeader, reportMarks, reportElapsed)

    /* ===================== 冷启动恢复 ===================== */

    private suspend fun restoreRunningIfAny() {
        val r = repo.findRunning() ?: return
        current = r
        sessionId = r.id
        info = Info(
            title = r.title, teacher = r.teacher, cls = r.cls, observer = r.observer,
            date = r.date, period = r.period, count = r.count, place = r.place,
            teacherOrg = r.teacherOrg, observerOrg = r.observerOrg,
            stage = r.stage, subject = r.subject
        )
        marks.clear()
        marks.addAll(repo.marks(r.id))
        baseSec = r.elapsedSec
        if (baseSec > 0) {
            runAt = SystemClock.elapsedRealtime()
            running = true
            paused = false
            startTicker()
        }
        screen = Screen.Clock
        tell("已恢复上次未结束的计时（录音未恢复）")
    }

    companion object {
        private const val KEY_REC_ENABLED = "rec_enabled"
    }
}
