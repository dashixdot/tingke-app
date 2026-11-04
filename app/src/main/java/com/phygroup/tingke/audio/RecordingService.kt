package com.phygroup.tingke.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.phygroup.tingke.MainActivity

/**
 * 录音期间的前台服务。
 *
 * 为什么必须有它：Android 9 起应用进入后台后麦克风会被静音，
 * 只声明 `foregroundServiceType="microphone"` 并真正 startForeground 的前台服务
 * 才能让 MediaRecorder 在熄屏/切后台时继续录到声音。
 */
class RecordingService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForegroundCompat()
            stopSelf()
            return START_NOT_STICKY
        }
        // 个别机型在缺少通知权限/类型声明不匹配时会抛异常，兜住以免整机崩溃；
        // 失败时录音退化为"仅前台可录"，不影响其他功能。
        runCatching { ensureForeground() }
        return START_STICKY
    }

    private fun ensureForeground() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "听课录音", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "听课期间维持录音可用"
                setShowBadge(false)
            }
            nm.createNotificationChannel(ch)
        }

        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("听课打标 · 正在录音")
            .setContentText("计时与录音在后台继续，点按返回")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pi)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTI_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTI_ID, notification)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    companion object {
        private const val CHANNEL_ID = "tingke_recording"
        private const val NOTI_ID = 1001
        const val ACTION_START = "com.phygroup.tingke.action.START_REC"
        const val ACTION_STOP = "com.phygroup.tingke.action.STOP_REC"

        fun start(ctx: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    ctx, Intent(ctx, RecordingService::class.java).setAction(ACTION_START)
                )
            }
        }

        fun stop(ctx: Context) {
            runCatching { ctx.stopService(Intent(ctx, RecordingService::class.java)) }
        }
    }
}
