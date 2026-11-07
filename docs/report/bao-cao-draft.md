# BÁO CÁO ĐỒ ÁN — HỆ THỐNG QUẢN LÝ SƠ ĐỒ GIA PHẢ

> **Bản nháp (draft)** — dùng để nộp và trình bày. Các chỗ đánh dấu `[ẢNH: ...]`
> cần chụp ảnh màn hình thực tế và chèn vào. Các thông tin kỹ thuật trong báo cáo
> này được đối chiếu trực tiếp với mã nguồn trong `src/main/java` và với các
> sơ đồ PlantUML trong `docs/uml/`.

| Mục | Nội dung |
|---|---|
| **Tên đồ án** | Hệ thống Quản lý Sơ đồ Gia phả (Family Tree Management) |
| **Môn học** | Lập trình hướng đối tượng (OOP) |
| **Ngôn ngữ / nền tảng** | Java 25, JavaFX 23, MySQL/MariaDB, JDBC thuần, Maven |
| **Mô hình kiến trúc** | Kiến trúc phân tầng (Layered Architecture) + MVC |
| **Module hệ thống** | JPMS — module `com.giapha` (`module-info.java`) |
| **Kiểm thử** | JUnit 5 — 201 test mặc định + 30 test tag `javafx` |
| **Mã nguồn** | <https://github.com/minhchien170704/do-an-gia-pha> |

---

## MỤC LỤC

