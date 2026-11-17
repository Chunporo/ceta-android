# CETA Android — Build Spec (dùng trực tiếp để code)

> Tài liệu này **không phải PRD**. PRD là `CETA_Android_Spec.md`; SRS là `ceta-mobile-v1.1.docx`.
> Đây là **bản vẽ thi công**: cấu trúc project, contract, state, DTO, thứ tự build. Đọc xong là code được.
> Nguồn sự thật về backend: `CETA_00_BASELINE_v1.md`. Nơi nào lệch, baseline thắng.

---

## 0. Cách dùng

| Bước | Việc |
|---|---|
| 1 | Dựng project theo §2 + §3, chạy được màn trắng |
| 2 | Code tầng `data` theo §5 + §6 với **MockWebServer**, chưa cần backend thật |
| 3 | Code màn hình theo §12, đúng thứ tự ở §14 |

Không làm ngược. Màn hình code trước tầng data = sửa lại 2 lần.

---

## 1. Mười điều bất di bất dịch

1. **Một màn = một ViewModel = một `UiState`.** Không gọi API trong Composable.
2. **Backend là nguồn sự thật duy nhất** cho ghế, trạng thái booking, giá. App không tự suy ra.
3. **Không cache sơ đồ ghế.** Cache phim/suất 6 giờ, vé 5 phút.
4. **Countdown tính từ `holdExpiresAt` của server**, hiệu chỉnh bằng `secondsRemaining` mỗi lần poll. Không đếm từ giờ máy.
5. **Không kết luận thanh toán từ deep link.** Luôn `GET /bookings/{id}`.
6. **Tiền là `Long` VND.** Không `Double`, không `Float`.
7. **Thời gian `Instant` UTC**, hiển thị theo `Asia/Ho_Chi_Minh`.
8. **Mọi string trong `strings.xml`.** Không hard-code tiếng Việt trong Composable.
9. **Không log token, không log payload thanh toán.**
10. **`minSdk 26`, `targetSdk` = bản Play yêu cầu tại thời điểm release.** Không đóng cứng số targetSdk vào tài liệu.

---

## 2. Cấu trúc project

Single module. Không multi-module ở bản 1 (thêm phức tạp, không thêm giá trị).

```
ceta-android/
├── gradle/libs.versions.toml
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/vn/ceta/app/
│       │   │   ├── CetaApp.kt                 @HiltAndroidApp
│       │   │   ├── MainActivity.kt
│       │   │   ├── navigation/
│       │   │   │   ├── Routes.kt
│       │   │   │   └── CetaNavHost.kt
│       │   │   ├── ui/
│       │   │   │   ├── theme/       Color.kt, Type.kt, Spacing.kt, Theme.kt
│       │   │   │   ├── component/   SeatLegend, PriceRow, PrimaryButton,
│       │   │   │   │                CountdownChip, EmptyState, ErrorBox,
│       │   │   │   │                LoadingBox, TopBar
│       │   │   │   └── screen/
│       │   │   │       ├── auth/    Login, Register, ForgotPassword
│       │   │   │       ├── home/    Home
│       │   │   │       ├── movie/   MovieList, MovieDetail
│       │   │   │       ├── showtime/ShowtimePicker
│       │   │   │       ├── seat/    SeatMap
│       │   │   │       ├── booking/ Checkout, BookingDetail, BookingList
│       │   │   │       ├── payment/ PaymentResult, RefundNotice
│       │   │   │       ├── ticket/  TicketList, TicketDetail
│       │   │   │       └── profile/ Profile, Settings
│       │   │   ├── domain/
│       │   │   │   ├── model/       Movie, Showtime, Seat, SeatStatus,
│       │   │   │   │                Booking, BookingStatus, Ticket, Money
│       │   │   │   └── usecase/     GetSeats, CreateBooking, CancelBooking,
│       │   │   │                    GetBooking, WatchHoldClock
│       │   │   ├── data/
│       │   │   │   ├── remote/
│       │   │   │   │   ├── api/     CetaApi.kt
│       │   │   │   │   ├── dto/     *Dto.kt
│       │   │   │   │   ├── mapper/  DtoMappers.kt
│       │   │   │   │   └── interceptor/ AuthInterceptor, ErrorInterceptor
│       │   │   │   ├── local/
│       │   │   │   │   ├── db/      CetaDatabase, dao/*, entity/*
│       │   │   │   │   ├── prefs/   SettingsStore (DataStore)
│       │   │   │   │   └── secure/  TokenStore (Keystore)
│       │   │   │   └── repository/  MovieRepo, ShowtimeRepo, SeatRepo,
│       │   │   │                    BookingRepo, TicketRepo
│       │   │   ├── di/              NetworkModule, DatabaseModule,
│       │   │   │                    RepositoryModule, ClockModule
│       │   │   └── core/            Result.kt, UiState.kt, ErrorMapper.kt,
│       │   │                        DispatcherProvider.kt, MoneyFormatter.kt,
│       │   │                        DateFormatters.kt
│       │   └── res/                 values/strings.xml, drawable/, mipmap/
│       └── test/java/vn/ceta/app/   unit/
```

