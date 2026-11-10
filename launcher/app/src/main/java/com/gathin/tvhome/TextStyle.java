package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

/** Makes Home's own texts (clock, date, the name under the picture, the Wi-Fi pill) bold with a black shadow. */
final class TextStyle {
    private TextStyle() {}

    static void apply(Activity activity) {
        float d = activity.getResources().getDisplayMetrics().density;
        int[] ids = {R.id.clock, R.id.ampm, R.id.date, R.id.shamsiDate,
                R.id.heroName, R.id.heroDesc, R.id.heroWhere, R.id.btnWifi};
        for (int id : ids) {
            View v = activity.findViewById(id);
            if (!(v instanceof TextView)) continue;
            TextView t = (TextView) v;
            Typeface current = t.getTypeface();
            t.setTypeface(current != null ? current : Typeface.DEFAULT, Typeface.BOLD);
            t.setShadowLayer(4f * d, 0f, 1.5f * d, 0xFF000000);
        }
    }
}
