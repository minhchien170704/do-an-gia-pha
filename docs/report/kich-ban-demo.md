# KỊCH BẢN DEMO 5 PHÚT — HỆ THỐNG QUẢN LÝ SƠ ĐỒ GIA PHẢ

> **Mục đích:** trình diễn trọn vẹn các chức năng chính và **các ca lỗi** trong
> 5 phút, đủ để giảng viên thấy hệ thống chạy thật, không chỉ nghe mô tả.
>
> **Chuẩn bị trước khi lên sân:**
> 1. MySQL/MariaDB đang chạy (port `3306`).
> 2. Nạp bộ dữ liệu mẫu 30 người / 5 thế hệ:
>    ```bash
>    mysql -u root -p < test-data/sample-data.sql
>    ```
> 3. Chạy ứng dụng **từ thư mục gốc dự án**:
>    ```bash
>    mvn javafx:run
>    ```
> 4. Cửa sổ ứng dụng đã mở, kích thước tối thiểu 1200×750 để thấy hết bảng.
> 5. **Đóng sẵn** các hộp thoại không cần dùng. Chuẩn bị sẵn 1 file ảnh `.png`
>    (ví dụ `anh_cha.png`) để demo chọn ảnh đại diện.

---

## TỔNG QUAN TIMELINE

| Thời gian | Nội dung | Mục tiêu |
|---|---|---|
| 0:00 – 0:30 | Mở app, giới thiệu màn hình danh sách | Thấy dữ liệu thật từ MySQL + thống kê |
| 0:30 – 1:15 | Thêm một thành viên mới **có ảnh đại diện** | Thấy luồng View → Controller → Service → MySQL |
| 1:15 – 1:45 | Tìm kiếm + xem chi tiết | Thấy tra cứu |
| 1:45 – 2:30 | Xem sơ đồ cây gia phả 2D + zoom + đổi người trung tâm | Thấy phần "đồ án" nhất |
| 2:30 – 3:00 | Xuất PNG | Thấy `PngWriter` tự viết hoạt động |
| 3:00 – 3:15 | **LỖI 1** — Nhập năm sinh sai | Thấy validation |
| 3:15 – 3:35 | **LỖI 2** — Gán sai giới tính vai trò (BR6) | Thấy quy tắc nghiệp vụ |
| 3:35 – 3:55 | **LỖI 3** — Tạo chu trình gia phả (BR3) | Thấy DFS phát hiện chu trình |
| 3:55 – 4:15 | **LỖI 4** — Xóa người đang làm cha/mẹ (BR4) | Thấy chặn xóa |
| 4:15 – 4:30 | **CẢNH BÁO** — Năm sinh cha ≥ con (BR5) | Thấy cảnh báo không chặn |
| 4:30 – 4:50 | Chạy `mvn clean test` (nếu có môi trường) | Thấy 201 test PASS |
| 4:50 – 5:00 | Tổng kết, mở câu hỏi | |

> **Mẹo:** nếu bị hỏi giữa lúc demo, hãy nhảy sang phần **"Câu hỏi thường gặp
> khi bị hỏi giữa demo"** ở cuối tài liệu.

---

## PHẦN A — CÁC BƯỚC CHÍNH (happy path)

### A0. [0:00 – 0:30] Mở ứng dụng — màn hình danh sách thành viên

**Thao tác:**
1. Khởi động `mvn javafx:run`.
2. Ứng dụng mở màn hình **"Thành viên dòng họ"**.

**Kết quả mong đợi:**
- Sidebar trái: "Sơ đồ Gia phả" + 2 mục *"Thành viên dòng họ"*, *"Cây gia phả trực quan"*.
- 4 thẻ thống kê: **Tổng thành viên = 30**, **Số thế hệ = 5**, **Thành viên Nam = 18**,
  **Thành viên Nữ = 12**.
- Bảng 7 cột: Ảnh | ID | Họ và tên | Năm sinh | Giới tính | Cha | Mẹ.
- Dòng đầu: `Nguyễn Văn Tổ` (1850, Nam) — không có Cha/Mẹ (hiển thị "—").
- Dòng 2: `Trần Thị Tổ` (1855, Nữ).
- Dòng 3: `Nguyễn Văn Cụ` (1880, Nam), Cha = *Nguyễn Văn Tổ*, Mẹ = *Trần Thị Tổ*.
- Dưới bảng: *"Hiển thị 30 trên tổng số 30 thành viên"*.

