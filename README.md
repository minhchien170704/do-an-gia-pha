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

### 4.3. Chạy toàn bộ kiểm thử (121 test cases)

```bash
mvn clean test
```

| Lớp kiểm thử | Số test | Phạm vi |
|---|---|---|
| `FamilyTreeServiceTest` | 44 | BR1–BR6, TC01–TC14, TC-CLEAR-01…05, cô lập dữ liệu |
| `PersonControllerTest` | 25 | Điều phối View ↔ Service, xử lý exception, callback điều hướng |
| `LargeSampleDataTest` | 22 | Bộ dữ liệu mẫu 30 người / 5 thế hệ (BR1–BR6, validation, ràng buộc CSDL, an toàn DROP DB) |
| `DatabaseConnectionTest` | 7 | Kết nối MySQL, metadata, bảng `PERSON`, kiểm chứng DB chính không bị đổi |
| `ImageManagerTest` | 12 | Định dạng ảnh, copy vào `./images/`, dọn file an toàn |
| `PngWriterTest` | 11 | Chữ ký PNG, khối IHDR/IDAT/IEND, CRC32, nén zlib |

**Điều kiện tiên quyết**: MySQL phải đang chạy. Các test dùng database **riêng** (tên kết thúc bằng
`_test`) được **tự động tạo ở `@BeforeAll`** và **tự động xóa ở `@AfterAll`** — database chính
`gia_pha` **không bao giờ** bị test ghi vào. Nếu CSDL không sẵn sàng, các test đó được đánh dấu
`SKIPPED` (không tính là `FAILED`).

### 4.4. Chạy riêng các test cần JavaFX Toolkit (tag `javafx` — 26 test cases)

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

Nếu bạn cộng tay từng dòng log Surefire (25 + 7 + 44 + 0 + 22 + 12 + 11) thì ra **121**,
không phải 142. Con số 142 xuất hiện khi **báo cáo cũ** cộng thêm một dòng
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
[INFO] Tests run: 121, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Dòng tổng này **không** bao gồm 26 test tag `javafx` (chạy riêng qua `mvn test -Pjavafx-tests`).

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
DatabaseConnection (JDBC Factory)
       ↓
MySQL / MariaDB
```

### 6.2. Các hình mẫu thiết kế đã dùng

| Hình mẫu | Thể hiện ở đâu | Chi tiết |
|---|---|---|
| **Layered Architecture / MVC** | Toàn bộ package `com.giapha.*` | View → Controller → Service → Repository → Database. Mỗi tầng chỉ biết tầng ngay dưới nó. |
| **Separation of Concerns** | Tách `view` / `controller` / `service` / `repository` / `database` | View chỉ hiển thị & bắt sự kiện; Service chỉ chứa quy tắc nghiệp vụ; Repository chỉ lo truy cập dữ liệu. |
| **Dependency Inversion (SOLID)** | `FamilyTreeService` phụ thuộc `PersonRepository` (interface) | Service **không** import `JdbcPersonRepository`, `DatabaseConnection`, `Connection`, `PreparedStatement`, `ResultSet` hay bất kỳ JDBC API nào. Thay thế triển khai CSDL không cần sửa Service. |
| **Repository Pattern** | `PersonRepository` + `JdbcPersonRepository` | Ẩn hoàn toàn SQL sau abstraction `findById` / `findAll` / `search` / `save` / `update` / `delete`. |
| **Factory (JDBC Factory)** | `DatabaseConnection.getConnection()` | Trả về **Connection mới** mỗi lần gọi; không dùng static connection dài hạn. Caller đóng kết nối bằng try-with-resources. |
| **Strategy cấu hình** | `DatabaseConnection.getUrl/getUser/getPassword` | Thứ tự ưu tiên System property → Environment variable → mặc định. Không hard-code mật khẩu. |
| **Custom Exception** | `com.giapha.exception.*` | Mỗi BR có exception riêng → tầng Controller bắt và hiển thị thông báo tiếng Việt rõ ràng, không văng stack trace ra UI. |
| **Data Access Object mở rộng (test)** | `TestPersonRepository` (chỉ trong test) | Ghi đè `getConnection()` để trỏ database test — cô lập dữ liệu tuyệt đối. |

### 6.3. Ranh giới trách nhiệm

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
│   │   │       ├── database/                 # DatabaseConnection (JDBC Factory)
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

Tất cả sơ đồ nằm tại thư mục `docs/uml/`:

| Sơ đồ | File |
|---|---|
| Lớp | `class-diagram.puml` / `class-diagram.png` |
| Thành phần | `component-diagram.puml` / `component-diagram.png` |
| Triển khai | `deployment-diagram.puml` / `deployment-diagram.png` |
| Ca sử dụng | `use-case.puml` / `use-case.png` |
| Tuần tự | `sequence-add-person.puml`, `sequence-assign-parent.puml`, `sequence-family-tree.puml` |
| Hoạt động | `activity-add-person.puml`, `activity-assign-parent.puml`, `activity-family-tree.puml` |

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
| **6.5** | Chuyển toàn bộ kiểm thử sang **JUnit 5** + `maven-surefire-plugin` (121 test mặc định + 26 test tag `javafx`) | ✅ Hoàn thành |
| **6.6** | Bộ dữ liệu mẫu & bảng test case trong `test-data/` (`LargeSampleDataTest`, 22 test) | ✅ Hoàn thành |
| **6.7** | **Cách ly dữ liệu test**: mọi lớp test dùng database riêng `*_test`; `TestDatabaseManager` quản lý vòng đời + `assertSafeToDrop` | ✅ Hoàn thành |

### Hạng mục chưa làm

| Hạng mục | Ghi chú |
|---|---|
| Kiểm thử tự động trải nghiệm người dùng cuối (chuột/bàn phím) | Bộ test xác nhận đúng luồng Controller–Service–Repository–MySQL, nhưng phần UX trên desktop cần kiểm thử thủ công qua `mvn javafx:run`. |
| Đóng gói bản phân phối (.exe / .msi) | Hiện chỉ chạy qua `mvn javafx:run`. |
| Chức năng xuất/nhập dữ liệu (JSON / CSV / GEDCOM) | Chưa triển khai. |
| Đăng nhập / phân quyền người dùng | Chưa triển khai. |