---

## 3. Gradle — pin thế nào cho khỏi vỡ

**Không chép số version từ tài liệu này.** Tạo project bằng Android Studio bản ổn định hiện tại, nó sinh `libs.versions.toml` với bộ version tương thích nhau. Sau đó chỉ thêm dependency, **không tự nâng version lẻ**.

`gradle/libs.versions.toml` — khung tối thiểu:

```toml
[versions]
composeBom = "<studio sinh ra>"
hilt      = "<studio sinh ra>"
retrofit  = "2.11.0"
okhttp    = "4.12.0"
room      = "<studio sinh ra>"
coil      = "2.7.0"

[libraries]
compose-bom      = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-ui       = { module = "androidx.compose.ui:ui" }
compose-material3= { module = "androidx.compose.material3:material3" }
compose-preview  = { module = "androidx.compose.ui:ui-tooling-preview" }
lifecycle-vm     = { module = "androidx.lifecycle:lifecycle-viewmodel-compose" }
nav-compose      = { module = "androidx.navigation:navigation-compose" }
hilt-android     = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler    = { module = "com.google.dagger:hilt-android-compiler", version.ref = "hilt" }
hilt-nav         = { module = "androidx.hilt:hilt-navigation-compose" }
retrofit         = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
retrofit-gson    = { module = "com.squareup.retrofit2:converter-gson", version.ref = "retrofit" }
okhttp           = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okhttp-log       = { module = "com.squareup.okhttp3:logging-interceptor", version.ref = "okhttp" }
room-runtime     = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx         = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler    = { module = "androidx.room:room-compiler", version.ref = "room" }
coil-compose     = { module = "io.coil-kt:coil-compose", version.ref = "coil" }
datastore        = { module = "androidx.datastore:datastore-preferences" }
security-crypto  = { module = "androidx.security:security-crypto" }
paging           = { module = "androidx.paging:paging-compose" }

[plugins]
android-app  = { id = "com.android.application" }
kotlin-android = { id = "org.jetbrains.kotlin.android" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose" }
ksp          = { id = "com.google.devtools.ksp" }
hilt         = { id = "com.google.dagger.hilt.android" }
```

**Ba thứ bắt buộc trong `app/build.gradle.kts`:**

```kotlin
android {
    defaultConfig {
        minSdk = 26
        targetSdk = /* số Play yêu cầu — kiểm tra Play Console */ 36
        buildConfigField("String", "API_BASE_URL", "\"https://api.ceta.vn/api/v1\"")
        buildConfigField("String", "PAYMENT_RETURN_SCHEME", "\"ceta\"")
    }
    buildFeatures { compose = true; buildConfig = true }
    buildTypes {
        debug   { applicationIdSuffix = ".debug" }
        release { isMinifyEnabled = true; isShrinkResources = true
                  proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") }
    }
}
```

