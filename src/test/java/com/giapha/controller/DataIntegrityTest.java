package com.giapha.controller;

import com.giapha.exception.CycleDetectedException;
import com.giapha.exception.InvalidParentGenderException;
import com.giapha.exception.SameParentException;
import com.giapha.exception.SelfParentException;
import com.giapha.model.Gender;
import com.giapha.model.Person;
import com.giapha.repository.PersonRepository;
import com.giapha.service.FamilyTreeService;
import com.giapha.support.TestDatabase;
import com.giapha.support.TestDatabaseManager;
import com.giapha.util.ImageManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Kiểm thử cho GĐ1 — <b>FIX DATA INTEGRITY</b>.
 *
 * <p>Phạm vi kiểm thử:
 * <ol>
 *     <li><b>C4</b> — xóa thành viên phải dọn file ảnh đại diện; nếu xóa DB thất bại
 *         thì ảnh phải được GIỮ NGUYÊN.</li>
 *     <li><b>C2/M7</b> — thêm thành viên kèm ảnh: lỗi xử lý ảnh KHÔNG được làm hỏng
 *         việc lưu thành viên, và không được tạo bản ghi trùng.</li>
 *     <li><b>C3</b> — các đường ghi dữ liệu phải áp dụng đồng nhất BR1–BR6.</li>
 * </ol>
 *
 * <p><b>Cô lập dữ liệu:</b> database riêng {@code gia_pha_integrity_test}
 * (tên kết thúc bằng {@code _test}) — không bao giờ ghi vào database chính {@code gia_pha}.
 * Thư mục ảnh cũng được chuyển sang thư mục tạm để không đụng tới {@code images/}
 * của ứng dụng thật.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("GĐ1 — Toàn vẹn dữ liệu: xóa ảnh khi xóa thành viên & thêm thành viên kèm ảnh")
class DataIntegrityTest {

    private static final String TEST_DB_NAME = "gia_pha_integrity_test";

    private static FamilyTreeService service;
    private static PersonController controller;

    private static String originalUserDir;

    // ============================================================
    // VÒNG ĐỜI
    // ============================================================

    @BeforeAll
    static void init() throws Exception {
        TestDatabaseManager.createFreshDatabase(TEST_DB_NAME);
        assumeTrue(TestDatabase.isAvailable(TEST_DB_NAME),
                "BỎ QUA: không kết nối được MySQL. Hãy khởi động MySQL trước khi chạy test.");
        service = new FamilyTreeService(TestDatabaseManager.repositoryFor(TEST_DB_NAME));
        controller = new PersonController(service);
    }

    @AfterAll
    static void drop() throws Exception {
        TestDatabaseManager.dropDatabase(TEST_DB_NAME);
    }

    @BeforeEach
    void setUp() {
        originalUserDir = System.getProperty("user.dir");
        TestDatabase.resetToSampleState(TEST_DB_NAME);
        cleanupExtraRecords();
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.dir", originalUserDir);
    }

    // ============================================================
    // NHÓM 1 — C4: XÓA THÀNH VIÊN VÀ XÓA ẢNH
    // ============================================================

    @Test
    @Order(1)
    @DisplayName("C4 — Xóa thành viên thành công → file ảnh đại diện bị xóa theo")
    void deletePersonRemovesAvatarFile(@TempDir Path tempDir) throws IOException {
        System.setProperty("user.dir", tempDir.toAbsolutePath().toString());

        // Chuẩn bị: id 7 là người CUỐI cùng trong cây (không có con) → xóa được.
        Path avatar = tempDir.resolve("images").resolve("person_7.png");
        Files.createDirectories(avatar.getParent());
        Files.writeString(avatar, "fake-png");
        setPhotoPath(7, "images/person_7.png");

        assertTrue(Files.exists(avatar), "Ảnh phải tồn tại trước khi xóa");

        controller.handleDelete(7);

        assertFalse(Files.exists(avatar),
                "Ảnh đại diện phải bị xóa sau khi xóa thành viên thành công (C4)");
        assertEquals(-1, TestDatabase.queryIdByName(TEST_DB_NAME, "Nguyễn Văn Cháu"),
                "Bản ghi DB phải bị xóa");
    }

