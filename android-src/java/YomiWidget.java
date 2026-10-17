package net.tantawy.yomi;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

/* الويدجت الكبير: الصلاة الجاية + مواقيت اليوم */
public class YomiWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) { WidgetRender.updateAll(c); }
    static void updateAll(Context c) { WidgetRender.updateAll(c); }
}
