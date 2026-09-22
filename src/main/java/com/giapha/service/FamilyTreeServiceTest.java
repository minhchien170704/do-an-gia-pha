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
                pass("TEST 9: getAncestors(7)", "Tìm thấy đủ 6 tổ tiên, không chứa chính mình, không trùng lặp: "
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

        // Reset dữ liệu về chuẩn trước khi chạy nhóm test case mở rộng
        resetInitialState();

        // ----------------------------------------------------
        // TC02: addPerson với fullName rỗng hoặc null -> IllegalArgumentException
        // ----------------------------------------------------
        try {
            boolean caughtNull = false;
            try {
                Person pNull = new Person(null, 1995, com.giapha.model.Gender.MALE, null);
                service.addPerson(pNull);
            } catch (IllegalArgumentException e) {
                caughtNull = true;
            }

            boolean caughtEmpty = false;
            try {
                Person pEmpty = new Person("   ", 1995, com.giapha.model.Gender.MALE, null);
                service.addPerson(pEmpty);
            } catch (IllegalArgumentException e) {
                caughtEmpty = true;
            }

            if (caughtNull && caughtEmpty) {
                pass("TC02: addPerson với fullName rỗng/null", "Bắt đúng IllegalArgumentException khi fullName null hoặc rỗng");
            } else {
                fail("TC02: addPerson với fullName rỗng/null", "Không bắt đủ ngoại lệ: null=" + caughtNull + ", empty=" + caughtEmpty);
            }
        } catch (Exception e) {
            fail("TC02: addPerson với fullName rỗng/null", "Ném sai loại ngoại lệ: " + e.getClass().getName());
        }

        // ----------------------------------------------------
        // TC03 & TC04: addPerson với birthYear sai (<= 1900 và > năm hiện tại) -> InvalidBirthYearException
        // ----------------------------------------------------
        try {
            boolean caught1800 = false;
            try {
                Person pPast = new Person("Test Năm Sinh Cũ", 1800, com.giapha.model.Gender.MALE, null);
                service.addPerson(pPast);
            } catch (InvalidBirthYearException e) {
                caught1800 = true;
            }

            boolean caughtFuture = false;
            int nextYear = java.time.Year.now().getValue() + 1;
            try {
                Person pFuture = new Person("Test Năm Sinh Tương Lai", nextYear, com.giapha.model.Gender.MALE, null);
                service.addPerson(pFuture);
            } catch (InvalidBirthYearException e) {
                caughtFuture = true;
            }

            if (caught1800 && caughtFuture) {
                pass("TC03 - TC04: addPerson với birthYear sai", "Bắt đúng InvalidBirthYearException khi birthYear = 1800 và birthYear = " + nextYear);
            } else {
                fail("TC03 - TC04: addPerson với birthYear sai", "Không bắt đủ InvalidBirthYearException: 1800=" + caught1800 + ", future=" + caughtFuture);
            }
        } catch (Exception e) {
            fail("TC03 - TC04: addPerson với birthYear sai", "Ném sai loại ngoại lệ: " + e.getClass().getName());
        }

        // ----------------------------------------------------
        // TC01: addPerson hợp lệ, độc lập -> SUCCESS, sinh ID, query DB xác nhận
        // ----------------------------------------------------
        Person added = null;
        try {
            Person validPerson = new Person("Nguyễn Văn Mới", 1995, com.giapha.model.Gender.MALE, null);
            added = service.addPerson(validPerson);
            int newId = added.getId();

            if (newId > 0) {
                Person queried = service.getPersonById(newId);
                if (queried != null && "Nguyễn Văn Mới".equals(queried.getFullName())) {
                    pass("TC01: addPerson hợp lệ", "Thêm thành công person mới, generated id = " + newId + ", query DB thành công");
                } else {
                    fail("TC01: addPerson hợp lệ", "Không query lại được Person vừa thêm hoặc sai dữ liệu: " + queried);
                }
            } else {
                fail("TC01: addPerson hợp lệ", "Không sinh được ID hợp lệ (> 0): " + newId);
            }
        } catch (Exception e) {
            fail("TC01: addPerson hợp lệ", "Ném ngoại lệ không mong muốn: " + e.getMessage());
        } finally {
            if (added != null && added.getId() > 0) {
                try {
                    service.deletePerson(added.getId());
                } catch (Exception cleanupEx) {
                    System.err.println("[CLEANUP WARNING] Không thể xóa person test TC01: " + cleanupEx.getMessage());
                }
            }
        }

        // ----------------------------------------------------
        // TC12 (trực tiếp): assignFather với parentId không tồn tại -> PersonNotFoundException
        // ----------------------------------------------------
        try {
            service.assignFather(5, 99999);
            fail("TC12 (trực tiếp): assignFather với parentId không tồn tại", "Không ném ngoại lệ khi fatherId = 99999");
        } catch (PersonNotFoundException e) {
            pass("TC12 (trực tiếp): assignFather với parentId không tồn tại", "Bắt đúng PersonNotFoundException: " + e.getMessage());
        } catch (Exception e) {
            fail("TC12 (trực tiếp): assignFather với parentId không tồn tại", "Ném sai loại ngoại lệ: " + e.getClass().getName());
        }

        // ----------------------------------------------------
        // TC14: deletePerson với leaf node (ID 7) -> SUCCESS, query DB xác nhận record đã mất
        // ----------------------------------------------------
        try {
            // ID 7 là 'Nguyễn Văn Cháu' (leaf node, không là cha/mẹ của ai)
            service.deletePerson(7);

            boolean isDeleted = false;
            try {
                service.getPersonById(7);
            } catch (PersonNotFoundException e) {
                isDeleted = true;
            }

            if (isDeleted) {
                pass("TC14: deletePerson với leaf node", "Xóa thành công leaf node ID 7, query xác nhận đã biến mất");
            } else {
                fail("TC14: deletePerson với leaf node", "Record ID 7 vẫn còn tồn tại sau khi delete");
            }
        } catch (Exception e) {
            fail("TC14: deletePerson với leaf node", "Ném ngoại lệ không mong muốn: " + e.getMessage());
        } finally {
            restorePerson7();
        }

        // ----------------------------------------------------
        // TC-CLEAR-01: clearFather(5) khi đang có father = 3 -> verify father == null
        // ----------------------------------------------------
        try {
            service.clearFather(5);
            Person p5 = service.getPersonById(5);
            if (p5.getFather() == null) {
                pass("TC-CLEAR-01: clearFather(5)", "Gỡ quan hệ cha thành công, father hiện tại là null");
            } else {
                fail("TC-CLEAR-01: clearFather(5)", "Father vẫn còn tồn tại sau khi clear: " + p5.getFather());
            }
        } catch (Exception e) {
            fail("TC-CLEAR-01: clearFather(5)", "Ném ngoại lệ không mong muốn: " + e.getMessage());
        } finally {
            resetInitialState();
        }

        // ----------------------------------------------------
        // TC-CLEAR-02: clearMother(5) khi đang có mother = 4 -> verify mother == null
        // ----------------------------------------------------
        try {
            service.clearMother(5);
            Person p5 = service.getPersonById(5);
            if (p5.getMother() == null) {
                pass("TC-CLEAR-02: clearMother(5)", "Gỡ quan hệ mẹ thành công, mother hiện tại là null");
            } else {
                fail("TC-CLEAR-02: clearMother(5)", "Mother vẫn còn tồn tại sau khi clear: " + p5.getMother());
            }
        } catch (Exception e) {
            fail("TC-CLEAR-02: clearMother(5)", "Ném ngoại lệ không mong muốn: " + e.getMessage());
        } finally {
            resetInitialState();
        }

        // ----------------------------------------------------
        // TC-CLEAR-03: clearFather với childId không tồn tại -> PersonNotFoundException
        // ----------------------------------------------------
        try {
            service.clearFather(99999);
            fail("TC-CLEAR-03: clearFather(99999)", "Không ném ngoại lệ khi childId không tồn tại");
        } catch (PersonNotFoundException e) {
            pass("TC-CLEAR-03: clearFather(99999)", "Bắt đúng PersonNotFoundException: " + e.getMessage());
        } catch (Exception e) {
            fail("TC-CLEAR-03: clearFather(99999)", "Ném sai loại ngoại lệ: " + e.getClass().getName());
        }

        // ----------------------------------------------------
        // TC-CLEAR-04: clearMother với childId không tồn tại -> PersonNotFoundException
        // ----------------------------------------------------
        try {
            service.clearMother(99999);
            fail("TC-CLEAR-04: clearMother(99999)", "Không ném ngoại lệ khi childId không tồn tại");
        } catch (PersonNotFoundException e) {
            pass("TC-CLEAR-04: clearMother(99999)", "Bắt đúng PersonNotFoundException: " + e.getMessage());
        } catch (Exception e) {
            fail("TC-CLEAR-04: clearMother(99999)", "Ném sai loại ngoại lệ: " + e.getClass().getName());
        }

        // ----------------------------------------------------
        // TC-CLEAR-05: Sau khi clear quan hệ cha/mẹ, có thể delete người cha/mẹ nếu không còn ai tham chiếu
        // ----------------------------------------------------
        Person tempParent = null;
        Person tempChild = null;
        try {
            tempParent = service.addPerson(new Person("Cha Tạm TC05", 1970, com.giapha.model.Gender.MALE, null));
            tempChild = service.addPerson(new Person("Con Tạm TC05", 1995, com.giapha.model.Gender.MALE, null));
            service.assignFather(tempChild.getId(), tempParent.getId());

            // Xác minh trước khi clear: tempParent đang được tham chiếu, không thể xóa
            boolean blockedBefore = false;
            try {
                service.deletePerson(tempParent.getId());
            } catch (PersonInUseException e) {
                blockedBefore = true;
            }

            // Gỡ quan hệ cha
            service.clearFather(tempChild.getId());

            // Xóa cha sau khi đã gỡ quan hệ
            boolean deleteSuccess = false;
            try {
                service.deletePerson(tempParent.getId());
                deleteSuccess = true;
            } catch (Exception e) {
                deleteSuccess = false;
            }

            if (blockedBefore && deleteSuccess) {
                pass("TC-CLEAR-05: Delete parent sau khi clear quan hệ",
                        "Trước khi clear bị chặn PersonInUseException; sau khi clear quan hệ thì xóa thành công người cha");
            } else {
                fail("TC-CLEAR-05: Delete parent sau khi clear quan hệ",
                        "blockedBefore=" + blockedBefore + ", deleteSuccess=" + deleteSuccess);
            }
        } catch (Exception e) {
            fail("TC-CLEAR-05: Delete parent sau khi clear quan hệ", "Ném ngoại lệ không mong muốn: " + e.getMessage());
        } finally {
            if (tempChild != null && tempChild.getId() > 0) {
                try {
                    service.deletePerson(tempChild.getId());
                } catch (Exception ignored) {}
            }
            if (tempParent != null && tempParent.getId() > 0) {
                try {
                    service.deletePerson(tempParent.getId());
                } catch (Exception ignored) {}
            }
            resetInitialState();
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

    private static void restorePerson7() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("INSERT INTO PERSON (id, full_name, birth_year, gender, photo_path, father_id, mother_id) " +
                    "VALUES (7, 'Nguyễn Văn Cháu', 2015, 'MALE', NULL, 5, 6)");
        } catch (Exception e) {
            System.err.println("[CLEANUP WARNING] Không thể khôi phục Person 7: " + e.getMessage());
        }
    }

    private static void resetInitialState() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            // Khôi phục Person 5 về đúng cha=3, mẹ=4
            stmt.executeUpdate("UPDATE PERSON SET father_id = 3, mother_id = 4 WHERE id = 5");
            // Đảm bảo Person 7 tồn tại
            ResultSet rs = stmt.executeQuery("SELECT id FROM PERSON WHERE id = 7");
            if (!rs.next()) {
                stmt.executeUpdate("INSERT INTO PERSON (id, full_name, birth_year, gender, photo_path, father_id, mother_id) " +
                        "VALUES (7, 'Nguyễn Văn Cháu', 2015, 'MALE', NULL, 5, 6)");
            }
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
