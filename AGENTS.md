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
5. **Database Connection (`com.giapha.database`)**:
   - `DatabaseConnection`: Utility class cung cấp Connection mới mỗi khi gọi `getConnection()`. Không dùng static long-lived connection.

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
│   ├── PersonInUseException.java      # Ném khi xóa person đang là cha/mẹ người khác (BR4)
│   ├── PersonNotFoundException.java   # Ném khi không tìm thấy Person theo ID
│   └── SelfParentException.java       # Ném khi tự gán làm cha/mẹ chính mình (BR1)
├── repository/
│   ├── PersonRepository.java          # Interface hợp đồng truy xuất dữ liệu
│   └── JdbcPersonRepository.java      # Cài đặt JDBC CRUD
├── database/
│   ├── DatabaseConnection.java        # Factory lấy JDBC Connection
│   └── DatabaseConnectionTest.java    # Kiểm tra kết nối CSDL thực tế
├── service/
│   ├── FamilyTreeService.java         # Business Service Layer
│   └── FamilyTreeServiceTest.java     # Kiểm thử nghiệp vụ & cô lập dữ liệu
└── view/
    ├── FamilyTreeView.java            # Màn hình cây phả hệ trực quan
    ├── PeopleListView.java            # Màn hình danh sách thành viên
    ├── PersonDetailView.java          # Màn hình chi tiết hồ sơ cá nhân
    ├── PersonFormView.java            # Form thêm / sửa thành viên
    └── SampleData.java                # Dữ liệu mẫu phục vụ kiểm thử UI độc lập
```

---

## 5. QUY TẮC NGHIỆP VỤ (BUSINESS RULES BR1–BR6)

- **BR1 — SELF PARENT**: Một người không thể là cha hoặc mẹ của chính mình (`childId == parentId`).
  - *Xử lý*: Ném `SelfParentException`.
- **BR2 — FATHER != MOTHER**: Cha và mẹ của cùng một người không được là cùng một Person (`father.id == mother.id`).
  - *Xử lý*: Từ chối bằng `IllegalArgumentException`.
- **BR3 — NO CYCLE**: Không được tạo chu trình vòng lặp trong gia phả (ví dụ: A là cha B, B là cha C thì C không thể là cha/mẹ của A).
  - *Xử lý*: Kiểm tra bằng thuật toán duyệt đồ thị (DFS traversal) từ `candidateParentId` ngược lên các thế hệ tổ tiên với `Set<Integer> visited` chống lặp. Nếu gặp `childId`, ném `CycleDetectedException`.
- **BR4 — PERSON IN USE**: Không được xóa một `Person` nếu người đó đang được tham chiếu làm `father` hoặc `mother` của bất kỳ ai trong hệ thống.
  - *Xử lý*: Ném `PersonInUseException`. Không cascade delete, không tự ý set NULL quan hệ con cái.
- **BR5 — PARENT BIRTH YEAR**: Năm sinh của cha/mẹ nên nhỏ hơn năm sinh của con.
  - *Xử lý*: Đây là quy tắc cảnh báo (WARNING), không phải hard validation, không ném exception làm gián đoạn lưu trữ.
- **BR6 — GENDER ROLE**:
  - `father` bắt buộc phải có giới tính `Gender.MALE`.
  - `mother` bắt buộc phải có giới tính `Gender.FEMALE`.
  - *Xử lý*: Sai giới tính ném `IllegalArgumentException`.

---

## 6. QUY TẮC VALIDATION DỮ LIỆU (`validatePerson`)

Mỗi khi tạo mới (`addPerson`) hoặc cập nhật (`updatePerson`), đối tượng `Person` phải vượt qua các điều kiện:
1. `person != null`
2. `fullName != null`, `fullName.trim()` không rỗng, độ dài từ 1 đến 100 ký tự.
3. `birthYear > 1900` và `birthYear <= Year.now().getValue()` (nếu vi phạm ném `InvalidBirthYearException`).
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
  - CHECK `chk_birth_year`: `birth_year > 1900 AND birth_year <= 2100`
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

### Kiểm tra kết nối cơ sở dữ liệu:
```bash
mvn exec:java "-Dexec.mainClass=com.giapha.database.DatabaseConnectionTest"
```

### Chạy bộ kiểm thử Service Layer (12 test cases):
```bash
mvn exec:java "-Dexec.mainClass=com.giapha.service.FamilyTreeServiceTest"
```

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
