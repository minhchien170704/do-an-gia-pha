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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Kiểm thử tầng Controller với <b>fake implementation</b> của các abstraction
 * mới (GĐ2B.1 — DECOUPLE PERSONCONTROLLER ↔ VIEW).
 *
 * <p><b>Mục tiêu kiểm thử:</b>
 * <ul>
 *     <li>Controller đọc dữ liệu form qua {@link FormDataSource} và gọi đúng Service.</li>
 *     <li>Controller phát hành đúng hành vi điều hướng qua {@link Navigator}.</li>
 *     <li>Controller dùng {@link PeopleListSource} để lấy dòng đang chọn và
 *         yêu cầu làm mới danh sách sau khi xóa.</li>
 *     <li>Dữ liệu đi qua abstraction KHÔNG phụ thuộc JavaFX Control —
 *         fake chỉ dùng {@code String}/{@code Gender}/{@code Person}.</li>
 *     <li>Các luồng thành công và thất bại giữ nguyên kết quả như trước refactor.</li>
 * </ul>
 *
 * <p><b>Không cần JavaFX Toolkit:</b> lớp test này KHÔNG khởi tạo JavaFX Toolkit.
 * Khi chạy ngoài FX Application Thread, Controller ghi thông báo ra console thay vì
 * hiển thị {@code Alert} — nhờ vậy test chạy được hoàn toàn độc lập giao diện.
 *
 * <p><b>Cô lập dữ liệu:</b> database riêng {@code gia_pha_abstraction_test}
 * (tên kết thúc bằng {@code _test}) — không bao giờ ghi vào database chính {@code gia_pha}.
 *
 * @see TestDatabaseManager
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("GĐ2B.1 — Controller ↔ View decoupling: fake FormDataSource / Navigator / PeopleListSource")
class PersonControllerAbstractionTest {

    private static final String TEST_DB_NAME = "gia_pha_abstraction_test";

    private static FamilyTreeService service;

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
    @AfterEach
    void reset() {
        if (TestDatabase.isAvailable(TEST_DB_NAME)) {
            TestDatabase.resetToSampleState(TEST_DB_NAME);
            cleanupExtraRecords();
        }
    }

    /**
     * Xóa các bản ghi thử nghiệm do chính lớp test này tạo ra (id > 7) để mỗi test
     * bắt đầu với đúng 7 bản ghi mẫu.
     *
     * <p>Các bản ghi này không có con nên xóa trực tiếp được — không vi phạm
     * ràng buộc khóa ngoại ON DELETE RESTRICT.
     */
    private static void cleanupExtraRecords() {
        try (java.sql.Connection conn = TestDatabaseManager.openConnection(TEST_DB_NAME);
             java.sql.PreparedStatement ps = conn.prepareStatement("DELETE FROM person WHERE id > 7")) {
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Không dọn được bản ghi thử nghiệm: " + e.getMessage(), e);
        }
    }

    // ============================================================
    // NHÓM 1 — FormDataSource: Controller đọc dữ liệu form và gọi đúng Service
    // ============================================================

    @Test
    @Order(1)
    @DisplayName("handleAdd() đọc dữ liệu qua FormDataSource và tạo đúng bản ghi trong CSDL")
    void handleAddReadsFormDataThroughAbstraction() {
        Person father = service.getPersonById(3);   // MALE — hợp lệ làm cha
        Person mother = service.getPersonById(4);   // FEMALE — hợp lệ làm mẹ

        FakeFormDataSource form = new FakeFormDataSource();
        form.fullName = "Nguyễn Văn Qua Abstraction";
        form.birthYear = "1995";
        form.gender = Gender.MALE;
        form.father = father;
        form.mother = mother;

        PersonController controller = new PersonController(service);
        controller.setFormDataSource(form);

        controller.handleAdd();

        int newId = TestDatabase.queryIdByName(TEST_DB_NAME, "Nguyễn Văn Qua Abstraction");
        assertTrue(newId > 0, "Controller phải tạo được thành viên từ dữ liệu của FormDataSource");

        Person verify = service.getPersonById(newId);
        assertEquals("Nguyễn Văn Qua Abstraction", verify.getFullName(),
                "Họ tên phải lấy từ FormDataSource");
        assertEquals(1995, verify.getBirthYear(),
                "Năm sinh phải được phân tích từ chuỗi do FormDataSource cung cấp");
        assertEquals(Gender.MALE, verify.getGender(),
                "Giới tính phải lấy từ FormDataSource");
        assertNotNull(verify.getFather(), "Cha phải lấy từ FormDataSource");
        assertEquals(3, verify.getFather().getId());
        assertNotNull(verify.getMother(), "Mẹ phải lấy từ FormDataSource");
        assertEquals(4, verify.getMother().getId());
    }

