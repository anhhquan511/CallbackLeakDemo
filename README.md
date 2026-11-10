# Demo callback và tài nguyên hệ thống gây Memory Leak

Project minh họa mục 2.1.2.3 của báo cáo, gồm ba ca BroadcastReceiver, listener trong Singleton và Bound Service. Mỗi ca có chế độ cố ý gây leak và đã sửa. Mục tiêu là chỉ ra Activity đã bị hủy nhưng vẫn bị một đăng ký sống lâu giữ lại, rồi kiểm chứng đường tham chiếu bằng LeakCanary.

## Chạy nhanh bằng APK có sẵn

Trong thư mục `prebuilt` có `CallbackLeakDemo-debug.apk`, được build từ mã nguồn trong gói này. Có thể chép APK sang điện thoại Android 8.0 trở lên và mở để cài; Android có thể yêu cầu cho phép cài từ ứng dụng đang mở file. Nếu muốn cài qua máy tính đã có adb:

```powershell
adb install -r prebuilt\CallbackLeakDemo-debug.apk
adb shell am start -n com.example.callbackleakdemo/.MainActivity
```

APK đã được build, nhưng chưa chạy trên thiết bị hoặc emulator trong môi trường tạo project. Cần thực hiện các bước kiểm thử ở mục 3 và ghi số đo thật trước khi dùng làm bằng chứng trong báo cáo. Nếu muốn đọc, sửa mã và đo bằng Profiler, mở project theo hướng dẫn dưới đây.

## 1. Mở và chạy trên Windows

1. Giải nén ZIP vào đường dẫn đơn giản, ví dụ `D:\AndroidDemos\android_callback_leak_demo`.
2. Android Studio → Open → chọn thư mục chứa `settings.gradle.kts`, KHÔNG chọn riêng thư mục `app`.
3. Đợi Gradle Sync tải dependency. Cần Internet lần đầu.
4. Trong SDK Manager, cài Android SDK Platform 35 và Android SDK Build-Tools 34.0.0 nếu IDE yêu cầu. Compile SDK 35 không có nghĩa điện thoại phải chạy Android 15.
5. Gradle JDK: dùng JDK 17 hoặc JDK 21 tương thích với Gradle 8.9. Nếu gặp lỗi JDK, đặt về JDK 17 trong Settings → Build, Execution, Deployment → Build Tools → Gradle.
6. Chạy bằng nút Run với build variant `debug`, không dùng Attach debugger trong lúc chờ LeakCanary.
7. Chấp nhận quyền thông báo nếu được hỏi trên Android 13+ để thấy thông báo LeakCanary. Nếu từ chối, có thể xem Logcat hoặc biểu tượng Leaks do công cụ cung cấp.

Thiết bị: Android 8.0/API 26 trở lên; đề xuất điện thoại thật hoặc emulator Android 13/API 33. Không cần server, Firebase, cơ sở dữ liệu hay quyền Internet ở thời điểm chạy app. Máy tính vẫn cần Internet để tải Gradle/dependency khi build.

Laptop RAM 16 GB: điện thoại Android thật qua USB thường thuận tiện hơn việc mở thêm emulator; bật Developer options → USB debugging và xác nhận thiết bị trên điện thoại. Không có Android thật thì dùng một emulator, đóng các VM và ứng dụng nặng khi demo.

Build bằng PowerShell từ thư mục project:

```powershell
.\gradlew.bat :app:assembleDebug
```

APK sau build nằm tại `app\build\outputs\apk\debug\app-debug.apk`.

Toolchain đã ghim: AGP 8.7.3, Gradle 8.9, Kotlin 2.0.21, core-ktx 1.15.0, LeakCanary 2.14, bytecode Java 17. Target SDK 34 và Compile SDK 35. Không cần nâng tự động AGP/Kotlin khi IDE đề xuất chỉ để chạy bài demo này.

## 2. Màn hình và nguyên lý chung

