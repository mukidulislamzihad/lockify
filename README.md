<p align="center">
  <img src="assets/logo.png" width="140" alt="Lockify logo">
</p>

### 📥 [Download the latest APK](https://github.com/mukidulislamzihad/lockify/releases/latest/download/Lockify.apk)

# 🔒 Lockify

A powerful personal app lock for Android, built with Kotlin. Lock any app behind a PIN, pattern or fingerprint, and make it very hard for anyone to switch the lock off.

> ⚠️ Built for **personal use on your own phone**. Do not install it on someone else's device without their knowledge.

## ✨ Features

- 🔐 **PIN, pattern and fingerprint** unlock, with PIN or pattern as the fallback for fingerprint
- ⚡ **Instant detection** of locked apps through an Accessibility Service
- 🛡️ **Tamper protection**
  - Settings, Play Store and Package Installer are always locked, so nobody can reach *Force stop*, *Clear storage* or *Uninstall*
  - Device Admin blocks normal uninstalling
  - If app data is ever wiped, the lock fails closed and keeps those apps blocked until setup is done again
- 🚫 **Brute-force lockout**: after 5 wrong attempts, the waiting time doubles each time (up to 1 hour)
- 🔑 **Recovery key** shown once at setup, in case you forget your PIN or pattern
- 📵 **Screenshots and recents preview blocked** on the lock screen and inside the app
- 🧂 Credentials are never stored in plain text: PBKDF2 hashes kept in encrypted storage
- 🎨 Minimal UI: an app list with a toggle for each app

## 🔓 Enabling the Accessibility Service (important)

Android blocks Accessibility for apps installed outside the Play Store ("Restricted setting"). Follow these steps once:

1. 🛡️ **Turn off Play Protect first**
   Play Store > tap your profile picture > **Play Protect** > ⚙️ Settings > turn off **Scan apps with Play Protect**.
2. 📥 **Install the APK** and open Lockify.
3. ♿ Go to **Settings > Accessibility > Installed apps > Lockify** and tap it once. A "Restricted setting" message appears, so tap **OK**.
4. ⚙️ Open **App info** for Lockify (long-press the app icon > ⓘ, or Settings > Apps > Lockify).
5. ⋮ Tap the **three dots** at the top right, then tap **Allow restricted settings** and confirm.
6. ✅ Go back to **Settings > Accessibility > Lockify** and turn it **On**.
7. 🔁 Return to Lockify. The first row should now show ✅.

> 💡 If the three-dot menu does not appear, repeat step 3 first, then try again.
> After everything is set up, you can turn **Play Protect** back on.


## 🚀 Setup

1. Install the APK and open **Lockify**.
2. Choose a PIN or pattern and **write down the recovery key**. It is shown only once.
3. On the main screen, turn every ❌ into ✅:
   - ♿ Accessibility Service (the core lock engine)
   - 🪟 Display over other apps
   - 🧷 Device Admin (uninstall protection)
   - 🔋 Battery optimization exemption
4. Switch on the apps you want to lock.

> On Android 13+, a sideloaded app may show "Restricted setting" when you enable Accessibility.
> Go to **Settings > Apps > Lockify > ⋮ > Allow restricted settings**, then try again.

## 🗑️ Uninstalling Lockify yourself

Open the app, unlock it and tap **"Pause protection for 10 minutes"**. During that time Settings, Play Store and Device Admin protection are off, so you can uninstall the app. Protection turns back on after 10 minutes.

## 📦 Building the APK

The APK is built automatically by **GitHub Actions**.

1. Push to this repository, or run the workflow manually from the **Actions** tab.
2. When the run turns green ✅, open it and download **Lockify-APK** from the *Artifacts* section.
3. Unzip it to get `app-debug.apk` and install it on your phone.

You can also open the project in Android Studio and press **Run**.

## ⚠️ Limitations

- 🔧 Safe Mode, ADB from a computer and a Factory Reset cannot be blocked by any normal app.
- 📱 Some phones (Xiaomi, Oppo, Vivo, Samsung) kill background services. Keep **Autostart** on and set battery to **No restrictions**.
- 🔁 After you unlock an app, leaving it (even for the home screen) locks it again.
- 🏪 Publishing on Google Play needs an Accessibility permission declaration. This project is meant for personal use.

## 🧱 Tech

Kotlin · AndroidX · BiometricPrompt · EncryptedSharedPreferences · AccessibilityService · Device Admin



## 📦 Download & Build

**One-click download:** [Lockify.apk](https://github.com/mukidulislamzihad/lockify/releases/latest/download/Lockify.apk)

The APK is rebuilt automatically by **GitHub Actions** on every push and published to the
[Releases](https://github.com/mukidulislamzihad/lockify/releases/latest) page. You can also
open the project in Android Studio and press **Run**.
