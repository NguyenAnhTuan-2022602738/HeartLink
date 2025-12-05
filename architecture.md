# HeartLink - Architecture Documentation

## 📋 Tổng quan dự án
**HeartLink** là ứng dụng Android hẹn hò (Dating App) được xây dựng với Firebase Backend, hỗ trợ đa ngôn ngữ (Tiếng Việt/Tiếng Anh), và tích hợp đầy đủ tính năng swipe, match, chat.

---

## 🏗️ Kiến trúc ứng dụng

### Architecture Pattern: **Modified MVVM + Repository Pattern**

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  Activities  │  │  Fragments   │  │   Adapters   │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│                     Business Layer                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Repositories │  │    Utils     │  │   Managers   │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│                       Data Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Firebase   │  │ SharedPrefs  │  │    Models    │  │
│  │   Realtime   │  │              │  │              │  │
│  │   Database   │  │              │  │              │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## 📁 Cấu trúc thư mục chi tiết

### **Java Source Code** (`app/src/main/java/vn/haui/heartlink/`)

```
vn.haui.heartlink/
│
├── 📄 HeartLinkApplication.java
│   └── Application class chính, khởi tạo Firebase, Emoji, Dark Mode, Localization
│
├── 📁 activities/ (16 files)
│   ├── MainActivity.java              → Màn hình chính với bottom navigation
│   ├── WelcomeActivity.java           → Màn hình chào mừng
│   ├── LoginActivity.java             → Đăng nhập
│   ├── RegisterActivity.java          → Đăng ký
│   ├── GenderSelectionActivity.java   → Chọn giới tính
│   ├── SeekingActivity.java           → Chọn mục đích hẹn hò
│   ├── InterestsActivity.java         → Chọn sở thích
│   ├── PhotoUploadActivity.java       → Tải ảnh lên
│   ├── LocationPermissionActivity.java → Yêu cầu quyền vị trí
│   ├── NotificationPermissionActivity.java → Yêu cầu quyền thông báo
│   ├── ProfileInfoActivity.java       → Nhập thông tin cá nhân
│   ├── ProfileDetailActivity.java     → Chi tiết hồ sơ người khác
│   ├── MatchSuccessActivity.java      → Màn hình ghép đôi thành công
│   └── ...
│
├── 📁 fragments/ (6 files)
│   ├── DiscoveryFragment.java         → Tab khám phá (swipe cards)
│   ├── MatchesFragment.java           → Tab danh sách matches
│   ├── MessagesFragment.java          → Tab tin nhắn
│   ├── ProfileFragment.java           → Tab hồ sơ cá nhân
│   ├── ChatBottomSheetFragment.java   → Bottom sheet chat
│   └── MatchesFilterBottomSheetFragment.java → Lọc matches
│
├── 📁 adapters/ (7 files)
│   ├── DiscoveryCardAdapter.java      → Adapter cho card stack swipe
│   ├── MatchesAdapter.java            → Adapter danh sách matches
│   ├── MessagesAdapter.java           → Adapter danh sách conversations
│   ├── ChatAdapter.java               → Adapter tin nhắn trong chat
│   ├── ProfilePhotoAdapter.java       → Adapter gallery ảnh
│   ├── UsersAdapter.java              → Adapter danh sách users (admin)
│   └── InterestChipAdapter.java       → Adapter chips sở thích
│
├── 📁 models/ (7 files)
│   ├── User.java                      → Model người dùng
│   ├── DiscoveryProfile.java          → Model cho discovery card
│   ├── FilterPreferences.java         → Model bộ lọc
│   ├── ChatMessage.java               → Model tin nhắn
│   ├── Conversation.java              → Model cuộc trò chuyện
│   └── ...
│
├── 📁 utils/ (12 files)
│   ├── UserRepository.java            → Repository quản lý users
│   ├── MatchRepository.java           → Repository quản lý matches
│   ├── ChatRepository.java            → Repository quản lý chat
│   ├── LikesNotificationManager.java  → Quản lý thông báo like
│   ├── MessagesNotificationManager.java → Quản lý thông báo message
│   ├── LocaleHelper.java              → Helper xử lý đa ngôn ngữ
│   ├── InterestMapper.java            → Mapper cho interests localization
│   ├── CloudinaryHelper.java          → Upload ảnh lên Cloudinary
│   └── ...
│
├── 📁 ui/ (4 files)
│   ├── GradientTextView.java          → Custom TextView với gradient
│   ├── FilterBottomSheetDialog.java   → Bottom sheet filter discovery
│   └── ...
│
└── 📁 admin/ (4 files)
    └── Admin related activities/fragments

```

