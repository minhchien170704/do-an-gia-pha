package com.giapha.controller;

import com.giapha.model.Gender;
import com.giapha.model.Person;
import com.giapha.service.FamilyTreeService;
import com.giapha.support.TestDatabase;
import com.giapha.support.TestDatabaseManager;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Kiểm thử hành vi {@code PersonController.handleEdit(Person, Person, Person)} khi
 * {@code FamilyTreeService} TỪ CHỐI quan hệ cha/mẹ (BR1, BR2, BR3, BR6).
 *
 * <p><b>Lỗi được sửa:</b> trước đây các {@code catch} trong {@code handleEdit} chỉ
 * gom lỗi vào {@code errorMsg} mà KHÔNG {@code return}. Luồng tiếp tục chạy xuống
 * bước ghi ảnh, hộp thoại mở đầu bằng "Đã cập nhật thành công..." dù service đã từ
 * chối, và ứng dụng điều hướng về danh sách. Hậu quả: ảnh đại diện vẫn được lưu
 * dù toàn bộ thao tác cập nhật đã bị từ chối.
 *
 * <p><b>Hành vi mong đợi sau khi sửa:</b>
 * <ul>
 *     <li>Service từ chối → <b>KHÔNG</b> ghi bất cứ thứ gì vào DB.</li>
 *     <li>Ảnh vừa copy vào {@code images/} phải được <b>dọn</b> (không để lại file rác).</li>
 *     <li><b>KHÔNG</b> điều hướng về danh sách — người dùng ở lại form để sửa.</li>
 * </ul>
 *
 * <p><b>Cô lập dữ liệu:</b> database riêng {@code gia_pha_editfail_test}
 * (tên kết thúc bằng {@code _test}), tự tạo ở {@code @BeforeAll} và xóa ở {@code @AfterAll}.
 * Mỗi test được {@code @BeforeEach}/{@code @AfterEach} đưa bảng {@code PERSON} về đúng
 * 7 bản ghi mẫu nguyên bản (bao gồm {@code photo_path = NULL}).
 *
 * <p>Test chạy NGOÀI JavaFX Application Thread nên {@code Alert} chỉ in ra console —
 * vì vậy test khẳng định <b>trạng thái</b> (DB, file ảnh, Navigator) chứ không
 * khẳng định nội dung hộp thoại.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("handleEdit — rollback khi service từ chối quan hệ (không ghi ảnh, không điều hướng)")
class EditFailureRollbackTest {

    private static final String TEST_DB_NAME = "gia_pha_editfail_test";

    /** Thư mục {@code images/} sinh ra cạnh ứng dụng (nơi {@code ImageManager} copy ảnh). */
    private static final Path IMAGE_DIR = Paths.get(System.getProperty("user.dir"), "images");

    private static FamilyTreeService service;

    /** Ảnh chụp danh sách file ảnh có sẵn TRƯỚC khi test chạy — để không xóa nhầm ảnh thật. */
    private Set<String> preExistingImages = new HashSet<>();

    @BeforeAll
    static void init() throws Exception {
        TestDatabaseManager.createFreshDatabase(TEST_DB_NAME);
        assumeTrue(TestDatabase.isAvailable(TEST_DB_NAME),
                "BỎ QUA: không kết nối được MySQL. Hãy khởi động MySQL trước khi chạy test.");
        service = new FamilyTreeService(TestDatabaseManager.repositoryFor(TEST_DB_NAME));
    }

    @AfterAll
    static void drop() throws Exception {
        TestDatabaseManager.dropDatabase(TEST_DB_NAME);
    }

    @BeforeEach
    void beforeEach() {
        preExistingImages = listImageFiles();
        resetSampleData();
    }

    @AfterEach
    void afterEach() {
        resetSampleData();
        cleanupGeneratedImages();
    }

    // ============================================================
    // CÁC CA THẤT BẠI — service từ chối quan hệ
    // ============================================================

