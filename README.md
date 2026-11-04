# Hệ Thống Quản Lý Sơ Đồ Gia Phả (Family Tree Management)

Dự án ứng dụng Desktop quản lý phả hệ và cây gia phả xây dựng bằng **Java 25**, **JavaFX 23** và **MySQL/MariaDB**, tuân thủ nghiêm ngặt các nguyên lý lập trình hướng đối tượng (OOP) và kiến trúc phân lớp chuẩn mực.

---

## 1. Tổng Quan Dự Án (Project Overview)
Hệ thống hỗ trợ gia đình, dòng tộc lưu trữ, tra cứu và hiển thị trực quan thông tin các thế hệ thành viên trong cây gia phả. Ứng dụng chú trọng vào:
- Tính toàn vẹn của quan hệ phả hệ (ngăn chặn chu trình vòng lặp, kiểm soát quan hệ huyết thống).
- Giao diện trực quan hiện đại, thân thiện, đồng bộ theo Design System riêng biệt.
- Kiến trúc module hóa rõ ràng, dễ dàng mở rộng và bảo trì.

---

## 2. Tính Năng Chính (Features)
- **Quản lý thành viên**: Thêm mới, cập nhật hồ sơ, tra cứu thông tin chi tiết (họ tên, năm sinh, giới tính, ảnh đại diện).
- **Thiết lập quan hệ phả hệ**: Gán cha (`assignFather`), gán mẹ (`assignMother`) với kiểm tra tự động các ràng buộc nghiệp vụ.
- **Phát hiện chu trình (Cycle Detection)**: Tự động ngăn chặn các quan hệ vòng lặp phi logic trong gia phả bằng thuật toán duyệt đồ thị (DFS).
- **Truy vấn phả hệ đa thế hệ**:
  - Truy vết toàn bộ tổ tiên tiền bối (`getAncestors`).
  - Tìm kiếm toàn bộ các thế hệ con cháu hậu duệ (`getDescendants`).
- **Xóa an toàn**: Ngăn chặn xóa các thành viên đang đóng vai trò làm cha hoặc mẹ của người khác trong hệ thống.
- **Trực quan hóa gia phả**: Hiển thị cây gia phả đa tầng với giao diện JavaFX hiện đại.

---

## 3. Công Nghệ Sử Dụng (Technology Stack)
- **Ngôn ngữ**: Java 25 (OpenJDK 25.0.2)
- **Hệ thống Module**: Java Platform Module System (`module-info.java`)
- **UI Framework**: JavaFX 23.0.2 (`javafx-controls`, `javafx-fxml`)
- **Build Tool**: Apache Maven 3.9.16
- **Hệ quản trị CSDL**: MySQL / MariaDB (hỗ trợ `utf8mb4`)
- **Kết nối CSDL**: JDBC thuần với `mysql-connector-j` 9.2.0
- **Kiểm thử tự động**: JUnit 5 (`junit-jupiter` 5.10.2) + `maven-surefire-plugin` 3.2.5
- **Mô hình hóa (Modeling)**: PlantUML

---

## 4. Kiến Trúc Hệ Thống (Architecture)
Dự án được tổ chức theo kiến trúc phân tầng (Layered Architecture):

```
JavaFX UI (View Layer)
       ↓
PersonController (Controller Layer)
       ↓
FamilyTreeService (Business Service Layer)
       ↓
PersonRepository (Data Access Interface)
       ↓
JdbcPersonRepository (Data Access Implementation)
       ↓
DatabaseConnection (JDBC Factory)
       ↓
MySQL / MariaDB Database
```

- **Separation of Concerns**: Tách biệt hoàn toàn giữa tầng giao diện, xử lý nghiệp vụ và truy xuất cơ sở dữ liệu.
- **Dependency Inversion**: `FamilyTreeService` phụ thuộc vào interface `PersonRepository`, không phụ thuộc trực tiếp vào triển khai JDBC hay kết nối cơ sở dữ liệu.

---

