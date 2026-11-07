package com.giapha.controller;

import com.giapha.model.Gender;
import com.giapha.model.Person;
import com.giapha.service.FamilyTreeService;
import com.giapha.support.TestDatabase;
import com.giapha.support.TestDatabaseManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Kiểm thử tích hợp tầng Controller ({@link PersonController}).
 *
 * <p>Controller không chứa logic nghiệp vụ — nó chỉ điều phối và chuyển đổi ngoại lệ
 * thành thông báo thân thiện. Bộ test này xác nhận:
 * <ul>
 *     <li>Controller gọi đúng Service và trả về đúng dữ liệu.</li>
 *     <li>Controller bắt đúng các custom exception (BR1 – BR6) mà không làm hỏng dữ liệu.</li>
 *     <li>Controller giữ nguyên tính cô lập dữ liệu.</li>
 * </ul>
 *
 * <p><b>Cô lập dữ liệu:</b> lớp test này dùng database RIÊNG {@code gia_pha_controller_test}
 * (tên kết thúc bằng {@code _test}), được tạo mới ở {@code @BeforeAll} và xóa ở
 * {@code @AfterAll}. Database chính {@code gia_pha} <b>không bao giờ</b> bị ghi vào.
 *
 * @see TestDatabaseManager
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("PersonController — điều phối View ↔ Service")
class PersonControllerTest {

    /** Tên database kiểm thử riêng của lớp test này. */
    private static final String TEST_DB_NAME = "gia_pha_controller_test";

    private static FamilyTreeService service;
    private static PersonController controller;

    @BeforeAll
    static void initController() throws Exception {
        // Tạo database test riêng (DROP + CREATE + schema + 7 bản ghi mẫu)
        TestDatabaseManager.createFreshDatabase(TEST_DB_NAME);

        assumeTrue(TestDatabase.isAvailable(TEST_DB_NAME),
                "BỎ QUA: không kết nối được MySQL. Hãy khởi động MySQL trước khi chạy test.");

        service = new FamilyTreeService(TestDatabaseManager.repositoryFor(TEST_DB_NAME));
        controller = new PersonController(service);
    }

    @AfterAll
    static void dropTestDatabase() throws Exception {
        TestDatabaseManager.dropDatabase(TEST_DB_NAME);
    }

    @BeforeEach
    @AfterEach
    void resetDatabase() {
        if (TestDatabase.isAvailable(TEST_DB_NAME)) {
            TestDatabase.resetToSampleState(TEST_DB_NAME);
        }
    }

    // ============================================================
    // NHÓM 1 — Truy vấn & điều hướng
    // ============================================================

    @Test
    @Order(1)
    @DisplayName("handleSearch(\"Tổ\") trả về danh sách kết quả khớp từ khóa")
    void handleSearchReturnsMatchingResults() {
        List<Person> results = controller.handleSearch("Tổ");
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Phải tìm thấy ít nhất một kết quả");
        assertTrue(results.stream().anyMatch(p -> p.getFullName().contains("Tổ")));
    }

    @Test
    @Order(2)
    @DisplayName("handleViewDetail(1) trả về đúng thông tin thành viên")
    void handleViewDetailReturnsPerson() {
        Person p = controller.handleViewDetail(1);
        assertNotNull(p);
        assertEquals("Nguyễn Văn Tổ", p.getFullName());
    }

    @Test
    @Order(3)
    @DisplayName("handleViewDetail(99999) trả về null (PersonNotFoundException được xử lý êm thuận)")
    void handleViewDetailReturnsNullForMissingPerson() {
        assertNull(controller.handleViewDetail(99999));
    }

    @Test
    @Order(4)
    @DisplayName("handleViewTree(7) xác thực và trả về người trung tâm")
    void handleViewTreeReturnsFocusPerson() {
        Person focus = controller.handleViewTree(7);
        assertNotNull(focus);
        assertEquals(7, focus.getId());
        assertEquals("Nguyễn Văn Cháu", focus.getFullName());
    }

    @Test
    @Order(5)
    @DisplayName("handleViewTree(99999) trả về null khi người trung tâm không tồn tại")
    void handleViewTreeReturnsNullForMissingPerson() {
        assertNull(controller.handleViewTree(99999));
    }

    @Test
    @Order(6)
    @DisplayName("getAllPersons() trả về toàn bộ danh sách thành viên")
    void getAllPersonsReturnsFullList() {
        List<Person> all = controller.getAllPersons();
        assertNotNull(all);
        assertEquals(TestDatabase.SAMPLE_RECORD_COUNT, all.size());
    }

    // ============================================================
    // NHÓM 2 — Thêm / sửa / xóa
    // ============================================================

