package net.tantawy.yomi;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

/* شاشة التنبيه: الأذان أو التذكير. في وضع الإسكات: بيتكلم، ويسكت ٣ ثواني يسمعك فيها، ويعيد لو ماسمعش */
public class AdhanActivity extends Activity {
    MediaPlayer mp; TextToSpeech tts; int nid; TextView meter;
    volatile boolean listening = false, armed = false, stopped = false;
    Handler h = new Handler(Looper.getMainLooper());
    boolean clap, adhanOn, calib; String speak, lang, pr; int reps = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE); if (km != null) km.requestDismissKeyguard(this, null);
        } else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        String title = getIntent().getStringExtra("title");
        speak = getIntent().getStringExtra("speak"); lang = getIntent().getStringExtra("lang"); pr = getIntent().getStringExtra("prayer");
        adhanOn = getIntent().getBooleanExtra("adhanOn", true); clap = getIntent().getBooleanExtra("clap", false); calib = getIntent().getBooleanExtra("calib", false);
        nid = getIntent().getIntExtra("nid", 0);

        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER); root.setPadding(60, 60, 60, 60);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{Color.parseColor("#1B3A6B"), Color.parseColor("#0B152F")}));
        TextView ic = new TextView(this); ic.setText(calib ? "👏" : "🕌"); ic.setTextSize(72); ic.setGravity(Gravity.CENTER);
        TextView t = new TextView(this); t.setText(calib ? "معايرة السمع" : title); t.setTextSize(28); t.setTextColor(Color.parseColor("#F3E2BC")); t.setGravity(Gravity.CENTER); t.setPadding(0, 30, 0, 10);
        TextView s = new TextView(this); s.setText(calib ? "روح المكان اللي بتبقى فيه بالليل، وصفّق مرة واحدة جامد" : (speak == null ? "" : speak)); s.setTextSize(17); s.setTextColor(Color.parseColor("#DDE3EE")); s.setGravity(Gravity.CENTER); s.setLineSpacing(8, 1.1f);
        Button stop = new Button(this); stop.setText(calib ? "إلغاء" : (adhanOn ? "إيقاف الأذان" : "إيقاف")); stop.setTextSize(18); stop.setTextColor(Color.parseColor("#0B152F"));
        GradientDrawable sb = new GradientDrawable(); sb.setCornerRadius(60); sb.setColor(Color.parseColor("#DCB96A")); stop.setBackground(sb);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 170); lp.topMargin = 80; stop.setLayoutParams(lp);
        stop.setOnClickListener(v -> finish());
        TextView hint = new TextView(this);
        hint.setText(clap ? "👏 صفّق أو قول «اسكت» وهو ساكت، أو دوس زرار الصوت" : calib ? "" : "دوس زرار الصوت في جنب الموبايل عشان يسكت");
        hint.setTextSize(14); hint.setTextColor(Color.parseColor("#AEB8C9")); hint.setGravity(Gravity.CENTER); hint.setPadding(0, 40, 0, 0);
        meter = new TextView(this); meter.setTextSize(16); meter.setTextColor(Color.parseColor("#DCB96A")); meter.setGravity(Gravity.CENTER); meter.setPadding(0, 24, 0, 0);
        root.addView(ic); root.addView(t); root.addView(s); root.addView(stop); root.addView(hint); root.addView(meter);
        setContentView(root);

        boolean mic = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        if ((clap || calib) && !mic) { meter.setText("⚠️ إذن المايك مقفول، افتح «يومي» واسمح بالمايك"); clap = false; }
        if ((clap || calib) && mic) startListen();
        if (calib) { armed = true; meter.setText("🎤 بسمعك… صفّق دلوقتي"); h.postDelayed(() -> { if (!stopped) { meter.setText("ماسمعتش تصفيق. قرّب شوية وجرّب تاني"); h.postDelayed(this::finish, 2500); } }, 15000); return; }
        startTts();
    }

    void startTts() {
        if (speak == null || speak.trim().isEmpty()) { afterSpeech(); return; }
        tts = new TextToSpeech(getApplicationContext(), st -> {
            if (st != TextToSpeech.SUCCESS) { h.post(this::afterSpeech); return; }
            tts.setLanguage(new Locale(lang == null || lang.isEmpty() ? "ar" : lang.split("-")[0]));
            YomiAlarmsPlugin.applyVoice(tts, getIntent().getStringExtra("voice"));
            tts.setSpeechRate(getIntent().getFloatExtra("rate", 0.9f));
            tts.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String i) { }
                @Override public void onDone(String i) { h.post(() -> afterSpeech()); }
                @Override public void onError(String i) { h.post(() -> afterSpeech()); }
            });
            h.post(this::sayOnce);
        });
    }
    void sayOnce() { if (stopped) return; armed = false; meter.setText(clap ? "🔊 بيتكلم…" : ""); tts.speak(speak, TextToSpeech.QUEUE_FLUSH, new Bundle(), "a" + reps); }

    /* بعد الكلام: في وضع الإسكات نسكت ٣ ثواني ونسمع */
    void afterSpeech() {
        if (stopped) return;
        reps++;
        if (clap) {
            armed = true; meter.setText("🎤 بسمعك… صفّق أو قول «اسكت»");
            h.postDelayed(() -> {
                if (stopped) return;
                armed = false;
                if (adhanOn) playAdhan();
                else if (reps < 3 && tts != null) sayOnce();
                else finish();
            }, 3200);
            return;
        }
        if (adhanOn) playAdhan(); else finish();
    }

    void playAdhan() {
        if (stopped) return;
        int id = getResources().getIdentifier("adhan_" + (pr == null || pr.isEmpty() ? "dhuhr" : pr), "raw", getPackageName());
        if (id == 0) id = getResources().getIdentifier("adhan_dhuhr", "raw", getPackageName());
        if (id == 0) { finish(); return; }
        try {
            mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            mp.setDataSource(this, android.net.Uri.parse("android.resource://" + getPackageName() + "/" + id));
            mp.setOnCompletionListener(m -> finish());
            mp.prepare(); mp.start();
            if (clap) meter.setText("دوس زرار الصوت عشان توقف الأذان");
        } catch (Exception e) { finish(); }
    }

    /* السمع: بنشتغل على الصوت الحاد (التصفيق) والصوت العالي المستمر («اسكت») وقت السكوت بس */
    void startListen() {
        SharedPreferences sp = getSharedPreferences(YomiAlarmsPlugin.PREF, MODE_PRIVATE);
        final int sens = getIntent().getIntExtra("sens", 2);
        final double saved = sp.getFloat("clapThr", 0f);
        final double minThr = calib ? 900 : (saved > 0 ? saved : (sens >= 3 ? 1200 : sens == 2 ? 2000 : 3500));
        listening = true;
        new Thread(() -> {
            AudioRecord ar = null;
            try {
                int sr = 16000;
                int bs = Math.max(AudioRecord.getMinBufferSize(sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT), 2048);
                ar = new AudioRecord(MediaRecorder.AudioSource.MIC, sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bs);
                if (ar.getState() != AudioRecord.STATE_INITIALIZED) { h.post(() -> meter.setText("⚠️ المايك مشغول")); return; }
                ar.startRecording();
                short[] buf = new short[400];
                double base = 0; int loud = 0; long armedAt = 0; boolean wasArmed = false; long lastUi = 0;
                while (listening) {
                    int n = ar.read(buf, 0, buf.length);
                    if (n <= 1) continue;
                    int dpeak = 0; long dsum = 0;
                    for (int i = 1; i < n; i++) { int d = Math.abs(buf[i] - buf[i - 1]); if (d > dpeak) dpeak = d; dsum += d; }
                    double davg = dsum / (double) (n - 1);
                    long now = System.currentTimeMillis();
                    if (armed && !wasArmed) { armedAt = now; base = davg; }
                    wasArmed = armed;
                    if (!armed) { loud = 0; continue; }
                    if (now - armedAt < 250) { base = Math.max(base, davg); continue; }
                    final double b0 = Math.max(base, 60);
                    final int pk = dpeak;
                    if (now - lastUi > 120) { lastUi = now; final int lvl = (int) Math.min(10, davg / (minThr / 10.0)); h.post(() -> { if (!stopped && armed) meter.setText("🎤 بسمعك  " + "▮▮▮▮▮▮▮▮▮▮".substring(0, Math.max(1, lvl))); }); }
                    boolean clapHit = pk > minThr && pk > b0 * 6;
                    loud = (davg > minThr / 4 && davg > b0 * 3.5) ? loud + 1 : 0;
                    if (clapHit || loud >= 5) {
                        if (calib) {
                            final float thr = (float) Math.max(900, pk * 0.45);
                            getSharedPreferences(YomiAlarmsPlugin.PREF, MODE_PRIVATE).edit().putFloat("clapThr", thr).apply();
                            h.post(() -> { stopped = true; meter.setText("✅ سمعتك! اتظبط على المسافة دي"); h.postDelayed(this::finish, 2200); });
                        } else h.post(() -> { meter.setText("👏 سمعتك، هسكت"); h.postDelayed(this::finish, 400); stopped = true; stopAll(); });
                        break;
                    }
                    base = base * 0.95 + davg * 0.05;
                }
            } catch (Exception ignored) {
            } finally { try { if (ar != null) { ar.stop(); ar.release(); } } catch (Exception ignored) { } }
        }).start();
    }

    void stopAll() {
        try { if (mp != null) { mp.stop(); } } catch (Exception ignored) { }
        try { if (tts != null) tts.stop(); } catch (Exception ignored) { }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) { finish(); return true; }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        stopped = true; listening = false; armed = false;
        h.removeCallbacksAndMessages(null);
        try { if (mp != null) { mp.stop(); mp.release(); } } catch (Exception ignored) { }
        try { if (tts != null) { tts.stop(); tts.shutdown(); } } catch (Exception ignored) { }
        try { ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(nid); } catch (Exception ignored) { }
        super.onDestroy();
    }
}
