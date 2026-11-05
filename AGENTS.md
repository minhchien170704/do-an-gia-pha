# AGENTS.md — HƯỚNG DẪN DÀNH CHO AI AGENTS

Tài liệu này là **Source of Truth** về kiến trúc, quy tắc phát triển, và ràng buộc kỹ thuật của dự án **Quản lý Sơ đồ Gia phả** (`do-an-gia-pha`). Mọi AI Agent khi tiếp cận, phân tích, review hoặc đóng góp code cho repository này **BẮT BUỘC** phải đọc và tuân thủ tuyệt đối các nguyên tắc dưới đây.

---

## 1. MỤC TIÊU ĐỒ ÁN
Xây dựng ứng dụng desktop quản lý cây gia phả (Family Tree Management) phục vụ môn học **Lập trình hướng đối tượng (OOP)**, bảo đảm tính mẫu mực về:
- Các nguyên lý thiết kế hướng đối tượng (Encapsulation, Inheritance, Polymorphism, Abstraction, SOLID).
- Kiến trúc phân lớp tách bạch (Separation of Concerns).
- Tính toàn vẹn dữ liệu phả hệ (không chu trình, không trùng lặp, ràng buộc quan hệ).
- Giao diện người dùng hiện đại, trực quan với JavaFX theo phong cách tối giản, nhất quán.

---

## 2. CÔNG NGHỆ & PHIÊN BẢN (TECH STACK)
- **Ngôn ngữ**: Java 25 (OpenJDK 25.0.2)
- **Hệ thống module**: Java Platform Module System (`module-info.java`, module `com.giapha`)
- **Giao diện (UI)**: JavaFX 23.0.2 (`javafx-controls`, `javafx-fxml`)
- **Quản lý dự án / Build Tool**: Apache Maven 3.9.16
- **Cơ sở dữ liệu**: MySQL / MariaDB (mặc định port `3306`, database `gia_pha`, charset `utf8mb4`)
- **Kết nối CSDL**: JDBC thuần với `mysql-connector-j` 9.2.0 (KHÔNG dùng ORM/Hibernate/JPA/Spring)
- **Kiểm thử tự động**: JUnit 5 (`junit-jupiter` 5.10.2) + `maven-surefire-plugin` 3.2.5
- **Thiết kế & Mô hình hóa**: PlantUML (các file `.puml` và `.png` trong `docs/uml/`)

---

## 3. KIẾN TRÚC PHÂN LỚP (LAYERED ARCHITECTURE)

Luồng dữ liệu và phụ thuộc bắt buộc đi theo một chiều:
```
JavaFX UI (View)
    ↓
PersonController (Controller)
    ↓
FamilyTreeService (Business Layer)
    ↓
PersonRepository (Data Access Interface)
    ↓
JdbcPersonRepository (Data Access Implementation)
    ↓
DatabaseConnection (JDBC Factory)
    ↓
MySQL / MariaDB
```

### Ràng buộc kiến trúc:
1. **View (`com.giapha.view`)**:
   - Chỉ đảm nhận hiển thị và bắt sự kiện người dùng (UI thuần).
   - Tương tác với logic hệ thống thông qua `Controller`.
   - Sử dụng Java code kết hợp CSS (`resources/css/style.css`), không can thiệp logic dữ liệu.
   - `FamilyTreeDiagram` (sơ đồ 2D) là component JavaFX thuần: nhận `List<Person>` + người trung tâm,
     tự dựng bố cục (hàng thế hệ + cột) và vẽ bằng `Pane` + `Line`; **không** gọi xuống Service/Repository.
   - Module JPMS phải khai báo `requires javafx.graphics` để dùng `javafx.scene.input.ScrollEvent`.
   - Xuất ảnh & in ấn:
     - **Xuất PNG**: `FamilyTreeDiagram.renderFullImage()` chụp toàn bộ sơ đồ (viewport = kích thước nội dung, zoom 1.0), `PngWriter` ghi file, `FileChooser` để chọn nơi lưu. Kết quả báo bằng `Alert`.
     - **In**: `javafx.print.PrinterJob` + `PrinterJob.printPage(node)` — **không** thêm dependency. Dùng `JobSettings.getPageLayout()` để lấy khổ giấy.
     - Nút bấm đặt trong `FamilyTreeView`; mọi lỗi (không tìm thấy máy in, ghi file lỗi, sơ đồ rỗng) đều phải báo bằng `Alert` thay vì ném exception ra UI.
