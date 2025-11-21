### Mô tả bài toán

Trong bối cảnh công nghệ số phát triển mạnh mẽ, việc kết nối và giao lưu giữa con người thông qua các nền tảng trực tuyến đã trở thành một phần tất yếu của cuộc sống hiện đại. Những ứng dụng mạng xã hội và hẹn hò như Facebook Dating, Tinder hay Bumble đã chứng minh được tiềm năng to lớn trong việc giúp người dùng tìm kiếm bạn bè, đối tượng phù hợp và mở rộng các mối quan hệ. Tuy nhiên, các nền tảng này thường tích hợp quá nhiều tính năng phức tạp, mang nặng yếu tố thương mại hoặc chưa thật sự tối ưu cho những người chỉ đơn giản muốn kết bạn, trò chuyện và chia sẻ cảm xúc một cách an toàn, thân thiện.

Từ thực tế đó, nhóm chúng tôi đề xuất xây dựng HeartLink - một ứng dụng di động kết bạn và kết nối cảm xúc, được phát triển hoàn toàn bằng ngôn ngữ Java với giao diện thiết kế XML trên nền tảng Android. Ứng dụng được lấy cảm hứng từ các nền tảng nổi tiếng như Tinder và Facebook Dating nhưng được tối giản hóa về chức năng, tối ưu trải nghiệm người dùng, đồng thời đảm bảo tính riêng tư và an toàn thông tin cá nhân. HeartLink hướng đến mục tiêu trở thành một nền tảng thuần Việt, giúp người dùng dễ dàng tìm kiếm và giao lưu với những người có cùng sở thích, cùng khu vực, và cùng nhịp cảm xúc trong cuộc sống.

Khi người dùng bắt đầu sử dụng ứng dụng, họ sẽ trải qua quá trình đăng ký và thiết lập hồ sơ cá nhân. Việc đăng ký có thể thực hiện bằng email hoặc số điện thoại thông qua Firebase Authentication nhằm đảm bảo tính xác thực. Sau khi đăng ký, người dùng sẽ được hướng dẫn hoàn thiện hồ sơ cá nhân bao gồm họ tên, giới tính, độ tuổi, nghề nghiệp, sở thích, mô tả ngắn gọn về bản thân và ảnh đại diện. Ứng dụng cho phép người dùng tùy chọn bật hoặc tắt chia sẻ vị trí GPS để đảm bảo quyền riêng tư. Tất cả dữ liệu người dùng sẽ được lưu trữ an toàn trên Firebase Realtime Database, còn hình ảnh sẽ được quản lý thông qua Firebase Storage.

Trung tâm của HeartLink là tính năng gợi ý kết nối (Matching), hoạt động dựa trên thuật toán gợi ý thông minh được xây dựng từ các tiêu chí như vị trí địa lý, giới tính, độ tuổi và sở thích. Khi mở ứng dụng, người dùng sẽ được hiển thị danh sách các hồ sơ được đề xuất dưới dạng thẻ lật (CardView). Mỗi thẻ bao gồm ảnh đại diện, tên, tuổi, nghề nghiệp, khoảng cách, và một số sở thích nổi bật. Người dùng có thể vuốt phải (Like) để thể hiện sự quan tâm hoặc vuốt trái (Pass) để bỏ qua. Khi cả hai người cùng Like nhau, hệ thống sẽ tự động tạo một kết nối (Match), và người dùng có thể bắt đầu trò chuyện với nhau trong mục Tin nhắn (Chat).

Tính năng Chat Realtime trong HeartLink cho phép hai người đã Match có thể trò chuyện với nhau ngay lập tức. Giao diện chat được thiết kế tối giản, hiện đại, hỗ trợ gửi tin nhắn văn bản, emoji, hình ảnh và hiển thị trạng thái online, đã xem. Tất cả dữ liệu trò chuyện được đồng bộ thời gian thực thông qua Firebase Realtime Database. Ngoài ra, người dùng có thể xóa cuộc trò chuyện, báo cáo người dùng vi phạm hoặc chặn tài khoản khi cần thiết. Mỗi khi có tin nhắn mới, lượt Like hoặc Match mới, hệ thống sẽ gửi thông báo đến người dùng qua Firebase Cloud Messaging (FCM), giúp họ không bỏ lỡ bất kỳ tương tác nào.