Flavour `staging` dùng `applicationIdSuffix = ".staging"` + base URL staging → cài song song với bản thật.

---

## 4. Core: `Result`, `UiState`, `ErrorMapper`

```kotlin
// core/Result.kt
sealed interface ApiResult<out T> {
    data class Ok<T>(val data: T) : ApiResult<T>
    data class Err(val error: ApiError) : ApiResult<Nothing>
}

data class ApiError(
    val code: String,          // SEAT_CONFLICT, HOLD_EXPIRED, ...
    val httpStatus: Int,
    val conflictSeatIds: List<Long> = emptyList(),
    val message: String? = null // chỉ để log, KHÔNG hiển thị thẳng
)
```

```kotlin
// core/UiState.kt
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data class Empty(val hint: String? = null) : UiState<Nothing>
    data class Error(val error: ApiError) : UiState<Nothing>
}
```

**`ErrorMapper` map `code` → `@StringRes`. App không hiển thị `message` của server** — server message dành cho dev, không dành cho khách.

| `code` từ backend | Màn xử lý | Chuỗi hiển thị (VI) |
|---|---|---|
| `SEAT_CONFLICT` | S11 | "Ghế {seat} vừa được người khác chọn. Bạn chọn lại giúp mình nhé." |
| `BOOKING_ALREADY_PENDING` | Snackbar | "Bạn đang có 1 đơn chờ thanh toán cho suất này." |
| `HOLD_EXPIRED` | S14 → SeatMap | "Đã hết thời gian giữ ghế." |
| `TOO_MANY_SEATS` | Snackbar | "Tối đa 8 ghế mỗi đơn." |
| `OUTSIDE_SALE_WINDOW` | Snackbar | "Suất này chưa hoặc không còn mở bán." |
| `INVALID_COUPLE_SEAT` | Snackbar | "Ghế đôi phải chọn đủ cặp." |
| `BOOKING_NOT_PENDING` | Dialog | "Đơn này không thể huỷ ở trạng thái hiện tại." |
| `UNAUTHORIZED` / 401 | → Login | "Phiên đăng nhập đã hết hạn." |
| mạng lỗi | ErrorBox | "Không kết nối được. Kiểm tra mạng và thử lại." |
| còn lại | ErrorBox | "Có lỗi xảy ra. Vui lòng thử lại." |

---

## 5. Network contract

### 5.1 `CetaApi.kt`

```kotlin
interface CetaApi {
    @POST("auth/login")            suspend fun login(@Body b: LoginRequest): TokenResponse
    @POST("auth/refresh")          suspend fun refresh(@Body b: RefreshRequest): TokenResponse
    @POST("auth/logout")           suspend fun logout(): Unit

    @GET("movies")                 suspend fun movies(@Query("page") p: Int, @Query("size") s: Int,
                                                       @Query("q") q: String?): PageResponse<MovieDto>
    @GET("movies/{id}")            suspend fun movie(@Path("id") id: Long): MovieDto

    @GET("showtimes")              suspend fun showtimes(@Query("movieId") m: Long,
                                                          @Query("date") d: String): List<ShowtimeDto>
    @GET("showtimes/{id}/seats")   suspend fun seats(@Path("id") showtimeId: Long): List<SeatDto>

    @POST("bookings")              suspend fun createBooking(@Body b: CreateBookingRequest,
                                                             @Header("Idempotency-Key") key: String): BookingDto
    @GET("bookings/{id}")          suspend fun booking(@Path("id") id: Long): BookingDto
    @DELETE("bookings/{id}")       suspend fun cancelBooking(@Path("id") id: Long,
                                                             @Body b: CancelRequest): Unit

    @POST("payments/vnpay/create-url") suspend fun createPaymentUrl(@Body b: PaymentUrlRequest): PaymentUrlDto

    @GET("tickets/my")             suspend fun myTickets(@Query("page") p: Int): PageResponse<TicketDto>
    @GET("tickets/{id}")           suspend fun ticket(@Path("id") id: Long): TicketDto
}
```