## 5. Cấu Trúc Thư Mục (Project Structure)
```
do-an-gia-pha/
├── pom.xml
├── README.md
├── AGENTS.md
├── .gitignore
├── database/
│   └── schema.sql                  # Định nghĩa bảng PERSON, trigger & 7 bản ghi mẫu
├── docs/
│   └── uml/                        # Toàn bộ PlantUML (.puml) và ảnh sơ đồ (.png)
├── test-data/
│   ├── sample-data.sql             # Bộ dữ liệu mẫu 30 người / 5 thế hệ
│   └── test-cases.md               # Bảng test case (mã, mô tả, dữ liệu vào, KQ mong đợi, KQ thực tế)
├── src/
    ├── main/
    │   ├── java/
    │   │   ├── module-info.java
    │   │   └── com/giapha/
    │   │       ├── Main.java
    │   │       ├── model/          # Person, Gender
    │   │       ├── exception/      # Custom Exceptions
    │   │       ├── database/       # DatabaseConnection
    │   │       ├── repository/     # PersonRepository, JdbcPersonRepository
    │   │       ├── service/        # FamilyTreeService
    │   │       ├── controller/     # PersonController
    │   │       ├── util/           # ImageManager (quản lý ảnh đại diện)
    │   │       │                   # PngWriter (ghi ảnh PNG thuần java.util.zip)
    │   │       └── view/           # JavaFX Views & Design System
    │   │                           # (PersonAvatar: component ảnh đại diện dùng chung)
    │   │                           # (FamilyTreeDiagram: sơ đồ cây gia phả 2D)
    │   └── resources/
    │       ├── css/                # style.css
    │       ├── fxml/               # FXML layouts
    │       └── images/             # Icon & hình ảnh tĩnh
    │                               # (Ảnh đại diện người dùng được copy vào ./images/ lúc runtime)
    └── test/
        └── java/
            └── com/giapha/
                ├── support/        # TestDatabase, FxTestSupport (tiện ích dùng chung)
                ├── database/       # DatabaseConnectionTest
                ├── service/        # FamilyTreeServiceTest
                ├── controller/     # PersonControllerTest
                ├── util/           # ImageManagerTest, PngWriterTest
                ├── testdata/       # LargeSampleDataTest (bộ dữ liệu 30 người / 5 thế hệ)
                └── view/           # AvatarFallbackTest, JavaFxViewTest (tag "javafx")
```

---

## 6. Quy Tắc Nghiệp Vụ (Business Rules)
1. **BR1 — Self Parent**: Một người không thể là cha hoặc mẹ của chính mình (`SelfParentException`).
2. **BR2 — Father != Mother**: Cha và mẹ của cùng một người không được là cùng một người (`SameParentException`).
3. **BR3 — No Cycle**: Cấm chu trình phả hệ; phát hiện qua DFS traversal (`CycleDetectedException`).
4. **BR4 — Person In Use**: Không cho phép xóa người đang là cha/mẹ của người khác (`PersonInUseException`).
5. **BR5 — Parent Birth Year**: Cảnh báo nếu năm sinh cha/mẹ lớn hơn hoặc bằng năm sinh con (Warning only).
6. **BR6 — Gender Role**: Cha bắt buộc là `Gender.MALE`, Mẹ bắt buộc là `Gender.FEMALE` (`InvalidParentGenderException`).

**Thứ tự ưu tiên kiểm tra** trong `assignFather` / `assignMother`:
`BR1 (SelfParent)` → `BR2 (SameParent)` → `BR6 (InvalidParentGender)` → `BR3 (Cycle)`.

**Ràng buộc năm sinh**: `1000 <= birthYear <= Year.now().getValue()`.
Hằng số `FamilyTreeService.MIN_BIRTH_YEAR = 1000` là nguồn sự thật duy nhất ở tầng Service;
ràng buộc tương ứng ở tầng CSDL là `CHECK (birth_year >= 1000 AND birth_year <= 2100)`.
Việc nới lỏng này nhằm phục vụ các bộ dữ liệu gia phả lịch sử nhiều thế hệ (xem `test-data/`).

