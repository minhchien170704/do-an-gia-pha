package com.giapha.controller;

import com.giapha.model.Gender;
import com.giapha.model.Person;
import com.giapha.service.FamilyTreeService;
import com.giapha.support.TestDatabase;
import com.giapha.support.TestDatabaseManager;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Kiểm thử hai lỗi nghiệp vụ đã được sửa:
 * <ol>
 *     <li><b>BR5 bị bỏ qua trong luồng lưu thực tế</b> — trước đây {@code PersonFormView}
 *         gọi {@code handleAdd(Person)} / {@code handleEdit(Person, Person, Person)} là các
 *         phương thức KHÔNG thực hiện kiểm tra {@code checkBirthYearWarning()}. Nay kiểm tra
 *         BR5 nằm trong chính các phương thức lưu đó.</li>
 *     <li><b>Cập nhật cha/mẹ không nguyên tử</b> — trước đây updatePerson → assignFather →
 *         assignMother chạy tuần tự, nên khi thao tác sau thất bại thì dữ liệu bị "cập nhật
 *         một nửa". Nay dùng {@code updatePersonWithParents()} — kiểm tra toàn bộ trước,
 *         ghi MỘT lần.</li>
 * </ol>
 *
 * <p><b>Cô lập dữ liệu:</b> database riêng {@code gia_pha_atomic_test}
 * (tên kết thúc bằng {@code _test}).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Tính nguyên tử cập nhật & BR5 trong luồng lưu thực tế")
class AtomicUpdateAndBr5Test {

    private static final String TEST_DB_NAME = "gia_pha_atomic_test";

    private static FamilyTreeService service;
    private static PersonController controller;

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
    @AfterEach
    void reset() {
        if (TestDatabase.isAvailable(TEST_DB_NAME)) {
            TestDatabase.resetToSampleState(TEST_DB_NAME);
        }
    }

    /**
     * Service wrapper đếm số lần {@code checkBirthYearWarning()} được gọi.
     */
    static class CountingService extends FamilyTreeService {
        int br5Calls = 0;

        CountingService(com.giapha.repository.PersonRepository r) {
            super(r);
        }

        @Override
        public List<String> checkBirthYearWarning(Person c, Person f, Person m) {
            br5Calls++;
            return super.checkBirthYearWarning(c, f, m);
        }
    }

    // ============================================================
    // NHÓM 1 — BR5 phải được kiểm tra trong luồng lưu thực tế
    // ============================================================

    @Test
    @Order(1)
    @DisplayName("handleAdd(Person) — luồng lưu thực tế — PHẢI gọi checkBirthYearWarning (BR5)")
    void addPathInvokesBr5Check() {
        CountingService cs = new CountingService(TestDatabaseManager.repositoryFor(TEST_DB_NAME));
        PersonController c = new PersonController(cs);

        // id 3 = "Nguyễn Văn Cha" sinh 1955. Con sinh 1950 → cha sinh sau con → vi phạm BR5
        Person child = new Person("Test BR5 Add", 1950, Gender.MALE, null);
        child.setFather(service.getPersonById(3));

        c.handleAdd(child);

        assertEquals(1, cs.br5Calls,
                "handleAdd(Person) phải gọi checkBirthYearWarning đúng 1 lần "
                        + "(đây là phương thức lưu thực sự từ PersonFormView)");
    }

    @Test
    @Order(2)
    @DisplayName("handleAdd(Person) KHÔNG gọi BR5 khi không có cha/mẹ (không hỏi vô ích)")
    void addPathSkipsBr5WhenNoParents() {
        CountingService cs = new CountingService(TestDatabaseManager.repositoryFor(TEST_DB_NAME));
        PersonController c = new PersonController(cs);

        c.handleAdd(new Person("Test BR5 Khong Co Cha Me", 1990, Gender.MALE, null));

        assertEquals(0, cs.br5Calls,
                "Không có cha/mẹ thì không có cảnh báo BR5 nào để hiển thị");
    }

    @Test
    @Order(3)
    @DisplayName("handleEdit(Person, Person, Person) — luồng lưu thực tế — PHẢI gọi BR5 khi đổi quan hệ")
    void editPathInvokesBr5Check() {
        CountingService cs = new CountingService(TestDatabaseManager.repositoryFor(TEST_DB_NAME));
        PersonController c = new PersonController(cs);

        Person target = service.getPersonById(5);
        Person updated = new Person("Test BR5 Edit", 1990, Gender.MALE, null);
        updated.setId(5);
        updated.setFather(target.getFather());
        updated.setMother(target.getMother());

        // Đổi cha từ id 3 (1955) sang id 1 (1930) → khác id → quan hệ THAY ĐỔI
        c.handleEdit(updated, service.getPersonById(1), target.getMother());

        assertEquals(1, cs.br5Calls,
                "handleEdit(Person, Person, Person) phải gọi checkBirthYearWarning khi quan hệ thay đổi");
    }

    @Test
    @Order(4)
    @DisplayName("handleEdit(Person, Person, Person) KHÔNG gọi BR5 khi quan hệ không đổi")
    void editPathSkipsBr5WhenUnchanged() {
        CountingService cs = new CountingService(TestDatabaseManager.repositoryFor(TEST_DB_NAME));
        PersonController c = new PersonController(cs);

        Person target = service.getPersonById(5);
        Person updated = new Person("Test BR5 Khong Doi", 1990, Gender.MALE, null);
        updated.setId(5);
        updated.setFather(target.getFather());
        updated.setMother(target.getMother());

        c.handleEdit(updated, target.getFather(), target.getMother());

        assertEquals(0, cs.br5Calls,
                "Quan hệ cha/mẹ không đổi thì không cần cảnh báo BR5");
    }

