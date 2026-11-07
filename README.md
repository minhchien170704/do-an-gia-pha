# Hệ Thống Quản Lý Sơ Đồ Gia Phả (Family Tree Management)

Ứng dụng desktop quản lý phả hệ và cây gia phả, xây dựng bằng **Java 25**, **JavaFX 23** và **MySQL/MariaDB**,
tuân thủ các nguyên lý lập trình hướng đối tượng (OOP) và kiến trúc phân tầng (Layered Architecture).

> Hướng dẫn này viết cho người **vừa clone về** và muốn chạy được ngay. Làm theo thứ tự
> [Mục 2 → Mục 3 → Mục 4](#2-cài-đặt-nhanh-5-phút) là chạy được ứng dụng.

---

## Mục lục

1. [Tổng quan dự án](#1-tổng-quan-dự-án)
2. [Cài đặt nhanh (5 phút)](#2-cài-đặt-nhanh-5-phút)
3. [Cấu hình cơ sở dữ liệu](#3-cấu-hình-cơ-sở-dữ-liệu)
4. [Build, chạy ứng dụng và chạy test](#4-build-chạy-ứng-dụng-và-chạy-test)
5. [Tính năng](#5-tính-năng)
6. [Kiến trúc & hình mẫu thiết kế](#6-kiến-trúc--hình-mẫu-thiết-kế)
7. [Quy tắc nghiệp vụ (BR1–BR6)](#7-quy-tắc-nghiệp-vụ-br1br6)
8. [Cấu trúc thư mục](#8-cấu-trúc-thư-mục)
9. [Tài liệu UML](#9-tài-liệu-uml)
10. [Trạng thái phát triển](#10-trạng-thái-phát-triển)

---

## 1. Tổng quan dự án

Hệ thống hỗ trợ gia đình, dòng tộc lưu trữ, tra cứu và hiển thị trực quan thông tin các thế hệ
thành viên trong cây gia phả. Trọng tâm:

- **Tính toàn vẹn dữ liệu phả hệ** — chặn chu trình vòng lặp, chặn cha = mẹ, chặn sai giới tính vai trò,
  chặn xóa người đang được tham chiếu.
- **Giao diện JavaFX hiện đại** — sơ đồ cây gia phả 2D có zoom/pan, xuất PNG, in ấn.
- **Kiến trúc module hóa (JPMS)** rõ ràng, dễ mở rộng và bảo trì.

**Công nghệ sử dụng**

| Thành phần | Phiên bản / Ghi chú |
|---|---|
| Ngôn ngữ | Java 25 (OpenJDK 25) |
| Module system | Java Platform Module System — `module-info.java`, module `com.giapha` |
| Giao diện | JavaFX 23.0.2 (`javafx-controls`, `javafx-fxml`) |
| Build tool | Apache Maven 3.9+ |
| CSDL | MySQL 8.x hoặc MariaDB 10.x — port `3306`, charset `utf8mb4` |
| Kết nối CSDL | JDBC thuần, `mysql-connector-j` 9.2.0 (**không** dùng ORM/Hibernate/JPA/Spring) |
| Kiểm thử | JUnit 5 (`junit-jupiter` 5.10.2) + `maven-surefire-plugin` 3.2.5 |
| Mô hình hóa | PlantUML (`docs/uml/*.puml` + `*.png`) |

---

## 2. Cài đặt nhanh (5 phút)

### 2.1. Yêu cầu hệ thống

| Phần mềm | Bắt buộc | Ghi chú |
|---|---|---|
| **JDK 25** | ✅ | Kiểm tra: `java -version` → phải hiện `25.x`. Không dùng JDK 17/21 — `pom.xml` đặt `source/target = 25` |
| **Apache Maven 3.9+** | ✅ | Kiểm tra: `mvn -v` |
| **MySQL Server 8.x / MariaDB 10.x** | ✅ | Có thể dùng bản đi kèm XAMPP. Chạy ở port `3306` |
| MySQL Shell / HeidiSQL / phpMyAdmin | ⬜ | Công cụ để nạp file `.sql` — bất kỳ client nào cũng được |
| Git | ⬜ | Chỉ cần nếu bạn clone từ repository |

> **Mẹo Windows**: nếu dùng XAMPP, mở **XAMPP Control Panel** → bấm **Start** ở dòng **MySQL**,
> hoặc chạy `net start mysql` trong Command Prompt (cần quyền Administrator).
> Nếu dùng MySQL Installer / dịch vụ Windows: `net start MySQL80` (tên service có thể khác).

### 2.2. Clone & biên dịch

```bash
git clone <URL_REPO>
cd do-an-gia-pha
mvn clean compile
```

Nếu `mvn` không có trong PATH, hãy cài [Apache Maven](https://maven.apache.org/download.cgi) và
thêm `mvn\bin` vào biến môi trường `PATH`.

### 2.3. Tạo database & nạp dữ liệu mẫu

Mở terminal tại thư mục gốc dự án và chạy:

```bash
# Nạp schema + 7 bản ghi mẫu nguyên bản (xoá & tạo lại database `gia_pha`)
mysql -u root --default-character-set=utf8mb4 -e "source database/schema.sql"
```

Nếu MySQL của bạn có mật khẩu: `mysql -u root -p --default-character-set=utf8mb4 -e "source database/schema.sql"`.

Kiểm tra kết quả:

```bash
mysql -u root -e "SELECT COUNT(*) FROM gia_pha.PERSON;"   # → 7
```

> ⚠️ `database/schema.sql` chứa `DROP DATABASE IF EXISTS gia_pha;` — chạy lại sẽ **xoá sạch**
> dữ liệu đang có. Chỉ chạy khi mới cài hoặc muốn khôi phục dữ liệu gốc.

### 2.4. Chạy ứng dụng

```bash
mvn javafx:run
```

> Ứng dụng dùng **đường dẫn tương đối** cho ảnh đại diện (`images/person_<id>.png`), nên
> **luôn chạy từ thư mục gốc dự án** (`mvn javafx:run` đã đảm bảo điều này).
> Nếu mở trực tiếp file JAR từ chỗ khác, ảnh sẽ không hiển thị.

---

## 3. Cấu hình cơ sở dữ liệu

`com.giapha.database.DatabaseConnection` đọc cấu hình theo thứ tự ưu tiên:
**System property → Environment variable → giá trị mặc định**.

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/gia_pha?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` | JDBC URL đầy đủ |
| `DB_USER` | `root` | Tên đăng nhập MySQL |
| `DB_PASSWORD` | *(rỗng)* | Mật khẩu MySQL |

### Cách đặt bằng Environment variable (Windows PowerShell)

```powershell
$env:DB_URL    = "jdbc:mysql://localhost:3306/gia_pha?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:DB_USER   = "root"
$env:DB_PASSWORD = "mat_khau_cua_ban"
mvn javafx:run
```

### Cách đặt bằng System property (khi chạy test / chạy từ IDE)

```bash
mvn javafx:run -DDB_URL="jdbc:mysql://localhost:3306/gia_pha?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" -DDB_USER=root -DDB_PASSWORD=secret
```

> 🔒 **Không** commit mật khẩu thật vào mã nguồn. `.gitignore` đã loại `.env` và `*.local`.
> Hãy dùng biến môi trường của máy bạn.

---

## 4. Build, chạy ứng dụng và chạy test

### 4.1. Build

```bash
mvn clean compile        # biên dịch
mvn clean package        # đóng gói thành target/do-an-gia-pha-1.0-SNAPSHOT.jar
```

### 4.2. Chạy ứng dụng

```bash
mvn javafx:run
```

### 4.3. Chạy toàn bộ kiểm thử (188 test cases)

```bash
mvn clean test
```

| Lớp kiểm thử | Số test | Phạm vi |
|---|---|---|
| `FamilyTreeServiceTest` | 44 | BR1–BR6, TC01–TC14, TC-CLEAR-01…05, cô lập dữ liệu |
| `PersonControllerTest` | 25 | Điều phối View ↔ Service, xử lý exception, callback điều hướng |
| `LargeSampleDataTest` | 22 | Bộ dữ liệu mẫu 30 người / 5 thế hệ (BR1–BR6, validation, ràng buộc CSDL, an toàn DROP DB) |
| `ImageManagerTest` | 12 | Định dạng ảnh, copy vào `./images/`, dọn file an toàn |
| `PngWriterTest` | 11 | Chữ ký PNG, khối IHDR/IDAT/IEND, CRC32, nén zlib |
| `AtomicUpdateAndBr5Test` | 9 | Cập nhật nguyên tử (cha/mẹ + thông tin cá nhân), BR5 trong luồng lưu của form |
| `DatabaseConnectionTest` | 7 | Kết nối MySQL, metadata, bảng `PERSON`, kiểm chứng DB chính không bị đổi |
| `JdbcPersonRepositoryConnectionTest` | 5 | Quyền sở hữu `Connection`: repo tự mở thì tự đóng, Connection ngoài thì không đóng |
| `BackendInitializationTest` | 3 | Kiểm tra kết nối MySQL thật trước khi hiển thị giao diện |

**Điều kiện tiên quyết**: MySQL phải đang chạy. Các test dùng database **riêng** (tên kết thúc bằng
`_test`) được **tự động tạo ở `@BeforeAll`** và **tự động xóa ở `@AfterAll`** — database chính
`gia_pha` **không bao giờ** bị test ghi vào. Nếu CSDL không sẵn sàng, các test đó được đánh dấu
`SKIPPED` (không tính là `FAILED`).

### 4.4. Chạy riêng các test cần JavaFX Toolkit (tag `javafx` — 30 test cases)

```bash
mvn test -Pjavafx-tests
mvn test -Pjavafx-tests -Dtest=JavaFxViewTest
mvn test -Pjavafx-tests -Dtest=AvatarFallbackTest
```

### 4.5. Chạy một lớp / một phương thức test cụ thể

```bash
mvn test -Dtest=FamilyTreeServiceTest
mvn test -Dtest=PersonControllerTest#handleAddCreatesPerson
mvn test -Dtest=LargeSampleDataTest
```

### 4.6. Giữ lại database test để debug

```bash
mvn test -Dtest=LargeSampleDataTest -Dkeep.test.db=true
```

> Chỉ database có tên **kết thúc bằng `_test`** mới được phép `DROP`. Trước khi xóa, hàm
> `TestDatabaseManager.assertSafeToDrop()` kiểm tra điều này và **ném `IllegalStateException`
> + không DROP** nếu tên không khớp — không bao giờ xóa nhầm database chính `gia_pha`.

### 4.7. Bộ dữ liệu mẫu mở rộng (`test-data/`)

| File | Nội dung |
|---|---|
| `test-data/sample-data.sql` | Script SQL tạo **30 bản ghi** `PERSON` — 5 thế hệ (1850 → 1970), 4 nhánh dòng họ |
| `test-data/test-cases.md` | Bảng test case: **mã, mô tả, dữ liệu vào, kết quả mong đợi, kết quả thực tế** + ma trận phủ BR1–BR6 |

Bộ test tương ứng là `src/test/java/com/giapha/testdata/LargeSampleDataTest.java` (22 test) — nạp trực tiếp
`test-data/sample-data.sql` vào database riêng `gia_pha_test`, nên dữ liệu mẫu và bảng test case
luôn **đồng bộ**.

```bash
# Nạp dữ liệu mẫu vào database CHÍNH để kiểm thử thủ công trên giao diện
mysql -u root -p < test-data/sample-data.sql

# Khôi phục lại 7 bản ghi mẫu nguyên bản
mysql -u root -p < database/schema.sql
```

> ⚠️ `sample-data.sql` **xoá toàn bộ** dữ liệu đang có trong bảng `PERSON`.

### 4.8. Giải thích lệch số liệu khi đọc log Surefire

Nếu bạn cộng tay từng dòng log Surefire (25 + 9 + 19 + 23 + 3 + 7 + 8 + 5 + 44 + 0 + 22 + 12 + 11) thì ra **188**,
không phải 210. Con số 210 xuất hiện khi **báo cáo cũ** cộng thêm một dòng
"22 test — FamilyTreeServiceTest (phần lồng)" *cùng* với dòng `FamilyTreeServiceTest$DataIsolation`
đã chứa 44 test — tức là **đếm trùng** các test của lớp lồng `@Nested`.

Cơ chế thật (đã kiểm chứng qua `target/surefire-reports/TEST-*.xml`):

- `FamilyTreeServiceTest` khai báo **43** `@Test` ở thân lớp + **1** `@Test` trong lớp lồng
  `@Nested DataIsolation` → **44** test thực thi.
- Surefire ghi **hai** dòng log cho cùng một lớp:
  - `... -- in com.giapha.service.FamilyTreeServiceTest$DataIsolation` → **44 test**
    (chứa cả 43 test thân lớp lẫn 1 test lồng — xác nhận qua thuộc tính `classname` trong XML);
  - `... -- in com.giapha.service.FamilyTreeServiceTest` → **0 test**, vì mọi test đã được
    gán cho container lồng. Đây là *vỏ rỗng*, **không phải** test bị mất.
- Vì vậy **chỉ dòng tổng cuối cùng** là con số chuẩn xác — đó là những gì Surefire thực sự thực thi:

```
[INFO] Results:
[INFO] Tests run: 188, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Dòng tổng này **không** bao gồm 30 test tag `javafx` (chạy riêng qua `mvn test -Pjavafx-tests`).

### 4.9. Ghi chú về xung đột JPMS ↔ JUnit 5

`src/main/java` có `module-info.java` (module `com.giapha`), còn `src/test/java` là test thuần
JUnit 5 nằm ở "unnamed module". Để tránh lỗi JPMS chặn reflection của JUnit 5, `pom.xml` cấu hình:

- `maven-compiler-plugin` → execution `default-testCompile` với `<useModulePath>false</useModulePath>`.
- `maven-surefire-plugin` → `<useModulePath>false</useModulePath>`.

> ⚠️ **Không được bỏ cấu hình này** khi thêm test mới — nếu không, `mvn test` sẽ lỗi compile
> hoặc lỗi `IllegalAccessError` khi JUnit 5 dùng reflection gọi các `@Test` method.

---

## 5. Tính năng

### 5.1. Quản lý thành viên

- Thêm mới, cập nhật hồ sơ, xem chi tiết (họ tên, năm sinh, giới tính, ảnh đại diện).
- Tìm kiếm theo từ khoá trên danh sách.
- **Ảnh đại diện**: chọn ảnh qua `FileChooser` (chỉ nhận `png` / `jpg` / `jpeg`), ảnh được copy vào
  thư mục `images/` cạnh ứng dụng và lưu **đường dẫn tương đối** (`images/person_<id>.<đuôi>`) vào DB.
- Component `PersonAvatar` hiển thị ảnh bo tròn, **tự fallback** về chữ cái đầu khi thiếu ảnh /
  file ảnh hỏng / sai định dạng — không bao giờ ném exception ra giao diện.

### 5.2. Quan hệ phả hệ

- Gán / gỡ cha (`assignFather` / `clearFather`) và mẹ (`assignMother` / `clearMother`).
- Mọi thao tác đều được kiểm tra theo 6 quy tắc nghiệp vụ (xem [Mục 7](#7-quy-tắc-nghiệp-vụ-br1br6)).
- **Cập nhật nguyên tử**: khi lưu từ form, `FamilyTreeService.updatePersonWithParents(...)` kiểm tra
  **toàn bộ** thông tin cá nhân + cha + mẹ **trước**, rồi mới ghi bằng **một** câu `UPDATE`.
  Nếu bất kỳ quan hệ nào vi phạm BR1–BR3/BR6 thì **không có** thay đổi nào được ghi — dữ liệu
  không bao giờ ở trạng thái "cập nhật một nửa".
- Tra cứu đa thế hệ: `getAncestors` (tổ tiên) và `getDescendants` (hậu duệ).
- Phát hiện chu trình phả hệ bằng thuật toán duyệt đồ thị (DFS) với `Set` chống lặp.
- Xóa an toàn: chặn xóa thành viên đang làm cha/mẹ của người khác (không cascade, không tự set NULL).

### 5.3. Sơ đồ cây gia phả 2D

Màn hình **Cây gia phả** dùng `TabPane` với hai tab:

| Tab | Nội dung |
|---|---|
| **Sơ đồ 2D** | `com.giapha.view.FamilyTreeDiagram` — vẽ bằng `Pane` + `Line` |
| **Danh sách phả hệ** | `TreeView` dạng danh sách phân cấp (bản cũ, giữ lại để đối chiếu) |

**Bố cục sơ đồ 2D**

- Mỗi thế hệ nằm trên một hàng; tổ tiên ở **trên**, hậu duệ ở **dưới** người trung tâm.
- Mỗi node hiển thị **avatar + họ tên + năm sinh + giới tính**.
- Cha và mẹ sinh chung một con được gom thành một *khối gia đình* để hiển thị cạnh nhau;
  con được canh chính giữa giữa hai vị trí cha/mẹ.
- Đường kẻ nối **từng cha/mẹ xuống đúng con của họ** (vẽ ở mức cá nhân, không nối nhầm vợ/chồng).
- Node của người trung tâm được tô viền đậm màu xanh.

**Tương tác**

| Thao tác | Kết quả |
|---|---|
| Kéo chuột trái | Di chuyển sơ đồ (pan) |
| Lăn chuột dọc | Cuộn dọc |
| Shift + lăn chuột | Cuộn ngang |
| Ctrl + lăn chuột | Zoom (0.25× – 2.5×) |
| Nút `−` / `+` / `Vừa khung` | Zoom out / zoom in / tự động vừa khung nhìn |
| Bấm vào node | Mở hồ sơ cá nhân của thành viên đó |

Combo box **"Người trung tâm"** thay đổi → sơ đồ 2D được vẽ lại tức thì.

### 5.4. Xuất PNG & In ấn

| Nút | Cơ chế | Ghi chú |
|---|---|---|
| **🖼 Xuất PNG** | `Node.snapshot()` với `SnapshotParameters.setViewport(...)` đúng bằng kích thước nội dung sơ đồ, sau đó ghi file bằng `com.giapha.util.PngWriter` | Ảnh chụp **toàn bộ** sơ đồ ở mức zoom 1.0, không bị cắt theo khung nhìn hiện tại. `FileChooser` cho chọn nơi lưu |
| **🖨 In** | `javafx.print.PrinterJob` + `PrinterJob.printPage(node)` | Dùng `JobSettings.getPageLayout()` để lấy khổ giấy; ảnh được bọc trong `StackPane` kích thước trang và canh giữa |

> **Không thêm dependency mới.** `PngWriter` hiện thực thuần bằng `java.util.zip` (`Deflater` + `CRC32`)
> nên module `com.giapha` không cần khai báo thêm `java.desktop` hay `javafx.swing`.

Kết quả thực hiện (thành công / thất bại / không tìm thấy máy in / sơ đồ rỗng) đều được báo bằng `Alert`
thay vì ném exception ra giao diện.

---

## 6. Kiến trúc & hình mẫu thiết kế

### 6.1. Luồng phụ thuộc một chiều

```
JavaFX UI (View)
       ↓
PersonController (Controller)
       ↓
FamilyTreeService (Business Service Layer)
       ↓
PersonRepository  ← interface (Data Access)
       ↓
JdbcPersonRepository (Data Access Implementation)
       ↓
DatabaseConnection (Static Factory)
       ↓
MySQL / MariaDB
```

### 6.2. Quyền sở hữu `Connection` & kiểm tra CSDL lúc khởi động

**Quyền sở hữu `Connection`** (`JdbcPersonRepository`):

| Nguồn `Connection` | Ai đóng? | Hành vi khi `Connection` đã đóng |
|---|---|---|
| Repository tự mở qua `DatabaseConnection.getConnection()` | **Repository** (`closeIfOwned(...)`) | Mở connection mới cho thao tác tiếp theo |
| `Connection` truyền từ bên ngoài (constructor có tham số) | **Bên ngoài** — repository chỉ **mượn** | Ném `SQLException` rõ ràng, **không** âm thầm chuyển sang database chính |

**Kiểm tra CSDL lúc khởi động** (`Main.initBackend()`): trước khi hiển thị giao diện, ứng dụng
mở một `Connection` thật qua `DatabaseConnection.getConnection()`. Nếu thất bại (MySQL chưa
chạy / sai cấu hình), ứng dụng **thông báo rõ cách cấu hình và dừng khởi động có kiểm soát**
thay vì để lỗi SQL lọt ra giữa lúc người dùng đang thao tác.

### 6.3. Các hình mẫu thiết kế đã dùng

Bảng dưới liệt kê **chỉ những mẫu có bằng chứng trực tiếp trong mã nguồn** — mỗi dòng đều
trỏ tới file cụ thể, tên lớp cụ thể và vai trò đóng vai trò nào trong mẫu.
Cột "Loại" phân biệt rõ: **Kiến trúc** (mô hình tổ chức hệ thống), **Mẫu thiết kế**
(GoF / enterprise pattern), **Idiom** (kỹ thuật lập trình quy ước, không phải mẫu GoF),
**Nguyên tắc** (SOLID & nguyên tắc thiết kế).

| Hình mẫu / Nguyên tắc | Loại | File & Lớp | Vai trò trong mẫu | Bằng chứng trong code |
|---|---|---|---|---|
| **Layered Architecture (Kiến trúc phân tầng)** | Kiến trúc | `com.giapha.view.*`, `com.giapha.controller.PersonController`, `com.giapha.service.FamilyTreeService`, `com.giapha.repository.PersonRepository` + `JdbcPersonRepository`, `com.giapha.database.DatabaseConnection` | Mỗi tầng là một vai trò riêng biệt: **View** (hiển thị) → **Controller** (điều phối) → **Service** (nghiệp vụ) → **Repository** (truy cập dữ liệu) → **Database** (kết nối). Tầng trên chỉ gọi tầng ngay dưới nó. | `view/*.java` chỉ `import com.giapha.controller.*` (không import service/repository/database); `FamilyTreeService` chỉ import `com.giapha.repository.PersonRepository`. |
| **MVC (Model–View–Controller)** | Kiến trúc | **Model**: `com.giapha.model.Person`, `com.giapha.model.Gender` · **View**: `PeopleListView`, `FamilyTreeView`, `FamilyTreeDiagram`, `PersonDetailView`, `PersonFormView`, `PersonAvatar` · **Controller**: `PersonController` · **Entry point / Composition Root**: `com.giapha.Main` | `Person`/`Gender` là **Model** (thuộc tính dữ liệu thuần + getter/setter, không chứa SQL hay UI); các lớp `*View` là **View** (dựng UI, bắt sự kiện); `PersonController` là **Controller** (nhận sự kiện từ View, gọi Service, trả kết quả về View). `Main` khởi tạo và nối các thành phần. | `PersonController.handleAdd()` đọc dữ liệu từ `FormDataSource` (abstraction do `PersonFormView` hiện thực) → gọi `familyTreeService.addPerson()` → `showInfoAlert()`; `Main.initBackend()` lắp ráp `JdbcPersonRepository → FamilyTreeService → PersonController`. |
| **Dependency Inversion (SOLID – D)** | Nguyên tắc (SOLID) | `com.giapha.service.FamilyTreeService` (trường `private final PersonRepository personRepository`) | Tầng cao (`Service`) phụ thuộc vào **abstraction** (`PersonRepository` interface) thay vì phụ thuộc vào chi tiết (`JdbcPersonRepository`, JDBC API). Đây là hướng phụ thuộc bị đảo so với mặc định. | `FamilyTreeService` **không** import `JdbcPersonRepository`, `DatabaseConnection`, `java.sql.*`; thân hàm chỉ gọi `personRepository.findById/save/update/delete`. |
| **Constructor Injection (DI)** | Mẫu thiết kế (DI) | `FamilyTreeService(PersonRepository)` · `PersonController(FamilyTreeService)` · `Main.initBackend()` | **Injection point** là constructor; **composition root** là `Main` — nơi duy nhất quyết định dùng triển khai nào (`new JdbcPersonRepository()`). Nhờ đó có thể thay CSDL (hoặc truyền repository test) mà không sửa Service. | `Main.initBackend()`: `new JdbcPersonRepository()` → `new FamilyTreeService(repository)` → `new PersonController(service)`; `Objects.requireNonNull(...)` chặn dependency null. |
| **Repository Pattern** | Mẫu thiết kế (Enterprise) | **Interface (abstraction)**: `com.giapha.repository.PersonRepository` (`findById`, `findAll`, `search`, `save`, `update`, `delete`) · **Concrete repository**: `com.giapha.repository.JdbcPersonRepository` | Interface đóng vai trò **abstraction của kho lưu trữ**; `JdbcPersonRepository` là **Concrete implementation** gắn SQL vào bảng `PERSON` và map `ResultSet` → đối tượng `Person`. Toàn bộ SQL nằm sau abstraction này. | `JdbcPersonRepository implements PersonRepository`; mọi câu `SELECT/INSERT/UPDATE/DELETE` đều nằm trong class này, không lọt sang tầng trên. |
| **Static Factory Method (JDBC Factory)** | Idiom (mẫu khởi tạo tập trung) | `com.giapha.database.DatabaseConnection.getConnection()` và `getConnection(url, user, password)` | **Factory** tạo đối tượng `java.sql.Connection` (product) mà không để client tự gọi `DriverManager`. Trả về **Connection mới** mỗi lần — không dùng connection `static` dài hạn; caller đóng bằng try-with-resources. | `DatabaseConnection` có constructor `private` (utility class) và chỉ có phương thức `static`; `JdbcPersonRepository.getConnection()` gọi `DatabaseConnection.getConnection()`. |
| **Template Method (điểm móc truy cập CSDL)** | Mẫu thiết kế (GoF) | **Base class**: `JdbcPersonRepository.getConnection()` (`protected`) · **Subclass**: `src/test/java/com/giapha/support/TestPersonRepository.getConnection()` (`@Override`) | Base class định nghĩa **bước có thể ghi đè** (lấy kết nối); subclass chỉ thay **bước cụ thể** để trỏ sang database test, còn toàn bộ CRUD kế thừa nguyên vẹn. Đây chính là cơ chế cho **Test Double** cô lập dữ liệu. | `TestPersonRepository extends JdbcPersonRepository` và `@Override protected Connection getConnection()` trả `TestDatabaseManager.openConnection(testDbName)`. |
| **Custom Exception (hierarchy lỗi nghiệp vụ)** | Mẫu thiết kế (Error handling) | `com.giapha.exception.*`: `SelfParentException`, `SameParentException`, `InvalidParentGenderException`, `CycleDetectedException`, `PersonInUseException`, `PersonNotFoundException`, `InvalidBirthYearException` — tất cả kế thừa `RuntimeException` | Mỗi lớp là **thông điệp có kiểu** cho một quy tắc nghiệp vụ (BR1–BR6). Tầng `Service` **ném** (không trả mã lỗi), tầng `Controller` **bắt** theo kiểu và ánh xạ sang thông báo tiếng Việt — stack trace không bao giờ lọt ra UI. | `FamilyTreeService.assignFather()` ném `SelfParentException` / `SameParentException` / `InvalidParentGenderException` / `CycleDetectedException`; `PersonController` có `catch` riêng cho từng loại và gọi `showErrorAlert(...)`. |
| **Observer (thuộc tính JavaFX)** | Mẫu thiết kế (GoF — dạng property observer) | **Subject**: `com.giapha.view.FamilyTreeDiagram` — trường `zoomProperty` (`ReadOnlyDoubleWrapper`), expose qua `contentScaleProperty()` · **Observer**: `com.giapha.view.FamilyTreeView` — `diagram.contentScaleProperty().addListener(...)` | `FamilyTreeDiagram` là **Subject** giữ trạng thái zoom và phát thông báo khi trạng thái đổi; `FamilyTreeView` là **Observer** cập nhật nhãn `%` trên toolbar mà không cần sơ đồ biết đến toolbar. | `FamilyTreeDiagram.contentScaleProperty()` trả `ReadOnlyDoubleProperty`; `FamilyTreeView` đăng ký listener để set `lblZoom.setText(... + "%")`. |
| **Utility Class (lớp tiện ích tĩnh)** | Idiom (tiện ích) | `com.giapha.util.ImageManager` · `com.giapha.util.PngWriter` | Constructor `private` + toàn bộ phương thức `static` → **không có trạng thái, không thể khởi tạo từ bên ngoài**; chỉ cung cấp hành vi dùng chung (copy ảnh, ghi file PNG). | Cả hai class đều có `private ImageManager()` / `private PngWriter()` và không có field instance. |

> **Ghi chú — các mục từng được liệt kê nhưng đã bị loại khỏi bảng** vì **không phải hình mẫu
> thiết kế theo đúng nghĩa** (không có bằng chứng cấu trúc tương ứng trong code):
>
> - ~~"Strategy cấu hình"~~ → `DatabaseConnection.getUrl()/getUser()/getPassword()` chỉ là **chuỗi
>   ưu tiên System property → Environment variable → giá trị mặc định** (thứ tự cố định,
>   không có interface/đối tượng chiến lược thay thế nhau) → thuộc loại **cấu hình theo môi trường**,
>   không phải Strategy Pattern.
> - ~~"Data Access Object mở rộng (test)"~~ → không phải một mẫu riêng; đã được gộp đúng bản chất
>   vào dòng **Template Method** (`TestPersonRepository` ghi đè điểm móc `getConnection()`).
> - ~~"Separation of Concerns"~~ → là **nguyên tắc thiết kế**, không phải hình mẫu cụ thể có
>   cấu trúc lớp định danh; nội dung của nó đã được mô tả đầy đủ ở mục **6.4 Ranh giới trách nhiệm**
>   và trong dòng *Layered Architecture* ở trên.
> - ~~"Singleton"~~ → **không** dùng: `DatabaseConnection` là utility class tĩnh, **không** giữ
>   instance `static` dài hạn; mỗi lần gọi đều trả `Connection` mới (xem dòng *Static Factory Method*).

#### Đường dẫn file đầy đủ của các lớp được nhắc tới trong bảng

| Lớp | Đường dẫn file |
|---|---|
| `Main` | `src/main/java/com/giapha/Main.java` |
| `Person`, `Gender` | `src/main/java/com/giapha/model/Person.java`, `src/main/java/com/giapha/model/Gender.java` |
| `PersonController` | `src/main/java/com/giapha/controller/PersonController.java` |
| `FamilyTreeService` | `src/main/java/com/giapha/service/FamilyTreeService.java` |
| `PersonRepository`, `JdbcPersonRepository` | `src/main/java/com/giapha/repository/PersonRepository.java`, `src/main/java/com/giapha/repository/JdbcPersonRepository.java` |
| `DatabaseConnection` | `src/main/java/com/giapha/database/DatabaseConnection.java` |
| `PeopleListView`, `FamilyTreeView`, `FamilyTreeDiagram`, `PersonDetailView`, `PersonFormView`, `PersonAvatar` | `src/main/java/com/giapha/view/` (mỗi lớp một file cùng tên) |
| `ImageManager`, `PngWriter` | `src/main/java/com/giapha/util/ImageManager.java`, `src/main/java/com/giapha/util/PngWriter.java` |
| 7 lớp exception BR1–BR6 | `src/main/java/com/giapha/exception/` (`SelfParentException`, `SameParentException`, `InvalidParentGenderException`, `CycleDetectedException`, `PersonInUseException`, `PersonNotFoundException`, `InvalidBirthYearException`) |
| `TestPersonRepository` | `src/test/java/com/giapha/support/TestPersonRepository.java` |

### 6.4. Ranh giới trách nhiệm

| Tầng | Trách nhiệm | KHÔNG được làm |
|---|---|---|
| **View** | Hiển thị, bắt sự kiện, gọi Controller | Không chứa SQL, không gọi Service/Repository trực tiếp |
| **Controller** | Điều phối View ↔ Service, chuyển exception thành thông báo | Không viết SQL, không gọi Repository/JDBC |
| **Service** | Quy tắc nghiệp vụ BR1–BR6, validation, phát hiện chu trình | Không import bất kỳ JDBC API nào |
| **Repository** | CRUD bảng `PERSON` bằng JDBC thuần | Không chứa quy tắc nghiệp vụ |
| **Database** | Cấp `Connection` mới | Không giữ connection dài hạn |

---

## 7. Quy tắc nghiệp vụ (BR1–BR6)

| Mã | Tên | Nội dung | Xử lý khi vi phạm |
|---|---|---|---|
| **BR1** | Self Parent | Một người không thể là cha hoặc mẹ của chính mình | `SelfParentException` |
| **BR2** | Father ≠ Mother | Cha và mẹ của cùng một người không được là cùng một người | `SameParentException` |
| **BR3** | No Cycle | Không được tạo chu trình vòng lặp trong gia phả | `CycleDetectedException` |
| **BR4** | Person In Use | Không xóa được `Person` đang được tham chiếu làm cha/mẹ của người khác | `PersonInUseException` |
| **BR5** | Parent Birth Year | Năm sinh của cha/mẹ nên nhỏ hơn năm sinh của con | **Cảnh báo** (không chặn) — `FamilyTreeService.checkBirthYearWarning(...)` trả về danh sách thông điệp; Controller hiện `Alert` với lựa chọn **Tiếp tục** / **Hủy** |
| **BR6** | Gender Role | `father` bắt buộc là `Gender.MALE`, `mother` bắt buộc là `Gender.FEMALE` | `InvalidParentGenderException` |

**Thứ tự ưu tiên kiểm tra** trong `assignFather` / `assignMother` (để thông báo đúng nguyên nhân):

```
BR1 (SelfParent) → BR2 (SameParent) → BR6 (InvalidParentGender) → BR3 (Cycle)
```

**Ràng buộc năm sinh**: `1000 <= birthYear <= Year.now().getValue()`.
Hằng số `FamilyTreeService.MIN_BIRTH_YEAR = 1000` là nguồn sự thật duy nhất ở tầng Service;
ràng buộc tương ứng ở tầng CSDL là `CHECK (birth_year >= 1000 AND birth_year <= 2100)`.
Việc nới lỏng này nhằm phục vụ các bộ dữ liệu gia phả lịch sử nhiều thế hệ (xem `test-data/`).

**Bảo vệ ở tầng CSDL** (`database/schema.sql`):

- `CHECK chk_birth_year` — `birth_year >= 1000 AND birth_year <= 2100`
- `CHECK chk_father_not_mother` — `father_id IS NULL OR mother_id IS NULL OR father_id <> mother_id`
- `TRIGGER trg_check_not_self_parent_update` — chặn `father_id = id` / `mother_id = id`
- `ON DELETE RESTRICT` cho cả hai khóa ngoại `father_id` / `mother_id`

---

## 8. Cấu trúc thư mục

```
do-an-gia-pha/
├── pom.xml
├── README.md
├── AGENTS.md                       # Hướng dẫn dành cho AI Agent (source of truth kiến trúc)
├── .gitignore
├── database/
│   └── schema.sql                  # Định nghĩa bảng PERSON, index, trigger & 7 bản ghi mẫu
├── docs/
│   └── uml/                        # Toàn bộ PlantUML (.puml) và ảnh sơ đồ (.png)
├── test-data/
│   ├── sample-data.sql             # Bộ dữ liệu mẫu 30 người / 5 thế hệ
│   └── test-cases.md               # Bảng test case (mã, mô tả, dữ liệu vào, KQ mong đợi, KQ thực tế)
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── module-info.java
│   │   │   └── com/giapha/
│   │   │       ├── Main.java                 # Điểm khởi chạy JavaFX
│   │   │       ├── model/                    # Person, Gender
│   │   │       ├── exception/                # Custom exception cho BR1–BR6
│   │   │       ├── database/                 # DatabaseConnection (Static Factory)
│   │   │       ├── repository/               # PersonRepository, JdbcPersonRepository
│   │   │       ├── service/                  # FamilyTreeService
│   │   │       ├── controller/               # PersonController
│   │   │       ├── util/                     # ImageManager, PngWriter
│   │   │       └── view/                     # JavaFX Views + FamilyTreeDiagram + PersonAvatar
│   │   └── resources/
│   │       ├── css/                          # style.css
│   │       ├── fxml/                         # FXML layouts
│   │       └── images/                       # Icon & ảnh tĩnh
│   │                                       # (ảnh đại diện người dùng copy vào ./images/ lúc runtime)
│   └── test/
│       └── java/
│           └── com/giapha/
│               ├── support/                  # TestDatabaseManager, TestPersonRepository,
│               │                             # TestDatabase, FxTestSupport
│               ├── database/                 # DatabaseConnectionTest
│               ├── service/                  # FamilyTreeServiceTest
│               ├── controller/               # PersonControllerTest
│               ├── util/                     # ImageManagerTest, PngWriterTest
│               ├── testdata/                 # LargeSampleDataTest (30 người / 5 thế hệ)
│               └── view/                     # AvatarFallbackTest, JavaFxViewTest (tag "javafx")
```

---

## 9. Tài liệu UML

Tất cả sơ đồ nằm tại thư mục `docs/uml/` (mã nguồn `.puml` + ảnh render `.png`).
Sơ đồ được đồng bộ với code hiện tại trong `src/main/java` — **không vẽ lớp test**.

| Sơ đồ | File |
|---|---|
| **Lớp — tổng quan** | `class-diagram.puml` / `class-diagram.png` |
| **Lớp (a) — model + exception** | `class-diagram-model-exception.puml` / `class-diagram-model-exception.png` |
| **Lớp (b) — service + repository + database** | `class-diagram-service-repository.puml` / `class-diagram-service-repository.png` |
| **Lớp (c) — view + controller** | `class-diagram-view-controller.puml` / `class-diagram-view-controller.png` |
| Thành phần | `component-diagram.puml` / `component-diagram.png` |
| Triển khai | `deployment-diagram.puml` / `deployment-diagram.png` |
| Ca sử dụng | `use-case.puml` / `use-case.png` |
| Tuần tự — thêm người | `sequence-add-person.puml` / `sequence-add-person.png` |
| Tuần tự — gán cha/mẹ | `sequence-assign-parent.puml` / `sequence-assign-parent.png` |
| Tuần tự — xem cây gia phả | `sequence-family-tree.puml` / `sequence-family-tree.png` |
| Tuần tự — xuất PNG / In | `sequence-export-print.puml` / `sequence-export-print.png` |
| Tuần tự — chọn & lưu ảnh đại diện | `sequence-photo-avatar.puml` / `sequence-photo-avatar.png` |
| Hoạt động — thêm người | `activity-add-person.puml` / `activity-add-person.png` |
| Hoạt động — gán cha/mẹ | `activity-assign-parent.puml` / `activity-assign-parent.png` |
| Hoạt động — xem cây gia phả | `activity-family-tree.puml` / `activity-family-tree.png` |
| Hoạt động — xuất PNG / In | `activity-export-print.puml` / `activity-export-print.png` |
| Hoạt động — chọn & lưu ảnh đại diện | `activity-photo-avatar.puml` / `activity-photo-avatar.png` |

### Render lại sơ đồ

Cần [PlantUML](https://plantuml.com/) + GraphViz (`dot`). Tải `plantuml.jar` rồi chạy
trong thư mục `docs/uml/`:

```bash
java -jar plantuml.jar -verbose *.puml
```

Lệnh trên vừa kiểm tra cú pháp vừa xuất lại toàn bộ file `.png`. Kiểm tra lỗi cú pháp
không cần render:

```bash
java -jar plantuml.jar -checkonly *.puml
```

> ### ⚠️ Lưu ý cú pháp PlantUML (đã kiểm chứng bằng PlantUML 1.2026.9)
>
> **Activity Diagram** và **Sequence Diagram** dùng **hai bộ từ khoá khác nhau** — nhầm
> giữa hai loại là nguyên nhân phổ biến nhất gây lỗi cú pháp:
>
> | Khối | Activity Diagram | Sequence Diagram |
> |---|---|---|
> | Rẽ nhánh | `if (điều kiện?) then (có)` … `elseif` … `else` … **`endif`** | **`alt`** … `else` … **`end`** |
> | Vòng lặp | **`while (điều kiện?)`** … **`endwhile`**<br>`repeat` … **`repeat while (điều kiện?) is (có)`** | **`loop`** … **`end`** |
> | Tuỳ chọn | ❌ **không hỗ trợ** (`opt` → lỗi cú pháp) | **`opt`** … **`end`** |
> | Song song | `fork` … `fork again` … **`end fork`**<br>`split` … `split again` … **`end split`** | **`par`** … `par again` … **`end`**<br>(không dùng từ khoá `and` — sẽ lỗi cú pháp) |
> | Ghi chú | `note right` … **`end note`** | `note right of X` … **`end note`** |
> | Nhóm | — | **`group`** … **`end`** |
> | Thoát vòng lặp | **`break`** (đặt trong `while`/`repeat`) | **`break`** (đặt trong `loop`) |
> | Rời nhánh | **`stop`** (kết thúc luồng) | `destroy X` / `create X` |
>
> Lưu ý thêm:
>
> - Trong **Activity Diagram**, khối `opt` và `loop` **không tồn tại** — phải chuyển
>   thành `if (…?) then … else … endif` (nhánh tuỳ chọn) hoặc `while (…?) … endwhile`
>   (vòng lặp).
> - Trong **Activity Diagram**, `repeat` kết thúc bằng **`repeat while`** (không phải
>   `end repeat`).
> - Trong **Activity Diagram**, `if` phải có **`endif`**; trong **Sequence Diagram**,
>   `alt`/`opt`/`loop` kết thúc bằng **`end`**.
> - Lệnh `title` phải nằm trên **một dòng**; muốn xuống dòng dùng `\n`.
>   Viết `title` trên nhiều dòng thật sẽ báo lỗi cú pháp.
> - `note right of X` (Sequence) khác `note right` (Activity) — thiếu `of <đối tượng>`
>   sẽ báo lỗi.

### Sơ đồ lớp được tách theo gói

Sơ đồ lớp gốc có 25 lớp + 7 exception → ảnh render rất rộng. Vì vậy:

- `class-diagram.puml` — **tổng quan**: chỉ hiển thị tên lớp, đầy đủ quan hệ giữa các gói.
- `class-diagram-model-exception.puml` — thuộc tính/phương thức của `Person`, `Gender` và 7 lớp exception.
- `class-diagram-service-repository.puml` — thuộc tính/phương thức của `FamilyTreeService`, `PersonRepository`, `JdbcPersonRepository`, `DatabaseConnection` + các stereotype mẫu thiết kế.
- `class-diagram-view-controller.puml` — thuộc tính/phương thức của 6 lớp `view` + `PersonController` + quan hệ Observer.

---

## 10. Trạng thái phát triển

| Cụm | Nội dung | Trạng thái |
|---|---|---|
| **1–4** | Thiết kế UML, kiến trúc, UI Mockup & Design System JavaFX | ✅ Hoàn thành |
| **5.1–5.2** | Model Layer, Custom Exceptions, Repository Layer (interface + JDBC) | ✅ Hoàn thành |
| **5.3** | Database Schema, MySQL setup, `DatabaseConnection` | ✅ Hoàn thành |
| **5.4** | `FamilyTreeService` + bộ test nghiệp vụ | ✅ Hoàn thành |
| **5.5** | `PersonController` + bộ test Controller (BR5 cảnh báo, ảnh đại diện, xử lý exception đầy đủ) | ✅ Hoàn thành |
| **5.6** | Tích hợp toàn diện JavaFX UI với MySQL | ✅ Hoàn thành |
| **5.7** | End-to-End verification, QA tích hợp, kiểm chứng toàn vẹn dữ liệu | ✅ Hoàn thành |
| **5.8** | Đồng bộ hóa tài liệu (README) và sơ đồ lớp | ✅ Hoàn thành |
| **5.9** | Native in-process JavaFX test harness (`JavaFxViewTest`) | ✅ Hoàn thành |
| **5.10** | `PersonAvatar` — component ảnh đại diện dùng chung, fallback an toàn | ✅ Hoàn thành |
| **6.1** | Sơ đồ cây gia phả 2D (`FamilyTreeDiagram`) — zoom & pan, node avatar, đường nối cha/mẹ–con | ✅ Hoàn thành |
| **6.2** | Xuất PNG (`PngWriter` thuần `java.util.zip`) và In ấn (`javafx.print.PrinterJob`) | ✅ Hoàn thành |
| **6.3** | Nới validate năm sinh xuống `1000 <= birthYear <= Year.now()` | ✅ Hoàn thành |
| **6.4** | Custom exception riêng cho BR2 (`SameParentException`) và BR6 (`InvalidParentGenderException`) | ✅ Hoàn thành |
| **6.5** | Chuyển toàn bộ kiểm thử sang **JUnit 5** + `maven-surefire-plugin` (188 test mặc định + 30 test tag `javafx`) | ✅ Hoàn thành |
| **6.6** | Bộ dữ liệu mẫu & bảng test case trong `test-data/` (`LargeSampleDataTest`, 22 test) | ✅ Hoàn thành |
| **6.7** | **Cách ly dữ liệu test**: mọi lớp test dùng database riêng `*_test`; `TestDatabaseManager` quản lý vòng đời + `assertSafeToDrop` | ✅ Hoàn thành |
| **6.8** | **Đồng bộ hóa toàn bộ sơ đồ PlantUML với code hiện tại**: class-diagram tách 4 sơ đồ theo gói, component/deployment/use-case cập nhật, thêm sequence + activity cho "Xuất PNG/In" và "Chọn & lưu ảnh đại diện" | ✅ Hoàn thành |

### Hạng mục chưa làm

| Hạng mục | Ghi chú |
|---|---|
| Kiểm thử tự động trải nghiệm người dùng cuối (chuột/bàn phím) | Bộ test xác nhận đúng luồng Controller–Service–Repository–MySQL, nhưng phần UX trên desktop cần kiểm thử thủ công qua `mvn javafx:run`. |
| Đóng gói bản phân phối (.exe / .msi) | Hiện chỉ chạy qua `mvn javafx:run`. |
| Chức năng xuất/nhập dữ liệu (JSON / CSV / GEDCOM) | Chưa triển khai. |
| Đăng nhập / phân quyền người dùng | Chưa triển khai. |