    @Test
    @Order(2)
    @DisplayName("C4 — Xóa DB thất bại (BR4) → ảnh đại diện được GIỮ NGUYÊN")
    void failedDbDeleteKeepsAvatarFile(@TempDir Path tempDir) throws IOException {
        System.setProperty("user.dir", tempDir.toAbsolutePath().toString());

        // id 3 là CHA của id 5 → BR4 chặn xóa.
        Path avatar = tempDir.resolve("images").resolve("person_3.png");
        Files.createDirectories(avatar.getParent());
        Files.writeString(avatar, "fake-png");
        setPhotoPath(3, "images/person_3.png");

        controller.handleDelete(3);

        assertTrue(Files.exists(avatar),
                "Ảnh KHÔNG được xóa khi thao tác xóa DB thất bại (C4)");
        assertEquals("Nguyễn Văn Cha", TestDatabase.queryString(TEST_DB_NAME, "full_name", "Nguyễn Văn Cha"),
                "Bản ghi DB phải được giữ nguyên");
    }

    @Test
    @Order(3)
    @DisplayName("C4 — Xóa thành viên không có ảnh → không ném exception, vẫn xóa thành công")
    void deletePersonWithoutAvatarSucceeds() {
        assertNull(TestDatabase.queryString(TEST_DB_NAME, "photo_path", "Nguyễn Văn Cháu"));

        controller.handleDelete(7);

        assertEquals(-1, TestDatabase.queryIdByName(TEST_DB_NAME, "Nguyễn Văn Cháu"),
                "Bản ghi vẫn phải bị xóa khi không có ảnh");
    }

    // ============================================================
    // NHÓM 2 — C2/M7: THÊM THÀNH VIÊN KÈM ẢNH
    // ============================================================

    @Test
    @Order(4)
    @DisplayName("C2 — Thêm thành viên kèm ảnh → file được copy và photo_path được ghi vào DB")
    void addPersonWithImagePersistsBoth(@TempDir Path tempDir) throws IOException {
        System.setProperty("user.dir", tempDir.toAbsolutePath().toString());

        Path source = tempDir.resolve("nguon.png");
        Files.write(source, "fake-png-content".getBytes(StandardCharsets.UTF_8));

        PersonController c = new ControllerWithImage(service, source.toString());
        Person added = c.handleAdd(new Person("Nguyễn Văn Có Ảnh", 1999, Gender.MALE, null));

        assertNotNull(added, "Thành viên phải được thêm thành công");
        assertTrue(added.getId() > 0, "Phải sinh id hợp lệ");

        Path copied = tempDir.resolve("images").resolve("person_" + added.getId() + ".png");
        assertTrue(Files.exists(copied), "File ảnh phải được copy vào thư mục images");

        String storedPath = TestDatabase.queryString(TEST_DB_NAME, "photo_path", "Nguyễn Văn Có Ảnh");
        assertNotNull(storedPath, "photo_path phải được ghi vào DB");
        assertEquals("images/person_" + added.getId() + ".png", storedPath);
    }

    @Test
    @Order(5)
    @DisplayName("C2 — Thêm thành viên THÀNH CÔNG nhưng cập nhật ảnh THẤT BẠI → thành viên vẫn tồn tại, KHÔNG báo lỗi thêm")
    void addPersonSucceedsEvenWhenPhotoUpdateFails(@TempDir Path tempDir) throws IOException {
        System.setProperty("user.dir", tempDir.toAbsolutePath().toString());

        Path source = tempDir.resolve("nguon.png");
        Files.write(source, "fake-png-content".getBytes(StandardCharsets.UTF_8));

        // Service cố tình làm updatePhotoPath() thất bại (giả lập lỗi DB).
        FamilyTreeService failingService = new ServiceWithFailingPhotoUpdate(
                TestDatabaseManager.repositoryFor(TEST_DB_NAME));
        PersonController c = new ControllerWithImage(failingService, source.toString());

        Person added = c.handleAdd(new Person("Nguyễn Văn Lỗi Ảnh", 1998, Gender.MALE, null));

        assertNotNull(added,
                "Thành viên ĐÃ được lưu vào DB → phải trả về đối tượng, không được báo thất bại");
        assertTrue(added.getId() > 0);

        int id = TestDatabase.queryIdByName(TEST_DB_NAME, "Nguyễn Văn Lỗi Ảnh");
        assertTrue(id > 0, "Bản ghi thành viên PHẢI tồn tại trong DB");
    }

