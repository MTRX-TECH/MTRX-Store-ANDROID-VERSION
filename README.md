# MTRX Store (Android)
**Custom Independent Application Marketplace for the MTRX Tech Ecosystem**  
*Developed by MTRX Tech (Marapathran Groups) • Version 1.0.0*

---

## 🌟 Overview

**MTRX Store** is a native, modern Android application marketplace that enables users to browse, install, update, and manage Android applications distributed directly outside Google Play.

Engineered with **Kotlin**, **Jetpack Compose (Material 3)**, and **Clean MVVM Architecture**, MTRX Store features a serverless GitHub-backed catalog system, real-time cryptographic SHA-256 integrity verification, native Android `PackageInstaller` session management, and background update checks via `WorkManager`.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose (Material 3) with full edge-to-edge rendering
- **Architecture**: MVVM with unidirectional data flow (UDF), Repository pattern & Clean Architecture
- **Dependency Injection**: Hilt (Dagger)
- **Networking**: Retrofit 2 & OkHttp 4 (HTTPS only, streaming byte downloader)
- **Local Database**: Room 2.6 with Coroutines & Flow
- **Image Loading**: Coil 2.7 (Compose image pipeline)
- **Background Tasks**: WorkManager with HiltWorker
- **Preferences**: Jetpack DataStore Preferences
- **Security**: SHA-256 verification, Android Network Security Config, zero telemetry

---

## 📱 Application Screens

1. **Home**:
   - Featured MTRX Hero Banner
   - Interactive category filter chips (*All*, *Productivity*, *Creativity*, *Utilities*, etc.)
   - Real-time search bar with instant title, description, and category filtering
   - "Recently Updated" horizontal carousel
   - "All Applications" grid with dynamic action buttons (*Get*, *Update*, *Open*, *Progress*)
   - Pull-to-refresh catalog synchronization
2. **App Detail**:
   - High-resolution icon, app title, developer badge ("MTRX Tech Verified")
   - Technical specifications card: Version, Package Size, Category, Min SDK
   - Single contextual Action Button (*Install* / *Update* / *Open* / *Downloading...* / *Installing...*)
   - Screenshot carousel
   - Comprehensive app description and version changelog
   - Cryptographic SHA-256 hash card for security transparency
   - 1-click Uninstall option for installed apps
3. **Updates**:
   - Summary of apps with newer versions available
   - Version difference indicators (`v1.0.0 ➔ v2.5.0`)
   - "Update All" button that coordinates batch updates
   - Clear up-to-date indicator when no updates are pending
4. **My Apps**:
   - Overview of all installed apps managed by MTRX Store
   - Total device storage footprint calculation
   - Quick "Open" and "Uninstall" action buttons
5. **Settings**:
   - Theme mode toggle (*Dark*, *Light*, *System Default*)
   - Custom GitHub Raw Catalog URL configuration with 1-click reset
   - Background update check interval (*6h*, *12h*, *24h*)
   - 1-click APK cache cleaner
   - App version, architecture, and corporate credits

---

## 🔒 Security Architecture

1. **SHA-256 Checksum Validation**:
   Before passing any downloaded APK to the Android installer, MTRX Store streams the file through `java.security.MessageDigest` and calculates its SHA-256 hash. If the hash does not match the catalog manifest, the file is **immediately deleted** and rejected.
2. **Automatic APK Cleanup**:
   Downloaded APKs are stored in private app cache (`context.cacheDir/apks/`) and are automatically deleted upon successful installation or when cleared via Settings.
3. **HTTPS-Only**:
   Enforced via `res/xml/network_security_config.xml`. Cleartext HTTP is disabled.
4. **Guided Unknown Sources Permission**:
   When a user initiates an install without `REQUEST_INSTALL_PACKAGES` granted, a guided dialog directs them directly to `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES`.

---

## 🌐 Serverless Backend: How It Works

MTRX Store requires **no dedicated web server or database backend**. The entire distribution infrastructure runs on GitHub:
- **Catalog Manifest**: A JSON file (`catalog.json`) hosted on GitHub (via `raw.githubusercontent.com`).
- **APK Hosting**: Hosted as binary assets in **GitHub Releases** (or custom HTTPS URLs).

### Catalog JSON Schema
```json
[
  {
    "packageName": "com.mtrx.office.text",
    "name": "MTRX Document Studio",
    "description": "Professional document creation, publishing and typography system.",
    "shortDescription": "Next-gen typography and document editorial workspace",
    "iconUrl": "https://raw.githubusercontent.com/MarapathranGroups/MTRX-Store-Catalog/main/icons/document_studio.png",
    "screenshots": [
      "https://raw.githubusercontent.com/MarapathranGroups/MTRX-Store-Catalog/main/screenshots/doc_1.png"
    ],
    "category": "Productivity",
    "versionName": "2.5.0",
    "versionCode": 250,
    "apkUrl": "https://github.com/MarapathranGroups/MTRX-Store-Catalog/releases/download/v2.5.0/MTRX-Document-Studio-v2.5.0.apk",
    "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "sizeBytes": 24500000,
    "changelog": "• Enhanced typography rendering engine\n• Offline citation manager",
    "minSdk": 26,
    "updatedAt": "2026-11-20T10:00:00Z"
  }
]
```

