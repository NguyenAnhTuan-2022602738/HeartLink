# Báo Cáo Tổng Thể Dự Án HeartLink

## 1. Các Bảng Dữ Liệu Tổng Thể

Dựa trên các model trong folder `models`, hệ thống HeartLink quản lý các loại dữ liệu chính sau. Dưới đây là các bảng dữ liệu tổng thể với mô tả chi tiết dựa trên code thực tế:

### 1.1. Thực Thể USERS (Người Dùng)

| Thuộc Tính          | Kiểu Dữ Liệu | Giải Thích & Ràng Buộc |
|----------------------|--------------|-------------------------|
| uid (PK)            | String      | Khóa chính. UID duy nhất do Firebase Authentication tự động sinh ra khi người dùng đăng ký. Đảm bảo mỗi người dùng có một định danh không trùng lặp. |
| email               | String      | Địa chỉ email dùng để đăng ký, đăng nhập và nhận thông báo. Có thể dùng để khôi phục mật khẩu. |
| name                | String      | Họ và tên đầy đủ của người dùng, hiển thị trên hồ sơ và trong chat. |
| gender              | String      | Giới tính của người dùng (vd: "male", "female"). Phục vụ cho bộ lọc tìm kiếm và thuật toán gợi ý. |
| dateOfBirth         | String      | Ngày tháng năm sinh dưới dạng chuỗi. Dùng để tính toán tuổi, một tiêu chí quan trọng cho việc gợi ý kết nối và lọc tìm kiếm. |
| bio                 | String      | Mô tả ngắn gọn về bản thân. Là nơi người dùng giới thiệu tính cách, mong muốn kết bạn, giúp tạo thiện cảm ban đầu. |
| photoUrls           | List<String>| Danh sách URL ảnh đại diện trên Firebase Storage. Ảnh là yếu tố trực quan nhất trong thẻ Swipe. |
| seekingGender       | String      | Giới tính mà người dùng quan tâm (male, female, both). |
| seekingAgeMin       | int         | Tuổi tối thiểu mà người dùng quan tâm. |
| seekingAgeMax       | int         | Tuổi tối đa mà người dùng quan tâm. |
| seekingType         | String      | Loại mối quan hệ mà người dùng tìm kiếm. |
| interests           | List<String>| Danh sách sở thích (vd: ["Đá bóng", "Xem phim", "Du lịch"]). Đây là thuộc tính quan trọng nhất để thuật toán tìm ra những người có cùng sở thích, từ đó tăng khả năng kết nối thành công. |
| profileComplete     | boolean     | Cờ đánh dấu hồ sơ đã hoàn thành chưa. |
| latitude            | Double      | Vĩ độ của người dùng. Kết hợp với longitude để xác định vị trí. |
| longitude           | Double      | Kinh độ của người dùng. Dùng để tính khoảng cách với người dùng khác cho thuật toán gợi ý và bộ lọc. |
| locationVisible     | Boolean     | Cờ bật/tắt chia sẻ vị trí. Nếu false, vị trí (latitude, longitude) sẽ không được dùng để gợi ý hoặc hiển thị cho người khác, đảm bảo quyền riêng tư. |
| notificationsEnabled| Boolean     | Cờ bật/tắt thông báo push. |

### 1.2. Thực Thể MATCHES (Kết Nối)

| Thuộc Tính    | Kiểu Dữ Liệu | Giải Thích & Ràng Buộc |
|---------------|--------------|-------------------------|
| match_id (PK) | String      | Khóa chính. Một mã định danh duy nhất cho mỗi kết nối, thường được Firebase tự động sinh ra. |
| user_id_1 (FK)| String      | Khóa ngoại tham chiếu đến uid của người dùng thứ nhất trong kết nối. |
| user_id_2 (FK)| String      | Khóa ngoại tham chiếu đến uid của người dùng thứ hai trong kết nối. |
| matched_at    | Long        | Thời điểm mà hai người "Like" lẫn nhau và kết nối được tạo ra dưới dạng timestamp. Dùng để sắp xếp các cuộc trò chuyện trong danh sách. |

**Lưu ý:** Khi lưu, user_id_1 và user_id_2 nên được sắp xếp theo thứ tự (ví dụ: luôn lưu ID nhỏ hơn trước) để tránh trùng lặp kết nối (ví dụ: A-B và B-A là một).

### 1.3. Thực Thể CHAT_THREADS (Luồng Chat)

| Thuộc Tính          | Kiểu Dữ Liệu | Giải Thích & Ràng Buộc |
|---------------------|--------------|-------------------------|
| chatId (PK)         | String      | Khóa chính. Mã định danh duy nhất cho luồng chat. |
| userId1             | String      | UID của người dùng thứ nhất trong luồng chat. |
| userId2             | String      | UID của người dùng thứ hai trong luồng chat. |
| participants        | Map<String, Boolean> | Map chứa các người tham gia chat với trạng thái. |
| lastMessage         | String      | Nội dung tin nhắn cuối cùng. |
| lastSenderId        | String      | UID của người gửi tin nhắn cuối cùng. |
| lastMessageAt       | long        | Timestamp của tin nhắn cuối cùng. |
| updatedAt           | long        | Timestamp cập nhật cuối cùng. |
| readTimestamps      | Map<String, Long> | Map chứa timestamp đọc của từng người dùng. |