    @Test
    @Order(6)
    @DisplayName("C2/M7 — Lỗi xử lý ảnh KHÔNG tạo bản ghi trùng lặp (chỉ đúng 1 bản ghi)")
    void imageFailureDoesNotCreateDuplicateRecords(@TempDir Path tempDir) throws IOException {
        System.setProperty("user.dir", tempDir.toAbsolutePath().toString());

        Path source = tempDir.resolve("nguon.png");
        Files.write(source, "fake-png-content".getBytes(StandardCharsets.UTF_8));

        FamilyTreeService failingService = new ServiceWithFailingPhotoUpdate(
                TestDatabaseManager.repositoryFor(TEST_DB_NAME));
        PersonController c = new ControllerWithImage(failingService, source.toString());

        c.handleAdd(new Person("Nguyễn Văn Không Trùng", 1997, Gender.MALE, null));

        int count = countByName("Nguyễn Văn Không Trùng");
        assertEquals(1, count,
                "Phải có ĐÚNG 1 bản ghi — lỗi ảnh không được sinh bản ghi trùng");

        // Tổng số bản ghi: 7 bản ghi mẫu + 1 bản ghi mới.
        assertEquals(TestDatabase.SAMPLE_RECORD_COUNT + 1, TestDatabase.countPersons(TEST_DB_NAME),
                "Không được có bản ghi thừa nào trong bảng person");
    }

    @Test
    @Order(7)
    @DisplayName("C2/M7 — updatePhotoPath chỉ cập nhật cột photo_path, KHÔNG ghi đè cột khác")
    void updatePhotoPathOnlyTouchesPhotoColumn() {
        Person before = service.getPersonById(5);
        String originalName = before.getFullName();
        int originalBirthYear = before.getBirthYear();
        Gender originalGender = before.getGender();
        int originalFatherId = before.getFather().getId();
        int originalMotherId = before.getMother().getId();

        service.updatePhotoPath(5, "images/person_5.png");

        Person after = service.getPersonById(5);
        assertEquals("images/person_5.png", after.getPhotoPath(), "photo_path phải được cập nhật");
        assertEquals(originalName, after.getFullName(), "Họ tên không được đổi");
        assertEquals(originalBirthYear, after.getBirthYear(), "Năm sinh không được đổi");
        assertEquals(originalGender, after.getGender(), "Giới tính không được đổi");
        assertEquals(originalFatherId, after.getFather().getId(), "Cha không được đổi");
        assertEquals(originalMotherId, after.getMother().getId(), "Mẹ không được đổi");
    }

    @Test
    @Order(8)
    @DisplayName("M7 — updatePhotoPath với đường dẫn rỗng → xóa ảnh đại diện (NULL trong DB)")
    void updatePhotoPathWithBlankClearsPhoto() {
        service.updatePhotoPath(5, "images/person_5.png");
        assertNotNull(TestDatabase.queryString(TEST_DB_NAME, "photo_path", "Nguyễn Văn Con"));

        service.updatePhotoPath(5, "   ");

        assertNull(TestDatabase.queryString(TEST_DB_NAME, "photo_path", "Nguyễn Văn Con"),
                "photo_path phải được đặt về NULL");
    }

    // ============================================================
    // NHÓM 3 — C3: ĐỒNG NHẤT VALIDATION TRÊN CÁC ĐƯỜNG GHI DỮ LIỆU
    // ============================================================