Bên cạnh các tính năng chính, HeartLink còn cho phép người dùng tùy chỉnh bộ lọc tìm kiếm nâng cao, giúp họ dễ dàng tìm được đối tượng phù hợp. Các tiêu chí lọc bao gồm giới tính, độ tuổi, sở thích chung và khoảng cách địa lý. Người dùng cũng có thể điều chỉnh bán kính tìm kiếm, bật hoặc tắt hiển thị vị trí và giới hạn thông tin cá nhân để tăng cường bảo mật. Mọi thao tác được thiết kế trực quan, giúp người dùng dù không quen với công nghệ vẫn có thể sử dụng dễ dàng.

Để đảm bảo môi trường sử dụng an toàn và tích cực, HeartLink tích hợp hệ thống quản trị viên (Admin Panel) với nhiều tính năng quản lý mạnh mẽ. Quản trị viên có thể theo dõi và quản lý người dùng, xử lý các báo cáo vi phạm, khóa hoặc xóa tài khoản vi phạm quy định, và gửi thông báo hệ thống. Ngoài ra, hệ thống còn có khả năng thống kê hoạt động người dùng theo ngày, tuần, tháng như số lượng người dùng mới, lượt Match, số cuộc trò chuyện được tạo ra, hay tỷ lệ báo cáo vi phạm. Các thống kê này giúp đội ngũ quản lý hiểu rõ hơn về tình hình vận hành hệ thống và tối ưu trải nghiệm người dùng.

Về mặt kỹ thuật, HeartLink được xây dựng theo kiến trúc Client - Cloud - Admin, trong đó:

- Client là ứng dụng Android viết bằng Java và XML sử dụng Material Design để hiển thị giao diện trực quan, hiện đại.
- Cloud được triển khai trên Firebase, đảm nhiệm các chức năng xác thực, lưu trữ, xử lý và truyền dữ liệu realtime giữa người dùng.
- Admin là tầng quản trị dùng để giám sát, phân quyền và xử lý các yêu cầu hệ thống.

Các thành phần kỹ thuật chính bao gồm:

- Firebase Authentication: xác thực tài khoản và đăng nhập bảo mật.
- Firebase Realtime Database: lưu trữ và đồng bộ dữ liệu người dùng, Match và tin nhắn.
- Firebase Storage: lưu ảnh đại diện và hình ảnh trò chuyện.
- Firebase Cloud Messaging (FCM): gửi thông báo realtime đến người dùng.
- Google Location Services (GPS): cung cấp vị trí địa lý chính xác cho tính năng gợi ý.

Dữ liệu người dùng được tổ chức trong Firebase dưới các node chính như:

- Users: lưu thông tin hồ sơ cá nhân (tên, tuổi, ảnh, vị trí, sở thích).
- Matches: lưu các cặp đã được kết nối.
- Chats: lưu toàn bộ tin nhắn giữa hai người.
- Reports: lưu các báo cáo vi phạm.

Toàn bộ hệ thống tuân thủ các quy tắc bảo mật nghiêm ngặt: dữ liệu truyền tải được mã hóa qua giao thức HTTPS, vị trí chỉ được chia sẻ khi người dùng cho phép, và quyền truy cập được phân cấp theo vai trò.

Về trải nghiệm người dùng, HeartLink được tối ưu hóa cho văn hóa và thói quen sử dụng tại Việt Nam. Thay vì tập trung vào mục tiêu hẹn hò, ứng dụng hướng tới việc kết nối bạn bè, mở rộng mạng lưới xã hội, đồng thời đảm bảo an toàn và riêng tư cho người dùng. Giao diện vuốt (Swipe UI) được thiết kế với tông màu nhẹ nhàng, biểu tượng rõ ràng, thao tác mượt mà và cảm giác tương tác thực tế. Mỗi lượt vuốt mang lại phản hồi trực quan giúp người dùng cảm thấy hứng thú khi sử dụng.

Về phía quản trị, HeartLink cho phép theo dõi tổng thể tình trạng hệ thống, phát hiện sớm hành vi bất thường, và hỗ trợ người dùng khi gặp sự cố. Admin có thể gửi thông báo bảo trì, cập nhật, hoặc chiến dịch truyền thông trực tiếp đến tất cả người dùng qua Firebase Messaging. Điều này giúp duy trì sự ổn định, tin cậy và tăng khả năng mở rộng của hệ thống trong tương lai.

Tổng thể, HeartLink là một nền tảng kết bạn và kết nối cảm xúc hiện đại, được thiết kế với định hướng "đơn giản nhưng tinh tế". Ứng dụng không chỉ giúp người dùng dễ dàng tìm kiếm và trò chuyện với những người có cùng sở thích mà còn đảm bảo yếu tố riêng tư và trải nghiệm tự nhiên. Bên cạnh giá trị sử dụng, dự án còn mang ý nghĩa học thuật rõ ràng, là minh chứng cho việc vận dụng các công nghệ Firebase, GPS, Java và XML trong quy trình phát triển phần mềm thực tế, từ phân tích yêu cầu, thiết kế hệ thống, phát triển ứng dụng, đến kiểm thử và triển khai sản phẩm.

