package net.tantawy.yomi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(YomiAlarmsPlugin.class);
        super.onCreate(savedInstanceState);
        saveGo(getIntent());
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECORD_AUDIO}, 1);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        saveGo(intent);
        try { getBridge().getWebView().evaluateJavascript("window.__yomiGo&&window.__yomiGo()", null); } catch (Exception ignored) { }
    }

    void saveGo(Intent in) {
        if (in == null) return;
        String g = in.getStringExtra("go");
        if (g != null && !g.isEmpty()) getSharedPreferences(YomiAlarmsPlugin.PREF, MODE_PRIVATE).edit().putString("go", g).apply();
    }
}
