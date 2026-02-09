package com.example.automationapp.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.automationapp.receiver.TimeAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler for exact time-based alarms using AlarmManager
 * This provides more precise timing than WorkManager for time-based triggers
 */
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "AlarmScheduler"
        const val EXTRA_RULE_ID = "rule_id"
        const val EXTRA_TRIGGER_ID = "trigger_id"
        private const val REQUEST_CODE_BASE = 10000
    }

    /**
     * Schedule an exact alarm for a time-based trigger
     *
     * @param ruleId The ID of the rule to trigger
     * @param triggerId The ID of the trigger
     * @param hour The hour to trigger (0-23)
     * @param minute The minute to trigger (0-59)
     * @param days List of days to repeat (e.g., "MON", "TUE"), empty for daily
     */
    fun scheduleTimeAlarm(
        ruleId: Long,
        triggerId: Long,
        hour: Int,
        minute: Int,
        days: List<String> = emptyList()
    ) {
        val calendar = getNextAlarmTime(hour, minute, days)

        if (calendar == null) {
            Log.w(TAG, "No valid alarm time found for rule $ruleId")
            return
        }

        val pendingIntent = createPendingIntent(ruleId, triggerId)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled exact alarm for rule $ruleId at ${calendar.time}")
                } else {
                    // Fall back to inexact alarm if we can't schedule exact alarms
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled inexact alarm for rule $ruleId at ${calendar.time}")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled exact alarm for rule $ruleId at ${calendar.time}")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling alarm", e)
            // Fall back to inexact alarm
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    /**
     * Schedule a repeating alarm for a time-based trigger
     */
    fun scheduleRepeatingAlarm(
        ruleId: Long,
        triggerId: Long,
        hour: Int,
        minute: Int,
        intervalMillis: Long = AlarmManager.INTERVAL_DAY
    ) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If the time has passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val pendingIntent = createPendingIntent(ruleId, triggerId)

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            intervalMillis,
            pendingIntent
        )

        Log.d(TAG, "Scheduled repeating alarm for rule $ruleId at ${calendar.time}")
    }

    /**
     * Cancel an alarm for a specific rule/trigger
     */
    fun cancelAlarm(ruleId: Long, triggerId: Long) {
        val pendingIntent = createPendingIntent(ruleId, triggerId)
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled alarm for rule $ruleId, trigger $triggerId")
    }

    /**
     * Cancel all alarms for a specific rule
     */
    fun cancelAllAlarmsForRule(ruleId: Long) {
        // Since we use ruleId in the request code, we can cancel by that
        val intent = Intent(context, TimeAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (REQUEST_CODE_BASE + ruleId).toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            Log.d(TAG, "Cancelled all alarms for rule $ruleId")
        }
    }

    /**
     * Reschedule an alarm after it fires (for non-repeating alarms with specific days)
     */
    fun rescheduleAlarm(
        ruleId: Long,
        triggerId: Long,
        hour: Int,
        minute: Int,
        days: List<String>
    ) {
        scheduleTimeAlarm(ruleId, triggerId, hour, minute, days)
    }

    private fun createPendingIntent(ruleId: Long, triggerId: Long): PendingIntent {
        val intent = Intent(context, TimeAlarmReceiver::class.java).apply {
            putExtra(EXTRA_RULE_ID, ruleId)
            putExtra(EXTRA_TRIGGER_ID, triggerId)
            action = "com.example.automationapp.TIME_TRIGGER_$ruleId"
        }

        return PendingIntent.getBroadcast(
            context,
            (REQUEST_CODE_BASE + ruleId).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getNextAlarmTime(hour: Int, minute: Int, days: List<String>): Calendar? {
        val calendar = Calendar.getInstance()

        if (days.isEmpty()) {
            // Daily alarm - set for today or tomorrow
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar
        }

        // Find the next day that matches
        val today = getDayOfWeek(calendar.get(Calendar.DAY_OF_WEEK))
        val todayIndex = days.indexOf(today)

        // Check if we can schedule for today
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        if (days.contains(today) && calendar.timeInMillis > System.currentTimeMillis()) {
            return calendar
        }

        // Find the next valid day
        val dayOrder = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
        val currentDayIndex = dayOrder.indexOf(today)

        for (i in 1..7) {
            val nextDayIndex = (currentDayIndex + i) % 7
            val nextDay = dayOrder[nextDayIndex]

            if (days.contains(nextDay)) {
                calendar.add(Calendar.DAY_OF_YEAR, i)
                return calendar
            }
        }

        return null
    }

    private fun getDayOfWeek(calendarDay: Int): String {
        return when (calendarDay) {
            Calendar.SUNDAY -> "SUN"
            Calendar.MONDAY -> "MON"
            Calendar.TUESDAY -> "TUE"
            Calendar.WEDNESDAY -> "WED"
            Calendar.THURSDAY -> "THU"
            Calendar.FRIDAY -> "FRI"
            Calendar.SATURDAY -> "SAT"
            else -> ""
        }
    }
}

