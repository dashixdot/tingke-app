package com.phygroup.tingke.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.phygroup.tingke.domain.Meta
import kotlinx.coroutines.flow.Flow

/* ===================== 实体 ===================== */

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val teacher: String,
    val cls: String,
    val observer: String,
    val date: String,
    val period: String,
    val count: String,
    val place: String,
    val startedAt: Long,
    /** null 表示仍在进行中 */
    val endedAt: Long?,
    val elapsedSec: Int,
    val hasAudio: Boolean,
    /** 授课教师单位 */
    val teacherOrg: String = Meta.DEFAULT_ORG,
    /** 听课人单位 */
    val observerOrg: String = Meta.DEFAULT_ORG,
    /** 学段：初中 / 高中 / 自定义 */
    val stage: String = Meta.DEFAULT_STAGE,
    /** 科目：中学科目 / 自定义 */
    val subject: String = Meta.DEFAULT_SUBJECT
)

@Entity(
    tableName = "marks",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class MarkEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val ts: Int,
    val label: String,
    val sub: String,
    val eval: String,
    val note: String
)

@Entity(
    tableName = "audio_segments",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class AudioSegmentEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val sessionId: String,
    val segIndex: Int,
    val path: String,
    val startSec: Int,
    val endSec: Int,
    val mime: String
)

/* ===================== DAO ===================== */

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    suspend fun listOnce(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun findRunning(): SessionEntity?

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun get(id: String): SessionEntity?

    /**
     * 必须用 @Upsert 而不是 @Insert(REPLACE)。
     * SQLite 的 INSERT OR REPLACE 是「先 DELETE 再 INSERT」，会触发 marks 表的
     * ON DELETE CASCADE，把这一节的打标全部删掉——这正是"回到历史记录打标全丢"的根因。
     */
    @Upsert
    suspend fun upsert(s: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: String)
}

data class SessionMarkCount(val sessionId: String, val c: Int)

@Dao
interface MarkDao {
    @Query("SELECT * FROM marks WHERE sessionId = :sid ORDER BY ts ASC")
    fun observe(sid: String): Flow<List<MarkEntity>>

    @Query("SELECT * FROM marks WHERE sessionId = :sid ORDER BY ts ASC")
    suspend fun list(sid: String): List<MarkEntity>

    @Query("SELECT sessionId, COUNT(*) AS c FROM marks GROUP BY sessionId")
    suspend fun counts(): List<SessionMarkCount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(m: MarkEntity)

    @Query("DELETE FROM marks WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface AudioDao {
    @Query("SELECT * FROM audio_segments WHERE sessionId = :sid ORDER BY segIndex ASC")
    suspend fun list(sid: String): List<AudioSegmentEntity>

    @Query("SELECT * FROM audio_segments WHERE sessionId = :sid ORDER BY segIndex ASC")
    fun observe(sid: String): Flow<List<AudioSegmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AudioSegmentEntity>)
}

/* ===================== 数据库 ===================== */

@Database(
    entities = [SessionEntity::class, MarkEntity::class, AudioSegmentEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun markDao(): MarkDao
    abstract fun audioDao(): AudioDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * v1 → v2：听课信息增加「授课教师单位 / 听课人单位 / 学段 / 科目」。
         * 必须写迁移而不是靠 fallbackToDestructiveMigration，否则用户已有的历史听课记录会被清空。
         * 新增列全部 NOT NULL + DEFAULT，老记录自动补上学校全称 / 初中 / 物理。
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `sessions` ADD COLUMN `teacherOrg` TEXT NOT NULL " +
                        "DEFAULT '${Meta.DEFAULT_ORG}'"
                )
                db.execSQL(
                    "ALTER TABLE `sessions` ADD COLUMN `observerOrg` TEXT NOT NULL " +
                        "DEFAULT '${Meta.DEFAULT_ORG}'"
                )
                db.execSQL(
                    "ALTER TABLE `sessions` ADD COLUMN `stage` TEXT NOT NULL " +
                        "DEFAULT '${Meta.DEFAULT_STAGE}'"
                )
                db.execSQL(
                    "ALTER TABLE `sessions` ADD COLUMN `subject` TEXT NOT NULL " +
                        "DEFAULT '${Meta.DEFAULT_SUBJECT}'"
                )
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tingke.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    // 未来若还有跨版本跳级的情况，仍以可卸载重装兜底
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