    @Test
    @Order(1)
    @DisplayName("BR3 — chu trình gia phả → DB không đổi, ảnh vừa copy bị dọn, KHÔNG điều hướng")
    void cycleRejectionRollsBackEverything() throws IOException {
        // id 1 ("Nguyễn Văn Tổ") là tổ gốc. Gán id 7 ("Nguyễn Văn Cháu") làm cha
        // của id 1 → id 7 → 5 → 3 → 1 tạo chu trình vòng lặp.
        Person original = service.getPersonById(1);

        FakeFormDataSource form = new FakeFormDataSource();
        form.photoSourcePath = createTempImage("cycle");

        RecordingNavigator navigator = new RecordingNavigator();
        PersonController controller = new PersonController(service);
        controller.setFormDataSource(form);
        controller.setNavigator(navigator);

        Person updated = buildUpdate(1, "Tên Mới Nhưng Bị Từ Chối", 1930, original);
        controller.handleEdit(updated, service.getPersonById(7), null);

        assertRollback(1, "Nguyễn Văn Tổ", 1930, navigator, "BR3 chu trình");
    }

    @Test
    @Order(2)
    @DisplayName("BR6 — cha là nữ → DB không đổi, ảnh vừa copy bị dọn, KHÔNG điều hướng")
    void invalidParentGenderRollsBackEverything() throws IOException {
        // id 2 ("Trần Thị Tổ") là FEMALE → không thể làm CHA.
        Person original = service.getPersonById(7);

        FakeFormDataSource form = new FakeFormDataSource();
        form.photoSourcePath = createTempImage("gender");

        RecordingNavigator navigator = new RecordingNavigator();
        PersonController controller = new PersonController(service);
        controller.setFormDataSource(form);
        controller.setNavigator(navigator);

        Person updated = buildUpdate(7, "Tên Mới Nhưng Sai Giới Tính Cha", 2015, original);
        controller.handleEdit(updated, service.getPersonById(2), original.getMother());

        assertRollback(7, "Nguyễn Văn Cháu", 2015, navigator, "BR6 sai giới tính vai trò");
    }

    @Test
    @Order(3)
    @DisplayName("BR2 — cha và mẹ là cùng một người → DB không đổi, ảnh bị dọn, KHÔNG điều hướng")
    void sameParentRejectionRollsBackEverything() throws IOException {
        Person original = service.getPersonById(5);

        FakeFormDataSource form = new FakeFormDataSource();
        form.photoSourcePath = createTempImage("sameparent");

        RecordingNavigator navigator = new RecordingNavigator();
        PersonController controller = new PersonController(service);
        controller.setFormDataSource(form);
        controller.setNavigator(navigator);

        Person updated = buildUpdate(5, "Tên Mới Nhưng Cha Mẹ Trùng Nhau", 1985, original);
        // id 1 là MALE nên đồng thời làm cả cha và mẹ → BR2 bị kích hoạt trước BR6.
        controller.handleEdit(updated, service.getPersonById(1), service.getPersonById(1));

        assertRollback(5, "Nguyễn Văn Con", 1985, navigator, "BR2 cha mẹ trùng nhau");
    }

    @Test
    @Order(4)
    @DisplayName("BR1 — tự gán làm cha chính mình → DB không đổi, ảnh bị dọn, KHÔNG điều hướng")
    void selfParentRejectionRollsBackEverything() throws IOException {
        Person original = service.getPersonById(5);

        FakeFormDataSource form = new FakeFormDataSource();
        form.photoSourcePath = createTempImage("selfparent");

        RecordingNavigator navigator = new RecordingNavigator();
        PersonController controller = new PersonController(service);
        controller.setFormDataSource(form);
        controller.setNavigator(navigator);

        Person updated = buildUpdate(5, "Tên Mới Nhưng Tự Gán Làm Cha", 1985, original);
        // Cha = chính thành viên đang sửa (id 5) → BR1.
        controller.handleEdit(updated, service.getPersonById(5), original.getMother());

        assertRollback(5, "Nguyễn Văn Con", 1985, navigator, "BR1 tự gán làm cha");
    }

    // ============================================================
    // CA THÀNH CÔNG — ảnh VÀ thông tin đều được ghi, điều hướng đúng 1 lần
    // ============================================================

