package net.tantawy.yomi;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/* منبهات «يومي»: بتتجدول في أندرويد نفسه، فبتشتغل والتطبيق مقفول */
@CapacitorPlugin(name = "YomiAlarms")
public class YomiAlarmsPlugin extends Plugin {
    static final String PREF = "yomi_alarms";

    static void scheduleList(Context ctx, JSONArray arr) throws Exception {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        int old = sp.getInt("n", 0);
        for (int i = 0; i < old; i++) {
            PendingIntent pi = PendingIntent.getBroadcast(ctx, 1000 + i, new Intent(ctx, YomiReceiver.class), PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null) { am.cancel(pi); pi.cancel(); }
        }
        boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
        long now = System.currentTimeMillis();
        int n = 0;
        for (int i = 0; i < arr.length() && n < 130; i++) {
            JSONObject o = arr.getJSONObject(i);
            long at = o.getLong("at");
            if (at <= now) continue;
            int nid = 1000 + n;
            Intent it = new Intent(ctx, YomiReceiver.class);
            it.putExtra("title", o.optString("title")); it.putExtra("body", o.optString("body")); it.putExtra("speak", o.optString("speak"));
            it.putExtra("lang", o.optString("lang", "ar")); it.putExtra("type", o.optString("type", "voice")); it.putExtra("prayer", o.optString("prayer", ""));
            it.putExtra("full", o.optBoolean("full", false)); it.putExtra("adhanOn", o.optBoolean("adhanOn", true)); it.putExtra("nid", nid);
            PendingIntent pi = PendingIntent.getBroadcast(ctx, nid, it, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            n++;
        }
        sp.edit().putInt("n", n).putString("list", arr.toString()).apply();
    }

    static void reschedule(Context ctx) {
        try { scheduleList(ctx, new JSONArray(ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("list", "[]"))); } catch (Exception ignored) { }
    }

    @PluginMethod
    public void schedule(PluginCall call) {
        try {
            Context ctx = getContext();
            JSArray arr = call.getArray("alarms");
            scheduleList(ctx, arr);
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            JSObject r = new JSObject();
            r.put("scheduled", ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt("n", 0));
            r.put("exact", Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms());
            call.resolve(r);
        } catch (Exception e) { call.reject(e.getMessage()); }
    }

    @PluginMethod
    public void setWidget(PluginCall call) {
        try {
            SharedPreferences.Editor e = getContext().getSharedPreferences(PREF, Context.MODE_PRIVATE).edit();
            JSObject d = call.getObject("data");
            if (d != null) e.putString("wdata", d.toString());
            else if (call.getArray("prayers") != null) { JSONObject o = new JSONObject(); o.put("prayers", call.getArray("prayers")); e.putString("wdata", o.toString()); }
            e.apply();
            WidgetRender.updateAll(getContext());
        } catch (Exception ignored) { }
        call.resolve();
    }

    @PluginMethod
    public void tasbeeh(PluginCall call) {
        SharedPreferences sp = getContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
        JSObject r = new JSObject();
        r.put("day", sp.getString("tasb_day", "")); r.put("n", sp.getInt("tasb_n", 0)); r.put("total", sp.getInt("tasb_total", 0));
        call.resolve(r);
    }

    @PluginMethod
    public void pendingGo(PluginCall call) {
        SharedPreferences sp = getContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
        JSObject r = new JSObject(); r.put("go", sp.getString("go", "")); sp.edit().remove("go").apply();
        call.resolve(r);
    }

    @PluginMethod
    public void dnd(PluginCall call) {
        JSObject r = new JSObject();
        try {
            android.app.NotificationManager nm = (android.app.NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT < 23) { r.put("ok", false); call.resolve(r); return; }
            if (!nm.isNotificationPolicyAccessGranted()) {
                if (call.getBoolean("ask", false)) { Intent i = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); getContext().startActivity(i); }
                r.put("ok", false); call.resolve(r); return;
            }
            Boolean on = call.getBoolean("on");
            if (on != null) nm.setInterruptionFilter(on ? android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY : android.app.NotificationManager.INTERRUPTION_FILTER_ALL);
            r.put("ok", true);
        } catch (Exception e) { r.put("ok", false); }
        call.resolve(r);
    }

    @PluginMethod
    public void status(PluginCall call) {
        Context ctx = getContext();
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        JSObject r = new JSObject();
        r.put("exact", Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms());
        r.put("scheduled", ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt("n", 0));
        r.put("version", 3);
        call.resolve(r);
    }

    static String today() { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()); }

    @PluginMethod
    public void steps(PluginCall call) {
        final Context ctx = getContext();
        if (Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            if (getActivity() != null) ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 7);
            JSObject r = new JSObject(); r.put("needPermission", true); call.resolve(r); return;
        }
        final SensorManager sm = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        Sensor s = sm == null ? null : sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (s == null) { JSObject r = new JSObject(); r.put("noSensor", true); call.resolve(r); return; }
        sm.registerListener(new SensorEventListener() {
            @Override public void onSensorChanged(SensorEvent e) {
                sm.unregisterListener(this);
                long total = (long) e.values[0];
                SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
                String d = today();
                if (!d.equals(sp.getString("stepDay", "")) || total < sp.getLong("stepBase", 0)) sp.edit().putString("stepDay", d).putLong("stepBase", total).apply();
                JSObject r = new JSObject(); r.put("today", total - sp.getLong("stepBase", total)); r.put("total", total); call.resolve(r);
            }
            @Override public void onAccuracyChanged(Sensor x, int y) { }
        }, s, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @PluginMethod
    public void requestExact(PluginCall call) {
        Context ctx = getContext();
        if (Build.VERSION.SDK_INT >= 31) {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (!am.canScheduleExactAlarms()) {
                Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + ctx.getPackageName()));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try { ctx.startActivity(i); } catch (Exception ignored) { }
            }
        }
        call.resolve();
    }

    @PluginMethod
    public void fullScreenSettings(PluginCall call) {
        try {
            Intent i = Build.VERSION.SDK_INT >= 34 ? new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:" + getContext().getPackageName()))
                    : new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, getContext().getPackageName());
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
        } catch (Exception ignored) { }
        call.resolve();
    }

    @PluginMethod
    public void batterySettings(PluginCall call) {
        try {
            Intent i = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
        } catch (Exception ignored) { }
        call.resolve();
    }

    @PluginMethod
    public void test(PluginCall call) {
        try {
            JSONArray one = new JSONArray();
            JSONObject o = new JSONObject();
            o.put("at", System.currentTimeMillis() + 5000); o.put("title", call.getString("title", "تجربة")); o.put("body", call.getString("body", ""));
            o.put("speak", call.getString("speak", "")); o.put("lang", call.getString("lang", "ar")); o.put("type", call.getString("type", "voice"));
            o.put("prayer", call.getString("prayer", "fajr")); o.put("full", call.getBoolean("full", false)); o.put("adhanOn", call.getBoolean("adhanOn", true));
            Context ctx = getContext();
            Intent it = new Intent(ctx, YomiReceiver.class);
            it.putExtra("title", o.getString("title")); it.putExtra("body", o.getString("body")); it.putExtra("speak", o.getString("speak")); it.putExtra("lang", o.getString("lang"));
            it.putExtra("type", o.getString("type")); it.putExtra("prayer", o.getString("prayer")); it.putExtra("full", o.getBoolean("full")); it.putExtra("adhanOn", o.getBoolean("adhanOn")); it.putExtra("nid", 999);
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, o.getLong("at"), PendingIntent.getBroadcast(ctx, 999, it, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        } catch (Exception ignored) { }
        call.resolve();
    }
}