    @Test
    @Order(2)
    @DisplayName("handleAdd() từ chối họ tên rỗng — không ghi vào CSDL")
    void handleAddRejectsBlankNameFromAbstraction() {
        FakeFormDataSource form = new FakeFormDataSource();
        form.fullName = "   ";
        form.birthYear = "1995";
        form.gender = Gender.MALE;

        PersonController controller = new PersonController(service);
        controller.setFormDataSource(form);

        assertDoesNotThrow(() -> controller.handleAdd());
        assertEquals(TestDatabase.SAMPLE_RECORD_COUNT, TestDatabase.countPersons(TEST_DB_NAME),
                "Không được ghi bản ghi khi họ tên rỗng");
    }

    @Test
    @Order(3)
    @DisplayName("handleAdd() từ chối năm sinh không phải số — không ghi vào CSDL")
    void handleAddRejectsNonNumericBirthYearFromAbstraction() {
        FakeFormDataSource form = new FakeFormDataSource();
        form.fullName = "Năm Sinh Sai Định Dạng";
        form.birthYear = "Năm2000";
        form.gender = Gender.MALE;

        PersonController controller = new PersonController(service);
        controller.setFormDataSource(form);

        assertDoesNotThrow(() -> controller.handleAdd());
        assertEquals(TestDatabase.SAMPLE_RECORD_COUNT, TestDatabase.countPersons(TEST_DB_NAME));
    }

    @Test
    @Order(4)
    @DisplayName("handleAdd() từ chối khi chưa chọn giới tính — không ghi vào CSDL")
    void handleAddRejectsMissingGenderFromAbstraction() {
        FakeFormDataSource form = new FakeFormDataSource();
        form.fullName = "Chưa Chọn Giới Tính";
        form.birthYear = "1995";
        form.gender = null;

        PersonController controller = new PersonController(service);
        controller.setFormDataSource(form);

        assertDoesNotThrow(() -> controller.handleAdd());
        assertEquals(TestDatabase.SAMPLE_RECORD_COUNT, TestDatabase.countPersons(TEST_DB_NAME));
    }

    @Test
    @Order(5)
    @DisplayName("handleAdd() không có FormDataSource → báo lỗi, không ghi vào CSDL")
    void handleAddWithoutFormDataSourceFailsSafely() {
        PersonController controller = new PersonController(service);

        assertDoesNotThrow(() -> controller.handleAdd());
        assertEquals(TestDatabase.SAMPLE_RECORD_COUNT, TestDatabase.countPersons(TEST_DB_NAME),
                "Không được ghi bản ghi khi không có nguồn dữ liệu form");
    }

    @Test
    @Order(6)
    @DisplayName("handleEdit() đọc dữ liệu qua FormDataSource và cập nhật nguyên tử")
    void handleEditReadsFormDataThroughAbstraction() {
        Person target = service.getPersonById(5);

        FakeFormDataSource form = new FakeFormDataSource();
        form.editingPerson = target;
        form.fullName = "Nguyễn Văn Con (Sửa Qua Abstraction)";
        form.birthYear = "1986";
        form.gender = Gender.MALE;
        form.father = target.getFather();
        form.mother = target.getMother();
        form.existingPhotoPath = target.getPhotoPath();

        PersonController controller = new PersonController(service);
        controller.setFormDataSource(form);

        controller.handleEdit();

        Person verify = service.getPersonById(5);
        assertEquals("Nguyễn Văn Con (Sửa Qua Abstraction)", verify.getFullName());
        assertEquals(1986, verify.getBirthYear());
        assertEquals(Gender.MALE, verify.getGender());
        assertNotNull(verify.getFather(), "Cha phải được giữ nguyên");
        assertNotNull(verify.getMother(), "Mẹ phải được giữ nguyên");
    }

