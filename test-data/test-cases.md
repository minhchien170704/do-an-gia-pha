# BẢNG TEST CASE — BỘ DỮ LIỆU MẪU 30 NGƯỜI / 5 THẾ HỆ

Thư mục này chứa **bộ dữ liệu kiểm thử mở rộng** cho dự án Quản lý Sơ đồ Gia phả
(`do-an-gia-pha`), dùng để kiểm chứng các quy tắc nghiệp vụ BR1–BR6 trên một cây
gia phả có quy mô thực tế (30 người, 5 thế hệ, 4 nhánh dòng họ).

| Mục | Nội dung |
|---|---|
| File dữ liệu SQL | [`sample-data.sql`](./sample-data.sql) |
| Bộ test JUnit 5 tương ứng | `src/test/java/com/giapha/testdata/LargeSampleDataTest.java` |
| Database riêng dùng cho test | `gia_pha_test` (tự động tạo ở đầu, tự động DROP ở cuối) |
| Lệnh chạy | `mvn test -Dtest=LargeSampleDataTest` |
| Giữ lại DB test để debug | `mvn test -Dtest=LargeSampleDataTest -Dkeep.test.db=true` |
| Kết quả | **22/22 test case PASS** |

---

## 1. CẤU TRÚC BỘ DỮ LIỆU

### 1.1. Sơ đồ cây (5 thế hệ)

```
Thế hệ 1 (1850 – 1855)                     2 người
 ├── Nguyễn Văn Tổ (1850, M)  ×  Trần Thị Tổ (1855, F)
 │
 ├────────────────────────────┴──────────────────────────────┐
 │                                                           │
Thế hệ 2 (1880 – 1887)                     4 người          │
 ├── Nguyễn Văn Cụ (1880, M)  ×  Phạm Thị Mẹ Cụ (1884, F)   │
 └── Nguyễn Văn Nhị (1883, M) ×  Lý Thị Mẹ Nhị (1887, F)    │
 │                                                           │
 ├──────────────────────────┬────────────────────────────────┘
 │                          │
Thế hệ 3 (1905 – 1916)                8 người
 ├── Nguyễn Văn Đại (1905, M)   ×  Đỗ Thị Mẹ Đại (1909, F)
 ├── Nguyễn Văn Tị (1908, M)    ×  Hoàng Thị Mẹ Tị (1912, F)
 ├── Nguyễn Văn Tuấn (1910, M)  ×  Bùi Thị Mẹ Tuấn (1914, F)
 └── Nguyễn Văn Thắng (1912, M) ×  Mai Thị Mẹ Thắng (1916, F)
 │
 ├──────────────────────────┬──────────────────────────────────┐
 │                          │                                  │
Thế hệ 4 (1935 – 1943)                8 người                  │
 ├── Nguyễn Văn Hùng (1935, M)  ×  Vũ Thị Mẹ Hùng (1939, F)   │
 ├── Nguyễn Văn Khoa (1938, M)  ×  Đinh Thị Mẹ Khoa (1941, F)  │
 ├── Nguyễn Văn Sơn (1936, M)   ×  Trịnh Thị Mẹ Sơn (1940, F)  │
 └── Nguyễn Văn Trí (1940, M)   ×  Chu Thị Mẹ Trí (1943, F)   │
 │                                                           │
 ├──────────────────────┬────────────────────────────────────┘
 │                      │
Thế hệ 5 (1960 – 1970)                8 người
 ├── Nguyễn Văn An (1960, M)       × Vũ Thị Mẹ Hùng
 ├── Nguyễn Văn Bình (1962, M)     × Vũ Thị Mẹ Hùng
 ├── Nguyễn Văn Cường (1965, M)   × Đinh Thị Mẹ Khoa
 ├── Nguyễn Văn Dũng (1968, M)     × Đinh Thị Mẹ Khoa
 ├── Nguyễn Văn Giang (1963, M)   × Trịnh Thị Mẹ Sơn
 ├── Nguyễn Văn Hà (1966, F)      × Trịnh Thị Mẹ Sơn
 ├── Nguyễn Văn Huy (1967, M)     × Chu Thị Mẹ Trí
 └── Nguyễn Văn Khang (1970, M)   × Chu Thị Mẹ Trí
```

### 1.2. Bảng id → thông tin (tham chiếu nhanh)