---

## 📖 Developer Guide & Operational Procedures

### 1. How to Add a New App to MTRX Store

1. **Build the APK**:
   Build your application's release APK:
   ```bash
   ./gradlew assembleRelease
   ```
2. **Compute the SHA-256 Hash**:
   On Windows (PowerShell):
   ```powershell
   Get-FileHash -Path "path\to\app-release.apk" -Algorithm SHA256 | Select-Object -ExpandProperty Hash
   ```
   On Linux/macOS:
   ```bash
   sha256sum path/to/app-release.apk
   ```
3. **Get File Size in Bytes**:
   ```powershell
   (Get-Item "path\to\app-release.apk").Length
   ```
4. **Upload to GitHub Releases**:
   - In your GitHub repository, create a new Release (e.g. tag `v1.0.0`).
   - Attach your `app-release.apk` to the release assets.
   - Copy the direct download link for the APK asset.
5. **Add Entry to `catalog.json`**:
   Add a new JSON object to your `catalog.json` with the fields: `packageName`, `name`, `versionName`, `versionCode`, `apkUrl`, `sha256`, `sizeBytes`, etc.
6. **Commit & Push**:
   Push the updated `catalog.json` to your GitHub repository. Every MTRX Store client will immediately discover the new app on pull-to-refresh or background sync!

---

### 2. How to Publish an App Update

1. **Bump Version in `build.gradle.kts`**:
   ```kotlin
   defaultConfig {
       versionCode = 2 // Must be greater than previous versionCode
       versionName = "1.1.0"
   }
   ```
2. **Build and Sign Release APK**:
   ```bash
   ./gradlew assembleRelease
   ```
3. **Calculate New SHA-256 Hash & File Size**:
   ```powershell
   Get-FileHash -Path "app\build\outputs\apk\release\app-release.apk" -Algorithm SHA256
   ```
4. **Upload to New GitHub Release**:
   Create tag `v1.1.0` and attach the APK.
5. **Update Entry in `catalog.json`**:
   Update `versionName`, `versionCode`, `apkUrl`, `sha256`, `sizeBytes`, `changelog`, and `updatedAt`.
6. **Automatic Detection**:
   MTRX Store compares the device's installed `versionCode` against the catalog's `versionCode`. When the catalog `versionCode` > installed `versionCode`, the store automatically transitions the button to **"Update"** and lists it under the **Updates** tab!

---

### 3. How to Generate & Back Up the Signing Keystore

To distribute APKs outside Google Play, your APK must be signed with your private release keystore.

#### Generate Keystore
Run `keytool` (included with JDK):
```bash
keytool -genkey -v -keystore mtrx-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias mtrx-key
```
You will be prompted for:
- Keystore password
- Name, Organization ("Marapathran Groups / MTRX Tech")
- Key alias password

#### Configure Signing in `app/build.gradle.kts`
Create a `keystore.properties` file in the project root (**never commit this file to git**):
```properties
storeFile=../mtrx-release-key.jks
storePassword=your_keystore_password
keyAlias=mtrx-key
keyPassword=your_key_password
```
Reference it in `app/build.gradle.kts`:
```kotlin
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

signingConfigs {
    create("release") {
        storeFile = file(keystoreProperties.getProperty("storeFile") ?: "mtrx-release-key.jks")
        storePassword = keystoreProperties.getProperty("storePassword")
        keyAlias = keystoreProperties.getProperty("keyAlias")
        keyPassword = keystoreProperties.getProperty("keyPassword")
    }
}
buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
    }
}
```

#### Backing Up the Keystore
> [!IMPORTANT]
> If you lose your signing keystore, Android will treat any future updates as an incompatible signature conflict (`STATUS_FAILURE_CONFLICT`), and users will be forced to manually uninstall the app (losing local data) before reinstalling.
>
> **Best Practices**:
> 1. Store `mtrx-release-key.jks` in a secure encrypted vault (e.g., MTRX Cloud Vault, Bitwarden, or an air-gapped USB drive).
> 2. Document the alias and passwords in a secure password manager.
> 3. Store the public certificate fingerprint:
>    ```bash
>    keytool -list -v -keystore mtrx-release-key.jks -alias mtrx-key
>    ```

---

### 4. How to Build the Release APK

1. **Clean Project**:
   ```bash
   ./gradlew clean
   ```
2. **Build Release APK**:
   ```bash
   ./gradlew assembleRelease
   ```
3. **Locate Generated APK**:
   The compiled, signed APK will be located at:
   ```
   app/build/outputs/apk/release/app-release.apk
   ```
4. **Verify APK Signature with `apksigner`**:
   ```bash
   apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
   ```

---

## ⚡ 1-Click Android Build Scripts

In the parent `android/` directory:
- **Build All Suite Apps**:
  ```cmd
  android\Build-All-Android-Apps.bat
  ```
- **1-Click Deploy to Connected Physical Device / Emulator**:
  ```cmd
  android\Install-All-To-Connected-Device.bat
  ```

---

*© 2026 MTRX Tech • Marapathran Groups. All rights reserved.*
