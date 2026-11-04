package com.phygroup.tingke.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phygroup.tingke.audio.AudioRecorder
import com.phygroup.tingke.domain.Evals
import com.phygroup.tingke.domain.Labels
import com.phygroup.tingke.domain.Mark
import com.phygroup.tingke.domain.envMarks
import com.phygroup.tingke.domain.evalIcon
import com.phygroup.tingke.domain.evalName
import com.phygroup.tingke.domain.fmt
import com.phygroup.tingke.domain.isEvalOnly
import com.phygroup.tingke.domain.joinedTeacher
import com.phygroup.tingke.domain.segDur
import com.phygroup.tingke.domain.unmarkedDur

/* ===================== 根 ===================== */

@Composable
fun TingKeRoot(vm: MainViewModel) {
    TingKeTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(Modifier.fillMaxSize()) {
                when (vm.screen) {
                    Screen.Start -> StartScreen(vm)
                    Screen.Clock -> ClockScreen(vm)
                    Screen.Report -> ReportScreen(vm)
                    Screen.History -> HistoryScreen(vm)
                }
                TkToastHost(
                    vm,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 92.dp)
                )
            }
        }
    }
}

/* ===================== 开始页 ===================== */

@Composable
private fun StartScreen(vm: MainViewModel) {
    var showInfo by remember { mutableStateOf(false) }

    // 录音 + 通知（前台服务需要通知权限才能显示常驻提示）一次申请
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* 授权与否都继续开始，未授权则走模拟录音 */ vm.startSession() }

    Column(Modifier.fillMaxSize()) {
        TkTopBar(
            title = if (vm.info.title.isBlank()) "听课打标" else vm.info.title,
            meta = listOf(listOf(vm.info.stage, vm.info.subject).filter { it.isNotBlank() }.joinToString(""), vm.info.cls, vm.info.teacher, vm.info.date)
                .filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "准备开始" }
        ) {
            TkMiniButton("编辑") { showInfo = true }
            TkMiniButton("历史") { vm.openHistory() }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 信息卡
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                InfoLine("课题", vm.info.title)
                InfoLine(
                    "学段 / 科目",
                    listOf(vm.info.stage, vm.info.subject).filter { it.isNotBlank() }
                        .joinToString(" ").ifBlank { "—" }
                )
                InfoLine("授课教师", joinedTeacher(vm.info.teacher, vm.info.teacherOrg))
                InfoLine("班级", vm.info.cls)
                InfoLine("听课人", joinedTeacher(vm.info.observer, vm.info.observerOrg))
                InfoLine(
                    "日期 / 节次",
                    listOf(vm.info.date, vm.info.period).filter { it.isNotBlank() }
                        .joinToString(" ").ifBlank { "—" }
                )
            }

            // 录音默认勾选（可取消）
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                    .clickable { vm.updateRecEnabled(!vm.recEnabled) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = vm.recEnabled, onCheckedChange = { vm.updateRecEnabled(it) })
                Column(Modifier.weight(1f)) {
                    Text("同时录音（默认开启）", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (vm.recEnabled) "开始听课即同步录音，便于课后回听核对；不需要可取消勾选。"
                        else "已取消：本节只记录时间轴，不录音。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // 底部开始按钮
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            TkPrimary(
                text = "开始听课",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (!vm.recEnabled) {
                    vm.startSession()
                } else {
                    val perms = ArrayList<String>()
                    perms.add(android.Manifest.permission.RECORD_AUDIO)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        perms.add(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permLauncher.launch(perms.toTypedArray())
                }
            }
        }
    }

    if (showInfo) {
        InfoEditorDialog(
            initial = vm.info,
            onDismiss = { showInfo = false },
            onSave = {
                vm.saveInfo(it)
                showInfo = false
            }
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(
            value.ifBlank { "未填写" },
            fontSize = 13.sp,
            color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

/* ===================== 计时页 ===================== */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClockScreen(vm: MainViewModel) {
    var showAxis by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current

    // 打标编辑弹窗状态：id=null 表示新增
    var tagEdit by remember { mutableStateOf<TagEditState?>(null) }
    // 删除确认
    var delId by remember { mutableStateOf<String?>(null) }
    // 结束确认（结束不可逆，必须二次确认）
    var showEnd by remember { mutableStateOf(false) }

    // 计时中按返回：先问一句，不要直接退到桌面（后台还有前台服务在录音）
    BackHandler(enabled = vm.running) { showEnd = true }

    val elapsed = vm.elapsed
    // "当前环节"与"上一条"都只看课堂环节标记；独立评课标记不参与
    val env = vm.marks.filterNot { isEvalOnly(it.label) }
    val curLabel = env.lastOrNull()
    val prevLabel = env.dropLast(1).lastOrNull()

    Column(Modifier.fillMaxSize()) {
        TkTopBar(
            title = vm.info.title.ifBlank { if (vm.running) "听课进行中" else "听课记录" },
            meta = listOf(listOf(vm.info.stage, vm.info.subject).filter { it.isNotBlank() }.joinToString(""), vm.info.date, vm.info.period, vm.info.cls, vm.info.teacher)
                .filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "—" }
        ) {
            TkMiniButton("编辑") { showInfo = true }
            TkMiniButton("历史") { vm.openHistory() }
        }

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(10.dp))

            // 计时
            Text(
                text = fmt(elapsed),
                fontSize = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (vm.paused) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onBackground
            )

            // 状态文字：与运行态严格同步
            Text(
                text = when {
                    vm.running && vm.paused -> "已暂停"
                    vm.running -> "听课中…"
                    vm.marks.isNotEmpty() || elapsed > 0 -> "本节已结束"
                    else -> "待开始"
                },
                fontSize = 13.sp,
                color = when {
                    vm.running && vm.paused -> LocalTkExtra.current.warn
                    vm.running -> LocalTkExtra.current.ok
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(Modifier.height(6.dp))

            // 录音指示 / 警示
            if (vm.recActive && !vm.recSim) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(LocalTkExtra.current.danger)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "录音 " + fmt(totalRecSec(vm.segs, vm.recActive, vm.recSim, elapsed)),
                        color = LocalTkExtra.current.danger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else if (vm.recActive && vm.recSim) {
                WarnBar("⚠ 麦克风不可用，本节仅计时，不会生成音频文件")
            } else if (vm.running && !vm.recEnabled) {
                WarnBar("本节未开启录音（结束后仍可查看时间轴记录）")
            }

            Spacer(Modifier.height(10.dp))

            // 当前环节
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "当前环节",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    // 补充细节放在当前环节卡片里：改的就是"现在这个环节"
                    if (curLabel != null) {
                        TextButton(
                            onClick = {
                                tagEdit = TagEditState(curLabel.id, curLabel.ts, curLabel.label)
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 8.dp, vertical = 0.dp
                            )
                        ) { Text("补充细节", fontSize = 12.sp) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (curLabel != null) {
                        LabelPill(curLabel.label, 15)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            fmt((elapsed - curLabel.ts).coerceAtLeast(0)),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (curLabel.sub.isNotBlank()) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "·" + curLabel.sub,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text("未打标", color = Color.White, fontSize = 15.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            fmt(elapsed),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 上一条（当前环节之前的那条环节标记，可单独修改）
            if (prevLabel != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        buildString {
                            append("上一条：")
                            append(prevLabel.label)
                            if (prevLabel.sub.isNotBlank()) append("·").append(prevLabel.sub)
                            append(" ").append(fmt(prevLabel.ts))
                            val ic = evalIcon(prevLabel.eval)
                            if (ic.isNotBlank()) append(" ").append(ic)
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            tagEdit = TagEditState(prevLabel.id, prevLabel.ts, prevLabel.label)
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 8.dp, vertical = 0.dp
                        )
                    ) { Text("修改", fontSize = 12.sp) }
                }
            }

            // 评课标记（独立记录：只记时刻与内容，不占环节时长、不进环节占比）
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("评课标记", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "（不计入环节占比）",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    val n = vm.marks.count { isEvalOnly(it.label) }
                    if (n > 0) {
                        Text(
                            "已记 $n 条",
                            fontSize = 11.sp,
                            color = Labels.colorOf(Evals.ONLY_LABEL)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(Evals.HIGHLIGHT, Evals.WEAK, Evals.DOUBT).forEach { e ->
                        EvalQuickChip(
                            text = evalName(e),
                            modifier = Modifier.weight(1f),
                            onTap = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (vm.paused) vm.tell("计时已暂停，继续后再评课")
                                else vm.quickEval(e)
                            },
                            onLongPress = {
                                tagEdit = TagEditState(
                                    existingId = null,
                                    ts = vm.elapsed,
                                    presetLabel = Evals.ONLY_LABEL,
                                    evalOnly = true,
                                    presetEval = e
                                )
                            }
                        )
                    }
                }
                Text(
                    "轻点立即记录 · 长按可加备注 · 可在时间轴里改删",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            // 标签坞
            if (vm.running) {
                Text(
                    "轻点标签立即打标 · 长按可补充子标签/备注",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Dock(
                    vm = vm,
                    onTap = { label ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (vm.paused) vm.tell("计时已暂停，继续后再打标")
                        else vm.quickTag(label)
                    },
                    onLongPress = { label ->
                        tagEdit = TagEditState(null, vm.elapsed, label)
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        // 底部操作
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            TkBarButton(
                "时间轴 " + (if (vm.marks.isEmpty()) "" else vm.marks.size.toString()),
                Modifier.weight(1f)
            ) { showAxis = true }
            TkBarButton(if (vm.paused) "继续" else "暂停", Modifier.weight(1f)) { vm.togglePause() }
            TkBarButton("结束", Modifier.weight(1f), danger = true) {
                if (!vm.running && vm.marks.isEmpty()) {
                    vm.tell("还没有开始听课")
                } else {
                    showEnd = true
                }
            }
        }
    }

    /* ---------- 时间轴抽屉 ---------- */
    if (showAxis) {
        ModalBottomSheet(
            onDismissRequest = { showAxis = false },
            sheetState = sheetState
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    "时间轴（${vm.marks.size} 条）",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                if (vm.marks.isEmpty()) {
                    Text(
                        "还没有打标。关闭本页，在计时页轻点下方环节标签即可立即记录。",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                } else {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        // 时长只在环节标记之间切分；评课标记是点事件，不带时长
                        val envList = envMarks(vm.marks)
                        val durOf = envList
                            .mapIndexed { i, m -> m.id to segDur(envList, vm.elapsed, i) }
                            .toMap()
                        val um = unmarkedDur(envList, vm.elapsed)
                        if (um > 0) {
                            AxisRow("00:00", "未打标", "", "", fmt(um), null, null, vm)
                        }
                        vm.marks.forEach { m ->
                            val evalOnly = isEvalOnly(m.label)
                            AxisRow(
                                timeText = fmt(m.ts),
                                label = m.label,
                                sub = m.sub,
                                eval = m.eval,
                                note = m.note,
                                durText = if (evalOnly) "" else fmt(durOf[m.id] ?: 0),
                                evalOnly = evalOnly,
                                onEdit = { tagEdit = TagEditState(m.id, m.ts, m.label) },
                                onDelete = { delId = m.id },
                                vm = vm
                            )
                        }
                    }
                }
            }
        }
    }

    /* ---------- 打标弹窗 ---------- */
    tagEdit?.let { st ->
        TagSheet(
            vm = vm,
            state = st,
            onDismiss = { tagEdit = null }
        )
    }

    /* ---------- 删除确认 ---------- */
    delId?.let { id ->
        val hint = vm.deleteHint(id)
        if (hint == null) {
            delId = null
        } else {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { delId = null },
                title = { Text("删除打标") },
                text = { Text(hint) },
                confirmButton = {
                    TextButton(onClick = {
                        vm.deleteMark(id)
                        delId = null
                    }) { Text("删除", color = LocalTkExtra.current.danger) }
                },
                dismissButton = {
                    TextButton(onClick = { delId = null }) { Text("取消") }
                }
            )
        }
    }

    if (showInfo) {
        InfoEditorDialog(
            initial = vm.info,
            onDismiss = { showInfo = false },
            onSave = {
                vm.saveInfo(it)
                showInfo = false
            }
        )
    }

    if (showEnd) {
        val msg = if (vm.marks.isEmpty()) {
            "本节还没有任何打标。结束将丢弃本次计时，确认结束？"
        } else {
            "已记录 ${vm.marks.size} 个打标，总时长 ${fmt(vm.elapsed)}。" +
                "结束会保存记录并进入报告页，之后可在「历史」回看。"
        }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEnd = false },
            title = { Text("退出听课？") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = {
                    showEnd = false
                    vm.endSession()
                }) { Text("结束本节") }
            },
            dismissButton = {
                TextButton(onClick = { showEnd = false }) { Text("继续听课") }
            }
        )
    }
}

private fun totalRecSec(
    segs: List<AudioRecorder.Segment>,
    active: Boolean,
    sim: Boolean,
    elapsed: Int
): Int {
    var sum = 0
    segs.forEach { sum += (it.endSec - it.startSec).coerceAtLeast(0) }
    if (active && !sim && segs.isEmpty()) sum += elapsed
    return sum
}

@Composable
private fun WarnBar(text: String) {
    val e = LocalTkExtra.current
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(e.warnBg)
            .border(1.dp, e.warnBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 12.sp, color = e.warnFg, lineHeight = 17.sp)
    }
}

@Composable
private fun TkBarButton(
    text: String,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (danger) Color.Transparent else base)
            .then(
                if (danger) Modifier.border(
                    1.dp, LocalTkExtra.current.danger, RoundedCornerShape(12.dp)
                ) else Modifier
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

/* ===================== 标签坞 ===================== */

@Composable
private fun Dock(
    vm: MainViewModel,
    onTap: (String) -> Unit,
    onLongPress: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Labels.all.chunked(7).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { def ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                1.dp, MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(10.dp)
                            )
                            .pointerInput(def.key) {
                                detectTapGestures(
                                    onTap = { onTap(def.key) },
                                    onLongPress = { onLongPress(def.key) }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            def.short,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                // 补齐最后一行，保持 7 列对齐
                repeat(7 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/* ===================== 时间轴条目 ===================== */

@Composable
private fun AxisRow(
    timeText: String,
    label: String,
    sub: String,
    eval: String,
    durText: String,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    vm: MainViewModel,
    note: String = "",
    evalOnly: Boolean = false
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            timeText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (evalOnly) Labels.colorOf(Evals.ONLY_LABEL)
            else MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(56.dp)
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (label == "未打标") {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    ) { Text("未打标", color = Color.White, fontSize = 13.sp) }
                } else {
                    LabelPill(label)
                }
                if (sub.isNotBlank()) {
                    Spacer(Modifier.width(5.dp))
                    Text("·$sub", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (eval.isNotBlank()) {
                    Spacer(Modifier.width(5.dp))
                    Text(evalName(eval), fontSize = 12.sp, color = Labels.colorOf(Evals.ONLY_LABEL))
                }
            }
            if (note.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    note,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!evalOnly) {
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 1.dp)
                ) {
                    Text(
                        "时长 $durText",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        if (onEdit != null && onDelete != null) {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onEdit) { Text("改", fontSize = 12.sp) }
                TextButton(onClick = onDelete) {
                    Text("删", fontSize = 12.sp, color = LocalTkExtra.current.danger)
                }
            }
        }
    }
    androidx.compose.material3.Divider(
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 1.dp
    )
}

/* ===================== 打标弹窗 ===================== */

/** 子标签里的"自定义"占位项；sub 是自由文本字段，自填内容直接入库 */
private const val CUSTOM_SUB = "自定义…"

data class TagEditState(
    val existingId: String?,
    /** 打开瞬间冻结的打标时刻 —— 不随保存时间变化 */
    val ts: Int,
    val presetLabel: String?,
    /** true = 独立评课标记模式（不选环节、只选评课类别 + 备注） */
    val evalOnly: Boolean = false,
    /** 评课标记模式下预选的类别（长按某颗评课按钮时带入） */
    val presetEval: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagSheet(
    vm: MainViewModel,
    state: TagEditState,
    onDismiss: () -> Unit
) {
    val existing: Mark? = state.existingId?.let { id -> vm.marks.firstOrNull { it.id == id } }

    // 独立评课标记模式：从评课卡片长按进来，或修改的本来就一条评课标记
    val onlyEval = state.evalOnly || (existing != null && isEvalOnly(existing.label))

    var label by remember {
        mutableStateOf(
            if (onlyEval) Evals.ONLY_LABEL
            else existing?.label ?: state.presetLabel ?: ""
        )
    }
    var sub by remember { mutableStateOf(if (onlyEval) "" else existing?.sub ?: "") }
    var eval by remember {
        mutableStateOf(existing?.eval?.ifBlank { state.presetEval } ?: state.presetEval)
    }
    var note by remember { mutableStateOf(existing?.note ?: "") }

    // 子标签支持自填：existing 的子标签若不在预设表里，视为自填，直接进"自定义"态并回填
    val initSub = existing?.sub.orEmpty()
    val initSubIsCustom =
        initSub.isNotBlank() && initSub !in Labels.subsOf(existing?.label.orEmpty())
    var customOn by remember { mutableStateOf(initSubIsCustom) }
    var customSub by remember { mutableStateOf(if (initSubIsCustom) initSub else "") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                when {
                    onlyEval && existing != null -> "修改评课标记"
                    onlyEval -> "评课标记"
                    existing != null -> "补充 / 修改这条打标"
                    else -> "打标"
                },
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (onlyEval) {
                    "记录时刻：${fmt(state.ts)}　（只记时刻与内容，不计入环节占比）"
                } else {
                    "打标时刻：${fmt(state.ts)}" +
                        if (existing == null) "（已冻结，不会随保存时间变化）" else ""
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
            )

            if (!onlyEval) {
                SectionLabel("① 环节（必选）")
                Labels.all.chunked(3).forEach { row ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { def ->
                            SelectableChip(
                                text = def.key,
                                selected = label == def.key,
                                modifier = Modifier.weight(1f)
                            ) {
                                label = def.key
                                sub = ""
                                customOn = false
                                customSub = ""
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }

                /*
                 * ② 子标签：预设子标签 + 一个「自定义…」。
                 * 选「自定义…」后出现输入框，自填内容直接存进 mark.sub（sub 本就是自由文本字段，
                 * 报告/CSV/文本时间轴都按字符串输出，无需改数据结构）。
                 */
                val subs = Labels.subsOf(label)
                SectionLabel("② 子标签（可选，可自填）")
                (subs + CUSTOM_SUB).chunked(3).forEach { row ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { s ->
                            if (s == CUSTOM_SUB) {
                                SelectableChip(
                                    text = CUSTOM_SUB,
                                    selected = customOn,
                                    small = true,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (customOn) {
                                        customOn = false
                                        customSub = ""
                                        sub = ""
                                    } else {
                                        customOn = true
                                        sub = customSub
                                    }
                                }
                            } else {
                                SelectableChip(
                                    text = s,
                                    selected = !customOn && sub == s,
                                    small = true,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (!customOn && sub == s) {
                                        sub = ""
                                    } else {
                                        customOn = false
                                        customSub = ""
                                        sub = s
                                    }
                                }
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                if (customOn) {
                    OutlinedTextField(
                        value = customSub,
                        onValueChange = {
                            customSub = it
                            sub = it
                        },
                        label = { Text("自填子标签", fontSize = 13.sp) },
                        placeholder = {
                            Text("例如：水槽演示折射、学生上台画受力图", fontSize = 13.sp)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    )
                }
            }

            if (onlyEval) {
                SectionLabel("评课类别（必选）")
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(Evals.HIGHLIGHT, Evals.WEAK, Evals.DOUBT).forEach { e ->
                        SelectableChip(
                            text = evalName(e),
                            selected = eval == e,
                            small = true,
                            modifier = Modifier.weight(1f)
                        ) { eval = if (eval == e) "" else e }
                    }
                }
            } else {
                SectionLabel("③ 评课标记（可选）")
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Evals.all.forEach { e ->
                        SelectableChip(
                            text = evalName(e),
                            selected = eval == e,
                            small = true,
                            modifier = Modifier.weight(1f)
                        ) { eval = e }
                    }
                }
            }

            SectionLabel(if (onlyEval) "备注（可选，写清具体在哪）" else "④ 备注（可选）")
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("例如：激光笔+水槽演示折射，直观", fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
                textStyle = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(12.dp))
            TkPrimary(
                text = "保存",
                enabled = if (onlyEval) eval.isNotBlank() else label.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (onlyEval) {
                    vm.commitEval(state.existingId, state.ts, eval, note)
                } else {
                    vm.commitMark(state.existingId, state.ts, label, sub, eval, note)
                }
                onDismiss()
            }
        }
    }
}

/** 评课卡片上的快捷按钮：轻点立即记录，长按打开带备注的弹窗 */
@Composable
private fun EvalQuickChip(
    text: String,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val tint = Labels.colorOf(Evals.ONLY_LABEL)
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            .pointerInput(text) {
                detectTapGestures(onTap = { onTap() }, onLongPress = { onLongPress() })
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            maxLines = 1
        )
    }
}

@Composable
private fun SelectableChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    small: Boolean = false,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .height(if (small) 40.dp else 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .border(
                1.dp,
                if (selected) primary else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = if (small) 12.sp else 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