    @Test
    @Order(9)
    @DisplayName("C3 — addPerson: BR1 tự gán mình làm cha → SelfParentException")
    void addPersonRejectsSelfParent() {
        Person p = new Person("Tự Gán", 1990, Gender.MALE, null);
        p.setId(50);
        p.setFather(new PersonStub(50, Gender.MALE));

        assertThrows(SelfParentException.class, () -> service.addPerson(p));
    }

    @Test
    @Order(10)
    @DisplayName("C3 — addPerson: BR2 cha và mẹ là cùng một người → SameParentException")
    void addPersonRejectsSameParent() {
        Person p = new Person("Cha Mẹ Trùng", 1990, Gender.MALE, null);
        p.setFather(new PersonStub(1, Gender.MALE));
        p.setMother(new PersonStub(1, Gender.MALE));

        assertThrows(SameParentException.class, () -> service.addPerson(p));
    }

    @Test
    @Order(11)
    @DisplayName("C3 — addPerson: BR6 mẹ có giới tính MALE → InvalidParentGenderException")
    void addPersonRejectsWrongParentGender() {
        Person p = new Person("Mẹ Sai Giới Tính", 1990, Gender.MALE, null);
        p.setMother(new PersonStub(1, Gender.MALE)); // id 1 là MALE trong dữ liệu mẫu

        assertThrows(InvalidParentGenderException.class, () -> service.addPerson(p));
    }

    @Test
    @Order(12)
    @DisplayName("C3 — assignFather: BR6 cha không phải MALE → InvalidParentGenderException")
    void assignFatherRejectsWrongGender() {
        // id 2 là FEMALE → không thể làm cha
        assertThrows(InvalidParentGenderException.class,
                () -> service.assignFather(7, 2));
    }

    @Test
    @Order(13)
    @DisplayName("C3 — assignMother: BR6 mẹ không phải FEMALE → InvalidParentGenderException")
    void assignMotherRejectsWrongGender() {
        // id 1 là MALE → không thể làm mẹ
        assertThrows(InvalidParentGenderException.class,
                () -> service.assignMother(7, 1));
    }

    @Test
    @Order(14)
    @DisplayName("C3 — assignFather: BR1 tự gán → SelfParentException")
    void assignFatherRejectsSelfParent() {
        assertThrows(SelfParentException.class, () -> service.assignFather(5, 5));
    }

    @Test
    @Order(15)
    @DisplayName("C3 — assignMother: BR2 mẹ trùng cha hiện tại → SameParentException")
    void assignMotherRejectsSameParent() {
        // id 5 đang có cha = id 3 → gán mẹ = id 3 → BR2
        assertThrows(SameParentException.class, () -> service.assignMother(5, 3));
    }

    @Test
    @Order(16)
    @DisplayName("C3 — assignFather: BR3 tạo chu trình → CycleDetectedException")
    void assignFatherRejectsCycle() {
        // id 5 là cha của id 7 → gán cha id 7 cho id 5 tạo chu trình 5→7→5
        assertThrows(CycleDetectedException.class, () -> service.assignFather(5, 7));
    }

    @Test
    @Order(17)
    @DisplayName("C3 — updatePersonWithParents: BR6 sai giới tính → ném, và dữ liệu KHÔNG đổi")
    void updateWithParentsRejectsWrongGender() {
        Person target = service.getPersonById(5);
        Person updated = new Person("Tên Sẽ Không Được Lưu", 1985, Gender.MALE, null);
        updated.setId(5);
        updated.setFather(target.getFather());
        updated.setMother(target.getMother());

        // id 2 là FEMALE → làm cha sẽ vi phạm BR6
        assertThrows(InvalidParentGenderException.class,
                () -> service.updatePersonWithParents(updated, service.getPersonById(2), target.getMother()));

        Person after = service.getPersonById(5);
        assertEquals("Nguyễn Văn Con", after.getFullName(),
                "Dữ liệu KHÔNG được thay đổi khi validation thất bại");
    }

