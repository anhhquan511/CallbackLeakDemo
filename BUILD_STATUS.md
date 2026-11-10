# Kết quả kiểm tra project

| Hạng mục | Kết quả |
|---|---|
| Biên dịch Kotlin và Java | Thành công |
| Gradle assembleDebug | Thành công, APK được tạo |
| Android Lint debug | Hoàn tất: 0 lỗi, 32 cảnh báo |
| Kiểm tra chữ ký APK | apksigner verify thành công, chữ ký v2 |
| Kiểm tra manifest đã đóng gói | Package com.example.callbackleakdemo; min SDK 26; target SDK 34; compile SDK 35 |
| Chạy trên thiết bị/emulator | Chưa thực hiện, không có thiết bị kết nối |
| Số đo RAM và leak trace thực tế | Chưa thu thập; cần làm theo README |

Kiểm tra bằng JDK 17 đầy đủ, Gradle 8.9, Android SDK 35 và Build Tools 34.0.0. Các cờ mạng và thư mục cache riêng dùng trong môi trường kiểm tra không thuộc cấu hình project giao cho người dùng.

Lint có cảnh báo chủ yếu về chuỗi giao diện tiếng Việt viết trực tiếp trong mã; ngoài ra có target API cũ, quy tắc backup và icon ứng dụng. Đây là ứng dụng demo cục bộ, chưa chuẩn bị phát hành lên cửa hàng. Không có kết quả kiểm thử UI hay kết luận đã hết leak trên thiết bị từ riêng việc build thành công.

APK debug: prebuilt/CallbackLeakDemo-debug.apk

SHA-256: `8a74dec97f263751a017cfaa23325a88ce209818a19f5b4db01e4e43e2dc5106`

Các bộ đếm trên dashboard là chỉ báo logic của bài demo. Cần đối chiếu với đường tham chiếu của Activity đã hủy trong LeakCanary/heap dump trước khi kết luận.