    @Test
    @Order(7)
    @DisplayName("handleEdit() gỡ được quan hệ cha/mẹ khi FormDataSource trả về null")
    void handleEditClearsRelationsViaAbstraction() {
        Person target = service.getPersonById(5);

        FakeFormDataSource form = new FakeFormDataSource();
        form.editingPerson = target;
        form.fullName = target.getFullName();
        form.birthYear = String.valueOf(target.getBirthYear());
        form.gender = target.getGender();
        form.father = null;   // gỡ cha
        form.mother = null;   // gỡ mẹ
        form.existingPhotoPath = target.getPhotoPath();

        PersonController controller = new PersonController(service);
        controller.setFormDataSource(form);

        controller.handleEdit();

        Person verify = service.getPersonById(5);
        assertNull(verify.getFather(), "Cha phải được gỡ về NULL");
        assertNull(verify.getMother(), "Mẹ phải được gỡ về NULL");
    }

    // ============================================================
    // NHÓM 2 — Navigator: Controller phát hành đúng hành vi điều hướng
    // ============================================================

    @Test
    @Order(8)
    @DisplayName("handleAdd() thành công → Navigator.navigateToPeopleList() được gọi")
    void navigatorReceivesPeopleListAfterAdd() {
        FakeFormDataSource form = new FakeFormDataSource();
        form.fullName = "Nguyễn Văn Điều Hướng";
        form.birthYear = "1995";
        form.gender = Gender.MALE;

        RecordingNavigator navigator = new RecordingNavigator();

        PersonController controller = new PersonController(service);
        controller.setFormDataSource(form);
        controller.setNavigator(navigator);

        controller.handleAdd();

        assertEquals(1, navigator.peopleListCount,
                "Navigator phải nhận đúng 1 lệnh quay lại danh sách sau khi thêm thành công");
        assertEquals(0, navigator.detailCount,
                "Không được điều hướng tới chi tiết khi thêm thành viên");
        assertEquals(0, navigator.treeCount,
                "Không được điều hướng tới cây gia phả khi thêm thành viên");
    }

    @Test
    @Order(9)
    @DisplayName("handleEdit() thành công → Navigator.navigateToPeopleList() được gọi")
    void navigatorReceivesPeopleListAfterEdit() {
        Person target = service.getPersonById(5);

        FakeFormDataSource form = new FakeFormDataSource();
        form.editingPerson = target;
        form.fullName = "Nguyễn Văn Con (Điều Hướng)";
        form.birthYear = String.valueOf(target.getBirthYear());
        form.gender = target.getGender();
        form.father = target.getFather();
        form.mother = target.getMother();
        form.existingPhotoPath = target.getPhotoPath();

        RecordingNavigator navigator = new RecordingNavigator();

        PersonController controller = new PersonController(service);
        controller.setFormDataSource(form);
        controller.setNavigator(navigator);

        controller.handleEdit();

        assertEquals(1, navigator.peopleListCount,
                "Navigator phải nhận lệnh quay lại danh sách sau khi sửa thành công");
    }

    @Test
    @Order(10)
    @DisplayName("handleViewDetail(id) → Navigator.navigateToPersonDetail(person) được gọi đúng người")
    void navigatorReceivesPersonDetail() {
        RecordingNavigator navigator = new RecordingNavigator();

        PersonController controller = new PersonController(service);
        controller.setNavigator(navigator);

        Person returned = controller.handleViewDetail(7);

        assertNotNull(returned, "handleViewDetail phải trả về đối tượng Person");
        assertEquals(1, navigator.detailCount, "Navigator phải nhận 1 lệnh mở chi tiết");
        assertNotNull(navigator.lastDetail, "Navigator phải nhận đúng đối tượng Person cần xem");
        assertSame(returned, navigator.lastDetail,
                "Navigator phải nhận CÙNG instance Person mà Controller trả về");
        assertEquals(7, navigator.lastDetail.getId());
    }

