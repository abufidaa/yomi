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
perms = ['REQUEST_INSTALL_PACKAGES', 'USE_FULL_SCREEN_INTENT', 'ACTIVITY_RECOGNITION', 'POST_NOTIFICATIONS', 'SCHEDULE_EXACT_ALARM', 'USE_EXACT_ALARM', 'WAKE_LOCK', 'VIBRATE', 'RECEIVE_BOOT_COMPLETED',
         'ACCESS_FINE_LOCATION', 'ACCESS_COARSE_LOCATION', 'RECORD_AUDIO', 'MODIFY_AUDIO_SETTINGS']
add = ''.join('    <uses-permission android:name="android.permission.%s" />\n' % p for p in perms if p not in x)
x = x.replace('<application', add + '    <queries><intent><action android:name="android.speech.RecognitionService" /></intent></queries>\n    <application', 1)
extra = '''        <receiver android:name=".YomiReceiver" android:exported="false" />
        <activity android:name=".AdhanActivity" android:exported="false" android:showWhenLocked="true" android:turnScreenOn="true" android:launchMode="singleTop" android:excludeFromRecents="true" android:theme="@style/AppTheme.NoActionBar" />
        <receiver android:name=".YomiWidget" android:exported="true" android:label="يومي: الصلاة الجاية (كبير)">
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
exec(open('../android-src/widgets.py', encoding='utf8').read())
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
open(R + '/xml/file_paths.xml', 'w', encoding='utf8').write('<?xml version="1.0" encoding="utf-8"?>\n<paths xmlns:android="http://schemas.android.com/apk/res/android">\n    <external-path name="my_images" path="." />\n    <cache-path name="my_cache_images" path="." />\n</paths>')
xm = open(m, encoding='utf8').read()
if 'FileProvider' not in xm:
    xm = xm.replace('</application>', '        <provider android:name="androidx.core.content.FileProvider" android:authorities="${applicationId}.fileprovider" android:exported="false" android:grantUriPermissions="true">\n            <meta-data android:name="android.support.FILE_PROVIDER_PATHS" android:resource="@xml/file_paths" />\n        </provider>\n    </application>', 1)
    open(m, 'w', encoding='utf8').write(xm)
print('patched android project')