**Cách nói:**
> "Đây là dữ liệu thật đọc từ MySQL — bộ mẫu 30 người, 5 thế hệ, 4 nhánh dòng họ.
> Bảng này được nạp qua `PeopleListView.reloadData()` → `PersonController.getAllPersons()`
> → `FamilyTreeService.getAllPersons()` → `JdbcPersonRepository.findAll()`.
> Cột Cha/Mẹ được liên kết ngay trong bộ nhớ sau khi `SELECT`, nên không cần N+1 query."

> ⚠️ **Lưu ý thống kê giới tính**: trong bộ mẫu, `Nguyễn Văn Hà` (id 28) là FEMALE,
> `Nguyễn Văn Giang` (id 27) là MALE. Tổng Nam = 18, Nữ = 12. Nếu con số hiển thị
> khác, hãy kiểm tra lại bạn đã nạp đúng `test-data/sample-data.sql` chưa.

---

### A1. [0:30 – 1:15] Thêm một thành viên mới (có ảnh đại diện)

**Thao tác:**
1. Bấm **"+ Thêm thành viên"**.
2. Bấm **"Chọn ảnh..."** → chọn file `anh_cha.png` đã chuẩn bị.
   → Khung xem trước bo tròn bên trái **hiện ảnh thật ngay**, dòng chữ nhỏ ghi
   *"Đã chọn: anh_cha.png"*.
3. Nhập **Họ và tên**: `Nguyễn Văn Mới`
4. Nhập **Năm sinh**: `1998`
5. Chọn **Giới tính**: `Nam`
6. Chọn **Cha**: `Nguyễn Văn An` (1960)
7. Chọn **Mẹ**: `Vũ Thị Mẹ Hùng` (1939)
8. Bấm **"Lưu thành viên"**.

**Kết quả mong đợi:**
- Alert *"Đã thêm thành viên "Nguyễn Văn Mới" vào gia phả thành công."*
- Ứng dụng tự quay về màn hình danh sách.
- Thẻ "Tổng thành viên" tăng lên **31**.
- Dòng mới `Nguyễn Văn Mới` (1998, Nam, Cha = Nguyễn Văn An, Mẹ = Vũ Thị Mẹ Hùng)
  nằm cuối bảng, có **avatar ảnh thật** ở cột đầu.
- Trong thư mục `images/` của dự án xuất hiện file `person_31.png`.

**Cách nói:**
> "Thêm người đi qua đúng chuỗi View → Controller → Service → Repository → MySQL.
> Điểm đáng chú ý: ảnh được copy **sau khi** CSDL đã sinh `id` — vì tên file là
> `person_<id>.<đuôi>`. Nên `PersonController.handleAdd()` phải INSERT trước,
> rồi copy ảnh, rồi UPDATE lại đường dẫn. Và trong DB chỉ lưu **đường dẫn tương đối**
> `images/person_31.png` — nên database không phụ thuộc vị trí cài đặt."

---

### A2. [1:15 – 1:45] Tìm kiếm & xem chi tiết

**Thao tác:**
1. Gõ vào ô *"Tìm kiếm theo họ tên..."* → `Nguyễn Văn`.
2. Quan sát bảng lọc còn **19 dòng** (dòng dưới bảng đổi thành
   *"Hiển thị 19 trên tổng số 31 thành viên"*).
3. Xóa từ khóa → bảng trở lại 31 dòng.
4. Chọn dòng `Nguyễn Văn Cụ` (id=3) → bấm **"Chi tiết"**.

**Kết quả mong đợi ở màn hình chi tiết:**
- Avatar lớn bo tròn (không có ảnh → hiển thị **chữ cái đầu "C"** trên nền tròn
  màu xanh-nhạt) — đây là cơ chế **fallback** của `PersonAvatar`.
- Tên lớn: `Nguyễn Văn Cụ`, dòng phụ: *"Sinh năm 1880 · Nam"*.
- Card "Thông tin cá nhân": Họ và tên / Năm sinh / Giới tính.
- Card "Quan hệ gia đình": 2 card "Cha" (Nguyễn Văn Tổ) và "Mẹ" (Trần Thị Tổ),
  bên dưới là "Các con (2)": `Nguyễn Văn Đại` (Nam), `Nguyễn Văn Tị` (Nam).

**Cách nói:**
> "Avatar tự fallback về chữ cái đầu khi không có ảnh — `PersonAvatar` nạp ảnh ở chế độ
> background loading và nếu file hỏng hoặc không tồn tại thì tự chuyển về placeholder,
> **không bao giờ** ném exception ra giao diện. Danh sách con lấy trực tiếp từ MySQL
> qua `PersonDetailView.getChildrenFromDB()`."

---

### A3. [1:45 – 2:30] Xem sơ đồ cây gia phả 2D