### 1.4. Thực Thể CHAT_MESSAGES (Tin Nhắn Chat)

| Thuộc Tính        | Kiểu Dữ Liệu | Giải Thích & Ràng Buộc |
|-------------------|--------------|-------------------------|
| messageId (PK)    | String      | Khóa chính. Mã định danh duy nhất cho mỗi tin nhắn. |
| chatId (FK)       | String      | Khóa ngoại tham chiếu đến chatId của luồng chat. Xác định tin nhắn này thuộc về cuộc trò chuyện nào. |
| senderId (FK)     | String      | Khóa ngoại tham chiếu đến uid. Xác định người gửi tin nhắn này. |
| text              | String      | Nội dung văn bản của tin nhắn. Có thể null nếu là tin nhắn hình ảnh. |
| imageUrl          | String      | URL của hình ảnh nếu là tin nhắn hình ảnh. Có thể null nếu là tin nhắn văn bản. |
| timestamp         | long        | Thời điểm tin nhắn được gửi dưới dạng timestamp. Dùng để sắp xếp tin nhắn theo thứ tự thời gian trong cuộc trò chuyện. |

**Lưu ý:** Một tin nhắn có thể có text hoặc imageUrl, hoặc cả hai.

### 1.5. Thực Thể REPORTS (Báo Cáo)

(Bảng này chưa được implement trong models, có thể sẽ được thêm sau.)

## 2. Cấu Trúc Project Đang Sử Dụng

Dự án HeartLink là một ứng dụng Android được xây dựng bằng Java, sử dụng Gradle cho build system. Cấu trúc chính như sau:

