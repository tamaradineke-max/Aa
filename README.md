# Erixclusive AI Studio

Android app project for the Erixclusive AI fashion photography and Vinted listing studio.

## Build a free test APK with GitHub Actions

1. Push these files to the repository's `main` branch (or `master`).
2. Open the repository on GitHub and choose **Actions**.
3. Select **Build Android APK** and wait for the workflow to finish successfully.
4. Open the completed workflow run, scroll to **Artifacts**, and download `Erixclusive-AI-Studio-debug-apk`.
5. Extract the ZIP and install `app-debug.apk` on your Android phone. Android may ask you to allow installation from that browser/file manager.

You can also start a build manually from **Actions → Build Android APK → Run workflow**. GitHub Actions provides a limited free allowance for public repositories; usage is subject to GitHub's current limits.

## Important: AI service setup

The project uses Firebase AI Logic for Gemini-powered image generation and Vinted listing generation. A successful APK build does **not** by itself activate those online AI features. To use them, create/configure a Firebase project, register the Android app using the exact `applicationId` from `app/build.gradle.kts`, enable Firebase AI Logic and the required App Check provider, then place the downloaded `google-services.json` in the `app/` directory. Do not commit private service-account credentials, API secrets, or debug tokens.

Without Firebase configuration the Android package can still be compiled, but AI requests will not work until Firebase is configured. Image generation may also require a Firebase billing plan depending on the model and usage; verify pricing in Firebase before enabling it.

## Package details

- App label: Erixclusive AI Studio
- Android application ID: `com.aistudio.vintedstudio.kmvrpq`
- Minimum Android version: Android 7.0 (API 24)
- APK produced by CI: `app/build/outputs/apk/debug/app-debug.apk`