**Thao tác:**
1. Bấm **"← Quay lại danh sách"**.
2. Bấm nút **"Cây gia phả"** trên thanh công cụ (với dòng `Nguyễn Văn Cụ` đang được
   chọn) → mở màn hình **Cây gia phả**.
3. Quan sát sơ đồ 2D ở tab "Sơ đồ 2D".
4. **Phóng to** 2–3 lần bằng nút `+` (hoặc Ctrl + lăn chuột) → nhãn "Zoom:" đổi
   từ 100% lên 144%, 173%...
5. Bấm nút **"Vừa khung"** → sơ đồ vừa khung nhìn, zoom về mức vừa.
6. **Kéo chuột trái** để kéo sơ đồ.
7. Bấm vào một node (ví dụ node `Nguyễn Văn Tổ`) → mở hồ sơ chi tiết của người đó.
8. Quay lại, đổi **"Người trung tâm"** trên toolbar sang `Nguyễn Văn An` (id=23)
   → sơ đồ được vẽ lại ngay lập tức.
9. Chuyển sang tab **"Danh sách phả hệ"** → thấy `TreeView` dạng phân cấp.

**Kết quả mong đợi:**
- Sơ đồ 2D: mỗi thế hệ một hàng; **tổ tiên ở trên**, **hậu duệ ở dưới** người
  trung tâm.
- Mỗi node gồm: avatar bo tròn + họ tên + năm sinh + badge giới tính (xanh dương
  = Nam, hồng = Nữ).
- **Cha và mẹ được gom cạnh nhau thành khối gia đình**, con nằm chính giữa dưới
  khoảng giữa hai vị trí cha/mẹ.
- Đường nối đi **từng cha/mẹ xuống đúng con của họ** (không nối nhầm vợ/chồng).
- Node của người trung tâm có **viền đậm màu xanh**.
- Chú giải màu ở góc phải toolbar.

**Cách nói:**
> "Đây là phần chính của đồ án. Thuật toán bố trí nằm trong
> `FamilyTreeDiagram`: `buildModel()` gán `generation` cho mỗi node (tổ tiên là -1, -2;
> hậu duệ là +1, +2) quyết định hàng; `buildUnits()` gom cha–mẹ thành *khối gia đình*;
> `layoutTree()` dùng thuật toán **cha canh giữa trên tập con** — lá được cấp cột
> liên tiếp, cha nhận cột trung bình giữa lá đầu và lá cuối; sau đó cả nhánh tổ
> tiên được dịch để thẳng hàng với người trung tâm."

---

### A4. [2:30 – 3:00] Xuất PNG

**Thao tác:**
1. Quay lại tab "Sơ đồ 2D".
2. **Thu nhỏ** sơ đồ bằng nút `−` vài lần (zoom ≈ 50%) để sơ đồ bị tràn ra ngoài
   khung nhìn — để chứng minh ảnh xuất ra **không bị cắt**.
3. Bấm **"🖼 Xuất PNG"**.
4. Chọn thư mục, để tên file mặc định `gia_pha_Nguyễn_Văn_An.png` → **Lưu**.

**Kết quả mong đợi:**
- Alert *"Xuất PNG thành công"* kèm đường dẫn tuyệt đối của file.
- Mở file PNG: **ảnh chứa TOÀN BỘ sơ đồ** (từ thế hệ 1 đến thế hệ 5), **không bị
  cắt** dù lúc xuất sơ đồ đang bị thu nhỏ và tràn khung nhìn.
- Ảnh có **nền trắng** (không trong suốt).
- **Zoom và góc nhìn trên màn hình không bị đổi** sau khi xuất.

**Cách nói:**
> "Hàm `FamilyTreeDiagram.renderFullImage()` tạm đặt zoom về 1.0, dùng
> `SnapshotParameters.setViewport(...)` đúng bằng kích thước nội dung sơ đồ để
> chụp **toàn bộ**, rồi `finally` khôi phục lại zoom/translate cũ. File PNG được ghi
> bằng `com.giapha.util.PngWriter` — **tự viết** bằng `java.util.zip` (`Deflater` +
> `CRC32`) thay vì dùng `javax.imageio`, để module `com.giapha` không phải khai báo
> thêm `requires java.desktop`."

---

## PHẦN B — CÁC CA LỖI (error cases)

> Mỗi ca lỗi dưới đây đều **gây ra** trong ứng dụng thật, không phải mô phỏng.
> Mục tiêu là cho thấy **quy tắc nghiệp vụ được kiểm tra ở đúng tầng** và
> **thông báo lỗi rõ ràng bằng tiếng Việt**.

### B1. [3:00 – 3:15] LỖI — Nhập năm sinh sai