    @Test
    @Order(11)
    @DisplayName("handleViewTree(id) → Navigator.navigateToFamilyTree(focusPerson) được gọi đúng người")
    void navigatorReceivesFamilyTree() {
        RecordingNavigator navigator = new RecordingNavigator();

        PersonController controller = new PersonController(service);
        controller.setNavigator(navigator);

        Person focus = controller.handleViewTree(7);

        assertNotNull(focus);
        assertEquals(1, navigator.treeCount, "Navigator phải nhận 1 lệnh mở cây gia phả");
        assertNotNull(navigator.lastFocus, "Navigator phải nhận đúng người trung tâm cây gia phả");
        assertSame(focus, navigator.lastFocus,
                "Navigator phải nhận CÙNG instance Person làm trung tâm");
        assertEquals(7, navigator.lastFocus.getId());
    }

    @Test
    @Order(12)
    @DisplayName("handleViewDetail(id không tồn tại) → Navigator KHÔNG được gọi")
    void navigatorNotInvokedOnFailure() {
        RecordingNavigator navigator = new RecordingNavigator();

        PersonController controller = new PersonController(service);
        controller.setNavigator(navigator);

        assertNull(controller.handleViewDetail(99999));
        assertEquals(0, navigator.detailCount,
                "Khi thất bại thì KHÔNG được điều hướng đi bất cứ đâu");
        assertEquals(0, navigator.peopleListCount);
        assertEquals(0, navigator.treeCount);
    }

    @Test
    @Order(13)
    @DisplayName("handleViewTree(id không tồn tại) → Navigator KHÔNG được gọi")
    void navigatorNotInvokedOnTreeFailure() {
        RecordingNavigator navigator = new RecordingNavigator();

        PersonController controller = new PersonController(service);
        controller.setNavigator(navigator);

        assertNull(controller.handleViewTree(99999));
        assertEquals(0, navigator.treeCount,
                "Khi không tìm thấy người trung tâm thì KHÔNG được điều hướng");
    }

    @Test
    @Order(14)
    @DisplayName("Controller KHÔNG cần Navigator để hoạt động — không ném NullPointerException")
    void controllerWorksWithoutNavigator() {
        FakeFormDataSource form = new FakeFormDataSource();
        form.fullName = "Nguyễn Văn Không Cần Navigator";
        form.birthYear = "1995";
        form.gender = Gender.MALE;

        PersonController controller = new PersonController(service);
        controller.setFormDataSource(form);

        assertDoesNotThrow(() -> controller.handleAdd());
        assertTrue(TestDatabase.queryIdByName(TEST_DB_NAME, "Nguyễn Văn Không Cần Navigator") > 0,
                "Thiếu Navigator không được làm hỏng luồng nghiệp vụ");
    }

    // ============================================================
    // NHÓM 3 — PeopleListSource: chọn dòng & làm mới danh sách
    // ============================================================

    @Test
    @Order(15)
    @DisplayName("handleDelete() dùng PeopleListSource.getSelectedPerson() khi chưa có context")
    void deleteUsesPeopleListSourceSelection() {
        // id 7 là người cuối cùng trong cây (không có con) → xóa được
        Person target = service.getPersonById(7);

        FakePeopleListSource list = new FakePeopleListSource();
        list.selectedPerson = target;

        PersonController controller = new PersonController(service);
        controller.setPeopleListSource(list);

        controller.handleDelete();

        assertEquals(-1, TestDatabase.queryIdByName(TEST_DB_NAME, "Nguyễn Văn Cháu"),
                "Thành viên phải bị xóa khỏi CSDL");
        assertEquals(1, list.refreshCount,
                "PeopleListSource.refresh() phải được gọi sau khi xóa thành công");
    }