### **Resources** (`app/src/main/res/`)

```
res/
│
├── 📁 layout/ (48 files)
│   ├── activity_main.xml
│   ├── fragment_discovery.xml
│   ├── item_discovery_card.xml
│   ├── dialog_in_app_notification.xml
│   └── ...
│
├── 📁 values/
│   ├── strings.xml                    → Strings tiếng Việt (mặc định)
│   ├── arrays.xml                     → Danh sách interests (VI)
│   ├── interest_keys.xml              → Keys cố định cho interests
│   ├── colors.xml
│   ├── themes.xml
│   ├── styles.xml
│   └── dimens.xml
│
├── 📁 values-en/
│   ├── strings.xml                    → Strings tiếng Anh
│   └── arrays.xml                     → Danh sách interests (EN)
│
├── 📁 values-night/
│   ├── colors.xml                     → Colors cho dark mode
│   └── themes.xml                     → Theme cho dark mode
│
├── 📁 drawable/ (159 files)
│   └── Icons, backgrounds, shapes
│
├── 📁 anim/ (6 files)
│   └── Animations (slide, fade, etc)
│
├── 📁 font/ (3 files)
│   └── Custom fonts (Montserrat)
│
└── 📁 menu/ (4 files)
    └── Bottom navigation, options menus
```

---

## 🔧 Các layer chính

### **1. Presentation Layer**

#### **Activities**
- Quản lý lifecycle và UI flow
- Navigation giữa các màn hình
- Không chứa business logic

#### **Fragments**
- Tái sử dụng UI components
- Nhẹ hơn Activities
- Sử dụng trong ViewPager, Bottom Navigation

#### **Adapters**
- RecyclerView adapters
- Binding data vào UI
- ViewHolder pattern

### **2. Business Layer**

#### **Repositories**
```java
UserRepository.getInstance()
  ├── getUserData(uid)
  ├── updateUser(uid, data)
  └── getAllUsers()

MatchRepository.getInstance()
  ├── likeUser(user, target, isSuperLike)
  ├── listenForIncomingLikes(uid, callback)
  └── removeInteraction(uid, targetUid)

ChatRepository.getInstance()
  ├── ensureDirectChat(uid1, uid2)
  ├── sendMessage(chatId, message)
  └── listenForNewMessages(uid, callback)
```

**Pattern:** Singleton pattern cho tất cả Repositories

#### **Utils & Helpers**
- `LocaleHelper`: Xử lý đa ngôn ngữ
- `InterestMapper`: Map interests keys ↔ display names
- `CloudinaryHelper`: Upload media
- Notification Managers: Tạo push notifications

### **3. Data Layer**

#### **Firebase Realtime Database Structure**
```
firebase/
├── Users/
│   └── {uid}/
│       ├── name
│       ├── email
│       ├── gender
│       ├── dateOfBirth
│       ├── interests: ["photography", "cooking", ...]
│       ├── photoUrls: [...]
│       ├── latitude, longitude
│       └── ...
│
├── Matches/
│   └── {uid}/
│       └── {partnerUid}/
│           ├── status: "matched" | "liked" | "received_like"
│           ├── type: "like" | "superlike"
│           ├── likedAt
│           └── matchedAt
│
└── Chats/
    └── {chatId}/
        ├── members: [uid1, uid2]
        └── messages/
            └── {messageId}/
                ├── senderId
                ├── text
                └── timestamp
```

