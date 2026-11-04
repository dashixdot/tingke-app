package com.phygroup.tingke.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phygroup.tingke.domain.Info
import com.phygroup.tingke.domain.Labels
import com.phygroup.tingke.domain.Meta
import kotlinx.coroutines.delay

/* ===================== 顶栏 ===================== */

@Composable
fun TkTopBar(
    title: String,
    meta: String,
    actions: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { actions() }
    }
}

@Composable
fun TkMiniButton(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 10.dp, vertical = 2.dp
        )
    ) {
        Text(text, color = Color.White, fontSize = 12.sp)
    }
}

/** 顶栏返回箭头（历史 / 报告页用，避免用户只能靠系统返回键乱猜） */
@Composable
fun TkBackButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 8.dp, vertical = 2.dp
        )
    ) {
        Text("‹ 返回", color = Color.White, fontSize = 13.sp)
    }
}

/* ===================== 按钮 ===================== */

@Composable
fun TkPrimary(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        )
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

/* ===================== 小节标签 ===================== */

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = LocalTkExtra.current.sub,
        modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
    )
}

/* ===================== 标签块（彩色胶囊） ===================== */

@Composable
fun LabelPill(label: String, fontSize: Int = 13) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Labels.colorOf(label))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(label, color = Color.White, fontSize = fontSize.sp, fontWeight = FontWeight.SemiBold)
    }
}

/* ===================== 比例条 ===================== */

@Composable
fun RatioBar(fraction: Float, color: Color, height: Int = 14) {
    val track = LocalTkExtra.current.track
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(height.dp / 2))
            .background(track)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height.dp)
                .clip(RoundedCornerShape(height.dp / 2))
                .background(color)
        )
    }
}

/* ===================== Toast（带撤销） ===================== */

@Composable
fun TkToastHost(vm: MainViewModel, modifier: Modifier = Modifier) {
    val t = vm.toast ?: return
    LaunchedEffect(t.id) {
        delay(if (t.actionLabel != null) 5000 else 1900)
        if (vm.toast?.id == t.id) vm.clearToast()
    }
    // 顶部展示：底部是标签坞与操作栏，放在下面一定挡操作
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xEE111827))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(t.text, color = Color.White, fontSize = 13.sp)
            if (t.actionLabel != null) {
                Spacer(Modifier.width(10.dp))
                TextButton(
                    onClick = {
                        vm.clearToast()
                        t.action?.invoke()
                    },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 8.dp, vertical = 2.dp
                    )
                ) {
                    Text(t.actionLabel, color = Color(0xFF93C5FD), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/* ===================== 听课信息编辑 ===================== */

/** 单选小胶囊（学段/科目用，横向可滚动，节省纵向空间） */
@Composable
private fun ChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .border(
                1.dp,
                if (selected) primary else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(9.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
fun InfoEditorDialog(initial: Info, onDismiss: () -> Unit, onSave: (Info) -> Unit) {
    var title by remember { mutableStateOf(initial.title) }
    var teacher by remember { mutableStateOf(initial.teacher) }
    var cls by remember { mutableStateOf(initial.cls) }
    var observer by remember { mutableStateOf(initial.observer) }
    var date by remember { mutableStateOf(initial.date) }
    var period by remember { mutableStateOf(initial.period) }
    var count by remember { mutableStateOf(initial.count) }
    var place by remember { mutableStateOf(initial.place) }
    var teacherOrg by remember { mutableStateOf(initial.teacherOrg) }
    var observerOrg by remember { mutableStateOf(initial.observerOrg) }
    var stage by remember { mutableStateOf(initial.stage) }
    var subject by remember { mutableStateOf(initial.subject) }

    // 学段/科目选了"自定义"时，用一个自由输入框
    var customStage by remember {
        mutableStateOf(initial.stage.isNotBlank() && initial.stage !in Meta.stages)
    }
    var customSubject by remember {
        mutableStateOf(initial.subject.isNotBlank() && initial.subject !in Meta.subjects)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("听课信息") },
        text = {
            // 字段多，竖屏窄屏下给个高度上限并可滚动
            Column(
                Modifier
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("课题") }, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )

                /* --- 学段 --- */
                Text(
                    "学段",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Meta.stages.forEach { s ->
                        ChoiceChip(s, !customStage && stage == s) {
                            customStage = false
                            stage = s
                        }
                    }
                    ChoiceChip(Meta.CUSTOM, customStage) {
                        customStage = true
                        if (stage in Meta.stages) stage = ""
                    }
                }
                if (customStage) {
                    OutlinedTextField(
                        value = stage, onValueChange = { stage = it },
                        label = { Text("自定义学段") }, singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    )
                }

                /* --- 科目 --- */
                Text(
                    "科目",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Meta.subjects.forEach { s ->
                        ChoiceChip(s, !customSubject && subject == s) {
                            customSubject = false
                            subject = s
                        }
                    }
                    ChoiceChip(Meta.CUSTOM, customSubject) {
                        customSubject = true
                        if (subject in Meta.subjects) subject = ""
                    }
                }
                if (customSubject) {
                    OutlinedTextField(
                        value = subject, onValueChange = { subject = it },
                        label = { Text("自定义科目") }, singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = teacher, onValueChange = { teacher = it },
                    label = { Text("授课教师") }, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = teacherOrg, onValueChange = { teacherOrg = it },
                    label = { Text("授课教师单位") }, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cls, onValueChange = { cls = it },
                    label = { Text("班级") }, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = observer, onValueChange = { observer = it },
                    label = { Text("听课人") }, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = observerOrg, onValueChange = { observerOrg = it },
                    label = { Text("听课人单位") }, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = date, onValueChange = { date = it },
                    label = { Text("日期") }, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = period, onValueChange = { period = it },
                    label = { Text("节次") }, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = count, onValueChange = { count = it },
                    label = { Text("班级人数") }, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = place, onValueChange = { place = it },
                    label = { Text("听课地点") }, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    Info(
                        title = title.trim(), teacher = teacher.trim(), cls = cls.trim(),
                        observer = observer.trim(),
                        date = date.trim().ifBlank { initial.date },
                        period = period.trim(), count = count.trim(), place = place.trim(),
                        teacherOrg = teacherOrg.trim().ifBlank { Meta.DEFAULT_ORG },
                        observerOrg = observerOrg.trim().ifBlank { Meta.DEFAULT_ORG },
                        stage = stage.trim().ifBlank { Meta.DEFAULT_STAGE },
                        subject = subject.trim().ifBlank { Meta.DEFAULT_SUBJECT }
                    )
                )
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