Với HeartLink, Nhóm 9 chúng em kỳ vọng mang đến cho người dùng một ứng dụng thân thiện, an toàn và ý nghĩa, giúp họ không chỉ "kết nối" mà còn thật sự "thấu hiểu" và "chạm đến cảm xúc" của nhau trong thế giới số.


### Yêu cầu chức năng

Hệ thống HeartLink cung cấp các chức năng chính sau:

#### Chức năng chính cho người dùng

- **Xác thực người dùng:**
- **Đăng ký:** Cho phép người dùng đăng ký tài khoản mới bằng email hoặc số điện thoại thông qua Firebase Authentication.
- **Đăng nhập:** Cho phép người dùng đăng nhập vào hệ thống bằng email/mật khẩu hoặc số điện thoại đã đăng ký.
- **Đăng xuất:** Cho phép người dùng kết thúc phiên làm việc
- **Quản lý Hồ sơ Cá nhân:** Cho phép người dùng tạo, xem và cập nhật hồ sơ cá nhân (họ tên, giới tính, tuổi, nghề nghiệp, sở thích, mô tả, ảnh đại diện) và cài đặt quyền riêng tư (bật/tắt chia sẻ vị trí).
- **Gợi ý & Kết nối (Matching):**
- Hiển thị danh sách hồ sơ được đề xuất dựa trên vị trí, độ tuổi, giới tính, sở thích.
- Cho phép người dùng "Like" (vuốt phải) hoặc "Pass" (vuốt trái) một hồ sơ.
- Tự động tạo "Match" khi hai người cùng "Like" lẫn nhau.
- **Trò chuyện Thời gian thực (Realtime Chat):** Cho phép hai người đã "Match" trò chuyện bằng tin nhắn văn bản, emoji, hình ảnh. Hiển thị trạng thái online và trạng thái đã xem tin nhắn.
- **Bộ lọc Tìm kiếm Nâng cao:** Cho phép người dùng tùy chỉnh tìm kiếm theo các tiêu chí: giới tính, độ tuổi, khoảng cách địa lý, sở thích.
- **Hệ thống Thông báo (Notification):** Gửi thông báo realtime đến người dùng khi có tin nhắn mới, lượt Like mới hoặc Match mới thông qua FCM.
- **Tính năng Bảo mật & An toàn:**
- Cho phép người dùng Báo cáo người dùng khác.
- Cho phép người dùng Chặn tài khoản khác.
- Cho phép xóa cuộc trò chuyện.

#### Chức năng quản trị viên (Admin Panel)

**\* Quản lý Người dùng**

- Quản lý người dùng (xem, khóa, xóa).
- Thống kê hoạt động (số người dùng mới, lượt Match, số cuộc trò chuyện, tỷ lệ báo cáo).

### Yêu cầu phi chức năng

**\* Hiệu năng:**

- Ứng dụng phải mượt mà, tốc độ phản hồi cho các thao tác vuốt, tải hồ sơ dưới 2 giây.
- Tin nhắn phải được đồng bộ và hiển thị trong vòng dưới 500ms.
- Hệ thống phải hỗ trợ đồng thời ít nhất 10,000 người dùng hoạt động.

**\* Bảo mật:**

- Dữ liệu phải được mã hóa khi truyền tải (HTTPS/TLS).
- Xác thực người dùng an toàn thông qua Firebase Auth.
- Quyền truy cập vào dữ liệu được phân quyền chặt chẽ (Firebase Security Rules).
- Vị trí người dùng chỉ được thu thập và chia sẻ khi có sự cho phép rõ ràng.

**\* Tính Khả dụng (Usability):**

- Giao diện thân thiện, trực quan, dễ sử dụng ngay cả với người dùng không rành công nghệ.
- Thiết kế theo nguyên tắc Material Design, tối ưu hóa cho thói quen người dùng Việt Nam.
- Thao tác vuốt (Swipe) mượt mà, mang lại phản hồi trực quan.

**\* Độ Tin cậy & Tính Sẵn sàng:**

- Hệ thống đạt thời gian hoạt động (uptime) trên 99.5%.
- Có cơ chế phục hồi khi mất kết nối mạng.

**\* Khả năng Bảo trì & Mở rộng:**