#### **SharedPreferences**
```java
HeartLinkPrefs/
├── darkModeEnabled: boolean
├── language: "vi" | "en"
└── ...

FilterPrefs/
├── interestedIn: "male" | "female" | "both"
├── minAge, maxAge
├── maxDistance
└── ...
```

---

## 🌍 Đa ngôn ngữ (Localization)

### **Strategy: Resource-based + Runtime wrapping**

#### **1. String Resources**
```xml
values/strings.xml         → Tiếng Việt (default)
values-en/strings.xml      → Tiếng Anh
```

#### **2. Dynamic Localization**
```java
// Application level
HeartLinkApplication.onCreate() 
  → applyLanguageSetting()

// Runtime context wrapping (cho notifications)
LocaleHelper.wrapContext(context)
  → Wrap với locale từ SharedPreferences
```

#### **3. Interests Localization**
```
Database stores: ["photography", "cooking", "yoga"]
              ↓
InterestMapper.keysToDisplayNames()
              ↓
Display shows: ["Nhiếp ảnh", "Nấu ăn", "Yoga"] (VI)
          or: ["Photography", "Cooking", "Yoga"] (EN)
```

**Advantage:** Database consistent, UI follows user preference

---

## 🎯 Design Patterns sử dụng

### **1. Singleton Pattern**
```java
UserRepository.getInstance()
MatchRepository.getInstance()
ChatRepository.getInstance()
```

### **2. Repository Pattern**
- Tách biệt data access logic khỏi UI
- Single source of truth
- Dễ test và maintain

### **3. Observer Pattern**
```java
FirebaseDatabase.addValueEventListener()
  → Realtime updates
  → Auto UI refresh
```

### **4. Adapter Pattern**
- RecyclerView Adapters
- ViewHolder pattern
- DiffUtil cho performance

### **5. Builder Pattern**
```java
NotificationCompat.Builder()
  .setTitle()
  .setContentText()
  .build()
```

### **6. Callback Pattern**
```java
interface MatchResultCallback {
    void onLikeRecorded();
    void onMatchCreated();
    void onError(Exception e);
}
```

---

## 🔐 Security & Best Practices

### **1. Firebase Security Rules**
```javascript
// Users: Chỉ đọc public info, chỉ chủ sở hữu mới sửa
// Matches: Chỉ 2 người liên quan mới đọc/ghi
// Chats: Chỉ members mới truy cập
```

### **2. Data Privacy**
- Location có toggle visible/hidden
- Interests stored as keys (không expose raw data)
- Photos upload qua Cloudinary (secure URLs)

### **3. Performance**
- RecyclerView với ViewHolder pattern
- Glide cho image loading + caching
- Firebase pagination cho large lists
- DiffUtil cho efficient updates

---

## 📱 User Flow

```
Launch App
    ↓
Welcome Screen ────────→ Login/Register
    ↓
Gender Selection
    ↓
Profile Info (Name, DOB)
    ↓
Seeking Type Selection
    ↓
Interests Selection
    ↓
Photo Upload
    ↓
Location Permission
    ↓
Notification Permission
    ↓
Main App
    ├── 🔍 Discovery (Swipe)
    ├── 💕 Matches
    ├── 💬 Messages
    └── 👤 Profile
```

---

## 🚀 Tính năng chính

### **Discovery (Swipe)**
- CardStackView for swipe gestures
- Distance-based filtering
- Age, gender, interests filtering
- Like / Superlike / Pass
- Real-time match detection

### **Matching**
- Mutual likes → Instant match
- Notification on incoming likes
- Filter matches (All, Matched, Liked, Superliked)
- In-app notification dialog

### **Messaging**
- Direct chat between matches
- Real-time message sync
- Emoji support (EmojiManager)
- Message notifications

