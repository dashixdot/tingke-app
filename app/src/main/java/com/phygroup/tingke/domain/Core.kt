package com.phygroup.tingke.domain

import androidx.compose.ui.graphics.Color
import java.util.Calendar

/* ===================== 标签体系 =====================
 * 依据：晋江市中小学课堂教学评价量表（导入/探究/巩固/小结/作业）
 *      + 物理组《"一节好课"标准讨论》（实验真做、当堂检测、板书板图）
 * 短名用于底部标签坞（2×7 排布，窄屏也能放得下）
 */
data class LabelDef(
    val key: String,
    val short: String,
    val subs: List<String>
)

object Labels {

    val all: List<LabelDef> = listOf(
        LabelDef("导入", "导入", listOf("情境引入", "复习引入", "实验引入")),
        LabelDef("目标呈现", "目标", listOf("行为目标", "核心素养目标")),
        LabelDef("新授讲解", "讲解", listOf("概念建构", "规律推导", "公式教学")),
        LabelDef("实验演示", "演示", listOf("教师演示", "现象展示")),
        LabelDef("学生实验", "生实", listOf("分组探究", "动手操作", "器材整理")),
        LabelDef("探究活动", "探究", listOf("提出问题", "猜想假设", "方案设计", "实验验证", "分析论证", "成果展示")),
        LabelDef("提问互动", "互动", listOf("追问", "学生汇报", "师生对话", "小组交流")),
        LabelDef("当堂检测", "检测", listOf("3–5分钟检测", "预测单", "随堂练习")),
        LabelDef("巩固练习", "巩固", listOf("例题讲评", "习题训练", "变式拓展")),
        LabelDef("小结", "小结", listOf("学生小结", "教师小结", "知识结构")),
        LabelDef("作业布置", "作业", listOf("分层作业", "挑战任务")),
        LabelDef("板书板图", "板书", listOf("板书画图", "思维导图", "知识网络")),
        LabelDef("多媒体与信息技术", "技术", listOf("AI工具", "模拟动画", "投屏展示")),
        LabelDef("拓展延伸", "拓展", listOf("生活应用", "学科前沿", "价值引领"))
    )

    private val palette = listOf(
        0xFF2563EB, 0xFF0891B2, 0xFF7C3AED, 0xFFDB2777, 0xFFDC2626, 0xFFEA580C,
        0xFFD97706, 0xFF16A34A, 0xFF15803D, 0xFF0D9488, 0xFF4F46E5, 0xFF9333EA,
        0xFFC026D3, 0xFFE11D48
    )

    fun colorOf(key: String): Color {
        // 独立评课标记固定用琥珀色，与环节配色区分开
        if (isEvalOnly(key)) return Color(0xFFD97706)
        val i = all.indexOfFirst { it.key == key }
        return Color(palette[(if (i < 0) 0 else i) % palette.size])
    }

    fun subsOf(key: String): List<String> =
        all.firstOrNull { it.key == key }?.subs ?: emptyList()

    fun shortOf(key: String): String =
        all.firstOrNull { it.key == key }?.short ?: if (isEvalOnly(key)) "评课" else key
}

/** 评课标记 */
object Evals {
    const val NONE = ""
    const val HIGHLIGHT = "亮点"
    const val WEAK = "不足"
    const val DOUBT = "疑问"
    val all = listOf(NONE, HIGHLIGHT, WEAK, DOUBT)

    /**
     * 独立评课标记使用的保留环节名。
     * 它只是时间轴上的一个"点事件"，不属于任何课堂环节，
     * 因此**不参与环节时长与占比统计**（见 [envMarks] / [shareData]）。
     */
    const val ONLY_LABEL = "评课标记"
}

/** 该条打标是否为"只记评课、不占环节时长"的独立记录 */
fun isEvalOnly(label: String): Boolean = label == Evals.ONLY_LABEL

/** 参与环节时长/占比统计的标记（排除独立评课标记） */
fun envMarks(marks: List<Mark>): List<Mark> = marks.filterNot { isEvalOnly(it.label) }

fun evalIcon(e: String?): String = when (e) {
    Evals.HIGHLIGHT -> "⭐"
    Evals.WEAK -> "⚠️"
    Evals.DOUBT -> "❓"
    else -> ""
}

fun evalName(e: String?): String = when (e) {
    Evals.HIGHLIGHT -> "⭐ 亮点"
    Evals.WEAK -> "⚠️ 不足"
    Evals.DOUBT -> "❓ 疑问"
    else -> "无"
}

/* ===================== 打标模型 ===================== */

data class Mark(
    val id: String,
    val ts: Int,
    val label: String,
    val sub: String = "",
    val eval: String = "",
    val note: String = ""
)

/** 听课信息里的固定选项（学段 / 科目 / 默认单位） */
object Meta {
    /** 学校全称，作为授课教师单位与听课人单位的默认值 */
    const val DEFAULT_ORG = "晋江市陈埭民族中学"