- Màn hình chính có công tắc: bật là ĐÃ SỬA, tắt là CỐ Ý GÂY LEAK. Mặc định là đã sửa.
- Chọn A, B hoặc C để vào một ca.
- Mỗi Activity demo có một mảng byte 1 MiB được ghi dữ liệu để mức giữ lại dễ quan sát. Đây là dữ liệu minh họa có chủ đích, không phải số RAM đo được từ ứng dụng thực tế trong báo cáo.
- Bấm Tạo lại màn hình để gọi `recreate()`: instance cũ bị hủy, instance mới được tạo. Có thể xoay màn hình thật để quan sát thêm; không khai báo `configChanges` để tránh bỏ qua tái tạo.
- App phát một sự kiện mỗi giây khi có Activity ở trạng thái Started. TickService có ticker riêng khi Service tồn tại. Các ticker không dùng để làm tác vụ dài thực tế và không bảo đảm chạy khi app bị hệ thống đóng/freeze.
- Khi callback gọi vào instance có `onDestroy()` đã chạy, Logcat ghi `STALE_CALLBACK`, dashboard tăng bộ đếm. Đây là bằng chứng callback vẫn truy cập được màn hình cũ; kết luận leak và đường giữ tham chiếu cần dựa thêm vào heap analysis.
- Dashboard chỉ giữ số đếm, không giữ Activity. Không có một danh sách debug chứa tham chiếu mạnh đến Activity khiến sai lệch nguồn leak.
- Một listener/receiver/binding còn tồn tại khi màn hình đang sử dụng nó là bình thường. Chỉ đánh giá đăng ký còn sót sau khi đóng hoặc tái tạo màn hình cũ; không kết luận leak chỉ từ con số 1 khi màn hình hiện tại đang mở.
- LeakCanary chỉ ở build debug; ngưỡng retained Activity hiển thị đặt là 1 cho demo. Công cụ vẫn cần thời gian theo dõi, GC và phân tích heap; không hứa sẽ có báo cáo sau đúng một số giây.

## 3. Chạy một ca đầy đủ

1. Force stop app để bắt đầu tiến trình sạch. Mở lại app.
2. Tắt công tắc, chọn ca cần thử.
3. Đợi thấy vài sự kiện để chắc chắn đăng ký/kết nối đã hoạt động.
4. Bấm Tạo lại màn hình 5–10 lần, đợi 1–2 giây giữa các lần; riêng ca Service phải đợi đã nhận sự kiện trước khi bấm.
5. Đóng màn hình về dashboard. Chụp bộ đếm đăng ký và callback cũ; xem Logcat tag `CallbackDemo`.
6. Giữ app ở foreground, đợi LeakCanary. Thường cần hàng chục giây, có thể lâu hơn. Mở báo cáo, tìm Activity demo đã bị hủy và đoạn leak trace thuộc code demo.
7. Chụp ảnh báo cáo leak và Memory Profiler nếu cần.
8. Force stop app lần nữa, mở lại, bật công tắc ĐÃ SỬA.
9. Chạy cùng ca, cùng số lần tạo lại, cùng khoảng chờ; đóng về dashboard.
10. Kiểm tra các đăng ký của ca đó đã về 0 và không có `STALE_CALLBACK` của ca trong lượt sạch. Kiểm tra heap/LeakCanary để xác nhận instance đã hủy không còn bị các đường tham chiếu đó giữ.

Không đổi từ lỗi sang sửa trong cùng một tiến trình rồi cho rằng leak cũ đã được dọn. Công tắc chỉ quyết định hành vi của màn hình MỚI. Các đăng ký bị bỏ quên ở lượt lỗi tồn tại tới khi tiến trình kết thúc. Xóa lịch sử báo cáo LeakCanary không đồng nghĩa giải phóng đối tượng.

### A. BroadcastReceiver

File: `app/src/main/java/com/example/callbackleakdemo/ReceiverDemoActivity.kt`.

Receiver đăng ký bằng applicationContext, nhưng anonymous receiver gọi phương thức của Activity nên vẫn giữ Activity. Bản lỗi không unregister trong onStop; sau mỗi recreate, receiver cũ tiếp tục nhận broadcast.

Bản sửa: `applicationContext.unregisterReceiver(receiver)` trong onStop, dùng đúng instance và đúng context, cờ registered chống đăng ký/hủy lặp sai.

