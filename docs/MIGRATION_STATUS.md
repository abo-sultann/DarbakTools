# Darbak Platform V1 — Migration Status

آخر تحديث: 2026-09-08

هذا الملف هو سجل الاعتماد الهندسي لتطبيقات شاشة السيارة. لا تُعتبر أي هجرة مكتملة بمجرد وجود الكود على فرع؛ الحالة `merged` تعني أن بوابة التحقق المناسبة نجحت ثم دُمج الفرع في `main`.

| التطبيق | الحالة | بوابة التحقق | ملاحظات المنصة |
|---|---|---|---|
| DarbakTools | reference-implementation | Lint + build | موطن Darbak Core وDesign Tokens وRegistry وQA Standard |
| Launcher 2026 | merged | Android 7 startup/lifecycle/storage/widgets/maintenance tests + signed release APK | Settings UI V1 + هوية دربك + التشخيص المخفي، مع إعادة استخدام أنظمة Launcher الأصلية |
| DarbakMaintenance | merged | Android 7.1 maintenance tests + signed release APK | Crash/health runtime + startup update guard + hidden diagnostics؛ ألوان الحالة الصحية محفوظة |
| Darbak Kids TV | merged | release compile + Android lint + APK signature verification | أدوات المنصة داخل منطقة الأب فقط؛ شاشة الطفل لم تتغير |
| DarbakAdhkar | merged | debug build + Android lint | نصوص صحيح مسلم ومنطق الورد والعداد لم تُمس؛ تشخيص محلي مخفي |
| DarbakMaps | merged-map-safe | clean + lint + unit tests + debug APK | Runtime خفيف فقط عند process startup؛ لا Mapsforge/GPS/network/license في Application |
| Laqqinni | merged | Quran asset 114/6236 + Robolectric API25 + screenshot API35 + unit tests + build + lint | اختبارات Robolectric متوافقة مع Java 17 وشاشة السيارة؛ لا تغيير في المصحف أو الحفظ |
| DarbAlSout2 | merged | Android 7.1/API25 emulator 1024×600 / 1GB + startup + dynamic version check + hidden diagnostics long-press + sync error paths | Crash runtime قبل DB/Auth/Drive؛ updater المتخصص SHA-256/package/version/API/signature محفوظ؛ QA run 34186768519 نجح ثم دُمج PR #2 إلى main بالـcommit eb6d481c8a7efb9957021bf63b67cd0f9d871bb6 |

## قواعد الإغلاق

1. لا دمج إلى `main` إذا فشل Build أو Lint أو اختبار أساسي خاص بالتطبيق.
2. لا نستبدل نظامًا متخصصًا ومستقرًا بمكوّن Core عام لمجرد التوحيد.
3. لا تُحمّل خرائط أو GPS أو شبكة أو قواعد بيانات ثقيلة من Darbak Platform أثناء بدء العملية.
4. التشخيص محلي ولا يرسل telemetry خارج الجهاز.
5. الهوية الرسمية: `دربك — تصميم وتطوير — أبوسلطان`.
6. الهدف التشغيلي: Android 7.1 / API 25، شاشة 1024×600 أفقية، عتاد ضعيف.