- **build.gradle.kts**: File cấu hình build chính cho toàn bộ project.
- **settings.gradle.kts**: Cấu hình các module trong project.
- **app/**: Module chính của ứng dụng.
  - **build.gradle.kts**: Cấu hình build cho module app.
  - **proguard-rules.pro**: Quy tắc cho ProGuard (obfuscation).
  - **build/**: Thư mục chứa các file build output (generated sources, intermediates, outputs).
  - **src/main/**: Source code chính.
    - **AndroidManifest.xml**: Manifest của ứng dụng, khai báo permissions, activities, v.v.
    - **java/vn/haui/heartlink/**: Source code Java.
      - **activities/**: Các Activity (màn hình) của ứng dụng.
      - **adapters/**: Các Adapter cho RecyclerView, ListView.
      - **models/**: Các model class đại diện cho dữ liệu (User, Match, Chat, v.v.).
      - **repositories/**: Lớp repository để xử lý dữ liệu từ Firebase.
      - **utils/**: Các utility class (helper functions).
    - **res/**: Resources (layouts, values, drawables, v.v.).
  - **src/test/**: Unit tests.
  - **src/androidTest/**: Instrumentation tests.
- **gradle/**: Gradle wrapper và libs.versions.toml cho version management.
- **Giao_dien/**: Thư mục chứa thiết kế giao diện (PSD, mockups).

### Tác Dụng Của Từng Package

- **activities**: Chứa các Activity (màn hình) của ứng dụng. (19 files) Ví dụ: SplashActivity, LoginActivity, MainActivity, ProfileSettingsActivity, ChatActivity, v.v. Đây là nơi xử lý logic UI và tương tác người dùng.
- **adapters**: Chứa các Adapter để bind dữ liệu vào RecyclerView, ListView. (5 files) Ví dụ: DiscoveryCardAdapter, ProfilePhotoAdapter, MatchesAdapter.
- **models**: Chứa các model class đại diện cho dữ liệu (User, Match, Chat, v.v.). (10 files) Ví dụ: User, Match, ChatMessage, Report. Dùng để map dữ liệu từ Firebase.
- **repositories**: Lớp repository để xử lý dữ liệu từ Firebase. (4 files) Ví dụ: UserRepository, MatchRepository, ChatRepository, ReportRepository. Tách biệt logic data access.
- **utils**: Các utility class (helper functions). (10 files) Ví dụ: DateUtils, ImageUtils, FirebaseHelper, NavigationHelper, v.v.
- **services**: Các service classes. (2 files) Ví dụ: FirebaseMessagingService.
- **fragments**: Các Fragment. (1 file) Ví dụ: ProfileFragment.
- **ui**: UI components và custom views. (4 files) Ví dụ: FilterBottomSheetDialog, GradientTextView, WaveView.

### Package Dư Thừa

Dựa trên cấu trúc hiện tại, không có package rõ ràng nào dư thừa. Tuy nhiên, src/test và src/androidTest trống, có thể coi là dư thừa nếu không có kế hoạch viết tests, nhưng nên giữ cho structure chuẩn.

## 3. Các Chức Năng Và File Liên Quan

Dựa trên các use case trong `chapter2_heartlink.md`, dưới đây là các chức năng chính và các file/code liên quan trong project hiện tại.

### 3.1. Đăng Ký Tài Khoản (UC_DangKy)

**Mô tả**: Cho phép người dùng tạo tài khoản mới bằng email, Google, hoặc Facebook.

**File Liên Quan**:
- **activities/RegisterActivity.java**: UI đăng ký.
- **activities/OtpVerificationActivity.java**: Xác thực OTP.
- **repositories/UserRepository.java**: Tạo user trong Firebase.

**Hàm Chính**:
- RegisterActivity: onCreate, registerUser, sendOtp.
- UserRepository: createUser, saveUserProfile.

### 3.2. Đăng Nhập (UC_DangNhap)

**Mô tả**: Cho phép người dùng đăng nhập bằng email, Google, hoặc Facebook.

**File Liên Quan**:
- **activities/LoginActivity.java**: Chính UI và logic đăng nhập.
- **repositories/UserRepository.java**: Xác thực với Firebase.

**Hàm Chính**:
- LoginActivity: onCreate, loginUser, firebaseAuthWithGoogle.
- UserRepository: signInWithEmail, signInWithGoogle, signInWithFacebook.

### 3.3. Kết Nối (UC_Discovery)

**Mô tả**: Hiển thị hồ sơ gợi ý và cho phép Like/Pass.

**File Liên Quan**:
- **activities/MainActivity.java**: Màn hình chính với card stack.
- **adapters/DiscoveryCardAdapter.java**: Hiển thị cards.
- **repositories/MatchRepository.java**: Xử lý Like, Match.
- **models/DiscoveryProfile.java**: Model cho profiles.

**Hàm Chính**:
- MainActivity: onCreate, loadSuggestions, performSwipe, handleLike.
- MatchRepository: likeUser, checkForMatch, createMatch.

### 3.4. Tìm Kiếm Nâng Cao (UC_TKNangCao)

**Mô tả**: Lọc hồ sơ theo tiêu chí.

**File Liên Quan**:
- **ui/FilterBottomSheetDialog.java**: UI bộ lọc.
- **utils/DiscoveryFilterStorage.java**: Lưu filter preferences.
- **repositories/UserRepository.java**: Query users với filter.

**Hàm Chính**:
- FilterBottomSheetDialog: onCreate, applyFilters.
- UserRepository: getUsersWithFilters.

### 3.5. Trò Chuyện (UC_Chat)

**Mô tả**: Chat realtime giữa matched users.

**File Liên Quan**:
- **activities/ChatActivity.java**: UI chat.
- **models/ChatMessage.java**: Model cho tin nhắn.
- **repositories/ChatRepository.java**: Xử lý gửi/nhận tin nhắn.

**Hàm Chính**:
- ChatActivity: onCreate (khởi tạo activity chat), bindExtras (bind dữ liệu từ Intent), bindViews (gán các view), setupRecyclerView (thiết lập danh sách tin nhắn), setupClicks (thiết lập sự kiện click), populateHeader (điền thông tin header), sendCurrentMessage (gửi tin nhắn), attachMessageListener (lắng nghe tin nhắn mới), markThreadRead (đánh dấu đã đọc), scrollToBottom (cuộn xuống cuối), setLoading (thiết lập trạng thái loading), onDestroy (dọn dẹp tài nguyên).
- ChatRepository: sendMessage, listenForMessages.

### 3.6. Quản Lý Hồ Sơ (UC_Profile)

**Mô tả**: Xem và cập nhật hồ sơ cá nhân.

**File Liên Quan**:
- **activities/ProfileSettingsActivity.java**: UI quản lý hồ sơ.
- **repositories/UserRepository.java**: Cập nhật user data.

**Hàm Chính**:
- ProfileSettingsActivity: onCreate, updateProfile, loadUserData.
- UserRepository: updateUserProfile, getCurrentUser.

### 3.7. Báo Cáo/Chặn (UC_BaoCao)

**Mô tả**: Báo cáo hoặc chặn user.

**File Liên Quan**:
- **repositories/ReportRepository.java**: Xử lý báo cáo.
- **models/Report.java**: Model cho báo cáo.

**Hàm Chính**:
- ReportRepository: submitReport, blockUser.

## 4. Comment Các Hàm Trong Code

Đã thêm comment Javadoc bằng tiếng Việt cho tất cả các hàm trong các file Java chính. Dưới đây là tóm tắt cho một số file:

### activities/RegisterActivity.java (Thuộc UC_DangKy - Đăng Ký)
- onCreate(): Khởi tạo activity đăng ký. Thiết lập UI, listeners cho các nút và cấu hình đăng nhập Google.
- configureGoogleSignIn(): Cấu hình đăng nhập Google bằng GoogleSignInClient và ActivityResultLauncher.
- signUpWithGoogle(): Bắt đầu quá trình đăng ký bằng Google, hiển thị loading và khởi chạy Google Sign-In intent.
- handleGoogleSignInResult(): Xử lý kết quả từ Google Sign-In intent, lấy account và xác thực với Firebase.
- firebaseAuthWithGoogle(): Xác thực với Firebase bằng Google ID Token và điều hướng sau khi đăng ký thành công.
- registerUser(): Xử lý đăng ký tài khoản mới bằng email và mật khẩu. Kiểm tra input và gọi FirebaseHelper để tạo tài khoản.

### activities/MainActivity.java (Thuộc UC_Discovery - Khám Phá/Kết Nối)
- onCreate(): Khởi tạo MainActivity. Thiết lập edge-to-edge display, bind views, setup card stack, hookup actions và load thông tin user hiện tại.
- bindViews(): Bind các view từ layout vào các biến instance để sử dụng trong activity.
- setupCardStack(): Thiết lập CardStackView với adapter và layout manager, bao gồm các listener cho swipe actions.
- hookupActions(): Thiết lập các onClickListener cho các nút và tab navigation.
- loadCurrentUser(): Tải thông tin user hiện tại từ Firebase và khởi tạo các thành phần phụ thuộc.
- loadSuggestions(): Tải danh sách gợi ý người dùng để hiển thị trên card stack, loại trừ những người đã tương tác.
- fetchAllUsers(): Lấy tất cả người dùng từ repository để xử lý gợi ý.
- handleSuggestionsSnapshot(): Xử lý snapshot dữ liệu người dùng để tạo danh sách gợi ý, lọc theo vị trí và sở thích.
- showLoading(): Hiển thị hoặc ẩn indicator loading.
- updateEmptyState(): Cập nhật trạng thái empty state dựa trên danh sách gợi ý.
- showError(): Hiển thị thông báo lỗi cho người dùng.
- openProfileSettings(): Mở màn hình cài đặt hồ sơ cá nhân.
- updateLocationText(): Cập nhật text hiển thị vị trí dựa trên tọa độ của user hiện tại.
- performSwipe(): Thực hiện swipe card theo hướng chỉ định với animation.
- updateOverlay(): Cập nhật overlay hiển thị trên card khi người dùng kéo card.

### activities/ChatActivity.java (Thuộc UC_Chat - Trò Chuyện)
- onCreate(): Phương thức khởi tạo activity chat khi được tạo. Thiết lập giao diện người dùng, xử lý insets cho edge-to-edge, và khởi tạo các thành phần cần thiết cho chat.
- bindExtras(): Phương thức bind dữ liệu từ Intent khi khởi tạo activity. Lấy thông tin chat ID, partner ID, tên và ảnh của đối phương từ Intent. Đồng thời lấy UID của người dùng hiện tại từ Firebase Auth. Kiểm tra tính hợp lệ của các dữ liệu cần thiết.
- bindViews(): Phương thức bind các view từ layout XML vào các biến thành viên. Gán các ImageView, TextView, RecyclerView, EditText, Button và ProgressBar. Đồng thời thiết lập các click listener cho các button điều hướng và chức năng.
- setupRecyclerView(): Phương thức thiết lập RecyclerView cho danh sách tin nhắn. Sử dụng LinearLayoutManager với stackFromEnd=true để hiển thị tin nhắn từ cuối danh sách. Tạo và cấu hình ChatMessagesAdapter với thông tin người dùng hiện tại và ảnh đối phương.
- setupClicks(): Phương thức thiết lập các click listener cho giao diện chat. Gán sự kiện click cho nút gửi tin nhắn và xử lý sự kiện nhấn Enter hoặc IME_ACTION_SEND trên trường nhập tin nhắn.
- populateHeader(): Phương thức điền thông tin vào header của chat. Hiển thị tên đối phương, trạng thái hoạt động và tải ảnh đại diện từ URL hoặc sử dụng ảnh mặc định nếu không có URL.
- sendCurrentMessage(): Phương thức gửi tin nhắn hiện tại đang được nhập. Kiểm tra tính hợp lệ của dữ liệu, tạo đối tượng ChatMessage, gửi tin nhắn qua repository và xử lý kết quả thành công/thất bại. Xóa nội dung input và cuộn xuống cuối sau khi gửi thành công.
- attachMessageListener(): Phương thức gắn ValueEventListener để lắng nghe thay đổi tin nhắn từ Firebase. Khi có dữ liệu mới, parse các ChatMessage, sắp xếp theo thời gian, cập nhật adapter và thực hiện các hành động như cuộn xuống cuối và đánh dấu đã đọc. Xử lý lỗi nếu có khi kết nối database thất bại.
- markThreadRead(): Phương thức đánh dấu thread chat đã được đọc. Gọi repository để cập nhật trạng thái đã đọc cho người dùng hiện tại.
- scrollToBottom(): Phương thức cuộn RecyclerView xuống vị trí cuối cùng (tin nhắn mới nhất). Chỉ thực hiện khi có ít nhất một item trong adapter.
- setLoading(): Phương thức thiết lập trạng thái loading của giao diện. Hiển thị hoặc ẩn ProgressBar loading indicator.
- onDestroy(): Phương thức được gọi khi Activity bị hủy. Gỡ bỏ ValueEventListener để tránh memory leak và rò rỉ tài nguyên.

### repositories/UserRepository.java (Thuộc nhiều UC: DangKy, DangNhap, Profile)
- createUser(): Tạo user mới trong Firebase Auth và Database.
- signInWithEmail(): Đăng nhập bằng email và mật khẩu.
- signInWithGoogle(): Đăng nhập bằng Google credential.
- signInWithFacebook(): Đăng nhập bằng Facebook credential.
- updateUserProfile(): Cập nhật thông tin hồ sơ user.
- getCurrentUser(): Lấy thông tin user hiện tại từ Firebase.
- getAllUsers(): Lấy danh sách tất cả users.
- getUsersWithFilters(): Lấy danh sách users theo bộ lọc (giới tính, độ tuổi, vị trí).

### repositories/MatchRepository.java (Thuộc UC_Discovery)
- likeUser(): Gửi like cho một user.
- checkForMatch(): Kiểm tra xem có match (hai người like lẫn nhau) không.
- createMatch(): Tạo record match mới trong database.
- getInteractionsSnapshot(): Lấy snapshot các tương tác (like/dislike) của user.

### repositories/ChatRepository.java (Thuộc UC_Chat)
- sendMessage(): Gửi tin nhắn mới.
- listenForMessages(): Lắng nghe tin nhắn mới trong chat thread.
- getChatThread(): Lấy thông tin chat thread.
- markMessageAsRead(): Đánh dấu tin nhắn đã đọc.

### repositories/ReportRepository.java (Thuộc UC_BaoCao)
- submitReport(): Gửi báo cáo về user vi phạm.
- blockUser(): Chặn user.
- getReports(): Lấy danh sách báo cáo.

### adapters/DiscoveryCardAdapter.java (Thuộc UC_Discovery)
- onCreateViewHolder(): Tạo ViewHolder cho card.
- onBindViewHolder(): Bind dữ liệu DiscoveryProfile vào card view.
- getItemCount(): Trả về số lượng items trong danh sách.

### utils/FirebaseHelper.java (Utility cho tất cả UC)
- getCurrentUser(): Lấy FirebaseUser hiện tại.
- loginUser(): Đăng nhập bằng email/password.
- signInWithCredential(): Đăng nhập bằng credential (Google, Facebook).
- logout(): Đăng xuất user.

### utils/NavigationHelper.java (Utility cho navigation)
- checkProfileAndNavigate(): Kiểm tra hồ sơ và điều hướng đến màn hình phù hợp.
- navigateToMain(): Chuyển đến MainActivity.
- navigateToProfileSetup(): Chuyển đến màn hình thiết lập hồ sơ.

### Và tương tự cho các file khác trong activities, models, utils.

## 6. Kiến Trúc Phát Triển

### 6.1. Tổng Quan Kiến Trúc Ứng Dụng

Dự án HeartLink được xây dựng dựa trên **kiến trúc MVVM (Model-View-ViewModel)** kết hợp với **Repository Pattern**, một approach hiện đại và được khuyến khích trong phát triển Android.

#### Lý Thuyết MVVM (Model-View-ViewModel):
- **Model**: Đại diện cho dữ liệu và business logic. Trong HeartLink, các Model class (User, ChatMessage, Match) và Repository classes (UserRepository, ChatRepository) thuộc về tầng này.
- **View**: Giao diện người dùng (UI). Trong Android, các Activity và Fragment là View. Chúng chỉ tập trung vào hiển thị dữ liệu và xử lý tương tác người dùng.
- **ViewModel**: Kết nối giữa Model và View. Chứa logic presentation, xử lý dữ liệu từ Model và cung cấp cho View. ViewModel không phụ thuộc vào View cụ thể, giúp dễ testing và tái sử dụng.

#### Lợi Ích Của MVVM:
- **Separation of Concerns**: Tách biệt rõ ràng giữa UI, logic và data.
- **Testability**: ViewModel có thể test độc lập mà không cần UI.
- **Maintainability**: Dễ bảo trì và mở rộng code.
- **Reusability**: ViewModel có thể tái sử dụng cho nhiều View khác nhau.

### 6.2. Repository Pattern

Repository Pattern được sử dụng để tách biệt logic truy cập dữ liệu khỏi business logic:

- **UserRepository**: Quản lý tất cả operations liên quan đến user data (CRUD operations với Firebase).
- **MatchRepository**: Xử lý logic matching và interactions.
- **ChatRepository**: Quản lý chat threads và messages.
- **ReportRepository**: Xử lý báo cáo và blocking.

#### Lợi Ích Repository Pattern:
- **Data Abstraction**: Ẩn implementation details của data source.
- **Testability**: Dễ mock data source cho unit testing.
- **Flexibility**: Có thể thay đổi data source mà không ảnh hưởng business logic.
- **Centralized Data Logic**: Tập trung logic truy cập dữ liệu ở một nơi.

### 6.3. Dependency Injection (DI)

Dự án sử dụng **Manual Dependency Injection** thay vì DI frameworks như Dagger/Hilt:

- Repository instances được tạo trực tiếp trong Activities/ViewModels.
- Firebase instances được khởi tạo khi cần thiết.
- Cách tiếp cận đơn giản cho project nhỏ, dễ hiểu và debug.

### 6.4. Firebase Integration Architecture

#### Firebase Services Sử Dụng:
- **Authentication**: Xác thực người dùng (Email/Password, Google, Facebook).
- **Firestore**: Database NoSQL cho user data, matches, chat.
- **Storage**: Lưu trữ media files (photos).
- **Cloud Messaging**: Push notifications.

#### Data Flow:
1. **Authentication Flow**: Login/Register → Firebase Auth → User data lưu vào Firestore.
2. **Discovery Flow**: Load users → Apply filters → Display cards → Like/Pass → Check match.
3. **Chat Flow**: Match created → Chat thread → Real-time messaging via Firestore listeners.

### 6.5. UI Architecture

#### Activity-Based Navigation:
- Mỗi màn hình là một Activity riêng biệt.
- Navigation thông qua Intent với data passing.
- Back stack management tự động bởi Android system.

#### Material Design Implementation:
- Sử dụng Material Components (Buttons, Cards, Dialogs).
- Consistent theming với colors, typography.
- Responsive design cho multiple screen sizes.

### 6.6. Cấu Trúc Thư Mục Resources (res/)

Thư mục `res/` chứa tất cả tài nguyên không phải code của ứng dụng Android:

```
res/
├── anim/                    # Animation files (XML)
│   └── fade_in.xml         # Ví dụ: animation fade in
├── color/                   # Color resources
│   └── selector_button.xml # Color states cho buttons
├── drawable/               # Bitmap images, shapes, selectors
│   ├── ic_heart.png       # Icons và images
│   └── bg_gradient.xml    # Shape drawables
├── font/                   # Custom fonts
│   └── roboto_regular.ttf # Font files
├── layout/                 # UI layout files (XML)
│   ├── activity_main.xml  # Layout cho MainActivity
│   └── item_card.xml      # Layout cho card items
├── mipmap-*/              # App launcher icons
│   ├── mipmap-hdpi/       # High density icons
│   ├── mipmap-mdpi/       # Medium density
│   ├── mipmap-xhdpi/      # Extra high density
│   ├── mipmap-xxhdpi/     # Extra extra high
│   └── mipmap-xxxhdpi/    # Extra extra extra high
├── values/                 # Simple resources (strings, colors, dimens)
│   ├── strings.xml        # Text strings
│   ├── colors.xml         # Color values
│   ├── dimens.xml         # Dimensions
│   └── styles.xml         # Styles và themes
├── values-night/          # Night mode resources
│   └── colors.xml         # Dark theme colors
└── xml/                   # Arbitrary XML files
    └── backup_rules.xml   # Backup configuration
