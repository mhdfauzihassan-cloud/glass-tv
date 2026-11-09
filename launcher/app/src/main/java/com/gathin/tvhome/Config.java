package com.gathin.tvhome;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class Config {
    private static final String TAG = "TvHome.Config";
    static final String FILE_NAME = "config.json";
    static final String WALLPAPER_NAME = "wallpaper.jpg";
    static final String WALLPAPERS_DIR = "wallpapers";

    final List<String> dockApps;
    final List<String> hiddenApps;
    final String liveTvPackage;
    final String liveTvAction;
    final String sourcePanelService;
    final String sourcePanelAction;
    final String inputAction;
    final int columns;
    final int rowsBelowDock;
    final boolean shamsiDate;
    final boolean moduleCards;
    final boolean reducedMotion;
    final String vpnApp;
    final boolean showHomeSettingsShortcut;
    final boolean showNetworkPill;
    final boolean showLiveTv;
    final int backgroundColor;
    final File wallpaper;
    final List<File> wallpapers;
    final long wallpaperIntervalMs;
    final boolean places;
    final int cacheCleanDays;
    final boolean overlayApplied;

    private Config(List<String> dockApps, List<String> hiddenApps, String liveTvPackage, String liveTvAction,
                   String sourcePanelService, String sourcePanelAction, String inputAction,
                   int columns, int rowsBelowDock,
                   boolean shamsiDate, boolean moduleCards, boolean reducedMotion, String vpnApp,
                   boolean showHomeSettingsShortcut, boolean showNetworkPill, boolean showLiveTv,
                   int backgroundColor,
                   File wallpaper, List<File> wallpapers, long wallpaperIntervalMs,
                   boolean places, int cacheCleanDays, boolean overlayApplied) {
        this.dockApps = Collections.unmodifiableList(dockApps);
        this.hiddenApps = Collections.unmodifiableList(hiddenApps);
        this.liveTvPackage = liveTvPackage;
        this.liveTvAction = liveTvAction;
        this.sourcePanelService = sourcePanelService;
        this.sourcePanelAction = sourcePanelAction;
        this.inputAction = inputAction;
        this.columns = columns;
        this.rowsBelowDock = rowsBelowDock;
        this.shamsiDate = shamsiDate;
        this.moduleCards = moduleCards;
        this.reducedMotion = reducedMotion;
        this.vpnApp = vpnApp;
        this.showHomeSettingsShortcut = showHomeSettingsShortcut;
        this.showNetworkPill = showNetworkPill;
        this.showLiveTv = showLiveTv;
        this.backgroundColor = backgroundColor;
        this.wallpaper = wallpaper;
        this.wallpapers = Collections.unmodifiableList(wallpapers);
        this.wallpaperIntervalMs = wallpaperIntervalMs;
        this.places = places;
        this.cacheCleanDays = cacheCleanDays;
        this.overlayApplied = overlayApplied;
    }

    static File dir(Context ctx) {
        return ctx.getExternalFilesDir(null);
    }

    static Config load(Context ctx) {
        Resources res = ctx.getResources();
        List<String> dock = new ArrayList<>(Arrays.asList(res.getStringArray(R.array.dock_apps)));
        List<String> hidden = new ArrayList<>(Arrays.asList(res.getStringArray(R.array.hidden_apps)));
        String liveTv = res.getString(R.string.live_tv_package).trim();
        String liveTvAction = res.getString(R.string.live_tv_action).trim();
        String panelService = res.getString(R.string.source_panel_service).trim();
        String panelAction = res.getString(R.string.source_panel_action).trim();
        String inputAction = res.getString(R.string.input_action).trim();
        int columns = res.getInteger(R.integer.columns);
        int rowsBelow = res.getInteger(R.integer.rows_below_dock);
        boolean shamsi = res.getBoolean(R.bool.shamsi_date);
        boolean moduleCards = res.getBoolean(R.bool.module_cards);
        boolean reducedMotion = res.getBoolean(R.bool.reduced_motion);
        String vpn = res.getString(R.string.vpn_app).trim();
        boolean homeSettings = res.getBoolean(R.bool.show_home_settings_shortcut);
        boolean networkPill = res.getBoolean(R.bool.show_network_pill);
        boolean liveTvRow = res.getBoolean(R.bool.show_live_tv);
        int background = 0xFF000000;
        boolean applied = false;

        File dir = dir(ctx);
        File file = dir != null ? new File(dir, FILE_NAME) : null;
        JSONObject json = read(file);
        if (json != null) {
            applied = true;
            dock = stringList(json, "dock_apps", dock);
            hidden = stringList(json, "hidden_apps", hidden);
            liveTv = string(json, "live_tv_package", liveTv);
            liveTvAction = string(json, "live_tv_action", liveTvAction);
            panelService = string(json, "source_panel_service", panelService);
            panelAction = string(json, "source_panel_action", panelAction);
            inputAction = string(json, "input_action", inputAction);
            columns = integer(json, "columns", columns, 1, 8);
            rowsBelow = integer(json, "rows_below_dock", rowsBelow, 0, 3);
            shamsi = bool(json, "shamsi_date", shamsi);
            moduleCards = bool(json, "module_cards", moduleCards);
            reducedMotion = bool(json, "reduced_motion", reducedMotion);
            vpn = string(json, "vpn_app", vpn);
            homeSettings = bool(json, "show_home_settings_shortcut", homeSettings);
            networkPill = bool(json, "show_network_pill", networkPill);
            liveTvRow = bool(json, "show_live_tv", liveTvRow);
            background = color(json, "background_color", background);
        }
        File wallpaper = dir != null ? new File(dir, WALLPAPER_NAME) : null;
        if (wallpaper != null && !(wallpaper.isFile() && wallpaper.length() > 0)) wallpaper = null;
        if (wallpaper == null) wallpaper = bundledWallpaper(ctx);
        boolean wallpaperOn = !(json != null && json.has("wallpaper") && !bool(json, "wallpaper", true));
        if (!wallpaperOn) wallpaper = null;
        List<File> wallpapers = new ArrayList<>();
        File[] pics = dir != null && wallpaperOn ? new File(dir, WALLPAPERS_DIR).listFiles() : null;
        if (pics != null) {
            for (File f : pics) {
                String n = f.getName().toLowerCase(java.util.Locale.ROOT);
                if (f.isFile() && f.length() > 0 && (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png"))) wallpapers.add(f);
            }
            Collections.sort(wallpapers);
        }
        int wallpaperMinutes = json != null ? integer(json, "wallpaper_minutes", 30, 0, 1440) : 30;
        int wallpaperSeconds = json != null ? integer(json, "wallpaper_seconds", 0, 5, 86400) : 0;
        long wallpaperIntervalMs = wallpaperSeconds > 0 ? wallpaperSeconds * 1000L : wallpaperMinutes * 60_000L;
        boolean places = wallpaperOn && json != null && bool(json, "places", false);
        int cacheCleanDays = json != null ? integer(json, "cache_clean_days", CacheCleaner.DEFAULT_DAYS, 0, 90)
                : CacheCleaner.DEFAULT_DAYS;

        return new Config(dock, hidden, liveTv, liveTvAction, panelService, panelAction, inputAction,
                Math.max(1, columns), Math.max(0, rowsBelow), shamsi, moduleCards, reducedMotion,
                vpn, homeSettings, networkPill, liveTvRow, background, wallpaper, wallpapers, wallpaperIntervalMs,
                places, cacheCleanDays, applied);
    }

    private static File bundledWallpaper(Context ctx) {
        try {
            File out = new File(ctx.getFilesDir(), "bundled_wallpaper.jpg");
            long updated = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).lastUpdateTime;
            if (!out.isFile() || out.length() == 0 || out.lastModified() < updated) {
                try (InputStream in = ctx.getAssets().open("wallpaper.jpg");
                     OutputStream os = new FileOutputStream(out)) {
                    byte[] buf = new byte[16 * 1024];
                    int r;
                    while ((r = in.read(buf)) > 0) os.write(buf, 0, r);
                }
            }
            return out.length() > 0 ? out : null;
        } catch (Exception e) {
            Log.w(TAG, "no bundled wallpaper: " + e.getMessage());
            return null;
        }
    }

    private static JSONObject read(File file) {
        if (file == null || !file.isFile()) return null;
        if (file.length() > 64 * 1024) {
            Log.w(TAG, "ignoring " + file + ": larger than 64 KB");
            return null;
        }
        try (InputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[(int) file.length()];
            int n = 0, r;
            while (n < buf.length && (r = in.read(buf, n, buf.length - n)) > 0) n += r;
            String text = new String(buf, 0, n, StandardCharsets.UTF_8);
            if (text.startsWith("\uFEFF")) text = text.substring(1);
            return new JSONObject(text);
        } catch (Exception e) {
            Log.w(TAG, "ignoring " + file + ": " + e.getMessage());
            return null;
        }
    }

    private static String string(JSONObject o, String key, String fallback) {
        Object v = o.opt(key);
        return v instanceof String ? ((String) v).trim() : fallback;
    }

    private static boolean bool(JSONObject o, String key, boolean fallback) {
        Object v = o.opt(key);
        return v instanceof Boolean ? (Boolean) v : fallback;
    }

    private static int integer(JSONObject o, String key, int fallback, int min, int max) {
        Object v = o.opt(key);
        if (!(v instanceof Number)) return fallback;
        int i = ((Number) v).intValue();
        return i < min || i > max ? fallback : i;
    }

    private static int color(JSONObject o, String key, int fallback) {
        Object v = o.opt(key);
        if (!(v instanceof String)) return fallback;
        try {
            int c = Color.parseColor(((String) v).trim());
            return c | 0xFF000000;
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private static List<String> stringList(JSONObject o, String key, List<String> fallback) {
        Object v = o.opt(key);
        if (!(v instanceof JSONArray)) return fallback;
        JSONArray a = (JSONArray) v;
        List<String> out = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) {
            Object item = a.opt(i);
            if (item instanceof String && !((String) item).trim().isEmpty()) out.add(((String) item).trim());
        }
        return out;
    }
}
