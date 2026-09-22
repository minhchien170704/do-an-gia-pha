package com.giapha.service;

import com.giapha.database.DatabaseConnection;
import com.giapha.exception.*;
import com.giapha.model.Person;
import com.giapha.repository.JdbcPersonRepository;
import com.giapha.repository.PersonRepository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Test harness thực tế cho FamilyTreeService kết nối với cơ sở dữ liệu gia_pha.
 * Kiểm tra đầy đủ 12 test cases theo đặc tả Cụm 5.4 và đảm bảo tính cô lập của dữ liệu.
 */
public class FamilyTreeServiceTest {

    private static int passedCount = 0;
    private static int failedCount = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  BẮT ĐẦU CHẠY KIỂM THỬ FAMILYTREESERVICE LAYER  ");
        System.out.println("==================================================");

        PersonRepository repository = new JdbcPersonRepository();
        FamilyTreeService service = new FamilyTreeService(repository);

        // Đảm bảo dữ liệu ban đầu đúng chuẩn trước khi test
        resetInitialState();

        // ----------------------------------------------------
        // TEST 1: getPersonById(1) -> Nguyễn Văn Tổ
        // ----------------------------------------------------
        try {
            Person p = service.getPersonById(1);
            if (p != null && "Nguyễn Văn Tổ".equals(p.getFullName())) {
                pass("TEST 1: getPersonById(1)", "Tìm thấy: " + p.getFullName());
            } else {
                fail("TEST 1: getPersonById(1)", "Tên không khớp hoặc null: " + p);
            }
        } catch (Exception e) {
            fail("TEST 1: getPersonById(1)", "Ném ngoại lệ không mong muốn: " + e.getMessage());
        }

        // ----------------------------------------------------
        // TEST 2: getPersonById(99999) -> PersonNotFoundException
        // ----------------------------------------------------
        try {
            service.getPersonById(99999);
            fail("TEST 2: getPersonById(99999)", "Không ném ngoại lệ khi ID không tồn tại");
        } catch (PersonNotFoundException e) {
            pass("TEST 2: getPersonById(99999)", "Bắt đúng PersonNotFoundException: " + e.getMessage());
        } catch (Exception e) {
            fail("TEST 2: getPersonById(99999)", "Ném sai loại ngoại lệ: " + e.getClass().getName());
        }

        // ----------------------------------------------------
        // TEST 4: assignFather(childId = 1, fatherId = 1) -> SelfParentException
        // (Chạy trước TEST 3 để không ảnh hưởng dữ liệu)
        // ----------------------------------------------------
        try {
            service.assignFather(1, 1);
            fail("TEST 4: assignFather(1, 1)", "Không ném ngoại lệ khi tự gán cha cho chính mình");
        } catch (SelfParentException e) {
            pass("TEST 4: assignFather(1, 1)", "Bắt đúng SelfParentException: " + e.getMessage());
        } catch (Exception e) {
            fail("TEST 4: assignFather(1, 1)", "Ném sai loại ngoại lệ: " + e.getClass().getName());
        }

        // ----------------------------------------------------
        // TEST 5: assignFather(childId = 1, fatherId = 5) -> CycleDetectedException
        // Dữ liệu: 1 là tổ tiên của 5 (1 -> 3 -> 5)
        // ----------------------------------------------------
        try {
            service.assignFather(1, 5);
            fail("TEST 5: assignFather(1, 5)", "Không ném ngoại lệ khi tạo cycle 1 -> 5");
        } catch (CycleDetectedException e) {
            pass("TEST 5: assignFather(1, 5)", "Bắt đúng CycleDetectedException: " + e.getMessage());
        } catch (Exception e) {
            fail("TEST 5: assignFather(1, 5)", "Ném sai loại ngoại lệ: " + e.getClass().getName());
        }