    @Test
    @Order(5)
    @DisplayName("Cập nhật hợp lệ → DB có tên mới + photo_path, Navigator được gọi đúng 1 lần")
    void validUpdatePersistsPhotoAndNavigates() throws IOException {
        Person original = service.getPersonById(5);

        FakeFormDataSource form = new FakeFormDataSource();
        form.photoSourcePath = createTempImage("success");

        RecordingNavigator navigator = new RecordingNavigator();
        PersonController controller = new PersonController(service);
        controller.setFormDataSource(form);
        controller.setNavigator(navigator);

        Person updated = buildUpdate(5, "Nguyễn Văn Con Đã Sửa", 1985, original);
        // Cha = id 3 (MALE), mẹ = id 4 (FEMALE) → hợp lệ, không tạo chu trình.
        controller.handleEdit(updated, service.getPersonById(3), service.getPersonById(4));

        // 1. Thông tin cá nhân đã được ghi.
        Person after = service.getPersonById(5);
        assertEquals("Nguyễn Văn Con Đã Sửa", after.getFullName(),
                "Tên mới phải được lưu vào DB");
        assertEquals(1985, after.getBirthYear());

        // 2. Ảnh đại diện đã được ghi với đường dẫn tương đối.
        assertEquals("images/person_5.png", after.getPhotoPath(),
                "photo_path phải được ghi vào DB");
        assertTrue(Files.isRegularFile(IMAGE_DIR.resolve("person_5.png")),
                "File ảnh phải tồn tại trong thư mục images/");

        // 3. Điều hướng về danh sách đúng 1 lần.
        assertEquals(1, navigator.peopleListCount,
                "Navigator phải nhận đúng 1 lệnh quay lại danh sách sau khi sửa thành công");
        assertEquals(0, navigator.detailCount);
        assertEquals(0, navigator.treeCount);
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    /**
     * Khẳng định trạng thái sau khi service TỪ CHỐI quan hệ:
     * DB không đổi, ảnh vừa copy bị dọn, Navigator KHÔNG được gọi.
     */
    private void assertRollback(int personId, String expectedName, int expectedBirthYear,
                                RecordingNavigator navigator, String context) {

        // 1. DB không đổi.
        Person after = service.getPersonById(personId);
        assertEquals(expectedName, after.getFullName(),
                context + " — tên KHÔNG được đổi khi thao tác bị từ chối");
        assertEquals(expectedBirthYear, after.getBirthYear(),
                context + " — năm sinh KHÔNG được đổi khi thao tác bị từ chối");
        assertNull(after.getPhotoPath(),
                context + " — photo_path KHÔNG được ghi khi thao tác bị từ chối");

        // 2. Ảnh vừa copy phải bị dọn — không để lại file rác trong images/.
        Path copiedImage = IMAGE_DIR.resolve("person_" + personId + ".png");
        assertFalse(Files.exists(copiedImage),
                context + " — ảnh vừa copy phải được dọn, nhưng file vẫn còn: " + copiedImage);

        // 3. KHÔNG được điều hướng — người dùng phải ở lại form.
        assertEquals(0, navigator.peopleListCount,
                context + " — KHÔNG được điều hướng về danh sách khi thao tác bị từ chối");
        assertEquals(0, navigator.detailCount,
                context + " — KHÔNG được điều hướng tới chi tiết khi thao tác bị từ chối");
        assertEquals(0, navigator.treeCount,
                context + " — KHÔNG được điều hướng tới cây gia phả khi thao tác bị từ chối");
    }

    /**
     * Tạo đối tượng Person mô tả thông tin MỚI mà người dùng muốn lưu,
     * giữ nguyên quan hệ cha/mẹ gốc để {@code fatherChanged}/{@code motherChanged}
     * so sánh được đúng.
     */
    private Person buildUpdate(int id, String newName, int birthYear, Person original) {
        Person updated = new Person(newName, birthYear, original.getGender(), null);
        updated.setId(id);
        updated.setFather(original.getFather());
        updated.setMother(original.getMother());
        return updated;
    }

    /**
     * Tạo một file ảnh PNG hợp lệ trong thư mục tạm để làm "ảnh nguồn" cho người dùng.
     *
     * <p>Chỉ cần nội dung là file tồn tại với đuôi {@code .png} — {@code ImageManager}
     * chỉ kiểm tra định dạng theo đuôi file, không đọc nội dung ảnh.
     */
    private String createTempImage(String prefix) throws IOException {
        Path dir = Files.createTempDirectory("editfail-" + prefix + "-");
        Path image = dir.resolve("source.png");
        Files.write(image, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
        return image.toAbsolutePath().toString();
    }

    /**
     * Đưa bảng {@code PERSON} về đúng 7 bản ghi mẫu nguyên bản.
     *
     * <p>Bổ sung việc xóa {@code photo_path} (helper dùng chung {@code TestDatabase}
     * không reset cột này) để bảo đảm mỗi test bắt đầu từ trạng thái sạch.
     */
    private void resetSampleData() {
        if (!TestDatabase.isAvailable(TEST_DB_NAME)) {
            return;
        }
        TestDatabase.resetToSampleState(TEST_DB_NAME);
        try (Connection conn = TestDatabaseManager.openConnection(TEST_DB_NAME);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE PERSON SET photo_path = NULL");
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Không thể đặt lại photo_path trong \"" + TEST_DB_NAME + "\": " + e.getMessage(), e);
        }
    }

    /**
     * Liệt kê tên các file ảnh đang có trong thư mục {@code images/}.
     */
    private static Set<String> listImageFiles() {
        Set<String> names = new HashSet<>();
        if (!Files.isDirectory(IMAGE_DIR)) {
            return names;
        }
        try (Stream<Path> stream = Files.list(IMAGE_DIR)) {
            stream.forEach(p -> names.add(p.getFileName().toString()));
        } catch (IOException ignored) {
            // Không đọc được thư mục → coi như không có file nào.
        }
        return names;
    }

    /**
     * Dọn các file ảnh do TEST sinh ra — KHÔNG đụng tới ảnh có sẵn trước khi test chạy
     * (ảnh thật của người dùng).
     */
    private void cleanupGeneratedImages() {
        for (String name : listImageFiles()) {
            if (!preExistingImages.contains(name)) {
                try {
                    Files.deleteIfExists(IMAGE_DIR.resolve(name));
                } catch (IOException ignored) {
                    // Dọn dẹp là thao tác phụ — bỏ qua lỗi.
                }
            }
        }
    }

    // ============================================================
    // FAKE IMPLEMENTATIONS — không cần JavaFX Toolkit
    // ============================================================

    /**
     * Fake {@link FormDataSource} — chỉ dùng kiểu dữ liệu thuần Java.
     */
    static class FakeFormDataSource implements FormDataSource {
        String fullName = "";
        String birthYear = "";
        Gender gender = null;
        Person father = null;
        Person mother = null;
        String photoSourcePath = null;
        String existingPhotoPath = null;
        Person editingPerson = null;

        @Override
        public String getFullNameInput() {
            return fullName;
        }

        @Override
        public String getBirthYearInput() {
            return birthYear;
        }

        @Override
        public Gender getGenderInput() {
            return gender;
        }

        @Override
        public Person getSelectedFather() {
            return father;
        }

        @Override
        public Person getSelectedMother() {
            return mother;
        }

        @Override
        public String getSelectedPhotoSourcePath() {
            return photoSourcePath;
        }

        @Override
        public String getExistingPhotoPath() {
            return existingPhotoPath;
        }

        @Override
        public Person getEditingPerson() {
            return editingPerson;
        }
    }

    /**
     * Fake {@link Navigator} — ghi nhận các hành vi điều hướng được yêu cầu.
     */
    static class RecordingNavigator implements Navigator {
        int peopleListCount = 0;
        int detailCount = 0;
        int treeCount = 0;
        Person lastDetail = null;
        Person lastFocus = null;
        final List<String> events = new ArrayList<>();

        @Override
        public void navigateToPeopleList() {
            peopleListCount++;
            events.add("peopleList");
        }

        @Override
        public void navigateToPersonDetail(Person person) {
            detailCount++;
            lastDetail = person;
            events.add("detail");
        }

        @Override
        public void navigateToFamilyTree(Person focusPerson) {
            treeCount++;
            lastFocus = focusPerson;
            events.add("tree");
        }
    }
}
