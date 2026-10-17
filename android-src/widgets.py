# بيتنفذ من جوه patch.py: ويدجتات «يومي» الخمسة، وألوانها، واختصارات الأيقونة
BGS = {'navy': ('#1F4378', '#0B152F', '#B08A3E'), 'emerald': ('#1E7A60', '#0D3B2E', '#C9A24A'), 'light': ('#FFFDF7', '#F3E9D2', '#D8C08A'), 'glass': ('#CC0B152F', '#990B152F', '#66DCB96A')}
for k, (c1, c2, st) in BGS.items():
    open(R + '/drawable/yw_bg_' + k + '.xml', 'w', encoding='utf8').write('<?xml version="1.0" encoding="utf-8"?>\n<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">\n    <gradient android:startColor="' + c1 + '" android:endColor="' + c2 + '" android:angle="270" />\n    <corners android:radius="28dp" />\n    <stroke android:width="1.5dp" android:color="' + st + '" />\n</shape>')
open(R + '/drawable/yw_btn.xml', 'w', encoding='utf8').write('<?xml version="1.0" encoding="utf-8"?>\n<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle"><solid android:color="#33DCB96A" /><corners android:radius="22dp" /><stroke android:width="1dp" android:color="#DCB96A" /></shape>')
NS = 'xmlns:android="http://schemas.android.com/apk/res/android"'
def tv(i, size, extra=''):
    return '<TextView android:id="@+id/' + i + '" android:layout_width="wrap_content" android:layout_height="wrap_content" android:textSize="' + size + '" ' + extra + ' />'
def root(extra):
    return '<LinearLayout ' + NS + ' android:id="@+id/wRoot" android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical" android:background="@drawable/yw_bg_navy" ' + extra + '>'
CH = '<Chronometer android:id="@+id/wCd" android:layout_width="wrap_content" android:layout_height="wrap_content" android:textSize="%s" android:textStyle="bold" />'
L = {
 'yw_prayer': root('android:padding="14dp"')
   + '<LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="horizontal" android:layoutDirection="rtl">'
   + '<TextView android:id="@+id/wBrand" android:layout_width="0dp" android:layout_weight="1" android:layout_height="wrap_content" android:text="✦ يومي" android:textSize="13sp" android:textStyle="bold" />' + tv('wHijri', '11sp') + '</LinearLayout>'
   + '<LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="horizontal" android:gravity="center_vertical" android:layoutDirection="rtl" android:layout_marginTop="4dp">'
   + '<LinearLayout android:layout_width="0dp" android:layout_weight="1" android:layout_height="wrap_content" android:orientation="vertical">' + tv('wLbl', '11sp', 'android:text="الصلاة الجاية"') + tv('wName', '30sp', 'android:textStyle="bold" android:text="يومي"') + '</LinearLayout>'
   + (CH % '26sp') + '</LinearLayout>'
   + tv('wTime', '12sp', 'android:layout_marginTop="2dp"')
   + '<LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="horizontal" android:layout_marginTop="8dp" android:layoutDirection="rtl">'
   + ''.join('<TextView android:id="@+id/p' + str(i) + '" android:layout_width="0dp" android:layout_weight="1" android:layout_height="wrap_content" android:gravity="center" android:textSize="11sp" android:lineSpacingExtra="2dp" />' for i in range(1, 6))
   + '</LinearLayout></LinearLayout>',
 'yw_small': root('android:gravity="center" android:padding="10dp"') + tv('wName', '16sp', 'android:textStyle="bold"') + (CH % '24sp') + tv('wTime', '11sp') + '</LinearLayout>',
 'yw_day': root('android:padding="14dp" android:layoutDirection="rtl"')
   + '<LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="horizontal" android:gravity="center_vertical"><TextView android:id="@+id/wTitle" android:layout_width="0dp" android:layout_weight="1" android:layout_height="wrap_content" android:text="☀️ يومك" android:textSize="13sp" android:textStyle="bold" />' + tv('wScore', '26sp', 'android:textStyle="bold"') + '</LinearLayout>'
   + '<ProgressBar android:id="@+id/wBar" style="?android:attr/progressBarStyleHorizontal" android:layout_width="match_parent" android:layout_height="8dp" android:max="100" android:layout_marginTop="4dp" android:progressTint="#DCB96A" />'
   + tv('wNext', '14sp', 'android:layout_marginTop="8dp" android:textStyle="bold" android:maxLines="1" android:ellipsize="end"') + tv('wPages', '12sp', 'android:layout_marginTop="2dp"') + tv('wMit', '12sp', 'android:maxLines="2" android:ellipsize="end"') + '</LinearLayout>',
 'yw_ayah': root('android:gravity="center" android:padding="16dp"') + tv('wText', '18sp', 'android:gravity="center" android:lineSpacingExtra="6dp" android:maxLines="5" android:ellipsize="end"') + tv('wSrc', '11sp', 'android:layout_marginTop="6dp"') + '</LinearLayout>',
 'yw_tasbeeh': root('android:gravity="center" android:padding="10dp"') + tv('wZ', '13sp', 'android:textStyle="bold" android:padding="4dp"') + tv('wCount', '34sp', 'android:textStyle="bold"')
   + '<TextView android:id="@+id/wTap" android:layout_width="match_parent" android:layout_height="44dp" android:gravity="center" android:text="📿 سبّح" android:textSize="16sp" android:textStyle="bold" android:background="@drawable/yw_btn" android:layout_marginTop="4dp" />'
   + tv('wReset', '11sp', 'android:text="صفّر" android:padding="4dp"') + '</LinearLayout>'}
