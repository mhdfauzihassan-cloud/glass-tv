package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * The prayer-times card on the right of Home: the five prayers, the next one with a countdown, and the
 * Hijri date. It takes no focus, so the D-pad moves over the tiles exactly as before.
 */
final class PrayerCard extends LinearLayout {
    private static final int GOLD = 0xFFFFD54F;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLUE = 0xFF2D5BBA;
    private static final int GREEN = 0xFF2E7D32;

    /** The card on screen, and the prayer whose azan is playing (-1 for none); the service reports both here. */
    private static PrayerCard current;
    private static volatile int azanPlaying = -1;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final TextView hijri, banner;
    private final TextView[] times = new TextView[5];
    private final TextView[] names = new TextView[5];
    private final GradientDrawable bannerBg = new GradientDrawable();
    private PrayerTimes.Day day;
    private boolean refreshing;
    private long lastTry;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            render();
            handler.postDelayed(this, 1000);
        }
    };

    /** Puts the card on Home and makes sure the azan alarms are set. */
    static void attach(Activity activity, FrameLayout root) {
        PrayerCard card = new PrayerCard(activity);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(activity, 264),
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.END | Gravity.TOP);
        lp.topMargin = dp(activity, 118);
        lp.rightMargin = dp(activity, 56);
        root.addView(card, lp);
        DzikirCard.attach(activity, root);
        PrayerScheduler.ensure(activity.getApplicationContext());
    }

    static void azanStarted(int idx) {
        azanPlaying = idx;
        PrayerCard c = current;
        if (c != null) c.handler.post(c::render);
    }

    static void azanEnded() {
        azanPlaying = -1;
        PrayerCard c = current;
        if (c != null) c.handler.post(c::render);
    }

    private static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    private PrayerCard(Context ctx) {
        super(ctx);
        setOrientation(VERTICAL);
        setFocusable(false);
        setFocusableInTouchMode(false);
        int pad = dp(ctx, 16);
        setPadding(pad, dp(ctx, 14), pad, dp(ctx, 14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xB3101A2E);
        bg.setCornerRadius(dp(ctx, 22));
        bg.setStroke(dp(ctx, 1), 0x33FFFFFF);
        setBackground(bg);

        TextView title = text(ctx, "Waktu Solat · " + PrayerTimes.PLACE, 14, GOLD, true);
        title.setGravity(Gravity.END);
        addView(title, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        for (int k = 0; k < 5; k++) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(HORIZONTAL);
            names[k] = text(ctx, PrayerTimes.NAMES[PrayerTimes.AZAN[k]], 16, WHITE, false);
            times[k] = text(ctx, "--:--", 16, WHITE, false);
            times[k].setGravity(Gravity.END);
            row.addView(names[k], new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
            row.addView(times[k], new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            LayoutParams rlp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            rlp.topMargin = dp(ctx, k == 0 ? 10 : 7);
            addView(row, rlp);
        }

        banner = text(ctx, "Memuat waktu solat…", 14, WHITE, true);
        banner.setGravity(Gravity.CENTER);
        bannerBg.setCornerRadius(dp(ctx, 14));
        bannerBg.setColor(BLUE);
        banner.setBackground(bannerBg);
        banner.setPadding(dp(ctx, 10), dp(ctx, 9), dp(ctx, 10), dp(ctx, 9));
        LayoutParams blp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        blp.topMargin = dp(ctx, 12);
        addView(banner, blp);

        hijri = text(ctx, "", 12, 0xCCFFFFFF, false);
        hijri.setGravity(Gravity.CENTER);
        LayoutParams hlp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        hlp.topMargin = dp(ctx, 9);
        addView(hijri, hlp);
    }

    private static TextView text(Context ctx, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(ctx);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        t.setSingleLine(true);
        return t;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        current = this;
        if (getWindowVisibility() == VISIBLE) start();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) start();
        else handler.removeCallbacks(tick);
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(tick);
        if (current == this) current = null;
        super.onDetachedFromWindow();
    }

    /** Shows the saved times at once and counts down every second while Home is in front. */
    private void start() {
        current = this;
        load();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    private void load() {
        final Context app = getContext().getApplicationContext();
        day = PrayerTimes.cached(app);
        boolean stale = day == null || !day.date.equals(LocalDate.now());
        long nowMs = System.currentTimeMillis();
        if (stale && !refreshing && nowMs - lastTry > 10 * 60_000L) {
            refreshing = true;
            lastTry = nowMs;
            new Thread(() -> {
                boolean ok = PrayerTimes.refresh(app);
                if (ok) PrayerScheduler.ensure(app);
                handler.post(() -> {
                    refreshing = false;
                    day = PrayerTimes.cached(app);
                    render();
                });
            }, "prayer-refresh").start();
        }
    }

    private void render() {
        if (day == null) {
            banner.setText("Memuat waktu solat…");
            bannerBg.setColor(BLUE);
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        int next = -1;
        for (int k = 0; k < 5; k++) {
            if (day.t[PrayerTimes.AZAN[k]].isAfter(now.toLocalTime())) {
                next = k;
                break;
            }
        }
        for (int k = 0; k < 5; k++) {
            times[k].setText(day.t[PrayerTimes.AZAN[k]].toString());
            int color = k == next ? GOLD : WHITE;
            Typeface face = k == next ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT;
            names[k].setTextColor(color);
            names[k].setTypeface(face);
            times[k].setTextColor(color);
            times[k].setTypeface(face);
        }
        int playing = azanPlaying;
        if (playing >= 0) {
            banner.setText("Masuk waktu " + PrayerTimes.NAMES[playing] + " · Azan");
            bannerBg.setColor(GREEN);
        } else {
            int idx = next >= 0 ? PrayerTimes.AZAN[next] : PrayerTimes.AZAN[0];
            LocalDateTime target = LocalDateTime.of(
                    next >= 0 ? now.toLocalDate() : now.toLocalDate().plusDays(1), day.t[idx]);
            long secs = Math.max(0, Duration.between(now, target).getSeconds());
            banner.setText("Berikutnya: " + PrayerTimes.NAMES[idx] + " " + day.t[idx] + " · " + clock(secs));
            bannerBg.setColor(BLUE);
        }
        hijri.setText(PrayerTimes.hijriText(day.hijri));
    }

    private static String clock(long secs) {
        long h = secs / 3600, m = (secs % 3600) / 60, s = secs % 60;
        return h > 0 ? String.format(Locale.US, "%d:%02d:%02d", h, m, s) : String.format(Locale.US, "%02d:%02d", m, s);
    }
}