Kỳ vọng về logic: sau N lần recreate và đóng màn hình, bản lỗi còn N+1 receiver nếu mọi đăng ký đều thành công; bản sửa còn 0. Callback cũ tiếp tục xuất hiện ở bản lỗi khi dashboard đang hiển thị.

Lý do dùng applicationContext cho cả hai bản: Activity context có cơ chế dọn một số đăng ký khi destroy tùy framework, làm lỗi khó tái hiện nhất quán. Đăng ký app scope nhưng callback capture Activity tạo một ví dụ rõ ràng; nó cũng chứng minh thay Context không đủ khi callback vẫn giữ màn hình.

### B. Listener trong Singleton

Files: `ListenerDemoActivity.kt`, `DemoState.kt`.

EventHub là Kotlin object tồn tại theo tiến trình. `listeners` giữ lambda; lambda gọi `onDemoEvent` của Activity. Bản lỗi không remove nên Activity cũ vẫn nhận tick. Bản sửa remove đúng lambda trong onStop.

Đường tham chiếu dự kiến về logic: Singleton EventHub → tập listeners → lambda → ListenerDemoActivity. Đường chính xác và tên lớp lambda do compiler sinh có thể khác; dùng báo cáo thật khi thuyết trình.

Sau N recreate và đóng: lỗi còn N+1 listener nếu đã chạy đủ vòng đời, sửa còn 0. Đây là ca nên trình bày đầu tiên vì dễ nhìn ra nguyên nhân và cách sửa.

### C. Bound Service

Files: `ServiceDemoActivity.kt`, `TickService.kt`.

Đây là PURE BOUND SERVICE: chỉ bind với BIND_AUTO_CREATE, KHÔNG gọi startService/startForegroundService. ServiceConnection và listener của Service đều có thể giữ Activity. Bản lỗi không remove listener và không unbind. Bản sửa làm đủ hai thao tác trong onStop.

`bindingRequested` phản ánh yêu cầu bind được chấp nhận, không chờ `onServiceConnected` mới đánh dấu. Nhờ vậy nếu màn hình dừng trước callback kết nối, bản sửa vẫn có thể giải phóng binding đã được chấp nhận. `onServiceDisconnected` không tự xóa trạng thái đăng ký binding, vì mất kết nối bất ngờ không đồng nghĩa đã unbind.

Kỳ vọng sau khi đóng màn hình cuối và đợi callback hệ thống: lỗi còn binding/listener và một Service; sửa còn 0 binding, 0 listener, Service bị hủy khi client cuối unbind. Xem log SERVICE CREATE/SERVICE DESTROY.

Không lấy số lượng Service instance làm số lượng Activity leak: nhiều client có thể cùng kết nối một Service instance.

## 4. Đo bằng Android Studio Memory Profiler

1. Dùng cùng thiết bị, cùng build debug và cùng ca cho hai lượt.
2. Mở Profiler, chọn process `com.example.callbackleakdemo` và tác vụ phân tích bộ nhớ/heap tương ứng trong phiên bản Android Studio đang dùng.
3. Ghi mốc RAM trước thử nghiệm. Lặp lại 5–10 lần recreate, đóng về dashboard.
4. Có thể yêu cầu GC rồi chụp heap dump. Lọc theo `ReceiverDemoActivity`, `ListenerDemoActivity` hoặc `ServiceDemoActivity`.
5. Phân biệt instance đang hiển thị với instance đã hủy. Xem References/đường đến GC Root và Retained Size.
6. So sánh hai lượt sau cùng điều kiện. Tổng RAM có thể dao động do runtime, GC, cache và chính LeakCanary. Mỗi payload 1 MiB không bảo đảm RAM hệ thống tăng đúng 1 MiB mỗi vòng.

Nếu dump và đo timing, tránh so thời điểm một bên đang phân tích heap với bên kia đang idle. Nên thu một lượt để xác nhận leak trace và một lượt riêng để đo xu hướng RAM ở điều kiện tương đương.

## 5. Bảng ghi kết quả thật

Không điền trước các con số như 380 MB, 58 MB hay giảm 84.7% trong báo cáo gốc. Project này không có phép đo nào chứng minh sẵn các con số đó.

