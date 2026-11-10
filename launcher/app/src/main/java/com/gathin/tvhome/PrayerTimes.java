package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;

/** Prayer times for one zone from JAKIM's e-Solat API, fetched once a day and kept on the device. */
final class PrayerTimes {
    private static final String TAG = "TvHome.Prayer";
    /** JAKIM zone: MLK01 is the whole of Melaka. */
    static final String ZONE = "MLK01";
    static final String PLACE = "Melaka";
    /** Order of the times in {@link Day#t}. */
    static final String[] NAMES = {"Subuh", "Syuruk", "Zohor", "Asar", "Maghrib", "Isyak"};
    /** The five prayers that have an azan (Syuruk has none). */
    static final int[] AZAN = {0, 2, 3, 4, 5};
    private static final String PREFS = "prayer";
    private static final String[] MONTHS = {"Muharram", "Safar", "Rabiulawal", "Rabiulakhir", "Jamadilawal",
            "Jamadilakhir", "Rejab", "Syaaban", "Ramadan", "Syawal", "Zulkaedah", "Zulhijjah"};

    private PrayerTimes() {}

    static final class Day {
        final LocalDate date;
        final String hijri;
        final LocalTime[] t = new LocalTime[6];

        Day(LocalDate date, String hijri) {
            this.date = date;
            this.hijri = hijri;
        }
    }

    /** The saved day, or null when nothing has been fetched yet. */
    static Day cached(Context ctx) {
        try {
            SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String date = p.getString("date", null);
            if (date == null) return null;
            Day d = new Day(LocalDate.parse(date), p.getString("hijri", ""));
            for (int i = 0; i < 6; i++) {
                String s = p.getString("t" + i, null);
                if (s == null) return null;
                d.t[i] = parseTime(s);
            }
            return d;
        } catch (Exception e) {
            Log.w(TAG, "cache unreadable: " + e.getMessage());
            return null;
        }
    }

    /** Fetches today's times for the zone and saves them; false when offline or the reply is unusable. */
    static boolean refresh(Context ctx) {
        HttpURLConnection c = null;
        try {
            URL url = new URL("https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=today&zone=" + ZONE);
            c = (HttpURLConnection) url.openConnection();
            c.setConnectTimeout(15000);
            c.setReadTimeout(20000);
            c.setRequestProperty("User-Agent", "GlassTV");
            if (c.getResponseCode() != 200) {
                Log.w(TAG, "e-solat answered " + c.getResponseCode());
                return false;
            }
            String body = readAll(c.getInputStream());
            JSONArray list = new JSONObject(body).getJSONArray("prayerTime");
            JSONObject p = list.getJSONObject(0);
            String[] keys = {"fajr", "syuruk", "dhuhr", "asr", "maghrib", "isha"};
            SharedPreferences.Editor e = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
            for (int i = 0; i < 6; i++) {
                LocalTime t = parseTime(p.getString(keys[i]));
                e.putString("t" + i, t.toString());
            }
            e.putString("date", LocalDate.now().toString());
            e.putString("hijri", p.optString("hijri", ""));
            e.apply();
            return true;
        } catch (Exception e) {
            Log.w(TAG, "refresh failed: " + e.getMessage());
            return false;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static LocalTime parseTime(String s) {
        String[] p = s.trim().split(":");
        return LocalTime.of(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
    }

    private static String readAll(InputStream in) throws Exception {
        try (InputStream is = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        }
    }

    /** "1448-06-27" becomes "27 Jamadilakhir 1448H"; anything else is shown as JAKIM sent it. */
    static String hijriText(String raw) {
        if (raw == null) return "";
        try {
            String[] p = raw.trim().split("-");
            if (p.length == 3 && p[0].length() == 4) {
                int month = Integer.parseInt(p[1]);
                return Integer.parseInt(p[2]) + " " + MONTHS[month - 1] + " " + p[0] + "H";
            }
        } catch (Exception ignored) {
        }
        return raw;
    }
}
