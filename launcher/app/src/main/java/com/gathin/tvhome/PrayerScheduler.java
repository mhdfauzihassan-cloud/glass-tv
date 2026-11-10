package com.gathin.tvhome;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Sets the system alarms that start the azan, and the nightly refresh of the day's times. */
final class PrayerScheduler {
    private static final String TAG = "TvHome.Prayer";
    static final String ACTION_AZAN = "com.gathin.tvhome.AZAN";
    static final String ACTION_REFRESH = "com.gathin.tvhome.PRAYER_REFRESH";
    private static final int REFRESH_CODE = 900;

    private PrayerScheduler() {}

    /** Alarms for every azan still to come today, and one just after midnight to fetch the next day. */
    static void ensure(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PrayerTimes.Day day = PrayerTimes.cached(ctx);
        LocalDateTime now = LocalDateTime.now();
        if (day != null && day.date.equals(LocalDate.now())) {
            for (int idx : PrayerTimes.AZAN) {
                LocalDateTime at = LocalDateTime.of(day.date, day.t[idx]);
                if (at.isAfter(now)) set(am, ctx, 100 + idx, ACTION_AZAN, idx, millis(at));
            }
        }
        set(am, ctx, REFRESH_CODE, ACTION_REFRESH, -1, millis(LocalDate.now().plusDays(1).atTime(0, 5)));
    }

    private static long millis(LocalDateTime t) {
        return t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static void set(AlarmManager am, Context ctx, int code, String action, int idx, long at) {
        Intent i = new Intent(ctx, AzanReceiver.class).setAction(action).putExtra("idx", idx);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, code, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        } catch (SecurityException e) {
            Log.w(TAG, "alarm refused: " + e.getMessage());
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }
}