    @Test
    @Order(18)
    @DisplayName("C3 — updatePhotoPath với id không tồn tại → PersonNotFoundException")
    void updatePhotoPathThrowsForUnknownId() {
        assertThrows(com.giapha.exception.PersonNotFoundException.class,
                () -> service.updatePhotoPath(999999, "images/person_999999.png"));
    }

    @Test
    @Order(19)
    @DisplayName("C2/M7 — handleEdit không xóa ảnh đại diện khi người dùng không chọn ảnh mới")
    void editWithoutNewImageKeepsExistingPhoto() {
        service.updatePhotoPath(5, "images/person_5.png");

        Person target = service.getPersonById(5);
        // Person truyền vào KHÔNG có photoPath (null) — mô phỏng form chỉ sửa thông tin
        Person updated = new Person("Đổi Tên Giữ Ảnh", 1985, Gender.MALE, null);
        updated.setId(5);
        updated.setFather(target.getFather());
        updated.setMother(target.getMother());

        controller.handleEdit(updated, target.getFather(), target.getMother());

        assertEquals("images/person_5.png", TestDatabase.queryString(TEST_DB_NAME, "photo_path", "Đổi Tên Giữ Ảnh"),
                "Ảnh đại diện phải được giữ nguyên khi người dùng không chọn ảnh mới");
    }

    // ============================================================
    // HELPER
    // ============================================================

    /**
     * Controller ghi đè {@code getSelectedPhotoSourcePath()} để mô phỏng việc
     * người dùng chọn một file ảnh trong form — KHÔNG cần JavaFX Toolkit.
     */
    static class ControllerWithImage extends PersonController {
        private final String sourcePath;

        ControllerWithImage(FamilyTreeService service, String sourcePath) {
            super(service);
            this.sourcePath = sourcePath;
        }

        @Override
        protected String getSelectedPhotoSourcePath() {
            return sourcePath;
        }
    }

    /**
     * Service cố tình làm {@code updatePhotoPath()} thất bại để giả lập lỗi DB
     * trong bước lưu ảnh.
     */
    static class ServiceWithFailingPhotoUpdate extends FamilyTreeService {
        ServiceWithFailingPhotoUpdate(PersonRepository repository) {
            super(repository);
        }

        @Override
        public void updatePhotoPath(int id, String photoPath) {
            throw new RuntimeException("Lỗi DB giả lập: không thể cập nhật photo_path");
        }
    }

    /**
     * Person rút gọn chỉ mang id + giới tính — dùng làm cha/mẹ giả.
     */
    static class PersonStub extends Person {
        PersonStub(int id, Gender gender) {
            super("Stub", 1900, gender, null);
            setId(id);
        }
    }

    /**
     * Xóa các bản ghi thử nghiệm do chính lớp test này tạo ra, để mỗi test
     * bắt đầu với đúng 7 bản ghi mẫu (id 1..7).
     */
    private static void cleanupExtraRecords() {
        try (Connection conn = TestDatabaseManager.openConnection(TEST_DB_NAME);
             PreparedStatement ps = conn.prepareStatement("DELETE FROM person WHERE id > 7")) {
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Không dọn được bản ghi thử nghiệm: " + e.getMessage(), e);
        }
    }

    private static void setPhotoPath(int id, String path) {
        try (Connection conn = TestDatabaseManager.openConnection(TEST_DB_NAME);
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE person SET photo_path = ? WHERE id = ?")) {
            ps.setString(1, path);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Không set được photo_path: " + e.getMessage(), e);
        }
    }

    private static void clearPhotoPath(int id) {
        setPhotoPath(id, null);
    }

    private static int countByName(String name) {
        try (Connection conn = TestDatabaseManager.openConnection(TEST_DB_NAME);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM person WHERE full_name = ?")) {
            ps.setString(1, name);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Không đếm được bản ghi: " + e.getMessage(), e);
        }
    }

    /** Bảo đảm ImageManager không bị ảnh hưởng bởi thư mục ảnh thật. */
    @SuppressWarnings("unused")
    private static void sanityImageManager() {
        assertNotNull(ImageManager.IMAGE_DIR_NAME);
    }
}
