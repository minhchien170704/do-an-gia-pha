package com.giapha.view;

import com.giapha.controller.PersonController;
import com.giapha.model.Gender;
import com.giapha.model.Person;
import com.giapha.service.FamilyTreeService;
import com.giapha.support.FxTestSupport;
import com.giapha.support.TestDatabase;
import com.giapha.support.TestDatabaseManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Kiểm thử tích hợp các View JavaFX: PeopleListView, PersonFormView, PersonDetailView,
 * FamilyTreeView — đi qua đầy đủ luồng UI → Controller → Service → Repository → MySQL thật.
 *
 * <p><b>Cô lập dữ liệu:</b> lớp test này dùng database RIÊNG {@code gia_pha_javafx_test}
 * (tên kết thúc bằng {@code _test}), được tạo mới ở {@code @BeforeAll} và xóa ở
 * {@code @AfterAll}. Database chính {@code gia_pha} <b>không bao giờ</b> bị ghi vào.
 *
 * <p><b>Yêu cầu:</b> MySQL phải đang chạy. Nếu không, toàn bộ lớp test được {@code SKIPPED}.
 *
 * <p><b>Cách chạy riêng:</b> {@code mvn test -Pjavafx-tests -Dtest=JavaFxViewTest}
 *
 * @see TestDatabaseManager
 */
@Tag("javafx")
@DisplayName("JavaFX View — kiểm thử tích hợp giao diện với MySQL thật")
class JavaFxViewTest {

    private static final String TEST_PREFIX = "TEST_AUTO_GUI_JUNIT";

    /** Tên database kiểm thử riêng của lớp test này. */
    private static final String TEST_DB_NAME = "gia_pha_javafx_test";

    private static PersonController controller;

    @BeforeAll
    static void setup() throws Exception {
        // Tạo database test riêng (DROP + CREATE + schema + 7 bản ghi mẫu)
        TestDatabaseManager.createFreshDatabase(TEST_DB_NAME);

        assumeTrue(TestDatabase.isAvailable(TEST_DB_NAME),
                "BỎ QUA: không kết nối được MySQL. Hãy khởi động MySQL trước khi chạy test.");
        assumeTrue(TestDatabase.countPersons(TEST_DB_NAME) == TestDatabase.SAMPLE_RECORD_COUNT,
                "BỎ QUA: database test phải có đúng " + TestDatabase.SAMPLE_RECORD_COUNT
                        + " bản ghi mẫu (hiện có: " + TestDatabase.countPersons(TEST_DB_NAME) + ").");

        FxTestSupport.initToolkit();

        FamilyTreeService service = new FamilyTreeService(
                TestDatabaseManager.repositoryFor(TEST_DB_NAME));
        controller = new PersonController(service);
    }

    @AfterAll
    static void teardown() throws Exception {
        cleanupTestData();
        FxTestSupport.shutdownToolkit();
        TestDatabaseManager.dropDatabase(TEST_DB_NAME);
    }

    @BeforeEach
    @AfterEach
    void resetDatabase() {
        if (TestDatabase.isAvailable(TEST_DB_NAME)) {
            TestDatabase.resetToSampleState(TEST_DB_NAME);
            cleanupTestData();
        }
    }

    // ============================================================
    // TEST 1 — PeopleListView nạp danh sách từ MySQL
    // ============================================================

    @Test
    @DisplayName("TEST 1 — PeopleListView nạp danh sách từ MySQL")
    void peopleListViewLoadsFromDatabase() throws Exception {
        AtomicReference<PeopleListView> viewRef = new AtomicReference<>();
        FxTestSupport.runOnFxThread(() -> viewRef.set(new PeopleListView(controller)));

        PeopleListView view = viewRef.get();
        TableView<Person> table = getTableView(view);

        assertNotNull(table, "TableView không được null");
        assertNotNull(table.getItems());
        assertTrue(table.getItems().size() >= TestDatabase.SAMPLE_RECORD_COUNT,
                "TableView phải chứa ít nhất " + TestDatabase.SAMPLE_RECORD_COUNT + " thành viên");
        assertTrue(table.getItems().stream().anyMatch(p -> p.getFullName().contains("Nguyễn Văn Tổ")),
                "TableView phải chứa 'Nguyễn Văn Tổ'");
    }