2. **Controller (`com.giapha.controller`)**:
   - Điều phối giữa View và Service.
   - Không chứa câu lệnh SQL, không gọi trực tiếp Repository hay JDBC.
3. **Service (`com.giapha.service`)**:
   - Đại diện bởi `FamilyTreeService`.
   - **CHỈ** phụ thuộc vào abstraction interface `PersonRepository`.
   - **TUYỆT ĐỐI KHÔNG** import hoặc sử dụng `JdbcPersonRepository`, `DatabaseConnection`, `Connection`, `PreparedStatement`, `ResultSet`, hoặc bất kỳ JDBC API nào.
   - Chịu trách nhiệm thực thi các quy tắc nghiệp vụ (BR1 – BR6), validation, và phát hiện chu trình.
4. **Repository (`com.giapha.repository`)**:
   - `PersonRepository`: Interface định nghĩa các hợp đồng truy xuất dữ liệu (`findById`, `findAll`, `save`, `update`, `delete`).
   - `JdbcPersonRepository`: Lớp cài đặt interface sử dụng JDBC thuần, thao tác với bảng `PERSON`.
   - **Quyền sở hữu `Connection` (bắt buộc):**
     - Repository **tự mở** Connection (qua `DatabaseConnection.getConnection()`) → repository **chịu trách nhiệm đóng** nó (qua `closeIfOwned(conn)`).
     - Repository **nhận** Connection từ bên ngoài (constructor có tham số) → repository chỉ **mượn**, **tuyệt đối không đóng**.
     - Nếu Connection bên ngoài đã đóng → ném `SQLException` rõ ràng, **không** âm thầm chuyển sang database chính `gia_pha`.
5. **Database Connection (`com.giapha.database`)**:
   - `DatabaseConnection`: Utility class cung cấp Connection mới mỗi khi gọi `getConnection()`. Không dùng static long-lived connection.
   - `Main.initBackend()` **phải kiểm tra kết nối thật** (mở 1 Connection thật) **trước khi** hiển thị giao diện; thất bại → thông báo rõ cách cấu hình và dừng khởi động có kiểm soát.
6. **Utility (`com.giapha.util`)**:
   - `ImageManager`: Tiện ích thuần (không phụ thuộc JavaFX/DB) xử lý ảnh đại diện — kiểm tra định dạng `png`/`jpg`/`jpeg`, copy vào thư mục `images/` cạnh ứng dụng, và dọn file an toàn.
   - `PngWriter`: Tiện ích ghi `WritableImage` ra file PNG — hiện thực **thuần** bằng `java.util.zip` (`Deflater` + `CRC32`), **không** dùng `javax.imageio` hay `javafx.swing` để tránh phải khai báo thêm module `java.desktop` / `javafx.swing`.
   - Không được đặt logic nghiệp vụ gia phả vào package này.

### Quy ước ảnh đại diện (photoPath):
- Người dùng chọn ảnh qua `FileChooser` trong `PersonFormView`; View chỉ giữ đường dẫn nguồn, **không** tự copy file.
- `PersonController` gọi `ImageManager.copyImageToAppDirectory(...)` **sau khi** thành viên đã có `id`, rồi lưu **đường dẫn tương đối** (`images/person_<id>.<đuôi>`) vào cột `photo_path`.
- Thư mục `images/` được sinh tự động cạnh ứng dụng và đã được thêm vào `.gitignore` (ảnh người dùng không được commit).
- Vì đường dẫn là tương đối, ứng dụng phải chạy từ thư mục gốc dự án (hoặc ảnh sẽ không tìm thấy khi mở lại).

---

## 4. CẤU TRÚC GÓI (PACKAGES)