for k, xml in L.items():
    open(R + '/layout/' + k + '.xml', 'w', encoding='utf8').write('<?xml version="1.0" encoding="utf-8"?>\n' + xml)
INFO = {'yomi_widget_info': ('yw_prayer', 250, 110), 'yw_small_info': ('yw_small', 110, 110), 'yw_day_info': ('yw_day', 250, 110), 'yw_ayah_info': ('yw_ayah', 250, 110), 'yw_tasbeeh_info': ('yw_tasbeeh', 110, 140)}
for k, (lay, w, h) in INFO.items():
    open(R + '/xml/' + k + '.xml', 'w', encoding='utf8').write('<?xml version="1.0" encoding="utf-8"?>\n<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android" android:minWidth="' + str(w) + 'dp" android:minHeight="' + str(h) + 'dp" android:updatePeriodMillis="1800000" android:initialLayout="@layout/' + lay + '" android:resizeMode="horizontal|vertical" android:widgetCategory="home_screen" />')
SC = ''.join('<shortcut android:shortcutId="' + sid + '" android:enabled="true" android:icon="@mipmap/ic_launcher" android:shortcutShortLabel="@string/sc_' + sid + '"><intent android:action="android.intent.action.VIEW" android:targetPackage="net.tantawy.yomi" android:targetClass="net.tantawy.yomi.MainActivity"><extra android:name="go" android:value="' + sid + '" /></intent></shortcut>' for sid in ('z', 'm', 'a', 'q'))
open(R + '/xml/shortcuts.xml', 'w', encoding='utf8').write('<?xml version="1.0" encoding="utf-8"?>\n<shortcuts xmlns:android="http://schemas.android.com/apk/res/android">' + SC + '</shortcuts>')
y = open(s, encoding='utf8').read()
y = y.replace('</resources>', '    <string name="sc_z">السبحة</string>\n    <string name="sc_m">المصحف</string>\n    <string name="sc_a">الأذكار</string>\n    <string name="sc_q">القبلة</string>\n</resources>')
open(s, 'w', encoding='utf8').write(y)
x = open(m, encoding='utf8').read()
x = x.replace('</activity>', '    <meta-data android:name="android.app.shortcuts" android:resource="@xml/shortcuts" />\n        </activity>', 1)
REC = '        <receiver android:name=".{c}" android:exported="true" android:label="{l}">\n            <intent-filter><action android:name="android.appwidget.action.APPWIDGET_UPDATE" /></intent-filter>\n            <meta-data android:name="android.appwidget.provider" android:resource="@xml/{i}" />\n        </receiver>\n'
wid = ''.join(REC.format(c=c, l=l, i=i) for c, l, i in (('WidgetSmall', 'يومي: صغير', 'yw_small_info'), ('WidgetDay', 'يومي: يومك', 'yw_day_info'), ('WidgetAyah', 'يومي: آية وذكر', 'yw_ayah_info'), ('WidgetTasbeeh', 'يومي: السبحة', 'yw_tasbeeh_info')))
wid += '        <receiver android:name=".WidgetAct" android:exported="false" />\n'
x = x.replace('</application>', wid + '    </application>', 1)
if 'ACCESS_NOTIFICATION_POLICY' not in x:
    x = x.replace('<application', '<uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" />\n    <application', 1)
open(m, 'w', encoding='utf8').write(x)
