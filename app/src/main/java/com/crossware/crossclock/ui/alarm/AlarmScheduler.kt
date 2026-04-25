package com.crossware.crossclock.ui.alarm

import com.crossware.crossclock.data.alarm.Alarm

/**
 * 闹钟调度接口，定义了安排和取消系统闹钟的标准操作。
 */
interface AlarmScheduler {
    /**
     * 根据闹钟实体信息安排一个系统定时任务。
     */
    fun scheduler(item: Alarm)

    /**
     * 取消一个已在系统中安排的闹钟任务。
     */
    fun cancel(item: Alarm)
}