```
com.giapha
├── Main.java                          # Điểm khởi chạy ứng dụng JavaFX
├── model/
│   ├── Gender.java                    # Enum: MALE, FEMALE
│   └── Person.java                    # Entity chứa thông tin cá nhân & tham chiếu cha/mẹ
├── exception/
│   ├── CycleDetectedException.java    # Ném khi phát hiện chu trình gia phả (BR3)
│   ├── InvalidBirthYearException.java # Ném khi năm sinh không hợp lệ
│   ├── InvalidParentGenderException.java # Ném khi giới tính cha/mẹ sai vai trò (BR6)
│   ├── PersonInUseException.java      # Ném khi xóa person đang là cha/mẹ người khác (BR4)
│   ├── PersonNotFoundException.java   # Ném khi không tìm thấy Person theo ID
│   ├── SameParentException.java       # Ném khi cha và mẹ là cùng một người (BR2)
│   └── SelfParentException.java       # Ném khi tự gán làm cha/mẹ chính mình (BR1)
├── repository/
│   ├── PersonRepository.java          # Interface hợp đồng truy xuất dữ liệu
│   └── JdbcPersonRepository.java      # Cài đặt JDBC CRUD
├── database/
│   └── DatabaseConnection.java        # Factory lấy JDBC Connection
├── service/
│   └── FamilyTreeService.java         # Business Service Layer
├── util/
│   ├── ImageManager.java              # Tiện ích ảnh đại diện: kiểm tra định dạng, copy vào ./images/
│   └── PngWriter.java                 # Ghi WritableImage ra file PNG (thuần java.util.zip, không thêm dependency)
└── view/
    ├── FamilyTreeView.java            # Màn hình cây phả hệ (Tab: Sơ đồ 2D + Danh sách phả hệ)
    ├── FamilyTreeDiagram.java         # Sơ đồ cây gia phả 2D (Pane + Line, zoom & pan)
    ├── PeopleListView.java            # Màn hình danh sách thành viên
    ├── PersonAvatar.java              # Component ảnh đại diện dùng chung (bo tròn + fallback)
    ├── PersonDetailView.java          # Màn hình chi tiết hồ sơ cá nhân
    ├── PersonFormView.java            # Form thêm / sửa thành viên
```

### Cấu trúc kiểm thử (`src/test/java`)

Toàn bộ test nằm trong `src/test/java` và dùng **JUnit 5**. Không còn file test nào trong
`src/main/java` (các test harness dạng `main()` đã được chuyển đổi sang JUnit 5).

```
src/test/java/com/giapha
├── support/
│   ├── TestDatabaseManager.java        # QUẢN LÝ VÒNG ĐỜI CSDL TEST: createFreshDatabase / dropDatabase / assertSafeToDrop
│   ├── TestPersonRepository.java        # Repository ghi đè getConnection() để trỏ database test
│   ├── TestDatabase.java                # Tiện ích truy vấn: đếm bản ghi, reset dữ liệu mẫu, dọn bản ghi test
│   └── FxTestSupport.java             # Tiện ích khởi tạo/quản lý JavaFX Toolkit cho test tag "javafx"
├── database/
│   ├── DatabaseConnectionTest.java    # 7 test — kết nối MySQL, metadata, bảng PERSON, DB chính không bị đổi
│   └── BackendInitializationTest.java # 3 test — kiểm tra kết nối MySQL thật trước khi hiển thị UI
├── service/
│   └── FamilyTreeServiceTest.java     # 44 test — BR1–BR6, TC01–TC14, TC-CLEAR-01…05, cô lập dữ liệu
├── repository/
│   └── JdbcPersonRepositoryConnectionTest.java # 5 test — quyền sở hữu Connection
├── controller/
│   ├── PersonControllerTest.java      # 25 test — điều phối View ↔ Service, xử lý exception
│   └── AtomicUpdateAndBr5Test.java    # 9 test — cập nhật nguyên tử + BR5 trong luồng lưu của form
├── util/
│   ├── ImageManagerTest.java          # 12 test — định dạng ảnh, copy, dọn file
│   └── PngWriterTest.java             # 11 test — chữ ký PNG, khối IHDR/IDAT/IEND, CRC32, nén zlib
├── testdata/
│   └── LargeSampleDataTest.java       # 22 test — bộ dữ liệu mẫu 30 người / 5 thế hệ (DB riêng gia_pha_test)
└── view/
    ├── AvatarFallbackTest.java        # 14 test (tag "javafx") — cơ chế fallback ảnh đại diện
    └── JavaFxViewTest.java            # 12 test (tag "javafx") — tích hợp UI → MySQL
```