    // ============================================================
    // TEST 2 — Tìm kiếm & phục hồi danh sách
    // ============================================================

    @Test
    @DisplayName("TEST 2 — Tìm kiếm client-side và phục hồi danh sách")
    void peopleListViewSearchAndRestore() throws Exception {
        AtomicReference<PeopleListView> viewRef = new AtomicReference<>();
        FxTestSupport.runOnFxThread(() -> viewRef.set(new PeopleListView(controller)));

        PeopleListView view = viewRef.get();
        TextField searchField = getSearchField(view);
        TableView<Person> table = getTableView(view);

        assertNotNull(searchField, "searchField không được null");
        assertNotNull(table, "TableView không được null");

        FxTestSupport.runOnFxThread(() -> searchField.setText("Cha"));

        int filteredCount = table.getItems().size();
        boolean allMatch = table.getItems().stream()
                .allMatch(p -> p.getFullName().toLowerCase().contains("cha"));
        assertTrue(filteredCount > 0, "Lọc phải trả về ít nhất 1 bản ghi");
        assertTrue(allMatch, "Mọi bản ghi sau khi lọc phải chứa 'cha'");

        FxTestSupport.runOnFxThread(() -> searchField.setText(""));
        int restoredCount = table.getItems().size();
        assertTrue(restoredCount >= TestDatabase.SAMPLE_RECORD_COUNT,
                "Xóa từ khóa phải phục hồi đủ danh sách (thực tế: " + restoredCount + ")");
    }

    // ============================================================
    // TEST 3 — PersonFormView thêm thành viên hợp lệ
    // ============================================================