**Thao tác:**
1. Bấm **"+ Thêm thành viên"**.
2. Họ và tên: `Nguyễn Văn Sai Năm`
3. Năm sinh: `999` → bấm **"Lưu thành viên"**.

**Kết quả mong đợi:**
- Alert loại **ERROR**, tiêu đề **"Lỗi năm sinh"**, nội dung:
  > *"Năm sinh không hợp lệ: 999 (phải >= 1000 và <= 2026)"*

- Người dùng **vẫn ở lại màn hình form** (không bị chuyển trang).
- Bấm **Hủy** → quay về danh sách, **không** có dòng mới (tổng vẫn 31).

**Bổ sung (nếu còn thời gian):** thử năm sinh `2027` (năm tương lai) → cùng loại
lỗi, thông báo *"Năm sinh không hợp lệ: 2027 (phải >= 1000 và <= 2026)"*.

**Cách nói:**
> "Validation nằm ở `FamilyTreeService.validatePerson()`, hằng số
> `MIN_BIRTH_YEAR = 1000` là nguồn sự thật duy nhất. Ngoài ra tầng CSDL còn có
> `CHECK (birth_year >= 1000 AND birth_year <= 2100)` — tức là kể cả khi bỏ qua
> tầng ứng dụng và ghi thẳng bằng SQL, CSDL vẫn chặn (test TC-19 đã kiểm chứng
> điều này)."

---

### B2. [3:15 – 3:35] LỖI — Gán sai giới tính vai trò (BR6)

**Thao tác:**
1. Bấm **Hủy** để thoát form.
2. Bấm **"+ Thêm thành viên"**.
3. Họ và tên: `Nguyễn Văn Sai Vai Trò`, Năm sinh `2000`, Giới tính `Nam`.
4. Ở dropdown **Cha**, **chọn một người NỮ** — ví dụ `Vũ Thị Mẹ Hùng` (1939).
5. Bấm **"Lưu thành viên"**.

**Kết quả mong đợi:**
- Alert loại **ERROR**, tiêu đề **"Lỗi giới tính vai trò"**, nội dung:
  > *"Cha phải có giới tính là MALE nhưng thực tế là FEMALE"*

- Người dùng ở lại form, **không** lưu được.

**Cách nói:**
> "Đây là BR6. Ở tầng Service, `FamilyTreeService.addPerson()` kiểm tra
> `father.getGender() != Gender.MALE` → ném `InvalidParentGenderException`.
> Vì mỗi quy tắc có **một exception riêng**, Controller bắt được đúng loại và hiện
> đúng nguyên nhân — không phải một thông báo chung chung kiểu 'lỗi dữ liệu'."

> ⚠️ **Lưu ý khi demo:** trên giao diện, dropdown "Cha" **đã được lọc sẵn chỉ còn
> người Nam** và dropdown "Mẹ" chỉ còn người Nữ (logic ở
> `PersonFormView.refreshComboBoxes()`). Vì vậy trong lúc demo trực tiếp, thao tác
> này **có thể không khả dụng**. Nếu vậy, hãy demo bằng **cách thay thế** ở cuối
> mục này.

**🔄 Cách thay thế để demo BR6 chắc chắn (khuyến nghị):**

```bash
# Mở terminal khác, chạy test chứng minh BR6 — dùng database test riêng, không ảnh hưởng dữ liệu demo
mvn test -Dtest=LargeSampleDataTest#tc06_parentGender
```

Kết quả in ra:

```
TC-10  BR6 — assignFather(23, 28) trong khi id 28 là FEMALE
       → InvalidParentGenderException: "Cha phải có giới tính là MALE nhưng thực tế là FEMALE"
TC-10b BR6 — assignMother(29, 23) trong khi id 23 là MALE
       → InvalidParentGenderException: "Mẹ phải có giới tính là FEMALE nhưng thực tế là MALE"
```

> **Lưu ý về tên phương thức test:** trong `LargeSampleDataTest`, phương thức kiểm thử
> TC-10 tên là `tc06_parentGender` (đặt tên theo thứ tự cũ, nhưng `@DisplayName` và
> dòng bảng in ra là `TC-10`). Vì vậy lệnh chạy đúng là `#tc06_parentGender`.
> Nếu không chắc, chạy toàn bộ lớp: `mvn test -Dtest=LargeSampleDataTest`.

**Cách nói (khi demo bằng test):**
> "BR6 còn được bảo vệ **hai lớp**: ở tầng Service bằng exception riêng, và ở tầng
> CSDL bằng cột kiểu `ENUM('MALE','FEMALE')` — dù có gán sai bằng SQL thì kiểu dữ liệu
> vẫn đúng. Test TC-10 và TC-10b trong `LargeSampleDataTest` kiểm chứng điều này."

