"""بيظبط مشروع أندرويد اللي Capacitor عمله: الكود، والأذونات، والأيقونة، والاسم، والتوقيع"""
import os, re, shutil
from PIL import Image
A = 'android/app'
J = A + '/src/main/java/net/tantawy/yomi'
os.makedirs(J, exist_ok=True)
for f in os.listdir('../android-src/java'):
    shutil.copy('../android-src/java/' + f, J + '/' + f)
m = A + '/src/main/AndroidManifest.xml'
x = open(m, encoding='utf8').read()
perms = ['USE_FULL_SCREEN_INTENT', 'ACTIVITY_RECOGNITION', 'POST_NOTIFICATIONS', 'SCHEDULE_EXACT_ALARM', 'USE_EXACT_ALARM', 'WAKE_LOCK', 'VIBRATE', 'RECEIVE_BOOT_COMPLETED',
         'ACCESS_FINE_LOCATION', 'ACCESS_COARSE_LOCATION', 'RECORD_AUDIO', 'MODIFY_AUDIO_SETTINGS']
add = ''.join('    <uses-permission android:name="android.permission.%s" />\n' % p for p in perms if p not in x)
x = x.replace('<application', add + '\n    <application', 1)
extra = '''        <receiver android:name=".YomiReceiver" android:exported="false" />
        <activity android:name=".AdhanActivity" android:exported="false" android:showWhenLocked="true" android:turnScreenOn="true" android:launchMode="singleTop" android:excludeFromRecents="true" android:theme="@style/AppTheme.NoActionBar" />
        <receiver android:name=".YomiWidget" android:exported="true" android:label="يومي: الصلاة الجاية">
            <intent-filter><action android:name="android.appwidget.action.APPWIDGET_UPDATE" /></intent-filter>
            <meta-data android:name="android.appwidget.provider" android:resource="@xml/yomi_widget_info" />
        </receiver>
        <receiver android:name=".YomiBoot" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
            </intent-filter>
        </receiver>
'''
x = x.replace('</application>', extra + '    </application>', 1)
open(m, 'w', encoding='utf8').write(x)
s = A + '/src/main/res/values/strings.xml'
y = open(s, encoding='utf8').read()
y = re.sub(r'(<string name="app_name">)[^<]*', r'\1يومي', y)
y = re.sub(r'(<string name="title_activity_main">)[^<]*', r'\1يومي', y)
open(s, 'w', encoding='utf8').write(y)
ic = Image.open('www/icon-512.png').convert('RGBA')
sizes = {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}
for d, px in sizes.items():
    p = A + '/src/main/res/mipmap-' + d
    os.makedirs(p, exist_ok=True)
    for n in ('ic_launcher.png', 'ic_launcher_round.png'):
        ic.resize((px, px), Image.LANCZOS).save(p + '/' + n)
    fg = Image.new('RGBA', (int(px * 2.25), int(px * 2.25)), (255, 255, 255, 255))
    small = ic.resize((int(px * 1.5), int(px * 1.5)), Image.LANCZOS)
    fg.alpha_composite(small, ((fg.width - small.width) // 2, (fg.height - small.height) // 2))
    fg.save(p + '/ic_launcher_foreground.png')
shutil.rmtree(A + '/src/main/res/mipmap-anydpi-v26', ignore_errors=True)
g = A + '/build.gradle'
z = open(g, encoding='utf8').read()
sign = '''
    signingConfigs {
        release {
            storeFile file("../../../android-src/yomi.p12")
            storeType "pkcs12"
            storePassword "yomi-tantawy-2026"
            keyAlias "yomi"
            keyPassword "yomi-tantawy-2026"
        }
    }
'''
z = z.replace('    buildTypes {', sign + '    buildTypes {', 1)
z = re.sub(r'(release \{\s*\n\s*minifyEnabled false)', r'\1\n            signingConfig signingConfigs.release', z, count=1)
open(g, 'w', encoding='utf8').write(z)
R = A + '/src/main/res'
for d in ('layout', 'xml', 'drawable', 'raw'):
    os.makedirs(R + '/' + d, exist_ok=True)
open(R + '/drawable/yomi_widget_bg.xml', 'w', encoding='utf8').write('''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <gradient android:startColor="#1B3A6B" android:endColor="#0B152F" android:angle="270" />
    <corners android:radius="26dp" />
    <stroke android:width="1.5dp" android:color="#B08A3E" />
</shape>''')
open(R + '/layout/yomi_widget.xml', 'w', encoding='utf8').write('''<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" android:id="@+id/wRoot"
    android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical"
    android:gravity="center" android:padding="12dp" android:background="@drawable/yomi_widget_bg">
    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="🕌 الصلاة الجاية" android:textColor="#C9D3E3" android:textSize="12sp" />
    <TextView android:id="@+id/wName" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="يومي" android:textColor="#F3E2BC" android:textSize="28sp" android:textStyle="bold" />
    <TextView android:id="@+id/wTime" android:layout_width="wrap_content" android:layout_height="wrap_content" android:textColor="#FFFFFF" android:textSize="13sp" />
    <Chronometer android:id="@+id/wCd" android:layout_width="wrap_content" android:layout_height="wrap_content" android:textColor="#DCB96A" android:textSize="22sp" android:textStyle="bold" />
    <TextView android:id="@+id/wAfter" android:layout_width="wrap_content" android:layout_height="wrap_content" android:textColor="#9FAABD" android:textSize="11sp" />
</LinearLayout>''')
open(R + '/xml/yomi_widget_info.xml', 'w', encoding='utf8').write('''<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="180dp" android:minHeight="110dp" android:updatePeriodMillis="1800000"
    android:initialLayout="@layout/yomi_widget" android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen" android:description="@string/app_name" />''')
for f in os.listdir('../android-src') if os.path.isdir('../android-src') else []:
    pass
for f in os.listdir('.'):
    if f.startswith('adhan_') and f.endswith('.ogg') and os.path.getsize(f) > 20000:
        shutil.copy(f, R + '/raw/' + f)
# شاشة البداية: كحلي وفيها الشعار
for root, dirs, files in os.walk(R):
    for f in files:
        if f == 'splash.png':
            pth = os.path.join(root, f)
            w, h = Image.open(pth).size
            bg = Image.new('RGBA', (w, h), (11, 21, 47, 255))
            sz = int(min(w, h) * 0.32)
            lg = ic.resize((sz, sz), Image.LANCZOS)
            bg.alpha_composite(lg, ((w - sz) // 2, (h - sz) // 2))
            bg.convert('RGB').save(pth)
st = R + '/values/styles.xml'
y = open(st, encoding='utf8').read()
y = y.replace('<style name="AppTheme.NoActionBar" parent="Theme.AppCompat.DayNight.NoActionBar">', '<style name="AppTheme.NoActionBar" parent="Theme.AppCompat.DayNight.NoActionBar">\n        <item name="android:statusBarColor">#0B152F</item>\n        <item name="android:navigationBarColor">#0B152F</item>', 1)
y = y.replace('<item name="android:background">@drawable/splash</item>', '<item name="android:background">@drawable/splash</item>\n        <item name="android:statusBarColor">#0B152F</item>', 1)
open(st, 'w', encoding='utf8').write(y)
print('patched android project')