    // ============================================================
    // NHÓM 2 — Cập nhật cha/mẹ phải NGUYÊN TỬ
    // ============================================================

    @Test
    @Order(5)
    @DisplayName("Gán cha THÀNH CÔNG nhưng gán mẹ THẤT BẠI → dữ liệu KHÔNG bị cập nhật một nửa")
    void failedMotherUpdateLeavesDataIntact() {
        // Chuẩn bị: id 5 "Nguyễn Văn Con" 1985, cha=3, mẹ=4
        // Tạo một nữ HẬU DUỆ của id 5 → dùng làm mẹ mới sẽ tạo chu trình (BR3)
        Person femaleDescendant = new Person("Test Hau Due Nu", 1990, Gender.FEMALE, null);
        femaleDescendant.setFather(service.getPersonById(5));
        femaleDescendant.setMother(service.getPersonById(4));
        femaleDescendant = service.addPerson(femaleDescendant);
        int femaleId = femaleDescendant.getId();

        Person target = service.getPersonById(5);
        Person updated = new Person("Tên Đã Đổi Nhưng Sẽ Không Được Lưu", 1985, Gender.MALE, null);
        updated.setId(5);
        updated.setFather(target.getFather());
        updated.setMother(target.getMother());

        int originalFatherId = target.getFather().getId();
        int originalMotherId = target.getMother().getId();
        String originalName = target.getFullName();

        // newFather = id 1 ("Nguyễn Văn Tổ") hợp lệ, không tạo chu trình
        // newMother = femaleId (nữ hậu duệ) → CycleDetectedException
        controller.handleEdit(updated, service.getPersonById(1), service.getPersonById(femaleId));

        Person after = service.getPersonById(5);

        assertEquals(originalName, after.getFullName(),
                "Tên KHÔNG được đổi — thao tác phải bị hủy hoàn toàn (tính nguyên tử)");
        assertEquals(originalFatherId, after.getFather().getId(),
                "Cha KHÔNG được đổi — dữ liệu không được cập nhật một nửa");
        assertEquals(originalMotherId, after.getMother().getId(),
                "Mẹ phải giữ nguyên");
    }

    @Test
    @Order(6)
    @DisplayName("Gán mẹ THÀNH CÔNG nhưng gán cha THẤT BẠI → dữ liệu KHÔNG bị cập nhật một nửa")
    void failedFatherUpdateLeavesDataIntact() {
        // id 5, gán cha = hậu duệ nam (id 7) → tạo chu trình BR3
        // mẹ = id 4 hợp lệ
        Person target = service.getPersonById(5);
        Person updated = new Person("Tên Đổi Nhưng Cha Lỗi", 1985, Gender.MALE, null);
        updated.setId(5);
        updated.setFather(target.getFather());
        updated.setMother(target.getMother());

        String originalName = target.getFullName();
        int originalMotherId = target.getMother().getId();

        controller.handleEdit(updated, service.getPersonById(7), service.getPersonById(4));

        Person after = service.getPersonById(5);

        assertEquals(originalName, after.getFullName(),
                "Tên KHÔNG được đổi khi thao tác thất bại");
        assertEquals(originalMotherId, after.getMother().getId(),
                "Mẹ KHÔNG được đổi — dữ liệu không được cập nhật một nửa");
    }

    @Test
    @Order(7)
    @DisplayName("Cập nhật hợp lệ (đổi cả cha và mẹ) → ghi đúng một lần, cả hai quan hệ đều đổi")
    void validUpdateChangesBothRelations() {
        Person target = service.getPersonById(5);
        Person updated = new Person("Cập Nhật Nguyên Tử", 1985, Gender.MALE, null);
        updated.setId(5);
        updated.setFather(target.getFather());
        updated.setMother(target.getMother());

        controller.handleEdit(updated, service.getPersonById(1), service.getPersonById(2));

        Person after = service.getPersonById(5);
        assertEquals("Cập Nhật Nguyên Tử", after.getFullName());
        assertEquals(1, after.getFather().getId(), "Cha phải là id 1");
        assertEquals(2, after.getMother().getId(), "Mẹ phải là id 2");
    }

    @Test
    @Order(8)
    @DisplayName("updatePersonWithParents — gỡ cha (null) vẫn giữ mẹ, ghi một lần")
    void clearingFatherKeepsMother() {
        Person target = service.getPersonById(5);
        Person updated = new Person("Gỡ Cha", 1985, Gender.MALE, null);
        updated.setId(5);
        updated.setFather(target.getFather());
        updated.setMother(target.getMother());

        controller.handleEdit(updated, null, target.getMother());

        Person after = service.getPersonById(5);
        assertNull(after.getFather(), "Cha phải được gỡ về NULL");
        assertNotNull(after.getMother(), "Mẹ phải được giữ nguyên");
        assertEquals(target.getMother().getId(), after.getMother().getId());
    }

    @Test
    @Order(9)
    @DisplayName("BR2 — cha và mẹ cùng một người → bị từ chối, dữ liệu giữ nguyên")
    void sameParentRejectedAtomically() {
        Person target = service.getPersonById(5);
        Person updated = new Person("Cha Mẹ Trùng Nhau", 1985, Gender.MALE, null);
        updated.setId(5);
        updated.setFather(target.getFather());
        updated.setMother(target.getMother());

        String originalName = target.getFullName();

        // id 1 là MALE → làm cả cha và mẹ → BR2
        controller.handleEdit(updated, service.getPersonById(1), service.getPersonById(1));

        Person after = service.getPersonById(5);
        assertEquals(originalName, after.getFullName(), "Dữ liệu phải giữ nguyên");
        assertEquals(target.getFather().getId(), after.getFather().getId());
    }
}