| id | Họ tên | Năm sinh | Giới tính | id cha | id mẹ | Thế hệ |
|---|---|---|---|---|---|---|
| 1 | Nguyễn Văn Tổ | 1850 | MALE | — | — | 1 |
| 2 | Trần Thị Tổ | 1855 | FEMALE | — | — | 1 |
| 3 | Nguyễn Văn Cụ | 1880 | MALE | 1 | 2 | 2 |
| 4 | Phạm Thị Mẹ Cụ | 1884 | FEMALE | — | — | 2 |
| 5 | Nguyễn Văn Nhị | 1883 | MALE | 1 | 2 | 2 |
| 6 | Lý Thị Mẹ Nhị | 1887 | FEMALE | — | — | 2 |
| 7 | Nguyễn Văn Đại | 1905 | MALE | 3 | 4 | 3 |
| 8 | Đỗ Thị Mẹ Đại | 1909 | FEMALE | — | — | 3 |
| 9 | Nguyễn Văn Tị | 1908 | MALE | 3 | 4 | 3 |
| 10 | Hoàng Thị Mẹ Tị | 1912 | FEMALE | — | — | 3 |
| 11 | Nguyễn Văn Tuấn | 1910 | MALE | 5 | 6 | 3 |
| 12 | Bùi Thị Mẹ Tuấn | 1914 | FEMALE | — | — | 3 |
| 13 | Nguyễn Văn Thắng | 1912 | MALE | 5 | 6 | 3 |
| 14 | Mai Thị Mẹ Thắng | 1916 | FEMALE | — | — | 3 |
| 15 | Nguyễn Văn Hùng | 1935 | MALE | 7 | 8 | 4 |
| 16 | Vũ Thị Mẹ Hùng | 1939 | FEMALE | — | — | 4 |
| 17 | Nguyễn Văn Khoa | 1938 | MALE | 7 | 8 | 4 |
| 18 | Đinh Thị Mẹ Khoa | 1941 | FEMALE | — | — | 4 |
| 19 | Nguyễn Văn Sơn | 1936 | MALE | 11 | 12 | 4 |
| 20 | Trịnh Thị Mẹ Sơn | 1940 | FEMALE | — | — | 4 |
| 21 | Nguyễn Văn Trí | 1940 | MALE | 11 | 12 | 4 |
| 22 | Chu Thị Mẹ Trí | 1943 | FEMALE | — | — | 4 |
| 23 | Nguyễn Văn An | 1960 | MALE | 15 | 16 | 5 |
| 24 | Nguyễn Văn Bình | 1962 | MALE | 15 | 16 | 5 |
| 25 | Nguyễn Văn Cường | 1965 | MALE | 17 | 18 | 5 |
| 26 | Nguyễn Văn Dũng | 1968 | MALE | 17 | 18 | 5 |
| 27 | Nguyễn Văn Giang | 1963 | MALE | 19 | 20 | 5 |
| 28 | Nguyễn Văn Hà | 1966 | FEMALE | 19 | 20 | 5 |
| 29 | Nguyễn Văn Huy | 1967 | MALE | 21 | 22 | 5 |
| 30 | Nguyễn Văn Khang | 1970 | MALE | 21 | 22 | 5 |

---

## 2. BẢNG TEST CASE

> **Cột "Kết quả thực tế"** được sinh tự động bởi `LargeSampleDataTest` — mỗi test case
> in ra một dòng bảng Markdown giống hệt bảng dưới đây khi chạy `mvn test -Dtest=LargeSampleDataTest`.