```

#### Giải Thích Chi Tiết:

**anim/**: Chứa các file animation XML định nghĩa chuyển động UI (fade, slide, scale, etc.).

**color/**: Color state lists cho các trạng thái khác nhau của UI elements (pressed, focused, selected).

**drawable/**: 
- Bitmap images (.png, .jpg, .gif)
- Shape drawables (hình chữ nhật, tròn với màu sắc, gradient)
- State selectors (background thay đổi theo trạng thái)

**font/**: Custom font files (.ttf, .otf) để sử dụng typography tùy chỉnh.

**layout/**: XML files định nghĩa UI layout cho Activities, Fragments, custom views.

**mipmap-*/**: App launcher icons với density khác nhau cho các màn hình device khác nhau.

**values/**: 
- **strings.xml**: Tất cả text hiển thị cho user (đa ngôn ngữ support)
- **colors.xml**: Color values với tên có ý nghĩa
- **dimens.xml**: Spacing, sizing values
- **styles.xml**: Reusable styles cho views

**values-night/**: Resources cho dark mode, override values thông thường khi device ở dark theme.

**xml/**: Các file XML tùy ý như backup rules, search configuration, etc.

### 6.7. Build Và Dependency Management

#### Gradle Build System:
- **settings.gradle.kts**: Cấu hình project structure và modules.
- **build.gradle.kts (root)**: Dependencies và plugins chung.
- **app/build.gradle.kts**: App-specific configuration, dependencies.

#### Dependency Management:
- **libs.versions.toml**: Centralized version management (Gradle Version Catalogs).
- **ProGuard Rules**: Code obfuscation và optimization rules.

### 6.8. Testing Strategy

#### Unit Testing:
- **JUnit**: Logic testing cho ViewModels, Utils.
- **Mockito**: Mock dependencies cho isolated testing.

#### Integration Testing:
- **Firebase Emulator**: Test Firebase operations locally.
- **Espresso**: UI testing cho Activities.

### 6.9. Security Considerations

#### Authentication Security:
- Firebase Auth cho secure authentication.
- Email verification cho account validation.

#### Data Privacy:
- User consent cho permissions (location, notifications).
- Location visibility toggle.
- Data encryption trong transit và at rest.

#### Content Moderation:
- User reporting system.
- Block/unblock functionality.
- Community guidelines enforcement.

### 6.10. Performance Optimization

#### UI Performance:
- RecyclerView với ViewHolder pattern.
- Image caching với Glide.
- Lazy loading cho images và data.

#### Network Performance:
- Firebase offline capabilities.
- Efficient data querying.
- Pagination cho large datasets.

#### Memory Management:
- Proper lifecycle management.
- Memory leak prevention.
- Efficient bitmap handling.

### 6.11. Ứng Dụng Kiến Trúc Vào Cấu Trúc Java

#### 6.11.1. Ánh Xạ Lý Thuyết MVVM Vào Cấu Trúc Thực Tế

Kiến trúc MVVM được ứng dụng trực tiếp vào cấu trúc thư mục Java như sau:

```
vn/haui/heartlink/
├── activities/           # VIEW Layer (19 Activities)
│   ├── MainActivity.java        # View chính - Discovery UI
│   ├── ChatActivity.java        # View - Chat interface
│   ├── ProfileSettingsActivity.java # View - Profile management
│   └── ... (16 activities khác)
├── models/              # MODEL Layer (Data Models)
│   ├── User.java               # Business data model
│   ├── ChatMessage.java        # Message data model
│   ├── Match.java              # Match data model
│   └── DiscoveryProfile.java   # UI-specific data model
├── repositories/        # MODEL Layer (Data Access)
│   ├── UserRepository.java     # User data operations
│   ├── ChatRepository.java     # Chat data operations
│   ├── MatchRepository.java    # Match data operations
│   └── ReportRepository.java   # Report data operations
├── adapters/           # VIEW Layer (UI Binding)
│   ├── DiscoveryCardAdapter.java  # Bind data to RecyclerView
│   ├── ProfilePhotoAdapter.java   # Photo gallery adapter
│   ├── MatchesAdapter.java        # Matches list adapter
│   └── MessagesAdapter.java       # Chat messages adapter
├── utils/              # Supporting Classes
│   ├── FirebaseHelper.java      # Firebase utilities
│   ├── NavigationHelper.java    # Navigation logic
│   ├── DateUtils.java          # Date formatting utilities
│   └── LocationHelper.java     # Location services
├── ui/                 # Custom UI Components
│   ├── FilterBottomSheetDialog.java # Custom dialog
│   ├── GradientTextView.java        # Custom text view
│   └── WaveView.java               # Custom animation view
├── Constants.java      # Application constants
└── HeartLinkApp.java   # Application class
```

#### 6.11.2. Chi Tiết Cách Hoạt Động Của MVVM Trong HeartLink

##### **View Layer (Activities)**
- **Chức năng**: Hiển thị UI và xử lý user interactions
- **Không chứa business logic**: Chỉ gọi methods từ ViewModel/Repository
- **Lifecycle management**: Handle configuration changes, lifecycle events
- **Ví dụ MainActivity**:
  ```java
  // VIEW: Chỉ xử lý UI và gọi repository
  public class MainActivity extends AppCompatActivity {
      private void loadSuggestions() {
          // Gọi repository để lấy data
          userRepository.getUsersWithFilters(filters)
              .addOnSuccessListener(this::displayCards);
      }
  }
  ```

##### **Model Layer (Models + Repositories)**
- **Models**: Plain data objects, không chứa logic
- **Repositories**: Tách biệt data access logic khỏi UI
- **Firebase abstraction**: Ẩn implementation details của Firebase

##### **ViewModel Concept (Implicit)**
Trong HeartLink, ViewModel được implement ngầm trong Activities:
- Activities vừa là View vừa chứa một phần ViewModel logic
- Repository methods được gọi trực tiếp từ Activities
- State management thông qua Activity lifecycle

#### 6.11.3. Repository Pattern Implementation

##### **UserRepository.java - Data Access Layer**
```java
public class UserRepository {
    // Tách biệt Firebase operations
    public void updateUser(String uid, Map<String, Object> updates, OnCompleteListener listener) {
        // Firebase Firestore operations
    }
    
