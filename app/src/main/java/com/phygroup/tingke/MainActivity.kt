package com.phygroup.tingke

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import com.phygroup.tingke.ui.MainViewModel
import com.phygroup.tingke.ui.TingKeRoot

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 全竖屏：本 App 只做手机端。Manifest 里已写 portrait，
        // 这里再用代码锁一次，防止个别 ROM / 分屏 / 大屏策略忽略 Manifest 声明。
        // 竖屏固定后不再触发 Activity 重建，录音回放不会因旋转而中断归零。
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContent {
            TingKeRoot(vm)

            // 听课计时期间保持屏幕常亮，避免息屏导致录音被打断
            val keepOn = vm.running
            DisposableEffect(keepOn) {
                if (keepOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                onDispose { }
            }
        }
    }
}
