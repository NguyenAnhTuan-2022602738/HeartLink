# Kiến Trúc Phát Triển Dự Án HeartLink

## 1. Tổng Quan Dự Án

HeartLink là một ứng dụng hẹn hò (dating app) trên nền tảng Android, được phát triển bằng ngôn ngữ Java. Ứng dụng cho phép người dùng tạo hồ sơ cá nhân, tìm kiếm và kết nối với những người phù hợp dựa trên sở thích, vị trí và các tiêu chí khác.

## 2. Công Nghệ Và Kiến Trúc

### 2.1. Nền Tảng Và Ngôn Ngữ
- **Platform**: Android (Java)
- **Build System**: Gradle
- **Version Control**: Git
- **Backend**: Firebase (Authentication, Firestore Database, Storage, Cloud Messaging)

### 2.2. Kiến Trúc Ứng Dụng
- **Architecture Pattern**: MVVM (Model-View-ViewModel) với Repository Pattern
- **UI Framework**: Android Views với Material Design Components
- **Dependency Injection**: Manual dependency injection (không sử dụng DI framework)
- **Database**: Firebase Firestore (NoSQL)
- **Storage**: Firebase Storage cho media files

## 3. Cấu Trúc Project

```
HeartLink/
├── app/
│   ├── build.gradle.kts          # Cấu hình build cho app module
│   ├── proguard-rules.pro        # Rules cho code obfuscation
│   ├── src/
│   │   ├── androidTest/          # Unit tests
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/vn/haui/heartlink/
│   │   │   │   ├── activities/   # UI Activities (19 activities)
│   │   │   │   ├── adapters/     # RecyclerView Adapters
│   │   │   │   ├── models/       # Data Models
│   │   │   │   ├── repositories/ # Data Access Layer
│   │   │   │   ├── utils/        # Utility Classes
│   │   │   │   └── fragments/    # UI Fragments (nếu có)
│   │   │   └── res/              # Resources (layouts, drawables, values)
│   │   └── test/                 # Unit tests
│   └── build/                    # Build outputs
├── gradle/
│   └── wrapper/                  # Gradle wrapper
├── build.gradle.kts              # Root build file
├── settings.gradle.kts           # Project settings
├── gradle.properties             # Gradle properties
└── local.properties              # Local properties
```

## 4. Các Thành Phần Chính

### 4.1. Activities (19 Activities)
Các activities được chia thành các nhóm chức năng:

#### Onboarding Flow:
- **SplashActivity**: Màn hình splash khi khởi động app
- **WelcomeActivity**: Màn hình chào mừng
- **GenderSelectionActivity**: Chọn giới tính
- **ProfileInfoActivity**: Nhập thông tin cơ bản (tên, ngày sinh)
- **SeekingActivity**: Chọn mục đích tìm kiếm
- **InterestsActivity**: Chọn sở thích cá nhân
- **PhotoUploadActivity**: Upload ảnh đại diện
- **LocationPermissionActivity**: Xin quyền truy cập vị trí
- **NotificationPermissionActivity**: Xin quyền gửi thông báo

#### Authentication:
- **LoginActivity**: Đăng nhập
- **RegisterActivity**: Đăng ký tài khoản
- **OtpVerificationActivity**: Xác thực OTP

#### Main Features:
- **MainActivity**: Activity chính, điều hướng giữa các tab
- **MatchesActivity**: Danh sách các matches
- **MessagesActivity**: Danh sách tin nhắn
- **ChatActivity**: Giao diện chat với một người
- **MatchSuccessActivity**: Thông báo khi có match mới

#### Profile Management:
- **ProfileDetailActivity**: Xem chi tiết profile của người khác
- **ProfileSettingsActivity**: Cài đặt và quản lý profile cá nhân

### 4.2. Models (Data Models)
- **User.java**: Model cho thông tin người dùng
- **ChatMessage.java**: Model cho tin nhắn chat
- **Match.java**: Model cho thông tin match
- **DiscoveryProfile.java**: Model cho profile hiển thị trong discovery

### 4.3. Repositories (Data Access Layer)
- **UserRepository.java**: Quản lý dữ liệu người dùng
- **MatchRepository.java**: Quản lý dữ liệu matches và interactions
- **ChatRepository.java**: Quản lý dữ liệu chat
- **ReportRepository.java**: Quản lý báo cáo và block users

### 4.4. Adapters (UI Adapters)
- **DiscoveryCardAdapter.java**: Adapter cho danh sách profile trong discovery
- **ProfilePhotoAdapter.java**: Adapter cho danh sách ảnh profile
- **MatchesAdapter.java**: Adapter cho danh sách matches
- **MessagesAdapter.java**: Adapter cho danh sách tin nhắn

### 4.5. Utils (Utility Classes)
- **FirebaseHelper.java**: Helper cho Firebase operations
- **NavigationHelper.java**: Helper cho navigation logic
- **LocationHelper.java**: Helper cho location services
- **PermissionHelper.java**: Helper cho permission management

## 5. Use Cases (Trường Hợp Sử Dụng)

### 5.1. UC_Authentication (Xác thực)
- Đăng ký tài khoản mới
- Đăng nhập bằng email/password
- Xác thực OTP
- Khôi phục mật khẩu
- Đăng xuất

