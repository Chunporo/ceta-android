<h1 align="center">CETA Cinema</h1>

<p align="center">
  A cinema ticket-booking app for Android, built with Jetpack Compose.<br>
  Pick a movie, choose seats, pay by VNPay, MoMo or bank transfer, and keep the e-ticket on your phone.
</p>

<p align="center">
  <img alt="API" src="https://img.shields.io/badge/API-26%2B-brightgreen.svg">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.2-7F52FF.svg">
  <img alt="Compose" src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg">
  <img alt="Backend" src="https://img.shields.io/badge/backend-Spring%20Boot-6DB33F.svg">
</p>

<p align="center">
  <img src="docs/screenshots/home.png" width="30%" alt="Home">
  <img src="docs/screenshots/seats.png" width="30%" alt="Seat map">
  <img src="docs/screenshots/vietqr.png" width="30%" alt="VietQR payment">
</p>

> [!NOTE]
> This is a personal learning project, not a commercial product. No real money moves: payments run against simulators (see [Payments](#payments-without-merchant-accounts)).

## Features

- **Browse:** now showing and coming soon, search, movie details, reviews and release reminders.
- **Showtimes:** by day, cinema and format (2D, 3D, IMAX), subtitled or dubbed.
- **Seats:** a seat map with standard, VIP and couple seats. Your seats are held for 10 minutes while you pay.
- **Checkout:** snacks, promo codes and CETA loyalty points, with the total recomputed by the server.
- **Payments:** VNPay, MoMo, and VietQR bank transfer. The VietQR screen shows the QR and the transfer note, then waits for the bank to confirm.
- **Tickets:** e-tickets with a QR code, upcoming and watched lists, notifications.
- **Accounts:** sign-in with Firebase (Google, phone) or email, account deletion.
- **Look and feel:** dark and light theme, Vietnamese and English.

<p align="center">
  <img src="docs/screenshots/showtimes.png" width="30%" alt="Showtimes">
  <img src="docs/screenshots/checkout.png" width="30%" alt="Checkout">
  <img src="docs/screenshots/ticket.png" width="30%" alt="E-ticket">
</p>

## Tech stack

- Minimum SDK 26, target SDK 37, Kotlin 2.2.
- UI: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 and Navigation Compose.
- Dependency injection: [Hilt](https://dagger.dev/hilt/).
- Async: Kotlin coroutines and Flow.
- Networking: [Retrofit](https://github.com/square/retrofit), [OkHttp](https://github.com/square/okhttp) and Gson.
- Images: [Coil 3](https://coil-kt.github.io/coil/), also used for the VietQR image.
- QR codes: [ZXing](https://github.com/zxing/zxing) renders the e-ticket QR on the device.
- Local storage: Jetpack DataStore for preferences, plus secure storage for the session.
- Auth: Firebase Authentication.
- Build: Gradle Kotlin DSL with a version catalog, three flavors (`dev`, `staging`, `prod`), R8 for release builds.

## Architecture

The app follows the layering in Android's [architecture guide](https://developer.android.com/topic/architecture): a UI layer of Compose screens and ViewModels, a domain layer of models and small helpers (seat selection, hold timer, payment outcome), and a data layer of repositories over the REST API.

```
app/src/main/java/com/ceta/cinema/
  ui/          Compose screens (one folder per feature), theme, shared components
  domain/      models, seat selection, hold timer, payment outcome
  data/        remote (Retrofit APIs, DTOs), local (preferences, secure storage), repositories
  core/        ApiResult, error mapping, caching, formatters, validators
  di/          Hilt modules
  navigation/  routes and the NavHost
```

- Screens expose a single `UiState`; ViewModels turn repository results into it. Network calls go through a safe-call wrapper that maps failures to `ApiResult` and user-facing errors.
- The server is the source of truth for prices and seat state. The app shows what the server computed and never recomputes a total.
- Payment screens poll the booking until the server marks it paid, so the result does not depend on how the payment provider redirects back.
- The release build keeps the network DTOs and Retrofit generics under R8 (`app/proguard-rules.pro`).

## Backend

The app talks to a Spring Boot backend (separate repository, `MovieTicketBackend`) over `/api/v1`. The `dev` flavor points at `http://10.0.2.2:8080/api/v1/`, which is your computer as seen from the Android emulator. Start the backend first, using its README.

## Build and run

Requirements: Android Studio or a JDK 17+ with the Android SDK, and an emulator or device on API 26+.

```bash
./gradlew :app:assembleDevDebug
adb install -r app/build/outputs/apk/dev/debug/app-dev-debug.apk
```

`staging` and `prod` point at `staging-api.ceta.vn` and `api.ceta.vn`, which are placeholders.

Sign-in uses Firebase Authentication. The committed `app/google-services.json` belongs to the author's Firebase project. To use Google or phone sign-in with your own backend, register the app in your own Firebase project and replace that file.

## Testing

```bash
./gradlew :app:testDevDebugUnitTest
```

## Payments without merchant accounts

The backend ships local simulators for VNPay, MoMo and VietQR, so the whole flow works offline. For VietQR, reach the QR screen in the app, then confirm the transfer from the backend repository:

```bash
python3 scripts/vietqr_sim.py CETA3056A1 249000   # <transfer note shown in the app> <amount>
```

The app picks the payment up and opens the e-ticket.