| Ca | Chế độ | Số recreate | RAM trước | RAM sau | Đăng ký sau đóng | Activity leak xác nhận | Bằng chứng |
|---|---|---:|---|---|---|---|---|
| Receiver | Lỗi | 10 | | | | | |
| Receiver | Sửa | 10 | | | | | |
| Listener | Lỗi | 10 | | | | | |
| Listener | Sửa | 10 | | | | | |
| Service | Lỗi | 10 | | | | | |
| Service | Sửa | 10 | | | | | |

Bộ đếm đăng ký là kiểm tra logic, không thay thế kết quả LeakCanary. Không có cảnh báo tức thời cũng chưa chứng minh hết leak. Leak trong Android/framework có thể xuất hiện độc lập; tập trung vào các lớp và đường tham chiếu do demo tạo.

## 6. Logcat và reset

Trong Android Studio Logcat, lọc theo package ứng dụng rồi tìm `CallbackDemo`. Các dòng cần trình bày: CREATE, DESTROY, STALE_CALLBACK, SERVICE CREATE, SERVICE DESTROY. Tìm LeakCanary để xác nhận công cụ đã khởi động.

Reset qua điện thoại: Settings → Apps → Callback Leak Demo → Force stop → mở lại. Chỉ vuốt khỏi Recent Apps không bảo đảm tiến trình đã bị kết thúc.

Hoặc Terminal đã có adb trong PATH:

```powershell
adb shell am force-stop com.example.callbackleakdemo
adb shell am start -n com.example.callbackleakdemo/.MainActivity
```

Nếu báo nhiều thiết bị, thêm `-s SERIAL` sau adb. Nếu không nhận adb, dùng Android Studio Terminal với đường dẫn đầy đủ đến `platform-tools\adb.exe` trong Android SDK.

## 7. Kịch bản thuyết trình khoảng 5–7 phút

1. Giới thiệu vấn đề: GC không thể thu hồi Activity đã hủy nếu đăng ký sống lâu vẫn giữ callback trỏ về Activity.
2. Trình bày ca Listener bản lỗi, recreate vài lần, chỉ bộ đếm và dòng STALE_CALLBACK. Mở ảnh leak trace đã chụp trước nếu phân tích live mất thời gian.
3. Chỉ dòng EventHub.add(listener) và chỗ thiếu remove. Reset process, chạy bản sửa với cùng thao tác, chỉ số listener trở về 0 khi thoát.
4. Chuyển ca Receiver: giải thích unregister và vì sao applicationContext vẫn có thể leak khi receiver capture Activity.
5. Chuyển ca Service: giải thích remove listener khác unbind; unbind cuối cùng cho phép pure bound Service bị hủy.
6. Trình bày bảng số đo thật và leak trace; kết luận phải ghép đăng ký/hủy với vòng đời phù hợp.

Chuẩn bị trước ảnh dashboard, Logcat và leak trace của từng ca để chủ động thời gian, nhưng nói rõ đó là kết quả lượt chạy đã chuẩn bị.

## 8. Giới hạn và phạm vi

Demo cố ý tạo leak trong ứng dụng riêng để học tập, không phục vụ chạy dài. Không có tác vụ nặng, sleep trên main thread hay kịch bản ANR vì mục này tập trung leak. Native heap/file descriptor, cảm biến, GPS, socket, RxJava và Fragment ViewBinding nằm ngoài ba ca cốt lõi này; có thể bổ sung sau nếu đề tài mở rộng.

MainActivity có Handler cập nhật dashboard nhưng luôn remove trong onStop. DemoApplication có ticker theo tiến trình và chỉ phát sự kiện khi có Activity Started; callback đó không giữ một Activity. TickService hủy ticker trong onDestroy.

## 9. Tài liệu chính thức

- Broadcast và hủy đăng ký: https://developer.android.com/develop/background-work/background-tasks/broadcasts
- Bound Service và vòng đời: https://developer.android.com/develop/background-work/services/bound-services
- LeakCanary: https://square.github.io/leakcanary/getting_started/
- Thiết lập LeakCanary: https://square.github.io/leakcanary/recipes/
- Heap dump: https://developer.android.com/studio/profile/capture-heap-dump
- AGP 8.7 và cấu hình tương thích: https://developer.android.com/build/releases/agp-8-7-0-release-notes