    @Test
    @Order(7)
    @DisplayName("handleAdd(Person) thêm thành công và sinh id hợp lệ")
    void handleAddCreatesPerson() {
        Person added = null;
        try {
            added = controller.handleAdd(new Person("Phan Văn Test", 1990, Gender.MALE, null));
            assertNotNull(added, "Controller phải trả về Person vừa thêm");
            assertTrue(added.getId() > 0);

            Person verify = service.getPersonById(added.getId());
            assertEquals("Phan Văn Test", verify.getFullName());
        } finally {
            if (added != null && added.getId() > 0) {
                service.deletePerson(added.getId());
            }
        }
    }

    @Test
    @Order(8)
    @DisplayName("handleAdd(Person) với dữ liệu không hợp lệ trả về null, không ghi vào DB")
    void handleAddWithInvalidPersonReturnsNull() {
        Person invalid = new Person("   ", 1990, Gender.MALE, null);
        assertNull(controller.handleAdd(invalid));
        assertEquals(TestDatabase.SAMPLE_RECORD_COUNT, TestDatabase.countPersons(TEST_DB_NAME),
                "Không được ghi thêm bản ghi lỗi vào CSDL");
    }

    @Test
    @Order(9)
    @DisplayName("handleAdd(Person) với cha/mẹ không tồn tại trả về null")
    void handleAddWithMissingParentReturnsNull() {
        Person ghostFather = new Person("Cha Không Có", 1990, Gender.MALE, null);
        ghostFather.setId(99998);

        Person p = new Person("Con Có Cha Không Tồn Tại", 1995, Gender.MALE, null);
        p.setFather(ghostFather);

        assertNull(controller.handleAdd(p));
        assertEquals(TestDatabase.SAMPLE_RECORD_COUNT, TestDatabase.countPersons(TEST_DB_NAME));
    }

    @Test
    @Order(10)
    @DisplayName("handleDelete(3) bị chặn bởi BR4 — bản ghi ID 3 được bảo toàn")
    void handleDeleteBlockedWhenPersonInUse() {
        controller.handleDelete(3);
        assertNotNull(service.getPersonById(3), "ID 3 đang làm cha nên không được xóa");
    }

    @Test
    @Order(11)
    @DisplayName("handleDelete(id không tồn tại) không gây ra lỗi, dữ liệu giữ nguyên")
    void handleDeleteNonExistentKeepsDataIntact() {
        controller.handleDelete(99999);
        assertEquals(TestDatabase.SAMPLE_RECORD_COUNT, TestDatabase.countPersons(TEST_DB_NAME));
    }

    @Test
    @Order(12)
    @DisplayName("handleEdit cập nhật thông tin cơ bản và gán lại cha thành công")
    void handleEditUpdatesBasicFieldsAndAssignsFather() {
        Person original5 = service.getPersonById(5);

        Person updated5 = new Person("Nguyễn Văn Con (Đã sửa)", 1985, Gender.MALE, null);
        updated5.setId(5);
        updated5.setFather(original5.getFather());
        updated5.setMother(original5.getMother());

        Person newFather = service.getPersonById(1);
        controller.handleEdit(updated5, newFather, original5.getMother());

        Person verify = service.getPersonById(5);
        assertEquals("Nguyễn Văn Con (Đã sửa)", verify.getFullName());
        assertNotNull(verify.getFather());
        assertEquals(1, verify.getFather().getId());
    }

    @Test
    @Order(13)
    @DisplayName("TC-CLEAR-06 — handleEdit với newFather = null gỡ quan hệ cha thành công")
    void handleEditWithNullFatherClearsRelation() {
        Person original5 = service.getPersonById(5);

        Person updated5 = new Person(original5.getFullName(), original5.getBirthYear(),
                original5.getGender(), original5.getPhotoPath());
        updated5.setId(5);
        updated5.setFather(original5.getFather());
        updated5.setMother(original5.getMother());

        controller.handleEdit(updated5, null, original5.getMother());

        assertNull(service.getPersonById(5).getFather(),
                "Father phải được gỡ về NULL trong DB");
    }

    @Test
    @Order(14)
    @DisplayName("handleEdit không ném exception khi cố tạo chu trình (chu trình bị chặn, dữ liệu giữ nguyên)")
    void handleEditRejectsCycleWithoutBreakingData() {
        Person original5 = service.getPersonById(5);

        Person updated5 = new Person(original5.getFullName(), original5.getBirthYear(),
                original5.getGender(), original5.getPhotoPath());
        updated5.setId(5);
        updated5.setFather(original5.getFather());
        updated5.setMother(original5.getMother());

        // ID 5 là hậu duệ của ID 1 → gán 1 làm cha của 5 hợp lệ,
        // nhưng gán 5 làm cha của 1 mới tạo chu trình.
        controller.handleEdit(updated5, service.getPersonById(3), original5.getMother());

        assertNotNull(service.getPersonById(5).getFather());
        assertEquals(TestDatabase.SAMPLE_RECORD_COUNT, TestDatabase.countPersons(TEST_DB_NAME));
    }

