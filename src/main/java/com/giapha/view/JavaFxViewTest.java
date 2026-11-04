package com.giapha.view;

import com.giapha.controller.PersonController;
import com.giapha.database.DatabaseConnection;
import com.giapha.model.Gender;
import com.giapha.model.Person;
import com.giapha.repository.JdbcPersonRepository;
import com.giapha.service.FamilyTreeService;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Phase 5.10 — Native In-Process JavaFX Test Harness.
 * Kiểm thử tự động tích hợp các View JavaFX hiện có bằng chính API của JavaFX,
 * kết nối backend FamilyTreeService -> PersonController -> MySQL thật.
 * Không sử dụng TestFX / JUnit theo đúng yêu cầu kiến trúc.
 */
public class JavaFxViewTest {

    private static final String TEST_PREFIX = "TEST_AUTO_GUI_510";

    private static int passedCount = 0;
    private static int failedCount = 0;

    private static Stage dummyStage;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  BẮT ĐẦU CHẠY KIỂM THỬ NATIVE JAVAFX VIEW TEST   ");
        System.out.println("==================================================");

        // 1. Kiểm tra trạng thái cơ sở dữ liệu ban đầu
        int initialCount = getDbRecordCount();
        System.out.println("[INFO] Số bản ghi MySQL trước kiểm thử: " + initialCount);
        if (initialCount != 7) {
            System.err.println("[BLOCKED] Cơ sở dữ liệu gia_pha không ở trạng thái mẫu 7 bản ghi (hiện có: " + initialCount + ").");
            System.exit(1);
        }

        // 2. Khởi tạo JavaFX Toolkit an toàn
        initJavaFxToolkit();

        // 3. Khởi tạo chuỗi backend thật: Repo -> Service -> Controller
        JdbcPersonRepository repository = new JdbcPersonRepository();
        FamilyTreeService service = new FamilyTreeService(repository);
        PersonController controller = new PersonController(service);

        int createdTestPersonId = -1;

