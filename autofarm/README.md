# AutoFarm 0.4 – bot Clash of Clans đọc màn hình, chạy trên máy tính bảng Android

Bot chụp màn hình game, **nhận diện các nút bằng hình mẫu bạn dạy**, **đọc số tài nguyên bằng OCR**,
tự xử lý tình huống, **tự nâng cấp công trình theo lộ trình** (có sẵn lộ trình **TH16 → TH17**)
và **tự cải thiện** dựa trên kết quả đã chạy. Mọi thứ chạy trên máy tính bảng:
không root, không máy tính, không giả lập, không gửi dữ liệu đi đâu.

> ⚠️ **Rủi ro:** Điều khoản của Supercell cấm dùng bot. Tài khoản có thể bị khóa tạm thời
> hoặc vĩnh viễn. Nên thử trên tài khoản phụ.

## Hai kênh chạy

| Kênh | Nút | Làm gì | Khi nào dùng |
|---|---|---|---|
| **Kênh Farm** | ▶ | Đi đánh + thu tài nguyên + nâng cấp (nếu bật "Kênh Farm: tự nâng cấp") | Muốn bot cày tài nguyên liên tục |
| **Kênh Lộ trình** | 🛠 | **Không đi đánh.** Cứ mỗi *N* phút: về làng → thu tài nguyên → giao việc cho thợ xây rảnh theo lộ trình → nghỉ tới lượt sau | Bạn tự đi đánh, hoặc chỉ muốn thợ xây không bao giờ rảnh |

- Mỗi lúc **chỉ chạy một kênh**. Đang chạy kênh này mà bấm kênh kia, bot nhắc bấm **■** trước.
- **■** dừng kênh đang chạy, dù là kênh nào.
- Dòng trạng thái trên bảng nổi ghi **[Farm]** hoặc **[Lộ trình]** ở đầu để biết kênh nào đang chạy.
- Trong lúc kênh chạy, **màn hình được giữ sáng** (game tắt màn hình là bot không chạm được). Dừng kênh
  thì máy lại tự tắt màn hình như bình thường. Đừng thoát game khi kênh Lộ trình đang nghỉ.
- Kênh Lộ trình cần **Android 11+**, đã dạy **nút Tấn công** (để biết đang ở làng) và **nhóm Tự nâng cấp**.
  Thời gian nghỉ giữa hai lượt chỉnh ở "Kênh Lộ trình: kiểm tra thợ xây mỗi (phút)" (mặc định 30, từ 5 tới 720).
  "Số vòng chạy" = 0 thì chạy mãi; đặt số khác thì kênh Lộ trình dừng sau chừng đó lượt.

## Kênh Farm làm gì trong mỗi vòng

1. **Về làng chính nếu bị lạc**: nhận ra nút *Tấn công* để biết đang ở làng. Nếu không thấy thì tự
   bấm *Về nhà*, *Đồng ý*, *Kết thúc trận*, *X*, hoặc đóng thông báo đã dạy. 3 lần không về được thì tự dừng.
2. **Đóng thông báo**: lúc chờ màn hình nào bot cũng kiểm tra luôn các thông báo đã dạy
   (mất kết nối, sự kiện, quảng cáo…) và bấm đóng.
3. **Thu tài nguyên**: tìm các biểu tượng vàng/tiên dược nổi trên cả màn hình và bấm từng cái.
4. **Tự nâng cấp** (nếu bật, mặc định cứ 3 vòng một lần): mở danh sách thợ xây, đọc tên + giá từng dòng,
   chọn mục theo **lộ trình** đã chọn rồi bấm *Nâng cấp*. **Kiểm tra giá có chữ đỏ không** (chữ đỏ là thiếu
   tài nguyên): đỏ thì bỏ qua, không đỏ mới bấm xác nhận.
5. **Luyện quân** (huấn luyện nhanh).
6. **Tìm trận, chọn làng**: đọc số vàng + tiên dược của làng địch. Ít hơn mức bạn đặt thì bấm *Tiếp theo*.
7. **Thả quân** theo đường bạn vạch.
8. **Chờ trận xong**: thấy nút *Về nhà* là biết trận đã kết thúc. Hết giờ mà chưa xong thì bấm *Kết thúc trận*.

Máy dưới Android 11 không đọc được màn hình: kênh Farm chỉ chạm theo tọa độ và chờ theo thời gian như bản 0.1,
kênh Lộ trình không dùng được.

## 1. Tải và cài

