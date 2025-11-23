---
description: Sửa luồng thông báo like và match
---

# Kế hoạch sửa luồng thông báo

## Vấn đề cần giải quyết:

### 1. Tối ưu tải thông báo
✓ Đã được xử lý đúng - chỉ load người cuối cùng like
- Cần cải thiện message: "A và X người khác đã thích bạn"

### 2. Nút "Xem hồ sơ" chưa hoạt động
- Code đã có tại `MainActivity.showInAppNotification()` line 242-244
- Cần verify hoạt động đúng với ProfileDetailActivity

### 3. Luồng match không đúng **[VẤN ĐỀ CHÍNH]**

**Hiện tại:**
- A like B → B nhận dialog ✓
- B like lại A → Cả A và B đều nhận `MatchSuccessActivity` ✗

**Yêu cầu:**
- A like B → B nhận `dialog_in_app_notification` ✓
- B like lại A:
  - B (người swipe): hiển thị `activity_match_success.xml` ✓
  - A (người bị liked): hiển thị `dialog_in_app_notification.xml` (chứ không phải MatchSuccessActivity)

## Giải pháp:

### Bước 1: Cải thiện message trong MainActivity
- File: `MainActivity.java`
- Method: `setupInAppNotificationListener()` line 189-194
- Sửa message để hiển thị "A và X người khác đã thích bạn"

### Bước 2: Verify nút "Xem hồ sơ"
- File: `MainActivity.java`
- Method: `showInAppNotification()` line 241-244
- Kiểm tra ProfileDetailActivity có nhận đúng userId không

### Bước 3: Sửa luồng match - KEY CHANGE
**Vấn đề:** Hiện tại cả 2 bên đều nhận MatchSuccessActivity

**Giải pháp:**
1. Trong `MainActivity.setupInAppNotificationListener()`:
   - Khi detect match mới, kiểm tra xem match này có phải do current user khởi tạo không
   - Nếu KHÔNG phải (tức là đối phương vừa like lại mình) → hiển thị dialog
   - Nếu có (tức là mình vừa like) → bỏ qua (vì DiscoveryFragment sẽ xử lý)

2. Trong `DiscoveryFragment.processSwipeAction()`:
   - Callback `onMatchCreated()` → hiển thị MatchSuccessActivity (người swipe)
   - MainActivity listener → hiển thị dialog (người nhận match)

**Code changes:**
- MainActivity line 182-183: Khi có match mới, check `selfInitiatedMatches`
- Nếu match do mình tạo → skip dialog (DiscoveryFragment sẽ show MatchSuccessActivity)
- Nếu match do đối phương → show dialog với message "đã ghép đôi với bạn!"

### Bước 4: Test luồng
1. User A like User B → User B nhận dialog "A vừa thích bạn"
2. User B ấn "Xem hồ sơ" → Mở ProfileDetailActivity
3. User B like lại User A:
   - User B → Hiển thị MatchSuccessActivity
   - User A → Hiển thị dialog "B đã ghép đôi với bạn!"

## Chi tiết code changes:

### MainActivity.setupInAppNotificationListener()
```java
// Line 182-196: Sửa logic hiển thị match notification
if (latestMatch != null) {
    // CHỈ hiển thị dialog nếu match KHÔNG do mình khởi tạo
    if (!selfInitiatedMatches.contains(latestMatch.getPartnerId())) {
        fetchUserAndShowNotification(latestMatch.getPartnerId(), "đã ghép đôi với bạn!", "Nhắn tin");
    }
    // Xóa khỏi set sau khi xử lý
    selfInitiatedMatches.remove(latestMatch.getPartnerId());
}
```

### MainActivity.showInAppNotification()
- Verify code line 241-244 hoạt động đúng
- ProfileDetailActivity có nhận được userId qua Constants.EXTRA_USER_ID

### Message formatting
Line 189-194: Cải thiện message cho likes
```java
String message;
if (newLikes.size() > 1) {
    message = String.format("%s và %d người khác đã thích bạn", latestLike.getDisplayName(), newLikes.size() - 1);
} else {
    message = "đã thích bạn";
}
```

## Notes:
- `selfInitiatedMatches` Set được quản lý qua `onMatchCreatedByUser()` callback
- DiscoveryFragment gọi `navigationListener.onMatchCreatedByUser()` trong `launchMatchSuccess()`
- MainActivity implement interface này và add partnerId vào Set