---

### B3. [3:35 – 3:55] LỖI — Tạo chu trình gia phả (BR3)

> **Đây là ca lỗi quan trọng nhất của demo** — thể hiện thuật toán DFS.

**Thao tác:**
1. Bấm **Hủy** để thoát form (nếu đang ở form).
2. Quay về danh sách. Gõ vào ô tìm kiếm: `Nguyễn Văn Tổ` → bấm Enter / bỏ qua,
   tìm dòng có **ID = 1**, họ tên `Nguyễn Văn Tổ` (sinh 1850, Nam, không có Cha/Mẹ).
3. Chọn dòng đó → bấm **"Sửa"**.
4. Ở dropdown **Cha**, chọn `Nguyễn Văn An` (id = 23, sinh 1960) — đây là **hậu duệ**
   của Nguyễn Văn Tổ.
5. Bấm **"Lưu thay đổi"**.

**Kết quả mong đợi:**
- Alert loại **ERROR**, tiêu đề **"Lỗi chu trình"**, nội dung:
  > *"Thao tác tạo chu trình trong cây gia phả: ..."*

  **KHÔNG** có câu *"Đã cập nhật thành công..."* — vì thao tác đã bị từ chối.
- Người dùng **ở lại màn hình sửa** (không bị điều hướng về danh sách).
- Quan hệ cha của `Nguyễn Văn Tổ` **không** bị gán (vẫn là "—" trong bảng).
- **Không có gì được ghi xuống CSDL** — kể cả thông tin cá nhân giữ nguyên bản cũ.
  **Ảnh đại diện cũng không bị đụng tới**: file ảnh trên đĩa giữ nguyên nội dung
  cũ, cột `photo_path` không đổi.
- **Lưu ý — đây là điểm cần nhấn mạnh:** luồng cập nhật là **nguyên tử**.
  `FamilyTreeService.updatePersonWithParents(...)` kiểm tra người cần sửa **tồn tại**
  rồi kiểm tra BR2 → BR1 → BR6 → BR3 cho **cả cha lẫn mẹ** **trước**, rồi mới ghi
  bằng **một** câu `UPDATE` duy nhất. Vì vậy khi BR3 bị vi phạm thì **không có gì
  được ghi cả** — kể cả thông tin cá nhân cũng giữ nguyên bản cũ. Đây là hành vi
  **đã được cố ý thiết kế và kiểm thử** (`AtomicUpdateAndBr5Test`,
  `EditFailureRollbackTest`) để tránh trạng thái "cập nhật một nửa".

**Cách nói:**
> "Đây là BR3 — chu trình gia phả. Thuật toán nằm ở
> `FamilyTreeService.checkCycle(childId, candidateParentId)`: ta **duyệt ngược** từ
> ứng viên cha đi lên các thế hệ tổ tiên; nếu gặp `childId` thì chắc chắn tạo
> chu trình. Dùng `Deque` + `push`/`pop` → đây là **DFS**. `Set<Integer> visited`
> là bắt buộc: nếu dữ liệu đã bị hỏng (chu trình do SQL thủ công tạo) mà không có
> `visited`, vòng lặp sẽ không bao giờ kết thúc → treo ứng dụng.
> Lưu ý: BR3 là quy tắc **duy nhất** không được bảo vệ ở tầng CSDL, vì MySQL không
> cho phép `CHECK` tham chiếu sang bảng khác. Đây là hạn chế đã được phân tích
> trong báo cáo."

---

### B4. [3:55 – 4:15] LỖI — Xóa người đang làm cha/mẹ (BR4)

**Thao tác:**
1. Bấm **"← Quay lại danh sách"** (nếu đang ở màn hình sửa).
2. Tìm dòng **ID = 3**, họ tên `Nguyễn Văn Cụ` (1880, Nam) — ô tìm kiếm gõ `Cụ`.
3. Chọn dòng đó → bấm **"Xóa"**.
4. Hộp thoại xác nhận *"Xóa thành viên: Nguyễn Văn Cụ"* → bấm **OK**.

**Kết quả mong đợi:**
- Alert loại **ERROR**, tiêu đề **"Không thể xóa thành viên"**, nội dung:
  > *"Thành viên này hiện đang đóng vai trò làm cha hoặc mẹ của người khác trong hệ
  > thống gia phả. Vui lòng thay đổi hoặc xóa các mối quan hệ con cái trước khi
  > thực hiện xóa."*

