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
perms = ['POST_NOTIFICATIONS', 'SCHEDULE_EXACT_ALARM', 'USE_EXACT_ALARM', 'WAKE_LOCK', 'VIBRATE', 'RECEIVE_BOOT_COMPLETED',
         'ACCESS_FINE_LOCATION', 'ACCESS_COARSE_LOCATION', 'RECORD_AUDIO', 'MODIFY_AUDIO_SETTINGS']
add = ''.join('    <uses-permission android:name="android.permission.%s" />\n' % p for p in perms if p not in x)
x = x.replace('<application', add + '\n    <application', 1)
x = x.replace('</application>', '        <receiver android:name=".YomiReceiver" android:exported="false" />\n    </application>', 1)
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
print('patched android project')