1. [Phát biểu và mô tả bài toán](#1-phát-biểu-và-mô-tả-bài-toán)
2. [Phân tích](#2-phân-tích)
3. [Thiết kế](#3-thiết-kế)
4. [Mô tả cài đặt và hướng dẫn sử dụng](#4-mô-tả-cài-đặt-và-hướng-dẫn-sử-dụng)
5. [Kiểm thử](#5-kiểm-thử)
6. [Kết luận](#6-kết-luận)
7. [Tài liệu tham khảo](#7-tài-liệu-tham-khảo)
8. [Phụ lục — Danh mục sơ đồ UML](#phụ-lục--danh-mục-sơ-đồ-uml)

---

## 1. PHÁT BIỂU VÀ MÔ TẢ BÀI TOÁN

### 1.1. Bối cảnh

Gia phả là dữ liệu mang ý nghĩa bản sắc, thường được lưu giữ trên giấy hoặc sổ
gia đình. Khi số lượng thành viên và số thế hệ tăng lên, việc cập nhật thủ công
trở nên khó khăn: dễ sai sót khi sửa quan hệ cha–mẹ, không kiểm soát được
tình huống một người bị gán làm cha của chính mình hoặc của hậu duệ của mình
(tạo chu trình vô lý), và không có cách nào "nhìn" toàn bộ dòng họ trong một
lần quét mắt.

### 1.2. Phát biểu bài toán

Xây dựng một ứng dụng desktop **Quản lý Sơ đồ Gia phả** cho phép:

1. **Nhập và quản lý** thông tin từng thành viên trong gia phả: họ và tên đầy đủ,
   ảnh đại diện, năm sinh, giới tính, và quan hệ cha–mẹ.
2. **Quản lý quan hệ phả hệ**: gán / gỡ cha, gán / gỡ mẹ cho từng thành viên.
3. **Bảo đảm tính toàn vẹn dữ liệu phả hệ** bằng các quy tắc nghiệp vụ rõ ràng
   (BR1–BR6) được kiểm tra ở cả tầng nghiệp vụ lẫn tầng cơ sở dữ liệu.
4. **Tra cứu** danh sách thành viên, tìm kiếm theo họ tên, xem chi tiết một hồ sơ.
5. **Hiển thị cây gia phả trực quan** dưới dạng sơ đồ 2D, có thể phóng to / thu nhỏ
   / kéo di chuyển.
6. **In ra cây gia phả** — xuất sơ đồ ra file ảnh PNG và in trực tiếp qua máy in.

### 1.3. Đối tượng và phạm vi

| Khái niệm | Mô tả |
|---|---|
| **Người dùng (User)** | Người sử dụng ứng dụng trên máy tính cá nhân (thành viên gia đình, người quản lý dòng họ). Không phân biệt vai trò, không có đăng nhập. |
| **Thành viên (Person)** | Một cá nhân trong gia phả, có họ tên, năm sinh, giới tính, ảnh đại diện (tùy chọn), và tham chiếu tới cha (father) và mẹ (mother). |
| **Quan hệ phả hệ** | Quan hệ trực hệ `cha → con` và `mẹ → con`. Mỗi thành viên có tối đa 1 cha và 1 mẹ; một cha/mẹ có thể có nhiều con. |
| **Cây gia phả / Sơ đồ 2D** | Cấu trúc đồ thị phả hệ được vẽ ra bằng đồ họa, gồm các node (avatar + họ tên + năm sinh + giới tính) và các đường nối cha/mẹ–con. |
| **Phạm vi** | Ứng dụng desktop chạy cục bộ, dữ liệu lưu trong MySQL/MariaDB cục bộ. **Không** bao gồm: đăng nhập/phân quyền, nhập–xuất theo chuẩn GEDCOM/CSV, đóng gói installer, chạy nhiều người dùng đồng thời. |

### 1.4. Yêu cầu chức năng

| Mã | Yêu cầu | Nơi thực hiện |
|---|---|---|
| UC01 | Thêm người mới (kèm ảnh đại diện, năm sinh, giới tính, cha/mẹ tùy chọn) | `PersonController.handleAdd()`, `FamilyTreeService.addPerson()` |
| UC02 | Sửa thông tin người (họ tên, năm sinh, giới tính, ảnh, gán lại cha/mẹ) | `PersonController.handleEdit()` |
| UC03 | Xóa người (chặn nếu đang là cha/mẹ của người khác) | `PersonController.handleDelete()`, `FamilyTreeService.deletePerson()` |
| UC04 | Tìm kiếm người theo họ tên | `PeopleListView` (`FilteredList` phía client) + `FamilyTreeService.searchPersons()` |
| UC05 | Xem danh sách toàn bộ thành viên kèm thống kê | `PeopleListView` |
| UC06 | Xem chi tiết hồ sơ một thành viên (kèm cha, mẹ, các con) | `PersonDetailView` |
| UC07 | Gán cha | `FamilyTreeService.assignFather()` |
| UC08 | Gán mẹ | `FamilyTreeService.assignMother()` |
| UC09 | Gỡ quan hệ cha/mẹ | `FamilyTreeService.clearFather()` / `clearMother()` |
| UC10 | Xem cây gia phả dạng sơ đồ 2D | `FamilyTreeDiagram` |
| UC11 | Tra cứu tổ tiên (nhiều thế hệ) | `FamilyTreeService.getAncestors()` |
| UC12 | Tra cứu hậu duệ (nhiều thế hệ) | `FamilyTreeService.getDescendants()` |
| UC13 | Chọn ảnh đại diện (png/jpg/jpeg) | `PersonFormView.handleChoosePhoto()`, `ImageManager` |
| UC14 | Xuất sơ đồ ra file PNG | `FamilyTreeView.handleExportPng()`, `PngWriter` |
| UC15 | In sơ đồ cây gia phả | `FamilyTreeView.handlePrint()` |

*(Xem sơ đồ ca sử dụng đầy đủ: `docs/uml/use-case.puml` → `docs/uml/use-case.png`)*

### 1.5. Yêu cầu phi chức năng

| Yêu cầu | Mô tả cụ thể trong hệ thống |
|---|---|
| **Tính toàn vẹn dữ liệu** | 6 quy tắc nghiệp vụ BR1–BR6 được kiểm ở tầng Service; bổ sung `CHECK` constraint và `TRIGGER` ở tầng CSDL. |
| **Phân tách trách nhiệm** | View không gọi Service/Repository; Service không import bất kỳ JDBC API nào; Controller không viết SQL. |
| **Không phụ thuộc framework** | Chỉ dùng JDK + JavaFX + JDBC thuần; không Spring/Hibernate/JPA/Lombok. |
| **Khả năng kiểm thử** | Mọi lớp test ghi vào CSDL dùng database riêng tên kết thúc bằng `_test`, tự tạo ở `@BeforeAll` và tự xóa ở `@AfterAll`. |
| **Thông báo lỗi thân thiện** | Mọi lỗi nghiệp vụ được ánh xạ thành `Alert` tiếng Việt; không có exception nào lọt ra giao diện. |
| **Tương thích module** | Module `com.giapha` chỉ `requires javafx.controls`, `javafx.graphics`, `java.sql`. |

### 1.6. Quy tắc nghiệp vụ BR1–BR6

| Mã | Tên | Nội dung | Xử lý khi vi phạm |
|---|---|---|---|
| **BR1** | Self Parent | Một người không thể là cha hoặc mẹ của chính mình | `SelfParentException` |
| **BR2** | Father ≠ Mother | Cha và mẹ của cùng một người không được là cùng một người | `SameParentException` |
| **BR3** | No Cycle | Không được tạo chu trình vòng lặp trong gia phả | `CycleDetectedException` |
| **BR4** | Person In Use | Không xóa được `Person` đang được tham chiếu làm cha/mẹ của người khác | `PersonInUseException` |
| **BR5** | Parent Birth Year | Năm sinh của cha/mẹ nên nhỏ hơn năm sinh của con | **Cảnh báo** (không chặn) — `FamilyTreeService.checkBirthYearWarning(...)` |
| **BR6** | Gender Role | `father` bắt buộc là `Gender.MALE`, `mother` bắt buộc là `Gender.FEMALE` | `InvalidParentGenderException` |

**Thứ tự ưu tiên kiểm tra** trong `assignFather()` / `assignMother()` — quyết định
để thông báo đúng nguyên nhân, không phải ngẫu nhiên:

```
BR1 (SelfParent) → BR2 (SameParent) → BR6 (InvalidParentGender) → BR3 (Cycle)
```

**Ràng buộc validation dữ liệu** (`FamilyTreeService.validatePerson()`):
họ tên không rỗng và dài 1–100 ký tự; `MIN_BIRTH_YEAR (1000) <= birthYear <= Year.now().getValue()`;
giới tính khác null; `photoPath` tùy chọn.

---

## 2. PHÂN TÍCH

### 2.1. Sơ đồ ca sử dụng (Use Case Diagram)

**File:** `docs/uml/use-case.puml` → ảnh `docs/uml/use-case.png`

Sơ đồ mô tả 15 ca sử dụng thuộc 5 nhóm, người dùng là actor duy nhất ("Người dùng"),
tương tác với hệ thống qua biên "Hệ thống quản lý gia phả".

**Nhóm "Quản lý thành viên"** (UC01–UC06): thêm, sửa, xóa, tìm kiếm, xem danh sách,
xem chi tiết. Đây là nhóm CRUD cơ bản, mỗi ca sử dụng tương ứng một phương thức
public của `PersonController`.

**Nhóm "Quan hệ phả hệ"** (UC07–UC09): gán cha, gán mẹ, gỡ quan hệ. Đây là nhóm
đặc thù của bài toán — thao tác gán quan hệ phải qua kiểm tra BR1–BR6.

**Nhóm "Xem cây gia phả"** (UC10–UC12): xem sơ đồ 2D, xem tổ tiên, xem hậu duệ.
UC10 `<<include>>` UC11 và UC12 — xem sơ đồ 2D **bắt buộc** phải nạp cả tổ tiên
lẫn hậu duệ, vì sơ đồ được bố trí theo thế hệ (tổ tiên ở trên, hậu duệ ở dưới).

**Nhóm "Ảnh đại diện"** (UC13): UC01 và UC02 `<<include>>` UC13 — chọn ảnh là
bước tùy chọn trong luồng thêm/sửa, không phải ca sử dụng độc lập.

**Nhóm "Xuất & In ấn"** (UC14, UC15): cả hai `<<include>>` UC10 — xuất PNG và in
đều cần một bản chụp sơ đồ 2D trước.

**Các quan hệ `<<extend>>`**: UC02 (sửa thông tin) `<<extend>>` UC07/UC08/UC09 —
tức là khi sửa hồ sơ mà người dùng đổi cha/mẹ thì luồng gán/gỡ quan hệ được
kích hoạt. Điều này khớp với cách `PersonController.handleEdit(Person, Person, Person)`
gọi `FamilyTreeService.updatePersonWithParents(...)` — hàm này **kiểm tra toàn bộ**
thông tin cá nhân + cha + mẹ **trước**, rồi ghi bằng **một** câu `UPDATE` duy nhất, nhờ đó
không bao giờ xảy ra trạng thái "cập nhật một nửa" (cha đã đổi nhưng mẹ vẫn giữ nguyên).

> **Điểm cần lưu ý khi trình bày:** sơ đồ không vẽ các lớp test, và không mô tả
> tính năng đăng nhập / xuất GEDCOM (chưa triển khai).

### 2.2. Sơ đồ lớp (Class Diagram)

Do hệ thống có 25 lớp + 7 lớp exception, sơ đồ lớp được **tách thành 4 file**
để ảnh render không bị quá rộng:

| File | Phạm vi |
|---|---|
| `docs/uml/class-diagram.puml` | Tổng quan: chỉ tên lớp + quan hệ giữa các gói |
| `docs/uml/class-diagram-model-exception.puml` | `Person`, `Gender` + 7 lớp exception |
| `docs/uml/class-diagram-service-repository.puml` | `FamilyTreeService`, `PersonRepository`, `JdbcPersonRepository`, `DatabaseConnection` |
| `docs/uml/class-diagram-view-controller.puml` | 6 lớp `view` + `PersonController` (kèm quan hệ Observer) |

**Model.** `Person` là entity trung tâm, chứa các thuộc tính `id`, `fullName`,
`birthYear`, `gender`, `photoPath`, `father`, `mother`. Quan hệ với cha/mẹ được
biểu diễn bằng **hai tham chiếu đối tượng** (`Person father`, `Person mother`) —
tức là quan hệ trực hệ được mô hình hóa trực tiếp trong đối tượng, không cần
bảng trung gian. `Gender` là enum chỉ có 2 giá trị `MALE` / `FEMALE`, khớp trực tiếp
với kiểu `ENUM('MALE','FEMALE')` ở tầng CSDL.

**Exception.** Cả 7 lớp exception đều kế thừa trực tiếp `RuntimeException`
(`SelfParentException`, `SameParentException`, `InvalidParentGenderException`,
`CycleDetectedException`, `PersonInUseException`, `PersonNotFoundException`,
`InvalidBirthYearException`). Mỗi lớp là **một thông điệp có kiểu** cho một quy tắc
nghiệp vụ: tầng Service **ném** (không trả mã lỗi), tầng Controller **bắt** theo
kiểu và ánh xạ sang thông báo tiếng Việt qua `showErrorAlert(...)`. Nhờ vậy stack
trace của JDBC/SQL không bao giờ lọt ra giao diện.

**Service.** `FamilyTreeService` giữ **một** trường `private final PersonRepository
personRepository` và **không** import `JdbcPersonRepository`, `DatabaseConnection` hay
bất kỳ `java.sql.*` nào. Đây là bằng chứng trực tiếp cho nguyên tắc Dependency
Inversion (SOLID – D).

**Repository.** `PersonRepository` là interface khai báo 6 hợp đồng:
`findById(int)`, `findAll()`, `search(String)`, `save(Person)`, `update(Person)`,
`delete(int)`. `JdbcPersonRepository implements PersonRepository` là cài đặt JDBC
thuần; toàn bộ câu `SELECT / INSERT / UPDATE / DELETE` nằm trong class này.

**View + Controller — Dependency Inversion (SOLID – D).** `PersonController`
**không import bất kỳ lớp nào** trong `com.giapha.view.*`. Thay vào đó, Controller
tự định nghĩa **3 abstraction** trong chính gói `com.giapha.controller`
(*consumer-owned*), mỗi abstraction chỉ dùng kiểu dữ liệu thuần Java
(`String`, `Gender`, `Person`) — không truyền JavaFX `Node`/`Control`:

| Abstraction | Trách nhiệm | Implementor thật |
|---|---|---|
| `FormDataSource` | Cung cấp dữ liệu form (họ tên, năm sinh, giới tính, cha, mẹ, ảnh nguồn, ảnh hiện có, người đang sửa) | `PersonFormView` |
| `PeopleListSource` | Cung cấp dòng đang chọn + `refresh()` | `PeopleListView` |
| `Navigator` | 3 hành vi điều hướng màn hình | Nối tại composition root (`Main`) |

6 lớp View đều nhận `PersonController` qua constructor — không View nào tự khởi
tạo tầng nghiệp vụ.

**Lợi ích trực tiếp:** nhờ 3 abstraction này, tầng kiểm thử có thể unit-test
`PersonController` bằng **fake implementation** (`PersonControllerAbstractionTest` —
23 test) **mà không cần khởi tạo JavaFX Toolkit**. Đây là ứng dụng trực tiếp của
nguyên tắc Dependency Inversion: cả hai phía (View và Controller) đều phụ thuộc
vào abstraction do Consumer sở hữu, không phụ thuộc vào nhau theo chiều cụ thể.

**Quan hệ Observer.** `FamilyTreeDiagram` giữ `zoomProperty`
(`ReadOnlyDoubleWrapper`) và expose qua `contentScaleProperty()`; `FamilyTreeView`
đăng ký listener để cập nhật nhãn phần trăm zoom — sơ đồ không cần biết đến toolbar.

### 2.3. Sơ đồ hoạt động (Activity Diagram)

| File | Quy trình |
|---|---|
| `docs/uml/activity-add-person.puml` | Thêm thành viên mới |
| `docs/uml/activity-assign-parent.puml` | Gán cha/mẹ |
| `docs/uml/activity-family-tree.puml` | Xem cây gia phả |
| `docs/uml/activity-export-print.puml` | Xuất PNG / In ấn |
| `docs/uml/activity-photo-avatar.puml` | Chọn & lưu ảnh đại diện |

**Activity "Thêm người"** (`activity-add-person.puml`) thể hiện đúng thứ tự kiểm tra
mà code thực thi trong `FamilyTreeService.addPerson()`:
validate thông tin cơ bản → kiểm tra BR2 (cha == mẹ) → kiểm tra BR6 cho cha
(nếu có) → kiểm tra BR6 cho mẹ (nếu có) → kiểm tra BR5 (cảnh báo năm sinh, có
lựa chọn Tiếp tục / Hủy) → lưu ảnh đại diện nếu có → ghi vào CSDL.

Điểm đáng chú ý là **BR5 được xử lý như một quy tắc cảnh báo**: sơ đồ vẽ rõ
nhánh "Tiếp tục" và "Hủy", chỉ khi người dùng chọn "Tiếp tục" thì luồng mới đi
tiếp tới bước lưu. Đây là điểm khác biệt so với 5 quy tắc còn lại vốn chặn
thao tác bằng exception.

**Activity "Gán cha/mẹ"** (`activity-assign-parent.puml`) mô tả cùng một thứ tự
ưu tiên BR1 → BR2 → BR6 → BR3, khớp với `assignFather()` / `assignMother()`.

**Activity "Xem cây gia phả"** (`activity-family-tree.puml`) mô tả 4 bước bố trí sơ đồ:
`buildModel()` → `buildUnits()` → `layoutAll()` → `drawEdges()` + `createNodeCard()`.

**Activity "Xuất PNG / In"** (`activity-export-print.puml`) tách rõ hai nhánh:
mỗi nhánh đều kiểm tra "sơ đồ có dữ liệu?" trước, và kết thúc bằng `Alert`
báo thành công / thất bại — không nhánh nào ném exception ra UI.

### 2.4. Sơ đồ tuần tự (Sequence Diagram)

| File | Kịch bản |
|---|---|
| `docs/uml/sequence-add-person.puml` | Thêm người: Form → Controller → Service → Repository → JDBC → MySQL, rồi vòng lại copy ảnh |
| `docs/uml/sequence-edit-person.puml` | Sửa người: kiểm tra người tồn tại + BR1/BR2/BR6/BR3 + BR5 **trước**, rồi ghi bằng **một** `UPDATE`; ảnh chỉ được copy **sau khi** service thành công |
| `docs/uml/sequence-assign-parent.puml` | Gán cha/mẹ với 4 nhánh exception BR1/BR2/BR6/BR3 |
| `docs/uml/sequence-family-tree.puml` | Xem cây: BFS ngược tổ tiên + tải toàn bộ danh sách để dựng đồ thị |
| `docs/uml/sequence-export-print.puml` | Xuất PNG (`PngWriter`) và In (`PrinterJob`) |
| `docs/uml/sequence-photo-avatar.puml` | Chọn ảnh → copy vào `images/` → lưu đường dẫn tương đối |

**`sequence-add-person.puml`** là sơ đồ trung tâm: nó thể hiện đúng luồng
View → Controller → Service → Repository → JDBC → DB, rồi quay lại Controller để
copy ảnh đại diện **sau khi** đã có `id` do CSDL sinh.

**`sequence-assign-parent.puml`** dùng khối `alt/else/end` chia 5 nhánh theo đúng
thứ tự ưu tiên, mỗi nhánh một loại exception riêng — thể hiện trực quan việc
Controller bắt exception theo kiểu chứ không bắt chung một `Exception`.

**`sequence-edit-person.puml`** mô tả luồng sửa: Controller yêu cầu Service kiểm tra
người cần sửa **tồn tại** (`getPersonById` → `PersonNotFoundException`), rồi kiểm tra
BR2 → BR1 → BR6 → BR3 cho **cả cha lẫn mẹ** và BR5 (cảnh báo)
**trước khi** ghi bất kỳ dữ liệu nào; nếu mọi kiểm tra đạt thì Service thực hiện
**một** câu `UPDATE` duy nhất. Nhánh lỗi trả về thông báo và **không** ghi gì —
loại bỏ hoàn toàn trạng thái "cập nhật một nửa". Ảnh đại diện chỉ được copy
**sau khi** Service đã thành công, nên khi Service từ chối thì **cả ảnh cũ trên
đĩa cũng không bị ghi đè**.

> **Lưu ý trung thực về từ "nguyên tử":** ở đây "nguyên tử" được hiểu theo nghĩa
> **kiểm tra toàn bộ trước, rồi ghi bằng MỘT câu `UPDATE`** — KHÔNG phải transaction
> nhiều bước (`BEGIN`/`COMMIT`) và KHÔNG phải khóa ghi. Bản thân bước "kiểm tra" và
> bước "ghi" **không khóa lẫn nhau**: giữa lúc kiểm tra và lúc `UPDATE` có thể có
> thay đổi từ người dùng khác. Điều này **chấp nhận được** vì ứng dụng này là
> desktop **một người dùng**, không có truy cập đồng thời. Nếu mở rộng thành
> nhiều người dùng cần thêm transaction + khóa tương ứng.

**`sequence-photo-avatar.puml`** thể hiện chi tiết quan trọng: ảnh được copy
**sau khi** có `id` (nút "Thêm mới"), tên file là `person_<id>.<đuôi>`, và CSDL
chỉ lưu **đường dẫn tương đối**.

### 2.5. Phân tích dữ liệu

Dữ liệu gia phả có tính chất **cây phân cấp với nhiều nhánh** — mỗi thành viên có
tối đa 1 cha và 1 mẹ, mỗi người có thể có nhiều con. Vì vậy cấu trúc lưu trữ
được chọn là:

- Một bảng `person` tự tham chiếu với hai khóa ngoại `father_id` và `mother_id`.
- Cả hai khóa ngoại dùng `ON DELETE RESTRICT` để CSDL chặn xóa người đang được
  tham chiếu — tức là BR4 được bảo vệ **hai lớp**: tầng Service (thông báo tiếng Việt
  thân thiện) và tầng CSDL (ràng buộc cứng).

---

## 3. THIẾT KẾ

### 3.1. Kiến trúc tổng thể

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

Luồng phụ thuộc là **đơn chiều**. Tầng trên chỉ gọi tầng ngay dưới nó.

| Tầng | Trách nhiệm | KHÔNG được làm |
|---|---|---|
| **View** (`com.giapha.view.*`) | Hiển thị, bắt sự kiện, gọi Controller | Không chứa SQL, không gọi Service/Repository trực tiếp |
| **Controller** (`com.giapha.controller.PersonController`) | Điều phối View ↔ Service, chuyển exception thành thông báo | Không viết SQL, không gọi Repository/JDBC |
| **Service** (`com.giapha.service.FamilyTreeService`) | Quy tắc nghiệp vụ BR1–BR6, validation, phát hiện chu trình | Không import bất kỳ JDBC API nào |
| **Repository** (`com.giapha.repository.*`) | CRUD bảng `person` bằng JDBC thuần | Không chứa quy tắc nghiệp vụ |
| **Database** (`com.giapha.database.DatabaseConnection`) | Cấp `Connection` mới | Không giữ connection dài hạn |
| **Util** (`com.giapha.util.*`) | Tiện ích tĩnh: ảnh đại diện, ghi PNG | Không chứa logic nghiệp vụ gia phả |

### 3.2. Sơ đồ thành phần (Component Diagram)

**File:** `docs/uml/component-diagram.puml` → ảnh `docs/uml/component-diagram.png`

Sơ đồ thành phần nhóm các lớp theo 5 gói: **Tầng Trình Bày** (JavaFX UI +
`PersonController`), **Tầng Nghiệp Vụ** (`FamilyTreeService`), **Tầng Lưu Trữ**
(`PersonRepository` interface + `JdbcPersonRepository` + `DatabaseConnection`),
**Tiện Ích** (`ImageManager`, `PngWriter`), **Cơ Sở Dữ Liệu** (MySQL `gia_pha`)
và **Hệ Thống Tệp** (thư mục `images/`).

Quan hệ quan trọng nhất được thể hiện bằng mũi tên `Service --> RepoInterface :
sử dụng (Dependency Inversion)` — Service phụ thuộc vào **interface**, không phụ
thuộc vào `JdbcPersonRepository`. Quan hệ `JdbcRepo ..|> RepoInterface :
implements (Repository Pattern)` thể hiện mẫu Repository.

Ngoài ra, `Controller --> ImageMgr : copy ảnh vào images/` và `UI --> PngW :
ghi file PNG` cho thấy hai tiện ích tĩnh được dùng từ **hai tầng khác nhau** —
đây là đặc điểm của utility class: không phụ thuộc tầng nào, chỉ cung cấp hành vi.

### 3.3. Sơ đồ triển khai (Deployment Diagram)

**File:** `docs/uml/deployment-diagram.puml` → ảnh `docs/uml/deployment-diagram.png`

Sơ đồ triển khai mô tả **hai node vật lý**:

- **Máy tính người dùng**: môi trường chạy Java 25 + JavaFX 23, chứa ứng dụng
  JavaFX và driver MySQL JDBC (Connector/J 9.2.0); cùng nằm trên node này là
  **thư mục làm việc ứng dụng** (`user.dir`) chứa thư mục `images/`.
- **Máy chủ MySQL**: DBMS chứa database `gia_pha` với bảng `person`.

Kết nối giữa hai node là **TCP/IP port 3306** qua JDBC.

Ghi chú quan trọng được đặt trong sơ đồ: vì `photo_path` lưu **đường dẫn tương đối**
(ví dụ `images/person_7.png`), ứng dụng **phải chạy từ thư mục gốc dự án** —
nếu chạy từ thư mục khác, `ImageManager.getImageDirectory()` sẽ trỏ sai chỗ và ảnh
không hiển thị.

Cấu hình kết nối đọc qua biến môi trường `DB_URL` / `DB_USER` / `DB_PASSWORD`
(ưu tiên System property → Environment variable → giá trị mặc định), **không
hard-code mật khẩu thật** vào mã nguồn.

### 3.4. Thiết kế cơ sở dữ liệu

**File:** `database/schema.sql`

#### 3.4.1. Cấu trúc bảng `person`

| Cột | Kiểu | Ràng buộc | Ý nghĩa |
|---|---|---|---|
| `id` | `INT AUTO_INCREMENT` | `PRIMARY KEY` | Mã định danh, tự sinh |
| `full_name` | `VARCHAR(100)` | `NOT NULL` | Họ và tên đầy đủ |
| `birth_year` | `INT` | `NOT NULL` | Năm sinh |
| `gender` | `ENUM('MALE','FEMALE')` | `NOT NULL` | Giới tính — ràng buộc ở mức CSDL luôn đúng kiểu |
| `photo_path` | `VARCHAR(255)` | `NULL` | Đường dẫn **tương đối** tới ảnh, ví dụ `images/person_7.png` |
| `father_id` | `INT` | `NULL`, FK → `person(id)` `ON DELETE RESTRICT` | Tham chiếu cha |
| `mother_id` | `INT` | `NULL`, FK → `person(id)` `ON DELETE RESTRICT` | Tham chiếu mẹ |

Ngoài ra có `CREATE INDEX idx_person_name ON person(full_name)` phục vụ tìm kiếm.

#### 3.4.2. Ràng buộc (CONSTRAINT) và Trigger

| Tên | Loại | Điều kiện | Bảo vệ quy tắc |
|---|---|---|---|
| `fk_father` | FOREIGN KEY | `father_id → person(id)` `ON DELETE RESTRICT` | BR4 |
| `fk_mother` | FOREIGN KEY | `mother_id → person(id)` `ON DELETE RESTRICT` | BR4 |
| `chk_birth_year` | CHECK | `birth_year >= 1000 AND birth_year <= 2100` | Validation |
| `chk_father_not_mother` | CHECK | `father_id IS NULL OR mother_id IS NULL OR father_id <> mother_id` | BR2 |
| `trg_check_not_self_parent_update` | TRIGGER `BEFORE UPDATE` | `NEW.father_id = NEW.id` hoặc `NEW.mother_id = NEW.id` → `SIGNAL SQLSTATE '45000'` | BR1 |

**Vì sao BR3 (chu trình) không có ở tầng CSDL?** MySQL/MariaDB không hỗ trợ
`CHECK` constraint tham chiếu tới bảng khác (không thể viết điều kiện "id này
không phải hậu duệ của id kia" bằng một biểu thức `CHECK` đơn giản). Vì vậy BR3
được bảo vệ **duy nhất ở tầng Service** bằng thuật toán duyệt đồ thị — xem
`FamilyTreeService.checkCycle(int, int)`.

**Lưu ý kỹ thuật:** `chk_birth_year` dùng hằng số `2100` thay vì `CURDATE()`,
vì MySQL/MariaDB yêu cầu biểu thức `CHECK` phải mang tính **xác định
(deterministic)**. Tầng Service dùng `Year.now().getValue()` nên chặn chặt hơn
(`<= 2026`); ràng buộc CSDL là lưới an toàn bổ sung.

#### 3.4.3. Dữ liệu mẫu

`database/schema.sql` kèm sẵn **7 bản ghi** đại diện cho 4 thế hệ:
`Nguyễn Văn Tổ` (1930) × `Trần Thị Tổ` (1932) → `Nguyễn Văn Cha` (1955)
× `Lê Thị Mẹ` (1958) → `Nguyễn Văn Con` (1985) × `Phạm Thị Dâu` (1987)
→ `Nguyễn Văn Cháu` (2015).

Bộ dữ liệu mẫu mở rộng **30 người / 5 thế hệ / 4 nhánh dòng họ** nằm ở
`test-data/sample-data.sql`, dùng cho kiểm thử (`LargeSampleDataTest`) và có thể
nạp vào database chính để demo thủ công.

### 3.5. Các hình mẫu thiết kế đã sử dụng

> Bảng này giữ nguyên nội dung như `README.md` §6.2 — chỉ liệt kê **những mẫu có
> bằng chứng trực tiếp trong mã nguồn**. Cột "Loại" phân biệt rõ: **Kiến trúc**
> (mô hình tổ chức hệ thống), **Mẫu thiết kế** (GoF / enterprise pattern),
> **Idiom** (kỹ thuật lập trình quy ước, không phải mẫu GoF), **Nguyên tắc**
> (SOLID & nguyên tắc thiết kế).

| Hình mẫu / Nguyên tắc | Loại | File & Lớp | Vai trò trong mẫu | Bằng chứng trong code |
|---|---|---|---|---|
| **Layered Architecture (Kiến trúc phân tầng)** | Kiến trúc | `com.giapha.view.*`, `com.giapha.controller.PersonController`, `com.giapha.service.FamilyTreeService`, `com.giapha.repository.PersonRepository` + `JdbcPersonRepository`, `com.giapha.database.DatabaseConnection` | Mỗi tầng là một vai trò riêng biệt: **View** (hiển thị) → **Controller** (điều phối) → **Service** (nghiệp vụ) → **Repository** (truy cập dữ liệu) → **Database** (kết nối). Tầng trên chỉ gọi tầng ngay dưới nó. | `view/*.java` chỉ `import com.giapha.controller.*` (không import service/repository/database); `FamilyTreeService` chỉ import `com.giapha.repository.PersonRepository`. |
| **MVC (Model–View–Controller)** | Kiến trúc | **Model**: `com.giapha.model.Person`, `com.giapha.model.Gender` · **View**: `PeopleListView`, `FamilyTreeView`, `FamilyTreeDiagram`, `PersonDetailView`, `PersonFormView`, `PersonAvatar` · **Controller**: `PersonController` · **Entry point / Composition Root**: `com.giapha.Main` | `Person`/`Gender` là **Model** (thuộc tính dữ liệu thuần + getter/setter, không chứa SQL hay UI); các lớp `*View` là **View** (dựng UI, bắt sự kiện); `PersonController` là **Controller** (nhận sự kiện từ View, gọi Service, trả kết quả về View). `Main` khởi tạo và nối các thành phần. | `PersonController.handleAdd()` đọc dữ liệu từ `FormDataSource` (abstraction do `PersonFormView` hiện thực) → gọi `familyTreeService.addPerson()` → `showInfoAlert()`; `Main.initBackend()` lắp ráp `JdbcPersonRepository → FamilyTreeService → PersonController`. |
| **Dependency Inversion (SOLID – D)** | Nguyên tắc (SOLID) | `com.giapha.service.FamilyTreeService` (trường `private final PersonRepository personRepository`) | Tầng cao (`Service`) phụ thuộc vào **abstraction** (`PersonRepository` interface) thay vì phụ thuộc vào chi tiết (`JdbcPersonRepository`, JDBC API). Đây là hướng phụ thuộc bị đảo so với mặc định. | `FamilyTreeService` **không** import `JdbcPersonRepository`, `DatabaseConnection`, `java.sql.*`; thân hàm chỉ gọi `personRepository.findById/save/update/delete`. |
| **Constructor Injection (DI)** | Mẫu thiết kế (DI) | `FamilyTreeService(PersonRepository)` · `PersonController(FamilyTreeService)` · `Main.initBackend()` | **Injection point** là constructor; **composition root** là `Main` — nơi duy nhất quyết định dùng triển khai nào (`new JdbcPersonRepository()`). Nhờ đó có thể thay CSDL (hoặc truyền repository test) mà không sửa Service. | `Main.initBackend()`: `new JdbcPersonRepository()` → `new FamilyTreeService(repository)` → `new PersonController(service)`; `Objects.requireNonNull(...)` chặn dependency null. |
| **Repository Pattern** | Mẫu thiết kế (Enterprise) | **Interface (abstraction)**: `com.giapha.repository.PersonRepository` (`findById`, `findAll`, `search`, `save`, `update`, `delete`) · **Concrete repository**: `com.giapha.repository.JdbcPersonRepository` | Interface đóng vai trò **abstraction của kho lưu trữ**; `JdbcPersonRepository` là **Concrete implementation** gắn SQL vào bảng `person` và map `ResultSet` → đối tượng `Person`. Toàn bộ SQL nằm sau abstraction này. | `JdbcPersonRepository implements PersonRepository`; mọi câu `SELECT/INSERT/UPDATE/DELETE` đều nằm trong class này, không lọt sang tầng trên. |
| **Static Factory Method (JDBC Factory)** | Idiom (mẫu khởi tạo tập trung) | `com.giapha.database.DatabaseConnection.getConnection()` và `getConnection(url, user, password)` | **Factory** tạo đối tượng `java.sql.Connection` (product) mà không để client tự gọi `DriverManager`. Trả về **Connection mới** mỗi lần — không dùng connection `static` dài hạn; caller đóng bằng try-with-resources. | `DatabaseConnection` có constructor `private` (utility class) và chỉ có phương thức `static`; `JdbcPersonRepository.getConnection()` gọi `DatabaseConnection.getConnection()`. |
| **Template Method (điểm móc truy cập CSDL)** | Mẫu thiết kế (GoF) | **Base class**: `JdbcPersonRepository.getConnection()` (`protected`) · **Subclass**: `src/test/java/com/giapha/support/TestPersonRepository.getConnection()` (`@Override`) | Base class định nghĩa **bước có thể ghi đè** (lấy kết nối); subclass chỉ thay **bước cụ thể** để trỏ sang database test, còn toàn bộ CRUD kế thừa nguyên vẹn. Đây chính là cơ chế cho **Test Double** cô lập dữ liệu. | `TestPersonRepository extends JdbcPersonRepository` và `@Override protected Connection getConnection()` trả `TestDatabaseManager.openConnection(testDbName)`. |
| **Custom Exception (hierarchy lỗi nghiệp vụ)** | Mẫu thiết kế (Error handling) | `com.giapha.exception.*`: `SelfParentException`, `SameParentException`, `InvalidParentGenderException`, `CycleDetectedException`, `PersonInUseException`, `PersonNotFoundException`, `InvalidBirthYearException` — tất cả kế thừa `RuntimeException` | Mỗi lớp là **thông điệp có kiểu** cho một quy tắc nghiệp vụ (BR1–BR6). Tầng `Service` **ném** (không trả mã lỗi), tầng `Controller` **bắt** theo kiểu và ánh xạ sang thông báo tiếng Việt — stack trace không bao giờ lọt ra UI. | `FamilyTreeService.assignFather()` ném `SelfParentException` / `SameParentException` / `InvalidParentGenderException` / `CycleDetectedException`; `PersonController` có `catch` riêng cho từng loại và gọi `showErrorAlert(...)`. |
| **Observer (thuộc tính JavaFX)** | Mẫu thiết kế (GoF — dạng property observer) | **Subject**: `com.giapha.view.FamilyTreeDiagram` — trường `zoomProperty` (`ReadOnlyDoubleWrapper`), expose qua `contentScaleProperty()` · **Observer**: `com.giapha.view.FamilyTreeView` — `diagram.contentScaleProperty().addListener(...)` | `FamilyTreeDiagram` là **Subject** giữ trạng thái zoom và phát thông báo khi trạng thái đổi; `FamilyTreeView` là **Observer** cập nhật nhãn `%` trên toolbar mà không cần sơ đồ biết đến toolbar. | `FamilyTreeDiagram.contentScaleProperty()` trả `ReadOnlyDoubleProperty`; `FamilyTreeView` đăng ký listener để set `lblZoom.setText(... + "%")`. |
| **Utility Class (lớp tiện ích tĩnh)** | Idiom (tiện ích) | `com.giapha.util.ImageManager` · `com.giapha.util.PngWriter` | Constructor `private` + toàn bộ phương thức `static` → **không có trạng thái, không thể khởi tạo từ bên ngoài**; chỉ cung cấp hành vi dùng chung (copy ảnh, ghi file PNG). | Cả hai class đều có `private ImageManager()` / `private PngWriter()` và không có field instance. |

**Ghi chú — các mục từng được liệt kê nhưng đã bị loại khỏi bảng** vì **không phải
hình mẫu thiết kế theo đúng nghĩa** (không có bằng chứng cấu trúc tương ứng trong code):

- ~~"Strategy cấu hình"~~ → `DatabaseConnection.getUrl()/getUser()/getPassword()` chỉ là
  **chuỗi ưu tiên System property → Environment variable → giá trị mặc định** (thứ tự
  cố định, không có interface/đối tượng chiến lược thay thế nhau) → thuộc loại
  **cấu hình theo môi trường**, không phải Strategy Pattern.
- ~~"Data Access Object mở rộng (test)"~~ → không phải một mẫu riêng; đã được gộp đúng
  bản chất vào dòng **Template Method** (`TestPersonRepository` ghi đè điểm móc `getConnection()`).
- ~~"Separation of Concerns"~~ → là **nguyên tắc thiết kế**, không phải hình mẫu cụ thể
  có cấu trúc lớp định danh; nội dung của nó đã được mô tả đầy đủ ở mục *Ranh giới
  trách nhiệm* (Mục 3.1) và trong dòng *Layered Architecture* ở trên.
- ~~"Singleton"~~ → **không** dùng: `DatabaseConnection` là utility class tĩnh, **không**
  giữ instance `static` dài hạn; mỗi lần gọi đều trả `Connection` mới (xem dòng
  *Static Factory Method*).

### 3.6. Lý do chọn các quyết định thiết kế chính

| Quyết định | Lý do |
|---|---|
| **Không dùng ORM** (Hibernate/JPA/MyBatis) | Đề tài môn Lập trình hướng đối tượng cần thể hiện rõ quan hệ giữa các lớp; dùng JDBC thuần cho thấy rõ luồng dữ liệu và đồng thời tránh "ẩn" SQL sau một framework. |
| **Service phụ thuộc interface `PersonRepository`** | Cho phép thay đổi hệ quản trị CSDL hoặc dùng repository giả (test double) mà **không sửa một dòng nào** ở tầng nghiệp vụ — đây là cơ sở để viết được bộ test cô lập dữ liệu. |
| **Mô hình hóa quan hệ cha/mẹ bằng tham chiếu đối tượng** trong `Person` | Giữ đúng tinh thần OOP: quan hệ phả hệ là quan hệ trực hệ, biểu diễn bằng object reference tự nhiên hơn là bảng trung gian; đồng thời vẫn ánh xạ được sang 2 cột `father_id`/`mother_id` khi lưu. |
| **Mỗi quy tắc nghiệp vụ 1 exception riêng** | Controller bắt theo kiểu và hiển thị thông báo **đúng nguyên nhân**; người dùng biết chính xác "sai giới tính vai trò" khác "tạo chu trình". |
| **BR5 là cảnh báo chứ không phải chặn** | Theo đặc tả: dữ liệu gia phả lịch sử có thể có năm sinh cha/mẹ không nhỏ hơn con (ví dụ ghi nhận sai). Chặn cứng sẽ làm người dùng không lưu được. |
| **Tự viết `PngWriter` bằng `java.util.zip`** | `javax.imageio` thuộc module `java.desktop`, `javafx.swing` là module riêng — dùng chúng sẽ phải khai báo thêm `requires` trong `module-info.java`, làm nới lỏng ranh giới module của dự án. Tự viết bằng `Deflater` + `CRC32` giữ cho module `com.giapha` chỉ cần `javafx.controls`, `javafx.graphics`, `java.sql`. |
| **Lưu đường dẫn ảnh tương đối** | Database không bị phụ thuộc vào vị trí cài đặt cụ thể; cùng một file `schema.sql` + `images/` chạy được trên máy khác. Đánh đổi: ứng dụng phải chạy từ thư mục gốc dự án. |
| **Sơ đồ 2D là component JavaFX thuần** (`Pane` + `Line`) | `FamilyTreeDiagram` không gọi xuống Service/Repository — nó nhận `List<Person>` + người trung tâm và tự dựng bố cục. Nhờ đó có thể kiểm thử và tái sử dụng component mà không cần dựng cả ứng dụng. |
| **Dùng `javafx.print.PrinterJob` cho in ấn** | JavaFX đã cung cấp sẵn cơ chế in (khung giấy, hộp thoại hệ thống); không cần thêm thư viện in ấn bên thứ ba. |
| **UI xây dựng hoàn toàn bằng Programmatic JavaFX (Java + CSS)**, không dùng FXML | Dự án không có file `.fxml` nào — toàn bộ UI được dựng bằng Java code kết hợp `style.css`. Nhờ đó không cần dependency `javafx-fxml`, `module-info.java` chỉ còn 3 `requires` (`javafx.controls`, `javafx.graphics`, `java.sql`), và logic giao diện đọc được như mã nguồn thông thường (thuận lợi cho môn OOP khi trình bày). |

---

## 4. MÔ TẢ CÀI ĐẶT VÀ HƯỚNG DẪN SỬ DỤNG

### 4.1. Yêu cầu hệ thống

| Phần mềm | Bắt buộc | Ghi chú |
|---|---|---|
| **JDK 25** | ✅ | `java -version` phải hiện `25.x` — `pom.xml` đặt `source/target = 25` |
| **Apache Maven 3.9+** | ✅ | `mvn -v` |
| **MySQL Server >= 8.0.16 / MariaDB >= 10.2.1** | ✅ | Port `3306`, charset `utf8mb4`. Có thể dùng bản đi kèm XAMPP. **Bắt buộc phiên bản này** vì ràng buộc `CHECK` chỉ có hiệu lực thật từ MySQL 8.0.16 / MariaDB 10.2.1 (trước đó `CHECK` bị bỏ qua) |
| **Client SQL** (MySQL Shell / HeidiSQL / phpMyAdmin) | ⬜ | Để nạp file `.sql` |
| **Git** | ⬜ | Chỉ cần nếu clone từ repository |

> **Mẹo Windows (XAMPP)**: mở **XAMPP Control Panel** → bấm **Start** ở dòng **MySQL**,
> hoặc chạy `net start mysql` trong Command Prompt (cần quyền Administrator).

### 4.2. Cài đặt

**Bước 1 — Clone và biên dịch**

```bash
git clone https://github.com/minhchien170704/do-an-gia-pha.git
cd do-an-gia-pha
mvn clean compile
```

**Bước 2 — Tạo database và nạp dữ liệu mẫu**

```bash
mysql -u root --default-character-set=utf8mb4 -e "source database/schema.sql"
```

Kiểm tra:

```bash
mysql -u root -e "SELECT COUNT(*) FROM gia_pha.person;"   # → 7
```

> ⚠️ `database/schema.sql` chứa `DROP DATABASE IF EXISTS gia_pha;` — chạy lại sẽ
> **xoá sạch** dữ liệu đang có. Chỉ chạy khi mới cài hoặc muốn khôi phục dữ liệu gốc.

**Bước 3 — Cấu hình kết nối (nếu MySQL của bạn có mật khẩu)**

```powershell
$env:DB_URL      = "jdbc:mysql://localhost:3306/gia_pha?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:DB_USER     = "root"
$env:DB_PASSWORD = "mat_khau_cua_ban"
```

**Bước 4 — Chạy ứng dụng**

```bash
mvn javafx:run
```

> Ứng dụng dùng **đường dẫn tương đối** cho ảnh đại diện (`images/person_<id>.png`),
> nên **luôn chạy từ thư mục gốc dự án**. Nếu mở trực tiếp file JAR từ chỗ khác,
> ảnh sẽ không hiển thị.

### 4.3. Giao diện người dùng

Ứng dụng có **5 màn hình**:

| # | Màn hình | Lớp View | Nội dung |
|---|---|---|---|
| 1 | **Danh sách thành viên** | `PeopleListView` | Bảng dữ liệu (Ảnh, ID, Họ tên, Năm sinh, Giới tính, Cha, Mẹ) + ô tìm kiếm + 4 thẻ thống kê (Tổng thành viên / Số thế hệ / Thành viên Nam / Thành viên Nữ) |
| 2 | **Thêm / Sửa thành viên** | `PersonFormView` | Form: ảnh đại diện (chọn qua `FileChooser`), họ tên, năm sinh, giới tính, chọn cha, chọn mẹ |
| 3 | **Chi tiết thành viên** | `PersonDetailView` | Avatar, tên, năm sinh/giới tính, thông tin cá nhân, card Cha – Mẹ, danh sách các con |
| 4 | **Cây gia phả (sơ đồ 2D)** | `FamilyTreeView` + `FamilyTreeDiagram` | Sơ đồ 2D (Pane + Line) có zoom/pan, kèm tab "Danh sách phả hệ" dạng `TreeView` |
| 5 | **Hộp thoại xác nhận / thông báo** | `javafx.scene.control.Alert` | Báo thành công, báo lỗi nghiệp vụ, cảnh báo BR5 (Tiếp tục / Hủy) |

#### 4.3.1. Màn hình danh sách thành viên

> `[ẢNH: màn hình danh sách thành viên]`

*Chú thích cho ảnh: bảng dữ liệu ở giữa với 7 cột; phía trên là 4 thẻ thống kê
(tổng thành viên, số thế hệ, số nam, số nữ); thanh công cụ có ô "Tìm kiếm theo họ
tên..." và các nút "+ Thêm thành viên", "Sửa", "Chi tiết", "Cây gia phả", "Xóa";
phía dưới là dòng "Hiển thị X trên tổng số Y thành viên"; bên trái là sidebar
điều hướng.*

#### 4.3.2. Màn hình thêm / sửa thành viên

> `[ẢNH: màn hình thêm/sửa thành viên]`

*Chú thích cho ảnh: khung xem trước ảnh đại diện bo tròn, nút "Chọn ảnh...";
ô "Họ và tên *", ô "Năm sinh *", dropdown "Giới tính *"; khu vực "Quan hệ gia
đình" gồm dropdown "Cha" và "Mẹ" (mỗi dropdown có lựa chọn đầu tiên là
"— Không có —"); cuối form là nút "Hủy" và "Lưu thành viên".*

#### 4.3.3. Màn hình chi tiết thành viên

> `[ẢNH: màn hình chi tiết thành viên]`

*Chú thích cho ảnh: card "hero" ở trên gồm avatar bo tròn, họ tên, dòng "Sinh năm
... · Nam/Nữ", nút "Chỉnh sửa" và "Vem cây gia phả"; bên dưới là card "Thông tin
cá nhân" và card "Quan hệ gia đình" (Cha – Mẹ – danh sách các con).*

#### 4.3.4. Màn hình cây gia phả (sơ đồ 2D)

> `[ẢNH: màn hình cây gia phả - sơ đồ 2D]`

*Chú thích cho ảnh: toolbar gồm dropdown "Người trung tâm", cụm điều khiển zoom
(− / 100% / + / Vừa khung), nút "🖼 Xuất PNG", nút "🖨 In" và chú giải màu; phía
dưới là sơ đồ 2D: mỗi thế hệ một hàng, tổ tiên ở trên, hậu duệ ở dưới, node
gồm avatar + họ tên + năm sinh + giới tính, node của người trung tâm có viền
đậm màu xanh.*

> `[ẢNH: màn hình cây gia phả - tab danh sách phả hệ]`

*Chú thích cho ảnh: tab thứ hai "Danh sách phả hệ" hiển thị dạng `TreeView`
phân cấp (tổ tiên ở trên, hậu duệ ở dưới).*

#### 4.3.5. Hộp thoại cảnh báo BR5

> `[ẢNH: hộp thoại cảnh báo năm sinh BR5]`

*Chú thích cho ảnh: hộp thoại tiêu đề "Cảnh báo năm sinh (BR5)", nội dung liệt kê
từng cảnh báo dạng "• Năm sinh của cha (...) không nhỏ hơn năm sinh của con (...)",
kết thúc bằng câu hỏi "Bạn vẫn muốn tiếp tục thao tác này không?" với 2 nút
**Tiếp tục** và **Hủy**.*

#### 4.3.6. Hộp thoại lỗi nghiệp vụ

> `[ẢNH: hộp thoại lỗi — tạo chu trình]`
>
> `[ẢNH: hộp thoại lỗi — xóa người đang làm cha/mẹ]`

##### Danh sách ảnh cần chụp để hoàn thiện mục 4.3

Các placeholder `[ẢNH: ...]` ở trên **được giữ nguyên** — người viết báo cáo cần
tự chụp ảnh thật từ ứng dụng đang chạy (`mvn javafx:run`) rồi thay vào. Danh sách
ảnh cần chụp:

| # | Ảnh cần chụp | Màn hình / thao tác | Gợi ý |
|---|---|---|---|
| 1 | **Màn hình danh sách thành viên** | `PeopleListView` | Chụp nguyên cửa sổ, thấy rõ bảng 7 cột + 4 thẻ thống kê + thanh công cụ + sidebar |
| 2 | **Màn hình thêm thành viên** | `PersonFormView` (chế độ thêm) | Bấm "+ Thêm thành viên" từ màn hình danh sách; nên có sẵn ảnh trong `images/` để khung xem trước hiện avatar |
| 3 | **Màn hình sửa thành viên** | `PersonFormView` (chế độ sửa) | Chọn 1 dòng rồi bấm "Sửa"; dropdown Cha/Mẹ đã chọn sẵn giá trị |
| 4 | **Màn hình chi tiết thành viên** | `PersonDetailView` | Chọn 1 dòng rồi bấm "Chi tiết"; thấy card hero + Thông tin cá nhân + Quan hệ gia đình |
| 5 | **Cây gia phả — sơ đồ 2D** | `FamilyTreeView` (tab 1) | Chọn người trung tâm, zoom vừa khung; nên zoom > 100% để thấy rõ node |
| 6 | **Cây gia phả — tab danh sách phả hệ** | `FamilyTreeView` (tab 2) | Chuyển sang tab "Danh sách phả hệ", mở rộng vài nút để thấy cấu trúc `TreeView` |
| 7 | **Hộp thoại cảnh báo BR5** | Sửa 1 người, chọn Cha có năm sinh **không nhỏ hơn** con → bấm "Lưu thay đổi" | Chụp khoảnh khắc hộp thoại hiện ra (nút **Tiếp tục** / **Hủy**) |
| 8 | **Hộp thoại lỗi — tạo chu trình** | Sửa tổ gia, chọn Cha là hậu duệ của chính người đó → bấm "Lưu thay đổi" | Chụp hộp thoại tiêu đề **"Lỗi chu trình"** — KHÔNG có câu "Đã cập nhật thành công" |
| 9 | **Hộp thoại lỗi — xóa người đang làm cha/mẹ** | Chọn 1 người đang làm cha/mẹ → bấm "Xóa" → xác nhận | Chụp hộp thoại tiêu đề **"Không thể xóa thành viên"** |
| 10 | *(tuỳ chọn)* **Hộp thoại thành công khi thêm** | Thêm 1 thành viên có ảnh | Chụp Alert "Đã thêm thành viên ... thành công" |
| 11 | *(tuỳ chọn)* **Sơ đồ đã xuất PNG** | Cây gia phả → bấm "🖼 Xuất PNG" rồi mở file | Chụp cửa sổ hiển thị ảnh PNG đã xuất |

**Lưu ý khi chụp:** dùng dữ liệu mẫu `test-data/sample-data.sql` (30 người / 5 thế
hệ) để sơ đồ 2D và cây phả hệ nhìn đầy đủ, đẹp và có nhiều nhánh. Nếu ứng dụng
đang chạy từ thư mục gốc dự án (`mvn javafx:run`) thì thư mục `images/` sẽ được
sinh tự động cạnh ứng dụng và avatar hiển thị đúng.

### 4.4. Hướng dẫn sử dụng

#### 4.4.1. Thêm một thành viên mới

1. Trên màn hình **Danh sách thành viên**, bấm **"+ Thêm thành viên"**.
2. (Tùy chọn) Bấm **"Chọn ảnh..."** → chọn file `.png` / `.jpg` / `.jpeg`.
   Ảnh hiển thị xem trước ngay trong khung bo tròn bên trái.
3. Nhập **Họ và tên**, **Năm sinh**, chọn **Giới tính**.
4. (Tùy chọn) Chọn **Cha** và **Mẹ** trong dropdown. Danh sách đã lọc sẵn theo giới
   tính (cha chỉ hiện nam, mẹ chỉ hiện nữ) và loại bỏ chính thành viên đang sửa.
5. Bấm **"Lưu thành viên"**.
6. Nếu năm sinh cha/mẹ không nhỏ hơn năm sinh con → hiện cảnh báo BR5, chọn
   **Tiếp tục** để lưu hoặc **Hủy** để quay lại.
7. Thông báo "Đã thêm thành viên ... vào gia phả thành công." → ứng dụng tự quay
   về danh sách.

#### 4.4.2. Sửa thông tin một thành viên

1. Chọn một dòng trong bảng → bấm **"Sửa"** (hoặc nhấp đúp vào dòng để xem chi tiết
   rồi bấm **"Chỉnh sửa"**).
2. Sửa các trường, bấm **"Lưu thay đổi"**.
3. Nếu đổi cha/mẹ, ứng dụng gọi `FamilyTreeService.updatePersonWithParents(...)` —
   toàn bộ thông tin cá nhân + cha + mẹ được **kiểm tra trước**, rồi mới ghi bằng
   **một** câu `UPDATE` duy nhất, nên mọi quy tắc BR1–BR6 vẫn được áp dụng và
   **không** xảy ra trạng thái "cập nhật một nửa". Nếu bỏ chọn cha/mẹ
   (chọn "— Không có —"), quan hệ được gỡ trong cùng thao tác đó.

#### 4.4.3. Xóa một thành viên

1. Chọn dòng cần xóa → bấm **"Xóa"** → hộp thoại xác nhận.
2. Nếu thành viên đang là cha/mẹ của người khác, hệ thống **từ chối** và hiện
   thông báo: *"Thành viên này hiện đang đóng vai trò làm cha hoặc mẹ của người
   khác trong hệ thống gia phả. Vui lòng thay đổi hoặc xóa các mối quan hệ con
   cái trước khi thực hiện xóa."* (BR4). Hãy gỡ quan hệ trước rồi xóa.

#### 4.4.4. Tìm kiếm

Gõ vào ô **"Tìm kiếm theo họ tên..."** — danh sách lọc tức thì (không phân biệt
hoa thường). Số liệu dưới bảng cập nhật theo: *"Hiển thị X trên tổng số Y thành
viên"*.

#### 4.4.5. Xem cây gia phả

1. Chọn một thành viên trong bảng → bấm **"Cây gia phả"**; hoặc bấm mục
   **"Cây gia phả trực quan"** trên sidebar.
2. Chọn **"Người trung tâm"** trên toolbar → sơ đồ được vẽ lại ngay.
3. Tương tác:

| Thao tác | Kết quả |
|---|---|
| Kéo chuột trái | Di chuyển sơ đồ (pan) |
| Lăn chuột dọc | Cuộn dọc |
| Shift + lăn chuột | Cuộn ngang |
| Ctrl + lăn chuột | Zoom (0.25× – 2.5×) |
| Nút `−` / `+` / `Vừa khung` | Zoom out / zoom in / tự động vừa khung nhìn |
| Bấm vào node | Mở hồ sơ cá nhân của thành viên đó |

#### 4.4.6. Xuất PNG

1. Bấm **"🖼 Xuất PNG"** → hộp thoại chọn nơi lưu (tên file mặc định:
   `gia_pha_<Tên người trung tâm>.png`).
2. Ảnh xuất ra là **toàn bộ sơ đồ** ở mức zoom 1.0 — không bị cắt theo khung nhìn
   hiện tại, kể cả phần đang bị thu nhỏ.
3. Thông báo "Xuất PNG thành công" kèm đường dẫn tuyệt đối của file.

#### 4.4.7. In sơ đồ

1. Bấm **"🖨 In"**.
2. Nếu hệ thống có máy in, hộp thoại in chuẩn của JavaFX hiện ra; nếu nền tảng không
   hỗ trợ, ứng dụng hiện hộp thoại xác nhận nội bộ *"In sơ đồ cây gia phả?"*.
3. Sơ đồ được canh giữa trên trang giấy theo khổ lấy từ `JobSettings.getPageLayout()`.
4. Thông báo "Đã gửi lệnh in" hoặc "Không in được" nếu máy in từ chối.

### 4.5. Chuẩn bị dữ liệu để demo

```bash
# Nạp bộ dữ liệu mẫu 30 người / 5 thế hệ vào database chính
mysql -u root -p < test-data/sample-data.sql
```

> ⚠️ Script này **xoá toàn bộ** dữ liệu đang có trong bảng `person`.
> Khôi phục: `mysql -u root -p < database/schema.sql`.

---

## 5. KIỂM THỬ

### 5.1. Chiến lược kiểm thử

Bộ kiểm thử dùng **JUnit 5** (`junit-jupiter` 5.10.2) + `maven-surefire-plugin` 3.2.5.

**Nguyên tắc cô lập dữ liệu (bắt buộc):**

- **Mọi** lớp test có ghi/xóa dữ liệu đều dùng **database riêng** có tên kết thúc
  bằng `_test`: `gia_pha_service_test`, `gia_pha_controller_test`, `gia_pha_conn_test`,
  `gia_pha_javafx_test`, `gia_pha_test`. Tuyệt đối không ghi vào database chính `gia_pha`.
- Vòng đời: `@BeforeAll` → `TestDatabaseManager.createFreshDatabase(<db>_test)`
  (DROP + CREATE + schema + 7 bản ghi mẫu); `@AfterAll` →
  `TestDatabaseManager.dropDatabase(<db>_test)`.
- **Ràng buộc an toàn khi DROP**: `TestDatabaseManager.assertSafeToDrop()` kiểm tra
  tên database **bắt buộc kết thúc bằng `_test`**; nếu không khớp (ví dụ `gia_pha`)
  hàm ném `IllegalStateException` và **không DROP**.
- Test phụ thuộc MySQL dùng `Assumptions.assumeTrue(...)` → nếu CSDL không sẵn sàng
  thì test được `SKIPPED` kèm thông báo (không tính là `FAILED`).
- Test cần JavaFX Toolkit được gắn `@Tag("javafx")` và **không** chạy trong
  `mvn test` mặc định.

### 5.2. Cơ cấu bộ kiểm thử

| Lớp kiểm thử | Số test | Phạm vi |
|---|---|---|
| `FamilyTreeServiceTest` | 47 | BR1–BR6, TC01–TC14, TC-CLEAR-01…05, `updatePersonWithParents` (tồn tại → BR2 → BR1 → BR6 → BR3), cô lập dữ liệu |
| `PersonControllerTest` | 25 | Điều phối View ↔ Service, xử lý exception, callback điều hướng |
| `PersonControllerAbstractionTest` | 23 | Fake `FormDataSource`/`Navigator`/`PeopleListSource` — KHÔNG cần JavaFX Toolkit |
| `LargeSampleDataTest` | 22 | Bộ dữ liệu mẫu 30 người / 5 thế hệ (BR1–BR6, validation, ràng buộc CSDL, an toàn DROP DB) |
| `DataIntegrityTest` | 19 | GĐ1: toàn vẹn dữ liệu ảnh + đồng nhất BR1–BR6 |
| `ImageManagerTest` | 12 | Định dạng ảnh, copy vào `./images/`, dọn file an toàn |
| `PngWriterTest` | 11 | Chữ ký PNG, khối IHDR/IDAT/IEND, CRC32, nén zlib |
| `EditFailureRollbackTest` | 10 | `handleEdit` rollback khi service từ chối quan hệ (BR1/BR2/BR3/BR6): DB không đổi, ảnh cũ không bị ghi đè, không điều hướng |
| `AtomicUpdateAndBr5Test` | 9 | Cập nhật nguyên tử (cha/mẹ + thông tin cá nhân), BR5 trong luồng lưu của form |
| `GenderTest` | 8 | Nhãn hiển thị + CSS class của enum `Gender` — KHÔNG cần JavaFX Toolkit |
| `DatabaseConnectionTest` | 7 | Kết nối MySQL, metadata, bảng `person`, kiểm chứng DB chính không bị đổi |
| `JdbcPersonRepositoryConnectionTest` | 5 | Quyền sở hữu `Connection`: repo tự mở thì tự đóng, Connection ngoài thì không đóng |
| `BackendInitializationTest` | 3 | Kiểm tra kết nối MySQL thật trước khi hiển thị giao diện |
| **Tổng (mặc định)** | **201** | `mvn clean test` |
| `AvatarFallbackTest` + `JavaFxViewTest` (tag `javafx`) | 30 | Chạy riêng bằng `mvn test -Pjavafx-tests` |

### 5.3. Bảng test case — bộ dữ liệu mẫu 30 người / 5 thế hệ

> Bảng dưới đây được lấy nguyên văn từ `test-data/test-cases.md` (mục 2).
> Cột "Kết quả thực tế" được sinh tự động bởi `LargeSampleDataTest` khi chạy.

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
| TC-20 | Trigger CSDL chặn tự làm cha/mẹ chính mình | `UPDATE person SET father_id = id WHERE id = 23` | CSDL từ chối (SQLException) | `SQLException`: Một người không thể là cha hoặc mẹ của chính mình | PASS |
| TC-21a | **An toàn DROP** — assertSafeToDrop("gia_pha_test") | Tên DB kết thúc bằng `_test` | Không ném exception | Không ném exception | PASS |
| TC-21b | **An toàn DROP** — assertSafeToDrop("gia_pha") | Tên database CHÍNH | Ném `IllegalStateException` | "Từ chối DROP database \"gia_pha\": tên database bắt buộc phải kết thúc bằng \"_test\" để bảo đảm không xóa nhầm dữ liệu chính." | PASS |
| TC-21c | **An toàn DROP** — assertSafeToDrop("") và (null) | Tên rỗng / null | Ném `IllegalStateException` | Cả hai đều ném IllegalStateException | PASS |
| TC-21d | **An toàn DROP** — assertSafeToDrop("test_gia_pha") | Chứa `_test` ở giữa nhưng không kết thúc | Ném `IllegalStateException` | Ném IllegalStateException | PASS |

**Tổng kết: 22 test case — 22 PASS, 0 FAIL.**

### 5.4. Ma trận phủ quy tắc nghiệp vụ

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

### 5.5. Kết quả chạy `mvn clean test`

Lệnh chạy:

```bash
mvn clean test
```

Kết quả thu được trên môi trường phát triển (Windows, JDK 25, MySQL/MariaDB đang chạy):

```
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0 -- in com.giapha.controller.EditFailureRollbackTest
[INFO] Tests run:  9, Failures: 0, Errors: 0, Skipped: 0 -- in com.giapha.controller.AtomicUpdateAndBr5Test
[INFO] Tests run: 19, Failures: 0, Errors: 0, Skipped: 0 -- in com.giapha.controller.DataIntegrityTest
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0 -- in com.giapha.controller.PersonControllerAbstractionTest
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0 -- in com.giapha.controller.PersonControllerTest
[INFO] Tests run:  3, Failures: 0, Errors: 0, Skipped: 0 -- in com.giapha.database.BackendInitializationTest
[INFO] Tests run:  7, Failures: 0, Errors: 0, Skipped: 0 -- in com.giapha.database.DatabaseConnectionTest
[INFO] Tests run:  8, Failures: 0, Errors: 0, Skipped: 0 -- in com.giapha.model.GenderTest
[INFO] Tests run:  5, Failures: 0, Errors: 0, Skipped: 0 -- in com.giapha.repository.JdbcPersonRepositoryConnectionTest
[INFO] Tests run: 47, Failures: 0, Errors: 0, Skipped: 0 -- in com.giapha.service.FamilyTreeServiceTest$DataIsolation
[INFO] Tests run:  0, Failures: 0, Errors: 0, Skipped: 0 -- in com.giapha.service.FamilyTreeServiceTest
[INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0 -- in com.giapha.testdata.LargeSampleDataTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0 -- in com.giapha.util.ImageManagerTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0 -- in com.giapha.util.PngWriterTest
[INFO] Results:
[INFO] Tests run: 201, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  13.162 s
```

| Chỉ số | Giá trị |
|---|---|
| Tổng test thực thi | **201** |
| Failures | **0** |
| Errors | **0** |
| Skipped | **0** |
| Thời gian | ~13 giây |
| Kết quả | **BUILD SUCCESS** |

**Ghi chú về cách đọc log Surefire:** Surefire ghi **hai** dòng cho cùng một lớp
khi lớp đó có test lồng `@Nested` — dòng `FamilyTreeServiceTest$DataIsolation`
(47 test) và dòng `FamilyTreeServiceTest` (0 test, là *vỏ rỗng* vì mọi test đã
được gán cho container lồng). **Chỉ dòng tổng cuối cùng (201) là con số chuẩn xác.**
Dòng này **không** bao gồm 30 test tag `javafx` (chạy riêng bằng
`mvn test -Pjavafx-tests`).

### 5.6. Chạy các nhóm test riêng

```bash
mvn test -Dtest=FamilyTreeServiceTest
mvn test -Dtest=PersonControllerTest#handleAddCreatesPerson
mvn test -Dtest=LargeSampleDataTest

# Giữ lại database test để debug trực tiếp
mvn test -Dtest=LargeSampleDataTest -Dkeep.test.db=true

# Các test cần JavaFX Toolkit
mvn test -Pjavafx-tests
mvn test -Pjavafx-tests -Dtest=JavaFxViewTest
mvn test -Pjavafx-tests -Dtest=AvatarFallbackTest
```

### 5.7. Ghi chú kỹ thuật — xung đột JPMS ↔ JUnit 5

`src/main/java` có `module-info.java` (module `com.giapha`), còn `src/test/java` là
test thuần JUnit 5 nằm ở "unnamed module". `pom.xml` cấu hình:

- `maven-compiler-plugin` → execution `default-testCompile` với `<useModulePath>false</useModulePath>`.
- `maven-surefire-plugin` → `<useModulePath>false</useModulePath>`.

> ⚠️ Đây là cấu hình **bắt buộc** — không được bỏ đi khi thêm test mới, nếu không
> `mvn test` sẽ lỗi compile hoặc lỗi `IllegalAccessError` khi JUnit 5 dùng
> reflection gọi các `@Test` method.

### 5.8. Hạn chế của phương pháp kiểm thử

- Bộ test **không** kiểm thử trải nghiệm người dùng cuối trên desktop (chuột/bàn phím);
  phần đó cần kiểm thử thủ công qua `mvn javafx:run`.
- Test tag `javafx` dùng `Platform.runLater()` + `CountDownLatch` (không dùng TestFX),
  nên không kiểm chứng được hành vi kéo–thả–zoom một cách định lượng.
- Chưa có kiểm thử hiệu năng (benchmark) với cây gia phả quy mô lớn hơn 30 người.

---

## 6. KẾT LUẬN

### 6.1. Kết quả đạt được

Hệ thống đã hoàn thành đầy đủ các yêu cầu của đề tài:

- Quản lý CRUD thành viên với ảnh đại diện, năm sinh, giới tính, quan hệ cha–mẹ.
- 6 quy tắc nghiệp vụ BR1–BR6 được kiểm tra ở tầng Service và bảo vệ bổ sung ở
  tầng CSDL bằng `CHECK` constraint + `TRIGGER` + `ON DELETE RESTRICT`.
- Sơ đồ cây gia phả 2D có zoom/pan, xuất PNG toàn sơ đồ và in ấn qua `PrinterJob`.
- Kiến trúc phân tầng rõ ràng, tuân thủ Dependency Inversion, không phụ thuộc framework.
- 201 test tự động + 30 test JavaFX, tất cả đều PASS; database chính `gia_pha` được
  bảo vệ khỏi mọi thao tác ghi của test.
- Cập nhật thành viên là **nguyên tử**: kiểm tra toàn bộ (cha/mẹ + thông tin cá nhân)
  trước, ghi bằng một câu `UPDATE` duy nhất — không có trạng thái "cập nhật một nửa".
  (Ở đây "nguyên tử" = kiểm tra toàn bộ rồi ghi bằng MỘT `UPDATE`; không dùng
  transaction nhiều bước, và bước kiểm tra – bước ghi không khóa lẫn nhau —
  chấp nhận được với ứng dụng desktop một người dùng.)
- Tài liệu UML đầy đủ 18 sơ đồ (`.puml` + `.png`) đồng bộ với code.

### 6.2. Ưu điểm

| Ưu điểm | Chi tiết |
|---|---|
| **Tính toàn vẹn dữ liệu được bảo vệ nhiều lớp** | BR1–BR6 ở tầng Service; `CHECK` + `TRIGGER` + FK `RESTRICT` ở tầng CSDL. Không thể tạo dữ liệu sai bằng cách đi vòng qua tầng ứng dụng (ví dụ chạy SQL trực tiếp). |
| **Kiến trúc rõ ràng, dễ bảo trì** | 5 tầng với trách nhiệm tách bạch; mỗi tầng có thể thay thế độc lập. Tầng Service không hề biết JDBC tồn tại. |
| **Không phụ thuộc framework, dễ trình bày trong đồ án OOP** | Chỉ JDK + JavaFX + JDBC thuần — mọi lớp, mọi mối quan hệ đều do tay viết, phù hợp mục tiêu môn học. |
| **Thông báo lỗi thân thiện** | Mỗi quy tắc có exception riêng → thông báo tiếng Việt đúng nguyên nhân, không lộ stack trace kỹ thuật ra UI. |
| **Mô hình hóa quan hệ phả hệ bằng OOP thuần** | `Person.father` / `Person.mother` là tham chiếu đối tượng; thuật toán DFS/BFS chạy trên object graph, không phải trên SQL phức tạp. |
| **Kiểm thử tự động đầy đủ và an toàn** | 231 test (201 + 30 JavaFX); mọi test ghi vào DB riêng `*_test` tự tạo/tự xóa; có bảo vệ chống DROP nhầm database chính. |
| **Sơ đồ 2D đẹp và linh hoạt** | Gom khối gia đình cha–mẹ, canh giữa con giữa hai vị trí, vẽ đường nối ở mức cá nhân (không nối nhầm vợ/chồng), hỗ trợ zoom 0.25×–2.5× và pan. |
| **Tự quản lý ảnh đại diện** | `PersonAvatar` fallback về chữ cái đầu khi thiếu ảnh / ảnh hỏng — **không bao giờ** ném exception ra giao diện; ảnh lưu bằng đường dẫn tương đối nên CSDL không phụ thuộc vị trí cài đặt. |
| **Tài liệu đầy đủ, đồng bộ** | 18 sơ đồ PlantUML render sẵn, README chi tiết, bộ test data + bảng test case, 3 tài liệu báo cáo/bảo vệ/demo trong `docs/report/`. |

### 6.3. Hạn chế

| Hạn chế | Ảnh hưởng |
|---|---|
| **Chưa có đăng nhập / phân quyền** | Mọi người dùng trên máy đều có toàn quyền sửa/xóa. |
| **Chưa có nhập–xuất dữ liệu theo chuẩn** (GEDCOM / CSV / JSON) | Không trao đổi được cây gia phả với phần mềm khác. |
| **Chưa đóng gói bản phân phối** (`.exe` / `.msi`) | Người dùng phải cài JDK 25 + Maven và chạy `mvn javafx:run`. |
| **Ứng dụng phải chạy từ thư mục gốc dự án** | Do lưu đường dẫn ảnh tương đối; nếu chạy từ nơi khác, ảnh đại diện không hiển thị. |
| **BR3 chỉ bảo vệ ở tầng Service** | MySQL/MariaDB không biểu đạt được ràng buộc "không phải hậu duệ" bằng `CHECK`; nếu ghi dữ liệu trực tiếp bằng SQL thì có thể tạo chu trình. |
| **Tìm kiếm chạy ở phía client** | `PeopleListView` lọc trên bộ nhớ đã nạp (`FilteredList`); `searchPersons()` có truy vấn SQL `LIKE` nhưng giao diện chính dùng bộ lọc client. Với dữ liệu rất lớn sẽ cần tối ưu. |
| **Chưa có kiểm thử hiệu năng / tải** | Chỉ kiểm thử trên cây 30 người / 5 thế hệ. |
| **Sơ đồ 2D chưa hỗ trợ** thu nhỏ rất nhiều / xuất vector (SVG/PDF) | Ở mức zoom tối thiểu (0.25×) vẫn phải cuộn để xem hết; chỉ xuất được PNG (raster). |
| **Chưa có giao diện đa ngôn ngữ** | Toàn bộ thông điệp viết cứng trong mã nguồn (tiếng Việt). |

### 6.4. Hướng phát triển

**Ngắn hạn (có thể làm ngay):**

1. **Đóng gói ứng dụng** thành file phân phối (jpackage → `.exe` / `.msi`) để người dùng
   không phải cài JDK/Maven.
2. **Sửa hạn chế đường dẫn ảnh tương đối**: chuyển sang lưu ảnh trong CSDL kiểu `BLOB`,
   hoặc lưu đường dẫn tuyệt đối cộng thư mục gốc cấu hình.
3. **Thêm chức năng nhập–xuất GEDCOM/CSV** để tương thích với phần mềm gia phả khác.
4. **Bổ sung kiểm thử hiệu năng** với cây 500–1000 người và kiểm thử tải song song.
5. **Thêm đơn vị đo lường thế hệ** và ngày sinh (hiện chỉ có năm sinh) — cần nới
   `full_name`/`birth_year` và cân nhắc lại ràng buộc `CHECK`.

**Dài hạn:**

6. **Đăng nhập & phân quyền** (quản trị viên / người xem / người sửa).
7. **Chuyển sang kiến trúc nhiều tầng có giao diện web** (Spring Boot + REST + frontend)
   nếu đề tài mở rộng — hiện tại kiến trúc phân tầng đã sẵn sàng cho bước này.
8. **Cải thiện thuật toán bố trí**: hỗ trợ cây gia phả rất rộng bằng thuật toán
   *Reingold–Tilford* / *Sugiyama* thay cho cách canh giữa tuyến tính hiện tại.
9. **Ghi nhật thay đổi (audit trail)**: lưu lại lịch sử sửa/xóa để truy vết nguồn gốc dữ liệu.
10. **Chia sẻ gia phả qua mã QR / link** để người thân xem cây gia phả trên điện thoại.

---

## 7. TÀI LIỆU THAM KHẢO

### 7.1. Tài liệu của dự án

| Tài liệu | Vị trí |
|---|---|
| Hướng dẫn sử dụng & kiến trúc (README) | `README.md` |
| Hướng dẫn dành cho AI Agent (source of truth kiến trúc) | `AGENTS.md` |
| Định nghĩa CSDL (schema, constraint, trigger, dữ liệu mẫu) | `database/schema.sql` |
| Bộ dữ liệu mẫu 30 người / 5 thế hệ | `test-data/sample-data.sql` |
| Bảng test case chi tiết (22 case) | `test-data/test-cases.md` |
| Mã nguồn sơ đồ PlantUML (18 file `.puml`) | `docs/uml/*.puml` |
| Ảnh sơ đồ render (18 file `.png`) | `docs/uml/*.png` |
| Sơ đồ tuần tự — chỉnh sửa thành viên (luồng cập nhật nguyên tử) | `docs/uml/sequence-edit-person.puml` |
| Tài liệu ôn bảo vệ (20 câu hỏi) | `docs/report/bao-ve-cheat-sheet.md` |
| Kịch bản demo 5 phút | `docs/report/kich-ban-demo.md` |

### 7.2. Tài liệu công nghệ

| Tài liệu | Nguồn |
|---|---|
| Java SE 25 — Java Platform Module System | <https://docs.oracle.com/en/java/javase/25/jfapi/> |
| JavaFX 23 — User Interface, Scene Graph, `javafx.print` | <https://openjfx.io/javadoc/23/> |
| PlantUML — Activity Diagram, Sequence Diagram, Class Diagram | <https://plantuml.com/> |
| MySQL 8.0 — Reference Manual (CHECK constraint, Trigger, InnoDB FK) | <https://dev.mysql.com/doc/refman/8.0/en/> |
| MariaDB — Knowledge Base (CHECK constraint, Trigger) | <https://mariadb.com/kb/en/> |
| MySQL Connector/J 9.2.0 | <https://dev.mysql.com/doc/connector-j/9.2/en/> |
| Apache Maven 3.9 User Guide | <https://maven.apache.org/guides/index.html> |
| JUnit 5 User Guide | <https://junit.org/junit5/docs/current/user-guide/> |
| Maven Surefire Plugin 3.2.5 | <https://maven.apache.org/surefire/maven-surefire-plugin/> |

### 7.3. Giáo trình & sách tham khảo

- Giáo trình **Lập trình hướng đối tượng (OOP)** — bài giảng và đề cương môn học.
- **Effective Java** — Joshua Bloch. *Item 1: Consider static factory methods*,
  *Item 63: Prefer interfaces to abstract classes*.
- **Design Patterns: Elements of Reusable Object-Oriented Software** — Erich Gamma,
  Richard Helm, Ralph Johnson, John Vlissides (GoF).
- **Refactoring: Improving the Design of Existing Code** — Martin Fowler.
  *Chapter 1: Refactoring — hỗ trợ giải thích cơ chế tách phương thức dài thành
  các phương thức nhỏ trong `FamilyTreeService`.*
- **Head First Design Patterns** — Eric Freeman, Elisabeth Freeman.

---

## PHỤ LỤC — DANH MỤC SƠ ĐỒ UML

Tất cả sơ đồ nằm tại `docs/uml/` (mã nguồn `.puml` + ảnh render `.png`) —
**18 sơ đồ** (18 file `.puml` + 18 file `.png`).

| # | Sơ đồ | File `.puml` | Ảnh `.png` |
|---|---|---|---|
| 1 | Lớp — tổng quan | `class-diagram.puml` | `class-diagram.png` |
| 2 | Lớp (a) — model + exception | `class-diagram-model-exception.puml` | `class-diagram-model-exception.png` |
| 3 | Lớp (b) — service + repository + database | `class-diagram-service-repository.puml` | `class-diagram-service-repository.png` |
| 4 | Lớp (c) — view + controller | `class-diagram-view-controller.puml` | `class-diagram-view-controller.png` |
| 5 | Thành phần | `component-diagram.puml` | `component-diagram.png` |
| 6 | Triển khai | `deployment-diagram.puml` | `deployment-diagram.png` |
| 7 | Ca sử dụng | `use-case.puml` | `use-case.png` |
| 8 | Tuần tự — thêm người | `sequence-add-person.puml` | `sequence-add-person.png` |
| 9 | Tuần tự — chỉnh sửa thành viên | `sequence-edit-person.puml` | `sequence-edit-person.png` |
| 10 | Tuần tự — gán cha/mẹ | `sequence-assign-parent.puml` | `sequence-assign-parent.png` |
| 11 | Tuần tự — xem cây gia phả | `sequence-family-tree.puml` | `sequence-family-tree.png` |
| 12 | Tuần tự — xuất PNG / In | `sequence-export-print.puml` | `sequence-export-print.png` |
| 13 | Tuần tự — chọn & lưu ảnh đại diện | `sequence-photo-avatar.puml` | `sequence-photo-avatar.png` |
| 14 | Hoạt động — thêm người | `activity-add-person.puml` | `activity-add-person.png` |
| 15 | Hoạt động — gán cha/mẹ | `activity-assign-parent.puml` | `activity-assign-parent.png` |
| 16 | Hoạt động — xem cây gia phả | `activity-family-tree.puml` | `activity-family-tree.png` |
| 17 | Hoạt động — xuất PNG / In | `activity-export-print.puml` | `activity-export-print.png` |
| 18 | Hoạt động — chọn & lưu ảnh đại diện | `activity-photo-avatar.puml` | `activity-photo-avatar.png` |

**Render lại toàn bộ sơ đồ** (cần PlantUML + GraphViz `dot`):

```bash
cd docs/uml
java -jar plantuml.jar -verbose *.puml
```

Chỉ kiểm tra cú pháp, không render:

```bash
java -jar plantuml.jar -checkonly *.puml
```
