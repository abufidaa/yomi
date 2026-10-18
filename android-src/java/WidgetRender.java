package net.tantawy.yomi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.SystemClock;
import android.widget.RemoteViews;
import androidx.core.app.NotificationCompat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/* بيرسم كل ويدجتات «يومي» بالبيانات اللي التطبيق بيبعتها */
public class WidgetRender {
    static final String[] DEF_Z = {"سبحان الله", "الحمد لله", "الله أكبر", "لا إله إلا الله", "أستغفر الله", "اللهم صلّ على محمد"};

    static SharedPreferences sp(Context c) { return c.getSharedPreferences(YomiAlarmsPlugin.PREF, Context.MODE_PRIVATE); }
    static JSONObject data(Context c) { try { return new JSONObject(sp(c).getString("wdata", "{}")); } catch (Exception e) { return new JSONObject(); } }
    static String theme(Context c) { return data(c).optString("theme", "navy"); }
    static boolean light(Context c) { return "light".equals(theme(c)); }
    static int bg(Context c) {
        switch (theme(c)) {
            case "emerald": return R.drawable.yw_bg_emerald;
            case "light": return R.drawable.yw_bg_light;
            case "glass": return R.drawable.yw_bg_glass;
            default: return R.drawable.yw_bg_navy;
        }
    }
    static int col(Context c, String dark, String lite) { return Color.parseColor(light(c) ? lite : dark); }
    static int gold(Context c) { return col(c, "#F3E2BC", "#8A6A22"); }
    static String fmt(long t) { return new SimpleDateFormat("h:mm a", new Locale("ar", "EG")).format(new Date(t)); }
    static String ar(long n) { return String.format(new Locale("ar", "EG"), "%d", n); }