### 5.2. UC_Profile (Hồ sơ cá nhân)
- Thiết lập profile ban đầu (onboarding)
- Chỉnh sửa thông tin cá nhân
- Upload và quản lý ảnh
- Cài đặt sở thích
- Quản lý quyền riêng tư (location, notifications)

### 5.3. UC_Discovery (Khám phá)
- Swipe để like/dislike profiles
- Lọc profiles theo tiêu chí (giới tính, độ tuổi, khoảng cách)
- Xem chi tiết profile
- Thuật toán gợi ý profiles

### 5.4. UC_Matching (Kết nối)
- Tạo match khi hai người like lẫn nhau
- Hiển thị danh sách matches
- Thông báo match mới
- Quản lý danh sách matches

### 5.5. UC_Chat (Trò chuyện)
- Gửi và nhận tin nhắn
- Hiển thị danh sách cuộc trò chuyện
- Real-time messaging
- Đánh dấu tin nhắn đã đọc

### 5.6. UC_Safety (An toàn)
- Báo cáo người dùng vi phạm
- Chặn người dùng
- Quản lý danh sách chặn

## 6. Database Schema

### 6.1. Users Collection
```json
{
  "uid": "firebase_user_id",
  "email": "user@example.com",
  "name": "Nguyễn Văn A",
  "gender": "male|female",
  "dateOfBirth": "15/05/1995",
  "bio": "Mô tả về bản thân",
  "photoUrls": ["url1", "url2", "url3"],
  "seekingGender": "male|female|both",
  "seekingAgeMin": 18,
  "seekingAgeMax": 50,
  "seekingType": "friend|chat|relationship|no_strings|later",
  "interests": ["Đá bóng", "Xem phim", "Du lịch"],
  "profileComplete": true,
  "latitude": 21.0285,
  "longitude": 105.8542,
  "locationVisible": true,
  "notificationsEnabled": true
}
```

### 6.2. Matches Collection
```json
{
  "matchId": "unique_match_id",
  "userId1": "user1_id",
  "userId2": "user2_id",
  "matchedAt": 1638360000000,
  "status": "active|inactive"
}
```

### 6.3. Chat Threads Collection
```json
{
  "chatId": "unique_chat_id",
  "participants": {
    "user1_id": true,
    "user2_id": true
  },
  "lastMessage": "Nội dung tin nhắn cuối",
  "lastSenderId": "sender_id",
  "lastMessageAt": 1638360000000
}
```

### 6.4. Messages Subcollection (trong Chat Threads)
```json
{
  "messageId": "unique_message_id",
  "senderId": "sender_id",
  "content": "Nội dung tin nhắn",
  "timestamp": 1638360000000,
  "type": "text|image",
  "readBy": {
    "user1_id": true,
    "user2_id": false
  }
}
```

## 7. API Integration

### 7.1. Firebase Services
- **Firebase Authentication**: Xác thực người dùng
- **Cloud Firestore**: Database NoSQL
- **Firebase Storage**: Lưu trữ file media
- **Firebase Cloud Messaging**: Push notifications
- **Firebase Analytics**: Phân tích sử dụng app

### 7.2. Third-party Libraries
- **Glide**: Image loading và caching
- **Material Components**: UI components
- **Google Play Services**: Location services, maps
- **AndroidX Libraries**: Modern Android APIs

## 8. Security Considerations

### 8.1. Authentication
- Firebase Authentication cho secure login
- Email verification cho account validation
- Password reset functionality

### 8.2. Data Privacy
- User consent cho location permissions
- Option to hide location
- Data encryption trong transit và at rest

### 8.3. Content Moderation
- User reporting system
- Block/unblock functionality
- Content guidelines enforcement

## 9. Performance Optimization

### 9.1. UI Performance
- RecyclerView với ViewHolder pattern
- Image caching với Glide
- Lazy loading cho images và data

### 9.2. Network Performance
- Firebase offline capabilities
- Efficient data querying
- Pagination cho large datasets

### 9.3. Memory Management
- Proper lifecycle management
- Memory leak prevention
- Efficient bitmap handling

## 10. Testing Strategy

### 10.1. Unit Testing
- JUnit cho logic testing
- Mockito cho dependency mocking
- Repository layer testing

### 10.2. Integration Testing
- Firebase emulator testing
- API integration testing
- Database operation testing

### 10.3. UI Testing
- Espresso cho UI automation
- Activity lifecycle testing
- User flow testing

## 11. Deployment & CI/CD

### 11.1. Build Process
- Gradle build system
- Automated signing configuration
- ProGuard cho code obfuscation

### 11.2. Distribution
- Google Play Store deployment
- Beta testing với Google Play Beta
- Crash reporting với Firebase Crashlytics

### 11.3. Version Management
- Semantic versioning
- Automated build numbering
- Release notes management

## 12. Future Enhancements

### 12.1. Planned Features
- Video calling integration
- Advanced matching algorithms
- Premium subscription features
- Social media integration

### 12.2. Technical Improvements
- Migration to Kotlin
- Implementation of DI framework (Dagger/Hilt)
- Offline-first architecture
- GraphQL API integration

---

**Tác giả**: GitHub Copilot  
**Ngày tạo**: 7 tháng 11, 2025  
**Version**: 1.0  
**Project**: HeartLink Dating App</content>
<parameter name="filePath">c:\Users\cuida\Documents\and\HeartLink\architecture.md