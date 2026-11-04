package com.phygroup.tingke.data

import com.phygroup.tingke.domain.Info
import com.phygroup.tingke.domain.Mark
import com.phygroup.tingke.domain.ReportHeader
import kotlinx.coroutines.flow.Flow

/** 领域模型 ←→ 实体 的转换集中在此，避免散落 */
fun Mark.toEntity(sessionId: String): MarkEntity =
    MarkEntity(id = id, sessionId = sessionId, ts = ts, label = label, sub = sub, eval = eval, note = note)

fun MarkEntity.toDomain(): Mark =
    Mark(id = id, ts = ts, label = label, sub = sub, eval = eval, note = note)

/** 某节存档的听课信息 → 可编辑的表单模型（历史记录里"编辑信息"用） */
fun SessionEntity.toInfo(): Info = Info(
    title = title,
    teacher = teacher,
    cls = cls,
    observer = observer,
    date = date,
    period = period,
    count = count,
    place = place,
    teacherOrg = teacherOrg,
    observerOrg = observerOrg,
    stage = stage,
    subject = subject
)

/** 某节存档 → 报告表头（导出、分享、报告页共用同一套口径） */
fun SessionEntity.toHeader(): ReportHeader = ReportHeader(
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

/** 表单模型 → 写回存档。空日期保留原值，避免用户清空后丢日期。 */
fun SessionEntity.withInfo(i: Info): SessionEntity = copy(
    title = i.title,
    teacher = i.teacher,
    cls = i.cls,
    observer = i.observer,
    date = i.date.ifBlank { date },
    period = i.period,
    count = i.count,
    place = i.place,
    teacherOrg = i.teacherOrg,
    observerOrg = i.observerOrg,
    stage = i.stage.ifBlank { stage },
    subject = i.subject.ifBlank { subject }
)

class TingKeRepository(private val db: AppDatabase) {

    /* 听课记录 */
    fun observeSessions(): Flow<List<SessionEntity>> = db.sessionDao().observeAll()

    /** 一次性读取（历史页用），避免额外起 Flow 收集 */
    suspend fun sessionsOnce(): List<SessionEntity> = db.sessionDao().listOnce()

    suspend fun findRunning(): SessionEntity? = db.sessionDao().findRunning()
    suspend fun session(id: String): SessionEntity? = db.sessionDao().get(id)
    suspend fun saveSession(s: SessionEntity) = db.sessionDao().upsert(s)
    suspend fun deleteSession(id: String) = db.sessionDao().delete(id)

    /* 打标 */
    fun observeMarks(sessionId: String): Flow<List<MarkEntity>> =
        db.markDao().observe(sessionId)

    suspend fun marks(sessionId: String): List<Mark> =
        db.markDao().list(sessionId).map { it.toDomain() }

    suspend fun saveMark(sessionId: String, mark: Mark) =
        db.markDao().upsert(mark.toEntity(sessionId))

    suspend fun deleteMark(id: String) = db.markDao().delete(id)

    /* 录音分段 */
    suspend fun saveAudio(sessionId: String, segs: List<AudioSegmentEntity>) =
        db.audioDao().insertAll(segs)

    suspend fun audio(sessionId: String): List<AudioSegmentEntity> =
        db.audioDao().list(sessionId)
}