**Quy tắc bắt buộc cho test:**
- Test **không** được nằm trong `src/main/java`.
- Mọi test có thay đổi dữ liệu CSDL **phải** có cleanup để trả bảng `PERSON` về đúng 7 bản ghi mẫu.
- **CÁCH LY DỮ LIỆU (bắt buộc):** **mọi** lớp test đụng tới CSDL **phải** dùng database RIÊNG có tên kết thúc bằng `_test`
  (`gia_pha_service_test`, `gia_pha_controller_test`, `gia_pha_conn_test`, `gia_pha_javafx_test`, `gia_pha_test`).
  Tuyệt đối không ghi vào database chính `gia_pha`.
- Test phụ thuộc MySQL dùng `Assumptions.assumeTrue(...)` → nếu CSDL không sẵn sàng thì test được
  `SKIPPED` kèm thông báo (không tính là `FAILED`).
- Test cần JavaFX Toolkit **phải** được gắn `@Tag("javafx")` để không chạy trong `mvn test` mặc định.
- Bộ test dữ liệu mẫu mở rộng **phải** dùng database riêng `gia_pha_test` (tự tạo/xóa mỗi lần chạy),
  không bao giờ ghi vào database chính `gia_pha`.
- **Ràng buộc an toàn khi DROP database test**: tên database bắt buộc phải **kết thúc bằng `_test`**.
  Hàm `TestDatabaseManager.assertSafeToDrop()` kiểm tra điều này và **ném `IllegalStateException`
  + không DROP** nếu tên không khớp. Được phép giữ lại DB test bằng cờ `-Dkeep.test.db=true`.
- Vòng đời chuẩn: `@BeforeAll` → `TestDatabaseManager.createFreshDatabase(<db>_test)`
  (DROP + CREATE + schema + 7 bản ghi mẫu); `@AfterAll` → `TestDatabaseManager.dropDatabase(<db>_test)`.
  Repository dùng trong test **phải** là `TestDatabaseManager.repositoryFor(<db>_test)`.

### Bộ dữ liệu kiểm thử (`test-data/`)

| File | Nội dung |
|------|----------|
| `test-data/sample-data.sql` | Script SQL tạo **30 bản ghi** `PERSON` — 5 thế hệ (1850 → 1970), 4 nhánh dòng họ |
| `test-data/test-cases.md` | Bảng test case: **mã, mô tả, dữ liệu vào, kết quả mong đợi, kết quả thực tế** + ma trận phủ BR1–BR6 |

Bộ test tương ứng: `src/test/java/com/giapha/testdata/LargeSampleDataTest.java` — nạp trực tiếp
`test-data/sample-data.sql` vào database `gia_pha_test` nên dữ liệu mẫu và bảng test case
luôn đồng bộ.

---

## 5. QUY TẮC NGHIỆP VỤ (BUSINESS RULES BR1–BR6)

- **BR1 — SELF PARENT**: Một người không thể là cha hoặc mẹ của chính mình (`childId == parentId`).
  - *Xử lý*: Ném `SelfParentException`.
- **BR2 — FATHER != MOTHER**: Cha và mẹ của cùng một người không được là cùng một Person (`father.id == mother.id`).
  - *Xử lý*: Ném `SameParentException` (custom exception riêng, thay cho `IllegalArgumentException`).
