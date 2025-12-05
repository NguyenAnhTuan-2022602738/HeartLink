# HeartLink - Cấu Trúc Thư Mục & Vai Trò

## 📂 Thư mục Java (`app/src/main/java/vn/haui/heartlink/`)

| Thư mục / Tệp | Vai trò |
|---------------|---------|
| **activities/** | Chứa 16 màn hình chính của ứng dụng: WelcomeActivity (màn hình chào mừng), LoginActivity/RegisterActivity (đăng nhập/đăng ký), GenderSelectionActivity (chọn giới tính), SeekingActivity (chọn mục đích hẹn hò), InterestsActivity (chọn sở thích), PhotoUploadActivity (tải ảnh), LocationPermissionActivity/NotificationPermissionActivity (xin quyền), ProfileInfoActivity (nhập thông tin cá nhân), MainActivity (màn hình chính với bottom navigation), ProfileDetailActivity (xem hồ sơ người khác), MatchSuccessActivity (thông báo ghép đôi thành công). |
| **fragments/** | Chứa 6 Fragment chính được sử dụng trong MainActivity: DiscoveryFragment (tab khám phá với tính năng swipe cards), MatchesFragment (tab danh sách người đã match và thích bạn), MessagesFragment (tab tin nhắn - danh sách cuộc trò chuyện), ProfileFragment (tab hồ sơ cá nhân với settings), ChatBottomSheetFragment (bottom sheet để chat trực tiếp), MatchesFilterBottomSheetFragment (bottom sheet lọc danh sách matches). |
| **adapters/** | Quản lý việc hiển thị danh sách dữ liệu trong RecyclerView: DiscoveryCardAdapter (adapter cho CardStackView - swipe cards), MatchesAdapter (danh sách matches với header sections), MessagesAdapter (danh sách conversations), ChatAdapter (tin nhắn trong chat), ProfilePhotoAdapter (gallery ảnh trong profile), UsersAdapter (danh sách users cho admin), InterestChipAdapter (chips sở thích). |
| **models/** | Định nghĩa 7 lớp dữ liệu chính: User (thông tin người dùng với tất cả fields: uid, name, email, gender, dateOfBirth, interests, photoUrls, location, bio, seekingType...), DiscoveryProfile (model tối ưu cho discovery cards), FilterPreferences (bộ lọc tìm kiếm: age range, distance, gender), ChatMessage (tin nhắn: senderId, text, timestamp), Conversation (cuộc trò chuyện: members, lastMessage), và các model hỗ trợ khác. |
| **utils/** | Chứa 12 lớp tiện ích quan trọng: **UserRepository** (quản lý CRUD users với Firebase), **MatchRepository** (xử lý logic like/match/superlike, listeners cho incoming likes), **ChatRepository** (quản lý chat và messages real-time), **CloudinaryHelper** (upload ảnh lên Cloudinary), **LikesNotificationManager** (tạo push notification khi có người thích), **MessagesNotificationManager** (notification cho tin nhắn mới), **LocaleHelper** (xử lý đa ngôn ngữ, wrap context cho correct locale), **InterestMapper** (chuyển đổi interests keys ↔ display names), và các helper khác cho validation, formatting. |
| **ui/** | Chứa 4 custom UI components: GradientTextView (TextView với hiệu ứng gradient màu), FilterBottomSheetDialog (bottom sheet filter cho discovery), ChatBottomSheetFragment (bottom sheet chat), và các custom views khác. |
| **admin/** | Chứa 4 file liên quan đến tính năng admin: quản lý users, xem thống kê, kiểm duyệt nội dung. |
| **HeartLinkApplication.java** | Lớp Application chính của ứng dụng. Khởi tạo và cấu hình: Firebase (Auth, Realtime Database), EmojiManager (hỗ trợ emoji trong chat), Dark Mode setting (apply theme toàn app), Language setting (đa ngôn ngữ Vi/En), Background listeners (lắng nghe incoming likes và messages real-time để tạo notifications), User online status tracking (cập nhật trạng thái online/offline). Đây là entry point đầu tiên khi app khởi động. |
| **Constants.java** | Định nghĩa các hằng số dùng chung trong app: notification channel IDs, request codes, intent extras, shared preferences keys. |

---

## 📂 Thư mục Resources (`app/src/main/res/`)

| Thư mục / Tệp | Vai trò |
|---------------|---------|
| **layout/** | Chứa 48 tệp XML định nghĩa giao diện của từng màn hình và component: **activity_*.xml** (layout cho Activities như activity_main.xml, activity_login.xml, activity_welcome.xml...), **fragment_*.xml** (layout cho Fragments như fragment_discovery.xml, fragment_matches.xml...), **item_*.xml** (layout cho RecyclerView items như item_discovery_card.xml, item_match_card.xml, item_message.xml...), **dialog_*.xml** (layout cho dialogs như dialog_in_app_notification.xml, dialog_filter.xml), **layout_bottom_sheet_*.xml** (bottom sheet layouts). Tất cả đều sử dụng Material Design 3 components và ConstraintLayout để responsive. |
| **drawable/** | Chứa 159+ tệp vector drawables và shape resources: **ic_*.xml** (icons cho bottom navigation, actions, interests như ic_home_nav_discover.xml, ic_heart_filled.xml, ic_interest_camera.xml...), **bg_*.xml** (backgrounds với gradient, rounded corners như bg_button_gradient.xml, bg_card.xml...), **shape_*.xml** (custom shapes), **selector_*.xml** (state selectors cho buttons), và các PNG images nếu cần. Sử dụng vector graphics để tối ưu kích thước và scale trên mọi màn hình. |
| **values/** | Thư mục chứa các resource values cho **Tiếng Việt (mặc định)**: **strings.xml** (300+ strings cho toàn bộ UI text: titles, buttons, messages, notifications...), **arrays.xml** (danh sách interests bằng tiếng Việt: "Nhiếp ảnh", "Mua sắm", "Nấu ăn"...), **interest_keys.xml** (keys cố định cho interests: "photography", "cooking"... - lưu vào database), **colors.xml** (định nghĩa màu sắc: colorPrimary, colorAccent, textColors...), **themes.xml** (AppTheme với Material Design 3), **styles.xml** (custom styles cho buttons, text, cards), **dimens.xml** (spacing, margins, sizes chuẩn). |
| **values-en/** | Thư mục chứa resource values cho **Tiếng Anh**: **strings.xml** (bản dịch tiếng Anh của tất cả strings), **arrays.xml** (interests bằng tiếng Anh: "Photography", "Shopping", "Cooking"...). Khi user chọn English trong settings, app tự động load resources từ thư mục này. |
| **values-night/** | Resources cho **Dark Mode**: **colors.xml** (màu tối cho dark theme: backgrounds tối, text sáng), **themes.xml** (DarkTheme extends từ AppTheme). Auto activate khi user bật Dark Mode trong settings hoặc theo system setting. |
| **anim/** | Chứa 6 tệp animation XML cho transitions mượt mà: **slide_in_right.xml / slide_out_left.xml** (chuyển màn hình sang phải/trái), **fade_in.xml / fade_out.xml** (hiệu ứng mờ dần), **scale_up.xml / scale_down.xml** (phóng to/thu nhỏ), **card_flip.xml** (lật card). Sử dụng trong Activity transitions và View animations. |
| **menu/** | Chứa 4 tệp định nghĩa menu: **bottom_navigation_menu.xml** (4 tabs: Discovery, Matches, Messages, Profile với icons và titles), **options_menu.xml** (menu 3 chấm nếu có), **filter_menu.xml** (options trong filter dialog), **chat_menu.xml** (actions trong chat). |
| **mipmap-*/** | Chứa launcher icon của app với nhiều độ phân giải: **mipmap-mdpi** (48x48dp), **mipmap-hdpi** (72x72dp), **mipmap-xhdpi** (96x96dp), **mipmap-xxhdpi** (144x144dp), **mipmap-xxxhdpi** (192x192dp), **mipmap-anydpi-v26** (adaptive icon cho Android 8.0+). Icon có foreground (logo HeartLink) và background layers để adaptive trên các launchers khác nhau. |
| **font/** | Chứa 3 font files custom: **montserrat_regular.ttf** (font chính cho text thường), **montserrat_bold.ttf** (font cho headings và buttons), **montserrat_semibold.ttf** (font cho emphasis text). Font Montserrat được chọn vì clean, modern và dễ đọc. |
| **xml/** | Chứa 2 tệp XML đặc biệt: **network_security_config.xml** (cấu hình HTTPS và certificate pinning để bảo mật), **backup_rules.xml** (quy tắc backup dữ liệu lên Google Drive). |

---

## 📂 Thư mục Manifest & Gradle

| Tệp | Vai trò |
|-----|---------|
| **AndroidManifest.xml** | File manifest chính khai báo: tất cả Activities và receivers, permissions cần thiết (INTERNET, ACCESS_FINE_LOCATION, POST_NOTIFICATIONS, CAMERA, READ_EXTERNAL_STORAGE...), application class (HeartLinkApplication), launcher activity (WelcomeActivity), intent filters, network security config, backup rules. Đây là file bắt buộc để Android biết cách chạy app. |
| **build.gradle (app)** | File Gradle cấu hình build cho module app: **SDK versions** (minSdk 24, targetSdk 34, compileSdk 34), **dependencies** (Firebase BOM, Material Design, Glide, CardStackView, CircleImageView, EmojiCompat...), **buildTypes** (debug/release với proguard rules), **compileOptions** (Java 8), **viewBinding** enabled. |
| **build.gradle (project)** | File Gradle cấu hình cho toàn project: **repositories** (Google, Maven Central), **classpath** cho plugins (Android Gradle Plugin, Google Services), **Kotlin version** nếu dùng. |
| **google-services.json** | File cấu hình Firebase: chứa API keys, project ID, app ID để kết nối với Firebase services (Authentication, Realtime Database, Cloud Storage, FCM). File này được download từ Firebase Console và **KHÔNG được commit lên Git** vì chứa sensitive data. |
| **proguard-rules.pro** | Quy tắc ProGuard cho release build: giữ lại các class Firebase, Glide, model classes, ngăn obfuscation gây lỗi runtime. Giúp shrink code và bảo mật khi release. |

---

## 📂 Cấu Trúc Firebase Realtime Database

| Node | Vai trò |
|------|---------|
| **/Users/{uid}/** | Lưu trữ thông tin đầy đủ của mỗi người dùng: profile fields (name, email, gender, dateOfBirth, bio, seekingType), interests array (keys: "photography", "cooking"...), photoUrls array (Cloudinary URLs), location (latitude, longitude, city), online status (online, lastSeen), stats (createdAt, superlikesRemaining). Read: public (một số fields), Write: chỉ owner. |
| **/Matches/{uid}/{partnerUid}/** | Lưu trữ quan hệ like/match giữa 2 users: **status** ("liked" = one-sided, "matched" = mutual, "received_like" = nhận like), **type** ("like" hoặc "superlike"), **timestamps** (likedAt, matchedAt), **cached data** (displayName, photoUrl để hiển thị nhanh). Mỗi user có node riêng chứa tất cả interactions của họ. Read/Write: chỉ 2 người liên quan. |
| **/Chats/{chatId}/** | Lưu metadata của mỗi cuộc trò chuyện: **members** array (2 UIDs), **timestamps** (createdAt, lastMessageAt), **last message info** (lastMessage text, lastSenderId). ChatId format: "uid1_uid2" (sorted alphabetically) để đảm bảo unique và consistent. Read/Write: chỉ members. |
| **/Chats/{chatId}/messages/{messageId}/** | Lưu từng tin nhắn trong chat: **senderId** (UID người gửi), **text** (nội dung), **timestamp** (thời gian gửi), **read** (đã đọc chưa). Messages được sort theo timestamp. Real-time listeners tự động update UI khi có tin nhắn mới. Write: chỉ members, Read: chỉ members. |

---

## 📂 Shared Preferences (Local Storage)

| Preference File | Keys | Vai trò |
|----------------|------|---------|
| **HeartLinkPrefs** | `darkModeEnabled` (boolean), `language` (String: "vi" hoặc "en"), `notificationsEnabled` (boolean) | Lưu settings của user: Dark Mode on/off, ngôn ngữ hiện tại (Vietnamese/English), bật/tắt notifications. Persist settings ngay cả khi đóng app. Load khi app start để apply đúng theme và language. |
| **FilterPrefs** | `interestedIn` (String: "male"/"female"/"both"), `minAge` (int), `maxAge` (int), `maxDistance` (int: km), `showMe` (boolean) | Lưu bộ lọc tìm kiếm của user trong Discovery tab: giới tính quan tâm, độ tuổi min/max, khoảng cách tối đa, ẩn/hiện profile của mình. Local only vì là preference cá nhân, không cần sync cross-device. |

---

## 🎯 Luồng Dữ Liệu Chính

### Discovery → Like → Match → Chat

```
1. DiscoveryFragment load candidates từ UserRepository
   → Filter theo FilterPreferences (age, gender, distance)
   → Exclude users đã liked/matched (query từ MatchRepository)
   → Display trong CardStackView (max 50 cards)

2. User swipe right (Like) hoặc tap Superlike
   → DiscoveryFragment.handleLike()
   → MatchRepository.likeUser(currentUser, targetUser, isSuperlike)
   → Write to /Matches/{currentUid}/{targetUid}
   → Check mutual like từ /Matches/{targetUid}/{currentUid}

3. Nếu mutual match:
   → Update status = "matched" cho cả 2 bên
   → ChatRepository.ensureDirectChat() tạo chatId
   → Create /Chats/{chatId} với members array
   → Show MatchSuccessActivity (celebration UI)
   → Notification đến target user (via FCM)

4. User tap "Wave" hoặc "Send Message"
   → Navigate to ChatBottomSheetFragment
   → ChatRepository.sendMessage(chatId, text)
   → Write to /Chats/{chatId}/messages/{pushKey}
   → Real-time listener update UI cho cả 2 bên
   → Notification nếu partner offline
```

---

## 🔧 Libraries & Dependencies Chính

| Library | Version | Mục đích |
|---------|---------|----------|
| **Firebase BOM** | 32.3.1 | Quản lý versions của tất cả Firebase libraries |
| **Firebase Auth** | - | Xác thực user (Email/Password, Google Sign-In) |
| **Firebase Database** | - | Realtime Database cho sync data real-time |
| **Material Components** | 1.10.0 | Material Design 3 components (Button, Card, TextField...) |
| **Glide** | 4.16.0 | Load và cache images từ URLs, hiệu quả cho RecyclerView |
| **CardStackView** | 2.3.4 | Swipeable card stack cho Discovery feature (Tinder-like) |
| **CircleImageView** | 3.1.0 | Hiển thị avatar hình tròn |
| **EmojiCompat** | 1.4.0 | Hỗ trợ emoji trong chat messages |
| **Cloudinary Android** | 2.5.0 | Upload ảnh lên Cloudinary CDN |
| **RecyclerView** | 1.3.1 | Hiển thị danh sách scrollable hiệu quả |
| **ConstraintLayout** | 2.1.4 | Layout responsive, flexible cho mọi màn hình |

---

## 📱 Permissions Yêu Cầu

| Permission | Bắt buộc? | Mục đích |
|------------|-----------|----------|
| `INTERNET` | ✅ Bắt buộc | Connect Firebase, load images từ Cloudinary |
| `ACCESS_FINE_LOCATION` | ❌ Optional | Lấy vị trí GPS chính xác để tính distance giữa users |
| `ACCESS_COARSE_LOCATION` | ❌ Optional | Lấy vị trí xấp xỉ (fallback nếu không có FINE) |
| `POST_NOTIFICATIONS` | ❌ Optional | Hiển thị push notifications (Android 13+) |
| `CAMERA` | ❌ Optional | Chụp ảnh trực tiếp cho profile pictures |
| `READ_EXTERNAL_STORAGE` | ❌ Optional | Chọn ảnh từ gallery (Android 12 trở xuống) |
| `READ_MEDIA_IMAGES` | ❌ Optional | Chọn ảnh từ gallery (Android 13+) |

**Runtime Permissions:** Location, Camera, Storage/Media được request khi cần (không request all at once). User có thể từ chối và vẫn dùng app (nhập location thủ công, skip photos).

---

## 🎨 Theming & Styling

### Color Palette

| Color | Light Mode | Dark Mode | Sử dụng |
|-------|-----------|-----------|---------|
| Primary | `#E94057` (Gradient Pink) | `#E94057` | Buttons, active icons, accents |
| Accent | `#8A2BE2` (Purple) | `#8A2BE2` | Gradient endpoints, highlights |
| Background | `#F8F8F8` | `#1A1A1A` | Screen backgrounds |
| Surface | `#FFFFFF` | `#2D2D2D` | Cards, dialogs |
| Text Primary | `#333333` | `#FFFFFF` | Headings, important text |
| Text Secondary | `#808080` | `#B0B0B0` | Subtitles, hints |

### Typography

| Style | Font | Size | Usage |
|-------|------|------|-------|
| Headline | Montserrat Bold | 28sp | Screen titles |
| Title | Montserrat SemiBold | 22sp | Card titles, dialog titles |
| Body | Montserrat Regular | 16sp | Normal text, messages |
| Caption | Montserrat Regular | 14sp | Timestamps, metadata |

---

**Document Version:** 1.0  
**Last Updated:** November 27, 2025  
**Project:** HeartLink Dating Application