### 5.2 DTO — đúng theo baseline §7

```kotlin
data class SeatDto(
    val seatId: Long, val label: String,          // "F7"
    val type: String,                             // STANDARD|VIP|SWEETBOX|DELUXE|WHEELCHAIR
    val price: Long,                              // VND
    val status: String                            // AVAILABLE|HELD|SOLD|BLOCKED
)

data class CreateBookingRequest(val showtimeId: Long, val seatIds: List<Long>)

data class BookingDto(
    val bookingId: Long,
    val status: String,                           // PENDING|PAID|CANCELLED|EXPIRED|REFUNDED
    val totalAmount: Long,
    val holdExpiresAt: String?,                   // ISO-8601 UTC, null khi không còn giữ
    val secondsRemaining: Long?,                  // server đã tính sẵn
    val seats: List<BookingSeatDto> = emptyList(),
    val tickets: List<TicketDto> = emptyList()
)

data class BookingSeatDto(val seatId: Long, val label: String, val price: Long)

data class TicketDto(
    val ticketId: Long, val ticketCode: String,
    val qrPayload: String, val status: String,    // VALID|USED|CANCELLED
    val movieTitle: String, val posterUrl: String?,
    val showtimeStart: String, val roomName: String, val seatLabel: String
)
```

**`holdExpiresAt` là chuỗi ISO, không phải epoch.** Parse bằng `Instant.parse()`; nếu backend trả epoch millis thì sửa ở đây, **không** sửa rải rác trong màn hình.

### 5.3 Interceptor

| Interceptor | Việc |
|---|---|
| `AuthInterceptor` | Gắn `Authorization: Bearer <token>`. Gặp 401 → gọi `auth/refresh` **một lần** (dùng `Mutex` để 5 request đồng thời không refresh 5 lần), retry request gốc. Refresh fail → xoá token → phát event `SessionExpired` |
| `ErrorInterceptor` | Parse body lỗi `{code,message,details}` → `ApiError`. 409 `SEAT_CONFLICT` → đọc `details[].seatId` vào `conflictSeatIds` |
| `LoggingInterceptor` | Chỉ bật ở `debug`. **Mask** header Authorization |

---

## 6. Local storage

### Room (cache, không phải nguồn sự thật)

```kotlin
@Entity(tableName = "cached_movie")
data class MovieEntity(@PrimaryKey val id: Long, val title: String, val posterUrl: String?,
                       val payload: String, val cachedAt: Long)

@Entity(tableName = "cached_showtime")
data class ShowtimeEntity(@PrimaryKey val id: Long, val movieId: Long, val startAt: String,
                          val roomName: String, val cachedAt: Long)
```

| Dữ liệu | TTL | Ghi chú |
|---|---|---|
| Phim, thể loại | 6 giờ | Hiện kèm banner "Đang xem dữ liệu đã lưu" |
| Suất chiếu | 6 giờ | |
| Vé | 5 phút | Để mở QR khi sóng yếu ở rạp |
| **Sơ đồ ghế** | **không cache** | Luôn `GET` mạng; lỗi mạng thì chặn đặt vé |
| Booking PENDING | lưu `bookingId` + `holdExpiresAt` | Để khôi phục sau process death |

### Secure storage

Đừng dùng `EncryptedSharedPreferences` một mình (deprecated dần). Dùng **DataStore + mã hoá bằng Android Keystore** (`AES/GCM`), hoặc `EncryptedSharedPreferences` nếu team đã quen.

```kotlin
interface TokenStore {
    suspend fun saveTokens(access: String, refresh: String)
    suspend fun access(): String?
    suspend fun refresh(): String?
    suspend fun clear()
}
```

### DataStore (không nhạy cảm)