- Kiến trúc rõ ràng (Client-Cloud-Admin), mã nguồn được tổ chức tốt để dễ dàng bảo trì, nâng cấp.
- Sử dụng Firebase giúp hệ thống có khả năng mở rộng (scaling) tự động.


### Mô tả chi tiết các Use case

#### Mô tả use case "Đăng ký tài khoản"

Bảng 2.1. Mô tả use case "Đăng ký tài khoản"

Mã Use case

UC_DangKy

Tên Use case

Đăng ký tài khoản

Tác nhân

Người dùng

Mô tả

Người dùng sử dụng chức năng này để tạo một tài khoản mới trên HeartLink thông qua Email/OTP, Google hoặc Facebook.

Sự kiện kích hoạt chức năng

Người dùng nhấn vào nút "Tạo tài khoản" trên màn hình chào

Tiền điều kiện

Người dùng chưa có tài khoản và có kết nối Internet

Luồng sự kiện chính

| **#** | **Thực hiện bởi** | **Hành động** |
| --- | --- | --- |
| 1   | Người dùng | Chọn phương thức đăng ký: "Tiếp tục với Email", "Tiếp tục với Google" hoặc "Tiếp tục với Facebook". |
| Tiếp tục với Email |     |     |
| 2a  | Người dùng | Nhập địa chỉ Email. |
| 3a  | Người dùng | Nhập mật khẩu và xác nhận mật khẩu |
| 4a  | Người dùng | Nhấn nút **"Đăng ký"** |
| 5a  | Hệ thống | Kiểm tra email đã tồn tại chưa |
| 6a  | Hệ thống | Lưu thông tin tài khoản (email + password mã hóa) và chuyển tới màn hình hoàn thiện hồ sơ |
| Tiếp tục với Google |     |     |
| 2b  | Hệ thống | Mở ra màn hình chọn tài khoản Google (Google Sign-In Intent). |
| 3b  | Người dùng  <br>Người dùng | Chọn một tài khoản Google từ thiết bị. |
| 4b  | Hệ thống | Xác thực thông tin với Google và nhận lại ID Token. |
| 5b  | Hệ thống | Dùng ID Token để đăng ký/xác thực trên Firebase. Tự động điền các thông tin cơ bản (email, tên, ảnh) và chuyển hướng đến màn hình hoàn thiện hồ sơ. |
| Tiếp tục với Facebook |     |     |
| 2c  | Hệ thống | Mở ra màn hình đăng nhập Facebook (Facebook Login Dialog). |
| 3c  | Người dùng | Nhập thông tin đăng nhập Facebook (nếu chưa đăng nhập) và cấp quyền cho ứng dụng. |
| 4c  | Hệ thống | Xác thực thông tin với Facebook và nhận lại Access Token. |
| 5c  | Hệ thống | Dùng Access Token để đăng ký/xác thực trên Firebase. Tự động điền các thông tin cơ bản (email, tên, ảnh) và chuyển hướng đến màn hình hoàn thiện hồ sơ. |

Luồng sự kiện thay thế

| **#** | **Thực hiện bởi** | **Hành động** |
| --- | --- | --- |
| 5a.1 | Hệ thống | Nếu email đã tồn tại → thông báo: **"Email này đã được sử dụng. Vui lòng đăng nhập."** |
| 3a.1 | Hệ thống | Nếu mật khẩu không khớp → thông báo "Xác nhận mật khẩu không trùng khớp" |
| 4b.1, 4c.1 | Người dùng | Hủy bỏ quá trình đăng ký với Google/Facebook. Use case kết thúc. |
| 4b.2, 4c.2 | Hệ thống | Nếu xác thực Google/Facebook thất bại, hiển thị thông báo "Đăng ký thất bại. Vui lòng thử lại.". |

Hậu điều kiện

Một tài khoản mới được tạo trong hệ thống. Người dùng đã được xác thực và được chuyển hướng để hoàn thiện hồ sơ.

#### Mô tả use case "Đăng nhập"

Bảng 2.2. Mô tả use case "Đăng nhập"

Mã Use case

UC_DangNhap

Tên Use case

Đăng nhập

Tác nhân

Người dùng

Mô tả

Người dùng sử dụng chức năng này để đăng nhập vào ứng dụng HeartLink thông qua Email/OTP, Google hoặc Facebook.

Sự kiện kích hoạt chức năng

Người dùng mở ứng dụng và nhấn vào nút "Đăng nhập".

Tiền điều kiện

Người dùng đã có tài khoản và có kết nối Internet.

