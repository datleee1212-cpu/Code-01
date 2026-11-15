# AutoFarm – bot farm Clash of Clans chạy trên máy tính bảng Android

App tự chạm vào các vị trí bạn đã chọn, chạy vòng farm liên tục:
**thu tài nguyên → luyện quân → tìm trận → thả quân → chờ → kết thúc → về làng**.

- Chạy hoàn toàn trên máy tính bảng. Không cần root, không cần máy tính hay giả lập.
- Dùng quyền **Trợ năng (Accessibility)** của Android để chạm (Android 7.0 trở lên).
- Không đọc dữ liệu game, không sửa game, không gửi dữ liệu đi đâu.

> ⚠️ **Rủi ro:** Điều khoản của Supercell cấm dùng bot. Tài khoản có thể bị khóa
> tạm thời hoặc vĩnh viễn. Nên thử trên tài khoản phụ.

## 1. Tải và cài

1. Trên máy tính bảng, mở trang GitHub của repo → **Releases** → `autofarm-latest`
   → tải **AutoFarm.apk**.
2. Mở file vừa tải. Nếu Android hỏi, cho phép **"Cài ứng dụng không rõ nguồn gốc"** với trình duyệt.

## 2. Bật quyền Trợ năng

1. Mở app **AutoFarm** → bấm **Mở cài đặt Trợ năng**.
2. Chọn **Ứng dụng đã tải xuống → AutoFarm → Bật**.
3. **Android 13 trở lên:** nếu nút bị mờ hoặc báo "Cài đặt bị hạn chế":
   vào **Cài đặt → Ứng dụng → AutoFarm → ⋮ (góc trên phải) → Cho phép cài đặt bị hạn chế**,
   rồi bật lại ở bước 2.

Bật xong sẽ thấy **bảng nổi** trên màn hình: `☰ ▶ ■ 📍 _`

| Nút | Chức năng |
|---|---|
| ☰ | Kéo để di chuyển bảng (để chỗ không che nút trong game) |
| ▶ | Bắt đầu farm |
| ■ | Dừng |
| 📍 | Cài vị trí các nút |
| _ | Thu gọn / mở rộng bảng |

## 3. Cài vị trí (chỉ làm một lần)

Mở Clash of Clans, đứng ở **làng chính**, xoay máy theo hướng bạn hay chơi, rồi bấm **📍**.
Màn hình hiện hướng dẫn từng bước, bạn chạm vào đúng nút được yêu cầu:

1. Mỏ vàng, bơm tiên dược, khoan tiên dược đen *(để thu tài nguyên – có thể bỏ qua)*
2. Huấn luyện → Huấn luyện nhanh → nút Huấn luyện đội hình 1 → X đóng *(có thể bỏ qua)*
3. **Tấn công!** → **Tìm trận** *(app tự bấm luôn để sang màn hình tiếp theo, tốn vàng tìm trận)*
4. Ô quân 1, ô quân 2, ô anh hùng
5. **Điểm thả quân đầu** và **điểm thả quân cuối**: chọn 2 điểm ở vùng trống ngoài
   viền làng địch. Quân được rải đều trên đoạn thẳng giữa 2 điểm này.
6. **Kết thúc trận** → **Đồng ý** → **Về nhà**

Mẹo:
- Nút chưa hiện trên màn hình → bấm **Ẩn 5 giây**, tự bấm trong game để mở, hướng dẫn sẽ hiện lại.
- Nút không dùng → **Bỏ qua**. Muốn xóa điểm đã lưu → **Xóa điểm**.
- Bấm **Xong** để thoát giữa chừng, các điểm đã chọn vẫn được lưu.
- Bắt buộc phải có: Tấn công, Tìm trận, Điểm thả quân đầu, Về nhà.

## 4. Chỉnh thông số (trong app AutoFarm)

| Thông số | Mặc định | Ý nghĩa |
|---|---|---|
| Số vòng farm | 0 | 0 = chạy mãi đến khi bấm ■ |
| Số lần chạm thả mỗi ô quân | 10 | Nhiều quân thì tăng lên |
| Nghỉ giữa các lần thả quân | 150 ms | |
| Nghỉ giữa các bước bấm nút | 1500 ms | Máy chậm thì tăng lên 2000–3000 |
| Chờ luyện quân trước khi đánh | 0 giây | Đặt bằng thời gian luyện quân nếu không dùng quân có sẵn |
| Chờ tìm trận | 8 giây | Mạng chậm thì tăng lên |
| Thời gian đánh | 90 giây | Sau đó bot bấm Kết thúc trận |
| Chờ về làng | 6 giây | |

## Giới hạn của bản 0.1

- Bot **chạm theo tọa độ**, chưa nhìn màn hình. Nếu game hiện thông báo lạ (sự kiện, quảng cáo,
  mất kết nối) bot không biết, cứ chạm tiếp. Hãy để ý trong lần chạy đầu.
- Đổi hướng xoay màn hình hoặc game cập nhật giao diện → cài lại vị trí bằng 📍.
- Chưa chọn làng theo lượng tài nguyên (bỏ qua làng nghèo).

Dự định bản sau: nhận diện hình ảnh (tự đóng thông báo, đọc tài nguyên để bấm "Tiếp theo"
khi làng nghèo), nhiều đường thả quân, thả phép.

## Build từ mã nguồn

GitHub Actions tự build mỗi khi đẩy thay đổi vào thư mục `autofarm/`
(xem `.github/workflows/autofarm.yml`). Build trên máy:

```bash
cd autofarm
./gradlew assembleDebug   # cần Android SDK, JDK 17
```
