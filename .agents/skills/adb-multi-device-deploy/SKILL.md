---
name: adb-multi-device-deploy
description: Pair, connect, and deploy XStreaming APKs via ADB Wireless Debugging or USB across Android phones, tablets, and Android TV devices, and configure VPN Split Tunneling for local Xbox Remote Play.
---

# Multi-Device ADB Deployment Skill (`adb-multi-device-deploy`)

Google Advanced Protection ("Account Protection") blocks on-device APK sideloading (`INSTALL_FAILED_ADVANCED_PROTECTION`), but permits `adb install` from a trusted computer. Use this skill to connect and deploy `app-prod-release.apk` to phones, tablets, and Android TV devices over the local network.

## 1. Discover Connected or mDNS ADB Services

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices -l
& $adb mdns services
```

Always pass `-s <serial_or_ip:port>` when installing to multiple devices.

---

## 2. Connecting to Android Phones & Tablets via Wireless Debugging

1. On the phone or tablet, open **Settings -> System -> Developer options -> Wireless debugging** and toggle it **On**.
2. **If pairing for the first time:**
   - Tap **Pair device with pairing code** to display the 6-digit code and pairing port.
   - Run:
     ```powershell
     & $adb pair <device_ip>:<pairing_port> <6_digit_code>
     ```
3. **Connect to the Wireless Debugging port:**
   - Look at the main **Wireless debugging** screen on the device for the `IP address & Port` (or check `& $adb mdns services`), then run:
     ```powershell
     & $adb connect <device_ip>:<connect_port>
     ```
4. **Suppress Play Protect "Unknown to Google Play" Prompt on ADB Installs (One-Time per Device):**
   ```powershell
   & $adb -s <device_ip>:<connect_port> shell settings put global verifier_verify_adb_installs 0
   ```
5. **Install or Upgrade XStreaming (Single-Session Rule for Sandboxed Shells):**
   - In sandboxed command runners where background child processes (like the ADB server daemon) are terminated at the end of each shell invocation, always run `adb connect`, `adb install -r`, and app relaunch **within a single command invocation**:
   ```powershell
   $apk = "android\app\build\outputs\apk\prod\release\app-prod-release.apk"
   adb connect <tablet_ip>:<connect_port>
   adb connect <phone_ip>:<connect_port>
   adb -s <tablet_ip>:<connect_port> install -r $apk
   adb -s <phone_ip>:<connect_port> install -r $apk
   adb -s <tablet_ip>:<connect_port> shell am force-stop com.xstreaming
   adb -s <tablet_ip>:<connect_port> shell monkey -p com.xstreaming -c android.intent.category.LAUNCHER 1
   adb -s <phone_ip>:<connect_port> shell am force-stop com.xstreaming
   adb -s <phone_ip>:<connect_port> shell monkey -p com.xstreaming -c android.intent.category.LAUNCHER 1
   ```

---

## 3. Connecting to Android TV / Google TV Streamer

1. Enable **Settings -> System -> Developer options -> USB debugging** (or **Wireless debugging** over Wi-Fi).
2. Connect on port `5555` (or the Wireless Debugging port):
   ```powershell
   & $adb connect <tv_ip>:5555
   & $adb -s <tv_ip>:5555 install -r "android\app\build\outputs\apk\prod\release\app-prod-release.apk"
   ```
   `XStreaming` includes `<category android:name="android.intent.category.LEANBACK_LAUNCHER" />` in `AndroidManifest.xml`, so it appears directly in the Android TV launcher row.

---

## 4. Local Network & VPN Tips for Xbox Remote Play (`xHome`)

If a VPN is active on the Android client during local Remote Play:
1. Enable **Local network sharing** (LAN bypass) or exclude **XStreaming** (`com.xstreaming`) via **Split Tunneling** so Xbox Live signaling and WebRTC UDP media use the direct local network path.
2. Keep **IPv6** disabled (`ipv6: false`) in XStreaming's `xHome` settings so `expandTeredoCandidates()` prioritizes direct local RFC1918 IPv4 LAN `host` candidates ahead of Teredo (`2001:0000:...`).