---

## 7. Cài Đặt Cơ Sở Dữ Liệu (Database Setup)
Khởi động MySQL Server trên cổng mặc định `3306` và thực thi script khởi tạo:
```bash
mysql -u root --default-character-set=utf8mb4 -e "source database/schema.sql"
```
Mặc định kết nối sử dụng `root` không mật khẩu trên `localhost:3306/gia_pha`. Có thể cấu hình lại qua biến môi trường hoặc system properties:
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`

---

## 8. Hướng Dẫn Biên Dịch & Chạy (Build & Run)

### Biên dịch dự án:
```bash
mvn clean compile
```

### Chạy ứng dụng giao diện JavaFX:
```bash
mvn javafx:run
```

### Sơ đồ cây gia phả 2D

Màn hình **Cây gia phả** sử dụng `TabPane` với hai tab:

| Tab | Nội dung |
|-----|----------|
| **Sơ đồ 2D** | Sơ đồ trực quan vẽ bằng `Pane` + `Line` (class `com.giapha.view.FamilyTreeDiagram`) |
| **Danh sách phả hệ** | `TreeView` dạng danh sách phân cấp (bản cũ, giữ lại để đối chiếu) |

**Cách bố trí sơ đồ 2D:**
- Mỗi thế hệ nằm trên một hàng; tổ tiên ở **trên**, hậu duệ ở **dưới** người trung tâm.
- Mỗi node hiển thị **avatar + họ tên + năm sinh + giới tính**.
- Cha và mẹ sinh chung một con được gom thành một *khối gia đình* (unit) để hiển thị cạnh nhau;
  con được canh chính giữa giữa hai vị trí cha/mẹ.
- Đường kẻ nối **từng cha/mẹ xuống đúng con của họ** (vẽ ở mức cá nhân, không nối nhầm vợ/chồng).
- Node của người trung tâm được tô viền đậm màu xanh.

**Tương tác:**

| Thao tác | Kết quả |
|----------|---------|
| Kéo chuột trái | Di chuyển sơ đồ (pan) |
| Lăn chuột dọc | Cuộn dọc |
| Shift + lăn chuột | Cuộn ngang |
| Ctrl + lăn chuột | Zoom (0.25× – 2.5×) |
| Nút `−` / `+` / `Vừa khung` | Zoom out / zoom in / tự động vừa khung nhìn |
| Bấm vào node | Mở hồ sơ cá nhân của thành viên đó |

Combo box **"Người trung tâm"** thay đổi → sơ đồ 2D được vẽ lại tức thì.

### Xuất PNG & In ấn

| Nút | Cơ chế | Ghi chú |
|-----|--------|---------|
| **🖼 Xuất PNG** | `Node.snapshot()` với `SnapshotParameters.setViewport(...)` đúng bằng kích thước nội dung sơ đồ, sau đó ghi file PNG bằng `com.giapha.util.PngWriter` | Ảnh chụp **toàn bộ** sơ đồ ở mức zoom 1.0, không bị cắt theo khung nhìn hiện tại. Hộp thoại chọn file do `FileChooser` cung cấp |
| **🖨 In** | `javafx.print.PrinterJob` + `PrinterJob.printPage(node)` | Dùng `JobSettings.getPageLayout()` để lấy khổ giấy; ảnh được bọc trong `StackPane` kích thước trang và canh giữa |

> **Không thêm dependency mới.** `PngWriter` hiện thực thuần bằng `java.util.zip` (`Deflater` + `CRC32`) nên module `com.giapha` không cần khai báo thêm `java.desktop` hay `javafx.swing`.

Kết quả thực hiện (thành công / thất bại / không tìm thấy máy in) đều được báo bằng `Alert`.

---

## 9. Hướng Dẫn Kiểm Thử (Testing)

Toàn bộ kiểm thử tự động dùng **JUnit 5** và nằm trong `src/test/java`.
Chạy bằng `maven-surefire-plugin` — lệnh chuẩn là `mvn test`.

> ⚠️ **Điều kiện tiên quyết cho các test phụ thuộc CSDL**: MySQL phải đang chạy. Các test dùng
> database **riêng** (tên kết thúc bằng `_test`) được **tự động tạo ở `@BeforeAll`** và
> **tự động xóa ở `@AfterAll`** — database chính `gia_pha` **không bao giờ** bị test ghi vào.
> Nếu CSDL không sẵn sàng, các test đó được đánh dấu `SKIPPED` kèm thông báo rõ ràng
> (không tính là `FAILED`).

### 1. Chạy toàn bộ kiểm thử (121 test cases):
```bash
mvn clean test
```
| Lớp kiểm thử | Số test | Phạm vi |
|---------------|---------|---------|
| `FamilyTreeServiceTest` | 44 | BR1–BR6, TC01–TC14, TC-CLEAR-01…05, cô lập dữ liệu |
| `PersonControllerTest` | 25 | Điều phối View ↔ Service, xử lý exception, callback điều hướng |
| `LargeSampleDataTest` | 22 | Bộ dữ liệu mẫu 30 người / 5 thế hệ (BR1–BR6, validation, ràng buộc CSDL, an toàn DROP DB) |
| `DatabaseConnectionTest` | 7 | Kết nối MySQL, metadata, bảng `PERSON`, kiểm chứng DB chính không bị đổi |
| `ImageManagerTest` | 12 | Định dạng ảnh, copy vào `./images/`, dọn file an toàn |
| `PngWriterTest` | 11 | Chữ ký PNG, khối IHDR/IDAT/IEND, CRC32, nén zlib |

> **Giải thích lệch số liệu "142" vs "121":** nếu bạn cộng tay từng dòng log Surefire
> (25 + 7 + 44 + 0 + 22 + 12 + 11) thì ra **121**, không phải 142. Con số 142 xuất hiện
> khi **báo cáo cũ** tự cộng thêm một dòng "22 test — FamilyTreeServiceTest (phần lồng)"
> *cùng* với dòng `FamilyTreeServiceTest$DataIsolation` đã chứa 44 test — tức là cộng
> trùng các test của lớp lồng `@Nested`.
>
> Cơ chế thật (đã kiểm chứng bằng file `target/surefire-reports/TEST-*.xml`):
> - `FamilyTreeServiceTest` khai báo **43** `@Test` ở thân lớp + **1** `@Test` trong
>   lớp lồng `@Nested DataIsolation` → **44** test thực thi.
> - Surefire ghi **hai** dòng log cho cùng một lớp:
>   `... -- in com.giapha.service.FamilyTreeServiceTest$DataIsolation` (44 test, chứa
>   cả 43 test thân lớp lẫn 1 test lồng — kiểm chứng qua `classname` trong XML),
>   và `... -- in com.giapha.service.FamilyTreeServiceTest` (0 test, vì mọi test đã
>   được gán cho container lồng).
> - Dòng `Tests run: 0 ... FamilyTreeServiceTest` là **vỏ rỗng**, không phải test bị mất.
>
> Vì vậy **chỉ dòng tổng cuối cùng** mới là con số chuẩn xác, và đó là những gì
> Surefire thực sự thực thi (không đếm trùng):
>
> ```
> [INFO] Results:
> [INFO] Tests run: 121, Failures: 0, Errors: 0, Skipped: 0
> [INFO] BUILD SUCCESS
> ```
>
> Dòng tổng này **không** bao gồm 26 test tag `javafx` (chạy riêng qua `mvn test -Pjavafx-tests`).

### 2. Chạy riêng các test cần JavaFX Toolkit (tag `javafx` — 26 test cases):
```bash
mvn test -Pjavafx-tests
```
Chạy riêng một lớp:
```bash
mvn test -Pjavafx-tests -Dtest=JavaFxViewTest
mvn test -Pjavafx-tests -Dtest=AvatarFallbackTest
```

> ⚠️ **Điều kiện tiên quyết**: MySQL phải đang chạy. Database test riêng `gia_pha_javafx_test`
> được tự động tạo/xóa. Nếu không đủ điều kiện, toàn bộ lớp test được `SKIPPED`.

**`AvatarFallbackTest` (14 test)** — kiểm tra component `PersonAvatar` (ảnh đại diện dùng chung cho
danh sách / chi tiết / cây gia phả / form):
- TC-AV-01: ảnh hợp lệ → hiển thị ảnh thật.
- TC-AV-02/03/07: file ảnh **không tồn tại**, file ảnh **hỏng**, file **không phải ảnh** → fallback về placeholder (chữ cái đầu), **không ném exception**.
- TC-AV-04/05/06: không có đường dẫn ảnh / đường dẫn rỗng / `person = null` → fallback placeholder.
- TC-AV-08/09: constructor với file cụ thể (xem trước ảnh trong form) → hiển thị ảnh thật / fallback.
- TC-AV-10/11: kích thước avatar và `resolveImageFile()` xử lý đường dẫn sai / null / rỗng.
- TC-AV-12/13/14: `ImageView` khi hiển thị ảnh thật, `Label` khi fallback, clip hình tròn.

**`JavaFxViewTest` (12 test)** — kiểm thử tích hợp in-process (không dùng TestFX):
- TEST 1: `PeopleListView` nạp danh sách thành viên từ MySQL vào `TableView`.
- TEST 2: Tìm kiếm client-side (`searchField`) và phục hồi danh sách đầy đủ.
- TEST 3: `PersonFormView` thêm thành viên mới hợp lệ (với cha/mẹ) vào MySQL.
- TEST 4: Validation từ chối dữ liệu không hợp lệ (tên rỗng, năm sinh sai số).
- TEST 5: `PersonFormView` sửa tên và gỡ quan hệ cha/mẹ (set NULL) qua form.
- TEST 6a/6b: Chặn xóa người đang làm cha/mẹ theo BR4; xóa thành viên thử nghiệm đã gỡ quan hệ.
- TEST 7a/7b: `PersonDetailView` hiển thị đúng cha/mẹ; `FamilyTreeView` dựng cây đa thế hệ.
- TEST 8: `FamilyTreeView` có sơ đồ 2D (`FamilyTreeDiagram`) đi kèm.
- TEST 9/10: CSDL giữ đúng 7 bản ghi mẫu sau toàn bộ test; JavaFX Toolkit sẵn sàng.

> **Lưu ý kỹ thuật**: ảnh được nạp ở chế độ background nên test **không** sleep trên JavaFX
> Application Thread (sẽ chặn nạp ảnh), và **không** gọi `Platform.exit()` trước khi ảnh nạp xong.
> JavaFX Toolkit không thể khởi tạo lại sau `Platform.exit()` — `FxTestSupport` chỉ đóng Stage giả
> và dùng shutdown hook.

> **Lưu ý**: Bộ test xác nhận luồng Controller–Service–Repository–MySQL đúng đắn, nhưng **không
> thay thế kiểm thử thủ công trên desktop** để xác nhận trải nghiệm người dùng cuối, hiển thị
> ảnh đại diện và tương tác chuột/bàn phím thực tế.

### 3. Chạy một lớp / một phương thức test cụ thể:
```bash
mvn test -Dtest=FamilyTreeServiceTest
mvn test -Dtest=PersonControllerTest#handleAddCreatesPerson
mvn test -Dtest=LargeSampleDataTest
```

### 4. Bộ dữ liệu mẫu mở rộng (`test-data/`)

Thư mục `test-data/` chứa **bộ dữ liệu kiểm thử thực tế**: 30 người / 5 thế hệ / 4 nhánh dòng họ.

| File | Nội dung |
|------|----------|
| `test-data/sample-data.sql` | Script SQL tạo 30 bản ghi `PERSON` (thế hệ 1 năm 1850 → thế hệ 5 năm 1970) |
| `test-data/test-cases.md` | Bảng test case đầy đủ: **mã, mô tả, dữ liệu vào, kết quả mong đợi, kết quả thực tế** + ma trận phủ BR1–BR6 |

Bộ test tương ứng là `src/test/java/com/giapha/testdata/LargeSampleDataTest.java` (22 test case),
tự tạo database riêng `gia_pha_test` (schema giống hệt `database/schema.sql`) ở đầu mỗi lần
chạy, nạp `sample-data.sql` vào đó, rồi **tự động DROP** database test ở cuối.
Trước khi DROP có kiểm tra an toàn: tên database bắt buộc phải **kết thúc bằng `_test`**,
nếu không sẽ ném lỗi và **không** DROP — không bao giờ xóa nhầm database chính `gia_pha`.

```bash
# Chạy riêng bộ test dữ liệu mẫu mở rộng
mvn test -Dtest=LargeSampleDataTest

