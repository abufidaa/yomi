package net.tantawy.yomi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import org.json.JSONObject;

/* منبهات «يومي»: بتتجدول في أندرويد نفسه، فبتشتغل والتطبيق مقفول */
@CapacitorPlugin(name = "YomiAlarms")
public class YomiAlarmsPlugin extends Plugin {
    static final String PREF = "yomi_alarms";

    private Intent alarmIntent(Context ctx, String title, String body, String speak, String lang, int nid) {
        Intent it = new Intent(ctx, YomiReceiver.class);
        it.putExtra("title", title); it.putExtra("body", body); it.putExtra("speak", speak);
        it.putExtra("lang", lang); it.putExtra("nid", nid);
        return it;
    }

    @PluginMethod
    public void schedule(PluginCall call) {
        try {
            Context ctx = getContext();
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            int old = sp.getInt("n", 0);
            for (int i = 0; i < old; i++) {
                PendingIntent pi = PendingIntent.getBroadcast(ctx, 1000 + i, new Intent(ctx, YomiReceiver.class),
                        PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
                if (pi != null) { am.cancel(pi); pi.cancel(); }
            }
            JSArray arr = call.getArray("alarms");
            boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
            long now = System.currentTimeMillis();
            int n = 0;
            for (int i = 0; i < arr.length() && n < 120; i++) {
                JSONObject o = arr.getJSONObject(i);
                long at = o.getLong("at");
                if (at <= now) continue;
                int nid = 1000 + n;
                Intent it = alarmIntent(ctx, o.optString("title"), o.optString("body"), o.optString("speak"), o.optString("lang", "ar"), nid);
                PendingIntent pi = PendingIntent.getBroadcast(ctx, nid, it, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                n++;
            }
            sp.edit().putInt("n", n).apply();
            JSObject r = new JSObject();
            r.put("scheduled", n);
            r.put("exact", exact);
            call.resolve(r);
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void status(PluginCall call) {
        Context ctx = getContext();
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        JSObject r = new JSObject();
        r.put("exact", Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms());
        r.put("scheduled", ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt("n", 0));
        call.resolve(r);
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
        Context ctx = getContext();
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Intent it = alarmIntent(ctx, call.getString("title", "تجربة"), call.getString("body", ""), call.getString("speak", ""), call.getString("lang", "ar"), 999);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 999, it, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000, pi);
        call.resolve();
    }
}