| Mã | Mô tả | Dữ liệu vào | Kết quả mong đợi | Kết quả thực tế | Trạng thái |
|---|---|---|---|---|---|
| TC-01 | Nạp bộ dữ liệu mẫu 30 người / 5 thế hệ | Nạp `sample-data.sql` vào DB `gia_pha_test` | 30 bản ghi | 30 bản ghi | PASS |
| TC-02 | Số thế hệ lớn nhất trong cây gia phả | Duyệt chiều sâu toàn bộ 30 người | 5 thế hệ | 5 thế hệ | PASS |
| TC-03 | Số hậu duệ của "Nguyễn Văn Tổ" (id=1) | `getDescendants(1)` | 18 người | 18 người | PASS |
| TC-04 | Số tổ tiên của "Nguyễn Văn An" (id=23) | `getAncestors(23)` | 8 người | 8 người | PASS |
| TC-05 | **BR1** — assignFather(7, 7) | Người con = người cha (id 7) | Ném `SelfParentException` | `SelfParentException`: "Một người không thể là cha hoặc mẹ của chính mình (id = 7)" | PASS |
| TC-06 | **BR2** — assignFather(23, 16) trong khi 16 là mẹ của 23 | id 16 (FEMALE) là mẹ hiện tại của id 23 | Ném `SameParentException` | `SameParentException`: "Cha và Mẹ không thể là cùng một người" | PASS |
| TC-07 | **BR3** — assignFather(1, 23) trong khi 23 là hậu duệ của 1 | Gán cháu nội làm cha của ông bà | Ném `CycleDetectedException` | `CycleDetectedException`: "Gán cha tạo chu trình vòng lặp trong cây gia phả" | PASS |
| TC-08 | **BR4** — deletePerson(3) khi id 3 đang làm cha của 2 người | id 3 là cha của id 7 và id 9 | Ném `PersonInUseException` | `PersonInUseException`: "Không thể xóa Person có id = 3 vì đang được dùng làm cha hoặc mẹ trong gia phả" | PASS |
| TC-09 | **BR5** — checkBirthYearWarning(con 1900, cha 1910) | Cha (1910) ≥ Con (1900) | 1 cảnh báo | 1 cảnh báo | PASS |
| TC-09b | **BR5** — số vi phạm trong bộ dữ liệu mẫu | Quét 30 người, so năm sinh cha/mẹ vs con | 0 vi phạm | 0 vi phạm | PASS |
| TC-10 | **BR6** — assignFather(23, 28) trong khi id 28 là FEMALE | id 28 = Nguyễn Văn Hà (FEMALE) | Ném `InvalidParentGenderException` | `InvalidParentGenderException`: "Cha phải có giới tính là MALE nhưng thực tế là FEMALE" | PASS |
| TC-10b | **BR6** — assignMother(29, 23) trong khi id 23 là MALE | id 23 = Nguyễn Văn An (MALE) | Ném `InvalidParentGenderException` | `InvalidParentGenderException`: "Mẹ phải có giới tính là FEMALE nhưng thực tế là MALE" | PASS |
| TC-11 | Chấp nhận năm sinh từ 1000 trở lên | `addPerson("Nguyễn Văn Cổ", 1000, MALE)` | Thành công, id > 0 | id = 31 | PASS |
| TC-12 | Từ chối năm sinh nhỏ hơn 1000 | `addPerson(..., 999, MALE)` | Ném `InvalidBirthYearException` | `InvalidBirthYearException`: "Năm sinh không hợp lệ: 999 (phải >= 1000 và <= 2026)" | PASS |
| TC-13 | Từ chối năm sinh trong tương lai | `addPerson(..., 2027, MALE)` (năm hiện tại + 1) | Ném `InvalidBirthYearException` | `InvalidBirthYearException`: "Năm sinh không hợp lệ: 2027 (phải >= 1000 và <= 2026)" | PASS |
| TC-14 | Tìm kiếm thành viên theo họ tên | `searchPersons("Nguyễn Văn")` | 19 kết quả | 19 kết quả | PASS |
| TC-14b | Tìm kiếm với từ khóa không tồn tại | `searchPersons("Không Tồn Tại XYZ")` | 0 kết quả | 0 kết quả | PASS |
| TC-15 | Từ chối họ tên rỗng | `addPerson("   ", 1990, MALE)` | Ném `IllegalArgumentException` | "Họ và tên không được để trống" | PASS |
| TC-16 | Từ chối họ tên dài trên 100 ký tự | `addPerson("A"×101, 1990, MALE)` | Ném `IllegalArgumentException` | "Độ dài họ và tên phải từ 1 đến 100 ký tự" | PASS |
| TC-17 | Xóa được thành viên lá (không có con) | `deletePerson(30)` — Nguyễn Văn Khang | Xóa thành công, còn 29 bản ghi | 29 bản ghi còn lại | PASS |
| TC-18 | Gỡ quan hệ cha/mẹ rồi mới xóa được | `clearFather(23)`, `clearFather(24)` → `deletePerson(15)` | Xóa thành công | Đã xóa khỏi DB | PASS |
| TC-19 | Ràng buộc CHECK của CSDL chặn năm sinh không hợp lệ | `INSERT ... birth_year = 500` (bỏ qua tầng Service) | CSDL từ chối (SQLException) | `SQLException`: CONSTRAINT `chk_birth_year` failed for `gia_pha_test`.`person` | PASS |
| TC-20 | Trigger CSDL chặn tự làm cha/mẹ chính mình | `UPDATE PERSON SET father_id = id WHERE id = 23` | CSDL từ chối (SQLException) | `SQLException`: Một người không thể là cha hoặc mẹ của chính mình | PASS |
| TC-21a | **An toàn DROP** — assertSafeToDrop("gia_pha_test") | Tên DB kết thúc bằng `_test` | Không ném exception | Không ném exception | PASS |
| TC-21b | **An toàn DROP** — assertSafeToDrop("gia_pha") | Tên database CHÍNH | Ném `IllegalStateException` | "Từ chối DROP database \"gia_pha\": tên database bắt buộc phải kết thúc bằng \"_test\" để bảo đảm không xóa nhầm dữ liệu chính." | PASS |
| TC-21c | **An toàn DROP** — assertSafeToDrop("") và (null) | Tên rỗng / null | Ném `IllegalStateException` | Cả hai đều ném IllegalStateException | PASS |
| TC-21d | **An toàn DROP** — assertSafeToDrop("test_gia_pha") | Chứa `_test` ở giữa nhưng không kết thúc | Ném `IllegalStateException` | Ném IllegalStateException | PASS |

