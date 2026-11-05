# TÀI LIỆU ÔN BẢO VỆ — HỆ THỐNG QUẢN LÝ SƠ ĐỒ GIA PHẢ

> Tài liệu dùng để ôn tập và trả lời câu hỏi giảng viên.
> Mọi tham chiếu đều trỏ tới **đúng tên file** trong `src/main/java/` và **đúng tên file**
> trong `docs/uml/`.
>
> **Bí kíp:** trước khi trả lời, luôn nói rõ **"file nào, hàm nào"** — đó là điểm
> giảng viên muốn kiểm tra.

---

## MỤC LỤC

1. [Bản đồ nhanh dự án](#1-bản-đồ-nhanh-dự-án)
2. [Luồng chạy từng chức năng chính](#2-luồng-chạy-từng-chức-năng-chính)
3. [Vì sao chọn từng quyết định thiết kế](#3-vì-sao-chọn-từng-quyết-định-thiết-kế)
4. [20 câu hỏi giảng viên có thể hỏi](#4-20-câu-hỏi-giảng-viên-có-thể-hỏi)
5. [Bảng tra nhanh tên hàm / tên lớp](#5-bảng-tra-nhanh-tên-hàm--tên-lớp)
6. [Các con số cần nhớ](#6-các-con-số-cần-nhớ)

---

## 1. BẢN ĐỒ NHANH DỰ ÁN

### 1.1. Cây package

```
src/main/java/com/giapha/
├── Main.java                              # Entry point JavaFX + Composition Root
├── model/
│   ├── Gender.java                        # enum MALE, FEMALE
│   └── Person.java                        # Entity: id, fullName, birthYear, gender, photoPath, father, mother
├── exception/                             # 7 lớp, đều extends RuntimeException
│   ├── SelfParentException.java           # BR1
│   ├── SameParentException.java            # BR2
│   ├── InvalidParentGenderException.java  # BR6
│   ├── CycleDetectedException.java         # BR3
│   ├── PersonInUseException.java           # BR4
│   ├── PersonNotFoundException.java
│   └── InvalidBirthYearException.java
├── repository/
│   ├── PersonRepository.java              # Interface (abstraction)
│   └── JdbcPersonRepository.java          # Cài đặt JDBC thuần
├── database/
│   └── DatabaseConnection.java            # Static Factory — cấp Connection mới
├── service/
│   └── FamilyTreeService.java             # Business layer — BR1..BR6
├── controller/
│   └── PersonController.java              # Điều phối View <-> Service
├── util/
│   ├── ImageManager.java                  # Utility tĩnh — ảnh đại diện
│   └── PngWriter.java                     # Utility tĩnh — ghi PNG bằng java.util.zip
└── view/
    ├── PeopleListView.java                # Danh sách thành viên (TableView + FilteredList)
    ├── PersonFormView.java                # Form thêm/sửa
    ├── PersonDetailView.java              # Hồ sơ chi tiết
    ├── FamilyTreeView.java                # Màn hình cây gia phả (TabPane + toolbar)
    ├── FamilyTreeDiagram.java             # Sơ đồ 2D (Pane + Line) — component JavaFX thuần
    └── PersonAvatar.java                  # Component avatar bo tròn + fallback
```

### 1.2. Luồng phụ thuộc một chiều

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

**Câu hỏi kinh điển:** "Tại sao Service không gọi thẳng `JdbcPersonRepository`?"
→ Vì `FamilyTreeService` giữ trường `private final PersonRepository personRepository`
và **không import** `JdbcPersonRepository`, `DatabaseConnection`, `java.sql.*`.
Đây là **Dependency Inversion (SOLID – D)**: tầng cao phụ thuộc abstraction,
không phụ thuộc chi tiết. Nhờ đó test có thể thay repository bằng
`TestPersonRepository` mà không sửa Service.

### 1.3. Sơ đồ UML liên quan

| Chủ đề | File trong `docs/uml/` |
|---|---|
| Ca sử dụng | `use-case.puml` |
| Lớp | `class-diagram.puml` + `class-diagram-model-exception.puml` + `class-diagram-service-repository.puml` + `class-diagram-view-controller.puml` |
| Thành phần | `component-diagram.puml` |
| Triển khai | `deployment-diagram.puml` |
| Tuần tự | `sequence-add-person.puml`, `sequence-assign-parent.puml`, `sequence-family-tree.puml`, `sequence-export-print.puml`, `sequence-photo-avatar.puml` |
| Hoạt động | `activity-add-person.puml`, `activity-assign-parent.puml`, `activity-family-tree.puml`, `activity-export-print.puml`, `activity-photo-avatar.puml` |

---

## 2. LUỒNG CHẠY TỪNG CHỨC NĂNG CHÍNH

### 2.1. Thêm một người mới

**Sơ đồ:** `docs/uml/sequence-add-person.puml` + `docs/uml/activity-add-person.puml`

| Bước | File & hàm | Việc làm |
|---|---|---|
| 1 | `view/PersonFormView.java` → `handleSave()` | Validate ở tầng View: họ tên rỗng, năm sinh rỗng/không phải số, giới tính null → `showAlert(...)` và dừng |
| 2 | `view/PersonFormView.java` → `handleSave()` | Tạo `Person`, `setFather(newFather)`, `setMother(newMother)` → `controller.handleAdd(newPerson)` |
| 3 | `controller/PersonController.java` → `handleAdd(Person)` | Gọi `familyTreeService.addPerson(person)` trong `try/catch` |
| 4 | `service/FamilyTreeService.java` → `addPerson(Person)` | `validatePerson(person)` → kiểm tra BR2 → kiểm tra BR6 (cha, mẹ) → `personRepository.save(person)` → `person.setId(generatedId)` |
| 5 | `repository/JdbcPersonRepository.java` → `save(Person)` | `INSERT INTO person (...)` với `Statement.RETURN_GENERATED_KEYS` → trả `generated id` |
| 6 | `controller/PersonController.java` → `handleAdd(Person)` | Nếu đã chọn ảnh: `persistSelectedImage(sourcePath, added.getId())` → `ImageManager.copyImageToAppDirectory(...)` → `familyTreeService.updatePerson(added)` |
| 7 | `controller/PersonController.java` | `showInfoAlert("Thành công", ...)` → `navigateToPeopleList.run()` → quay về danh sách |

**Điểm quan trọng cần nhấn mạnh:** ảnh được copy **sau khi** đã có `id` do CSDL sinh,
vì tên file là `person_<id>.<đuôi>`. Vì vậy `handleAdd()` phải làm **2 lần ghi DB**
(INSERT rồi UPDATE đường dẫn ảnh) — đây là điểm hỏi hay.

**Sơ đồ Activity** (`activity-add-person.puml`) khớp 1-1 với thứ tự trong
`addPerson()`: validate → BR2 → BR6 (cha) → BR6 (mẹ) → BR5 (cảnh báo) → lưu ảnh →
ghi DB.

### 2.2. Gán cha / gán mẹ

**Sơ đồ:** `docs/uml/sequence-assign-parent.puml` + `docs/uml/activity-assign-parent.puml`

Hàm: `FamilyTreeService.assignFather(int childId, int fatherId)`
và `FamilyTreeService.assignMother(int childId, int motherId)`.

**Thứ tự kiểm tra (đúng thứ tự trong code):**

```
BR1 (SelfParent) → BR2 (SameParent) → BR6 (InvalidParentGender) → BR3 (Cycle)
```

| # | Kiểm tra | Điều kiện | Exception ném ra |
|---|---|---|---|
| 0 | Tồn tại | `getPersonById(childId)`, `getPersonById(parentId)` | `PersonNotFoundException` |
| 1 | **BR1** | `child.getId() == parent.getId()` | `SelfParentException` |
| 2 | **BR2** | `child.getMother().getId() == father.getId()` (hoặc ngược lại với `assignMother`) | `SameParentException` |
| 3 | **BR6** | `father.getGender() != Gender.MALE` (hoặc `mother.getGender() != Gender.FEMALE`) | `InvalidParentGenderException.forRole(...)` |
| 4 | **BR3** | `checkCycle(childId, parentId)` trả `true` | `CycleDetectedException` |
| 5 | Ghi | `child.setFather(father)` → `personRepository.update(child)` | — |

**Vì sao đúng thứ tự này?** Để thông báo lỗi chỉ ra **đúng nguyên nhân gốc**.
Ví dụ nếu kiểm tra BR6 trước BR2: người dùng chọn một người nữ làm cha, và người
đó đã là mẹ hiện tại → cả 2 lỗi đúng, nhưng thông báo "sai giới tính" ít hữu ích
hơn "cha và mẹ là cùng một người". Ưu tiên BR1 trước vì BR1 là lỗi "tuyệt đối"
(bất kỳ id nào cũng vi phạm), còn BR3 để cuối vì nó **đắt nhất** (phải duyệt đồ thị).

**Controller bắt lỗi:** `PersonController.handleAssignFather(int, int)` có `catch`
riêng cho `SelfParentException`, `CycleDetectedException`, `PersonNotFoundException`,
`InvalidParentGenderException`, `SameParentException`, `IllegalArgumentException`,
`Exception` — mỗi loại một thông báo tiếng Việt khác nhau.

**Gỡ quan hệ:** `FamilyTreeService.clearFather(int childId)` / `clearMother(int childId)`
→ `child.setFather(null)` → `personRepository.update(child)`.

### 2.3. Phát hiện chu trình (BR3) bằng DFS

**File/hàm:** `service/FamilyTreeService.java` → `private boolean checkCycle(int childId, int candidateParentId)`

**Ý tưởng:** Nếu `childId` là **tổ tiên** của `candidateParentId`, thì gán
`candidateParentId` làm cha/mẹ của `childId` sẽ khép kín một vòng lặp. Vì vậy ta
**duyệt ngược** từ `candidateParentId` đi lên các thế hệ tổ tiên; nếu gặp `childId`
thì có chu trình.

```java
private boolean checkCycle(int childId, int candidateParentId) {
    if (childId == candidateParentId) {
        return true;
    }

    Set<Integer> visited = new HashSet<>();
    Deque<Integer> stack = new ArrayDeque<>();
    stack.push(candidateParentId);
    visited.add(candidateParentId);

    while (!stack.isEmpty()) {
        int currentId = stack.pop();
        if (currentId == childId) {
            return true;
        }

        Person current = personRepository.findById(currentId);
        if (current != null) {
            if (current.getFather() != null) {
                int fId = current.getFather().getId();
                if (fId == childId) {
                    return true;
                }
                if (visited.add(fId)) {
                    stack.push(fId);
                }
            }
            if (current.getMother() != null) {
                int mId = current.getMother().getId();
                if (mId == childId) {
                    return true;
                }
                if (visited.add(mId)) {
                    stack.push(mId);
                }
            }
        }
    }

    return false;
}
```

**Giải thích từng dòng (để trả lời khi bị hỏi):**

| Dòng | Ý nghĩa |
|---|---|
| `if (childId == candidateParentId) return true;` | Trường hợp BR1 — cũng là chu trình độ dài 1. Kiểm tra sớm để không cần duyệt. |
| `Set<Integer> visited` | **Chống lặp vô hạn.** Vì dữ liệu có thể đã chứa chu trình (nếu ai đó ghi trực tiếp bằng SQL), nếu không có `visited` thì vòng lặp while sẽ không bao giờ kết thúc. |
| `Deque<Integer> stack` + `stack.push`/`stack.pop` | Dùng **stack (LIFO)** thay vì queue → đây chính là **DFS** (duyệt chiều sâu). `push`/`pop` trên đầu `Deque` là O(1). |
| `visited.add(fId)` trả `false` nếu đã thấy | Đây là cách kiểm tra "đã thăm" **và đánh dấu** cùng lúc — trả về `false` nghĩa là id đã có trong set → không push lại. |
| `if (fId == childId) return true;` | Tối ưu nhỏ: phát hiện chu trình **mà không cần push** `childId` vào stack. |
| `personRepository.findById(currentId)` | Duyệt trên **object graph** — mỗi node là một `Person` có sẵn `getFather()` / `getMother()`. |
| `return false` | Không gặp `childId` → gán an toàn. |

**Độ phức tạp:** `O(V + E)` với `V` = số tổ tiên của `candidateParentId`,
`E` = số cạnh trong nhánh tổ tiên đó. Với `V` nhỏ (gia phả thực tế vài chục
người) thì rất nhanh.

**Vì sao dùng `visited`?** Trả lời: vì `checkCycle` là **hàm phòng thủ** — nó
phải trả lời đúng **ngay cả khi dữ liệu đã bị hỏng** (chu trình được tạo bởi
SQL thủ công). Nếu thiếu `visited`, chương trình sẽ treo.

**Vì sao không bảo vệ BR3 ở tầng CSDL?** Vì MySQL/MariaDB không cho phép
`CHECK` constraint tham chiếu tới bảng khác — không thể viết điều kiện
"`id` này không phải hậu duệ của `id` kia" bằng một biểu thức `CHECK` đơn giản.
BR3 là quy tắc **duy nhất** chỉ được bảo vệ ở tầng Service. (BR1, BR2, BR4 có
ràng buộc tương đương ở CSDL: `trg_check_not_self_parent_update`,
`chk_father_not_mother`, `ON DELETE RESTRICT`.)

### 2.4. Truy vấn tổ tiên và hậu duệ (BFS)

**File/hàm:** `service/FamilyTreeService.java`

#### `getAncestors(int id)` — tổ tiên, dùng **BFS**

```java
Queue<Integer> queue = new ArrayDeque<>();
queue.add(focus.getFather().getId());
queue.add(focus.getMother().getId());

while (!queue.isEmpty()) {
    int curId = queue.poll();
    if (visited.add(curId)) {
        Person p = personRepository.findById(curId);
        if (p != null) {
            ancestors.add(p);
            if (p.getFather() != null) queue.add(p.getFather().getId());
            if (p.getMother() != null) queue.add(p.getMother().getId());
        }
    }
}
```

- Dùng `Queue` (FIFO) → **BFS**. Vì người trung tâm là giữa, BFS đi ra theo
  **vòng** từ gần ra xa → thuật toán "bánh kính" (tangent circles) cho cây
  gia phả, đúng như cách vẽ sơ đồ truyền thống.
- `visited` khởi tạo với `focus.getId()` để **không tính chính người đó** vào
  danh sách tổ tiên.
- Kết quả: `getAncestors(23)` trên bộ dữ liệu mẫu trả về **8 người** (TC-04).

#### `getDescendants(int id)` — hậu duệ, dùng **BFS trên đồ thị đảo**

```java
List<Person> all = personRepository.findAll();
Map<Integer, List<Person>> childrenMap = new HashMap<>();
for (Person p : all) {
    if (p.getFather() != null) childrenMap.computeIfAbsent(p.getFather().getId(), k -> new ArrayList<>()).add(p);
    if (p.getMother() != null) childrenMap.computeIfAbsent(p.getMother().getId(), k -> new ArrayList<>()).add(p);
}

Queue<Integer> queue = new ArrayDeque<>();
queue.add(focus.getId());
while (!queue.isEmpty()) {
    int curId = queue.poll();
    List<Person> children = childrenMap.getOrDefault(curId, Collections.emptyList());
    for (Person child : children) {
        if (visited.add(child.getId())) {
            descendants.add(child);
            queue.add(child.getId());
        }
    }
}
```

- Bản chất là **duyệt đồ thị theo hướng cha→con**, tức là BFS trên đồ thị đảo
  của quan hệ phả hệ.
- Khác `getAncestors()` ở chỗ: dựng **bản đồ con** (`childrenMap`) một lần từ
  `findAll()` thay vì gọi `findById()` cho mỗi node. Lý do: hướng cha→con **không
  thể đi ngược trên object graph** — `Person` chỉ giữ tham chiếu tới cha/mẹ,
  không giữ danh sách con. Nên phải dựng chỉ mục `childrenMap` trong bộ nhớ.
- Kết quả: `getDescendants(1)` trên bộ dữ liệu mẫu trả về **18 người** (TC-03).

**So sánh DFS (BR3) vs BFS (tổ tiên/hậu duệ):**

| | `checkCycle` (BR3) | `getAncestors` / `getDescendants` |
|---|---|---|
| Cấu trúc dữ liệu | `Deque` + `push`/`pop` | `Queue` + `add`/`poll` |
| Kiểu duyệt | DFS (chiều sâu) | BFS (chiều rộng) |
| Mục đích | Chỉ cần biết "có tới được `childId` không" | Cần danh sách đầy đủ theo thứ tự từ gần ra xa để bố trí sơ đồ |
| Hướng | Đi **ngược** (tổ tiên) | `getAncestors` đi ngược; `getDescendants` đi xuống |

### 2.5. Bố trí sơ đồ 2D

**File:** `view/FamilyTreeDiagram.java` — component JavaFX thuần, **không** gọi
Service/Repository.

**Điểm vào:** `FamilyTreeDiagram.setData(List<Person> allPersons, Person focusPerson)`
→ gọi `rebuild()`.

`rebuild()` thực hiện 5 bước:

```
buildModel()      → buildUnits()      → collectAncestorUnits()/collectDescendantUnits()
   → layoutAll()  → assignPixelPositions()  → drawEdges() + createNodeCard()
```

#### Bước 1 — `buildModel()`: dựng tập node hiển thị

Duyệt BFS từ người trung tâm (`Deque` + `removeFirst()`), đi lên qua cha/mẹ
(`current.generation - 1`) và đi xuống qua con (`current.generation + 1`).
Khi gặp con, hàm còn thêm **vợ/chồng** của người trung tâm trong cặp cha–mẹ
(`otherParent(child, current.person)`) để cây không bị thiếu "nửa gia đình".

**`generation`** = số thế hệ so với người trung tâm: tổ tiên là `-1, -2, ...`,
hậu duệ là `+1, +2, ...`. Đây là giá trị quyết định **hàng** trong sơ đồ.

#### Bước 2 — `buildUnits()`: gom khối gia đình

Một `Unit` là **khối gia đình cha + mẹ** cùng sinh chung một con. Hàm duyệt các
node, nếu node có đủ cha và mẹ **và cả cha lẫn mẹ đều có trong tập hiển thị**
→ gom 2 node đó vào một `Unit`. Node còn lại thành `Unit` độc lập.

Sau đó liên kết `Unit.above` / `Unit.below` bằng quan hệ cha/mẹ–con.

**Vì sao cần Unit?** Nếu vẽ từng node riêng, cha và mẹ sẽ bị tách rời và con sẽ
không nằm giữa. Gom thành khối cho phép **con được canh giữa giữa hai vị trí
cha/mẹ** — đúng cách vẽ sơ đồ gia phả truyền thống.

#### Bước 3 — `collectAncestorUnits()` / `collectDescendantUnits()`

Hai hàm DFS (`Deque` + `push`/`pop`) trên đồ thị `Unit.above` / `Unit.below`,
trả về tập id các `Unit` thuộc nhánh tổ tiên / hậu duệ. Tập này được dùng để
**giới hạn phạm vi bố trí**: mỗi cây chỉ bố trí các unit thuộc nhánh của nó.

#### Bước 4 — `layoutAll()`: thuật toán "cha canh giữa trên tập con"

Hàm `layoutTree(Unit node, Set<Integer> allowed, double[] counter)`:

```java
if (kids.isEmpty()) {
    node.start = counter[0];
    counter[0] += node.size();
    return;
}

double first = 0, last = 0;
for (int i = 0; i < kids.size(); i++) {
    layoutTree(kids.get(i), allowed, counter);
    if (i == 0) first = kids.get(i).start;
    last = kids.get(i).start + kids.get(i).size() - 1;
}
node.start = (first + last) / 2.0 - (node.size() - 1) / 2.0;
```

- **Cột** (`start`) được cấp phát như một danh sách tuyến tính: mỗi lá chiếm
  `node.size()` cột liên tiếp (bộ đếm `counter` chia sẻ giữa các cây).
- Node cha nhận **cột trung bình** giữa lá đầu tiên và lá cuối cùng
  (`(first + last) / 2`), trừ đi nửa chiều rộng của chính nó
  (`(node.size() - 1) / 2`) → cha nằm **chính giữa** tập con.
- `layoutAll()` gọi `layoutTree()` 2 lần: một lần cho nhánh hậu duệ (gốc =
  `focusUnit`), một lần cho từng gốc của nhánh tổ tiên.
- Sau đó **dịch cả nhánh tổ tiên** sao cho khối ngay trên người trung tâm thẳng
  hàng với người trung tâm:
  ```java
  double shift = focusUnit.start - anchor.start;
  for (Unit u : units.values()) {
      if (ancestorIds.contains(u.id)) u.start += shift;
  }
  ```

#### Bước 5 — `assignPixelPositions()`: cột/hàng → pixel

```java
double columnWidth = NODE_WIDTH + H_GAP;   // 150 + 22 = 172
double rowHeight   = NODE_HEIGHT + V_GAP;  // 72 + 62 = 134

double offset = -minColumn * columnWidth + MARGIN;   // MARGIN = 40
n.pixelX = column * columnWidth + offset;
n.pixelY = (n.generation - minGen) * rowHeight + MARGIN;
```

Đồng thời tính `baseWidth` / `baseHeight` = **kích thước nội dung sơ đồ ở zoom 1.0** —
đây là giá trị dùng cho `fitToView()` và `renderFullImage()`.

#### Bước 6 — `drawEdges()` + `createNodeCard()`

- `drawEdges()` vẽ đường kẻ **ở mức cá nhân**: mỗi cha/mẹ nối xuống **đúng con
  của họ** (không vẽ theo khối gia đình) — tránh nối nhầm vợ/chồng của người
  trung tâm với cha/mẹ của họ.
- `createNodeCard()` tạo card cho mỗi node: `PersonAvatar` + họ tên + năm sinh +
  badge giới tính. Node của người trung tâm được tô viền đậm màu xanh.
- Thứ tự thêm vào scene: **đường kẻ trước, node sau** → node nằm đè lên
  đường kẻ.

#### Tương tác (zoom / pan)

| Hàm | Cơ chế |
|---|---|
| `zoomAround(localX, localY, factor)` | Giữ nguyên vị trí con trỏ: tính toạ độ nội dung trước, scale lại, rồi bù `translateX/Y`. Giới hạn `MIN_ZOOM = 0.25` … `MAX_ZOOM = 2.5`. |
| `zoomBy(factor)` | Nhân zoom, lấy tâm component làm gốc (nút `−` / `+` dùng hệ số `1/1.2` và `1.2`). |
| `resetZoom()` | Zoom về 1.0, translate về (0,0), rồi `fitToView()`. |
| `fitToView()` | `zoom = min((getWidth()-20)/baseWidth, (getHeight()-20)/baseHeight)`, canh giữa bằng `translateX/Y`. |

### 2.6. Xuất PNG

**Sơ đồ:** `docs/uml/sequence-export-print.puml` + `docs/uml/activity-export-print.puml`

**File/hàm:** `view/FamilyTreeView.java` → `handleExportPng()`;
`view/FamilyTreeDiagram.java` → `renderFullImage()`;
`util/PngWriter.java` → `write(WritableImage, File)`, `toByteArray(WritableImage)`,
`encodePng(int, int, byte[])`.

**Luồng:**

1. `diagram.renderFullImage()` → trả `WritableImage` (hoặc `null` nếu sơ đồ rỗng).
2. `FileChooser` (bộ lọc `*.png`) với tên file mặc định từ `defaultExportFileName()`
   → `gia_pha_<Tên>.png` (ký tự không phải chữ/số bị thay bằng `_`).
3. `PngWriter.write(image, file)`.
4. Báo `Alert` "Xuất PNG thành công" kèm đường dẫn tuyệt đối.

**`renderFullImage()` — mấu chốt của tính năng:**

```java
double oldScaleX = content.getScaleX();       // lưu trạng thái cũ
// ...
content.setScaleX(1.0);
content.setScaleY(1.0);
content.setTranslateX(0);
content.setTranslateY(0);

try {
    SnapshotParameters params = new SnapshotParameters();
    params.setViewport(new Rectangle2D(0, 0, baseWidth, baseHeight));  // viewport = KÍCH THƯỚC NỘI DUNG
    params.setFill(Color.WHITE);
    image = content.snapshot(params, null);
} finally {
    // khôi phục lại zoom/translate cũ
}
```

Ba ý tưởng quan trọng:
1. **Tạm đặt zoom = 1.0** → ảnh không bị phụ thuộc vào mức zoom người dùng đang đặt.
2. **`setViewport` đúng bằng `baseWidth × baseHeight`** → chụp **TOÀN BỘ** sơ đồ,
   không bị cắt theo khung nhìn hiện tại (kể cả phần đang bị thu nhỏ ra ngoài).
3. **`finally` khôi phục trạng thái** → người dùng không bị mất góc nhìn sau khi xuất.

**`PngWriter` — bộ ghi PNG thuần bằng `java.util.zip`:**

| Hàm | Việc |
|---|---|
| `write(WritableImage, File)` | Mở `FileOutputStream` rồi gọi `write(image, out)` |
| `write(WritableImage, OutputStream)` | `toByteArray(image)` → `out.write(png)` |
| `toByteArray(WritableImage)` | Kiểm tra kích thước > 0 → `extractRawPixels` → `encodePng` |
| `extractRawPixels(image, w, h)` | Duyệt `PixelReader.getArgb(x, y)`, tách ra R,G,B,A; **mỗi dòng bắt đầu bằng 1 byte filter = 0** (filter type "None") |
| `encodePng(w, h, raw)` | Kiểm tra `raw.length == height * (1 + width * 4)` → ghi chữ ký + IHDR + IDAT + IEND |
| `deflate(byte[])` | `java.util.zip.Deflater` → nén zlib |
| `writeChunk(out, type, data)` | length (4 byte big-endian) + type (ASCII) + data + **CRC32** (`crc.update(typeBytes); crc.update(data)`) |
| `writeInt(out, value)` | Ghi số nguyên 4 byte big-endian |

**Cấu trúc file PNG sinh ra (chuẩn):**

```
[8 byte chữ ký] 89 50 4E 47 0D 0A 1A 0A
[IHDR] width(4) height(4) bitDepth(1)=8 colorType(1)=6(RGBA)
       compression(1)=0 filter(1)=0 interlace(1)=0  + CRC32
[IDAT] dữ liệu pixel nén zlib (Deflater)              + CRC32
[IEND] rỗng                                            + CRC32
```

### 2.7. Lưu ảnh đại diện

**Sơ đồ:** `docs/uml/sequence-photo-avatar.puml` + `docs/uml/activity-photo-avatar.puml`

**File/hàm:** `view/PersonFormView.java` → `handleChoosePhoto()`;
`controller/PersonController.java` → `persistSelectedImage(String, int)`;
`util/ImageManager.java` → `isAllowedImage(String)`, `getExtension(String)`,
`getImageDirectory()`, `copyImageToAppDirectory(String, int)`, `deleteImageQuietly(String)`.

**Luồng:**

| Bước | Nơi làm | Việc |
|---|---|---|
| 1 | `PersonFormView.handleChoosePhoto()` | `FileChooser` với bộ lọc `ImageManager.IMAGE_FILTER` = `Ảnh đại diện (*.png, *.jpg, *.jpeg)`. |
| 2 | `PersonFormView.handleChoosePhoto()` | `ImageManager.isAllowedImage(chosen.getName())` → nếu sai định dạng thì `showAlert("Định dạng ảnh không hợp lệ...")` và dừng. |
| 3 | `PersonFormView` | Lưu `selectedPhotoSourcePath = chosen.getAbsolutePath()` — **View chỉ giữ đường dẫn nguồn, KHÔNG copy file**. |
| 4 | `PersonController.persistSelectedImage(sourcePath, personId)` | Kiểm tra `isAllowedImage` lần nữa → `ImageManager.copyImageToAppDirectory(sourcePath, personId)`. |
| 5 | `ImageManager.copyImageToAppDirectory()` | `Files.copy(source, target, REPLACE_EXISTING)` với `target = images/person_<id>.<đuôi>` → trả về **đường dẫn tương đối** `"images/person_<id>.<đuôi>"`. |
| 6 | `PersonController` | Gán `added.setPhotoPath(newPhotoPath)` → `familyTreeService.updatePerson(added)` → lưu vào DB. |

**Quy ước đường dẫn ảnh:**

- Thư mục: `System.getProperty("user.dir")` + `/images` (tự tạo nếu chưa có) —
  `ImageManager.getImageDirectory()`.
- Tên file: `person_<id>.<đuôi>` — đặt theo id để ảnh mới **ghi đè** ảnh cũ
  (`REPLACE_EXISTING`), không sinh file rác.
- Trong DB chỉ lưu **đường dẫn tương đối** (`images/person_7.png`) → CSDL không
  phụ thuộc vị trí cài đặt.
- Đánh đổi: ứng dụng **phải chạy từ thư mục gốc dự án**, nếu không ảnh sẽ không
  tìm thấy khi mở lại.
- `ImageManager.deleteImageQuietly(String)` — xóa file ảnh, **không ném exception**
  (thao tác phụ trợ), và chỉ xóa file nằm trong thư mục `images/` (kiểm tra
  `target.startsWith(imageDir)`) để tránh xóa nhầm file ngoài.

**`PersonAvatar` — component hiển thị ảnh dùng chung:**

| Hàm | Vai trò |
|---|---|
| `PersonAvatar(Person, int size)` | Dựng placeholder trước, rồi thử nạp ảnh thật |
| `PersonAvatar(Person, int size, File imageFile)` | Dùng cho khung xem trước ảnh vừa chọn trong form |
| `buildPlaceholder()` | Nền tròn + chữ cái đầu của họ tên, màu theo giới tính |
| `loadImageIfAvailable()` | `PersonAvatar.resolveImageFile(photoPath)` → nếu file tồn tại & đọc được thì nạp |
| `attachImage(Image)` | Gắn listener `progressProperty` (nạp xong → `applyImage`) và `errorProperty` (lỗi → `showPlaceholder`) |
| `applyImage(Image)` | Tạo `ImageView`, bo tròn bằng `Circle` clip, ẩn placeholder |
| `showPlaceholder()` | Quay lại chữ cái đầu |
| `resolveImageFile(String)` | **static** — thử đường dẫn tương đối trước, rồi đường dẫn tuyệt đối (dự phòng dữ liệu cũ) |
| `extractInitial()` | Lấy chữ cái đầu của phần cuối họ tên (ví dụ "Nguyễn Văn An" → "A") |

**Cơ chế fallback (điểm hỏi hay):** ảnh được nạp ở chế độ **background loading**
(`new Image(uri, true)`). Nếu file hỏng → `image.isError()` → giữ placeholder.
**Không bao giờ** ném exception ra giao diện — mọi lỗi đều bị bọc trong
`try/catch (Exception)`.

### 2.8. In ấn

**File/hàm:** `view/FamilyTreeView.java` → `handlePrint()`, `buildPrintableNode(PrinterJob)`,
`confirmPrint(PrinterJob)`.

| Bước | Việc |
|---|---|
| 1 | `diagram.renderFullImage()` → nếu `null` → Alert "Không thể in" (sơ đồ trống) |
| 2 | `PrinterJob.createPrinterJob()` → nếu `null` → Alert "Không tìm thấy máy in" |
| 3 | `confirmPrint(job)`: thử `job.showPrintDialog(...)` (hộp thoại hệ thống của JavaFX); nếu nền tảng không hỗ trợ (`catch (Throwable)`) → hiện `Dialog` nội bộ "In sơ đồ cây gia phả?" |
| 4 | `buildPrintableNode(job)`: bọc ảnh trong `ImageView` → `StackPane` có `prefWidth/prefHeight` bằng khổ giấy lấy từ `job.getJobSettings().getPageLayout()`, nền trắng, padding 24 |
| 5 | `job.printPage(printable)` → trả `true` → Alert "Đã gửi lệnh in"; `false` → Alert "Không in được" |
| 6 | `catch (RuntimeException)` → Alert "In thất bại" |

---

## 3. VÌ SAO CHỌN TỪNG QUYẾT ĐỊNH THIẾT KẾ

### 3.1. Vì sao tự viết `PngWriter` thay vì dùng `javax.imageio` / `javafx.swing`?

Đây là câu hỏi **quan trọng nhất** trong phần thiết kế. Lý do:

1. **Ranh giới module (JPMS).** Dự án dùng Java Platform Module System với
   `module-info.java`:
   ```java
   module com.giapha {
       requires javafx.controls;
       requires javafx.graphics;
       requires javafx.fxml;
       requires java.sql;
       ...
   }
   ```
   - `javax.imageio` thuộc module **`java.desktop`**. Dùng nó sẽ phải thêm
     `requires java.desktop;` — kéo theo toàn bộ hệ sinh thái AWT/Swing (rất nặng)
     vào một ứng dụng thuần JavaFX.
   - `javafx.swing` (`SwingFXUtils`) là **module riêng** `javafx.swing`, cũng phải
     `requires javafx.swing;` và còn phải khởi tạo Swing toolkit.
   - Với `java.util.zip` (`Deflater`, `CRC32`) — thuộc `java.base`, **đã có sẵn**
     trong mọi JRE, không cần khai báo gì thêm.
   → Kết quả: module `com.giapha` giữ được đúng 4 dependency tối thiểu. Đây là
   **tính module hóa** đúng nghĩa — mỗi dependency đều có lý do.

2. **Đúng tinh thần đồ án OOP.** `javax.imageio` là API procedural dạng
   `ImageIO.write(image, "png", file)` — không lộ ra cấu trúc định dạng PNG. Tự
   viết cho thấy hiểu biết thực chất về **cấu trúc file PNG**: chữ ký 8 byte,
   khối IHDR/IDAT/IEND, byte filter mỗi dòng, CRC32 cho từng khối.

3. **Không phụ thuộc AWT.** `javax.imageio` kéo theo `java.awt.image.BufferedImage`
   — phải chuyển đổi qua lại giữa `WritableImage` (JavaFX) và `BufferedImage`
   (AWT), tốn CPU và bộ nhớ cho một thao tác chỉ ghi file.

4. **Kiểm thử được độc lập.** `PngWriter.encodePng(int, int, byte[])` là hàm
   **thuần** chỉ dùng `java.base` → `PngWriterTest` kiểm thử được trong môi
   trường headless mà không cần JavaFX Toolkit: xác minh chữ ký PNG, cấu trúc
   khối IHDR/IDAT/IEND, giá trị CRC32, và tính nén zlib.

5. **Kích thước & dependency.** Không thêm artifact nào vào `pom.xml`.

**Nhược điểm thừa nhận:** phải tự hiện thực chuẩn PNG; ảnh đầu ra không hỗ trợ
nén kiểu khác (chỉ zlib filter 0) → dung lượng lớn hơn `ImageIO` tối ưu; và
**không** hỗ trợ metadata/ICCP.

### 3.2. Vì sao Service phụ thuộc interface `PersonRepository`?

- Đây là **Dependency Inversion (SOLID – D)**: tầng cao (Service) phụ thuộc
  abstraction, không phụ thuộc chi tiết (JDBC).
- Bằng chứng trong code: `FamilyTreeService` không có `import com.giapha.repository.JdbcPersonRepository`,
  không có `import java.sql.*`.
- Lợi ích thực tế: `TestPersonRepository extends JdbcPersonRepository` chỉ ghi đè
  đúng **một** hàm `getConnection()` để trỏ sang database test → toàn bộ CRUD
  được kiểm thử với SQL thật, mà **không sửa một dòng nào** ở tầng Service.

### 3.3. Vì sao dùng Constructor Injection thay vì Setter?

- **Bắt buộc phụ thuộc**: `Objects.requireNonNull(...)` trong constructor chặn
  dependency null ngay từ đầu → đối tượng luôn ở trạng thái hợp lệ sau khi tạo.
- **Đối tượng bất biến về dependency**: `private final PersonRepository personRepository`.
- **Composition root rõ ràng**: `Main.initBackend()` là nơi **duy nhất** quyết định
  dùng triển khai nào:
  ```java
  JdbcPersonRepository repository = new JdbcPersonRepository();
  FamilyTreeService service = new FamilyTreeService(repository);
  controller = new PersonController(service);
  ```
- Với setter injection, đối tượng tồn tại trong trạng thái "chưa có dependency"
  → dễ sinh `NullPointerException` khó truy.

### 3.4. Vì sao mỗi quy tắc nghiệp vụ một exception riêng?

- **Thông báo đúng nguyên nhân**: Controller bắt theo kiểu → "Cha phải có giới tính
  là MALE nhưng thực tế là FEMALE" khác hẳn "Gán cha tạo chu trình vòng lặp".
- **Không lộ chi tiết kỹ thuật**: tất cả đều kế thừa `RuntimeException` → người dùng
  không phải đọc stack trace SQL.
- **Bảo trì được**: thêm quy tắc mới = thêm 1 lớp exception + 1 nhánh `catch`,
  không sửa code cũ.
- Đã bỏ `IllegalArgumentException` chung chung cho BR2 và BR6 — vì không phân
  biệt được "sai vì cùng một người" với "sai vì giới tính".

### 3.5. Vì sao BR5 là cảnh báo chứ không phải chặn?

- Theo đặc tả: năm sinh cha/mẹ **"nên"** nhỏ hơn con → "nên" = khuyến nghị, không
  phải ràng buộc cứng.
- Thực tế gia phả: dữ liệu lịch sử có thể có năm sinh ghi nhầm; chặn cứng khiến
  người dùng không lưu được dữ liệu thật.
- Cơ chế: `FamilyTreeService.checkBirthYearWarning(child, father, mother)` trả về
  `List<String>` (rỗng = không có vấn đề) — **không ném exception**.
  `PersonController.confirmBirthYearWarning(...)` hiện `Alert` với 2 nút
  **Tiếp tục** / **Hủy**; chỉ khi chọn "Tiếp tục" mới lưu.
- Với `handleEdit()`, cảnh báo **chỉ** hiện khi quan hệ cha/mẹ thực sự thay đổi
  (`fatherChanged(...)` / `motherChanged(...)`) — sửa tên không gây cảnh báo.

### 3.6. Vì sao lưu đường dẫn ảnh **tương đối**?

- CSDL không bị phụ thuộc vào vị trí cài đặt → copy database sang máy khác vẫn
  dùng được (chỉ cần copy kèm thư mục `images/`).
- Tên file `person_<id>.<đuôi>` giúp **ghi đè** ảnh cũ thay vì sinh file rác.
- **Đánh đổi đã biết**: ứng dụng phải chạy từ thư mục gốc dự án
  (`System.getProperty("user.dir")`), nếu không ảnh không tìm thấy khi mở lại.
  Đây là đánh đổi được nêu rõ trong `README.md` và `deployment-diagram.puml`.

### 3.7. Vì sao `FamilyTreeDiagram` là component JavaFX thuần?

- Nhận `List<Person>` + người trung tâm, **tự dựng bố cục** — không gọi xuống
  Service/Repository. Nhờ đó component có thể tái sử dụng và kiểm thử độc lập.
- Vẽ bằng `Pane` + `Line` + `VBox` (không dùng `WebView`, không nhúng HTML) →
  **không thêm dependency** (JavaFX Web chưa có trong `javafx-controls`).
- `module-info.java` phải khai báo `requires javafx.graphics` để dùng
  `javafx.scene.input.ScrollEvent` (cuộn dọc/ngang, zoom bằng Ctrl + lăn chuột).

### 3.8. Vì sao dùng `ON DELETE RESTRICT` thay vì `CASCADE` hay `SET NULL`?

- `CASCADE`: xóa cha sẽ xóa luôn toàn bộ con cháu → mất dữ liệu hàng loạt.
- `SET NULL`: xóa cha sẽ biến con thành "mồ côi" mà không có cảnh báo.
- `RESTRICT`: **CSDL tự chặn** → BR4 được bảo vệ ở tầng CSDL. Tầng Service
  (`checkPersonInUse(int)`) kiểm tra trước để hiện thông báo tiếng Việt thân thiện.
  → **Hai lớp bảo vệ**: ứng dụng báo lỗi dễ hiểu; CSDL chặn cứng.

### 3.9. Vì sao `checkCycle` dùng DFS còn `getAncestors` dùng BFS?

- `checkCycle` chỉ cần **câu trả lời có/không** ("có tới được `childId` không?"),
  và DFS có thể **thoát sớm** ngay khi tìm thấy → nhanh hơn trong trường hợp
  chu trình ở gần. Dùng `Deque` + `push`/`pop` (LIFO) là cách hiện thực DFS gọn nhất.
- `getAncestors` / `getDescendants` cần **danh sách đầy đủ theo thứ tự từ gần
  ra xa** để bố trí sơ đồ 2D → BFS (FIFO) cho kết quả đúng thứ tự tầng.
- Chi phí: DFS stack có thể sâu bằng số thế hệ; BFS queue rộng bằng số người cùng
  thế hệ. Với quy mô gia phả thực tế, cả hai đều không đáng kể.

### 3.10. Vì sao `getDescendants` phải dựng `childrenMap`?

- `Person` chỉ giữ tham chiếu **tới cha/mẹ**, không giữ danh sách **con** (đây là
  quan hệ 1–N, giữ tham chiếu ngược sẽ tạo vòng lặp trong object graph và gây
  rò rỉ bộ nhớ).
- Hướng "đi xuống" vì vậy không thể đi trực tiếp trên object graph → phải quét
  `findAll()` một lần và dựng `Map<Integer, List<Person>>` (chỉ mục con theo id
  cha/mẹ). Sau đó BFS trên bản đồ này.
- Chi phí: 1 lần `findAll()` + O(n) dựng map, thay vì nhiều lần `findById()`.

### 3.11. Vì sao mỗi lớp test dùng database riêng `*_test`?

- **Cô lập**: test chạy song song hay tuần tự đều không ảnh hưởng dữ liệu thật.
- **Không phụ thuộc thứ tự**: mỗi lớp test tự tạo database sạch ở `@BeforeAll`.
- **Bảo vệ chống tai nạn**: `TestDatabaseManager.assertSafeToDrop()` kiểm tra tên
  database **bắt buộc kết thúc bằng `_test`**; nếu không khớp (ví dụ `gia_pha`)
  → ném `IllegalStateException` và **không DROP**. Đây là hàng phòng thủ cuối cùng.
- **Dễ chẩn đoán**: `-Dkeep.test.db=true` giữ lại database để debug trực tiếp
  bằng MySQL client.

### 3.12. Vì sao `DatabaseConnection` không dùng Singleton / connection pool?

- Yêu cầu đề tài: "Không dùng static long-lived connection".
- `DatabaseConnection` là **utility class tĩnh** (constructor `private`), mỗi lần
  gọi `getConnection()` trả về một `Connection` **mới**; caller đóng bằng
  try-with-resources (`try (Connection conn = getConnection(); ...)`).
- Ở quy mô ứng dụng desktop 1 người dùng, việc mở/đóng connection mỗi truy vấn
  là **đủ nhanh** và tránh rủi ro connection stale.
- Nếu mở rộng thành hệ thống nhiều người dùng → thay bằng `HikariCP` (pool),
  không cần sửa Service vì abstraction `PersonRepository` đã cô lập tầng lưu trữ.

---

## 4. 20 CÂU HỎI GIẢNG VIÊN CÓ THỂ HỎI

### Câu 1 — Vì sao chọn kiến trúc phân tầng? Vì sao không làm một lớp gộp tất cả?

**Gợi ý trả lời:**
Một lớp gộp tất cả sẽ vỡ ra khi đổi giao diện hoặc đổi CSDL. Kiến trúc phân tầng
(`view/` → `controller/` → `service/` → `repository/` → `database/`) đặt mỗi loại
trách nhiệm vào một nơi, giúp **thay thế tầng độc lập**: đổi từ MySQL sang
PostgreSQL chỉ cần viết repository mới, Service không đổi dòng nào; đổi giao diện
từ JavaFX sang web chỉ cần viết View/Controller mới, Service giữ nguyên.
Bằng chứng trong code: `FamilyTreeService` không import bất kỳ JDBC API nào, và
`view/*.java` không import `service`/`repository`/`database`.

---

### Câu 2 — `FamilyTreeService` có import `JdbcPersonRepository` không? Vì sao?

**Gợi ý trả lời:**
Không. Trường `private final PersonRepository personRepository` — kiểu là **interface**.
Đây là **Dependency Inversion (SOLID – D)**: tầng cao phụ thuộc abstraction, không
phụ thuộc chi tiết. Lợi ích cụ thể: `TestPersonRepository extends JdbcPersonRepository`
ghi đè đúng một hàm `getConnection()` để trỏ sang database test → kiểm thử được
toàn bộ CRUD với SQL thật mà không sửa Service.

---

### Câu 3 — Mô hình quan hệ cha–mẹ được lưu thế nào? Có cần bảng trung gian không?

**Gợi ý trả lời:**
Không cần bảng trung gian. Bảng `PERSON` tự tham chiếu: hai cột `father_id` và
`mother_id`, mỗi cột là FK tới `PERSON(id)`. Ở tầng OOP, `Person` giữ hai tham chiếu
đối tượng `father` / `mother`. Quan hệ này là **quan hệ trực hệ** (mỗi người tối đa
1 cha, 1 mẹ) nên biểu diễn bằng object reference tự nhiên hơn bảng trung gian.
Lưu ý: hướng "con → cha" là thuận, nhưng hướng "cha → con" thì **không** thể lưu
trực tiếp trong `Person` (vì sẽ tạo vòng lặp tham chiếu) — vì vậy `getDescendants()`
phải dựng `childrenMap` từ `findAll()`.

---

### Câu 4 — BR1 (tự làm cha/mẹ chính mình) được kiểm tra ở đâu? Có bảo vệ ở tầng CSDL không?

**Gợi ý trả lời:**
Ở tầng Service: `FamilyTreeService.assignFather()` / `assignMother()` ném
`SelfParentException` — và đây là kiểm tra **đầu tiên** trong thứ tự ưu tiên.
Ở tầng CSDL: có **Trigger** `trg_check_not_self_parent_update` (`BEFORE UPDATE`),
`SIGNAL SQLSTATE '45000'` khi `NEW.father_id = NEW.id` hoặc `NEW.mother_id = NEW.id`.
**Lý do dùng Trigger chứ không dùng CHECK:** MySQL/MariaDB không cho phép tham chiếu
cột `AUTO_INCREMENT` (`id`) trong biểu thức `CHECK`, nên không thể viết
`CHECK (father_id <> id)`. Trigger giải quyết được. Ngoài ra `chk_father_not_mother`
là constraint cho BR2.

---

### Câu 5 — BR3 (chu trình) có được bảo vệ ở tầng CSDL không? Vì sao?

**Gợi ý trả lời:**
**Không** — và đây là giới hạn *cố ý đã phân tích*. MySQL/MariaDB không cho phép
`CHECK` constraint tham chiếu sang bảng khác, nên không thể biểu đạt điều kiện
"id X không phải hậu duệ của id Y" bằng một biểu thức `CHECK`. Vì vậy BR3 được
bảo vệ **duy nhất ở tầng Service** bằng `FamilyTreeService.checkCycle(int, int)` —
DFS ngược từ ứng viên cha/mẹ đi lên các thế hệ tổ tiên, nếu gặp `childId` thì
có chu trình. Hệ quả đã nêu trong báo cáo: nếu ai đó ghi trực tiếp bằng SQL thì
vẫn có thể tạo chu trình — đây là hạn chế đã biết.

---

### Câu 6 — Vì sao thứ tự kiểm tra là BR1 → BR2 → BR6 → BR3?

**Gợi ý trả lời:**
Để thông báo lỗi chỉ ra **đúng nguyên nhân gốc**. Ví dụ: nếu kiểm tra BR6 trước
BR2, khi người dùng chọn một người nữ làm cha và người đó đã là mẹ hiện tại, cả 2
lỗi đều đúng, nhưng thông báo "sai giới tính" ít hữu ích hơn "cha và mẹ là cùng
một người". Ngoài ra BR1 là lỗi "tuyệt đối" (bất kỳ id nào cũng vi phạm) nên kiểm
tra trước; BR3 để cuối vì nó **đắt nhất** — phải duyệt đồ thị, còn các lỗi khác
chỉ là so sánh số. Đây là thứ tự được ghi rõ trong `AGENTS.md` và trong
`sequence-assign-parent.puml` (khối `alt/else/end` chia 5 nhánh).

---

### Câu 7 — Thuật toán phát hiện chu trình hoạt động thế nào? Vì sao cần `visited`?

**Gợi ý trả lời:**
`FamilyTreeService.checkCycle(int childId, int candidateParentId)`: nếu `childId` là
tổ tiên của `candidateParentId` thì gán sẽ khép kín vòng lặp → ta **duyệt ngược**
từ `candidateParentId` đi lên qua các nhánh cha/mẹ; gặp `childId` là có chu trình.
Dùng `Deque` + `push`/`pop` → **DFS**. `Set<Integer> visited` là **bắt buộc** vì
hàm này phải trả lời đúng **ngay cả khi dữ liệu đã hỏng** (chu trình do SQL thủ
công tạo): không có `visited` thì vòng `while` không bao giờ kết thúc → treo
chương trình. `visited.add(fId)` trả `false` khi id đã thấy → không push lại.

---

### Câu 8 — `getAncestors()` và `getDescendants()` dùng thuật toán nào? Khác nhau thế nào?

**Gợi ý trả lời:**
Cả hai dùng **BFS** (FIFO, `Queue` + `add`/`poll`) — vì cần danh sách đầy đủ **theo
thứ tự từ gần ra xa** để bố trí sơ đồ 2D (tổ tiên ở trên, hậu duệ ở dưới).
Khác biệt: `getAncestors()` đi ngược theo `getFather()`/`getMother()` sẵn có trong
đối tượng; `getDescendants()` phải **dựng `childrenMap`** từ `findAll()` trước,
vì `Person` không giữ danh sách con (giữ tham chiếu ngược sẽ tạo vòng lặp tham
chiếu). Kết quả kiểm chứng trên bộ dữ liệu mẫu: `getAncestors(23)` = 8 người
(TC-04), `getDescendants(1)` = 18 người (TC-03).

---

### Câu 9 — BR5 khác BR1–BR4, BR6 ở chỗ nào? Vì sao?

**Gợi ý trả lời:**
BR5 là quy tắc **mức CẢNH BÁO**, không phải hard validation. Đặc tả nói năm sinh
cha/mẹ *"nên"* nhỏ hơn con → khuyến nghị, không phải ràng buộc cứng. Thực tế gia
phả lịch sử có thể ghi nhầm năm sinh; chặn cứng sẽ khiến người dùng không lưu được
dữ liệu thật. Cơ chế: `FamilyTreeService.checkBirthYearWarning(child, father,
mother)` trả `List<String>` (rỗng = OK) — **không ném exception**;
`PersonController.confirmBirthYearWarning()` hiện `Alert` với 2 nút **Tiếp tục** /
**Hủy**, chỉ khi chọn "Tiếp tục" mới lưu. Với `handleEdit()`, cảnh báo chỉ hiện khi
quan hệ cha/mẹ thực sự thay đổi.

---

### Câu 10 — BR4 (không xóa người đang làm cha/mẹ) được bảo vệ ở mấy tầng?

**Gợi ý trả lời:**
**Hai tầng.** Tầng Service: `FamilyTreeService.deletePerson(int)` gọi
`checkPersonInUse(int)` → nếu người đó đang là cha/mẹ thì ném `PersonInUseException`;
`PersonController.handleDelete(int)` bắt và hiện thông báo tiếng Việt hướng dẫn
cách xử lý. Tầng CSDL: hai khóa ngoại `fk_father`, `fk_mother` dùng
**`ON DELETE RESTRICT`** → CSDL tự chặn xóa. Chọn `RESTRICT` thay vì `CASCADE`
(không mất dữ liệu hàng loạt) và `SET NULL` (không tạo "mồ côi" ngầm). Cũng lưu ý
Service **không** tự set NULL quan hệ của con — người dùng phải chủ động gỡ
bằng `clearFather()` / `clearMother()`.

---

### Câu 11 — Vì sao tự viết `PngWriter` thay vì dùng `javax.imageio`?

**Gợi ý trả lời:**
Vì **ranh giới module (JPMS)**. `javax.imageio` thuộc module `java.desktop` — dùng nó
sẽ phải thêm `requires java.desktop;` vào `module-info.java`, kéo theo cả hệ sinh
thái AWT/Swing vào ứng dụng thuần JavaFX; `javafx.swing` (`SwingFXUtils`) là module
riêng, cũng phải khai báo thêm. Còn `java.util.zip` (`Deflater` + `CRC32`) thuộc
`java.base` — có sẵn trong mọi JRE, **không cần khai báo gì**. Nhờ đó
`module com.giapha` giữ đúng 4 dependency: `javafx.controls`, `javafx.graphics`,
`javafx.fxml`, `java.sql`. Thêm nữa, tự viết cho thấy hiểu cấu trúc file PNG
(chữ ký 8 byte, khối IHDR/IDAT/IEND, byte filter mỗi dòng, CRC32 từng khối), và
`encodePng(int,int,byte[])` là hàm thuần `java.base` nên `PngWriterTest` kiểm thử
được trong môi trường headless không cần JavaFX Toolkit. Nhược điểm đã thừa nhận:
ảnh lớn hơn `ImageIO` tối ưu và không có metadata.

---

### Câu 12 — Sơ đồ cây gia phả 2D được bố trí thế nào? Thuật toán nào?

**Gợi ý trả lời:**
Trong `view/FamilyTreeDiagram.java`, `rebuild()` gọi 5 bước:
1. `buildModel()` — BFS từ người trung tâm, gán `generation` (−1, −2 ở tổ tiên;
   +1, +2 ở hậu duệ) quyết định **hàng**; đồng thời thêm vợ/chồng của người
   trung tâm trong cặp cha–mẹ để cây không thiếu "nửa gia đình".
2. `buildUnits()` — gom **khối gia đình** cha+mẹ thành một `Unit` để con được
   canh giữa giữa hai vị trí cha/mẹ.
3. `collectAncestorUnits()` / `collectDescendantUnits()` — DFS giới hạn phạm vi
   từng nhánh.
4. `layoutAll()` → `layoutTree()` — thuật toán **"cha canh giữa trên tập con"**:
   lá được cấp cột liên tiếp bằng bộ đếm `counter`; node cha nhận cột trung bình
   giữa lá đầu và lá cuối, trừ nửa chiều rộng của chính nó.
   Sau đó cả nhánh tổ tiên được **dịch** để khối ngay trên người trung tâm thẳng
   hàng.
5. `assignPixelPositions()` — cột/hàng → pixel với `NODE_WIDTH=150`,
   `H_GAP=22`, `NODE_HEIGHT=72`, `V_GAP=62`, `MARGIN=40`; đồng thời tính
   `baseWidth`/`baseHeight` (kích thước nội dung ở zoom 1.0).
Đường kẻ vẽ ở **mức cá nhân** (`drawEdges()`) — mỗi cha/mẹ nối xuống đúng con
của họ, tránh nối nhầm vợ/chồng.

---

### Câu 13 — Tại sao xuất PNG được **toàn bộ** sơ đồ chứ không bị cắt?

**Gợi ý trả lời:**
`FamilyTreeDiagram.renderFullImage()`: (1) **tạm đặt** `scaleX = scaleY = 1.0`
và `translate = (0,0)`; (2) dùng `SnapshotParameters.setViewport(new Rectangle2D(0, 0,
baseWidth, baseHeight))` — viewport đúng bằng **kích thước nội dung sơ đồ**, nên
ảnh không bị cắt theo khung nhìn hiện tại, kể cả phần đang thu nhỏ ra ngoài;
(3) `finally` **khôi phục lại** zoom/translate cũ để người dùng không mất góc nhìn.
Sau đó `FamilyTreeView.handleExportPng()` gọi `PngWriter.write(image, file)`.
Kết quả độc lập với mức zoom người dùng đang đặt.

---

### Câu 14 — In ấn dùng thư viện nào? Có thêm dependency nào không?

**Gợi ý trả lời:**
Dùng `javafx.print.PrinterJob` — JavaFX đã có sẵn, **không thêm dependency**.
`FamilyTreeView.handlePrint()`: `PrinterJob.createPrinterJob()` (nếu `null` → Alert
"Không tìm thấy máy in"); `confirmPrint(job)` thử `job.showPrintDialog(...)` (hộp
thoại hệ thống), nếu nền tảng không hỗ trợ thì `catch (Throwable)` và hiện `Dialog`
nội bộ; `buildPrintableNode(job)` bọc ảnh trong `ImageView` → `StackPane` có kích
thước bằng khổ giấy lấy từ `job.getJobSettings().getPageLayout()`, nền trắng,
canh giữa; `job.printPage(printable)`; mọi nhánh thành công/thất bại đều báo bằng
`Alert`, không ném exception ra UI.

---

### Câu 15 — Ảnh đại diện được lưu thế nào? Vì sao lưu đường dẫn tương đối?

**Gợi ý trả lời:**
`PersonFormView.handleChoosePhoto()` mở `FileChooser` (chỉ `png`/`jpg`/`jpeg`), kiểm
`ImageManager.isAllowedImage(...)`, rồi chỉ giữ `selectedPhotoSourcePath` — **View
không copy file**. `PersonController.persistSelectedImage(sourcePath, personId)` gọi
`ImageManager.copyImageToAppDirectory(...)`: copy vào `images/person_<id>.<đuôi>`
(`REPLACE_EXISTING`) và trả về **đường dẫn tương đối** `images/person_7.png`, đó là
giá trị lưu vào cột `photo_path`. Việc copy diễn ra **sau khi** đã có `id` (nên
`handleAdd()` phải INSERT rồi UPDATE). Lưu tương đối để CSDL không phụ thuộc vị trí
cài đặt — đánh đổi: ứng dụng phải chạy từ thư mục gốc dự án
(`System.getProperty("user.dir")`), đã ghi rõ trong `README.md` và
`deployment-diagram.puml`.

---

### Câu 16 — Nếu file ảnh hỏng hoặc không tồn tại thì sao? Có crash ứng dụng không?

**Gợi ý trả lời:**
Không. `view/PersonAvatar.java` có cơ chế **fallback 2 tầng**:
(1) nếu `photoPath` hợp lệ và file đọc được → hiển thị ảnh thật bo tròn bằng
`Circle` clip; (2) nếu file không tồn tại / không đọc được / hỏng → hiển thị **chữ
cái đầu** của họ tên trên nền tròn màu theo giới tính (`buildPlaceholder()`).
Ảnh nạp ở chế độ **background loading** (`new Image(uri, true)`), listener
`progressProperty` áp ảnh khi nạp xong, listener `errorProperty` quay lại
placeholder. Mọi lỗi đều bị bọc trong `try/catch (Exception)` → **không bao giờ**
ném exception ra giao diện. Có bộ test riêng `AvatarFallbackTest` (tag `javafx`).

---

### Câu 17 — Làm sao đảm bảo test không làm hỏng dữ liệu thật?

**Gợi ý trả lời:**
Ba lớp bảo vệ:
1. **Cô lập**: mọi lớp test đụng CSDL dùng database **riêng** tên kết thúc bằng
   `_test` (`gia_pha_service_test`, `gia_pha_controller_test`, `gia_pha_conn_test`,
   `gia_pha_javafx_test`, `gia_pha_test`). Vòng đời: `@BeforeAll` →
   `TestDatabaseManager.createFreshDatabase(<db>_test)` (DROP + CREATE + schema + 7
   bản ghi mẫu); `@AfterAll` → `dropDatabase(<db>_test)`.
2. **An toàn DROP**: `TestDatabaseManager.assertSafeToDrop()` kiểm tra tên database
   **bắt buộc kết thúc bằng `_test`**; không khớp (ví dụ `gia_pha`) → ném
   `IllegalStateException` và **không DROP**. Có test riêng TC-21a…TC-21d.
3. **Repository trỏ đúng DB test**: `TestPersonRepository extends JdbcPersonRepository`
   ghi đè `getConnection()` → `TestDatabaseManager.openConnection(testDbName)`.
Ngoài ra `DatabaseConnectionTest` còn **kiểm chứng** database chính không bị thay
đổi. Nếu MySQL không chạy, test dùng `Assumptions.assumeTrue(...)` → `SKIPPED`,
không tính là `FAILED`.

---

### Câu 18 — Vì sao `updatePerson()` không cho sửa quan hệ cha/mẹ?

**Gợi ý trả lời:**
Có chủ đích. `FamilyTreeService.updatePerson(Person)` luôn lấy `father`/`mother`
**hiện tại từ DB** (`existing = getPersonById(person.getId())`) rồi gán đè lên
object, bỏ qua hoàn toàn giá trị caller truyền vào — vì `updatePerson()` **không
bao giờ** được phép thay đổi quan hệ cha/mẹ. Việc đổi quan hệ đi qua
`assignFather()` / `assignMother()` / `clearFather()` / `clearMother()` để **mọi
quy tắc BR1–BR6 được áp dụng đầy đủ**. Nếu `updatePerson()` cho phép sửa
`father_id` trực tiếp thì các quy tắc này sẽ bị bypass. Đây cũng là lý do
`PersonController.handleEdit(Person, Person, Person)` tách rõ 2 bước: cập nhật
thông tin cơ bản (`updatePerson`) rồi mới gán/gỡ quan hệ (`assignFather` /
`clearFather` / `assignMother` / `clearMother`), và báo kết quả gộp.

---

### Câu 19 — Vì sao có `module-info.java`? Có khó khăn gì khi dùng JUnit 5 không?

**Gợi ý trả lời:**
`module-info.java` khai báo module `com.giapha` với đúng 4 dependency
(`javafx.controls`, `javafx.graphics`, `javafx.fxml`, `java.sql`) và `opens`
cho `javafx.graphics`/`javafx.fxml` — đây là **tính module hóa** đúng nghĩa, mỗi
dependency đều có lý do cụ thể ( ví dụ `javafx.graphics` để dùng
`javafx.scene.input.ScrollEvent` cho cuộn/zoom trong `FamilyTreeDiagram`).
Khó khăn: `src/main/java` có `module-info.java` (module có tên) còn `src/test/java`
là test thuần JUnit 5 nằm ở **"unnamed module"**. Nếu ép test compile/chạy theo
module path, JPMS sẽ chặn reflection của JUnit 5 → lỗi compile hoặc
`IllegalAccessError`. Vì vậy `pom.xml` cấu hình
`maven-compiler-plugin` (execution `default-testCompile`) và `maven-surefire-plugin`
với `<useModulePath>false</useModulePath>`. Cấu hình này **bắt buộc** — không được
bỏ khi thêm test mới.

---

### Câu 20 — Hệ thống có thể mở rộng thế nào? Hạn chế lớn nhất là gì?

**Gợi ý trả lời:**
**Hạn chế đã biết:** (1) BR3 chỉ bảo vệ ở tầng Service, không có ở tầng CSDL — nếu
ghi trực tiếp bằng SQL thì vẫn tạo được chu trình; (2) ứng dụng phải chạy từ
thư mục gốc dự án do lưu đường dẫn ảnh tương đối; (3) chưa có đăng nhập/phân
quyền, chưa có nhập–xuất GEDCOM/CSV, chưa đóng gói installer; (4) chưa có kiểm
thử hiệu năng với quy mô lớn hơn 30 người.
**Mở rộng:** vì kiến trúc đã phân tầng và Service chỉ phụ thuộc interface
`PersonRepository`, nên các hướng đi đều không cần sửa tầng nghiệp vụ:
(1) đóng gói bằng `jpackage`; (2) đổi sang lưu ảnh dạng BLOB trong CSDL để bỏ ràng
buộc thư mục làm việc; (3) thêm `HikariCP` connection pool khi mở rộng thành hệ
thống nhiều người dùng; (4) chuyển sang kiến trúc web (Spring Boot + REST) — thay
lớp View/Controller, giữ nguyên Service; (5) nâng thuật toán bố trí lên
Reingold–Tilford/Sugiyama cho cây rất rộng; (6) thêm audit trail để truy vết
nguồn gốc dữ liệu.

---

## 5. BẢNG TRA NHANH TÊN HÀM / TÊN LỚP

| Tên | File | Vai trò |
|---|---|---|
| `Main.initBackend()` | `Main.java` | Composition Root — lắp ráp Repository → Service → Controller |
| `Main.showPeopleList()` / `showPersonForm()` / `showPersonDetail()` / `showFamilyTree()` | `Main.java` | Điều hướng giữa các màn hình, inject Controller vào View |
| `Person` | `model/Person.java` | Entity: 7 trường, quan hệ cha/mẹ bằng tham chiếu đối tượng |
| `Gender` | `model/Gender.java` | enum `MALE`, `FEMALE` |
| `FamilyTreeService.addPerson(Person)` | `service/FamilyTreeService.java` | Validate + BR2 + BR6 → `save()` |
| `FamilyTreeService.updatePerson(Person)` | `service/FamilyTreeService.java` | Chỉ sửa thông tin cơ bản, **giữ nguyên** cha/mẹ từ DB |
| `FamilyTreeService.deletePerson(int)` | `service/FamilyTreeService.java` | BR4 — chặn xóa người đang được tham chiếu |
| `FamilyTreeService.assignFather(int, int)` | `service/FamilyTreeService.java` | BR1 → BR2 → BR6 → BR3 |
| `FamilyTreeService.assignMother(int, int)` | `service/FamilyTreeService.java` | BR1 → BR2 → BR6 → BR3 |
| `FamilyTreeService.clearFather(int)` / `clearMother(int)` | `service/FamilyTreeService.java` | Gỡ quan hệ (set null) |
| `FamilyTreeService.getAncestors(int)` | `service/FamilyTreeService.java` | **BFS** ngược — tổ tiên |
| `FamilyTreeService.getDescendants(int)` | `service/FamilyTreeService.java` | **BFS** trên `childrenMap` — hậu duệ |
| `FamilyTreeService.searchPersons(String)` | `service/FamilyTreeService.java` | `personRepository.search(keyword)` |
| `FamilyTreeService.getAllPersons()` | `service/FamilyTreeService.java` | `personRepository.findAll()` |
| `FamilyTreeService.checkBirthYearWarning(Person, Person, Person)` | `service/FamilyTreeService.java` | **BR5** — trả `List<String>`, không ném exception |
| `FamilyTreeService.validatePerson(Person)` | `service/FamilyTreeService.java` | `private` — tên 1–100 ký tự, năm sinh 1000..năm nay, giới tính khác null |
| `FamilyTreeService.checkCycle(int, int)` | `service/FamilyTreeService.java` | `private` — **DFS** phát hiện chu trình |
| `FamilyTreeService.checkPersonInUse(int)` | `service/FamilyTreeService.java` | `private` — **BR4** |
| `FamilyTreeService.MIN_BIRTH_YEAR` | `service/FamilyTreeService.java` | Hằng số `1000` — nguồn sự thật duy nhất |
| `PersonController.handleAdd()` / `handleAdd(Person)` | `controller/PersonController.java` | UC01 |
| `PersonController.handleEdit()` / `handleEdit(Person, Person, Person)` | `controller/PersonController.java` | UC02 |
| `PersonController.handleDelete()` / `handleDelete(int)` | `controller/PersonController.java` | UC03 |
| `PersonController.handleSearch()` / `handleSearch(String)` | `controller/PersonController.java` | UC04 |
| `PersonController.handleViewDetail(int)` | `controller/PersonController.java` | UC06 |
| `PersonController.handleAssignFather(int, int)` / `handleAssignMother(int, int)` | `controller/PersonController.java` | UC07 / UC08 |
| `PersonController.handleClearFather(int)` / `handleClearMother(int)` | `controller/PersonController.java` | UC09 |
| `PersonController.handleViewTree(int)` | `controller/PersonController.java` | UC10 — xác thực focus tồn tại rồi nạp tổ tiên/hậu duệ |
| `PersonController.confirmBirthYearWarning(...)` | `controller/PersonController.java` | `private` — hiện Alert BR5 (Tiếp tục / Hủy) |
| `PersonController.persistSelectedImage(String, int)` | `controller/PersonController.java` | `private` — copy ảnh + sinh đường dẫn tương đối |
| `PersonController.showErrorAlert/showInfoAlert/showWarningAlert` | `controller/PersonController.java` | `private` — bọc `Alert`, fallback `println` nếu không ở FX thread |
| `JdbcPersonRepository.findById(int)` | `repository/JdbcPersonRepository.java` | `SELECT` + `LEFT JOIN` cha/mẹ |
| `JdbcPersonRepository.findAll()` | `repository/JdbcPersonRepository.java` | `SELECT` tất cả, **liên kết cha/mẹ trong bộ nhớ** |
| `JdbcPersonRepository.search(String)` | `repository/JdbcPersonRepository.java` | `SELECT ... WHERE full_name LIKE ?` |
| `JdbcPersonRepository.save(Person)` | `repository/JdbcPersonRepository.java` | `INSERT` + `RETURN_GENERATED_KEYS` |
| `JdbcPersonRepository.update(Person)` | `repository/JdbcPersonRepository.java` | `UPDATE` — dùng cho cả gán/gỡ quan hệ |
| `JdbcPersonRepository.delete(int)` | `repository/JdbcPersonRepository.java` | `DELETE` |
| `JdbcPersonRepository.getConnection()` | `repository/JdbcPersonRepository.java` | `protected` — **điểm móc** cho Template Method |
| `DatabaseConnection.getConnection()` | `database/DatabaseConnection.java` | `static` — cấp `Connection` mới mỗi lần |
| `DatabaseConnection.getUrl()/getUser()/getPassword()` | `database/DatabaseConnection.java` | System property → Env var → mặc định |
| `ImageManager.isAllowedImage(String)` | `util/ImageManager.java` | Chỉ nhận `png`/`jpg`/`jpeg`, không phân biệt hoa thường |
| `ImageManager.copyImageToAppDirectory(String, int)` | `util/ImageManager.java` | Copy vào `images/person_<id>.<đuôi>`, trả đường dẫn tương đối |
| `ImageManager.deleteImageQuietly(String)` | `util/ImageManager.java` | Dọn file an toàn, **không ném exception** |
| `ImageManager.resolveImageFile` — *thuộc `PersonAvatar`* | `view/PersonAvatar.java` | `static` — phân giải đường dẫn ảnh thành `File` |
| `PngWriter.write(WritableImage, File)` | `util/PngWriter.java` | Ghi file PNG |
| `PngWriter.encodePng(int, int, byte[])` | `util/PngWriter.java` | Hàm **thuần** `java.base` — test được headless |
| `FamilyTreeDiagram.setData(List<Person>, Person)` | `view/FamilyTreeDiagram.java` | Điểm vào — nạp dữ liệu & vẽ lại |
| `FamilyTreeDiagram.rebuild()` | `view/FamilyTreeDiagram.java` | `private` — pipeline dựng sơ đồ |
| `FamilyTreeDiagram.buildModel()` | `view/FamilyTreeDiagram.java` | `private` — BFS dựng tập node + `generation` |
| `FamilyTreeDiagram.buildUnits(Map)` | `view/FamilyTreeDiagram.java` | `private` — gom khối gia đình cha–mẹ |
| `FamilyTreeDiagram.layoutAll(...)` | `view/FamilyTreeDiagram.java` | `private` — bố trí 2 nhánh + dịch nhánh tổ tiên |
| `FamilyTreeDiagram.layoutTree(Unit, Set, double[])` | `view/FamilyTreeDiagram.java` | `private` — "cha canh giữa trên tập con" |
| `FamilyTreeDiagram.assignPixelPositions(...)` | `view/FamilyTreeDiagram.java` | `private` — cột/hàng → pixel, tính `baseWidth/Height` |
| `FamilyTreeDiagram.drawEdges(...)` / `createNodeCard(...)` | `view/FamilyTreeDiagram.java` | `private` — vẽ đường kẻ & node |
| `FamilyTreeDiagram.zoomAround/zoomBy/resetZoom/fitToView` | `view/FamilyTreeDiagram.java` | Zoom 0.25×–2.5×, giữ vị trí con trỏ |
| `FamilyTreeDiagram.renderFullImage()` | `view/FamilyTreeDiagram.java` | Chụp **toàn bộ** sơ đồ ở zoom 1.0 |
| `FamilyTreeDiagram.contentScaleProperty()` | `view/FamilyTreeDiagram.java` | Read-only property — Subject của Observer |
| `FamilyTreeView.handleExportPng()` | `view/FamilyTreeView.java` | `private` — FileChooser + `PngWriter` |
| `FamilyTreeView.handlePrint()` | `view/FamilyTreeView.java` | `private` — `PrinterJob` |
| `FamilyTreeView.buildPrintableNode(PrinterJob)` | `view/FamilyTreeView.java` | `private` — bọc ảnh trong khung trang |
| `FamilyTreeView.confirmPrint(PrinterJob)` | `view/FamilyTreeView.java` | `private` — hộp thoại hệ thống, fallback Dialog nội bộ |
| `FamilyTreeView.defaultExportFileName()` | `view/FamilyTreeView.java` | `private` — `gia_pha_<Tên>.png` |
| `PeopleListView.reloadData()` / `refresh()` | `view/PeopleListView.java` | Nạp lại từ MySQL qua Controller |
| `PeopleListView.calculateGenerations(List)` / `getDepth(Person, Map)` | `view/PeopleListView.java` | Tính số thế hệ cho thẻ thống kê |
| `PersonFormView.handleChoosePhoto()` | `view/PersonFormView.java` | `private` — FileChooser + kiểm định dạng |
| `PersonFormView.handleSave()` | `view/PersonFormView.java` | `private` — validate View → gọi Controller |
| `PersonFormView.refreshComboBoxes()` | `view/PersonFormView.java` | `private` — lọc cha theo MALE, mẹ theo FEMALE, loại bỏ chính mình |
| `PersonDetailView.getChildrenFromDB()` | `view/PersonDetailView.java` | `private` — lọc con từ `controller.getAllPersons()` |
| `PersonAvatar.resolveImageFile(String)` | `view/PersonAvatar.java` | `public static` — phân giải đường dẫn ảnh |
| `PersonAvatar.isShowingRealImage()` | `view/PersonAvatar.java` | `public` — dùng cho kiểm thử |

---

## 6. CÁC CON SỐ CẦN NHỚ

| Con số | Giá trị | Nơi dùng |
|---|---|---|
| Tổng test mặc định | **121** (0 fail, 0 error, 0 skip) | `mvn clean test` |
| Test tag `javafx` | **26** | `mvn test -Pjavafx-tests` |
| Test case trong `test-data/test-cases.md` | **22** (22 PASS) | `LargeSampleDataTest` |
| Lớp exception | **7**, đều `extends RuntimeException` | `com.giapha.exception.*` |
| Quy tắc nghiệp vụ | **BR1–BR6** | `FamilyTreeService` |
| Sơ đồ PlantUML | **17** (`.puml` + `.png`) | `docs/uml/` |
| Module | `com.giapha`, **4** `requires` | `module-info.java` |
| Năm sinh tối thiểu | `MIN_BIRTH_YEAR = 1000` | `FamilyTreeService` |
| Năm sinh tối đa | `Year.now().getValue()` (Service) / `2100` (CSDL CHECK) | — |
| Độ dài họ tên | 1–100 ký tự | `validatePerson()` |
| Số bản ghi mẫu trong `schema.sql` | **7** (4 thế hệ) | `database/schema.sql` |
| Số bản ghi trong bộ test mẫu | **30** (5 thế hệ, 4 nhánh) | `test-data/sample-data.sql` |
| Hậu duệ của id=1 | **18** (TC-03) | `getDescendants(1)` |
| Tổ tiên của id=23 | **8** (TC-04) | `getAncestors(23)` |
| Số thế hệ của bộ mẫu | **5** (TC-02) | — |
| Zoom tối thiểu / tối đa | `0.25` / `2.5` | `FamilyTreeDiagram` |
| Hệ số zoom nút `+` / `−` | `1.2` / `1/1.2` | `FamilyTreeView.createToolbar()` |
| Kích thước node | `NODE_WIDTH = 150`, `NODE_HEIGHT = 72` | `FamilyTreeDiagram` |
| Khoảng cách | `H_GAP = 22`, `V_GAP = 62`, `MARGIN = 40` | `FamilyTreeDiagram` |
| Kích thước avatar | `DEFAULT_SIZE = 70`, `SMALL_SIZE = 34` | `PersonAvatar` |
| Kích thước ảnh PNG | `bit depth = 8`, `color type = 6 (RGBA)` | `PngWriter.encodePng()` |
| Chữ ký PNG | 8 byte: `89 50 4E 47 0D 0A 1A 0A` | `PngWriter` |
| Định dạng ảnh cho phép | `png`, `jpg`, `jpeg` | `ImageManager` |
| Tên file ảnh | `images/person_<id>.<đuôi>` | `ImageManager` |
| Cổng MySQL | `3306`, database `gia_pha`, charset `utf8mb4` | `DatabaseConnection` |