- Dòng `Nguyễn Văn Cụ` **vẫn còn** trong bảng. Tổng thành viên không đổi.

**Cách nói:**
> "BR4 được bảo vệ **hai lớp**: tầng Service — `FamilyTreeService.deletePerson()`
> gọi `checkPersonInUse()` rồi ném `PersonInUseException`, để hiện được thông báo
> tiếng Việt thân thiện; và tầng CSDL — hai khóa ngoại dùng `ON DELETE RESTRICT`
> nên MySQL tự chặn. Chọn `RESTRICT` thay vì `CASCADE` (không mất dữ liệu hàng loạt)
> hay `SET NULL` (không tạo 'mồ côi' ngầm)."

---

### B5. [4:15 – 4:30] CẢNH BÁO — Năm sinh cha không nhỏ hơn con (BR5)

> **Ca này đặc biệt quan trọng** vì BR5 **không chặn** thao tác — chỉ cảnh báo.

**Thao tác:**
1. Bấm **"+ Thêm thành viên"**.
2. Họ và tên: `Nguyễn Văn Cháu Ngoại`, Năm sinh `1980`, Giới tính `Nam`.
3. Ở dropdown **Cha**, chọn `Nguyễn Văn An` (id = 23, sinh **1960**).
4. Bấm **"Lưu thành viên"**.

**Kết quả mong đợi:**
- Alert loại **WARNING**, tiêu đề **"Cảnh báo năm sinh (BR5)"**, header:
  *"Năm sinh của cha/mẹ không nhỏ hơn năm sinh của con"*, nội dung:
  > *"• Năm sinh của cha (Nguyễn Văn An, 1960) không nhỏ hơn năm sinh của con
  > (Nguyễn Văn Cháu Ngoại, 1980)."*
  > *"Bạn vẫn muốn tiếp tục thao tác này không?"*

- **2 nút: "Tiếp tục" và "Hủy"**.
- Bấm **"Hủy"** → quay lại form, **không lưu** (tổng vẫn 31).
- Bấm **"Lưu thành viên"** lại → chọn **"Tiếp tục"** → lần này **được lưu**,
  tổng thành viên tăng lên **32**.

**Cách nói:**
> "BR5 là quy tắc **mức cảnh báo**, không phải ràng buộc cứng — khác 5 quy tắc còn lại.
> Lý do: dữ liệu gia phả lịch sử đôi khi ghi nhầm năm sinh, chặn cứng sẽ khiến
> người dùng không lưu được. Cơ chế: `FamilyTreeService.checkBirthYearWarning()`
> trả về `List<String>` — **không ném exception**; `PersonController` hiện Alert
> với 2 lựa chọn và chỉ tiếp tục lưu khi người dùng chọn 'Tiếp tục'."

---

### B6. [4:30 – 4:50] Kiểm thử tự động

**Thao tác (mở terminal khác):**

```bash
mvn clean test
```

**Kết quả mong đợi (cuối output):**