`last_email`, `notification_enabled`, `last_selected_cinema_id` (dù bản 1 chỉ 1 cụm — để sẵn).

---

## 7. Navigation

```kotlin
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val MOVIE_LIST = "movies?q={q}"
    const val MOVIE_DETAIL = "movie/{movieId}"
    const val SHOWTIME = "showtime/{movieId}"
    const val SEAT_MAP = "seats/{showtimeId}"
    const val CHECKOUT = "checkout/{bookingId}"
    const val PAYMENT_RESULT = "payment/result/{bookingId}"
    const val REFUND_NOTICE = "payment/refund/{bookingId}"
    const val TICKETS = "tickets"
    const val TICKET_DETAIL = "tickets/{ticketId}"
    const val BOOKINGS = "bookings"
    const val BOOKING_DETAIL = "bookings/{bookingId}"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
}
```

Deep link trong `AndroidManifest`:

```xml
<intent-filter android:autoVerify="false">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="ceta" android:host="payment" android:pathPrefix="/result" />
</intent-filter>
```

Bottom nav 4 tab: **Home · Movies · Tickets · Profile** (Notifications gộp vào Profile như icon chuông — bớt 1 tab, đỡ chật).

**Back trong luồng đặt vé:** ở `Checkout` → hỏi "Huỷ đơn và nhả ghế?" → gọi `DELETE /bookings/{id}` → về `SeatMap`. Không dùng `onBackPressed` lặng lẽ.

---

## 8. Countdown giữ ghế — chỗ dễ sai nhất

```kotlin
class HoldClock(private val now: () -> Instant = Instant::now) {
    private var deadline: Instant? = null

    fun start(expiresAt: Instant) { deadline = expiresAt }
    fun sync(serverSecondsRemaining: Long) { deadline = now().plusSeconds(serverSecondsRemaining) }
    fun remaining(): Duration = deadline?.let { Duration.between(now(), it) } ?: Duration.ZERO
    fun isExpired(): Boolean = remaining() <= Duration.ZERO
}
```

Quy tắc:

1. Bắt đầu từ `booking.holdExpiresAt`.
2. Mỗi lần poll `GET /bookings/{id}` → gọi `sync(secondsRemaining)`. Đây là **hiệu chỉnh** chống lệch đồng hồ máy.
3. `LaunchedEffect` cập nhật 1 giây/lần **chỉ khi màn hình foreground** (`repeatOnLifecycle(STARTED)`), tránh đốt pin.
4. Còn **≤ 120 giây** → chip vàng + text "Ghế sẽ được nhả sau 2:00", nút "Thanh toán ngay".
5. Hết giờ → disable mọi nút, hiện "Đã hết thời gian giữ ghế", **không** hiển thị số âm.
6. Quay lại từ background mà quá hạn → đi thẳng về SeatMap kèm thông báo, không cho thanh toán.

**Test được:** `HoldClock(now = fakeClock::now)` — test 6 case trên không cần chờ thật.

---

## 9. Thanh toán — máy trạng thái

```
[Checkout] --create-url--> mở VNPay (Custom Tab)
      |
      +-- user quay lại / deep link --> [PaymentResult: PROCESSING]
                                          |  poll GET /bookings/{id]
                                          |
        +--> PAID       -> [TicketDetail]  (chỉ khi backend xác nhận)
        +--> PENDING    -> poll tiếp: 2s, 3s, 5s, 8s (tối đa 60s) rồi hiện "đang xử lý" + nút "Kiểm tra lại"
        +--> EXPIRED    -> "Hết thời gian thanh toán" + [Chọn lại ghế]
        +--> CANCELLED  -> "Thanh toán chưa hoàn tất" + [Thử lại]
        +--> REFUNDED   -> [RefundNotice] "Hoàn tiền trong 24 giờ"
```

Bắt buộc:

