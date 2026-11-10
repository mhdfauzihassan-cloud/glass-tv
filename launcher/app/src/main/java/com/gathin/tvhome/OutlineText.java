package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.widget.TextView;

/**
 * Bold text with a dark outline-like shadow, so it stays readable over a bright wallpaper. The text is
 * drawn twice: the shadow is soft, and a second pass makes it dense enough to read as a black edge.
 */
final class OutlineText extends TextView {
    OutlineText(Context context) {
        super(context);
        float d = context.getResources().getDisplayMetrics().density;
        setTypeface(Typeface.DEFAULT_BOLD);
        setShadowLayer(3.5f * d, 0f, 1.2f * d, 0xFF000000);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        super.onDraw(canvas);
    }
}