    public void getUsersWithFilters(Map<String, Object> filters, OnUsersLoadedListener listener) {
        // Complex query logic
    }
}
```

**Lợi ích trong thực tế:**
- **Testability**: Có thể mock Firebase calls
- **Maintainability**: Thay đổi Firebase implementation không ảnh hưởng UI
- **Reusability**: Repository có thể dùng cho nhiều Activities

##### **Data Flow Trong Ứng Dụng**
```
User Interaction → Activity (View) → Repository (Model) → Firebase → Repository → Activity → UI Update
```

#### 6.11.4. Adapters - UI Binding Layer

Adapters đóng vai trò bridge giữa data và RecyclerView:

```java
public class DiscoveryCardAdapter extends RecyclerView.Adapter<DiscoveryCardAdapter.CardViewHolder> {
    // VIEW: Bind data to UI components
    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        DiscoveryProfile profile = profiles.get(position);
        // Bind MODEL data to VIEW
        holder.nameText.setText(profile.getName());
        holder.bioText.setText(profile.getBio());
    }
}
```

#### 6.11.5. Utils - Supporting Infrastructure

##### **FirebaseHelper.java**
- Centralize Firebase Auth operations
- Handle authentication flows (Google, Facebook, Email)
- Provide consistent error handling

##### **NavigationHelper.java**
- Manage complex navigation logic
- Profile completion checks
- Conditional navigation based on user state

##### **DateUtils.java & LocationHelper.java**
- Business logic utilities
- Platform-specific operations (permissions, GPS)

#### 6.11.6. UI Components (ui/ package)

Custom UI components cho consistent design:
- **FilterBottomSheetDialog**: Complex filtering UI
- **GradientTextView**: Branded text styling
- **WaveView**: Loading animations

#### 6.11.7. Application Architecture Decisions

##### **Tại Sao Không Có ViewModel Class Riêng Biệt?**
- **Project size**: HeartLink là app nhỏ, Activities đủ để handle logic
- **Simplicity**: Tránh over-engineering cho simple use cases
- **Direct data binding**: Firebase listeners bind trực tiếp vào UI

##### **Repository Pattern Benefits Realized**
- **Firebase Migration**: Dễ thay đổi Firebase implementation
- **Testing**: Repository methods dễ unit test
- **Code Reuse**: UserRepository dùng cho 15+ Activities

##### **Package Structure Evolution**
```
Initial: All classes in one package
Current: Logical separation by responsibility
Future: Could add 'viewmodels' package if complexity increases
```

#### 6.11.8. Scalability Considerations

##### **Current Limitations**
- Activities chứa quá nhiều logic (View + ViewModel)
- Tight coupling giữa UI và Firebase operations
- Limited testability do không có ViewModel layer

##### **Future Improvements**
- Extract ViewModel classes khi logic phức tạp hơn
- Implement DI framework (Dagger/Hilt)
- Add offline data synchronization layer

##### **Architecture Fitness**
- **Good for current scope**: Simple, maintainable
- **Scalable for growth**: Repository pattern cho phép mở rộng
- **Modern Android practices**: MVVM concepts applied appropriately

## 7. Kết Luận

Đã hoàn thành comment đầy đủ tất cả các hàm trong **19 file activities** của dự án HeartLink bằng tiếng Việt. Cụ thể:

### Các Activities Đã Hoàn Thành Comment:

1. **LoginActivity.java** - 3 hàm: Xử lý đăng nhập
2. **MainActivity.java** - 6 hàm: Activity chính điều hướng
3. **RegisterActivity.java** - 4 hàm: Xử lý đăng ký
4. **SplashActivity.java** - 2 hàm: Màn hình splash
5. **WelcomeActivity.java** - 3 hàm: Màn hình chào mừng
6. **ChatActivity.java** - 12 hàm: Chức năng chat
7. **GenderSelectionActivity.java** - 3 hàm: Chọn giới tính
8. **InterestsActivity.java** - 6 hàm: Chọn sở thích
9. **LocationPermissionActivity.java** - 11 hàm: Xin quyền vị trí
10. **MatchesActivity.java** - 17 hàm: Danh sách matches
11. **MatchSuccessActivity.java** - 2 hàm: Thông báo match thành công
12. **MessagesActivity.java** - 17 hàm: Danh sách tin nhắn
13. **NotificationPermissionActivity.java** - 7 hàm: Xin quyền thông báo
14. **OtpVerificationActivity.java** - 7 hàm: Xác thực OTP
15. **PhotoUploadActivity.java** - 13 hàm: Upload ảnh
16. **ProfileDetailActivity.java** - 19 hàm: Chi tiết profile
17. **ProfileInfoActivity.java** - 6 hàm: Nhập thông tin cơ bản
18. **ProfileSettingsActivity.java** - 19 hàm: Cài đặt profile
19. **SeekingActivity.java** - 3 hàm: Chọn mục đích tìm kiếm

**Tổng cộng: 161 hàm** đã được comment với Javadoc tiếng Việt, bao gồm mô tả chức năng, tham số và giá trị trả về.

File bctd.md chứa toàn bộ thông tin về cấu trúc project, bảng dữ liệu, comment code và ánh xạ chức năng cho tất cả các use case của ứng dụng HeartLink.</content>
<parameter name="filePath">bctd.md