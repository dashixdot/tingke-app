package com.phygroup.tingke

import android.app.Application
import android.content.Context
import com.phygroup.tingke.audio.AudioRecorder
import com.phygroup.tingke.data.AppDatabase
import com.phygroup.tingke.data.TingKeRepository

class TingKeApp : Application() {
    val repo: TingKeRepository by lazy {
        TingKeRepository(AppDatabase.get(this))
    }

    override fun onCreate() {
        super.onCreate()
        RecorderHolder.init(this)
    }
}

/**
 * 录音器放在进程级单例里：Activity 重建、Compose 重进都不影响正在进行的录音。
 */
object RecorderHolder {
    @Volatile
    private var instance: AudioRecorder? = null

    fun init(ctx: Context) {
        if (instance == null) {
            synchronized(this) {
                if (instance == null) instance = AudioRecorder(ctx.applicationContext)
            }
        }
    }

    fun get(ctx: Context): AudioRecorder {
        init(ctx)
        return instance!!
    }
}