# Giữ lại database test (gia_pha_test) sau khi chạy để debug trực tiếp
mvn test -Dtest=LargeSampleDataTest -Dkeep.test.db=true

# Nạp dữ liệu mẫu vào database chính để kiểm thử thủ công trên giao diện
mysql -u root -p < test-data/sample-data.sql
```
> ⚠️ `sample-data.sql` **xoá toàn bộ** dữ liệu đang có trong bảng `PERSON`.
> Khôi phục lại dữ liệu gốc: `mysql -u root -p < database/schema.sql`.

### 5. Ghi chú về xung đột JPMS ↔ JUnit 5

`src/main/java` có `module-info.java` (module `com.giapha`), còn `src/test/java` là test thuần
JUnit 5 nằm ở "unnamed module". Để tránh lỗi JPMS chặn reflection của JUnit 5, `pom.xml` cấu hình:
- `maven-compiler-plugin` → execution `default-testCompile` với `<useModulePath>false</useModulePath>`.
- `maven-surefire-plugin` → `<useModulePath>false</useModulePath>`.

> ⚠️ **Không được bỏ cấu hình này** khi thêm test mới — nếu không, `mvn test` sẽ báo lỗi compile
> hoặc lỗi `IllegalAccessError` khi JUnit 5 dùng reflection gọi các `@Test` method.

---

## 10. Tài Liệu UML (UML Documentation)
Tất cả sơ đồ nằm tại thư mục `docs/uml/`:
- Sơ đồ lớp: `class-diagram.puml` / `class-diagram.png` (cập nhật đầy đủ các tầng View, Controller, Service, Repository, Database)
- Sơ đồ thành phần: `component-diagram.puml` / `component-diagram.png`
- Sơ đồ triển khai: `deployment-diagram.puml` / `deployment-diagram.png`
- Sơ đồ ca sử dụng: `use-case.puml` / `use-case.png`
- Sơ đồ tuần tự & hoạt động cho các ca sử dụng: Thêm người, Gán cha mẹ, Xem cây gia phả.

---

## 11. Trạng Thái Phát Triển (Development Status)
- [x] **Cụm 1–4**: Thiết kế UML, kiến trúc, UI Mockup & Design System JavaFX.
- [x] **Cụm 5.1–5.2**: Model Layer, Custom Exceptions & Repository Layer (Interface & JDBC Implementation).
- [x] **Cụm 5.3**: Database Schema, MySQL setup, DatabaseConnection.
- [x] **Cụm 5.4**: FamilyTreeService Layer & Bộ test nghiệp vụ (22/22 test cases PASS).
- [x] **Cụm 5.5**: PersonController & Bộ test Controller (8/8 test cases PASS).
   - `PersonController` là cầu nối duy nhất giữa View JavaFX và `FamilyTreeService` — View **không** gọi trực tiếp Service hay Repository.
   - Bắt toàn bộ ngoại lệ nghiệp vụ (`SelfParentException`, `SameParentException`, `InvalidParentGenderException`, `CycleDetectedException`, `PersonInUseException`, `InvalidBirthYearException`, `PersonNotFoundException`, `IllegalArgumentException`, `SQLException`…) và hiển thị bằng `Alert` tiếng Việt — không văng stack trace ra giao diện.
   - **BR5 (cảnh báo năm sinh)** được kiểm tra ở tầng Service qua `FamilyTreeService.checkBirthYearWarning(child, father, mother)`; Controller hiển thị `Alert` cảnh báo với hai lựa chọn **Tiếp tục** / **Hủy**.
   - **Ảnh đại diện**: nút *Chọn ảnh...* dùng `FileChooser` (chỉ nhận `png` / `jpg` / `jpeg`); ảnh được copy vào thư mục `images/` cạnh ứng dụng và lưu **đường dẫn tương đối** (`images/person_<id>.<đuôi>`) vào DB. Tiện ích nằm ở `com.giapha.util.ImageManager`.
- [x] **Cụm 5.6**: Tích hợp toàn diện JavaFX UI với Backend MySQL (kết nối trực tiếp, loại bỏ hoàn toàn SampleData khỏi luồng chạy chính).
- [x] **Cụm 5.7**: End-to-End Verification, QA tích hợp và kiểm chứng toàn vẹn dữ liệu MySQL.
- [x] **Cụm 5.8**: Đồng bộ hóa tài liệu (README) và sơ đồ lớp (Class Diagram).
- [x] **Cụm 5.9**: Native In-Process JavaFX Test Harness (`JavaFxViewTest`) — 9/9 test cases PASS, bảo toàn 7 bản ghi mẫu.
- [x] **Cụm 5.10**: Hiển thị ảnh đại diện thật trên toàn bộ giao diện — component dùng chung `com.giapha.view.PersonAvatar` (ảnh bo tròn, tự fallback về chữ cái đầu khi thiếu ảnh / file ảnh hỏng / sai định dạng, **không ném exception ra UI**), tích hợp vào `PeopleListView` (cột Ảnh), `PersonDetailView` (avatar + thumbnail), `FamilyTreeView` (avatar trên mỗi node) và `PersonFormView` (xem trước). Bộ test `AvatarFallbackTest` — 14/14 PASS.
- [x] **Cụm 6.1**: Sơ đồ cây gia phả 2D (`FamilyTreeDiagram`) — vẽ bằng `Pane` + `Line`, mỗi thế hệ một hàng, node gồm avatar + họ tên + năm sinh + giới tính, đường nối cha/mẹ xuống con, hỗ trợ zoom & pan. Giữ `TreeView` cũ làm tab thứ hai.
- [x] **Cụm 6.2**: Xuất PNG (`Node.snapshot()` + `PngWriter` thuần `java.util.zip`) và In ấn (`javafx.print.PrinterJob`) — không thêm dependency, kết quả báo bằng `Alert`.
- [x] **Cụm 6.3**: Nới validate năm sinh xuống `1000 <= birthYear <= Year.now()` (hằng số `FamilyTreeService.MIN_BIRTH_YEAR`).
- [x] **Cụm 6.4**: Custom exception riêng cho BR2 (`SameParentException`) và BR6 (`InvalidParentGenderException`), thay `IllegalArgumentException`; thứ tự kiểm tra `BR1 → BR2 → BR6 → BR3`.
- [x] **Cụm 6.5**: Chuyển toàn bộ kiểm thử sang **JUnit 5** + `maven-surefire-plugin` (120 test mặc định + 26 test tag `javafx`), tạo `src/test/java`, xử lý xung đột `module-info.java` bằng `useModulePath=false`.
- [x] **Cụm 6.6**: Bộ dữ liệu mẹ & bảng test case trong `test-data/` — `sample-data.sql` (30 người / 5 thế hệ) + `test-cases.md` (22 test case có kết quả thực tế) + `LargeSampleDataTest` (22 test PASS).
