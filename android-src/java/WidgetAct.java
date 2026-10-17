package net.tantawy.yomi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.Build;

/* لمسات الويدجت: السبحة والآية */
public class WidgetAct extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent in) {
        SharedPreferences sp = WidgetRender.sp(c);
        String a = in.getAction() == null ? "" : in.getAction();
        String today = YomiAlarmsPlugin.today();
        if (!today.equals(sp.getString("tasb_day", ""))) sp.edit().putString("tasb_day", today).putInt("tasb_n", 0).apply();
        if (a.endsWith("TASB_INC")) {
            sp.edit().putInt("tasb_n", sp.getInt("tasb_n", 0) + 1).putInt("tasb_total", sp.getInt("tasb_total", 0) + 1).apply();
            try { Vibrator v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE); if (v != null) { if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(18, 120)); else v.vibrate(18); } } catch (Exception ignored) { }
        } else if (a.endsWith("TASB_RESET")) sp.edit().putInt("tasb_n", 0).apply();
        else if (a.endsWith("TASB_NEXT")) sp.edit().putInt("tasb_i", sp.getInt("tasb_i", 0) + 1).apply();
        else if (a.endsWith("AYAH_NEXT")) sp.edit().putInt("ayah_i", sp.getInt("ayah_i", 0) + 1).apply();
        WidgetRender.updateAll(c);
    }
}