```
[INFO] Tests run: 201, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Cách nói:**
> "Bộ test dùng JUnit 5, 201 test mặc định (thêm 30 test JavaFX chạy riêng bằng `mvn test -Pjavafx-tests`), tất cả PASS. Điểm quan trọng: **mọi** test ghi
> vào CSDL đều dùng database riêng tên kết thúc bằng `_test` — được tự tạo ở
> `@BeforeAll` và tự xóa ở `@AfterAll`. Ngoài ra `TestDatabaseManager.assertSafeToDrop()`
> bắt buộc tên database phải kết thúc bằng `_test`, nếu không khớp thì ném
> `IllegalStateException` và **không DROP** — đảm bảo không bao giờ xóa nhầm database
> chính `gia_pha`. Database demo của tôi đang chạy vẫn nguyên vẹn."

---

### B7. [4:50 – 5:00] Tổng kết

**Cách nói (gợi ý):**
> "Tóm lại: hệ thống gồm 5 tầng phân tầng — View → Controller → Service → Repository
> → Database. Tầng Service chỉ phụ thuoộc interface `PersonRepository`, **không**
> import bất kỳ JDBC API nào — đó là Dependency Inversion, và chính điều đó cho
> phép viết được bộ test cô lập dữ liệu như vừa rồi. 6 quy tắc nghiệp vụ được
> kiểm ở tầng Service và bảo vệ bổ sung ở tầng CSDL bằng CHECK, TRIGGER và
> `ON DELETE RESTRICT`. Phần sơ đồ 2D là component JavaFX thuần — nhận danh sách
> `Person` rồi tự dựng bố cục, không gọi xuống tầng dưới. Cảm ơn thầy/cô!"

---

## PHẦN C — BẢNG NHÁNH LÚC ĐIỂM ĐÁNH GIÁ

| Câu hỏi / nhận xét có thể gặp | Trả lời ngắn |
|---|---|
| "Sao phải INSERT rồi UPDATE 2 lần khi thêm người?" | Vì ảnh được copy **sau khi** CSDL sinh `id`, mà tên file là `person_<id>.<đuôi>`. Không có `id` thì không đặt được tên file. |
| "Đường dẫn ảnh lưu trong DB là gì?" | Đường dẫn **tương đối**: `images/person_31.png`. Nhờ vậy DB không phụ thuộc vị trí cài đặt; đánh đổi là app phải chạy từ thư mục gốc dự án. |
| "Tìm kiếm chạy ở DB hay ở client?" | Ở **client**: `PeopleListView` dùng `FilteredList` lọc trên bộ nhớ đã nạp. Service vẫn có `searchPersons()` dùng `SELECT ... LIKE ?` qua `JdbcPersonRepository.search()`. |
| "Số thế hệ tính thế nào?" | `PeopleListView.calculateGenerations()` — đệ quy `getDepth()` theo cha/mẹ, lấy giá trị max. Với bộ mẫu 5 thế hệ là kết quả đúng. |
| "Có hỗ trợ người chưa biết cha/mẹ không?" | Có — `father_id` / `mother_id` là `NULL`, giao diện hiển thị "—" và cho phép gán sau bằng `assignFather()` / `assignMother()`. |
| "Chu trình có bị chặn ở tầng CSDL không?" | **Không** — BR3 là quy tắc duy nhất chỉ bảo vệ ở tầng Service, vì MySQL/MariaDB không cho phép `CHECK` tham chiếu sang bảng khác. Đây là hạn chế đã nêu trong báo cáo. |
| "Có thể xóa hàng loạt không?" | Chưa triển khai. Xóa đơn lẻ đã bị chặn bởi BR4 nếu người đó đang làm cha/mẹ. |
| "In ấn cần cài thêm gì?" | Không. Dùng `javafx.print.PrinterJob` của JavaFX — không thêm dependency nào. Nếu máy không có máy in, ứng dụng báo rõ và gợi ý dùng Xuất PNG. |
| "Tại sao tự viết PngWriter?" | Để tránh `requires java.desktop` (cho `javax.imageio`) và `requires javafx.swing` trong `module-info.java` — giữ module `com.giapha` gọn với đúng 4 dependency. |

---

## PHẦN D — NHÁNH DỰ PHÒNG (nếu có thời gian)

### D1. Demo gỡ quan hệ rồi xóa được (để đối chiếu với B4)

1. Chọn `Nguyễn Văn Cụ` (id=3) → **"Sửa"**.
2. Ở dropdown **Cha**, chọn **"— Không có —"** → **"Lưu thay đổi"**.
   → Alert *"Đã cập nhật thành công thông tin thành viên. • Đã gỡ quan hệ cha thành công."*
3. Tương tự với dropdown **Mẹ**.
4. Quay lại danh sách, chọn `Nguyễn Văn Cụ` → **"Xóa"** → OK.
   → Alert *"Đã xóa thành viên có ID = 3 khỏi hệ thống gia phả."*
   → Tổng thành viên giảm 1.

**Cách nói:**
> "Sau khi gỡ hết quan hệ, xóa được. Đó là hành vi **cố ý**: Service không tự ý set
> NULL quan hệ của con — người dùng phải chủ động gỡ bằng `clearFather()` /
> `clearMother()`, để không mất thông tin ngoài ý muốn."

### D2. Demo in ấn

1. Bấm **"🖨 In"**.
   - Nếu máy có máy in cài sẵn → hộp thoại in của hệ thống hiện ra → chọn máy in →
     OK → Alert *"Đã gửi lệnh in"*.
   - Nếu không → Alert *"Không tìm thấy máy in — Không phát hiện được máy in nào trên
     hệ thống. Vui lòng cài đặt máy in hoặc dùng chức năng Xuất PNG."*

### D3. Demo thuật toán DFS (nếu giảng viên hỏi sâu)

Mở `src/main/java/com/giapha/service/FamilyTreeService.java` tại hàm
`checkCycle(int childId, int candidateParentId)` và giải thích từng dòng:

```java
private boolean checkCycle(int childId, int candidateParentId) {
    if (childId == candidateParentId) return true;          // BR1 — chu trình độ dài 1

    Set<Integer> visited = new HashSet<>();                  // chống lặp vô hạn
    Deque<Integer> stack = new ArrayDeque<>();               // DFS (LIFO)
    stack.push(candidateParentId);
    visited.add(candidateParentId);

    while (!stack.isEmpty()) {
        int currentId = stack.pop();
        if (currentId == childId) return true;               // gặp tổ tiên → có chu trình

        Person current = personRepository.findById(currentId);
        if (current != null) {
            if (current.getFather() != null) {
                int fId = current.getFather().getId();
                if (fId == childId) return true;
                if (visited.add(fId)) stack.push(fId);        // add trả false nếu đã thấy
            }
            if (current.getMother() != null) { /* tương tự */ }
        }
    }
    return false;
}
```

**Điểm cần nhấn:** `visited.add(fId)` trả `false` khi id đã có trong set → dùng
chính kết quả trả về để vừa kiểm tra "đã thăm" vừa đánh dấu, chỉ một lọt `HashSet`.

---

## PHỤ LỤC — NHÁNH THAY THẾ CHO TỪNG CA LỖI (dùng khi giao diện chặn)

> Nếu một ca lỗi không kích hoạt được trên giao diện (do dropdown đã lọc sẵn),
> dùng nhánh thay thế bằng **test tự động** — cùng một logic nghiệp vụ, chạy trên
> database test riêng, **không** ảnh hưởng dữ liệu đang demo.

| Ca lỗi | Trên giao diện | Lệnh test thay thế | Exception kỳ vọng |
|---|---|---|---|
| Năm sinh sai | ✅ Có (nhập `999` hoặc `2027`) | `mvn test -Dtest=LargeSampleDataTest#tc12_birthYearTooSmall` | `InvalidBirthYearException` |
| Sai giới tính vai trò (BR6) | ⚠️ Dropdown đã lọc | `mvn test -Dtest=LargeSampleDataTest#tc06_parentGender` | `InvalidParentGenderException` |
| Tự làm cha/mẹ (BR1) | ⚠️ Dropdown loại bỏ chính mình | `mvn test -Dtest=LargeSampleDataTest#tc05_selfParent` | `SelfParentException` |
| Cha trùng mẹ (BR2) | ⚠️ Dropdown tách theo giới tính | `mvn test -Dtest=LargeSampleDataTest#tc06_sameParent` | `SameParentException` |
| Tạo chu trình (BR3) | ✅ Có (Sửa → gán cha là hậu duệ) | `mvn test -Dtest=LargeSampleDataTest#tc07_cycle` | `CycleDetectedException` |
| Xóa người đang là cha/mẹ (BR4) | ✅ Có | `mvn test -Dtest=LargeSampleDataTest#tc08_personInUse` | `PersonInUseException` |
| Trigger CSDL chặn tự làm cha (BR1 ở tầng DB) | ❌ Không (cần SQL) | `mvn test -Dtest=LargeSampleDataTest#tc20_dbSelfParentTrigger` | `SQLException` |