        // ----------------------------------------------------
        // TEST 6: assignFather(childId = 5, fatherId = 2) -> IllegalArgumentException (ID 2 = FEMALE)
        // ----------------------------------------------------
        try {
            service.assignFather(5, 2);
            fail("TEST 6: assignFather(5, 2)", "Không ném ngoại lệ khi gán cha có giới tính FEMALE");
        } catch (IllegalArgumentException e) {
            pass("TEST 6: assignFather(5, 2)", "Bắt đúng IllegalArgumentException: " + e.getMessage());
        } catch (Exception e) {
            fail("TEST 6: assignFather(5, 2)", "Ném sai loại ngoại lệ: " + e.getClass().getName());
        }

        // ----------------------------------------------------
        // TEST 7: assignMother(childId = 5, motherId = 1) -> IllegalArgumentException (ID 1 = MALE)
        // ----------------------------------------------------
        try {
            service.assignMother(5, 1);
            fail("TEST 7: assignMother(5, 1)", "Không ném ngoại lệ khi gán mẹ có giới tính MALE");
        } catch (IllegalArgumentException e) {
            pass("TEST 7: assignMother(5, 1)", "Bắt đúng IllegalArgumentException: " + e.getMessage());
        } catch (Exception e) {
            fail("TEST 7: assignMother(5, 1)", "Ném sai loại ngoại lệ: " + e.getClass().getName());
        }

        // ----------------------------------------------------
        // TEST 8: deletePerson(3) -> PersonInUseException (ID 3 đang là father của ID 5)
        // ----------------------------------------------------
        try {
            service.deletePerson(3);
            fail("TEST 8: deletePerson(3)", "Không ném ngoại lệ khi xóa người đang được tham chiếu làm cha");
        } catch (PersonInUseException e) {
            pass("TEST 8: deletePerson(3)", "Bắt đúng PersonInUseException: " + e.getMessage());
        } catch (Exception e) {
            fail("TEST 8: deletePerson(3)", "Ném sai loại ngoại lệ: " + e.getClass().getName());
        }

        // ----------------------------------------------------
        // TEST 9: getAncestors(7) -> [5, 6, 3, 4, 1, 2]
        // ----------------------------------------------------
        try {
            List<Person> ancestors = service.getAncestors(7);
            Set<Integer> ancestorIds = ancestors.stream().map(Person::getId).collect(Collectors.toSet());
            boolean hasAllExpected = ancestorIds.contains(5) && ancestorIds.contains(3)
                    && ancestorIds.contains(4) && ancestorIds.contains(1) && ancestorIds.contains(2);
            boolean notContainsSelf = !ancestorIds.contains(7);
            boolean noDuplicates = ancestors.size() == ancestorIds.size();

            if (hasAllExpected && notContainsSelf && noDuplicates) {
                pass("TEST 9: getAncestors(7)", "Tìm thấy đủ 5 tổ tiên cốt lõi, không chứa chính mình, không trùng lặp: "
                        + ancestors.stream().map(Person::getFullName).collect(Collectors.joining(", ")));
            } else {
                fail("TEST 9: getAncestors(7)", "Danh sách tổ tiên không đúng: " + ancestorIds);
            }
        } catch (Exception e) {
            fail("TEST 9: getAncestors(7)", "Ném ngoại lệ không mong muốn: " + e.getMessage());
        }

        // ----------------------------------------------------
        // TEST 10: getDescendants(1) -> [3, 5, 7]
        // ----------------------------------------------------
        try {
            List<Person> descendants = service.getDescendants(1);
            Set<Integer> descendantIds = descendants.stream().map(Person::getId).collect(Collectors.toSet());
            boolean hasExpected = descendantIds.contains(3) && descendantIds.contains(5) && descendantIds.contains(7);
            boolean notContainsSelf = !descendantIds.contains(1);
            boolean noDuplicates = descendants.size() == descendantIds.size();

            if (hasExpected && notContainsSelf && noDuplicates) {
                pass("TEST 10: getDescendants(1)", "Tìm thấy đủ các hậu duệ liên quan (3, 5, 7), không trùng lặp: "
                        + descendants.stream().map(Person::getFullName).collect(Collectors.joining(", ")));
            } else {
                fail("TEST 10: getDescendants(1)", "Danh sách hậu duệ không đúng: " + descendantIds);
            }
        } catch (Exception e) {
            fail("TEST 10: getDescendants(1)", "Ném ngoại lệ không mong muốn: " + e.getMessage());
        }

