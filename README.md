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
        │       └── view/           # JavaFX Views & Design System
        └── resources/
            ├── css/                # style.css
            ├── fxml/               # FXML layouts
            └── images/             # Icon & hình ảnh
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

### 2. Kiểm thử nghiệp vụ Service Layer (12 Test Cases):
```bash
mvn exec:java "-Dexec.mainClass=com.giapha.service.FamilyTreeServiceTest"
```
Bộ test bao gồm:
- Tìm kiếm theo ID tồn tại & không tồn tại.
- Gán cha/mẹ hợp lệ và sai giới tính.
- Kiểm tra tự gán bản thân (SelfParentException).
- Kiểm tra phát hiện chu trình (CycleDetectedException).
- Kiểm tra ràng buộc xóa (PersonInUseException).
- Truy vết tổ tiên đa tầng (Ancestors) và hậu duệ (Descendants).
- Tự động hoàn trả và xác minh trạng thái toàn vẹn của dữ liệu mẫu (`COUNT(*) = 7`).

---

## 10. Tài Liệu UML (UML Documentation)
Tất cả sơ đồ nằm tại thư mục `docs/uml/`:
- Sơ đồ lớp: `class-diagram.puml` / `class-diagram.png`
- Sơ đồ thành phần: `component-diagram.puml` / `component-diagram.png`
- Sơ đồ triển khai: `deployment-diagram.puml` / `deployment-diagram.png`
- Sơ đồ ca sử dụng: `use-case.puml` / `use-case.png`
- Sơ đồ tuần tự & hoạt động cho các ca sử dụng: Thêm người, Gán cha mẹ, Xem cây gia phả.

---

## 11. Trạng Thái Phát Triển (Development Status)
- [x] **Cụm 1–4**: Thiết kế UML, kiến trúc, UI Mockup & Design System JavaFX.
- [x] **Cụm 5.1–5.2**: Model Layer, Custom Exceptions & Repository Layer (Interface & JDBC Implementation).
- [x] **Cụm 5.3**: Database Schema, MySQL setup, DatabaseConnection.
- [x] **Cụm 5.4**: FamilyTreeService Layer & Bộ test nghiệp vụ (12/12 test cases PASS).
- [ ] **Cụm 5.5**: PersonController & Tích hợp kết nối UI Controller.