        try {
            // ----------------------------------------------------
            // TEST 1: PeopleListView nạp danh sách từ MySQL
            // ----------------------------------------------------
            testPeopleListViewLoad(controller);

            // ----------------------------------------------------
            // TEST 2: Client-side Search & Restore trong PeopleListView
            // ----------------------------------------------------
            testPeopleListViewSearch(controller);

            // ----------------------------------------------------
            // TEST 3: PersonFormView Thêm thành viên hợp lệ
            // ----------------------------------------------------
            createdTestPersonId = testPersonFormAdd(controller);

            // ----------------------------------------------------
            // TEST 4: PersonFormView Validation từ chối dữ liệu sai
            // ----------------------------------------------------
            testPersonFormValidation(controller);

            // ----------------------------------------------------
            // TEST 5: PersonFormView Sửa và Gỡ quan hệ cha/mẹ
            // ----------------------------------------------------
            if (createdTestPersonId > 0) {
                testPersonFormEditAndClearParent(controller, createdTestPersonId);
            } else {
                fail("TEST 5: Sửa & gỡ quan hệ", "Không thể chạy vì TEST 3 chưa tạo được bản ghi thử nghiệm");
            }

            // ----------------------------------------------------
            // TEST 6: Xóa an toàn — Chặn xóa người được tham chiếu (BR4) & Xóa test person
            // ----------------------------------------------------
            testSafeDelete(controller, createdTestPersonId);
            createdTestPersonId = -1; // Đã xóa thành công

            // ----------------------------------------------------
            // TEST 7: PersonDetailView & FamilyTreeView hiển thị quan hệ
            // ----------------------------------------------------
            testDetailAndFamilyTreeView(controller);

        } catch (Throwable t) {
            System.err.println("[ERROR] Ngoại lệ ngoài dự kiến trong quá trình kiểm thử: " + t.getMessage());
            t.printStackTrace();
            failedCount++;
        } finally {
            // Dọn dẹp dữ liệu thử nghiệm trong MySQL (nếu còn sót lại)
            cleanupTestData();

            // Xác minh lại trạng thái cơ sở dữ liệu
            int finalCount = getDbRecordCount();
            System.out.println("[INFO] Số bản ghi MySQL sau kiểm thử: " + finalCount);

            // Đóng JavaFX Toolkit
            shutdownJavaFxToolkit();

            System.out.println("==================================================");
            System.out.println("  TỔNG KẾT: " + passedCount + " PASSED, " + failedCount + " FAILED");
            System.out.println("==================================================");

            if (failedCount > 0 || finalCount != 7) {
                System.exit(1);
            } else {
                System.exit(0);
            }
        }
    }

    // =========================================================================
    // CÁC TEST CASES CHI TIẾT
    // =========================================================================

    /**
     * TEST 1: Kiểm tra PeopleListView nạp danh sách 7 người từ MySQL và hiển thị trên TableView.
     */
    private static void testPeopleListViewLoad(PersonController controller) {
        try {
            AtomicReference<PeopleListView> viewRef = new AtomicReference<>();
            runOnFxThread(() -> {
                PeopleListView view = new PeopleListView(controller);
                viewRef.set(view);
            });

            PeopleListView view = viewRef.get();
            TableView<Person> table = getTableView(view);

            if (table != null && table.getItems() != null && table.getItems().size() >= 7) {
                boolean hasTo = table.getItems().stream().anyMatch(p -> p.getFullName().contains("Nguyễn Văn Tổ"));
                if (hasTo) {
                    pass("TEST 1: PeopleListView nạp dữ liệu từ MySQL",
                            "TableView chứa " + table.getItems().size() + " thành viên từ DB, có 'Nguyễn Văn Tổ'");
                } else {
                    fail("TEST 1: PeopleListView nạp dữ liệu từ MySQL", "Không tìm thấy 'Nguyễn Văn Tổ' trong TableView");
                }
            } else {
                fail("TEST 1: PeopleListView nạp dữ liệu từ MySQL",
                        "TableView null hoặc số lượng bản ghi không đủ: " + (table != null ? table.getItems().size() : 0));
            }
        } catch (Exception e) {
            fail("TEST 1: PeopleListView nạp dữ liệu từ MySQL", "Ngoại lệ: " + e.getMessage());
        }
    }

    /**
     * TEST 2: Kiểm tra chức năng tìm kiếm Client-side trên TableView và phục hồi danh sách khi xóa từ khóa.
     */
    private static void testPeopleListViewSearch(PersonController controller) {
        try {
            AtomicReference<PeopleListView> viewRef = new AtomicReference<>();
            runOnFxThread(() -> {
                PeopleListView view = new PeopleListView(controller);
                viewRef.set(view);
            });

            PeopleListView view = viewRef.get();
            TextField searchField = getSearchField(view);
            TableView<Person> table = getTableView(view);

            if (searchField == null || table == null) {
                fail("TEST 2: Tìm kiếm và phục hồi danh sách", "Không tìm thấy searchField hoặc TableView");
                return;
            }

            // Gõ "Cha" vào ô tìm kiếm trên JavaFX Thread
            runOnFxThread(() -> searchField.setText("Cha"));

            int filteredCount = table.getItems().size();
            boolean match = table.getItems().stream().allMatch(p -> p.getFullName().toLowerCase().contains("cha"));

            if (filteredCount > 0 && match) {
                // Xóa ô tìm kiếm để phục hồi danh sách
                runOnFxThread(() -> searchField.setText(""));
                int restoredCount = table.getItems().size();

                if (restoredCount >= 7) {
                    pass("TEST 2: Tìm kiếm và phục hồi danh sách",
                            "Lọc đúng " + filteredCount + " bản ghi chứa 'Cha', phục hồi đủ " + restoredCount + " bản ghi");
                } else {
                    fail("TEST 2: Tìm kiếm và phục hồi danh sách", "Không phục hồi đủ danh sách sau khi xóa tìm kiếm: " + restoredCount);
                }
            } else {
                fail("TEST 2: Tìm kiếm và phục hồi danh sách", "Lọc không chính xác: " + filteredCount + " bản ghi");
            }
        } catch (Exception e) {
            fail("TEST 2: Tìm kiếm và phục hồi danh sách", "Ngoại lệ: " + e.getMessage());
        }
    }

    /**
     * TEST 3: Mở PersonFormView, nhập dữ liệu hợp lệ, chọn cha/mẹ từ ComboBox và lưu vào MySQL.
     */
    private static int testPersonFormAdd(PersonController controller) {
        int generatedId = -1;
        try {
            AtomicReference<PersonFormView> formRef = new AtomicReference<>();
            runOnFxThread(() -> {
                PersonFormView form = new PersonFormView(controller, null);
                formRef.set(form);
            });

            PersonFormView form = formRef.get();
            TextField txtName = (TextField) getFormField(form, "txtFullName");
            TextField txtYear = (TextField) getFormField(form, "txtBirthYear");
            @SuppressWarnings("unchecked")
            ComboBox<Gender> cboGender = (ComboBox<Gender>) getFormField(form, "cboGender");
            @SuppressWarnings("unchecked")
            ComboBox<Person> cboFather = (ComboBox<Person>) getFormField(form, "cboFather");
            @SuppressWarnings("unchecked")
            ComboBox<Person> cboMother = (ComboBox<Person>) getFormField(form, "cboMother");

            // Chọn cha: Nguyễn Văn Cha (ID 3), mẹ: Lê Thị Mẹ (ID 4)
            runOnFxThread(() -> {
                txtName.setText(TEST_PREFIX);
                txtYear.setText("1995");
                cboGender.setValue(Gender.MALE);

                // Tìm và chọn cha có ID = 3
                for (Person p : cboFather.getItems()) {
                    if (p != null && p.getId() == 3) {
                        cboFather.setValue(p);
                        break;
                    }
                }
                // Tìm và chọn mẹ có ID = 4
                for (Person p : cboMother.getItems()) {
                    if (p != null && p.getId() == 4) {
                        cboMother.setValue(p);
                        break;
                    }
                }

                // Gọi handleSave() trên form
                invokeHandleSave(form);
            });

            // Xác minh trong MySQL
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT id, full_name, birth_year, gender, father_id, mother_id FROM PERSON WHERE full_name = ?")) {
                ps.setString(1, TEST_PREFIX);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        generatedId = rs.getInt("id");
                        int bYear = rs.getInt("birth_year");
                        int fId = rs.getInt("father_id");
                        int mId = rs.getInt("mother_id");

                        if (bYear == 1995 && fId == 3 && mId == 4) {
                            pass("TEST 3: Thêm Person hợp lệ qua PersonFormView",
                                    "Thêm thành công ID = " + generatedId + " (Năm sinh 1995, Father ID 3, Mother ID 4) vào MySQL");
                        } else {
                            fail("TEST 3: Thêm Person hợp lệ qua PersonFormView",
                                    "Dữ liệu lưu không khớp: bYear=" + bYear + ", fId=" + fId + ", mId=" + mId);
                        }
                    } else {
                        fail("TEST 3: Thêm Person hợp lệ qua PersonFormView", "Không tìm thấy bản ghi trong MySQL sau khi lưu");
                    }
                }
            }
        } catch (Exception e) {
            fail("TEST 3: Thêm Person hợp lệ qua PersonFormView", "Ngoại lệ: " + e.getMessage());
        }
        return generatedId;
    }

    /**
     * TEST 4: Validation từ chối dữ liệu không hợp lệ (họ tên rỗng, năm sinh sai).
     * Xác minh không có bản ghi lỗi nào lọt vào MySQL.
     */
    private static void testPersonFormValidation(PersonController controller) {
        try {
            AtomicReference<PersonFormView> formRef = new AtomicReference<>();
            runOnFxThread(() -> {
                PersonFormView form = new PersonFormView(controller, null);
                formRef.set(form);
            });

            PersonFormView form = formRef.get();
            TextField txtName = (TextField) getFormField(form, "txtFullName");
            TextField txtYear = (TextField) getFormField(form, "txtBirthYear");
            @SuppressWarnings("unchecked")
            ComboBox<Gender> cboGender = (ComboBox<Gender>) getFormField(form, "cboGender");

            // Kịch bản 1: Để tên rỗng
            runOnFxThread(() -> {
                txtName.setText("   ");
                txtYear.setText("2000");
                cboGender.setValue(Gender.MALE);
                invokeHandleSave(form);
            });

            // Kịch bản 2: Năm sinh không phải số
            runOnFxThread(() -> {
                txtName.setText(TEST_PREFIX + "_INVALID");
                txtYear.setText("Năm2000");
                cboGender.setValue(Gender.MALE);
                invokeHandleSave(form);
            });

            // Xác minh MySQL không có bản ghi nào chứa _INVALID hoặc tên rỗng
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT COUNT(*) AS total FROM PERSON WHERE full_name LIKE ? OR TRIM(full_name) = ''")) {
                ps.setString(1, TEST_PREFIX + "_INVALID%");
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    int count = rs.getInt("total");
                    if (count == 0) {
                        pass("TEST 4: Validation từ chối dữ liệu không hợp lệ",
                                "Đã chặn lưu khi họ tên rỗng hoặc năm sinh sai; MySQL không có bản ghi lỗi");
                    } else {
                        fail("TEST 4: Validation từ chối dữ liệu không hợp lệ", "Có " + count + " bản ghi không hợp lệ trong MySQL");
                    }
                }
            }
        } catch (Exception e) {
            fail("TEST 4: Validation từ chối dữ liệu không hợp lệ", "Ngoại lệ: " + e.getMessage());
        }
    }

    /**
     * TEST 5: Sửa Person và gỡ quan hệ cha/mẹ qua PersonFormView.
     */
    private static void testPersonFormEditAndClearParent(PersonController controller, int testPersonId) {
        try {
            // Lấy person từ service để truyền vào form sửa
            Person personToEdit = controller.handleViewDetail(testPersonId);
            if (personToEdit == null) {
                fail("TEST 5: Sửa Person và gỡ quan hệ cha/mẹ", "Không tìm thấy person ID = " + testPersonId);
                return;
            }

            AtomicReference<PersonFormView> formRef = new AtomicReference<>();
            runOnFxThread(() -> {
                PersonFormView form = new PersonFormView(controller, personToEdit);
                formRef.set(form);
            });

            PersonFormView form = formRef.get();
            TextField txtName = (TextField) getFormField(form, "txtFullName");
            @SuppressWarnings("unchecked")
            ComboBox<Person> cboFather = (ComboBox<Person>) getFormField(form, "cboFather");
            @SuppressWarnings("unchecked")
            ComboBox<Person> cboMother = (ComboBox<Person>) getFormField(form, "cboMother");

            String newName = TEST_PREFIX + "_EDIT";

            // Sửa tên và chọn "— Không có —" (null) cho cả cha và mẹ
            runOnFxThread(() -> {
                txtName.setText(newName);
                cboFather.setValue(null);
                cboMother.setValue(null);
                invokeHandleSave(form);
            });

            // Kiểm tra MySQL
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT full_name, father_id, mother_id FROM PERSON WHERE id = ?")) {
                ps.setInt(1, testPersonId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String updatedName = rs.getString("full_name");
                        rs.getInt("father_id");
                        boolean fNull = rs.wasNull();
                        rs.getInt("mother_id");
                        boolean mNull = rs.wasNull();

                        if (newName.equals(updatedName) && fNull && mNull) {
                            pass("TEST 5: Sửa Person và gỡ quan hệ cha/mẹ qua PersonFormView",
                                    "Đổi tên thành '" + updatedName + "', gỡ thành công cha/mẹ về NULL trong MySQL");
                        } else {
                            fail("TEST 5: Sửa Person và gỡ quan hệ cha/mẹ qua PersonFormView",
                                    "Dữ liệu chưa khớp: name=" + updatedName + ", fatherNull=" + fNull + ", motherNull=" + mNull);
                        }
                    } else {
                        fail("TEST 5: Sửa Person và gỡ quan hệ cha/mẹ qua PersonFormView", "Không tìm thấy bản ghi ID " + testPersonId);
                    }
                }
            }
        } catch (Exception e) {
            fail("TEST 5: Sửa Person và gỡ quan hệ cha/mẹ qua PersonFormView", "Ngoại lệ: " + e.getMessage());
        }
    }

    /**
     * TEST 6: Xóa an toàn.
     * 1. Xóa người đang làm cha/mẹ (ID 3 - cha của ID 5) -> Phải bị từ chối theo BR4.
     * 2. Xóa bản ghi thử nghiệm (đã gỡ quan hệ) -> Phải thành công.
     */
    private static void testSafeDelete(PersonController controller, int testPersonId) {
        try {
            // Case 6a: Thử xóa ID 3 (đang được tham chiếu làm cha của ID 5)
            controller.handleDelete(3);

            // Kiểm tra ID 3 vẫn còn trong MySQL
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS total FROM PERSON WHERE id = 3")) {
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt("total") == 1) {
                        pass("TEST 6a: Chặn xóa Person đang được tham chiếu (BR4)",
                                "Bảo toàn thành công ID = 3 (đang làm cha); không thể xóa");
                    } else {
                        fail("TEST 6a: Chặn xóa Person đang được tham chiếu (BR4)", "ID 3 đã bị xóa trái phép!");
                    }
                }
            }

            // Case 6b: Xóa test person đã gỡ quan hệ
            if (testPersonId > 0) {
                controller.handleDelete(testPersonId);

                // Kiểm tra testPersonId đã biến mất khỏi MySQL
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS total FROM PERSON WHERE id = ?")) {
                    ps.setInt(1, testPersonId);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        if (rs.getInt("total") == 0) {
                            pass("TEST 6b: Xóa thành viên thử nghiệm đã gỡ quan hệ",
                                    "Xóa thành công test person ID = " + testPersonId + " khỏi MySQL");
                        } else {
                            fail("TEST 6b: Xóa thành viên thử nghiệm đã gỡ quan hệ", "Bản ghi vẫn tồn tại trong MySQL sau khi xóa");
                        }
                    }
                }
            }
        } catch (Exception e) {
            fail("TEST 6: Xóa an toàn", "Ngoại lệ: " + e.getMessage());
        }
    }

    /**
     * TEST 7: Kiểm tra PersonDetailView hiển thị đúng cha/mẹ và FamilyTreeView duyệt đúng cấu trúc cây.
     */
    private static void testDetailAndFamilyTreeView(PersonController controller) {
        try {
            // Lấy ID 7: Nguyễn Văn Cháu (cha là ID 5: Nguyễn Văn Con, mẹ là ID 6: Phạm Thị Dâu)
            Person p7 = controller.handleViewDetail(7);
            if (p7 == null) {
                fail("TEST 7: PersonDetailView & FamilyTreeView", "Không tìm thấy thành viên ID = 7");
                return;
            }

            // 7a. Kiểm tra PersonDetailView
            AtomicReference<PersonDetailView> detailRef = new AtomicReference<>();
            runOnFxThread(() -> {
                PersonDetailView detailView = new PersonDetailView(controller, p7);
                detailRef.set(detailView);
            });

            PersonDetailView detailView = detailRef.get();
            List<Label> labels = new ArrayList<>();
            collectLabels(detailView.getRoot(), labels);

            boolean hasFatherName = labels.stream().anyMatch(l -> l.getText() != null && l.getText().contains("Nguyễn Văn Con"));
            boolean hasMotherName = labels.stream().anyMatch(l -> l.getText() != null && l.getText().contains("Phạm Thị Dâu"));

            if (hasFatherName && hasMotherName) {
                pass("TEST 7a: PersonDetailView hiển thị đúng cha mẹ",
                        "Hiển thị đúng cha: 'Nguyễn Văn Con' và mẹ: 'Phạm Thị Dâu'");
            } else {
                fail("TEST 7a: PersonDetailView hiển thị đúng cha mẹ",
                        "Không tìm thấy nhãn cha/mẹ mong muốn: fatherFound=" + hasFatherName + ", motherFound=" + hasMotherName);
            }

            // 7b. Kiểm tra FamilyTreeView
            AtomicReference<FamilyTreeView> treeRef = new AtomicReference<>();
            runOnFxThread(() -> {
                FamilyTreeView treeView = new FamilyTreeView(controller, p7);
                treeRef.set(treeView);
            });

            FamilyTreeView treeView = treeRef.get();
            TreeView<?> tv = (TreeView<?>) getFormField(treeView, "treeView");

            if (tv != null && tv.getRoot() != null) {
                TreeItem<?> rootItem = tv.getRoot();
                int childCount = rootItem.getChildren().size();
                // rootItem phải chứa Ancestor section, Focus item, và có thể Descendant section
                if (childCount >= 2) {
                    pass("TEST 7b: FamilyTreeView duyệt cây gia phả đa thế hệ",
                            "Cây phả hệ của ID = 7 hiển thị thành công với " + childCount + " phân nhánh chính (Tổ tiên & Người trung tâm)");
                } else {
                    fail("TEST 7b: FamilyTreeView duyệt cây gia phả đa thế hệ", "Số lượng nhánh gốc không đủ: " + childCount);
                }
            } else {
                fail("TEST 7b: FamilyTreeView duyệt cây gia phả đa thế hệ", "treeView hoặc rootItem bị null");
            }
        } catch (Exception e) {
            fail("TEST 7: PersonDetailView & FamilyTreeView", "Ngoại lệ: " + e.getMessage());
        }
    }

    // =========================================================================
    // HELPER METHODS: JAVAFX INITIALIZATION, THREADING & REFLECTION
    // =========================================================================

    private static void initJavaFxToolkit() {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(() -> {
                // Tắt tự động thoát khi đóng Stage
                Platform.setImplicitExit(false);
                dummyStage = new Stage();
                dummyStage.setTitle("JavaFxViewTest Dummy Stage");
                setupDialogAutoDismiss();
                latch.countDown();
            });
        } catch (IllegalStateException e) {
            // Toolkit đã được khởi tạo trước đó
            setupDialogAutoDismiss();
            latch.countDown();
        }
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                System.err.println("[WARN] Timeout khởi tạo JavaFX Toolkit");
            }
        } catch (InterruptedException ignored) {}
    }

    private static void shutdownJavaFxToolkit() {
        try {
            runOnFxThread(() -> {
                if (dummyStage != null) {
                    dummyStage.close();
                }
                Platform.exit();
            });
        } catch (Exception ignored) {}
    }

    /**
     * Tự động đóng các cửa sổ hộp thoại Alert / Dialog khi hiển thị,
     * ngăn chặn phương thức showAndWait() làm treo luồng test tự động.
     */
    private static void setupDialogAutoDismiss() {
        Window.getWindows().addListener((ListChangeListener<Window>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Window w : change.getAddedSubList()) {
                        if (w instanceof Stage && w != dummyStage) {
                            Stage s = (Stage) w;
                            Platform.runLater(() -> {
                                try {
                                    s.close();
                                } catch (Exception ignored) {}
                            });
                        }
                    }
                }
            }
        });
    }

    @FunctionalInterface
    private interface FxAction {
        void execute() throws Exception;
    }

    private static void runOnFxThread(FxAction action) throws Exception {
        if (Platform.isFxApplicationThread()) {
            action.execute();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.execute();
            } catch (Throwable t) {
                errorRef.set(t);
            } finally {
                latch.countDown();
            }
        });
        boolean finished = latch.await(10, TimeUnit.SECONDS);
        if (!finished) {
            throw new RuntimeException("JavaFX Application Thread action timed out sau 10 giây");
        }
        if (errorRef.get() != null) {
            if (errorRef.get() instanceof Exception) {
                throw (Exception) errorRef.get();
            }
            throw new RuntimeException(errorRef.get());
        }
    }

    @SuppressWarnings("unchecked")
    private static TableView<Person> getTableView(PeopleListView view) throws Exception {
        Field f = PeopleListView.class.getDeclaredField("tableView");
        f.setAccessible(true);
        return (TableView<Person>) f.get(view);
    }

    private static TextField getSearchField(PeopleListView view) throws Exception {
        Field f = PeopleListView.class.getDeclaredField("searchField");
        f.setAccessible(true);
        return (TextField) f.get(view);
    }

    private static Object getFormField(Object target, String fieldName) throws Exception {
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
        if (node instanceof Label) {
            labelList.add((Label) node);
        } else if (node instanceof ScrollPane) {
            Node content = ((ScrollPane) node).getContent();
            if (content != null) {
                collectLabels(content, labelList);
            }
        } else if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                collectLabels(child, labelList);
            }
        }
    }

    private static int getDbRecordCount() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) AS total FROM PERSON")) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Không thể đếm bản ghi trong DB: " + e.getMessage());
        }
        return -1;
    }

    private static void cleanupTestData() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM PERSON WHERE full_name LIKE ?")) {
            ps.setString(1, TEST_PREFIX + "%");
            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                System.out.println("[CLEANUP] Đã dọn dẹp " + deleted + " bản ghi thử nghiệm khỏi MySQL.");
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Lỗi khi dọn dẹp test data: " + e.getMessage());
        }
    }

    private static void pass(String testName, String detail) {
        passedCount++;
        System.out.println("[PASS] " + testName + " -> " + detail);
    }

    private static void fail(String testName, String detail) {
        failedCount++;
        System.err.println("[FAIL] " + testName + " -> " + detail);
    }
}
