# AutoFarm 0.2 – bot Clash of Clans đọc màn hình, chạy trên máy tính bảng Android

Bot chụp màn hình game, **nhận diện các nút bằng hình mẫu bạn dạy**, **đọc số tài nguyên bằng OCR**,
tự xử lý tình huống và **tự nâng cấp công trình**. Mọi thứ chạy trên máy tính bảng:
không root, không máy tính, không giả lập, không gửi dữ liệu đi đâu.

> ⚠️ **Rủi ro:** Điều khoản của Supercell cấm dùng bot. Tài khoản có thể bị khóa tạm thời
> hoặc vĩnh viễn. Nên thử trên tài khoản phụ.

## Bot làm gì trong mỗi vòng

1. **Về làng chính nếu bị lạc**: nhận ra nút *Tấn công* để biết đang ở làng. Nếu không thấy thì tự
   bấm *Về nhà*, *Đồng ý*, *Kết thúc trận*, *X*, hoặc đóng thông báo đã dạy. 3 lần không về được thì tự dừng.
2. **Đóng thông báo**: lúc chờ màn hình nào bot cũng kiểm tra luôn các thông báo đã dạy
   (mất kết nối, sự kiện, quảng cáo…) và bấm đóng.
3. **Thu tài nguyên**: tìm các biểu tượng vàng/tiên dược nổi trên cả màn hình và bấm từng cái.
4. **Tự nâng cấp** (nếu bật, mặc định cứ 3 vòng một lần): mở danh sách thợ xây, chọn dòng 1, 2, 3,
   bấm *Nâng cấp*. **Kiểm tra giá có chữ đỏ không** (chữ đỏ là thiếu tài nguyên): đỏ thì bỏ qua, không đỏ mới bấm xác nhận.
5. **Luyện quân** (huấn luyện nhanh).
6. **Tìm trận, chọn làng**: đọc số vàng + tiên dược của làng địch. Ít hơn mức bạn đặt thì bấm *Tiếp theo*.
7. **Thả quân** theo đường bạn vạch.
8. **Chờ trận xong**: thấy nút *Về nhà* là biết trận đã kết thúc. Hết giờ mà chưa xong thì bấm *Kết thúc trận*.

Máy dưới Android 11 không đọc được màn hình, bot chỉ chạm theo tọa độ và chờ theo thời gian như bản 0.1.

## 1. Tải và cài

Trên máy tính bảng: **Releases → `autofarm-latest` → AutoFarm.apk**, mở để cài
(cho phép "cài ứng dụng không rõ nguồn gốc" nếu được hỏi). Cài đè lên bản cũ được,
vị trí đã dạy vẫn giữ.

## 2. Bật quyền Trợ năng

Mở AutoFarm → **Mở cài đặt Trợ năng → Ứng dụng đã tải xuống → AutoFarm → Bật**.

- **Android 13+**: nếu nút bị mờ, vào **Thông tin ứng dụng → ⋮ → Cho phép cài đặt bị hạn chế**.
- **Vừa cập nhật từ 0.1**: **tắt rồi bật lại** Trợ năng để app có quyền chụp màn hình.

Bảng nổi: `☰ ▶ ■ 📍 🔍 _`

| Nút | Chức năng |
|---|---|
| ☰ | Kéo để di chuyển bảng (đặt chỗ không che nút nào của game) |
| ▶ / ■ | Chạy / dừng |
| 📍 | Dạy bot (chọn nhóm) |
| 🔍 | Kiểm tra: bot thấy những gì trên màn hình hiện tại, độ giống bao nhiêu %, đọc được số nào |
| _ | Thu gọn bảng |

## 3. Dạy bot

Bấm **📍**, chọn nhóm, rồi chạm vào đúng thứ được yêu cầu. Mỗi lần chạm, bot **chụp lại hình quanh
chỗ đó** để sau này tự tìm trên màn hình. Với các nút dùng để chuyển màn hình, bot tự bấm luôn để bạn
đi tiếp sang màn hình sau.

| Nhóm | Dạy những gì | Mẹo |
|---|---|---|
| **Đánh trận** | Huấn luyện (tùy chọn) → **Tấn công** → **Tìm trận** → **Tiếp theo** → **vùng số vàng**, **vùng số tiên dược** (chạm 2 góc) → ô quân, anh hùng → **2 điểm thả quân** → Kết thúc trận → Đồng ý → **Về nhà** | Vùng số: chỉ khoanh phần chữ số, đừng lấy cả biểu tượng |
| **Thu tài nguyên** | Biểu tượng vàng / tiên dược / tiên dược đen nổi trên mỏ | Dạy lúc mỏ đang đầy (biểu tượng đang hiện) |
| **Tự nâng cấp** | Biểu tượng thợ xây → dòng 2 → dòng 1 → nút **Nâng cấp** → nút **giá xác nhận** | Bot không bấm nút giá lúc dạy. Dạy xong tự đóng bảng |
| **Đóng thông báo** | Tối đa 6 mẫu: nút X, OK, Thử lại, Tải lại… | Gặp thông báo mới thì mở nhóm này dạy thêm. **Không dạy nút mua bằng ngọc** |

Khác:
- Nút chưa hiện → **Ẩn 5 giây** để tự thao tác trong game. **Bỏ qua** = giữ nguyên. **Xóa** = xóa mục đó.
- Dạy xong bấm **🔍** ở từng màn hình (làng, xem làng địch, trận xong) để kiểm tra.
  Nút nào báo ✗ dù đang hiện thì dạy lại, hoặc giảm "Độ giống tối thiểu".

## 4. Thông số

| Thông số | Mặc định | Ghi chú |
|---|---|---|
| Số vòng farm | 0 | 0 = chạy mãi |
| Chỉ đánh làng có vàng + tiên dược từ | 300000 | 0 = đánh mọi làng |
| Bỏ qua tối đa bao nhiêu làng | 20 | Mỗi lần *Tiếp theo* tốn vàng |
| Tự nâng cấp công trình | Tắt | Cần dạy nhóm "Tự nâng cấp" |
| Thử nâng cấp sau mỗi bao nhiêu vòng | 3 | |
| Số lần chạm thả mỗi ô quân | 10 | |
| Nghỉ giữa các bước bấm nút | 1500 ms | Máy chậm thì tăng |
| Thời gian đánh tối đa | 90 giây | Trận xong sớm thì bot về luôn |
| Độ giống tối thiểu | 82 % | Nhận nhầm → tăng; bỏ sót → giảm |
| Cỡ hình mẫu khi dạy | 8 % | Nút to thì tăng. Đổi xong phải dạy lại |

## Giới hạn

- Hình mẫu phụ thuộc độ phân giải và hướng xoay màn hình. Đổi máy hoặc xoay khác thì phải dạy lại.
- Game cập nhật giao diện thì phải dạy lại các nút bị đổi.
- OCR đọc số trong game không phải lúc nào cũng đúng 100%. Không đọc được thì bot đánh luôn
  (không bấm *Tiếp theo* mãi). Dùng 🔍 khi đang xem làng địch để kiểm tra.
- Tự nâng cấp chỉ thử 3 dòng đầu trong danh sách thợ xây, chưa nâng tường.
- Bảng nổi cũng hiện trong ảnh chụp. Đừng để bảng che nút của game.

## Build từ mã nguồn

GitHub Actions tự build khi có thay đổi trong `autofarm/` (xem `.github/workflows/autofarm.yml`).
Trên máy: `cd autofarm && ./gradlew assembleDebug` (cần JDK 17 và Android SDK).