### **Profile**
- Complete profile setup
- Photo gallery
- Interests display (localized)
- Stats (Likes, Matches, Superlikes)
- Settings (Dark mode, Language, Notifications)

---

## 🛠️ Tech Stack

### **Core**
- **Language:** Java
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)

### **Architecture**
- Modified MVVM
- Repository Pattern
- Singleton Pattern

### **Backend**
- Firebase Realtime Database
- Firebase Authentication
- Firebase Cloud Storage (via Cloudinary)

### **UI Libraries**
- Material Design 3
- CardStackView (Swipe cards)
- CircleImageView
- Glide (Image loading)
- EmojiCompat (Emoji support)

### **Localization**
- Android Resources (values/values-en)
- Runtime locale switching
- Context wrapping for notifications

---

## 📊 Data Flow Example: Like Feature

```
1. User swipes right on DiscoveryFragment
        ↓
2. DiscoveryFragment.handleLike()
        ↓
3. MatchRepository.likeUser(currentUser, targetUser, false)
        ↓
4. Firebase: Update /Matches/{uid}/{targetUid}
        ↓
5a. If mutual → status: "matched"
    → Callback: onMatchCreated()
    → Show MatchSuccessActivity
        ↓
5b. If one-sided → status: "liked"
    → Callback: onLikeRecorded()
    → Toast: "Like sent!"
        ↓
6. Target user's listener (if online)
    → HeartLinkApplication.likesListener
    → LikesNotificationManager.showLikeNotification()
    → System notification appears
```

---

## 🎨 UI/UX Principles

1. **Material Design 3** guidelines
2. **Gradient themes** (colorPrimary, colorAccent)
3. **Dark mode** support
4. **Smooth animations** (fade, slide, scale)
5. **Responsive layouts** (ConstraintLayout, RecyclerView)
6. **Empty states** handling
7. **Loading states** (ProgressBar, shimmer)
8. **Error handling** (Snackbar, Toast)

---

## 📝 Naming Conventions

### **Java Classes**
- Activities: `*Activity.java` (e.g. `MainActivity.java`)
- Fragments: `*Fragment.java` (e.g. `DiscoveryFragment.java`)
- Adapters: `*Adapter.java` (e.g. `MatchesAdapter.java`)
- Models: `*.java` (e.g. `User.java`)
- Utils: `*Helper.java`, `*Manager.java`, `*Repository.java`

### **Resources**
- Layouts: `activity_*.xml`, `fragment_*.xml`, `item_*.xml`, `dialog_*.xml`
- IDs: `snake_case` (e.g. `user_avatar`, `matches_list`)
- Colors: `colorPrimary`, `textColorPrimary`
- Strings: `screen_component_description` (e.g. `matches_status_liked_you`)

---

## 🔄 State Management

### **User Session**
- Firebase Authentication (persistent)
- Auto login if session active

### **UI State**
- Fragment lifecycle-aware
- ViewModel pattern (minimal - mostly in Repositories)
- LiveData-like observers via Firebase listeners

### **App State**
- SharedPreferences for settings
- Application class for global state

---

## 📈 Future Improvements

1. **Architecture:**
   - Full MVVM with ViewModels
   - Dependency Injection (Hilt/Dagger)
   - Coroutines/RxJava for async

2. **Features:**
   - Voice/Video calls
   - Stories/Feed
   - Advanced matching algorithm
   - In-app purchases (premium features)

3. **Performance:**
   - Pagination for large lists
   - Image compression
   - Offline support

4. **Testing:**
   - Unit tests for Repositories
   - UI tests with Espresso
   - Integration tests

---

## 📄 License & Credits

**Project:** HeartLink - Dating Application  
**Author:** Nguyen Anh Tuan  
**Year:** 2024  
**University:** Hanoi University of Industry (HAUI)

---

**Document Version:** 1.0  
**Last Updated:** November 27, 2025  
**Generated by:** Antigravity AI Assistant