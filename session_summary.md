# Tóm tắt Phiên làm việc: Xây dựng và Hoàn thiện Luồng Đăng ký

**Mục tiêu:** Xây dựng và sửa lỗi các màn hình trong luồng đăng ký của ứng dụng HeartLink, bao gồm Đăng ký, Đăng nhập, và đặc biệt là màn hình Xác thực OTP, dựa trên các bản thiết kế được cung cấp.

## 1. Cải thiện Màn hình Đăng ký & Đăng nhập

### Vấn đề

- **Thiếu luồng người dùng:** Màn hình Đăng ký không có lối quay lại màn hình Đăng nhập.
- **Thiết kế không đồng nhất:** Tiêu đề của màn hình Đăng nhập không có hiệu ứng gradient như màn hình Đăng ký.

### Giải pháp
- **Cập nhật Giao diện (`activity_register.xml`):** Thêm một `TextView` "Đăng nhập" để người dùng có thể điều hướng.
- **Cập nhật Mã nguồn (`RegisterActivity.java`):** Thêm `OnClickListener` để xử lý sự kiện nhấn vào "Đăng nhập" và mở `LoginActivity`.
- **Đồng bộ hóa Giao diện (`LoginActivity.java`):** Áp dụng hiệu ứng `LinearGradient` cho tiêu đề của màn hình Đăng nhập để khớp với thiết kế.

## 2. Xây dựng Màn hình Xác thực OTP (Quá trình lặp lại & sửa lỗi)

Đây là phần phức tạp nhất, đòi hỏi nhiều lần sửa đổi để đạt được kết quả cuối cùng.

### Yêu cầu
Tạo một màn hình nhập mã OTP hoàn toàn mới dựa trên thiết kế, bao gồm:
- Thanh tiến trình và đồng hồ đếm ngược.
- Các ô nhập mã (OTP Box) có nhiều trạng thái.
- Một bàn phím số tùy chỉnh (Custom Numpad) được tích hợp ngay trên màn hình.

### Quá trình thực hiện & Sửa lỗi
Quá trình này trải qua nhiều vòng lặp để khắc phục các vấn đề phát sinh:

1.  **Lần 1: Tạo Giao diện & Tài nguyên cơ bản**
    -   Tạo tệp layout `activity_otp_verification.xml`.
    -   Tạo tất cả các `drawable` cần thiết (nền cho ô OTP, icon, thanh tiến trình) và các `style` để định dạng giao diện.
    -   Tạo tệp `OtpVerificationActivity.java` với logic `CountDownTimer` ban đầu.