Luồng sự kiện chính

| **#** | **Thực hiện bởi** | **Hành động** |
| --- | --- | --- |
| 1   | Người dùng | Chọn phương thức đăng nhập: "Đăng nhập với Email", "Tiếp tục với Google" hoặc "Tiếp tục với Facebook". |
| Đăng nhập với Email |     |     |
| 2a  | Người dùng | Nhập địa chỉ Email. |
| 3a  | Người dùng | Nhập mật khẩu |
| 4a  | Người dùng | Nhấn "Đăng nhập" |
| 5a  | Hệ thống | Xác thực email & mật khẩu |
| 6a  | Hệ thống | Nếu đúng, chuyển tới màn hình chính (Discovery) |
| Tiếp tục với Google |     |     |
| 2b  | Hệ thống | Mở ra màn hình chọn tài khoản Google (Google Sign-In Intent). |
| 3b  | Người dùng | Chọn một tài khoản Google từ thiết bị. |
| 4b  | Hệ thống | Xác thực thông tin với Google và nhận lại ID Token. |
| 5b  | Hệ thống | Dùng ID Token để xác thực trên Firebase. Nếu thành công, cho phép đăng nhập và chuyển hướng đến màn hình chính (Discovery). |
| Tiếp tục với Facebook |     |     |
| 2c  | Hệ thống | Mở ra màn hình đăng nhập Facebook (Facebook Login Dialog). |
| 3c  | Người dùng | Nhập thông tin đăng nhập Facebook (nếu chưa đăng nhập) và cấp quyền cho ứng dụng. |
| 4c  | Hệ thống | Xác thực thông tin với Facebook và nhận lại Access Token. |
| 5c  | Hệ thống | Dùng Access Token để xác thực trên Firebase. Nếu thành công, cho phép đăng nhập và chuyển hướng đến màn hình chính (Discovery). |

Luồng sự kiện thay thế

| **#** | **Thực hiện bởi** | **Hành động** |
| --- | --- | --- |
| 5a.1 | Hệ thống | Mật khẩu sai → "Sai mật khẩu. Vui lòng thử lại." |
| 5a.2 | Hệ thống | Email chưa đăng ký → "Email chưa được đăng ký." |
| 5b.1, 5c.1 | Hệ thống | Nếu tài khoản Google/Facebook chưa từng được dùng để đăng ký, hiển thị thông báo "Tài khoản chưa được đăng ký. Vui lòng đăng ký.". |

Hậu điều kiện

Người dùng được xác thực và có quyền truy cập vào các chức năng của ứng dụng.

#### Mô tả use case "Kết nối"

Bảng 2.3. Mô tả use case "Kết nối"

Mã Use case

UC_Discovery

Tên Use case

Kết nối

Tác nhân

Người dùng

Mô tả

Người dùng sử dụng chức năng này để xem các hồ sơ được gợi ý và thực hiện Like/Pass.

Sự kiện kích hoạt chức năng

Người dùng truy cập vào tab "Khám phá" (Discovery) trên màn hình chính.

Tiền điều kiện

Người dùng đã đăng nhập và có kết nối Internet.

Luồng sự kiện chính

| **#** | **Thực hiện bởi** | **Hành động** |
| --- | --- | --- |
| 1   | Hệ thống | Tự động hiển thị một hồ sơ đề xuất (dựa trên vị trí, tuổi, sở thích) dưới dạng thẻ (Card). |
| 2   | Người dùng | Thực hiện thao tác: Vuốt phải (Like) hoặc Vuốt trái (Pass) trên thẻ hồ sơ. |
| 3   | Hệ thống | Ghi nhận hành động và ẩn thẻ vừa tương tác, hiển thị thẻ tiếp theo. |
| 4   | Hệ thống | Nếu cả hai người dùng cùng Like nhau, tạo một "Match" và gửi thông báo cho cả hai. |

Luồng sự kiện thay thế

| **#** | **Thực hiện bởi** | **Hành động** |
| --- | --- | --- |
| 1a  | Hệ thống | Nếu không còn hồ sơ nào để đề xuất trong bán kính tìm kiếm, hiển thị thông báo "Hãy thử mở rộng bộ lọc của bạn!". |

Hậu điều kiện

Hành động Like/Pass được ghi nhận. Một kết nối mới (Match) có thể được tạo ra.

#### Mô tả use case "Tìm kiếm nâng cao"

Bảng 2.4. Mô tả use case "Tìm kiếm nâng cao"

Mã Use case

UC_TKNangCao

Tên Use case

Tìm kiếm nâng cao

Tác nhân

