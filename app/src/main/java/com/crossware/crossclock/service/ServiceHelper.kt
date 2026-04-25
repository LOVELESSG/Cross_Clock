package com.crossware.crossclock.service

import android.content.Context
import android.content.Intent

/**
 * 服务辅助工具类，用于简化对后台服务的启动和操作。
 */
object ServiceHelper {

    /**
     * 触发前台服务（秒表服务）执行特定操作。
     * @param context 上下文对象。
     * @param action 要执行的具体操作（例如启动、停止或取消）。
     */
    fun triggerForegroundService(context: Context, action: String) {
        Intent(context, StopWatchService::class.java).apply {
            this.action = action
            context.startService(this)
        }
    }
}