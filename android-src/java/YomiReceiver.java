package net.tantawy.yomi;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import androidx.core.app.NotificationCompat;
import java.util.Locale;

/* لما ييجي ميعاد المنبه: إشعار + صوت Google بيقول الجملة، حتى والتطبيق مقفول */
public class YomiReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent in) {
        final String title = in.getStringExtra("title");
        final String body = in.getStringExtra("body");
        final String speak = in.getStringExtra("speak");
        final String lang = in.getStringExtra("lang");
        int nid = in.getIntExtra("nid", 1);
        final String type = in.getStringExtra("type");
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        WidgetRender.updateAll(ctx);
        if ("dndOn".equals(type) || "dndOff".equals(type)) {
            try {
                if (Build.VERSION.SDK_INT >= 23 && nm.isNotificationPolicyAccessGranted())
                    nm.setInterruptionFilter("dndOn".equals(type) ? NotificationManager.INTERRUPTION_FILTER_PRIORITY : NotificationManager.INTERRUPTION_FILTER_ALL);
            } catch (Exception ignored) { }
            return;
        }
        if ("steps".equals(type)) {
            final PendingResult spr = goAsync();
            final android.hardware.SensorManager sm = (android.hardware.SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
            android.hardware.Sensor sen = sm == null ? null : sm.getDefaultSensor(android.hardware.Sensor.TYPE_STEP_COUNTER);
            if (sen == null) { spr.finish(); return; }
            final Context app = ctx.getApplicationContext();
            sm.registerListener(new android.hardware.SensorEventListener() {
                @Override public void onSensorChanged(android.hardware.SensorEvent e) {
                    sm.unregisterListener(this);
                    app.getSharedPreferences(YomiAlarmsPlugin.PREF, Context.MODE_PRIVATE).edit().putString("stepDay", YomiAlarmsPlugin.today()).putLong("stepBase", (long) e.values[0]).apply();
                    try { spr.finish(); } catch (Exception ignored) { }
                }
                @Override public void onAccuracyChanged(android.hardware.Sensor a, int b) { }
            }, sen, android.hardware.SensorManager.SENSOR_DELAY_NORMAL);
            new Handler(Looper.getMainLooper()).postDelayed(() -> { try { spr.finish(); } catch (Exception ignored) { } }, 8000);
            return;
        }

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("yomi_main", "تنبيهات يومي", NotificationManager.IMPORTANCE_HIGH);
            ch.enableVibration(true);
            ch.setVibrationPattern(new long[]{0, 400, 200, 400});
            nm.createNotificationChannel(ch);
        }
        if (("adhan".equals(type) && in.getBooleanExtra("full", false)) || in.getBooleanExtra("clap", false)) {
            Intent fs = new Intent(ctx, AdhanActivity.class);
            fs.putExtras(in);
            fs.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
            PendingIntent fpi = PendingIntent.getActivity(ctx, nid, fs, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            NotificationCompat.Builder fb = new NotificationCompat.Builder(ctx, "yomi_adhan")
                    .setSmallIcon(R.mipmap.ic_launcher).setContentTitle(title).setContentText(speak)
                    .setPriority(NotificationCompat.PRIORITY_MAX).setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setFullScreenIntent(fpi, true).setContentIntent(fpi).setAutoCancel(true).setOngoing(true);
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel ac = new NotificationChannel("yomi_adhan", "الأذان", NotificationManager.IMPORTANCE_HIGH);
                ac.setSound(null, null);
                nm.createNotificationChannel(ac);
            }
            try { nm.notify(nid, fb.build()); } catch (Exception ignored) { }
            try { ctx.startActivity(fs); } catch (Exception ignored) { }
            return;
        }
        Intent open = new Intent(ctx, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, nid, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String text = (body == null || body.isEmpty()) ? (speak == null ? "" : speak) : body;
        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, "yomi_main")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(speak == null || speak.isEmpty() ? text : speak))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pi);
        try { nm.notify(nid, b.build()); } catch (Exception ignored) { }

        if (speak == null || speak.trim().isEmpty()) return;
        final PendingResult pr = goAsync();
        final Handler h = new Handler(Looper.getMainLooper());
        final TextToSpeech[] t = new TextToSpeech[1];
        final boolean[] finished = {false};
        final Runnable done = () -> {
            if (finished[0]) return;
            finished[0] = true;
            try { if (t[0] != null) { t[0].stop(); t[0].shutdown(); } } catch (Exception ignored) { }
            try { pr.finish(); } catch (Exception ignored) { }
        };
        h.postDelayed(done, 25000);
        t[0] = new TextToSpeech(ctx.getApplicationContext(), status -> {
            if (status != TextToSpeech.SUCCESS) { h.post(done); return; }
            try {
                String l = (lang == null || lang.isEmpty()) ? "ar" : lang.split("-")[0];
                t[0].setLanguage(new Locale(l));
                t[0].setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
                t[0].setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String id) { }
                    @Override public void onDone(String id) { h.post(done); }
                    @Override public void onError(String id) { h.post(done); }
                });
                t[0].speak(speak, TextToSpeech.QUEUE_FLUSH, new Bundle(), "yomi");
            } catch (Exception e) { h.post(done); }
        });
    }
}