    static PendingIntent open(Context c, int rc) {
        Intent i = new Intent(c, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(c, rc, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    static PendingIntent act(Context c, String a, int rc) {
        Intent i = new Intent(c, WidgetAct.class);
        i.setAction(a);
        return PendingIntent.getBroadcast(c, rc, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    static JSONObject[] next2(Context c) {
        JSONArray a = data(c).optJSONArray("prayers");
        JSONObject n = null, m = null;
        long now = System.currentTimeMillis();
        if (a != null) for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null && o.optLong("at") > now) { if (n == null) n = o; else { m = o; break; } }
        }
        return new JSONObject[]{n, m};
    }
    static void chrono(RemoteViews v, int id, long at) {
        v.setChronometer(id, SystemClock.elapsedRealtime() + (at - System.currentTimeMillis()), null, true);
        if (Build.VERSION.SDK_INT >= 24) v.setChronometerCountDown(id, true);
    }

    static void updateAll(Context c) {
        upd(c, YomiWidget.class, "prayer");
        upd(c, WidgetSmall.class, "small");
        upd(c, WidgetDay.class, "day");
        upd(c, WidgetAyah.class, "ayah");
        upd(c, WidgetTasbeeh.class, "tasbeeh");
        ongoing(c);
    }
    static void upd(Context c, Class<?> cls, String type) {
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            int[] ids = m.getAppWidgetIds(new ComponentName(c, cls));
            if (ids.length == 0) return;
            m.updateAppWidget(ids, build(c, type));
        } catch (Exception ignored) { }
    }

    static RemoteViews build(Context c, String type) {
        String pkg = c.getPackageName();
        JSONObject d = data(c);
        int txt = col(c, "#FFFFFF", "#1B2333"), sub = col(c, "#A9B4C7", "#5B6578"), gd = gold(c);
        if (type.equals("prayer")) {
            RemoteViews v = new RemoteViews(pkg, R.layout.yw_prayer);
            v.setInt(R.id.wRoot, "setBackgroundResource", bg(c));
            JSONObject[] nx = next2(c);
            v.setTextViewText(R.id.wHijri, d.optString("hijri", ""));
            v.setTextColor(R.id.wBrand, gd); v.setTextColor(R.id.wHijri, sub); v.setTextColor(R.id.wLbl, sub); v.setTextColor(R.id.wTime, txt);
            v.setTextColor(R.id.wName, gd); v.setTextColor(R.id.wCd, gd);
            if (nx[0] != null) {
                long at = nx[0].optLong("at");
                v.setTextViewText(R.id.wName, nx[0].optString("n"));
                v.setTextViewText(R.id.wTime, "الساعة " + fmt(at) + (nx[1] != null ? "  ·  بعدها " + nx[1].optString("n") + " " + fmt(nx[1].optLong("at")) : ""));
                chrono(v, R.id.wCd, at);
            } else { v.setTextViewText(R.id.wName, "افتح يومي"); v.setTextViewText(R.id.wTime, "عشان المواقيت تتحدّث"); }
            int[] cells = {R.id.p1, R.id.p2, R.id.p3, R.id.p4, R.id.p5};
            JSONArray today = d.optJSONArray("today");
            String nextName = nx[0] != null ? nx[0].optString("n") : "";
            for (int i = 0; i < 5; i++) {
                JSONObject o = today != null ? today.optJSONObject(i) : null;
                if (o == null) { v.setTextViewText(cells[i], ""); continue; }
                v.setTextViewText(cells[i], o.optString("n") + "\n" + fmt(o.optLong("at")));
                boolean isNext = o.optString("n").equals(nextName) && o.optLong("at") == (nx[0] != null ? nx[0].optLong("at") : -1);
                v.setTextColor(cells[i], isNext ? gd : sub);
            }
            v.setOnClickPendingIntent(R.id.wRoot, open(c, 70));
            return v;
        }
        if (type.equals("small")) {
            RemoteViews v = new RemoteViews(pkg, R.layout.yw_small);
            v.setInt(R.id.wRoot, "setBackgroundResource", bg(c));
            JSONObject[] nx = next2(c);
            v.setTextColor(R.id.wName, gd); v.setTextColor(R.id.wCd, txt); v.setTextColor(R.id.wTime, sub);
            if (nx[0] != null) { v.setTextViewText(R.id.wName, "🕌 " + nx[0].optString("n")); v.setTextViewText(R.id.wTime, fmt(nx[0].optLong("at"))); chrono(v, R.id.wCd, nx[0].optLong("at")); }
            else v.setTextViewText(R.id.wName, "يومي");
            v.setOnClickPendingIntent(R.id.wRoot, open(c, 71));
            return v;
        }
        if (type.equals("day")) {
            RemoteViews v = new RemoteViews(pkg, R.layout.yw_day);
            v.setInt(R.id.wRoot, "setBackgroundResource", bg(c));
            int sc = d.optInt("score", 0);
            v.setTextViewText(R.id.wScore, ar(sc) + "٪");
            v.setProgressBar(R.id.wBar, 100, sc, false);
            v.setTextViewText(R.id.wNext, d.optString("nextBlock", ""));
            v.setTextViewText(R.id.wPages, d.optString("pages", ""));
            v.setTextViewText(R.id.wMit, d.optString("mit", ""));
            v.setTextColor(R.id.wTitle, sub); v.setTextColor(R.id.wScore, gd); v.setTextColor(R.id.wNext, txt); v.setTextColor(R.id.wPages, sub); v.setTextColor(R.id.wMit, sub);
            v.setOnClickPendingIntent(R.id.wRoot, open(c, 72));
            return v;
        }
        if (type.equals("ayah")) {
            RemoteViews v = new RemoteViews(pkg, R.layout.yw_ayah);
            v.setInt(R.id.wRoot, "setBackgroundResource", bg(c));
            JSONArray a = d.optJSONArray("ayat");
            int i = sp(c).getInt("ayah_i", 0);
            String t = "﴿أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ﴾", s = "الرعد: ٢٨";
            if (a != null && a.length() > 0) { JSONObject o = a.optJSONObject(Math.abs(i) % a.length()); if (o != null) { t = o.optString("t"); s = o.optString("s"); } }
            v.setTextViewText(R.id.wText, t); v.setTextViewText(R.id.wSrc, s + "  ·  المس للتالي");
            v.setTextColor(R.id.wText, txt); v.setTextColor(R.id.wSrc, gd);
            v.setOnClickPendingIntent(R.id.wRoot, act(c, "net.tantawy.yomi.AYAH_NEXT", 80));
            return v;
        }
        RemoteViews v = new RemoteViews(pkg, R.layout.yw_tasbeeh);
        v.setInt(R.id.wRoot, "setBackgroundResource", bg(c));
        String today = YomiAlarmsPlugin.today();
        if (!today.equals(sp(c).getString("tasb_day", ""))) sp(c).edit().putString("tasb_day", today).putInt("tasb_n", 0).apply();
        JSONArray z = d.optJSONArray("zikr");
        int zi = sp(c).getInt("tasb_i", 0);
        String name = (z != null && z.length() > 0) ? z.optString(Math.abs(zi) % z.length()) : DEF_Z[Math.abs(zi) % DEF_Z.length];
        v.setTextViewText(R.id.wZ, name + "  ⇄");
        v.setTextViewText(R.id.wCount, ar(sp(c).getInt("tasb_n", 0)));
        v.setTextColor(R.id.wZ, sub); v.setTextColor(R.id.wCount, gd); v.setTextColor(R.id.wTap, txt); v.setTextColor(R.id.wReset, sub);
        v.setOnClickPendingIntent(R.id.wTap, act(c, "net.tantawy.yomi.TASB_INC", 81));
        v.setOnClickPendingIntent(R.id.wZ, act(c, "net.tantawy.yomi.TASB_NEXT", 82));
        v.setOnClickPendingIntent(R.id.wReset, act(c, "net.tantawy.yomi.TASB_RESET", 83));
        return v;
    }

    /* إشعار ثابت فيه الصلاة الجاية وعداد تنازلي */
    static void ongoing(Context c) {
        try {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            JSONObject d = data(c);
            JSONObject[] nx = next2(c);
            if (!d.optBoolean("ongoing", false) || nx[0] == null) { nm.cancel(5); return; }
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel ch = new NotificationChannel("yomi_ongoing", "الصلاة الجاية (ثابت)", NotificationManager.IMPORTANCE_LOW);
                ch.setShowBadge(false); nm.createNotificationChannel(ch);
            }
            long at = nx[0].optLong("at");
            NotificationCompat.Builder b = new NotificationCompat.Builder(c, "yomi_ongoing")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("🕌 الصلاة الجاية: " + nx[0].optString("n"))
                    .setContentText("الساعة " + fmt(at) + "  ·  " + d.optString("hijri", ""))
                    .setStyle(new NotificationCompat.BigTextStyle().bigText("الساعة " + fmt(at) + "  ·  " + d.optString("hijri", "")
                            + (d.optString("dhikr", "").isEmpty() ? "" : "\n📿 " + d.optString("dhikr", ""))
                            + (d.optString("mit", "").isEmpty() ? "" : "\n" + d.optString("mit", ""))))
                    .setOngoing(true).setSilent(true).setOnlyAlertOnce(true).setShowWhen(true).setWhen(at).setUsesChronometer(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW).setContentIntent(open(c, 73));
            if (Build.VERSION.SDK_INT >= 24) b.setChronometerCountDown(true);
            nm.notify(5, b.build());
        } catch (Exception ignored) { }
    }
}