- **BR3 — NO CYCLE**: Không được tạo chu trình vòng lặp trong gia phả (ví dụ: A là cha B, B là cha C thì C không thể là cha/mẹ của A).
  - *Xử lý*: Kiểm tra bằng thuật toán duyệt đồ thị (DFS traversal) từ `candidateParentId` ngược lên các thế hệ tổ tiên với `Set<Integer> visited` chống lặp. Nếu gặp `childId`, ném `CycleDetectedException`.
- **BR4 — PERSON IN USE**: Không được xóa một `Person` nếu người đó đang được tham chiếu làm `father` hoặc `mother` của bất kỳ ai trong hệ thống.
  - *Xử lý*: Ném `PersonInUseException`. Không cascade delete, không tự ý set NULL quan hệ con cái.
- **BR5 — PARENT BIRTH YEAR**: Năm sinh của cha/mẹ nên nhỏ hơn năm sinh của con.
  - *Xử lý*: Đây là quy tắc cảnh báo (WARNING), không phải hard validation, không ném exception làm gián đoạn lưu trữ.
  - *Cơ chế*: Tầng Service cung cấp `checkBirthYearWarning(child, father, mother)` trả về danh sách thông điệp cảnh báo (rỗng = không có vấn đề). Tầng Controller gọi hàm này và hiển thị `Alert` cảnh báo với hai lựa chọn **Tiếp tục** / **Hủy** — chỉ tiếp tục lưu khi người dùng chọn "Tiếp tục".
- **BR6 — GENDER ROLE**:
  - `father` bắt buộc phải có giới tính `Gender.MALE`.
  - `mother` bắt buộc phải có giới tính `Gender.FEMALE`.
  - *Xử lý*: Sai giới tính ném `InvalidParentGenderException` (custom exception riêng, thay cho `IllegalArgumentException`).

**Thứ tự ưu tiên kiểm tra** trong `assignFather` / `assignMother` (quan trọng để thông báo đúng nguyên nhân):
`BR1 (SelfParent)` → `BR2 (SameParent)` → `BR6 (InvalidParentGender)` → `BR3 (Cycle)`.

### 5.1. Cập nhật thành viên phải **NGUYÊN TỬ** (bắt buộc)

- Luồng lưu từ form **KHÔNG** được gọi `updatePerson()` rồi `assignFather()` rồi `assignMother()` — cách đó tạo ra trạng thái "cập nhật một nửa" (cha đã đổi nhưng mẹ thất bại).
- Thay vào đó dùng `FamilyTreeService.updatePersonWithParents(person, newFather, newMother)`:
  1. Kiểm tra **toàn bộ** trước: validate thông tin cá nhân + BR1/BR2/BR6/BR3 cho **cả cha lẫn mẹ**.
  2. Chỉ khi **mọi** kiểm tra đạt mới ghi bằng **một** câu `UPDATE` duy nhất.
  3. Nếu bất kỳ kiểm tra nào fail → **không ghi gì cả**.
- BR5 **phải** chạy trong luồng lưu thật của form: `PersonFormView` đăng ký itself qua `controller.setPersonFormView(this)` rồi gọi `controller.handleAdd()` / `controller.handleEdit()` (overload **không tham số**) — không gọi overload có tham số để tránh bỏ qua kiểm tra BR5.

---

## 6. QUY TẮC VALIDATION DỮ LIỆU (`validatePerson`)

Mỗi khi tạo mới (`addPerson`) hoặc cập nhật (`updatePerson`), đối tượng `Person` phải vượt qua các điều kiện:
1. `person != null`
2. `fullName != null`, `fullName.trim()` không rỗng, độ dài từ 1 đến 100 ký tự.
3. `birthYear >= 1000` và `birthYear <= Year.now().getValue()` (nếu vi phạm ném `InvalidBirthYearException`). Hằng số `FamilyTreeService.MIN_BIRTH_YEAR = 1000` là nguồn sự thật duy nhất — không hard-code `1900`/`1000` ở nơi khác.
4. `gender != null`.
5. `photoPath`: tùy chọn, cho phép `null` hoặc chuỗi rỗng.

---

## 7. CƠ SỞ DỮ LIỆU & SCHEMA (`database/schema.sql`)