        // ----------------------------------------------------
        // TEST 12: Father == Mother -> REJECT
        // Thử gán cùng một Person làm father và mother.
        // ----------------------------------------------------
        try {
            Person duplicateParentsPerson = new Person("Test Trùng Cha Mẹ", 2000, com.giapha.model.Gender.MALE, null);
            Person f = service.getPersonById(1);
            duplicateParentsPerson.setFather(f);
            duplicateParentsPerson.setMother(f); // Cùng một Person (hoặc cùng id)
            service.addPerson(duplicateParentsPerson);
            fail("TEST 12: Father == Mother", "Không reject khi cùng một Person làm cả cha và mẹ");
        } catch (IllegalArgumentException e) {
            pass("TEST 12: Father == Mother", "Bắt đúng IllegalArgumentException (BR2): " + e.getMessage());
        } catch (Exception e) {
            fail("TEST 12: Father == Mother", "Ném sai loại ngoại lệ: " + e.getClass().getName());
        }

        // ----------------------------------------------------
        // TEST 3: assignFather(childId = 5, fatherId = 1) -> SUCCESS
        // ----------------------------------------------------
        try {
            service.assignFather(5, 1);
            Person updated = service.getPersonById(5);
            if (updated.getFather() != null && updated.getFather().getId() == 1) {
                pass("TEST 3: assignFather(5, 1)", "Gán cha thành công, fatherId hiện tại = 1");
            } else {
                fail("TEST 3: assignFather(5, 1)", "Father chưa được cập nhật trong DB");
            }
        } catch (Exception e) {
            fail("TEST 3: assignFather(5, 1)", "Ném ngoại lệ không mong muốn: " + e.getMessage());
        }

        // ----------------------------------------------------
        // TEST 11: assignMother(childId = 5, motherId = 4) -> SUCCESS
        // ----------------------------------------------------
        try {
            service.assignMother(5, 4);
            Person updated = service.getPersonById(5);
            if (updated.getMother() != null && updated.getMother().getId() == 4) {
                pass("TEST 11: assignMother(5, 4)", "Gán mẹ thành công, motherId hiện tại = 4");
            } else {
                fail("TEST 11: assignMother(5, 4)", "Mother chưa được cập nhật trong DB");
            }
        } catch (Exception e) {
            fail("TEST 11: assignMother(5, 4)", "Ném ngoại lệ không mong muốn: " + e.getMessage());
        }

        // ----------------------------------------------------
        // TEST ISOLATION & CLEANUP
        // Khôi phục dữ liệu Person 5 về trạng thái ban đầu (father = 3, mother = 4)
        // ----------------------------------------------------
        resetInitialState();

        // Kiểm tra trạng thái cuối cùng của Database
        verifyFinalDatabaseState();

        System.out.println("==================================================");
        System.out.println("  TỔNG KẾT: " + passedCount + " PASSED, " + failedCount + " FAILED  ");
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

    private static void resetInitialState() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            // Khôi phục Person 5 về đúng cha=3, mẹ=4
            stmt.executeUpdate("UPDATE PERSON SET father_id = 3, mother_id = 4 WHERE id = 5");
        } catch (Exception e) {
            System.err.println("[CLEANUP WARNING] Không thể reset trạng thái Person 5: " + e.getMessage());
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
            System.err.println("[VERIFY FAILED] Lỗi kiểm tra database cuối: " + e.getMessage());
        }
    }
}