Người dùng

Mô tả

Người dùng sử dụng chức năng này để thu hẹp phạm vi hồ sơ được gợi ý theo ý muốn.

Sự kiện kích hoạt chức năng

Người dùng nhấn vào nút "Bộ lọc" trên màn hình Discovery.

Tiền điều kiện

Người dùng đã đăng nhập và có kết nối Internet.

Luồng sự kiện chính

| **#** | **Thực hiện bởi** | **Hành động** |
| --- | --- | --- |
| 1   | Người dùng | Thiết lập các tiêu chí lọc (Giới tính, Độ tuổi, Khoảng cách, Sở thích). |
| 2   | Người dùng | Nhấn nút "Áp dụng". |
| 3   | Hệ thống | Lưu cài đặt bộ lọc và cập nhật ngay danh sách hồ sơ đề xuất theo tiêu chí mới. |

Luồng sự kiện thay thế

| **#** | **Thực hiện bởi** | **Hành động** |
| --- | --- | --- |
| 2a  | Người dùng | Nhấn nút "Đặt lại" để đưa tất cả bộ lọc về mặc định. |

Hậu điều kiện

Danh sách hồ sơ đề xuất được làm mới theo các bộ lọc đã chọn.

#### Mô tả use case "Trò chuyện"

Bảng 2.5. Mô tả use case "Tìm kiếm nâng cao"

Mã Use case

UC_Chat

Tên Use case

Trò chuyện

Tác nhân

Người dùng

Mô tả

Người dùng sử dụng chức năng này để nhắn tin với người mà họ đã Match.

Sự kiện kích hoạt chức năng

Người dùng nhấn vào một cuộc trò chuyện trong mục "Tin nhắn" hoặc từ thông báo Match.

Tiền điều kiện

Người dùng đã đăng nhập và có ít nhất một Match.

Luồng sự kiện chính

| **#** | **Thực hiện bởi** | **Hành động** |
| --- | --- | --- |
| 1   | Hệ thống | Hiển thị giao diện trò chuyện và lịch sử tin nhắn (nếu có). |
| 2   | Người dùng | Nhập tin nhắn văn bản hoặc chọn gửi hình ảnh/emoji. |
| 3   | Người dùng | Nhấn nút "Gửi". |
| 4   | Hệ thống | Lưu tin nhắn vào Firebase Realtime Database và hiển thị ngay lên giao diện của cả hai người. |

Luồng sự kiện thay thế

| **#** | **Thực hiện bởi** | **Hành động** |
| --- | --- | --- |
| 4a  | Hệ thống | Nếu mất kết nối, tin nhắn được lưu tạm và thử gửi lại khi có mạng. |

Hậu điều kiện

Tin nhắn được gửi đi và hiển thị trong cuộc trò chuyện.

#### Mô tả use case "Quản lý hồ sơ"

Bảng 2.6. Mô tả use case "Quản lý hồ sơ"

Mã Use case

UC_Profile

Tên Use case

Quản lý hồ sơ cá nhân

Tác nhân

Người dùng

Mô tả

Người dùng sử dụng chức năng này để xem và cập nhật thông tin cá nhân của mình.

Sự kiện kích hoạt chức năng

Người dùng nhấn vào tab "Hồ sơ" (Profile) trên màn hình chính.

Tiền điều kiện

Người dùng đã đăng nhập.

Luồng sự kiện chính

| **#** | **Thực hiện bởi** | **Hành động** |
| --- | --- | --- |
| 1   | Hệ thống | Hiển thị thông tin hồ sơ hiện tại của người dùng. |
| 2   | Người dùng | Nhấn nút "Chỉnh sửa". |
| 3   | Người dùng | Cập nhật các thông tin mong muốn (ảnh đại diện, nghề nghiệp, sở thích, bio...). |
| 4   | Người dùng | Nhấn nút "Lưu". |
| 5   | Hệ thống | Kiểm tra và cập nhật thông tin mới vào Firebase Realtime Database. |

Luồng sự kiện thay thế

| **#** | **Thực hiện bởi** | **Hành động** |
| --- | --- | --- |
| 5a  | Hệ thống | Nếu có lỗi (vd: ảnh quá lớn), hiển thị thông báo lỗi cụ thể. |

Hậu điều kiện

Thông tin hồ sơ cá nhân được cập nhật mới nhất trên hệ thống.

#### Mô tả use case "Báo cáo/Chặn"

Bảng 2.7. Mô tả use case "Báo cáo/Chặn"

Mã Use case

UC_BaoCao

Tên Use case