    /** 学段（可另选"自定义"自由填写） */
    val stages = listOf("初中", "高中")

    /** 中学科目（可另选"自定义"自由填写） */
    val subjects = listOf(
        "语文", "数学", "英语", "物理", "化学", "生物",
        "道德与法治", "历史", "地理", "科学",
        "体育与健康", "音乐", "美术", "信息技术", "通用技术"
    )

    const val CUSTOM = "自定义"

    const val DEFAULT_STAGE = "初中"
    const val DEFAULT_SUBJECT = "物理"
}

data class Info(
    val title: String = "",
    val teacher: String = "",
    val cls: String = "",
    val observer: String = "",
    val date: String = todayStr(),
    val period: String = "",
    val count: String = "",
    val place: String = "",
    /** 授课教师所在单位 */
    val teacherOrg: String = Meta.DEFAULT_ORG,
    /** 听课人所在单位 */
    val observerOrg: String = Meta.DEFAULT_ORG,
    /** 学段：初中 / 高中 / 自定义文本 */
    val stage: String = Meta.DEFAULT_STAGE,
    /** 科目：中学科目 / 自定义文本 */
    val subject: String = Meta.DEFAULT_SUBJECT
)

/* ===================== 时间与格式化 ===================== */

fun fmt(sec: Int): String {
    val s = if (sec < 0) 0 else sec
    val h = s / 3600
    val m = (s % 3600) / 60
    val ss = s % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, ss)
    else String.format("%02d:%02d", m, ss)
}

/** 本地日期（不要用 toISOString，那是 UTC，东八区 0–8 点会显示前一天） */
fun todayStr(): String {
    val c = Calendar.getInstance()
    return String.format(
        "%04d-%02d-%02d",
        c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)
    )
}

fun stampOf(millis: Long): String {
    val c = Calendar.getInstance()
    c.timeInMillis = millis
    return String.format(
        "%04d-%02d-%02d %02d:%02d",
        c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
        c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)
    )
}

/* ===================== 统计口径 ===================== */

/** 第 i 段时长 = 下一个标的时间 − 本标时间；末段 = 结束时刻 − 末标时间 */
fun segDur(marks: List<Mark>, elapsed: Int, i: Int): Int {
    if (i < 0 || i >= marks.size) return 0
    val a = marks[i]
    val b = marks.getOrNull(i + 1)
    return ((b?.ts ?: elapsed) - a.ts).coerceAtLeast(0)
}

/** 未打标段 = 0 → 首个打标；无打标时 = 全程 */
fun unmarkedDur(marks: List<Mark>, elapsed: Int): Int =
    if (marks.isEmpty()) elapsed.coerceAtLeast(0) else marks.first().ts.coerceAtLeast(0)

data class ShareRow(val label: String, val dur: Int, val pct: Double)

data class ShareData(
    val total: Int,
    val rows: List<ShareRow>,
    val unmarked: Int,
    val unmarkedPct: Double
)

/**
 * 环节占比：只在**课堂环节标记**之间切分时长。
 * 独立评课标记（[Evals.ONLY_LABEL]）是点事件，不占时长、不生成行、不影响"未打标"计算。
 */
fun shareData(marks: List<Mark>, elapsed: Int): ShareData {
    val total = elapsed.coerceAtLeast(0)
    val env = envMarks(marks)
    val acc = LinkedHashMap<String, Int>()
    for (i in env.indices) {
        val m = env[i]
        acc[m.label] = (acc[m.label] ?: 0) + segDur(env, elapsed, i)
    }
    val rows = acc.entries
        .map { ShareRow(it.key, it.value, if (total > 0) it.value * 100.0 / total else 0.0) }
        .sortedByDescending { it.dur }
    val um = unmarkedDur(env, elapsed)
    return ShareData(
        total = total,
        rows = rows,
        unmarked = um,
        unmarkedPct = if (total > 0) um * 100.0 / total else 0.0
    )
}

/* ===================== 文本 / CSV ===================== */

data class ReportHeader(
    val title: String,
    val cls: String,
    val teacher: String,
    val date: String,
    val period: String,
    /** 学段（初中/高中/自定义） */
    val stage: String = "",
    /** 科目（物理/化学/…/自定义） */
    val subject: String = "",
    /** 授课教师单位 */
    val teacherOrg: String = "",
    /** 听课人 */
    val observer: String = "",
    /** 听课人单位 */
    val observerOrg: String = ""
)

/** 听课信息 → 报告表头（导出/展示共用一套口径） */
fun Info.asHeader(): ReportHeader = ReportHeader(
    title = title,
    cls = cls,
    teacher = teacher,
    date = date,
    period = period,
    stage = stage,
    subject = subject,
    teacherOrg = teacherOrg,
    observer = observer,
    observerOrg = observerOrg
)

/** 授课教师 + 单位（单位为空时只显示姓名） */
fun joinedTeacher(teacher: String, org: String): String = when {
    teacher.isBlank() -> org
    org.isBlank() -> teacher
    else -> "$teacher（$org）"
}

