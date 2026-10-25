# JARVIS Cyberpunk Android

Kotlin + Jetpack Compose asosidagi Android loyiha. Binafsha neon/cyberpunk dizayn.

## Haqiqatan ishlaydigan funksiyalar
- Cyberpunk interfeys va animatsiyali AI orb
- Mahalliy salomlashish, vaqt/sana, yordam buyruqlari
- Eslatmalarni qurilmada saqlash
- Chat tarixini qurilmada saqlash va tozalash
- Android Speech Recognizer orqali ovozdan matn (qurilma xizmatiga bog‘liq)
- Android Text-to-Speech orqali javobni ovozda o‘qish
- YouTube/Google havolalarini ochish
- Internet qidiruvini brauzerda ochish
- O‘zbekcha, inglizcha, ruscha til tanlash

## Hozircha yo‘q
Umumiy savollarga javob beruvchi bulutli AI, doimiy uyg‘otish so‘zi, avtomatik ijtimoiy tarmoq postlari, rasm/fayl AI tahlili va vaqtga belgilangan eslatma bildirishnomalari hali ulanmagan. Soxta tugma qo‘shmaslik uchun bularni mavjud deb ko‘rsatmaydi. Keyin haqiqiy API/ruxsatlar bilan qo‘shish mumkin.

## APK olish — GitHub Actions
1. GitHub’da yangi repository yarating.
2. ZIP ichidagi fayllarni repository root’iga joylang: `settings.gradle.kts`, `build.gradle.kts`, `app/`, `.github/` to‘g‘ridan-to‘g‘ri root’da bo‘lsin.
3. `Actions` bo‘limiga kiring. `Build JARVIS Cyberpunk APK` ishga tushadi (yoki `Run workflow` bosing).
4. Yashil build tugagach, artifact `JARVIS-Cyberpunk-debug-APK` ni yuklab oling. Artifact ZIP ichidan `.apk` faylni oling.

## Kompyuterda build
JDK 17 va Android SDK Platform 35 kerak. Loyiha root’ida `gradle :app:assembleDebug` buyrug‘i. APK: `app/build/outputs/apk/debug/app-debug.apk`.

Replit Android APK yig‘ishga har doim ham mos emas; GitHub Actions bu loyiha uchun tayyorlangan build usuli.
