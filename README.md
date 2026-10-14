# Vibe — Android social app starter (Kotlin + Jetpack Compose + Firebase)

This is an Android Studio project starter for an original social app inspired by common social-feed patterns. It includes:
- Email/password sign-in and registration using Firebase Authentication
- Firestore-backed feed posts
- 24-hour Stories at the top of the feed (expired stories are filtered by `expiresAt`)
- Create text posts and text stories
- Basic profile/sign-out screen
- Firestore and Storage security rules
- Firebase setup and build instructions

It is a starter project, not a production-complete ShareChat clone. It does not use ShareChat branding or private code. Media upload, direct messaging, moderation, push notifications, reporting, and production-grade pagination are not implemented in this first version.

## Requirements
- Android Studio (current stable)
- JDK 17
- Android SDK 35
- A Firebase project
- Internet connection for Gradle and Firebase

## 1. Add your Firebase project
1. Create a project at https://console.firebase.google.com/
2. Add an Android app with package name `com.example.vibe`.
3. Download `google-services.json` and place it in `app/google-services.json`.
4. In Firebase Authentication, enable **Email/Password**.
5. Create a Cloud Firestore database.
6. Enable Firebase Storage if you plan to extend this project with image/video uploads.
7. In Firebase Console, publish the rules from `firestore.rules` and `storage.rules`.
8. For Storage rules, use the rules in `storage.rules` only after enabling Firebase Storage.

Do not commit `google-services.json` to a public repository. This project intentionally does not include a Firebase configuration file because it must belong to your own Firebase project.

## 2. Open and build
1. Unzip the project.
2. Open the `MalayalamSocialApp` folder in Android Studio.
3. Let Gradle sync.
4. Add `app/google-services.json`.
5. Run on an Android device or emulator (API 24+).

Command-line build from the project folder:
```bash
./gradlew assembleDebug
```
The APK, if the build succeeds, will be at:
`app/build/outputs/apk/debug/app-debug.apk`

The wrapper JAR is not included in this source bundle. If Android Studio asks for a Gradle wrapper, choose its wrapper setup action or run `gradle wrapper` with Gradle installed. You can also build from Android Studio.

## 3. Firestore data model
- `users/{uid}`: `uid`, `displayName`, `email`, `createdAt`
- `posts/{postId}`: `authorId`, `authorName`, `text`, `createdAt`
- `stories/{storyId}`: `authorId`, `authorName`, `text`, `createdAt`, `expiresAt`

Stories are queried for `expiresAt > now`. Firestore rules enforce that story expiration is no more than 24 hours after creation. Expired story documents may remain in Firestore until deleted; filtering makes them invisible in the app. For automatic deletion, configure a Firestore TTL policy on the `expiresAt` field in the Firebase console. TTL deletion is asynchronous and is not an exact timer.

## Important production notes
Before releasing publicly, add:
- App Check, abuse/rate limiting, moderation/reporting, block controls
- A server-side trusted timestamp and stricter validation (clients can manipulate device time)
- Pagination and indexes as required by Firebase query errors
- Image/video upload with file-size/type validation and Storage rules
- Privacy policy, terms, account deletion, consent and store listing
- Automated tests and crash reporting
