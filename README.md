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
└── src/
    └── main/
        ├── java/
        │   ├── module-info.java
        │   └── com/giapha/
        │       ├── Main.java
        │       ├── model/          # Person, Gender
        │       ├── exception/      # Custom Exceptions
        │       ├── database/       # DatabaseConnection, DatabaseConnectionTest
        │       ├── repository/     # PersonRepository, JdbcPersonRepository
        │       ├── service/        # FamilyTreeService, FamilyTreeServiceTest
        │       ├── controller/     # PersonController
        │       ├── util/           # ImageManager (quản lý ảnh đại diện)
        │       └── view/           # JavaFX Views & Design System
        │                           # (PersonAvatar: component ảnh đại diện dùng chung)
        └── resources/
            ├── css/                # style.css
            ├── fxml/               # FXML layouts
            └── images/             # Icon & hình ảnh tĩnh
                                    # (Ảnh đại diện người dùng được copy vào ./images/ lúc runtime)
```

---

## 6. Quy Tắc Nghiệp Vụ (Business Rules)
1. **BR1 — Self Parent**: Một người không thể là cha hoặc mẹ của chính mình (`SelfParentException`).
2. **BR2 — Father != Mother**: Cha và mẹ của cùng một người không được là cùng một người (`IllegalArgumentException`).
3. **BR3 — No Cycle**: Cấm chu trình phả hệ; phát hiện qua DFS traversal (`CycleDetectedException`).
4. **BR4 — Person In Use**: Không cho phép xóa người đang là cha/mẹ của người khác (`PersonInUseException`).
5. **BR5 — Parent Birth Year**: Cảnh báo nếu năm sinh cha/mẹ lớn hơn hoặc bằng năm sinh con (Warning only).
6. **BR6 — Gender Role**: Cha bắt buộc là `Gender.MALE`, Mẹ bắt buộc là `Gender.FEMALE` (`IllegalArgumentException`).

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

---

## 9. Hướng Dẫn Kiểm Thử (Testing)

### 1. Kiểm tra kết nối CSDL:
```bash
mvn exec:java "-Dexec.mainClass=com.giapha.database.DatabaseConnectionTest"
```

### 2. Kiểm thử nghiệp vụ Service Layer (22 Test Cases):
```bash
mvn exec:java "-Dexec.mainClass=com.giapha.service.FamilyTreeServiceTest"
```
Bộ test bao gồm:
- Tra cứu thành viên theo ID tồn tại & không tồn tại (`PersonNotFoundException`).
- Gán cha/mẹ hợp lệ và kiểm tra giới tính vai trò (`IllegalArgumentException`).
- Kiểm tra tự gán bản thân (`SelfParentException`).
- Phát hiện và ngăn chặn chu trình gia phả (`CycleDetectedException`).
- Ngăn chặn xóa cha/mẹ đang được tham chiếu (`PersonInUseException`).
- Gỡ quan hệ cha/mẹ (`clearFather`, `clearMother`) và cho phép xóa sau khi gỡ.
- Kiểm tra hợp lệ năm sinh (`InvalidBirthYearException`).
- Truy vết tổ tiên đa tầng (`getAncestors`) và hậu duệ (`getDescendants`).
- Tự động hoàn trả và bảo toàn tuyệt đối 7 bản ghi mẫu (`COUNT(*) = 7`).

### 3. Kiểm thử Controller Layer (8 Test Cases):
```bash
mvn exec:java "-Dexec.mainClass=com.giapha.controller.PersonControllerTest"
```
Bộ test bao gồm:
- Tìm kiếm thành viên theo từ khóa (`handleSearch`).
- Xem chi tiết thành viên (`handleViewDetail`).
- Xem cây gia phả (`handleViewTree`).
- Thêm mới thành viên vào cơ sở dữ liệu (`handleAdd`).
- Chặn xóa người đang làm cha/mẹ theo quy tắc BR4 (`handleDelete`).
- Cập nhật thông tin và điều chỉnh cha/mẹ qua quy trình 6 bước (`handleEdit`).
- Gỡ quan hệ cha/mẹ khi chọn "Không có" qua form (`TC-CLEAR-06`).
- Tự động hoàn trả và xác minh trạng thái toàn vẹn của dữ liệu mẫu (`COUNT(*) = 7`).

### 4. Kiểm thử View Layer — Native In-Process JavaFX Test (9 Test Cases):
```bash
mvn exec:java "-Dexec.mainClass=com.giapha.view.JavaFxViewTest"
```
> ⚠️ **Điều kiện tiên quyết**: MySQL phải đang chạy và bảng `PERSON` phải chứa đúng **7 bản ghi mẫu** trước khi chạy. Test sẽ tự block nếu không đủ điều kiện.

Bộ test kiểm thử nội quá trình (in-process), không dùng TestFX/JUnit, bao gồm:
- TEST 1: `PeopleListView` nạp danh sách 7 thành viên từ MySQL vào `TableView`.
- TEST 2: Tìm kiếm client-side (`searchField`) và phục hồi danh sách đầy đủ.
- TEST 3: `PersonFormView` thêm thành viên mới hợp lệ (với cha/mẹ) vào MySQL.
- TEST 4: Validation từ chối dữ liệu không hợp lệ (tên rỗng, năm sinh sai số).
- TEST 5: `PersonFormView` sửa tên và gỡ quan hệ cha/mẹ (set NULL) qua form.
- TEST 6a: Chặn xóa người đang làm cha/mẹ theo BR4 (`PersonInUseException`).
- TEST 6b: Xóa thành viên thử nghiệm đã gỡ quan hệ thành công.
- TEST 7a: `PersonDetailView` hiển thị đúng tên cha/mẹ.
- TEST 7b: `FamilyTreeView` dựng cây gia phả đa thế hệ thành công.
- Cleanup tự động: Xóa toàn bộ bản ghi test, bảo toàn nguyên vẹn 7 bản ghi mẫu.

> **Lưu ý**: Đây là kiểm thử JavaFX event/in-process sử dụng `Platform.runLater()` và `CountDownLatch`. Bộ test xác nhận luồng Controller–Service–Repository–MySQL đúng đắn, nhưng **không thay thế kiểm thử thủ công trên desktop** để xác nhận trải nghiệm người dùng cuối, hiển thị ảnh đại diện và tương tác chuột/bàn phím thực tế.

### 5. Kiểm thử PersonAvatar — Ảnh đại diện & Fallback (16 Test Cases):
```bash
mvn exec:java "-Dexec.mainClass=com.giapha.view.AvatarFallbackTest"
```
Bộ test kiểm tra component `PersonAvatar` (ảnh đại diện dùng chung cho danh sách / chi tiết / cây gia phả / form):
- TC-AV-01: ảnh hợp lệ → hiển thị ảnh thật (có `ImageView` + clip bo tròn).
- TC-AV-02/03/07: file ảnh **không tồn tại**, file ảnh **hỏng**, file **không phải ảnh** → fallback về placeholder (chữ cái đầu), **không ném exception**.
- TC-AV-04/05/06: không có đường dẫn ảnh / đường dẫn rỗng / `person = null` → fallback placeholder.
- TC-AV-08/09: constructor với file cụ thể (dùng cho xem trước ảnh trong form) → hiển thị ảnh thật / fallback.
- TC-AV-10/11: kiểm tra kích thước avatar và `resolveImageFile()` xử lý đường dẫn sai / null.

> **Lưu ý kỹ thuật**: ảnh được nạp ở chế độ background nên test **không** sleep trên JavaFX Application Thread (sẽ chặn nạp ảnh), và **không** gọi `Platform.exit()` trước khi ảnh nạp xong.

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
   - Bắt toàn bộ ngoại lệ nghiệp vụ (`SelfParentException`, `CycleDetectedException`, `PersonInUseException`, `InvalidBirthYearException`, `PersonNotFoundException`, `IllegalArgumentException`, `SQLException`…) và hiển thị bằng `Alert` tiếng Việt — không văng stack trace ra giao diện.
   - **BR5 (cảnh báo năm sinh)** được kiểm tra ở tầng Service qua `FamilyTreeService.checkBirthYearWarning(child, father, mother)`; Controller hiển thị `Alert` cảnh báo với hai lựa chọn **Tiếp tục** / **Hủy**.
   - **Ảnh đại diện**: nút *Chọn ảnh...* dùng `FileChooser` (chỉ nhận `png` / `jpg` / `jpeg`); ảnh được copy vào thư mục `images/` cạnh ứng dụng và lưu **đường dẫn tương đối** (`images/person_<id>.<đuôi>`) vào DB. Tiện ích nằm ở `com.giapha.util.ImageManager`.
- [x] **Cụm 5.6**: Tích hợp toàn diện JavaFX UI với Backend MySQL (kết nối trực tiếp, loại bỏ hoàn toàn SampleData khỏi luồng chạy chính).
- [x] **Cụm 5.7**: End-to-End Verification, QA tích hợp và kiểm chứng toàn vẹn dữ liệu MySQL.
- [x] **Cụm 5.8**: Đồng bộ hóa tài liệu (README) và sơ đồ lớp (Class Diagram).
- [x] **Cụm 5.9**: Native In-Process JavaFX Test Harness (`JavaFxViewTest`) — 9/9 test cases PASS, bảo toàn 7 bản ghi mẫu.
- [x] **Cụm 5.10**: Hiển thị ảnh đại diện thật trên toàn bộ giao diện — component dùng chung `com.giapha.view.PersonAvatar` (ảnh bo tròn, tự fallback về chữ cái đầu khi thiếu ảnh / file ảnh hỏng / sai định dạng, **không ném exception ra UI**), tích hợp vào `PeopleListView` (cột Ảnh), `PersonDetailView` (avatar + thumbnail), `FamilyTreeView` (avatar trên mỗi node) và `PersonFormView` (xem trước). Bộ test `AvatarFallbackTest` — 16/16 PASS.