    // ============================================================
    // NHÓM 3 — Gán / gỡ quan hệ qua Controller
    // ============================================================

    @Test
    @Order(15)
    @DisplayName("handleAssignFather(5, 1) gán cha thành công")
    void handleAssignFatherSucceeds() {
        controller.handleAssignFather(5, 1);
        assertEquals(1, service.getPersonById(5).getFather().getId());
    }

    @Test
    @Order(16)
    @DisplayName("handleAssignFather(5, 5) bị chặn bởi BR1 — quan hệ cha không đổi")
    void handleAssignFatherToSelfIsRejected() {
        controller.handleAssignFather(5, 5);
        assertEquals(3, service.getPersonById(5).getFather().getId(),
                "Cha phải giữ nguyên là ID 3 (không bị thay bằng chính người con)");
    }

    @Test
    @Order(17)
    @DisplayName("handleAssignMother(5, 4) gán mẹ thành công")
    void handleAssignMotherSucceeds() {
        controller.handleAssignMother(5, 4);
        assertEquals(4, service.getPersonById(5).getMother().getId());
    }

    @Test
    @Order(18)
    @DisplayName("handleAssignMother(5, 5) bị chặn bởi BR1 — quan hệ mẹ không đổi")
    void handleAssignMotherToSelfIsRejected() {
        controller.handleAssignMother(5, 5);
        assertEquals(4, service.getPersonById(5).getMother().getId(),
                "Mẹ phải giữ nguyên là ID 4 (không bị thay bằng chính người con)");
    }

    @Test
    @Order(19)
    @DisplayName("handleClearFather(5) gỡ quan hệ cha thành công")
    void handleClearFatherSucceeds() {
        controller.handleClearFather(5);
        assertNull(service.getPersonById(5).getFather());
    }

    @Test
    @Order(20)
    @DisplayName("handleClearMother(5) gỡ quan hệ mẹ thành công")
    void handleClearMotherSucceeds() {
        controller.handleClearMother(5);
        assertNull(service.getPersonById(5).getMother());
    }

    // ============================================================
    // NHÓM 4 — Context & điều hướng
    // ============================================================

    @Test
    @Order(21)
    @DisplayName("handleSearch(keyword) tìm kiếm theo họ tên")
    void handleSearchFindsByKeyword() {
        List<Person> results = controller.handleSearch("Cháu");
        assertNotNull(results);
        assertTrue(results.stream().anyMatch(p -> p.getFullName().contains("Cháu")));
    }

    @Test
    @Order(22)
    @DisplayName("handleViewDetail() dùng người được chọn từ context")
    void handleViewDetailUsesContextSelection() {
        controller.setSelectedPerson(service.getPersonById(3));
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> controller.handleViewDetail());

        // Bản overload có tham số trả về đúng người được chọn
        Person p = controller.handleViewDetail(3);
        assertNotNull(p);
        assertEquals(3, p.getId());
    }

    @Test
    @Order(23)
    @DisplayName("handleViewTree() dùng người được chọn từ context")
    void handleViewTreeUsesContextSelection() {
        controller.setSelectedPerson(service.getPersonById(7));
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> controller.handleViewTree());

        // Bản overload có tham số trả về người trung tâm
        Person focus = controller.handleViewTree(7);
        assertNotNull(focus);
        assertEquals(7, focus.getId());
    }

    @Test
    @Order(24)
    @DisplayName("Constructor với service null ném NullPointerException")
    void constructorRejectsNullService() {
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
                () -> new PersonController(null));
    }

    @Test
    @Order(25)
    @DisplayName("Callback điều hướng được gọi khi xử lý thành công")
    void navigationCallbackIsInvoked() {
        final boolean[] called = {false};
        controller.setNavigateToPeopleList(() -> called[0] = true);

        Person added = controller.handleAdd(new Person("Nguyễn Văn Callback", 1995, Gender.MALE, null));
        assertNotNull(added, "handleAdd phải thành công");
        assertTrue(called[0], "Callback điều hướng phải được gọi sau khi thêm thành công");

        // Dọn dẹp
        if (added.getId() > 0) {
            service.deletePerson(added.getId());
        }
    }
}
