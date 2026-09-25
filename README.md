# 📅 Thời Khóa Biểu AI - Trợ Lý Lịch Học & Công Việc Thông Minh

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-brightgreen.svg?style=flat&logo=android)](https://developer.android.com/jetpack/compose)
[![Google Gemini](https://img.shields.io/badge/Gemini%20AI-Multimodal%20Vision-blue.svg?style=flat&logo=google)](https://ai.google.dev/)
[![Room Database](https://img.shields.io/badge/Room-Database-orange.svg?style=flat&logo=sqlite)](https://developer.android.com/training/data-storage/room)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Android CI](https://github.com/actions/workflows/android.yml/badge.svg)](.github/workflows/android.yml)

Ứng dụng Android hiện đại được xây dựng hoàn toàn bằng **Kotlin** và **Jetpack Compose (Material 3)**, tích hợp trí tuệ nhân tạo **Google Gemini AI** giúp tự động chuyển đổi ảnh chụp thời khóa biểu thành lịch trình số hóa, thiết lập chu kỳ lặp lại thông minh, nhắc nhở trước giờ học và đồng bộ mượt mà với **Google Calendar**.

---

## ✨ Tính Năng Nổi Bật

### 1. 🤖 Quét Thời Khóa Biểu Bằng AI (Gemini Vision)
- **Chụp ảnh trực tiếp từ camera** hoặc **chọn ảnh từ thư viện**.
- Sử dụng mô hình nhận diện đa phương thức **Gemini 3.5 Flash** để bóc tách chính xác mọi định dạng thời khóa biểu (bảng in, bảng excel, ảnh chụp màn hình, chữ viết tay).
- Tự động trích xuất thông minh:
  - 📚 **Tên môn học / Sự kiện**
  - 👨‍🏫 **Tên giảng viên / Giáo viên**
  - 📍 **Phòng học / Địa điểm**
  - 🗓️ **Thứ trong tuần (Thứ 2 - Chủ Nhật)**
  - ⏰ **Thời gian bắt đầu & kết thúc (định dạng 24h)**
  - 📝 **Ghi chú chi tiết**
- Cho phép người dùng xem trước, chọn lọc và chỉnh sửa trước khi lưu vào danh sách.

### 2. 🔑 Tự Nhập & Quản Lý Gemini API Key Cá Nhân
- Người dùng có thể tự nhập API Key cá nhân hoàn toàn miễn phí từ [Google AI Studio](https://aistudio.google.com).
- Lưu trữ cục bộ bảo mật trên thiết bị qua `Encrypted/SharedPreferences`.
- Tùy chọn ẩn/hiện API Key, kiểm tra tính hợp lệ và chỉnh sửa bất cứ lúc nào.

### 3. 🔁 Chu Kỳ Lặp Lại Linh Hoạt
- Hỗ trợ các chu kỳ lặp lại theo nhu cầu:
  - **Hàng tuần (Weekly):** Cố định theo ngày trong tuần.
  - **Hàng tháng (Monthly):** Lặp lại vào ngày tương ứng mỗi tháng.
  - **Hàng năm (Yearly):** Lặp lại theo niên khóa hoặc sự kiện kỷ niệm.
  - **Không lặp lại (Once):** Dành cho các buổi học bù, thi học kỳ hoặc sự kiện đặc biệt.

### 4. ⚡ Dành Cho Người Bận Rộn (Smart Assistant)
- **Thẻ thông minh hôm nay:** Tự động phát hiện và ghim lớp học đang diễn ra hoặc đếm ngược đến môn học kế tiếp (Ví dụ: *"Còn 35 phút nữa đến giờ học"*).
- Thanh tiến độ ngày học giúp bạn nắm bắt khối lượng công việc trong nháy mắt.
- Lọc lịch trình theo ngày trong tuần hoặc xem toàn bộ danh sách.

### 5. ⏰ Hệ Thống Nhắc Nhở & Báo Thức Tự Động
- Tích hợp `AlarmManager` và `NotificationManager` chuẩn Android.
- Tùy chọn nhắc nhở trước: **10 phút**, **15 phút**, **30 phút** hoặc **1 giờ**.
- Bật/tắt rung và chuông báo linh hoạt.
- Tự động kích hoạt lại lịch báo thức sau khi thiết bị khởi động lại (`BOOT_COMPLETED`).
- Nút kiểm tra thông báo tức thì trong mục Cài đặt.

### 6. 📅 Đồng Bộ Hoàn Hảo Với Google Calendar
- **Thêm nhanh từng môn vào ứng dụng Google Calendar:** Nhấn biểu tượng Google Calendar trên từng thẻ môn học để mở ứng dụng Google Calendar với đầy đủ thông tin sự kiện, phòng học, chu kỳ lặp lại (`RRULE:FREQ=...`) và nhắc nhở.
- **Đồng bộ hàng loạt vào Lịch hệ thống:** Tích hợp trực tiếp với Android `CalendarContract`, tự động đưa tất cả môn học vào tài khoản Google Calendar được chọn trên điện thoại chỉ với 1 chạm.
- **Xuất file chuẩn `.ics` (iCalendar):** Dễ dàng chia sẻ hoặc nhập vào Google Calendar trên web và mọi thiết bị khác.

---

## 🏗️ Kiến Trúc & Công Nghệ Sử Dụng

| Thành phần | Công nghệ / Thư viện |
| :--- | :--- |
| **Ngôn ngữ** | Kotlin 2.0.21 |
| **Giao diện (UI)** | Jetpack Compose, Material Design 3 (M3) |
| **Kiến trúc** | Clean Architecture + MVVM (Model-View-ViewModel) |
| **Quản lý dữ liệu** | Room Database (SQLite ORM) + Kotlin Coroutines & Flow |
| **Trí tuệ nhân tạo (AI)** | Google Gemini Multimodal REST API (`gemini-2.5-flash`) |
| **Mạng & Xử lý JSON** | OkHttp3, Retrofit, Moshi Kotlin |
| **Xử lý ảnh** | Coil Compose, AndroidX Activity Result Contracts |
| **Hẹn giờ & Thông báo**| AlarmManager, NotificationCompat, BroadcastReceiver |
| **Đồng bộ Lịch** | Android CalendarContract Provider & iCalendar (`.ics`) Builder |
| **Testing** | JUnit 4, Robolectric, AndroidX Compose UI Test |

---

## 📁 Cấu Trúc Thư Mục Dự Án

```
├── .github/workflows/
│   └── android.yml                # CI tự động build & test trên GitHub
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml # Cấu hình quyền, AlarmReceiver, FileProvider
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt # Entry point ứng dụng
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/      # Room Database, ScheduleDao, AppDatabase
│   │   │   │   │   ├── model/      # Entity ScheduleItem, Enums, DayOfWeek
│   │   │   │   │   ├── repository/ # ScheduleRepository quản lý dữ liệu
│   │   │   │   │   └── service/    # GeminiScheduleParser (gửi ảnh & nhận diện)
│   │   │   │   ├── receiver/       # ScheduleAlarmReceiver xử lý báo thức
│   │   │   │   ├── ui/
│   │   │   │   │   ├── dialogs/    # AddEditScheduleDialog, ApiKeyDialog
│   │   │   │   │   ├── screens/    # TimetableScreen, ScanScheduleScreen, SettingsScreen, MainScreen
│   │   │   │   │   ├── theme/      # Material 3 Color, Theme, Typography
│   │   │   │   │   └── viewmodel/  # TimetableViewModel xử lý toàn bộ logic
│   │   │   │   └── util/           # GoogleCalendarHelper, NotificationHelper, PreferencesManager
│   │   │   └── res/                # XML Resources, Drawables, App Icons, Strings
│   │   └── test/                   # Robolectric & Unit Tests
│   └── build.gradle.kts            # Cấu hình Gradle module app
├── build.gradle.kts                # Cấu hình Gradle Root
├── settings.gradle.kts             # Tên project & danh mục kho lưu trữ
├── gradlew & gradlew.bat           # Gradle Wrapper cho Linux/macOS/Windows
├── .env.example                    # Mẫu cấu hình API key
├── LICENSE                         # Giấy phép nguồn mở MIT
└── README.md                       # Tài liệu hướng dẫn sử dụng
```

---

## 🚀 Hướng Dẫn Cài Đặt & Chạy Ứng Dụng

### Yêu Cầu Môi Trường
- **Android Studio** Ladybug (2024.2) trở lên.
- **JDK:** Java 17 trở lên.
- **Android SDK:** Compile SDK 36 (Android 15+), Min SDK 24 (Android 7.0+).

### Các Bước Cài Đặt

1. **Clone repository:**
   ```bash
   git clone https://github.com/<your-username>/thoi-khoa-bieu-ai.git
   cd thoi-khoa-bieu-ai
   ```

2. **Mở dự án trong Android Studio:**
   - Chọn **File > Open** và chọn thư mục `thoi-khoa-bieu-ai`.
   - Chờ Android Studio đồng bộ Gradle (`Sync Project with Gradle Files`).

3. **Chạy ứng dụng:**
   - Kết nối thiết bị Android thật (bật USB Debugging) hoặc khởi động máy ảo (Android Emulator).
   - Nhấn nút **Run 'app'** (`Shift + F10`) để cài đặt và khởi chạy.

---

## 🔑 Hướng Dẫn Lấy Gemini API Key Miễn Phí

1. Truy cập [Google AI Studio](https://aistudio.google.com).
2. Đăng nhập bằng tài khoản Google.
3. Nhấp vào **"Get API key"** và chọn **"Create API key in new project"**.
4. Sao chép chuỗi API key được cấp.
5. Mở ứng dụng **Thời Khóa Biểu AI**, nhấn vào biểu tượng chìa khóa 🔑 ở góc phải trên cùng và dán API key vào.

---

## 📤 Hướng Dẫn Đẩy Code Lên GitHub

Nếu bạn muốn tạo một repository mới trên GitHub và tải toàn bộ mã nguồn lên:

### Bước 1: Tạo Repository Trên GitHub
1. Truy cập [GitHub](https://github.com/new).
2. Đặt tên repository: `thoi-khoa-bieu-ai` (hoặc tên tùy thích).
3. Đặt ở chế độ **Public** hoặc **Private**.
4. **Không** tích chọn "Add a README file" hay ".gitignore" (vì dự án đã có sẵn đầy đủ).
5. Nhấn **Create repository**.

### Bước 2: Chạy Các Lệnh Git Trong Thư Mục Dự Án
Mở Terminal tại thư mục gốc của dự án và chạy các lệnh sau:

```bash
# Khởi tạo git repository
git init

# Thêm tất cả các file vào git
git add .

# Tạo commit đầu tiên
git commit -m "feat: initial commit - thoi khoa bieu AI with Gemini multimodal & Google Calendar sync"

# Đổi nhánh chính thành main
git branch -M main

# Liên kết với GitHub repo của bạn (thay URL bằng repo của bạn)
git remote add origin https://github.com/<tai-khoan-cua-ban>/thoi-khoa-bieu-ai.git

# Đẩy code lên GitHub
git push -u origin main
```

---

## 🧪 Chạy Kiểm Thử Tự Động (Testing)

Bạn có thể chạy toàn bộ unit tests và Robolectric tests bằng lệnh:

- **Linux / macOS:**
  ```bash
  ./gradlew testDebugUnitTest
  ```
- **Windows:**
  ```cmd
  gradlew.bat testDebugUnitTest
  ```

---

## 📜 Giấy Phép (License)

Dự án được phân phối dưới giấy phép [MIT License](LICENSE). Bạn hoàn toàn có quyền sử dụng, sửa đổi và phân phối theo mục đích cá nhân hoặc thương mại.