- **Bảng `PERSON`**:
  - `id INT AUTO_INCREMENT PRIMARY KEY`
  - `full_name VARCHAR(100) NOT NULL`
  - `birth_year INT NOT NULL`
  - `gender ENUM('MALE', 'FEMALE') NOT NULL`
  - `photo_path VARCHAR(255) NULL`
  - `father_id INT NULL` (FK tới `PERSON(id)` ON DELETE RESTRICT)
  - `mother_id INT NULL` (FK tới `PERSON(id)` ON DELETE RESTRICT)
  - CHECK `chk_birth_year`: `birth_year >= 1000 AND birth_year <= 2100`
  - CHECK `chk_father_not_mother`: `father_id IS NULL OR mother_id IS NULL OR father_id <> mother_id`
  - TRIGGER `trg_check_not_self_parent_update`: Ngăn chặn `father_id = id` hoặc `mother_id = id` ở tầng DB.
- **Dữ liệu mẫu**: Gồm 7 bản ghi đại diện cho 4 thế hệ (`Nguyễn Văn Tổ`, `Trần Thị Tổ` $\rightarrow$ `Nguyễn Văn Cha`, `Lê Thị Mẹ` $\rightarrow$ `Nguyễn Văn Con`, `Phạm Thị Dâu` $\rightarrow$ `Nguyễn Văn Cháu`).

---

## 8. TÀI LIỆU UML HIỆN CÓ (`docs/uml/`)

Dự án đã được mô hình hóa đầy đủ bằng PlantUML và render sẵn file ảnh `.png`:
- **Class Diagram**: `class-diagram.puml` / `class-diagram.png`
- **Component Diagram**: `component-diagram.puml` / `component-diagram.png`
- **Deployment Diagram**: `deployment-diagram.puml` / `deployment-diagram.png`
- **Use Case Diagram**: `use-case.puml` / `use-case.png`
- **Sequence Diagrams**:
  - `sequence-add-person.puml`
  - `sequence-edit-person.puml` — luồng cập nhật **nguyên tử** (kiểm tra trước, 1 `UPDATE`)
  - `sequence-assign-parent.puml`
  - `sequence-family-tree.puml`
- **Activity Diagrams**:
  - `activity-add-person.puml`
  - `activity-assign-parent.puml`
  - `activity-family-tree.puml`

---

## 9. HƯỚNG DẪN BUILD & TEST

### Biên dịch dự án:
```bash
mvn clean compile
```

### Chạy ứng dụng JavaFX:
```bash
mvn javafx:run
```

### Chạy toàn bộ kiểm thử tự động (JUnit 5 — 138 test cases):
```bash
mvn clean test
```
> **Điều kiện tiên quyết**: MySQL phải đang chạy. Các test dùng database riêng `*_test` được tự tạo/xóa.
> Nếu CSDL không sẵn sàng, các test phụ thuộc CSDL sẽ được `SKIPPED` (không tính là `FAILED`).
>
> Bộ test mặc định **KHÔNG** chạy các test cần JavaFX Toolkit (tag `javafx`) — xem phần bên dưới.

### Chạy riêng các test cần JavaFX Toolkit (tag `javafx` — 26 test cases):
```bash
mvn test -Pjavafx-tests
```
Chạy riêng một lớp test:
```bash
mvn test -Pjavafx-tests -Dtest=JavaFxViewTest
mvn test -Pjavafx-tests -Dtest=AvatarFallbackTest
```
> **Điều kiện tiên quyết**: MySQL phải đang chạy. Database test riêng `gia_pha_javafx_test` được tự tạo/xóa.
> Nếu không đủ điều kiện, toàn bộ lớp test được `SKIPPED` kèm thông báo rõ ràng.
>
> **Lưu ý kỹ thuật cho AI agents**:
> - Đây là kiểm thử in-process JavaFX sử dụng `Platform.runLater()` + `CountDownLatch` (không dùng TestFX).
> - Ảnh được nạp ở chế độ background. Test **không được** sleep trên JavaFX Application Thread
>   (sẽ chặn tiến trình nạp ảnh), và **không được** gọi `Platform.exit()` trước khi ảnh nạp xong.
> - JavaFX Toolkit **không thể khởi tạo lại** sau khi `Platform.exit()` được gọi. Vì vậy
>   `FxTestSupport` chỉ đóng Stage giả và đăng ký shutdown hook, thay vì gọi `Platform.exit()`.
> - Bộ test **không** kiểm thử trải nghiệm người dùng cuối trên desktop (chuột/bàn phím) —
>   phần đó cần kiểm thử thủ công qua `mvn javafx:run`.