**Tổng kết: 22 test case — 22 PASS, 0 FAIL.**

---

## 3. MA TRẬN PHỦ QUY TẮC NGHIỆP VỤ

| Quy tắc | Mô tả | Test case phủ | Kết quả |
|---|---|---|---|
| **BR1** | SELF PARENT — không tự làm cha/mẹ chính mình | TC-05, TC-20 | PASS |
| **BR2** | FATHER ≠ MOTHER — cha và mẹ phải khác nhau | TC-06 | PASS |
| **BR3** | NO CYCLE — không tạo chu trình vòng lặp | TC-07 | PASS |
| **BR4** | PERSON IN USE — không xóa người đang làm cha/mẹ | TC-08, TC-18 | PASS |
| **BR5** | PARENT BIRTH YEAR — cảnh báo năm sinh cha/mẹ (mức WARNING) | TC-09, TC-09b | PASS |
| **BR6** | GENDER ROLE — cha phải MALE, mẹ phải FEMALE | TC-10, TC-10b | PASS |
| **Validation** | Năm sinh trong khoảng `1000 .. Year.now()` | TC-11, TC-12, TC-13 | PASS |
| **Validation** | Họ tên 1–100 ký tự, không rỗng | TC-15, TC-16 | PASS |
| **Truy vấn** | Tổ tiên / hậu duệ / tìm kiếm | TC-03, TC-04, TC-14, TC-14b | PASS |
| **CRUD** | Thêm / xóa / gỡ quan hệ | TC-11, TC-17, TC-18 | PASS |
| **CSDL** | CHECK constraint + Trigger bảo vệ ở tầng DB | TC-19, TC-20 | PASS |
| **An toàn** | Ràng buộc DROP chỉ database kết thúc bằng `_test` | TC-21a…TC-21d | PASS |

---

## 4. HƯỚNG DẪN SỬ DỤNG

### 4.1. Nạp dữ liệu mẫu vào database chính (`gia_pha`)

Dùng để **kiểm thử thủ công** trên giao diện JavaFX (`mvn javafx:run`):

```bash
mysql -u root -p < test-data/sample-data.sql
```

> ⚠️ Script **xoá toàn bộ** dữ liệu hiện có trong bảng `PERSON` (kể cả 7 bản ghi mẫu
> nguyên bản của `database/schema.sql`).
>
> **Khôi phục lại dữ liệu gốc:**
> ```bash
> mysql -u root -p < database/schema.sql
> ```

### 4.2. Chạy bộ test JUnit 5

```bash
# Chỉ bộ test dữ liệu mẫu mở rộng
mvn test -Dtest=LargeSampleDataTest

# Giữ lại database test (gia_pha_test) sau khi chạy để debug trực tiếp
mvn test -Dtest=LargeSampleDataTest -Dkeep.test.db=true

# Toàn bộ bộ test mặc định (gồm cả LargeSampleDataTest)
mvn clean test
```

**Vòng đời database test:**
1. `@BeforeAll` → `DROP DATABASE IF EXISTS` + `CREATE DATABASE gia_pha_test` (làm mới hoàn toàn mỗi lần chạy).
2. `@AfterAll` → mặc định **DROP** database test; nếu truyền `-Dkeep.test.db=true` thì **giữ lại**.

**Ràng buộc an toàn:** trước khi DROP, hàm `assertSafeToDrop()` kiểm tra tên database
bắt buộc phải **kết thúc bằng `_test`**. Nếu không khớp (ví dụ `gia_pha`), hàm ném
`IllegalStateException` và **không** thực hiện DROP — đảm bảo không bao giờ xóa nhầm
database chính `gia_pha`.

Nhờ các ràng buộc trên, dữ liệu mẫu 7 bản ghi của database chính `gia_pha`
**không bị ảnh hưởng**.

### 4.3. Quy ước khi thêm test case mới

1. Thêm dữ liệu mẫu vào `test-data/sample-data.sql` (giữ nguyên cấu trúc 5 thế hệ).
2. Thêm một `@Test` mới trong `LargeSampleDataTest` với `@Order` tăng dần.
3. Gọi `record(mã, mô tả, kết quả mong đợi, kết quả thực tế, điều kiện đạt)` để
   dòng kết quả được in ra tự động.
4. Cập nhật bảng ở mục **2. BẢNG TEST CASE** của file này cho khớp với output thực tế.
