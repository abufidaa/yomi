package net.tantawy.yomi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/* بعد ما الموبايل يقفل ويفتح: نرجّع المنبهات */
public class YomiBoot extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent in) {
        YomiAlarmsPlugin.reschedule(ctx);
        YomiWidget.updateAll(ctx);
    }
}