Trên máy tính bảng: **Releases → `autofarm-latest` → AutoFarm.apk**, mở để cài
(cho phép "cài ứng dụng không rõ nguồn gốc" nếu được hỏi). Cài đè lên bản cũ được,
vị trí đã dạy, thông số và dữ liệu đã học vẫn giữ.

## 2. Bật quyền Trợ năng

Mở AutoFarm → **Mở cài đặt Trợ năng → Ứng dụng đã tải xuống → AutoFarm → Bật**.

- **Android 13+**: nếu nút bị mờ, vào **Thông tin ứng dụng → ⋮ → Cho phép cài đặt bị hạn chế**.
- **Vừa cập nhật từ 0.1**: **tắt rồi bật lại** Trợ năng để app có quyền chụp màn hình.

Bảng nổi: `☰ ▶ 🛠 ■ 📍 🔍 _`

| Nút | Chức năng |
|---|---|
| ☰ | Kéo để di chuyển bảng (đặt chỗ không che nút nào của game) |
| ▶ | Chạy **kênh Farm** |
| 🛠 | Chạy **kênh Lộ trình** (chỉ nâng cấp, không đánh) |
| ■ | Dừng kênh đang chạy |
| 📍 | Dạy bot (chọn nhóm) |
| 🔍 | Kiểm tra: bot thấy những gì trên màn hình hiện tại, độ giống bao nhiêu %, đọc được số nào |
| _ | Thu gọn bảng (chỉ còn ☰ và _; bấm _ lần nữa để mở lại) |

## 3. Dạy bot

Bấm **📍**, chọn nhóm, rồi chạm vào đúng thứ được yêu cầu. Mỗi lần chạm, bot **chụp lại hình quanh
chỗ đó** để sau này tự tìm trên màn hình. Với các nút dùng để chuyển màn hình, bot tự bấm luôn để bạn
đi tiếp sang màn hình sau.

| Nhóm | Dạy những gì | Mẹo |
|---|---|---|
| **Đánh trận** | Huấn luyện (tùy chọn) → **Tấn công** → **Tìm trận** → **Tiếp theo** → **vùng số vàng**, **vùng số tiên dược** (chạm 2 góc) → ô quân, anh hùng → **2 điểm thả quân** → Kết thúc trận → Đồng ý → **Về nhà** | Vùng số: chỉ khoanh phần chữ số, đừng lấy cả biểu tượng. Kênh Lộ trình chỉ cần nút **Tấn công** của nhóm này |
| **Thu tài nguyên** | Biểu tượng vàng / tiên dược / tiên dược đen nổi trên mỏ | Dạy lúc mỏ đang đầy (biểu tượng đang hiện) |
| **Tự nâng cấp** | Vùng số kho của bạn, vùng số thợ rảnh, biểu tượng thợ xây → dòng 1 (khung) → dòng 2 → nút **Nâng cấp** → nút **giá xác nhận** | Bot không bấm nút giá lúc dạy. Dạy xong tự đóng bảng. Cần cho cả 2 kênh |
| **Đóng thông báo** | Tối đa 6 mẫu: nút X, OK, Thử lại, Tải lại… | Gặp thông báo mới thì mở nhóm này dạy thêm. **Không dạy nút mua bằng ngọc** |

Khác:
- Nút chưa hiện → **Ẩn 5 giây** để tự thao tác trong game. **Bỏ qua** = giữ nguyên. **Xóa** = xóa mục đó.
- Dạy xong bấm **🔍** ở từng màn hình (làng, xem làng địch, trận xong) để kiểm tra.
  Nút nào báo ✗ dù đang hiện thì dạy lại, hoặc giảm "Độ giống tối thiểu".

## 4. Thông số

Màn hình chính chia thông số thành 5 nhóm. Sửa xong bấm **Lưu thông số**; áp dụng từ lần bấm ▶ / 🛠 tiếp theo.

**⚔ Farm (kênh ▶)**

| Thông số | Mặc định | Ghi chú |
|---|---|---|
| Số vòng chạy | 0 | 0 = chạy mãi. Kênh Lộ trình: số lượt |
| Chỉ đánh làng có vàng + tiên dược từ | 300000 | 0 = đánh mọi làng |
| Bỏ qua tối đa bao nhiêu làng | 20 | Mỗi lần *Tiếp theo* tốn vàng |
| Số lần chạm thả mỗi ô quân | 10 | |
| Tự cải thiện | Bật | Xem mục "Tự cải thiện" bên dưới |

**🛠 Nâng cấp & lộ trình**