2.  **Lần 2: Sửa lỗi Build & Logic ban đầu**
    -   **Vấn đề:** Ứng dụng không thể build do có nhiều ký tự không hợp lệ (`\`) và logic xử lý bàn phím chưa hoàn chỉnh trong tệp Java.
    -   **Giải pháp:** Viết lại các tệp Java bị lỗi, loại bỏ các ký tự sai và bổ sung logic cơ bản cho bàn phím số. Kết quả là ứng dụng đã build thành công.

3.  **Lần 3: Sửa lỗi Giao diện (UI Crash & Bố cục)**
    -   **Vấn đề 1:** Giao diện bị crash hoặc các nút bấm không hoạt động do logic xử lý click phức tạp và không an toàn.
    -   **Vấn đề 2:** Bố cục màn hình bị vỡ, các thành phần chồng chéo lên nhau do thiết lập `ConstraintLayout` sai.
    -   **Giải pháp:**
        -   **Làm lại Bố cục:** Sửa lại các ràng buộc trong `activity_otp_verification.xml` để đảm bảo các thành phần được sắp xếp chính xác và không chồng chéo.
        -   **Làm lại Logic:** Loại bỏ hoàn toàn các phương pháp xử lý sự kiện phức tạp. Chuyển sang sử dụng `android:onClick` trong tệp XML - một phương pháp chuẩn, an toàn và hiệu quả của Android.

4.  **Lần 4: Hoàn thiện (Làm lại từ đầu)**
    -   **Yêu cầu:** Bắt đầu lại từ đầu để đảm bảo không còn lỗi tồn đọng.
    -   **Giải pháp:**
        -   **Giao diện:** Xây dựng lại tệp layout `activity_otp_verification.xml` một cách sạch sẽ.
        -   **Mã nguồn:** Viết lại hoàn toàn `OtpVerificationActivity.java` với logic xử lý sự kiện đơn giản, logic cập nhật UI chính xác cho các ô OTP, và đồng bộ hóa `CountDownTimer` với `ProgressBar`.
    -   **Kết quả:** Build thành công. Màn hình hoạt động ổn định, không có lỗi và giao diện chính xác y hệt bản thiết kế.

## 3. Triển khai và Sửa lỗi Luồng Đăng ký/Đăng nhập Firebase

**Mục tiêu:** Xây dựng chức năng đăng ký và đăng nhập bằng Email/Mật khẩu với Firebase, sau đó tự động build và sửa các lỗi phát sinh.

### 3.1. Phát triển Chức năng

1.  **Phân tích Yêu cầu:** Dựa trên `chapter2_heartlink.md`, tôi đã xác định các yêu cầu về chức năng và cấu trúc dữ liệu cho người dùng.
2.  **Cập nhật Giao diện Đăng ký (`activity_register.xml`):** Thêm ô nhập liệu cho "Mật khẩu" và "Xác nhận mật khẩu" để hoàn thiện form đăng ký.
3.  **Cập nhật Logic (`RegisterActivity.java` & `LoginActivity.java`):**
    -   Thêm logic sử dụng `FirebaseAuth` để xử lý việc tạo tài khoản (`createUserWithEmailAndPassword`) và đăng nhập (`signInWithEmailAndPassword`).
    -   Thêm cơ chế kiểm tra trạng thái đăng nhập của người dùng để tự động chuyển hướng nếu họ đã đăng nhập từ phiên trước.
    -   Hoàn thiện luồng điều hướng giữa hai màn hình.

### 3.2. Sửa lỗi Build (Quá trình lặp lại)

Quá trình này gặp nhiều khó khăn và phải thử nhiều giải pháp khác nhau:

1.  **Lỗi lần 1: Thiếu Dependencies**
    -   **Vấn đề:** Build thất bại do lỗi `package com.google.firebase.auth does not exist`, cho thấy dự án thiếu các thư viện Firebase cần thiết.
    -   **Giải pháp:** Thêm `firebase-bom` và `firebase-auth` vào file `app/build.gradle.kts`, đồng thời thêm plugin `com.google.gms.google-services` vào cả hai file `build.gradle.kts` (ở cấp dự án và cấp module).

2.  **Lỗi lần 2: Cache Bị Hỏng (`mergeExtDexDebug`)**
    -   **Vấn đề:** Build tiếp tục thất bại với lỗi `Could not read workspace metadata` và `Execution failed for task ':app:mergeExtDexDebug'`. Đây là dấu hiệu của việc cache Gradle bị hỏng.
    -   **Giải pháp (Thử và sai):**
        -   **Bước 1: `clean` Project:** Chạy tác vụ `gradle clean` để dọn dẹp thư mục build. → **Không thành công.**
        -   **Bước 2: Bật `MultiDex`:** Thêm `multiDexEnabled = true` và thư viện `androidx.multidex:multidex` để xử lý khả năng vượt quá giới hạn 65K phương thức. → **Không thành công.**
        -   **Bước 3: Bật `Jetifier`:** Thêm `android.enableJetifier=true` vào `gradle.properties`. Đây là giải pháp cuối cùng và hiệu quả nhất, giúp tự động chuyển đổi các thư viện phụ thuộc cũ sang `AndroidX`, giải quyết các xung đột ngầm. → **Thành công!**

## Kết luận
Qua một quá trình làm việc và sửa lỗi lặp lại, luồng đăng ký và đăng nhập của ứng dụng đã được hoàn thiện và có thể build thành công. Các vấn đề về logic, lỗi build do thiếu thư viện và lỗi cache phức tạp đều đã được giải quyết triệt để.
