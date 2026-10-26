package com.crossware.crossclock.ui.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 闹钟取消接收器。
 * 当用户滑动关闭闹钟通知或闹钟超时时触发，停止铃声播放服务。
 */
class AlarmCancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmRingtoneService.stopRingtone(context)
    }
}
