package net.tantawy.yomi;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/* ويدجت الشاشة الرئيسية: الصلاة الجاية وعداد تنازلي */
public class YomiWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context ctx, AppWidgetManager m, int[] ids) { updateAll(ctx); }

    static void updateAll(Context ctx) {
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(ctx);
            int[] ids = m.getAppWidgetIds(new ComponentName(ctx, YomiWidget.class));
            if (ids.length == 0) return;
            RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.yomi_widget);
            JSONArray a = new JSONArray(ctx.getSharedPreferences(YomiAlarmsPlugin.PREF, Context.MODE_PRIVATE).getString("wid", "[]"));
            long now = System.currentTimeMillis();
            JSONObject next = null, after = null;
            for (int i = 0; i < a.length(); i++) { JSONObject o = a.getJSONObject(i); if (o.getLong("at") > now) { if (next == null) next = o; else { after = o; break; } } }
            SimpleDateFormat f = new SimpleDateFormat("h:mm a", new Locale("ar", "EG"));
            if (next != null) {
                long at = next.getLong("at");
                v.setTextViewText(R.id.wName, next.getString("n"));
                v.setTextViewText(R.id.wTime, "الساعة " + f.format(new Date(at)));
                v.setChronometer(R.id.wCd, SystemClock.elapsedRealtime() + (at - now), null, true);
                v.setChronometerCountDown(R.id.wCd, true);
                v.setTextViewText(R.id.wAfter, after != null ? "وبعدها " + after.getString("n") + " " + f.format(new Date(after.getLong("at"))) : "");
            } else {
                v.setTextViewText(R.id.wName, "افتح يومي");
                v.setTextViewText(R.id.wTime, "عشان المواقيت تتحدّث");
                v.setTextViewText(R.id.wAfter, "");
            }
            Intent open = new Intent(ctx, MainActivity.class);
            v.setOnClickPendingIntent(R.id.wRoot, PendingIntent.getActivity(ctx, 77, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            m.updateAppWidget(ids, v);
        } catch (Exception ignored) { }
    }
}
