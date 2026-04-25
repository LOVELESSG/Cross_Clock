package com.crossware.crossclock.ui.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlin.system.exitProcess

/**
 * 闹钟取消接收器。
 * 当用户滑动关闭闹钟通知或在全屏界面点击取消时，执行清理逻辑。
 */
class AlarmCancelReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 简单实现：退出进程以停止铃声播放
        // 注意：在实际复杂应用中，通常应通过 Binder 或 EventBus 停止特定的铃声播放服务
        exitProcess(0)
    }
}
