package net.tantawy.yomi;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.view.KeyEvent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

/* شاشة الأذان الكاملة: بتظهر فوق قفل الشاشة زي المنبه */
public class AdhanActivity extends Activity {
    MediaPlayer mp; TextToSpeech tts; int nid; volatile boolean listening = false; Thread det;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE); if (km != null) km.requestDismissKeyguard(this, null);
        } else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        final String title = getIntent().getStringExtra("title"), speak = getIntent().getStringExtra("speak"), lang = getIntent().getStringExtra("lang"), pr = getIntent().getStringExtra("prayer");
        final boolean adhanOn = getIntent().getBooleanExtra("adhanOn", true);
        nid = getIntent().getIntExtra("nid", 0);

        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER); root.setPadding(60, 60, 60, 60);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{Color.parseColor("#1B3A6B"), Color.parseColor("#0B152F")}); root.setBackground(bg);
        TextView ic = new TextView(this); ic.setText("🕌"); ic.setTextSize(72); ic.setGravity(Gravity.CENTER);
        TextView t = new TextView(this); t.setText(title); t.setTextSize(30); t.setTextColor(Color.parseColor("#F3E2BC")); t.setGravity(Gravity.CENTER); t.setPadding(0, 30, 0, 10);
        TextView s = new TextView(this); s.setText(speak == null ? "" : speak); s.setTextSize(17); s.setTextColor(Color.parseColor("#DDE3EE")); s.setGravity(Gravity.CENTER); s.setLineSpacing(8, 1.1f);
        Button stop = new Button(this); stop.setText("إيقاف الأذان"); stop.setTextSize(18); stop.setTextColor(Color.parseColor("#0B152F"));
        GradientDrawable sb = new GradientDrawable(); sb.setCornerRadius(60); sb.setColor(Color.parseColor("#DCB96A")); stop.setBackground(sb);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 170); lp.topMargin = 80; stop.setLayoutParams(lp);
        stop.setOnClickListener(v -> finish());
        final boolean clap = getIntent().getBooleanExtra("clap", false);
        if (!adhanOn) stop.setText("إيقاف");
        root.addView(ic); root.addView(t); root.addView(s); root.addView(stop);
        TextView hint = new TextView(this);
        hint.setText(clap ? "👏 صفّق مرتين، أو قول «اسكت» بصوت عالي، أو دوس زرار الصوت" : "دوس زرار الصوت في جنب الموبايل عشان يسكت");
        hint.setTextSize(14); hint.setTextColor(Color.parseColor("#AEB8C9")); hint.setGravity(Gravity.CENTER); hint.setPadding(0, 40, 0, 0);
        root.addView(hint);
        if (clap) startClap(getIntent().getIntExtra("sens", 2));
        setContentView(root);

        final Runnable playAdhan = () -> runOnUiThread(() -> {
            if (!adhanOn) { if (!getIntent().getBooleanExtra("clap", false)) finish(); else root.postDelayed(this::finish, 4000); return; }
            int id = getResources().getIdentifier("adhan_" + (pr == null ? "dhuhr" : pr), "raw", getPackageName());
            if (id == 0) id = getResources().getIdentifier("adhan_dhuhr", "raw", getPackageName());
            if (id == 0) return;
            try {
                mp = new MediaPlayer();
                mp.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
                mp.setDataSource(this, android.net.Uri.parse("android.resource://" + getPackageName() + "/" + id));
                mp.setOnCompletionListener(m -> finish());
                mp.prepare(); mp.start();
            } catch (Exception ignored) { }
        });
        if (speak != null && !speak.trim().isEmpty()) {
            tts = new TextToSpeech(getApplicationContext(), st -> {
                if (st != TextToSpeech.SUCCESS) { playAdhan.run(); return; }
                tts.setLanguage(new Locale(lang == null || lang.isEmpty() ? "ar" : lang.split("-")[0]));
                tts.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String i) { }
                    @Override public void onDone(String i) { playAdhan.run(); }
                    @Override public void onError(String i) { playAdhan.run(); }
                });
                tts.speak(speak, TextToSpeech.QUEUE_FLUSH, new Bundle(), "adhan");
            });
        } else playAdhan.run();
    }

    /* سماع التصفيق أو الصوت العالي عشان يسكت */
    void startClap(final int sens) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        listening = true;
        final int thr = sens >= 3 ? 7000 : sens == 2 ? 11000 : 16000;
        final double ratio = sens >= 3 ? 5.5 : sens == 2 ? 7 : 9;
        final double shout = sens >= 3 ? 5000 : sens == 2 ? 7500 : 10000;
        det = new Thread(() -> {
            AudioRecord ar = null;
            try {
                int sr = 16000;
                int bs = Math.max(AudioRecord.getMinBufferSize(sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT), 2048);
                ar = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bs);
                if (ar.getState() != AudioRecord.STATE_INITIALIZED) return;
                ar.startRecording();
                short[] buf = new short[512];
                double base = 600; long last = 0; int spikes = 0, loud = 0; long t0 = System.currentTimeMillis();
                while (listening) {
                    int n = ar.read(buf, 0, buf.length);
                    if (n <= 0) continue;
                    int peak = 0; long sum = 0;
                    for (int i = 0; i < n; i++) { int a = Math.abs(buf[i]); if (a > peak) peak = a; sum += a; }
                    double avg = sum / (double) n;
                    long now = System.currentTimeMillis();
                    if (now - t0 < 800) { base = Math.max(base, avg); continue; }
                    boolean sharp = peak > thr && peak > base * ratio && avg < peak * 0.3;
                    if (sharp && now - last > 110) {
                        spikes = (now - last < 900) ? spikes + 1 : 1;
                        last = now;
                        if (spikes >= 2) { runOnUiThread(this::finish); break; }
                    }
                    loud = (avg > shout && avg > base * 3) ? loud + 1 : 0;
                    if (loud >= 4) { runOnUiThread(this::finish); break; }
                    base = base * 0.96 + avg * 0.04;
                }
            } catch (Exception ignored) {
            } finally { try { if (ar != null) { ar.stop(); ar.release(); } } catch (Exception ignored) { } }
        });
        det.start();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_POWER) { finish(); return true; }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        listening = false;
        try { if (mp != null) { mp.stop(); mp.release(); } } catch (Exception ignored) { }
        try { if (tts != null) { tts.stop(); tts.shutdown(); } } catch (Exception ignored) { }
        try { ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(nid); } catch (Exception ignored) { }
        super.onDestroy();
    }
}