### Xử lý xung đột JPMS ↔ JUnit 5 (đã cấu hình sẵn trong `pom.xml`)

`src/main/java` có `module-info.java` (module `com.giapha`), còn `src/test/java` là test thuần
JUnit 5 nằm ở "unnamed module". Để tránh lỗi JPMS chặn reflection của JUnit 5, `pom.xml` cấu hình:
- `maven-compiler-plugin` → execution `default-testCompile` với `<useModulePath>false</useModulePath>`
  (test compile trên classpath, không phải module path).
- `maven-surefire-plugin` → `<useModulePath>false</useModulePath>` (test chạy trên classpath).

> **Lưu ý**: `maven-surefire-plugin` với `<useModulePath>false</useModulePath>` sẽ bỏ qua
> `module-info.class` khi chạy test. Đây là cấu hình **bắt buộc** — không được bỏ đi khi thêm test mới.

### Chạy một lớp test cụ thể:
```bash
mvn test -Dtest=FamilyTreeServiceTest
mvn test -Dtest=PersonControllerTest#handleAddCreatesPerson
```

### Bộ dữ liệu mẫu mở rộng (`test-data/`)
```bash
# Chạy bộ test trên bộ dữ liệu 30 người / 5 thế hệ (tự tạo DB riêng gia_pha_test)
mvn test -Dtest=LargeSampleDataTest

# Giữ lại DB test để debug trực tiếp (mặc định là tự DROP sau khi chạy)
mvn test -Dtest=LargeSampleDataTest -Dkeep.test.db=true

# Nạp dữ liệu mẫu vào database chính để kiểm thử thủ công trên giao diện
mysql -u root -p < test-data/sample-data.sql
```
> ⚠️ `sample-data.sql` **xoá toàn bộ** dữ liệu đang có trong bảng `PERSON` (kể cả 7 bản ghi mẫu
> nguyên bản của `database/schema.sql`). Khôi phục: `mysql -u root -p < database/schema.sql`.
>
> Bảng test case đầy đủ (mã / mô tả / dữ liệu vào / kết quả mong đợi / kết quả thực tế) nằm trong
> `test-data/test-cases.md`.

---

## 10. NGUYÊN TẮC BẮT BUỘC DÀNH CHO AI AGENTS

1. **ĐỌC EXISTING CODE TRƯỚC KHI THỰC HIỆN BẤT KỲ THAY ĐỔI NÀO**:
   - Luôn kiểm tra file hiện có, interface và chữ ký phương thức đã chốt trước khi viết code mới.
2. **TUYỆT ĐỐI KHÔNG THAY ĐỔI KIẾN TRÚC & PHẠM VI (SCOPE)**:
   - **KHÔNG** đưa Spring, Hibernate, JPA, MyBatis hay Lombok vào dự án.
   - **KHÔNG** gọi JDBC trực tiếp từ Service hoặc Controller.
   - **KHÔNG** tự ý sửa đổi `schema.sql` nếu chưa có yêu cầu rõ ràng.
   - **KHÔNG** tự ý thay đổi các diagram trong `docs/uml/`.
   - **KHÔNG** tự ý đổi kiểu dữ liệu các model `Person`, `Gender` hoặc các exception.
3. **BẢO MẬT & BẢO TOÀN DỮ LIỆU**:
   - Tuyệt đối không commit credentials, tokens, mật khẩu vào Git.
   - Các bài test làm thay đổi dữ liệu database bắt buộc phải có cleanup/rollback để đảm bảo tính cô lập (`test isolation`) và trả CSDL về trạng thái 7 bản ghi mẫu ban đầu.