- Mở VNPay bằng **Custom Tab** (`androidx.browser`), không dùng WebView. VNPay chặn/khó chịu với WebView, và Custom Tab chia sẻ cookie trình duyệt nên thẻ đã lưu dùng được.
- Không gọi API tạo URL lần hai khi đang có request bay (nút disable + `isSubmitting` flag).
- Lưu `pendingBookingId` vào DataStore ngay khi tạo booking — app bị kill vẫn khôi phục được.
- **Không** hiển thị `vnp_ResponseCode` lấy từ deep link. Chỉ để log.

---

## 10. Seat map

**Không dùng `LazyVerticalGrid` cho sơ đồ ghế.** Ghế cần zoom/pan và bố cục không đều (lối đi, khoảng trống) — dùng `Canvas` + `detectTransformGestures`.

```kotlin
data class SeatMapUiState(
    val loading: Boolean = true,
    val rows: List<SeatRow> = emptyList(),      // đã group theo hàng từ backend
    val selected: Set<Long> = emptySet(),       // seatId
    val totalAmount: Long = 0,
    val maxSeats: Int = 8,
    val conflictSeats: List<Long> = emptyList(),// từ 409
    val error: ApiError? = null
)
```

Quy tắc tương tác:

| Hành vi | Kết quả |
|---|---|
| Bấm ghế `AVAILABLE` | Chọn/bỏ chọn; tính lại `totalAmount` |
| Bấm ghế `HELD` / `SOLD` / `BLOCKED` | Không phản ứng (không toast — tránh ồn) |
| Chọn ghế thứ 9 | Snackbar "Tối đa 8 ghế mỗi đơn", không chọn |
| Chọn 1 nửa `SWEETBOX` | Tự chọn cả cặp; nếu nửa kia không `AVAILABLE` → snackbar |
| Pinch zoom | Giữ nguyên ghế đã chọn (state ở ViewModel, không ở composable) |
| Xoay màn hình | Giữ nguyên (ViewModel + `rememberSaveable` cho transform) |
| Nhận WebSocket `HELD` cho ghế đang chọn | Bỏ khỏi `selected`, snackbar "Ghế F7 vừa có người chọn" |

**Màu ghế (không chỉ dùng màu — thêm ký hiệu cho người mù màu):**

| Trạng thái | Màu | Ký hiệu |
|---|---|---|
| AVAILABLE | xám nhạt viền | không |
| SELECTED | accent đậm | ✓ |
| HELD | xám đậm | gạch chéo mảnh |
| SOLD | xám đậm hơn | ✕ |
| BLOCKED | gần trắng, viền nét đứt | không vẽ số ghế |

Chú thích (legend) cố định dưới sơ đồ, luôn hiện — khách không phải đoán.

---

## 11. Design tokens

Material 3, một accent duy nhất. Không dùng gradient, không bóng đổ nặng.

```kotlin
object Spacing { val xs=4.dp; val s=8.dp; val m=16.dp; val l=24.dp; val xl=32.dp }

object CetaColors {
    val Accent      = Color(0xFFE23744)  // đỏ vé
    val AccentDark  = Color(0xFFB01E2B)
    val Surface     = Color(0xFFFAFAFA)
    val OnSurface   = Color(0xFF1A1A1A)
    val Muted       = Color(0xFF6B6B6B)
    val Warn        = Color(0xFFF2A93B)  // countdown ≤2 phút
    val Danger      = Color(0xFFD32F2F)
    val SeatFree    = Color(0xFFE8E8E8)
    val SeatSold    = Color(0xFF9E9E9E)
}
```

Typography: 1 font system (Roboto). Scale: Title 22sp/28, H 18sp/24, Body 14sp/20, Caption 12sp/16, Mono cho mã vé.

**Số tiền luôn format `###.000 ₫`** qua `MoneyFormatter` — một chỗ duy nhất, test 4 case (0, 1.000, 1.250.000, âm).

---

## 12. Mười bốn màn — spec để code

Mỗi màn: route · API · state · thành phần · trạng thái phải có · AC.