Báo cáo/Chặn

Tác nhân

Người dùng

Mô tả

Người dùng sử dụng chức năng này để báo cáo hoặc chặn một người dùng khác vì các hành vi không phù hợp.

Sự kiện kích hoạt chức năng

Người dùng nhấn vào menu (3 chấm) trên hồ sơ hoặc trong phòng chat của người cần báo cáo/chặn.

Tiền điều kiện

Người dùng đã đăng nhập và đang xem hồ sơ/trò chuyện với một người dùng khác.

Luồng sự kiện chính

| **#** | **Thực hiện bởi** | **Hành động** |
| --- | --- | --- |
| 1   | Người dùng | Chọn "Báo cáo" hoặc "Chặn". |
| 2   | Hệ thống | Hiển thị hộp thoại xác nhận và (với Báo cáo) yêu cầu chọn lý do. |
| 3   | Người dùng | Chọn lý do và nhấn "Xác nhận". |
| 4   | Hệ thống | Ghi nhận sự việc: |
| 5   | Hệ thống | Kiểm tra và cập nhật thông tin mới vào Firebase Realtime Database. |

Luồng sự kiện thay thế

| **#** | **Thực hiện bởi** | **Hành động** |
| --- | --- | --- |
| 3a  | Người dùng | Nhấn "Hủy" để hủy bỏ thao tác. |

Hậu điều kiện

Người dùng bị chặn hoặc bị báo cáo đã được ghi nhận vào hệ thống.

## Thiết kế hệ thống

### Các yêu cầu về dữ liệu

**\* Hệ thống cần quản lý các loại dữ liệu chính sau:Thông tin người dùng (Users):** UID (Firebase Auth), email/số điện thoại, họ tên, giới tính, ngày sinh, nghề nghiệp, sở thích (mảng), mô tả bản thân, URL ảnh đại diện, tọa độ địa lý (lat, lng), cài đặt hiển thị vị trí.

- **Thông tin kết nối (Matches):** MatchID, UserID1, UserID2, thời gian tạo Match.
- **Thông tin tin nhắn (Chats):** MessageID, MatchID, SenderID, Nội dung tin nhắn (văn bản/URL ảnh), timestamp, trạng thái đã xem.
- **Thông tin báo cáo (Reports):** ReportID, UserID (người báo cáo), ReportedUserID (người bị báo cáo), lý do, trạng thái xử lý.

**\* Mô tả chi tiết thực thể và thuộc tính:**

#### Thực thể USERS (Người dùng)

Đây là thực thể trung tâm, lưu trữ toàn bộ thông tin cá nhân của người dùng.

| **Thuộc Tính** | **Kiểu Dữ Liệu** | **Giải Thích & Ràng Buộc** |
| --- | --- | --- |
| **user_id** (PK) | String | **Khóa chính.** Đây là UID duy nhất do Firebase Authentication tự động sinh ra khi người dùng đăng ký. Đảm bảo mỗi người dùng có một định danh không trùng lặp. |
| **email** | String | Địa chỉ email dùng để đăng ký, đăng nhập và nhận thông báo. Có thể dùng để khôi phục mật khẩu. |
| **phone** | String | Số điện thoại xác thực, là một phương thức đăng nhập thay thế. |
| **full_name** | String | Họ và tên đầy đủ của người dùng, hiển thị trên hồ sơ và trong chat. |
| **gender** | String | Giới tính của người dùng (vd: "male", "female"). Phục vụ cho bộ lọc tìm kiếm và thuật toán gợi ý. |
| **date_of_birth** | Date | Ngày tháng năm sinh. Dùng để tính toán tuổi, một tiêu chí quan trọng cho việc gợi ý kết nối và lọc tìm kiếm. |
| **occupation** | String | Nghề nghiệp, giúp tạo ấn tượng và cung cấp thông tin nền tảng về người dùng. |
| **interests** | Mảng String | **Danh sách sở thích** (vd: \["Đá bóng", "Xem phim", "Du lịch"\]). Đây là thuộc tính **quan trọng nhất** để thuật toán tìm ra những người có cùng sở thích, từ đó tăng khả năng kết nối thành công. |
| **bio** | String | Mô tả ngắn gọn về bản thân. Là nơi người dùng giới thiệu tính cách, mong muốn kết bạn, giúp tạo thiện cảm ban đầu. |
| **profile_image_url** | String | **Đường dẫn đến ảnh đại diện** trên Firebase Storage. Ảnh là yếu tố trực quan nhất trong thẻ Swipe. |
| **latitude** | Float | Vĩ độ của người dùng. Kết hợp với longitude để xác định vị trí. |
| **longitude** | Float | Kinh độ của người dùng. Dùng để tính khoảng cách với người dùng khác cho thuật toán gợi ý và bộ lọc. |
| **is_location_visible** | Boolean | **Cờ bật/tắt chia sẻ vị trí.** Nếu false, vị trí (latitude, longitude) sẽ không được dùng để gợi ý hoặc hiển thị cho người khác, đảm bảo quyền riêng tư. |
| **created_at** | Datetime | Thời điểm tài khoản được tạo. Dùng cho mục đích thống kê và quản trị. |