    @Test
    @Order(16)
    @DisplayName("handleDelete() bị chặn bởi BR4 → KHÔNG gọi refresh()")
    void deleteBlockedByBr4DoesNotRefresh() {
        // id 3 là CHA của id 5 → BR4 chặn xóa
        Person target = service.getPersonById(3);

        FakePeopleListSource list = new FakePeopleListSource();
        list.selectedPerson = target;

        PersonController controller = new PersonController(service);
        controller.setPeopleListSource(list);

        controller.handleDelete();

        assertNotNull(service.getPersonById(3),
                "Bản ghi ID 3 đang được tham chiếu nên phải được bảo toàn");
        assertEquals(0, list.refreshCount,
                "Xóa thất bại thì KHÔNG được làm mới danh sách");
    }

    @Test
    @Order(17)
    @DisplayName("handleViewDetail() dùng PeopleListSource.getSelectedPerson() khi chưa có context")
    void viewDetailUsesPeopleListSourceSelection() {
        Person target = service.getPersonById(4);

        FakePeopleListSource list = new FakePeopleListSource();
        list.selectedPerson = target;

        RecordingNavigator navigator = new RecordingNavigator();

        PersonController controller = new PersonController(service);
        controller.setPeopleListSource(list);
        controller.setNavigator(navigator);

        controller.handleViewDetail();

        assertEquals(1, navigator.detailCount, "Navigator phải nhận lệnh mở chi tiết");
        assertNotNull(navigator.lastDetail);
        assertEquals(4, navigator.lastDetail.getId(),
                "Phải mở chi tiết đúng người đang chọn trên bảng");
    }

    @Test
    @Order(18)
    @DisplayName("handleViewTree() dùng PeopleListSource.getSelectedPerson() khi chưa có context")
    void viewTreeUsesPeopleListSourceSelection() {
        Person target = service.getPersonById(7);

        FakePeopleListSource list = new FakePeopleListSource();
        list.selectedPerson = target;

        RecordingNavigator navigator = new RecordingNavigator();

        PersonController controller = new PersonController(service);
        controller.setPeopleListSource(list);
        controller.setNavigator(navigator);

        controller.handleViewTree();

        assertEquals(1, navigator.treeCount, "Navigator phải nhận lệnh mở cây gia phả");
        assertNotNull(navigator.lastFocus);
        assertEquals(7, navigator.lastFocus.getId(),
                "Phải mở cây gia phả với đúng người trung tâm đang chọn trên bảng");
    }

    @Test
    @Order(19)
    @DisplayName("handleViewDetail() không có người được chọn → KHÔNG điều hướng")
    void viewDetailWithoutSelectionDoesNotNavigate() {
        FakePeopleListSource list = new FakePeopleListSource();
        list.selectedPerson = null;

        RecordingNavigator navigator = new RecordingNavigator();

        PersonController controller = new PersonController(service);
        controller.setPeopleListSource(list);
        controller.setNavigator(navigator);

        controller.handleViewDetail();

        assertEquals(0, navigator.detailCount,
                "Không có người được chọn thì KHÔNG được điều hướng");
    }

    // ============================================================
    // NHÓM 4 — Tính trừu tượng: dữ liệu qua abstraction không phụ thuộc JavaFX
    // ============================================================

    @Test
    @Order(20)
    @DisplayName("Các kiểu dữ liệu qua abstraction KHÔNG phải JavaFX Control/Node")
    void abstractionTypesArePlainJava() {
        // FormDataSource chỉ dùng kiểu dữ liệu thuần Java — không có javafx.* nào.
        for (java.lang.reflect.Method m : FormDataSource.class.getMethods()) {
            assertFalse(m.getReturnType().getName().startsWith("javafx."),
                    "FormDataSource không được trả về kiểu JavaFX: " + m.getReturnType());
            for (Class<?> p : m.getParameterTypes()) {
                assertFalse(p.getName().startsWith("javafx."),
                        "FormDataSource không được tham số kiểu JavaFX: " + p);
            }
        }

        for (java.lang.reflect.Method m : Navigator.class.getMethods()) {
            assertFalse(m.getReturnType().getName().startsWith("javafx."),
                    "Navigator không được trả về kiểu JavaFX: " + m.getReturnType());
            for (Class<?> p : m.getParameterTypes()) {
                assertFalse(p.getName().startsWith("javafx."),
                        "Navigator không được tham số kiểu JavaFX: " + p);
            }
        }

        for (java.lang.reflect.Method m : PeopleListSource.class.getMethods()) {
            assertFalse(m.getReturnType().getName().startsWith("javafx."),
                    "PeopleListSource không được trả về kiểu JavaFX: " + m.getReturnType());
            for (Class<?> p : m.getParameterTypes()) {
                assertFalse(p.getName().startsWith("javafx."),
                        "PeopleListSource không được tham số kiểu JavaFX: " + p);
            }
        }
    }

