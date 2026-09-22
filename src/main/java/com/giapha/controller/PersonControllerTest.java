package com.giapha.controller;

import com.giapha.database.DatabaseConnection;
import com.giapha.model.Gender;
import com.giapha.model.Person;
import com.giapha.repository.JdbcPersonRepository;
import com.giapha.service.FamilyTreeService;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

/**
 * Test harness kiểm thử tích hợp cho PersonController.
 * Đảm bảo PersonController gọi đúng FamilyTreeService, thực hiện đúng các thao tác CRUD,
 * xử lý đúng các ngoại lệ và giữ nguyên vẹn cơ sở dữ liệu mẫu.
 */
public class PersonControllerTest {

    private static int passedCount = 0;
    private static int failedCount = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  BẮT ĐẦU CHẠY KIỂM THỬ PERSONCONTROLLER  ");
        System.out.println("==================================================");

        FamilyTreeService service = new FamilyTreeService(new JdbcPersonRepository());
        PersonController controller = new PersonController(service);

        // Reset database trước khi test
        resetDatabase();

        // 1. Test handleSearch qua Controller
        try {
            List<Person> results = controller.handleSearch("Tổ");
            if (results != null && !results.isEmpty() && results.stream().anyMatch(p -> p.getFullName().contains("Tổ"))) {
                pass("CTRL-TEST 1: handleSearch(\"Tổ\")", "Tìm thấy kết quả: " + results.size() + " bản ghi");
            } else {
                fail("CTRL-TEST 1: handleSearch(\"Tổ\")", "Không tìm thấy kết quả phù hợp");
            }
        } catch (Exception e) {
            fail("CTRL-TEST 1: handleSearch(\"Tổ\")", "Ngoại lệ: " + e.getMessage());
        }

        // 2. Test handleViewDetail qua Controller
        try {
            Person p = controller.handleViewDetail(1);
            if (p != null && "Nguyễn Văn Tổ".equals(p.getFullName())) {
                pass("CTRL-TEST 2: handleViewDetail(1)", "Lấy đúng thông tin: " + p.getFullName());
            } else {
                fail("CTRL-TEST 2: handleViewDetail(1)", "Sai thông tin hoặc null: " + p);
            }
        } catch (Exception e) {
            fail("CTRL-TEST 2: handleViewDetail(1)", "Ngoại lệ: " + e.getMessage());
        }

        // 3. Test handleViewDetail với ID không tồn tại
        try {
            Person p = controller.handleViewDetail(99999);
            if (p == null) {
                pass("CTRL-TEST 3: handleViewDetail(99999)", "Xử lý êm thuận PersonNotFoundException, trả về null");
            } else {
                fail("CTRL-TEST 3: handleViewDetail(99999)", "Kỳ vọng null nhưng lại có dữ liệu: " + p);
            }
        } catch (Exception e) {
            fail("CTRL-TEST 3: handleViewDetail(99999)", "Ngoại lệ không mong muốn: " + e.getMessage());
        }

        // 4. Test handleViewTree qua Controller
        try {
            Person focus = controller.handleViewTree(7);
            if (focus != null && focus.getId() == 7) {
                pass("CTRL-TEST 4: handleViewTree(7)", "Xác thực và xem cây gia phả thành công cho ID = 7");
            } else {
                fail("CTRL-TEST 4: handleViewTree(7)", "Không tìm thấy focus person 7");
            }
        } catch (Exception e) {
            fail("CTRL-TEST 4: handleViewTree(7)", "Ngoại lệ: " + e.getMessage());
        }

        // 5. Test handleAdd qua Controller
        Person added = null;
        try {
            Person newP = new Person("Phan Văn Test", 1990, Gender.MALE, null);
            added = controller.handleAdd(newP);
            if (added != null && added.getId() > 0) {
                pass("CTRL-TEST 5: handleAdd", "Thêm thành công thành viên ID = " + added.getId());
            } else {
                fail("CTRL-TEST 5: handleAdd", "Không thêm được thành viên");
            }
        } catch (Exception e) {
            fail("CTRL-TEST 5: handleAdd", "Ngoại lệ: " + e.getMessage());
        } finally {
            if (added != null && added.getId() > 0) {
                try {
                    service.deletePerson(added.getId());
                } catch (Exception ignored) {}
            }
        }

