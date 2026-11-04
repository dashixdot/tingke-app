package com.phygroup.tingke.ui

import android.media.MediaPlayer
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phygroup.tingke.data.SessionEntity
import com.phygroup.tingke.data.toInfo
import com.phygroup.tingke.domain.Evals
import com.phygroup.tingke.domain.Labels
import com.phygroup.tingke.domain.evalName
import com.phygroup.tingke.domain.fmt
import com.phygroup.tingke.domain.isEvalOnly
import com.phygroup.tingke.domain.joinedTeacher
import com.phygroup.tingke.domain.shareData
import com.phygroup.tingke.domain.stampOf
import com.phygroup.tingke.export.Exports
import kotlinx.coroutines.delay
import java.io.File

/* ===================== 报告页 ===================== */

@Composable
fun ReportScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    // 结束后的报告回主页；从历史进来的报告回历史
    val goBack: () -> Unit = { if (vm.reportIsHistory) vm.openHistory() else vm.goStart() }
    BackHandler { goBack() }

    // 历史报告页可修改听课信息（课题/教师/班级/日期/节次/人数/地点）
    var showInfo by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TkTopBar(
            title = vm.reportHeader.title.ifBlank { "听课记录" },
            meta = listOf(
                listOf(vm.reportHeader.stage, vm.reportHeader.subject)
                    .filter { it.isNotBlank() }.joinToString(""),
                vm.reportHeader.cls,
                joinedTeacher(vm.reportHeader.teacher, vm.reportHeader.teacherOrg),
                vm.reportHeader.date, vm.reportHeader.period
            ).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "—" }
        ) {
            TkBackButton { goBack() }
            if (vm.reportIsHistory) TkMiniButton("编辑信息") { showInfo = true }
            if (!vm.reportIsHistory) TkMiniButton("历史") { vm.openHistory() }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (vm.reportIsHistory) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Text(
                        "这是历史记录（只读，可从右上「编辑信息」改正听课信息）：" +
                            "${vm.reportHeader.title.ifBlank { "未命名" }}" +
                            "　${vm.reportHeader.date}　时长 ${fmt(vm.reportElapsed)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        lineHeight = 17.sp
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            /* --- 录音 --- */
            AudioSection(vm)

            /* --- Tab --- */
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TabButton("环节占比", vm.repMode == RepMode.Share, Modifier.weight(1f)) {
                    vm.repMode = RepMode.Share
                }
                TabButton("文本时间轴", vm.repMode == RepMode.Text, Modifier.weight(1f)) {
                    vm.repMode = RepMode.Text
                }
                TabButton("CSV", vm.repMode == RepMode.Csv, Modifier.weight(1f)) {
                    vm.repMode = RepMode.Csv
                }
            }
            Spacer(Modifier.height(12.dp))

            when (vm.repMode) {
                RepMode.Share -> ShareTab(vm)
                RepMode.Text -> CodeBox(vm.currentReportText())
                RepMode.Csv -> CodeBox(vm.currentReportCsv().removePrefix("\uFEFF"))
            }

            /* --- 导出 --- */
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExportButton("下载 CSV", Modifier.weight(1f)) {
                    runCatching {
                        Exports.shareCsv(ctx, vm.reportHeader, vm.reportMarks, vm.reportElapsed)
                    }
                }
                ExportButton("导出占比长图", Modifier.weight(1f)) {
                    runCatching {
                        Exports.shareSharePng(ctx, vm.reportHeader, vm.reportMarks, vm.reportElapsed)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExportButton("复制文本", Modifier.weight(1f)) {
                    Exports.copyText(ctx, vm.currentReportText())
                    vm.tell("已复制到剪贴板")
                }
                ExportButton("分享文本", Modifier.weight(1f)) {
                    runCatching {
                        Exports.shareText2(ctx, vm.reportHeader, vm.reportMarks, vm.reportElapsed)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // 底部
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            TkBarButton2("历史记录", Modifier.weight(1f), false) { vm.openHistory() }
            TkBarButton2("再听一节", Modifier.weight(1f), false) { vm.newSession() }
        }
    }

    if (showInfo) {
        val sid = vm.reportSessionId()
        if (sid == null) {
            showInfo = false
        } else {
            InfoEditorDialog(
                initial = vm.reportInfo(),
                onDismiss = { showInfo = false },
                onSave = { ni ->
                    vm.updateSessionInfo(sid, ni)
                    showInfo = false
                }
            )
        }
    }
}

@Composable
private fun TabButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExportButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TkBarButton2(
    text: String,
    modifier: Modifier = Modifier,
    danger: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (danger) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (danger) LocalTkExtra.current.danger
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CodeBox(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LocalTkExtra.current.track)
            .padding(14.dp)
    ) {
        Text(
            text,
            fontSize = 12.sp,
            lineHeight = 19.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/* ===================== 环节占比 ===================== */

@Composable
private fun ShareTab(vm: MainViewModel) {
    val data = shareData(vm.reportMarks, vm.reportElapsed)
    if (vm.reportMarks.isEmpty()) {
        Text(
            "暂无打标记录，无法统计环节占比。",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 40.dp)
        )
        return
    }
    Column(Modifier.fillMaxWidth()) {
        Text(
            "总时长 ${fmt(data.total)}，共 ${vm.reportMarks.size} 条记录（其中评课标记 " +
                "${vm.reportMarks.count { isEvalOnly(it.label) }} 条不计入占比）；" +
                "占比 = 该类累计时长 ÷ 总计时（保留 1 位小数）。",
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))

        data.rows.forEach { r ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    r.label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Labels.colorOf(r.label)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${fmt(r.dur)} · ${String.format("%.1f", r.pct)}%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(5.dp))
            RatioBar((r.pct / 100.0).toFloat(), Labels.colorOf(r.label))
            Spacer(Modifier.height(12.dp))
        }

        if (data.unmarked > 0) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "未打标",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${fmt(data.unmarked)} · ${String.format("%.1f", data.unmarkedPct)}%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(5.dp))
            RatioBar(
                (data.unmarkedPct / 100.0).toFloat(),
                MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
        }

        androidx.compose.material3.Divider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(
                "合计 ${fmt(data.total)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            val covered = data.rows.sumOf { it.pct } + (if (data.unmarked > 0) data.unmarkedPct else 0.0)
            Text(
                "已归属 ${String.format("%.1f", covered)}%",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 独立评课标记：只列清单，不参与占比
        val evalList = vm.reportMarks.filter { isEvalOnly(it.label) && it.eval.isNotBlank() }
        if (evalList.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text(
                "评课标记（${evalList.size} 条，不计入环节占比）",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Labels.colorOf(Evals.ONLY_LABEL)
            )
            Spacer(Modifier.height(6.dp))
            evalList.forEach { m ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(
                        fmt(m.ts),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(52.dp)
                    )
                    Text(
                        evalName(m.eval),
                        fontSize = 12.sp,
                        color = Labels.colorOf(Evals.ONLY_LABEL),
                        modifier = Modifier.width(68.dp)
                    )
                    Text(
                        m.note.ifBlank { "—" },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/* ===================== 录音回听 ===================== */

@Composable
private fun AudioSection(vm: MainViewModel) {
    val ctx = LocalContext.current
    val baseName = vm.reportHeader.title.ifBlank { "听课" }

    if (vm.reportIsHistory) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(LocalExtraWarnBg())
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Text(
                if (vm.reportAudio.isEmpty()) "本次未保存音频文件。"
                else "历史记录附带 ${vm.reportAudio.size} 段录音。",
                fontSize = 12.sp,
                color = LocalTkExtra.current.warnFg,
                lineHeight = 17.sp
            )
        }
        Spacer(Modifier.height(10.dp))
    }

    // 统一成 (标题, 文件) 列表，历史与本次走同一套渲染
    val items: List<Pair<String, File>> = if (vm.reportIsHistory) {
        vm.reportAudio.map {
            Pair(
                if (vm.reportAudio.size > 1)
                    "第 ${it.segIndex + 1} 段 · ${fmt(it.startSec)}–${fmt(it.endSec)}"
                else "录音回听",
                File(it.path)
            )
        }
    } else {
        val reals = vm.segs.filter { !it.sim && it.file.exists() && it.file.length() > 0 }
        reals.mapIndexed { i, s ->
            Pair(
                if (reals.size > 1)
                    "第 ${i + 1} 段 · ${fmt(s.startSec)}–${fmt(s.endSec)}"
                else "录音回听（累计 ${fmt(reals.sumOf { it.endSec - it.startSec })}）",
                s.file
            )
        }
    }
    val existing = items.filter { it.second.exists() && it.second.length() > 0 }

    if (existing.isNotEmpty()) {
        existing.forEachIndexed { i, pair ->
            val (t, f) = pair
            AudioRow(
                title = t,
                path = f.absolutePath,
                onShare = { Exports.shareAudio(ctx, f, "$baseName-录音${i + 1}") }
            )
        }
        if (existing.size > 1) {
            Spacer(Modifier.height(2.dp))
            ExportButton("分享全部录音", Modifier.fillMaxWidth()) {
                Exports.shareAudioAll(ctx, existing.map { it.second }, "$baseName-全部录音")
            }
        }
        Spacer(Modifier.height(10.dp))
    } else if (!vm.reportIsHistory && vm.segs.any { it.sim }) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(LocalExtraWarnBg())
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Text(
                "⚠ 本次为模拟录音，未生成音频文件（麦克风不可用）。时长统计仍然有效。",
                fontSize = 12.sp,
                color = LocalTkExtra.current.warnFg,
                lineHeight = 17.sp
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun LocalExtraWarnBg(): Color = LocalTkExtra.current.warnBg

/**
 * 单段录音：播放/暂停 + 可拖动进度 + 已播/总时长 + 分享。
 * 播放结束会把状态复位（否则会一直停在"暂停"）。
 */
@Composable
private fun AudioRow(title: String, path: String, onShare: () -> Unit) {
    var player by remember(path) { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember(path) { mutableStateOf(false) }
    var ready by remember(path) { mutableStateOf(false) }
    var durMs by remember(path) { mutableIntStateOf(0) }
    var posMs by remember(path) { mutableIntStateOf(0) }
    var err by remember(path) { mutableStateOf<String?>(null) }

    DisposableEffect(path) {
        val p = MediaPlayer()
        player = p
        p.setOnPreparedListener {
            durMs = runCatching { p.duration }.getOrDefault(0)
            ready = true
        }
        p.setOnCompletionListener {
            playing = false
            posMs = 0
            runCatching { p.seekTo(0) }
        }
        p.setOnErrorListener { _, _, _ ->
            err = "音频无法播放"
            ready = false
            playing = false
            true
        }
        runCatching {
            p.setDataSource(path)
            p.prepareAsync()
        }.onFailure { err = it.message }
        onDispose {
            runCatching { p.release() }
            player = null
        }
    }

    LaunchedEffect(playing) {
        while (playing) {
            posMs = runCatching { player?.currentPosition ?: 0 }.getOrDefault(0)
            delay(200)
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onShare) { Text("分享", fontSize = 12.sp) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                enabled = ready,
                onClick = {
                    val p = player ?: return@TextButton
                    runCatching {
                        if (playing) {
                            p.pause()
                            playing = false
                        } else if (ready) {
                            p.start()
                            playing = true
                        }
                    }
                }
            ) {
                Text(
                    if (playing) "⏸" else "▶",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = if (durMs > 0) (posMs.toFloat() / durMs).coerceIn(0f, 1f) else 0f,
                enabled = ready,
                onValueChange = { v ->
                    if (durMs > 0) {
                        posMs = (v * durMs).toInt()
                        runCatching { player?.seekTo(posMs) }
                    }
                },
                modifier = Modifier.weight(1f)
            )
            Text(
                "${fmt(posMs / 1000)}/${fmt(durMs / 1000)}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.width(80.dp)
            )
        }
        if (err != null) {
            Text(err!!, fontSize = 11.sp, color = LocalTkExtra.current.danger)
        }
    }
    Spacer(Modifier.height(6.dp))
}


/* ===================== 历史页 ===================== */

@Composable
fun HistoryScreen(vm: MainViewModel) {
    // 历史是二级页：返回主页面；若仍在听课，则回计时页以免丢失本节入口
    val goBack: () -> Unit = { if (vm.running) vm.backToClock() else vm.goStart() }
    BackHandler { goBack() }

    // 正在编辑听课信息的那一节（null = 未编辑）
    var editing by remember { mutableStateOf<SessionEntity?>(null) }
    // 待确认删除的那一节（null = 没有待删任务）
    var deleting by remember { mutableStateOf<HistoryItem?>(null) }

    Column(Modifier.fillMaxSize()) {
        TkTopBar(
            title = "历史记录",
            meta = "共 ${vm.history.size} 节"
        ) {
            TkBackButton { goBack() }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (vm.history.isEmpty()) {
                Text(
                    "还没有历史听课记录。结束一节听课后会自动存档在这里。",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 48.dp)
                )
            }
            vm.history.forEach { item ->
                val s = item.session
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp, MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { vm.viewSession(s.id) }
                        .padding(14.dp)
                ) {
                    Text(
                        s.title.ifBlank { "未命名课题" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        listOf(
                            listOf(s.stage, s.subject).filter { it.isNotBlank() }.joinToString(""),
                            s.cls,
                            joinedTeacher(s.teacher, s.teacherOrg),
                            s.date,
                            s.period
                        )
                            .filter { it.isNotBlank() }.joinToString(" · ")
                            .ifBlank { "—" },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "时长 ${fmt(s.elapsedSec)}　·　打标 ${item.markCount} 条" +
                            (if (s.hasAudio) "　·　含录音" else "") +
                            (if (s.endedAt == null) "　·　进行中" else ""),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { vm.viewSession(s.id) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 8.dp, vertical = 2.dp
                            )
                        ) {
                            Text("查看报告", fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = { editing = s },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 8.dp, vertical = 2.dp
                            )
                        ) {
                            Text("编辑信息", fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = { deleting = item },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 8.dp, vertical = 2.dp
                            )
                        ) {
                            Text(
                                "删除",
                                fontSize = 12.sp,
                                color = LocalTkExtra.current.danger
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            stampOf(s.startedAt),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            if (vm.running) {
                TkBarButton2("返回计时", Modifier.weight(1f), false) { vm.backToClock() }
            }
            TkBarButton2("开始新的一节", Modifier.weight(1f), false) { vm.newSession() }
        }
    }

    // 历史记录里补录/改正听课信息：只改信息字段，打标与录音分段原样保留
    editing?.let { s ->
        InfoEditorDialog(
            initial = s.toInfo(),
            onDismiss = { editing = null },
            onSave = { ni ->
                vm.updateSessionInfo(s.id, ni)
                editing = null
            }
        )
    }

    // 删除前必须二次确认（原先点了就删，误触即丢整节记录）
    deleting?.let { item ->
        val s = item.session
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除这条听课记录？") },
            text = {
                Text(
                    "「${s.title.ifBlank { "未命名课题" }}」${s.date}　" +
                        "时长 ${fmt(s.elapsedSec)}　打标 ${item.markCount} 条" +
                        (if (s.hasAudio) "　含录音" else "") +
                        "\n\n删除后，这条记录的听课信息、打标、录音文件会一起删除，" +
                        "不可恢复。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteSession(s.id)
                    deleting = null
                }) { Text("删除", color = LocalTkExtra.current.danger) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("取消") }
            }
        )
    }
}