| Thông số | Mặc định | Ghi chú |
|---|---|---|
| Lộ trình nâng cấp | 1 | 1 Cân bằng, 2 Farm, 3 Phòng thủ, **4 TH16→TH17** |
| Cấp Nhà chính hiện tại | 16 | Dùng cho lộ trình 4 |
| Cho phép tự nâng Nhà chính | Tắt | Mặc định bot **không** tự nâng Nhà chính |
| Số tướng nâng cùng lúc tối đa | 2 | Giữ tướng còn lại đi war |
| Kênh Farm: tự nâng cấp công trình | Tắt | Kênh Lộ trình thì luôn nâng cấp |
| Kênh Farm: thử nâng cấp sau mỗi bao nhiêu vòng | 3 | |
| Kênh Lộ trình: kiểm tra thợ xây mỗi (phút) | 30 | 5–720 |
| Số công trình nâng tối đa mỗi lượt | 3 | |
| Số dòng nhìn thấy / số trang danh sách | 5 / 2 | Theo màn hình máy bạn |

**🏦 Kho**: sức chứa kho vàng / tiên dược / tiên dược đen (0 = không rõ), xả vào tường khi kho đầy từ 90 %,
nghỉ 10 phút khi kho đầy mà không còn gì để nâng.

**⏱ Thời gian**

| Thông số | Mặc định | Ghi chú |
|---|---|---|
| Nghỉ giữa các bước bấm nút | 1500 ms | Máy chậm thì tăng |
| Thời gian đánh tối đa | 90 giây | Trận xong sớm thì bot về luôn |

**👁 Nhận diện**

| Thông số | Mặc định | Ghi chú |
|---|---|---|
| Độ giống tối thiểu | 82 % | Nhận nhầm → tăng; bỏ sót → giảm |
| Cỡ hình mẫu khi dạy | 8 % | Nút to thì tăng. Đổi xong phải dạy lại |

## 5. Lộ trình TH16 → TH17 (lộ trình số 4)

Dựa trên file hướng dẫn [`docs/lo-trinh-TH16-TH17.txt`](docs/lo-trinh-TH16-TH17.txt).
Người dùng cũ vẫn để lộ trình 1 (Cân bằng); muốn dùng thì vào **Nâng cấp & lộ trình → Lộ trình nâng cấp = 4**,
điền đúng **Cấp Nhà chính hiện tại**, rồi chạy **🛠** (hoặc ▶ có bật tự nâng cấp).

Các giai đoạn bot đi theo:

| Giai đoạn | Nội dung chính |
|---|---|
| **A – TH16 cuối** | Xong checklist TH16 (Lab, 4 Trại quân, Lâu đài bang hội, Hero Hall 10, Blacksmith, Pet House…). **Eagle Artillery cấp 7 là bắt buộc** trước khi lên TH17. Đổ tài nguyên thừa vào tường |
| **B – Tuần 1–2 ở TH17** | Lab + tướng chính → Lâu đài bang hội → Trại quân → Hero Hall 11 → đặt Firespitter / Giga Bomb |
| **C – Tháng 1–2** | Trại quân còn lại, Xưởng phép + Xưởng phép đen, Grand Warden rồi Royal Champion (mỗi lần một tướng), Pet House, Inferno Artillery, Firespitter cấp tiếp |
| **D – Tháng 3–6** | Dragon Duke, King, Minion Prince; phòng thủ diện rộng & phòng không (Scattershot, Inferno, Air Defense, Wizard Tower, Bomber Tower, Multi-Archer, Ricochet…); bẫy |
| **E – Cuối TH17** | Phòng thủ rìa (Cannon, Archer Tower, Mortar…), tường |

Quy tắc bot luôn giữ:
- **Không tự nâng Nhà chính** trừ khi bạn tích "Cho phép tự nâng Nhà chính". Lên TH17 sẽ gộp Eagle Artillery
  vào Nhà chính và **không hoàn tác được**, nên tốt nhất bạn tự bấm khi đã sẵn sàng.
- **Giới hạn số tướng nâng cùng lúc** (mặc định 2) để luôn còn tướng đi war.
- **Tường chỉ là chỗ xả** vàng/tiên dược thừa khi kho gần đầy, không để dành tiền riêng cho tường.
- **Không bao giờ để thợ rảnh**: chưa đủ tiền cho mục lý tưởng thì làm mục tốt thứ hai.
- **Book / Hammer / ngọc không tự động**: bot không dùng vật phẩm phép, không bấm nút ngọc. Trang bị tướng,
  Ore, Pet, nghiên cứu Lab, gắn Apprentice / Lab Assistant cũng do bạn tự làm.

