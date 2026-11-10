package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalDate;

/** A small card with the day's zikir: a heart and "Zikir Selasa" above a rule, the words in Arabic below. */
final class DzikirCard extends LinearLayout {
    /**
     * One zikir for each day, Isnin (Monday) first. These are placeholders: change the words
     * between the quotation marks to follow your own practice.
     */
    private static final String[] DZIKIR = {
            "لا إله إلا الله",        // Isnin
            "يا الله",                // Selasa
            "أستغفر الله",            // Rabu
            "الحمد لله",              // Khamis
            "اللهم صل على محمد",      // Jumaat
            "سبحان الله",             // Sabtu
            "الله أكبر"               // Ahad
    };
    private static final String[] HARI = {"Isnin", "Selasa", "Rabu", "Khamis", "Jumaat", "Sabtu", "Ahad"};

    /** Where the card sits: from the left and top edges, and how wide, in dp. */
    private static final int LEFT_DP = 55, TOP_DP = 250, WIDTH_DP = 300;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final TextView title, words;
    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            bind();
            handler.postDelayed(this, 60_000L);
        }
    };

    static void attach(Activity activity, FrameLayout root) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(activity, WIDTH_DP),
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.TOP);
        lp.leftMargin = dp(activity, LEFT_DP);
        lp.topMargin = dp(activity, TOP_DP);
        root.addView(new DzikirCard(activity), lp);
    }

    private static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    private DzikirCard(Context ctx) {
        super(ctx);
        setOrientation(VERTICAL);
        setFocusable(false);
        setFocusableInTouchMode(false);
        int pad = dp(ctx, 16);
        setPadding(pad, dp(ctx, 12), pad, dp(ctx, 12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xB3101A2E);
        bg.setCornerRadius(dp(ctx, 22));
        bg.setStroke(dp(ctx, 1), 0x33FFFFFF);
        setBackground(bg);

        LinearLayout head = new LinearLayout(ctx);
        head.setOrientation(HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LayoutParams hp = new LayoutParams(dp(ctx, 20), dp(ctx, 18));
        hp.rightMargin = dp(ctx, 10);
        head.addView(new Heart(ctx), hp);
        title = new OutlineText(ctx);
        title.setTextSize(16);
        title.setTextColor(0xFFFFFFFF);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setSingleLine(true);
        head.addView(title, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        addView(head, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        View rule = new View(ctx);
        rule.setBackgroundColor(0x33FFFFFF);
        LayoutParams rp = new LayoutParams(LayoutParams.MATCH_PARENT, dp(ctx, 1));
        rp.topMargin = dp(ctx, 10);
        addView(rule, rp);

        words = new OutlineText(ctx);
        words.setTextSize(30);
        words.setTextColor(0xFFFFFFFF);
        words.setGravity(Gravity.CENTER);
        LayoutParams wp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        wp.topMargin = dp(ctx, 8);
        addView(words, wp);
        bind();
    }

    private void bind() {
        int i = LocalDate.now().getDayOfWeek().getValue() - 1;   // Monday = 0
        title.setText("Zikir " + HARI[i]);
        words.setText(DZIKIR[i]);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getWindowVisibility() == VISIBLE) start();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) start();
        else handler.removeCallbacks(refresh);
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(refresh);
        super.onDetachedFromWindow();
    }

    private void start() {
        handler.removeCallbacks(refresh);
        handler.post(refresh);
    }

    /** A small white heart, drawn here so it does not depend on a font having the symbol. */
    private static final class Heart extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        Heart(Context c) {
            super(c);
            paint.setColor(0xFFFFFFFF);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight();
            path.reset();
            path.moveTo(w * 0.5f, h * 0.95f);
            path.cubicTo(-w * 0.15f, h * 0.55f, w * 0.05f, -h * 0.05f, w * 0.5f, h * 0.30f);
            path.cubicTo(w * 0.95f, -h * 0.05f, w * 1.15f, h * 0.55f, w * 0.5f, h * 0.95f);
            path.close();
            canvas.drawPath(path, paint);
        }
    }
}