        // 6. Test handleDelete với người đang được dùng làm cha (ID 3)
        try {
            controller.handleDelete(3);
            // Xác nhận ID 3 vẫn còn nguyên vẹn trong DB
            Person p3 = service.getPersonById(3);
            if (p3 != null) {
                pass("CTRL-TEST 6: handleDelete(3) (Person in use)", "Chặn xóa thành công, bản ghi ID 3 được bảo toàn");
            } else {
                fail("CTRL-TEST 6: handleDelete(3)", "ID 3 đã bị xóa trái phép");
            }
        } catch (Exception e) {
            fail("CTRL-TEST 6: handleDelete(3)", "Ngoại lệ: " + e.getMessage());
        }

        // 7. Test handleEdit với nguyên tắc tách bạch: chỉ cập nhật basic fields, không bypass cycle
        try {
            Person original5 = service.getPersonById(5);
            // Sửa tên và gán cha mới là 1
            Person updated5 = new Person("Nguyễn Văn Con (Đã sửa)", 1985, Gender.MALE, null);
            updated5.setId(5);
            updated5.setFather(original5.getFather()); // Giữ cha gốc
            updated5.setMother(original5.getMother()); // Giữ mẹ gốc

            Person newFather1 = service.getPersonById(1);
            controller.handleEdit(updated5, newFather1, original5.getMother());

            Person verify5 = service.getPersonById(5);
            if ("Nguyễn Văn Con (Đã sửa)".equals(verify5.getFullName()) && verify5.getFather() != null && verify5.getFather().getId() == 1) {
                pass("CTRL-TEST 7: handleEdit", "Cập nhật basic fields và assignFather thành công qua quy trình 6 bước");
            } else {
                fail("CTRL-TEST 7: handleEdit", "Dữ liệu sau edit không đúng: " + verify5);
            }
        } catch (Exception e) {
            fail("CTRL-TEST 7: handleEdit", "Ngoại lệ: " + e.getMessage());
        } finally {
            // Khôi phục lại Person 5 về nguyên bản
            resetDatabase();
        }

        // 8. TC-CLEAR-06: Test handleEdit khi newFather = null (mô phỏng chọn "Không có" trong ComboBox)
        try {
            Person original5 = service.getPersonById(5);
            // Chuẩn bị cập nhật với newFather = null và newMother giữ nguyên
            Person updated5 = new Person(original5.getFullName(), original5.getBirthYear(), original5.getGender(), original5.getPhotoPath());
            updated5.setId(5);
            updated5.setFather(original5.getFather());
            updated5.setMother(original5.getMother());

            controller.handleEdit(updated5, null, original5.getMother());

            Person verify5 = service.getPersonById(5);
            if (verify5.getFather() == null) {
                pass("TC-CLEAR-06: handleEdit với newFather = null", "Gỡ quan hệ cha thành công qua handleEdit khi chọn 'Không có' (father == null)");
            } else {
                fail("TC-CLEAR-06: handleEdit với newFather = null", "Father vẫn chưa được gỡ trong DB: " + verify5.getFather());
            }
        } catch (Exception e) {
            fail("TC-CLEAR-06: handleEdit với newFather = null", "Ngoại lệ: " + e.getMessage());
        } finally {
            resetDatabase();
        }

        // Xác nhận trạng thái cuối cùng của Database
        verifyFinalDatabaseState();

        System.out.println("==================================================");
        System.out.println("  TỔNG KẾT CONTROLLER: " + passedCount + " PASSED, " + failedCount + " FAILED  ");
        System.out.println("==================================================");

        if (failedCount > 0) {
            System.exit(1);
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

    private static void resetDatabase() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE PERSON SET full_name = 'Nguyễn Văn Con', father_id = 3, mother_id = 4 WHERE id = 5");
        } catch (Exception e) {
            System.err.println("[CLEANUP WARNING] Lỗi reset DB: " + e.getMessage());
        }
    }

    private static void verifyFinalDatabaseState() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM PERSON")) {
            if (rs.next()) {
                int count = rs.getInt(1);
                if (count == 7) {
                    System.out.println("[VERIFY OK] Database gia_pha có đúng 7 bản ghi mẫu nguyên bản.");
                } else {
                    System.err.println("[VERIFY FAILED] Số bản ghi trong PERSON là " + count + " (kỳ vọng 7).");
                }
            }
        } catch (Exception e) {
            System.err.println("[VERIFY FAILED] Lỗi kiểm tra database: " + e.getMessage());
        }
    }
}