### S1 Login
`login` · `POST auth/login` · `LoginUiState(email, password, loading, error)` · Email/Password field, nút Đăng nhập, link Quên mật khẩu, link Đăng ký · lỗi inline dưới field · AC: sai mật khẩu không xoá email; thành công → Home.

### S2 Register
`register` · `POST auth/register` · 4 field + validate tại chỗ (email regex, mật khẩu ≥8) · AC: không gọi API khi validate fail.

### S3 Forgot Password
`forgot-password` · `POST auth/forgot` · luôn hiện "Nếu email tồn tại, hướng dẫn đã được gửi" (không lộ email có tồn tại).

### S4 Home
`home` · `GET movies?page=0` (now showing) + featured · `HomeUiState(loading, nowShowing, featured, error)` · Carousel featured + lưới poster 2 cột · skeleton khi load, banner offline khi dùng cache.

### S5 Movie List / Search
`movies` · `GET movies?q&page` · Paging 3 · Empty state "Không tìm thấy phim nào".

### S6 Movie Detail
`movie/{id}` · `GET movies/{id}` + `GET showtimes?movieId` · poster, meta, mô tả thu gọn 4 dòng + "Xem thêm", nút "Chọn suất".

### S7 Showtime Picker
`showtime/{movieId}` · `GET showtimes?movieId&date` · tab ngày 7 ngày tới · chip giờ chiếu, suất đã qua bị mờ và không bấm.

### S8 Seat Map
`seats/{showtimeId}` · `GET showtimes/{id}/seats` · state ở §10 · thanh dưới: "N ghế · {tổng}" + nút Tiếp tục (disable khi 0 ghế) · banner S12 nếu >70% HELD.

### S9 Checkout
`checkout/{bookingId}` · `GET bookings/{id}` + `POST payments/vnpay/create-url` · tóm tắt phim/suất/ghế/tổng, countdown ở header, nút "Thanh toán VNPay" · AC: hai lần bấm nhanh chỉ mở một Custom Tab.

### S10 Payment Result
`payment/result/{bookingId}` · poll `GET bookings/{id}` · máy trạng thái §9 · AC: không bao giờ hiện "Thành công" trước khi backend trả `PAID`.

### S11 Refund Notice
`payment/refund/{bookingId}` · đọc từ `BookingDto` · "Rất tiếc, ghế đã được người khác mua. Chúng tôi sẽ hoàn tiền trong 24 giờ." + mã đơn + nút liên hệ · **không** có nút back về giỏ.

### S12 Ticket List
`tickets` · `GET tickets/my` · card vé: poster nhỏ, tên phim, giờ, ghế, badge `VALID`/`USED`.

### S13 Ticket Detail
`tickets/{id}` · `GET tickets/{id}` · QR lớn (ít nhất 240dp), mã chữ dưới QR (dự phòng khi camera rạp lỗi), tự tăng độ sáng màn hình khi mở, giảm về ban đầu khi đóng · cache 5 phút.

### S14 Profile / Settings
`profile`, `settings` · `GET/PUT profile` · thông tin, đổi mật khẩu, ngôn ngữ (bản 1 chỉ VI), thông báo, xoá tài khoản, phiên bản app.

**Mọi màn phải có đủ 4 trạng thái:** Loading (skeleton, không spinner giữa màn) · Empty (có câu gợi ý hành động) · Error (có nút thử lại) · Content.

---

## 13. Testing

| Tầng | Công cụ | Cái gì phải test |
|---|---|---|
| Unit | JUnit5 + MockK + Turbine | ViewModel state sau mỗi event; `HoldClock` 6 case; `ErrorMapper` 9 code; `MoneyFormatter` 4 case |
| Repository | MockWebServer | 200 map đúng; 409 → `conflictSeatIds`; 401 → refresh 1 lần rồi success |
| UI | Compose UI Test | Ghế thứ 9 bị chặn; nút disable khi hết giờ; countdown hiển thị đúng mm:ss |