#### Thực thể MATCHES (Kết nối)

Lưu trữ thông tin về các cặp đã kết nối thành công (Match).

| **Thuộc Tính** | **Kiểu Dữ Liệu** | **Giải Thích & Ràng Buộc** |
| --- | --- | --- |
| **match_id** (PK) | String | **Khóa chính.** Một mã định danh duy nhất cho mỗi kết nối, thường được Firebase tự động sinh ra. |
| **user_id_1** (FK) | String | **Khóa ngoại** tham chiếu đến user_id của người dùng thứ nhất trong kết nối. |
| **user_id_2** (FK) | String | **Khóa ngoại** tham chiếu đến user_id của người dùng thứ hai trong kết nối. |
| **matched_at** | Datetime | Thời điểm mà hai người "Like" lẫn nhau và kết nối được tạo ra. Dùng để sắp xếp các cuộc trò chuyện trong danh sách. |

**Lưu ý:** Khi lưu, user_id_1 và user_id_2 nên được sắp xếp theo thứ tự (ví dụ: luôn lưu ID nhỏ hơn trước) để tránh trùng lặp kết nối (ví dụ: A-B và B-A là một).

#### Thực thể CHATS (Tin nhắn)

Lưu trữ toàn bộ lịch sử trò chuyện giữa hai người đã Match.

| **Thuộc Tính** | **Kiểu Dữ Liệu** | **Giải Thích & Ràng Buộc** |
| --- | --- | --- |
| **message_id** (PK) | String | **Khóa chính.** Mã định danh duy nhất cho mỗi tin nhắn, được Firebase tự động sinh ra. |
| **match_id** (FK) | String | **Khóa ngoại** tham chiếu đến match_id. Xác định tin nhắn này thuộc về cuộc trò chuyện nào giữa hai người. |
| **sender_id** (FK) | String | **Khóa ngoại** tham chiếu đến user_id. Xác định người gửi tin nhắn này. |
| **message_type** | String | **Loại tin nhắn.** Ví dụ: "text" cho tin nhắn văn bản, "image" cho tin nhắn hình ảnh. Thuộc tính này quyết định cách hiển thị tin nhắn. |
| **message_content** | String | **Nội dung tin nhắn.** Nếu message_type là "text", đây sẽ là đoạn văn bản. Nếu là "image", đây sẽ là URL của hình ảnh được lưu trong Firebase Storage. |
| **sent_at** | Datetime | Thời điểm tin nhắn được gửi. Dùng để sắp xếp tin nhắn theo thứ tự thời gian trong cuộc trò chuyện. |
| **is_seen** | Boolean | **Cờ đã xem.** Cho biết người nhận đã xem tin nhắn này chưa. Dùng để hiển thị trạng thái "đã xem" (seen) trong giao diện chat. |

#### Thực thể REPORTS (Báo cáo)

Lưu trữ các khiếu nại, báo cáo vi phạm từ người dùng.

| **Thuộc Tính** | **Kiểu Dữ Liệu** | **Giải Thích & Ràng Buộc** |
| --- | --- | --- |
| **report_id** (PK) | String | **Khóa chính.** Mã định danh duy nhất cho mỗi báo cáo. |
| **reporter_id** (FK) | String | **Khóa ngoại** tham chiếu đến user_id của người tạo báo cáo. |
| **reported_user_id** (FK) | String | **Khóa ngoại** tham chiếu đến user_id của người bị báo cáo. |
| **reason** | String | **Lý do báo cáo.** Mô tả ngắn gọn của người báo cáo về hành vi vi phạm (vd: "Spam", "Ảnh không phù hợp", "Quấy rối"). |
| **status** | String | **Trạng thái xử lý.** Ví dụ: "pending" (đang chờ xử lý), "resolved" (đã xử lý xong). Giúp Quản trị viên theo dõi tiến độ. |
| **reported_at** | Datetime | Thời điểm báo cáo được gửi. Dùng để sắp xếp và thống kê. |