    @Test
    @Order(21)
    @DisplayName("PersonController KHÔNG import bất kỳ lớp nào trong com.giapha.view.*")
    void controllerDoesNotDependOnViewPackage() {
        // Kiểm tra ở mức bytecode: không có lớp nào của com.giapha.view trong
        // các hằng số pool / kiểu tham chiếu của PersonController.
        for (java.lang.reflect.Method m : PersonController.class.getDeclaredMethods()) {
            assertNotJavaFxViewType(m.getReturnType(), "return type của " + m.getName());
            for (Class<?> p : m.getParameterTypes()) {
                assertNotJavaFxViewType(p, "tham số của " + m.getName());
            }
        }
        for (java.lang.reflect.Field f : PersonController.class.getDeclaredFields()) {
            assertNotJavaFxViewType(f.getType(), "trường " + f.getName());
        }
    }

    @Test
    @Order(22)
    @DisplayName("Dữ liệu truyền qua FormDataSource là bản sao độc lập — không rò rỉ trạng thái View")
    void formDataSourceCarriesPlainValues() {
        FakeFormDataSource form = new FakeFormDataSource();
        form.fullName = "Nguyễn Văn Độc Lập";
        form.birthYear = "1995";
        form.gender = Gender.MALE;
        form.father = new Person("Cha Giả", 1950, Gender.MALE, null);
        form.father.setId(77);

        assertEquals("Nguyễn Văn Độc Lập", form.getFullNameInput());
        assertEquals("1995", form.getBirthYearInput());
        assertEquals(Gender.MALE, form.getGenderInput());
        assertEquals(77, form.getSelectedFather().getId());
        assertNull(form.getSelectedMother(), "Mẹ chưa chọn phải trả về null");
        assertNull(form.getSelectedPhotoSourcePath(), "Chưa chọn ảnh phải trả về null");
        assertNull(form.getExistingPhotoPath(), "Chưa có ảnh đã lưu phải trả về null");
        assertNull(form.getEditingPerson(), "Form thêm mới không có người đang sửa");
    }

    @Test
    @Order(23)
    @DisplayName("Fake Navigator ghi nhận đúng thứ tự hành vi điều hướng")
    void navigatorRecordsInvocationOrder() {
        RecordingNavigator navigator = new RecordingNavigator();

        PersonController controller = new PersonController(service);
        controller.setNavigator(navigator);

        controller.handleViewDetail(3);
        controller.handleViewTree(7);

        assertEquals(List.of("detail", "tree"), navigator.events,
                "Navigator phải nhận đúng thứ tự hành vi điều hướng được yêu cầu");
    }

    // ============================================================
    // HELPER — Fake implementation
    // ============================================================

    /**
     * Fake {@link FormDataSource} — chỉ dùng kiểu dữ liệu thuần Java,
     * KHÔNG cần JavaFX Toolkit.
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
     * Fake {@link PeopleListSource} — đếm số lần refresh() được gọi.
     */
    static class FakePeopleListSource implements PeopleListSource {
        Person selectedPerson = null;
        int refreshCount = 0;

        @Override
        public Person getSelectedPerson() {
            return selectedPerson;
        }

        @Override
        public void refresh() {
            refreshCount++;
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

    /**
     * Khẳng định kiểu dữ liệu không thuộc gói {@code com.giapha.view}.
     */
    private static void assertNotJavaFxViewType(Class<?> type, String context) {
        assertFalse(type.getName().startsWith("com.giapha.view"),
                "PersonController không được phụ thuộc lớp View concrete (" + context + "): " + type.getName());
    }
}
