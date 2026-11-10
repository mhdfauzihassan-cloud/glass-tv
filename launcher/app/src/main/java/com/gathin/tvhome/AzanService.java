package com.gathin.tvhome;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

/** Plays the azan (assets/azan.mp3, or azan_subuh.mp3 at Subuh) and takes audio focus so other apps pause. */
public class AzanService extends Service {
    private static final String TAG = "TvHome.Prayer";
    private static final String CHANNEL = "azan";
    private static final long MAX_MS = 8 * 60_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private MediaPlayer player;
    private AudioManager audio;
    private AudioFocusRequest focus;
    private boolean finished;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int idx = intent != null ? intent.getIntExtra("idx", 0) : 0;
        if (idx < 0 || idx >= PrayerTimes.NAMES.length) idx = 0;
        startForegroundNotice(PrayerTimes.NAMES[idx]);
        release();
        finished = false;
        PrayerCard.azanStarted(idx);
        play(idx == 0 ? "azan_subuh.mp3" : "azan.mp3");
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::finish, MAX_MS);
        return START_NOT_STICKY;
    }

    private void startForegroundNotice(String name) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Azan", NotificationManager.IMPORTANCE_LOW));
        Notification n = new Notification.Builder(this, CHANNEL)
                .setContentTitle("Masuk waktu " + name)
                .setContentText("Azan berkumandang")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build();
        if (Build.VERSION.SDK_INT >= 29) startForeground(1701, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        else startForeground(1701, n);
    }

    private void play(String file) {
        try {
            AssetFileDescriptor afd;
            try {
                afd = getAssets().openFd(file);
            } catch (Exception missing) {
                afd = getAssets().openFd("azan.mp3");   // no Subuh recording: the ordinary azan
            }
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
            audio = getSystemService(AudioManager.class);
            focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attrs).build();
            audio.requestAudioFocus(focus);
            player = new MediaPlayer();
            player.setAudioAttributes(attrs);
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            player.setOnCompletionListener(mp -> finish());
            player.setOnErrorListener((mp, what, extra) -> {
                finish();
                return true;
            });
            player.prepare();
            player.start();
        } catch (Exception e) {
            // No azan file in the app: a short tone, so the time is still announced.
            Log.w(TAG, "azan file not played: " + e.getMessage());
            try {
                new ToneGenerator(AudioManager.STREAM_MUSIC, 100).startTone(ToneGenerator.TONE_PROP_BEEP2, 2000);
            } catch (Exception ignored) {
            }
            handler.postDelayed(this::finish, 2500);
        }
    }

    private void release() {
        if (player != null) {
            try {
                player.stop();
            } catch (Exception ignored) {
            }
            player.release();
            player = null;
        }
        if (audio != null && focus != null) {
            audio.abandonAudioFocusRequest(focus);
            focus = null;
        }
    }

    private void finish() {
        if (finished) return;
        finished = true;
        release();
        PrayerCard.azanEnded();
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        release();
        if (!finished) PrayerCard.azanEnded();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