**Fake server trước, backend sau.** Viết `MockWebServer` với 4 file JSON (seats, booking, 409 conflict, payment result) — dựng được toàn bộ UI trước khi backend xong. Đây là cách duy nhất để dev mobile không bị chặn.

---

## 14. Thứ tự build — 10 ngày

| Ngày | Việc | Done khi |
|---|---|---|
| **D1** | Project + Hilt + Nav + theme + 2 màn trắng | App chạy, điều hướng được Login ↔ Home |
| **D2** | Network layer + interceptor + `ErrorMapper` + MockWebServer | Test repository xanh với 4 JSON mẫu |
| **D3** | Auth (S1–S3) + TokenStore + refresh mutex | Đăng nhập/đăng xuất/refresh chạy với fake server |
| **D4** | Home + Movie list + Detail (S4–S6) + Room cache | Xoay máy bay vẫn xem được phim đã cache |
| **D5** | Showtime + **SeatMap** (S7–S8) | Chọn ghế, tính tiền, chặn ghế thứ 9, cặp SWEETBOX |
| **D6** | Create booking + 409 → S11-conflict + countdown | Bấm Tiếp tục 2 lần không tạo 2 đơn |
| **D7** | Checkout + Custom Tab + PaymentResult (S9–S11) | 4 nhánh trạng thái đúng, kill app vẫn khôi phục |
| **D8** | Ticket list + Ticket Detail + QR (S12–S13) | QR sáng màn hình, cache 5 phút |
| **D9** | Profile + Settings + xoá tài khoản (S14) | — |
| **D10** | Nối backend thật + sửa lệch DTO + smoke | 1 giao dịch sandbox end-to-end |

**Gate:** hết D6 mà 409 chưa xử lý đúng (hiện đúng ghế xung đột) thì **không** làm D7. Sai ở đây = khách bị trừ tiền rồi mới biết mất ghế.

---

## 15. Checklist trước khi nối backend thật

- [ ] `API_BASE_URL` không phải `localhost`; emulator dùng `10.0.2.2`
- [ ] DTO khớp 100% với response thật (bắt bằng log `debug` một lần)
- [ ] `holdExpiresAt` parse đúng định dạng (ISO vs epoch)
- [ ] 409 trả về đúng `details[].seatId` — thử bằng 2 máy
- [ ] Deep link `ceta://payment/result` mở đúng màn ở **bản release** (không chỉ debug)
- [ ] Không còn log token
- [ ] `minSdk 26` chạy thử trên máy Android 8
- [ ] R8 bật mà app vẫn chạy (test bản release, không chỉ debug)

---

## 16. Mười cái bẫy đã biết

1. **Đếm ngược từ giờ máy** → khách chỉnh giờ, countdown nhảy loạn. Dùng `secondsRemaining` của server.
2. **Cache sơ đồ ghế** → khách thấy ghế trống, chọn, trả tiền, mất ghế. Không cache.
3. **Tin deep link** → hiện "Thành công" khi thực tế `PENDING`. Luôn poll.
4. **Refresh token song song 5 lần** → 4 lần fail, đá văng user. Dùng `Mutex`.
5. **`LocalDateTime` thay `Instant`** → lệch 7 giờ ở chỗ suất chiếu.
6. **`Double` cho tiền** → sai số hiển thị. `Long`.
7. **WebView cho VNPay** → thẻ đã lưu không dùng được, một số ngân hàng chặn.
8. **Composable tự gọi API trong `init`** → gọi lại mỗi recomposition. Dùng `LaunchedEffect(Unit)` hoặc `uiState` từ ViewModel.
9. **Không lưu `pendingBookingId`** → app bị kill lúc thanh toán, khách mất dấu đơn.
10. **Test bằng fake data cứng trong ViewModel** → nối backend thật vỡ hàng loạt. Dùng MockWebServer từ D2.