> **Lưu ý về tên phương thức test:** các mã test trong `LargeSampleDataTest` được
> đặt tên theo mã test case trong `test-data/test-cases.md` (TC-05, TC-06, …),
> **trừ TC-10** (BR6) vì phương thức vẫn mang tên cũ là `tc06_parentGender`.
> Nếu `-Dtest=...#tcXX` không chạy, chạy toàn bộ lớp:
> `mvn test -Dtest=LargeSampleDataTest` — output sẽ in ra đầy đủ 22 dòng bảng
> test case Markdown, trong đó có cột "Kết quả thực tế" và trạng thái PASS.

---

## CHECKLIST TRƯỚC KHI LÊN SÂN

- [ ] MySQL/MariaDB đang chạy → `mysql -u root -e "SELECT COUNT(*) FROM gia_pha.person;"` trả **30**
- [ ] `mvn clean compile` chạy không lỗi
- [ ] Chạy `mvn javafx:run` thử 1 lần từ thư mục gốc → app mở, không lỗi kết nối
- [ ] Có sẵn 1 file ảnh `.png` để demo chọn ảnh đại diện
- [ ] Có sẵn 1 file ảnh `.jpg` **không hợp lệ** (đổi đuôi thành `.gif` chẳng hạn) để demo lỗi định dạng ảnh
- [ ] Mở sẵn file `FamilyTreeService.java` tại hàm `checkCycle` trong IDE (phòng khi bị hỏi sâu)
- [ ] Mở sẵn `docs/uml/use-case.png`, `docs/uml/class-diagram-service-repository.png`,
      `docs/uml/sequence-add-person.png` (phòng khi giảng viên yêu cầu xem sơ đồ)
- [ ] Terminal sẵn để chạy `mvn clean test` (nếu mạng không có, chạy offline với `mvn -o clean test`)