Tiến độ (mục nào đã xong, đang ở giai đoạn nào) hiện ở màn hình chính, mục **Tiến độ lộ trình & tự cải thiện**.

## 6. Tự cải thiện

Bot **ghi nhớ** kết quả mỗi trận và mỗi lượt nâng cấp vào bộ nhớ riêng trên máy (không gửi đi đâu),
giữ lại cả khi tắt app hay khởi động lại máy. Khi bật **"Tự cải thiện"** (mặc định bật), bot dựa vào đó
để **tự chỉnh ngưỡng chọn làng** (làng nghèo quá thì nâng ngưỡng, tìm mãi không ra thì hạ) và
**thời gian chờ** (máy/mạng chậm thì chờ lâu hơn).

- Xem bot đã học được gì: màn hình chính → **Tiến độ lộ trình & tự cải thiện** (cập nhật mỗi lần mở app).
- Muốn bot học lại từ đầu: bấm **Xóa dữ liệu đã học** → **Xóa**. Vị trí / hình mẫu đã dạy và thông số bạn
  nhập **không bị xóa**. Nhớ bấm ■ dừng bot trước khi xóa.
- Tắt ô "Tự cải thiện" thì bot vẫn ghi nhớ nhưng giữ nguyên thông số bạn nhập.

## Giới hạn

- Hình mẫu phụ thuộc độ phân giải và hướng xoay màn hình. Đổi máy hoặc xoay khác thì phải dạy lại.
- Game cập nhật giao diện thì phải dạy lại các nút bị đổi.
- OCR đọc số trong game không phải lúc nào cũng đúng 100%. Không đọc được thì bot đánh luôn
  (không bấm *Tiếp theo* mãi). Dùng 🔍 khi đang xem làng địch để kiểm tra.
- Bot chỉ thấy các mục hiện trong danh sách thợ xây (đọc tối đa "số trang" bạn đặt), nhận biết công trình
  theo tên nên tên lạ có thể bị xếp sai nhóm.
- Bảng nổi cũng hiện trong ảnh chụp. Đừng để bảng che nút của game.
- Kênh Lộ trình giữ màn hình sáng suốt lúc chạy, nên cắm sạc máy tính bảng.

## Kiểm thử bằng game giả lập (tự chạy trên GitHub)

Thư mục [`sim/`](sim) chứa một **game Clash of Clans giả lập** (`FakeGame`) và bộ kịch bản kiểm thử
(`Sim`). Lõi bot (`app/src/main/java/com/code02/autofarm/core`) là Java thuần nên chạy được trên cả máy tính
bảng lẫn giả lập. Mỗi lần có thay đổi trong `autofarm/`, GitHub Actions chạy giả lập **trước** khi build APK;
kịch bản nào sai là không ra bản mới. Có từ 9 kịch bản trở lên, ví dụ:

1. Farm có thông báo bất ngờ, mất kết nối, OCR đọc sai 5 % → vẫn đánh đủ trận, đóng thông báo, không bấm ngọc.
2. Bắt đầu ở màn hình lạ (kết quả trận, danh sách thợ, bảng huấn luyện…) → tự về làng.
3. Lộ trình nâng cấp (Cân bằng / Farm / Phòng thủ, tên tiếng Anh lẫn tiếng Việt) → chọn đúng thứ tự, không nâng Nhà chính.
4. Để dành tiên dược cho mục ưu tiên cao, vẫn nâng mục dùng vàng.
5. Kho đầy → xả vào tường, rồi vẫn đi đánh.
6. Kho đầy mà hết thợ → chờ, không tốn vàng tìm trận.
7. Chỉ tính phần tài nguyên còn chứa được khi chọn làng.
8. Máy không chụp được màn hình (Android dưới 11) → chạm theo tọa độ.
9. Nhận biết tên công trình (tiếng Anh / tiếng Việt).

Chạy trên máy tính (cần JDK 17):

```
cd autofarm
javac -encoding UTF-8 -d build/sim $(find app/src/main/java/com/code02/autofarm/core sim/src -name '*.java')
java -Dstdout.encoding=UTF-8 -cp build/sim sim.Sim
```

## Build từ mã nguồn

GitHub Actions tự build khi có thay đổi trong `autofarm/` (xem `.github/workflows/autofarm.yml`).
Trên máy: `cd autofarm && ./gradlew assembleDebug` (cần JDK 17 và Android SDK).