fun reportText(h: ReportHeader, marks: List<Mark>, elapsed: Int): String {
    val sb = StringBuilder()
    // 第一行：课题 + 学段科目 + 时长
    sb.append("【").append(h.title.ifBlank { "未命名课题" }).append("】")
    val meta = listOf(h.stage, h.subject).filter { it.isNotBlank() }.joinToString("")
    if (meta.isNotBlank()) sb.append(meta)
    sb.append("  时长 ").append(fmt(elapsed))
    // 第二行：班级 / 授课教师（单位）/ 日期 / 节次
    val line2 = listOf(
        h.cls,
        joinedTeacher(h.teacher, h.teacherOrg).let { if (it.isBlank()) "" else "授课 $it" },
        h.date,
        h.period
    ).filter { it.isNotBlank() }
    if (line2.isNotEmpty()) sb.append("\n").append(line2.joinToString("　"))
    // 第三行：听课人（单位）
    if (h.observer.isNotBlank() || h.observerOrg.isNotBlank()) {
        sb.append("\n听课人 ").append(joinedTeacher(h.observer, h.observerOrg))
    }
    if (marks.isEmpty()) {
        sb.append("\n（暂无打标记录）")
        return sb.toString()
    }
    val env = envMarks(marks)
    val um = unmarkedDur(env, elapsed)
    if (um > 0) sb.append("\n00:00 未打标  〔时长 ").append(fmt(um)).append("〕")
    // 时长按"环节序列"计算，评课标记只显示时刻与内容
    val durOf = HashMap<String, Int>()
    env.forEachIndexed { i, m -> durOf[m.id] = segDur(env, elapsed, i) }
    marks.forEach { m ->
        sb.append("\n").append(fmt(m.ts)).append(" ").append(m.label)
        if (m.sub.isNotBlank()) sb.append("·").append(m.sub)
        val ic = evalIcon(m.eval)
        if (ic.isNotEmpty()) sb.append(" ").append(ic)
        if (isEvalOnly(m.label)) {
            sb.append("  〔不计入环节占比〕")
        } else {
            sb.append("  〔时长 ").append(fmt(durOf[m.id] ?: 0)).append("〕")
        }
        if (m.note.isNotBlank()) sb.append("  ").append(m.note)
    }
    val data = shareData(marks, elapsed)
    sb.append("\n\n—— 环节占比 ——")
    data.rows.forEach { r ->
        sb.append("\n").append(r.label).append("  ").append(fmt(r.dur))
            .append("  ").append(String.format("%.1f%%", r.pct))
    }
    if (data.unmarked > 0) {
        sb.append("\n未打标  ").append(fmt(data.unmarked))
            .append("  ").append(String.format("%.1f%%", data.unmarkedPct))
    }
    sb.append("\n合计  ").append(fmt(data.total)).append("  100.0%")

    val evals = marks.filter { isEvalOnly(it.label) && it.eval.isNotBlank() }
    if (evals.isNotEmpty()) {
        sb.append("\n\n—— 评课标记（").append(evals.size).append(" 条，不计入环节占比）——")
        evals.forEach { m ->
            sb.append("\n").append(fmt(m.ts)).append(" ").append(evalName(m.eval))
            if (m.note.isNotBlank()) sb.append("  ").append(m.note)
        }
    }
    return sb.toString()
}

private fun csvCell(v: String?): String = "\"" + (v ?: "").replace("\"", "\"\"") + "\""

/** CSV 带 UTF-8 BOM，Excel 直接打开不乱码。末尾两列带学段/科目，便于跨学期汇总 */
fun reportCsv(h: ReportHeader, marks: List<Mark>, elapsed: Int): String {
    val sb = StringBuilder()
    sb.append("\uFEFF")
    sb.append("序号,时间(秒),时间,环节,子标签,评课标记,环节时长(秒),备注,学段,科目\r\n")
    val env = envMarks(marks)
    val um = unmarkedDur(env, elapsed)
    if (um > 0) {
        sb.append(
            listOf(
                "0", "0", "00:00", "未打标", "", "", um.toString(), "开始至首个打标",
                h.stage, h.subject
            ).joinToString(",", transform = ::csvCell)
        ).append("\r\n")
    }
    val durOf = HashMap<String, Int>()
    env.forEachIndexed { i, m -> durOf[m.id] = segDur(env, elapsed, i) }
    marks.forEachIndexed { i, m ->
        val evalOnly = isEvalOnly(m.label)
        sb.append(
            listOf(
                (i + 1).toString(), m.ts.toString(), fmt(m.ts), m.label,
                m.sub, m.eval,
                // 独立评课标记不占环节时长，留空表示"不参与统计"
                if (evalOnly) "" else (durOf[m.id] ?: 0).toString(),
                m.note, h.stage, h.subject
            ).joinToString(",", transform = ::csvCell)
        ).append("\r\n")
    }
    return sb.toString()
}