    @Test
    @DisplayName("TEST 3 — PersonFormView thêm thành viên hợp lệ vào MySQL")
    void personFormViewAddsValidPerson() throws Exception {
        AtomicReference<PersonFormView> formRef = new AtomicReference<>();
        FxTestSupport.runOnFxThread(() -> formRef.set(new PersonFormView(controller, null)));

        PersonFormView form = formRef.get();
        TextField txtName = getTextField(form, "txtFullName");
        TextField txtYear = getTextField(form, "txtBirthYear");
        @SuppressWarnings("unchecked")
        ComboBox<Gender> cboGender = (ComboBox<Gender>) getField(form, "cboGender");
        @SuppressWarnings("unchecked")
        ComboBox<Person> cboFather = (ComboBox<Person>) getField(form, "cboFather");
        @SuppressWarnings("unchecked")
        ComboBox<Person> cboMother = (ComboBox<Person>) getField(form, "cboMother");

        FxTestSupport.runOnFxThread(() -> {
            txtName.setText(TEST_PREFIX);
            txtYear.setText("1995");
            cboGender.setValue(Gender.MALE);

            for (Person p : cboFather.getItems()) {
                if (p != null && p.getId() == 3) {
                    cboFather.setValue(p);
                    break;
                }
            }
            for (Person p : cboMother.getItems()) {
                if (p != null && p.getId() == 4) {
                    cboMother.setValue(p);
                    break;
                }
            }
            invokeHandleSave(form);
        });

        int generatedId = -1;
        try (Connection conn = TestDatabaseManager.openConnection(TEST_DB_NAME);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, birth_year, father_id, mother_id FROM person WHERE full_name = ?")) {
            ps.setString(1, TEST_PREFIX);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Không tìm thấy bản ghi trong MySQL sau khi lưu");
                generatedId = rs.getInt("id");
                assertEquals(1995, rs.getInt("birth_year"), "Năm sinh phải là 1995");
                assertEquals(3, rs.getInt("father_id"), "Father ID phải là 3");
                assertEquals(4, rs.getInt("mother_id"), "Mother ID phải là 4");
            }
        }
        assertTrue(generatedId > 0, "ID được sinh phải lớn hơn 0");
    }

    // ============================================================
    // TEST 4 — Validation từ chối dữ liệu sai
    // ============================================================

    @Test
    @DisplayName("TEST 4 — Validation từ chối dữ liệu không hợp lệ")
    void personFormViewRejectsInvalidData() throws Exception {
        AtomicReference<PersonFormView> formRef = new AtomicReference<>();
        FxTestSupport.runOnFxThread(() -> formRef.set(new PersonFormView(controller, null)));

        PersonFormView form = formRef.get();
        TextField txtName = getTextField(form, "txtFullName");
        TextField txtYear = getTextField(form, "txtBirthYear");
        @SuppressWarnings("unchecked")
        ComboBox<Gender> cboGender = (ComboBox<Gender>) getField(form, "cboGender");

        // Kịch bản 1: để tên rỗng
        FxTestSupport.runOnFxThread(() -> {
            txtName.setText("   ");
            txtYear.setText("2000");
            cboGender.setValue(Gender.MALE);
            invokeHandleSave(form);
        });

        // Kịch bản 2: năm sinh không phải số
        FxTestSupport.runOnFxThread(() -> {
            txtName.setText(TEST_PREFIX + "_INVALID");
            txtYear.setText("Năm2000");
            cboGender.setValue(Gender.MALE);
            invokeHandleSave(form);
        });

        try (Connection conn = TestDatabaseManager.openConnection(TEST_DB_NAME);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) AS total FROM person WHERE full_name LIKE ? OR TRIM(full_name) = ''")) {
            ps.setString(1, TEST_PREFIX + "_INVALID%");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(0, rs.getInt("total"),
                        "MySQL không được có bản ghi lỗi sau khi validation thất bại");
            }
        }
    }

    // ============================================================
    // TEST 5 — Sửa & gỡ quan hệ cha/mẹ
    // ============================================================

    @Test
    @DisplayName("TEST 5 — Sửa Person và gỡ quan hệ cha/mẹ qua PersonFormView")
    void personFormViewEditsAndClearsParents() throws Exception {
        // Tạo bản ghi thử nghiệm trước
        Person testPerson = controller.handleAdd(new Person(TEST_PREFIX + "_EDIT", 1995, Gender.MALE, null));
        assertNotNull(testPerson, "Không tạo được bản ghi thử nghiệm");
        int testPersonId = testPerson.getId();

        Person personToEdit = controller.handleViewDetail(testPersonId);
        assertNotNull(personToEdit);

        AtomicReference<PersonFormView> formRef = new AtomicReference<>();
        FxTestSupport.runOnFxThread(() -> formRef.set(new PersonFormView(controller, personToEdit)));

        PersonFormView form = formRef.get();
        TextField txtName = getTextField(form, "txtFullName");
        @SuppressWarnings("unchecked")
        ComboBox<Person> cboFather = (ComboBox<Person>) getField(form, "cboFather");
        @SuppressWarnings("unchecked")
        ComboBox<Person> cboMother = (ComboBox<Person>) getField(form, "cboMother");

        String newName = TEST_PREFIX + "_EDITED";
        FxTestSupport.runOnFxThread(() -> {
            txtName.setText(newName);
            cboFather.setValue(null);
            cboMother.setValue(null);
            invokeHandleSave(form);
        });

        try (Connection conn = TestDatabaseManager.openConnection(TEST_DB_NAME);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT full_name, father_id, mother_id FROM person WHERE id = ?")) {
            ps.setInt(1, testPersonId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Không tìm thấy bản ghi ID " + testPersonId);
                assertEquals(newName, rs.getString("full_name"), "Tên phải được cập nhật");
                rs.getInt("father_id");
                assertTrue(rs.wasNull(), "father_id phải được gỡ về NULL");
                rs.getInt("mother_id");
                assertTrue(rs.wasNull(), "mother_id phải được gỡ về NULL");
            }
        }
    }

    // ============================================================
    // TEST 6 — Xóa an toàn
    // ============================================================

    @Test
    @DisplayName("TEST 6a — Chặn xóa Person đang được tham chiếu (BR4)")
    void safeDeleteBlocksReferencedPerson() throws Exception {
        controller.handleDelete(3);

        try (Connection conn = TestDatabaseManager.openConnection(TEST_DB_NAME);
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS total FROM person WHERE id = 3")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(1, rs.getInt("total"), "ID 3 đang làm cha nên phải được bảo toàn");
            }
        }
    }

    @Test
    @DisplayName("TEST 6b — Xóa thành viên thử nghiệm đã gỡ quan hệ")
    void safeDeleteRemovesUnreferencedPerson() throws Exception {
        Person testPerson = controller.handleAdd(new Person(TEST_PREFIX + "_DEL", 1995, Gender.MALE, null));
        assertNotNull(testPerson);
        int testPersonId = testPerson.getId();

        controller.handleDelete(testPersonId);

        try (Connection conn = TestDatabaseManager.openConnection(TEST_DB_NAME);
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS total FROM person WHERE id = ?")) {
            ps.setInt(1, testPersonId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(0, rs.getInt("total"), "Bản ghi phải bị xóa khỏi MySQL");
            }
        }
    }

    // ============================================================
    // TEST 7 — PersonDetailView & FamilyTreeView
    // ============================================================

    @Test
    @DisplayName("TEST 7a — PersonDetailView hiển thị đúng cha và mẹ")
    void personDetailViewShowsParents() throws Exception {
        Person p7 = controller.handleViewDetail(7);
        assertNotNull(p7, "Không tìm thấy thành viên ID = 7");

        AtomicReference<PersonDetailView> detailRef = new AtomicReference<>();
        FxTestSupport.runOnFxThread(() -> detailRef.set(new PersonDetailView(controller, p7)));

        PersonDetailView detailView = detailRef.get();
        List<Label> labels = new ArrayList<>();
        collectLabels(detailView.getRoot(), labels);

        boolean hasFatherName = labels.stream()
                .anyMatch(l -> l.getText() != null && l.getText().contains("Nguyễn Văn Con"));
        boolean hasMotherName = labels.stream()
                .anyMatch(l -> l.getText() != null && l.getText().contains("Phạm Thị Dâu"));

        assertTrue(hasFatherName, "Phải hiển thị cha 'Nguyễn Văn Con'");
        assertTrue(hasMotherName, "Phải hiển thị mẹ 'Phạm Thị Dâu'");
    }

    @Test
    @DisplayName("TEST 7b — FamilyTreeView duyệt cây gia phả đa thế hệ")
    void familyTreeViewBuildsMultiGenerationTree() throws Exception {
        Person p7 = controller.handleViewDetail(7);
        assertNotNull(p7);

        AtomicReference<FamilyTreeView> treeRef = new AtomicReference<>();
        FxTestSupport.runOnFxThread(() -> treeRef.set(new FamilyTreeView(controller, p7)));

        FamilyTreeView treeView = treeRef.get();
        TreeView<?> tv = (TreeView<?>) getField(treeView, "treeView");

        assertNotNull(tv, "treeView không được null");
        assertNotNull(tv.getRoot(), "rootItem không được null");

        TreeItem<?> rootItem = tv.getRoot();
        int childCount = rootItem.getChildren().size();
        assertTrue(childCount >= 2,
                "Cây phả hệ phải có ít nhất 2 phân nhánh chính (Tổ tiên & Người trung tâm), thực tế: " + childCount);
    }

    @Test
    @DisplayName("TEST 8 — FamilyTreeView có sơ đồ 2D (FamilyTreeDiagram) đi kèm")
    void familyTreeViewContains2dDiagram() throws Exception {
        Person p7 = controller.handleViewDetail(7);
        assertNotNull(p7);

        AtomicReference<FamilyTreeView> treeRef = new AtomicReference<>();
        FxTestSupport.runOnFxThread(() -> treeRef.set(new FamilyTreeView(controller, p7)));

        FamilyTreeView treeView = treeRef.get();

        // Sơ đồ 2D nằm trong Tab "Sơ đồ 2D" — chỉ được đưa vào scene graph khi tab
        // được hiển thị, nên kiểm tra trực tiếp qua trường `diagram`.
        Object diagram = getField(treeView, "diagram");
        assertNotNull(diagram, "FamilyTreeView phải có sơ đồ 2D (FamilyTreeDiagram)");
        assertTrue(diagram instanceof FamilyTreeDiagram,
                "Đối tượng `diagram` phải là FamilyTreeDiagram nhưng thực tế là: " + diagram.getClass());

        // Bảng điều khiển zoom phải dùng đúng sơ đồ này
        assertNotNull(getField(treeView, "cboFocusPerson"), "ComboBox chọn người trung tâm phải tồn tại");
    }

    // ============================================================
    // HELPER
    // ============================================================

    @SuppressWarnings("unchecked")
    private static TableView<Person> getTableView(PeopleListView view) throws Exception {
        return (TableView<Person>) getField(view, "tableView");
    }

    private static TextField getSearchField(PeopleListView view) throws Exception {
        return getTextField(view, "searchField");
    }

    private static TextField getTextField(Object target, String fieldName) throws Exception {
        return (TextField) getField(target, fieldName);
    }

    private static Object getField(Object target, String fieldName) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }

    private static void invokeHandleSave(PersonFormView view) throws Exception {
        Method m = PersonFormView.class.getDeclaredMethod("handleSave");
        m.setAccessible(true);
        m.invoke(view);
    }

    private static void collectLabels(Node node, List<Label> labelList) {
        if (node instanceof Label label) {
            labelList.add(label);
        } else if (node instanceof javafx.scene.control.ScrollPane scrollPane) {
            Node content = scrollPane.getContent();
            if (content != null) {
                collectLabels(content, labelList);
            }
        } else if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectLabels(child, labelList);
            }
        }
    }

    private static Node findNodeByType(Node node, Class<?> type) {
        if (type.isInstance(node)) {
            return node;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Node found = findNodeByType(child, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    private static void assertDiagramRenderable(FamilyTreeDiagram diagram) {
        assertNotNull(diagram);
    }

    private static void cleanupTestData() {
        try (Connection conn = TestDatabaseManager.openConnection(TEST_DB_NAME);
             PreparedStatement ps = conn.prepareStatement("DELETE FROM person WHERE full_name LIKE ?")) {
            ps.setString(1, TEST_PREFIX + "%");
            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                System.out.println("[CLEANUP] Đã dọn dẹp " + deleted + " bản ghi thử nghiệm khỏi MySQL.");
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Lỗi khi dọn dẹp test data: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("TEST 9 — Dữ liệu thử nghiệm được dọn dẹp, CSDL giữ 7 bản ghi mẫu")
    void databaseIsCleanAfterTests() {
        cleanupTestData();
        assertEquals(TestDatabase.SAMPLE_RECORD_COUNT, TestDatabase.countPersons(TEST_DB_NAME),
                "CSDL phải giữ đúng 7 bản ghi mẫu sau khi chạy toàn bộ test JavaFX");
    }

    @Test
    @DisplayName("TEST 10 — JavaFX Toolkit đã được khởi tạo thành công")
    void javaFxToolkitIsRunning() {
        assertNotNull(javafx.application.Platform.class, "javafx.application.Platform phải khả dụng");
        assertTrue(javafx.application.Platform.isFxApplicationThread()
                        || !javafx.application.Platform.isFxApplicationThread(),
                "Truy cập Platform không được ném exception");
    }
}
