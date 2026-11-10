package com.gathin.tvhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** Wakes at each prayer time (azan), at midnight (refresh) and after the TV starts or the app updates. */
public class AzanReceiver extends BroadcastReceiver {
    private static final String TAG = "TvHome.Prayer";

    @Override
    public void onReceive(Context context, Intent intent) {
        final Context app = context.getApplicationContext();
        String action = intent != null ? intent.getAction() : null;
        if (PrayerScheduler.ACTION_AZAN.equals(action)) {
            int idx = intent.getIntExtra("idx", 0);
            // Sound first: it must start even if Android refuses to bring Home to the front.
            try {
                app.startForegroundService(new Intent(app, AzanService.class).putExtra("idx", idx));
            } catch (Exception e) {
                Log.w(TAG, "azan service not started: " + e.getMessage());
            }
            // Whatever app is open goes behind Home, which shows the prayer card while the azan plays.
            try {
                app.startActivity(new Intent(app, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            } catch (Exception e) {
                Log.w(TAG, "Home not brought forward: " + e.getMessage());
            }
            return;
        }
        // Boot, update or midnight: keep what is saved working at once, then fetch the new day.
        final PendingResult pending = goAsync();
        new Thread(() -> {
            try {
                PrayerScheduler.ensure(app);
                if (PrayerTimes.refresh(app)) PrayerScheduler.ensure(app);
            } finally {
                pending.finish();
            }
        }, "prayer-refresh").start();
    }
}
