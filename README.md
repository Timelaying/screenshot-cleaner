# Screenshot Cleaner

Screenshot Cleaner is an Android app that helps users review and clean screenshots older than 30 days. It works locally on the phone, sends a notification when old screenshots are found, and lets the user swipe right to keep or swipe left to delete after confirmation.

## MVP

- Scan device screenshots through MediaStore.
- Find screenshots older than 30 days.
- Request image/media and notification permissions.
- Notify the user when old screenshots exist.
- Open a review screen from the notification.
- Swipe right to keep.
- Swipe left to request deletion.
- Remember kept screenshots locally with Room.

## Stack

- Kotlin
- Jetpack Compose
- MVVM-style repository layer
- Room
- WorkManager
- MediaStore
- GitHub Actions

## Build

GitHub Actions builds the app on every pull request and push to `main`.

```bash
gradle testDebugUnitTest assembleDebug
```

Local builds require Android Studio or a JDK plus Android SDK.

