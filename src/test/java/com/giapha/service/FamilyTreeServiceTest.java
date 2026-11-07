package com.giapha.service;

import com.giapha.exception.CycleDetectedException;
import com.giapha.exception.InvalidBirthYearException;
import com.giapha.exception.InvalidParentGenderException;
import com.giapha.exception.PersonInUseException;
import com.giapha.exception.PersonNotFoundException;
import com.giapha.exception.SameParentException;
import com.giapha.exception.SelfParentException;
import com.giapha.model.Gender;
import com.giapha.model.Person;
import com.giapha.repository.PersonRepository;
import com.giapha.support.TestDatabase;
import com.giapha.support.TestDatabaseManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;

import java.time.Year;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Kiểm thử tầng nghiệp vụ {@link FamilyTreeService} với MySQL thật.
 *
 * <p>Bao phủ đầy đủ 6 quy tắc nghiệp vụ (BR1 – BR6) và các nhóm kiểm thử mở rộng
 * (TC01 – TC14, TC-CLEAR-01 … TC-CLEAR-05).
 *
 * <p><b>Cô lập dữ liệu:</b> lớp test này dùng database RIÊNG {@code gia_pha_service_test}
 * (tên kết thúc bằng {@code _test}), được tạo mới ở {@code @BeforeAll} và xóa ở
 * {@code @AfterAll}. Database chính {@code gia_pha} — dữ liệu thật của người dùng —
 * <b>không bao giờ</b> bị ghi vào.
 *
 * @see TestDatabaseManager
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("FamilyTreeService — quy tắc nghiệp vụ BR1–BR6")
class FamilyTreeServiceTest {

    /** Tên database kiểm thử riêng của lớp test này. */
    private static final String TEST_DB_NAME = "gia_pha_service_test";

    private static PersonRepository repository;
    private static FamilyTreeService service;

    @BeforeAll
    static void initService() throws Exception {
        // Tạo database test riêng (DROP + CREATE + schema + 7 bản ghi mẫu)
        TestDatabaseManager.createFreshDatabase(TEST_DB_NAME);

        assumeTrue(TestDatabase.isAvailable(TEST_DB_NAME),
                "BỎ QUA: không kết nối được MySQL. Hãy khởi động MySQL trước khi chạy test.");

        repository = TestDatabaseManager.repositoryFor(TEST_DB_NAME);
        service = new FamilyTreeService(repository);
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
    // NHÓM 1 — Truy vấn cơ bản
    // ============================================================

    @Test
    @Order(1)
    @DisplayName("getPersonById(1) trả về 'Nguyễn Văn Tổ'")
    void getPersonByIdReturnsRootAncestor() {
        Person p = service.getPersonById(1);
        assertNotNull(p);
        assertEquals("Nguyễn Văn Tổ", p.getFullName());
        assertEquals(Gender.MALE, p.getGender());
        assertEquals(1930, p.getBirthYear());
        assertNull(p.getFather());
        assertNull(p.getMother());
    }

    @Test
    @Order(2)
    @DisplayName("getPersonById(99999) ném PersonNotFoundException")
    void getPersonByIdThrowsWhenNotFound() {
        PersonNotFoundException ex = assertThrows(PersonNotFoundException.class,
                () -> service.getPersonById(99999));
        assertNotNull(ex.getMessage());
    }

    @Test
    @Order(3)
    @DisplayName("getAllPersons() trả về 7 bản ghi mẫu")
    void getAllPersonsReturnsSampleRecords() {
        List<Person> all = service.getAllPersons();
        assertNotNull(all);
        assertEquals(TestDatabase.SAMPLE_RECORD_COUNT, all.size());
    }

    // ============================================================
    // NHÓM 2 — BR1: Tự gán làm cha/mẹ chính mình
    // ============================================================

    @Test
    @Order(4)
    @DisplayName("BR1 — assignFather(child, child) ném SelfParentException")
    void assignFatherToSelfThrowsSelfParent() {
        assertThrows(SelfParentException.class, () -> service.assignFather(1, 1));
    }

    @Test
    @Order(5)
    @DisplayName("BR1 — assignMother(child, child) ném SelfParentException")
    void assignMotherToSelfThrowsSelfParent() {
        assertThrows(SelfParentException.class, () -> service.assignMother(1, 1));
    }

    // ============================================================
    // NHÓM 3 — BR3: Không tạo chu trình
    // ============================================================

    @Test
    @Order(6)
    @DisplayName("BR3 — assignFather tạo chu trình ném CycleDetectedException")
    void assignFatherCreatingCycleThrows() {
        // 1 là tổ tiên của 5 (1 -> 3 -> 5)
        assertThrows(CycleDetectedException.class, () -> service.assignFather(1, 5));
    }

    @Test
    @Order(7)
    @DisplayName("BR3 — assignMother tạo chu trình ném CycleDetectedException")
    void assignMotherCreatingCycleThrows() {
        // Chu trình theo nhánh MẸ: cần một nữ có hậu duệ là 1.
        // Tạo nữ "Chu Trình Mẹ" là con của ID 3 (hậu duệ của ID 1) → 1 là tổ tiên của nữ này.
        Person female = service.addPerson(new Person("Chu Trình Mẹ", 2010, Gender.FEMALE, null));
        try {
            service.assignFather(female.getId(), 3);

            // Gán nữ này làm MẸ của 1 → 1 là tổ tiên của chính người mẹ → chu trình
            assertThrows(CycleDetectedException.class, () -> service.assignMother(1, female.getId()));
        } finally {
            if (female.getId() > 0) {
                try {
                    service.clearFather(female.getId());
                    service.deletePerson(female.getId());
                } catch (Exception ignored) {
                    // dọn dẹp là thao tác phụ trợ
                }
            }
        }
    }

    // ============================================================
    // NHÓM 4 — BR6: Giới tính vai trò cha/mẹ
    // ============================================================

    @Test
    @Order(8)
    @DisplayName("BR6 — gán cha có giới tính FEMALE ném InvalidParentGenderException")
    void assignFatherWithFemaleGenderThrows() {
        // ID 2 = Trần Thị Tổ (FEMALE)
        InvalidParentGenderException ex = assertThrows(InvalidParentGenderException.class,
                () -> service.assignFather(5, 2));
        assertTrue(ex.getMessage().toLowerCase().contains("males")
                        || ex.getMessage().contains("MALE")
                        || ex.getMessage().contains("Cha"),
                "Thông báo lỗi phải nói rõ vai trò/giới tính: " + ex.getMessage());
    }

    @Test
    @Order(9)
    @DisplayName("BR6 — gán mẹ có giới tính MALE ném InvalidParentGenderException")
    void assignMotherWithMaleGenderThrows() {
        // ID 1 = Nguyễn Văn Tổ (MALE)
        assertThrows(InvalidParentGenderException.class, () -> service.assignMother(5, 1));
    }

    // ============================================================
    // NHÓM 5 — BR4: Không xóa Person đang được tham chiếu
    // ============================================================

    @Test
    @Order(10)
    @DisplayName("BR4 — deletePerson(3) khi đang là cha của ID 5 ném PersonInUseException")
    void deletePersonInUseThrows() {
        assertThrows(PersonInUseException.class, () -> service.deletePerson(3));
    }

    @Test
    @Order(11)
    @DisplayName("BR4 — deletePerson với id không tồn tại ném PersonNotFoundException")
    void deleteNonExistentThrows() {
        assertThrows(PersonNotFoundException.class, () -> service.deletePerson(99999));
    }

    // ============================================================
    // NHÓM 6 — Duyệt tổ tiên / hậu duệ
    // ============================================================

    @Test
    @Order(12)
    @DisplayName("getAncestors(7) trả về đủ 6 tổ tiên, không chứa chính mình, không trùng lặp")
    void getAncestorsReturnsFullChain() {
        List<Person> ancestors = service.getAncestors(7);
        Set<Integer> ids = ancestors.stream().map(Person::getId).collect(Collectors.toSet());

        assertTrue(ids.contains(5), "Phải chứa cha (ID 5)");
        assertTrue(ids.contains(6), "Phải chứa mẹ (ID 6)");
        assertTrue(ids.contains(3), "Phải chứa ông (ID 3)");
        assertTrue(ids.contains(4), "Phải chứa bà (ID 4)");
        assertTrue(ids.contains(1), "Phải chứa tổ (ID 1)");
        assertTrue(ids.contains(2), "Phải chứa mẹ tổ (ID 2)");
        assertFalse(ids.contains(7), "Không được chứa chính người được hỏi");
        assertEquals(ancestors.size(), ids.size(), "Không được có tổ tiên trùng lặp");
    }

    @Test
    @Order(13)
    @DisplayName("getDescendants(1) trả về đủ các hậu duệ (3, 5, 7), không trùng lặp")
    void getDescendantsReturnsFullChain() {
        List<Person> descendants = service.getDescendants(1);
        Set<Integer> ids = descendants.stream().map(Person::getId).collect(Collectors.toSet());

        assertTrue(ids.contains(3), "Phải chứa con (ID 3)");
        assertTrue(ids.contains(5), "Phải chứa cháu (ID 5)");
        assertTrue(ids.contains(7), "Phải chứa chắt (ID 7)");
        assertFalse(ids.contains(1), "Không được chứa chính người được hỏi");
        assertEquals(descendants.size(), ids.size(), "Không được có hậu duệ trùng lặp");
    }

    // ============================================================
    // NHÓM 7 — BR2: Cha trùng mẹ
    // ============================================================

    @Test
    @Order(14)
    @DisplayName("BR2 — addPerson với cùng một người làm cha và mẹ ném SameParentException")
    void addPersonWithSameFatherAndMotherThrows() {
        Person father = service.getPersonById(1);
        Person p = new Person("Test Trùng Cha Mẹ", 2000, Gender.MALE, null);
        p.setFather(father);
        p.setMother(father);

        assertThrows(SameParentException.class, () -> service.addPerson(p));
    }

    @Test
    @Order(15)
    @DisplayName("BR2 — assignFather trùng với mẹ hiện tại ném SameParentException")
    void assignFatherSameAsCurrentMotherThrows() {
        // ID 5 đang có mother = 4; gán father = 4 => vi phạm BR2
        assertThrows(SameParentException.class, () -> service.assignFather(5, 4));
    }

    @Test
    @Order(16)
    @DisplayName("BR2 — assignMother trùng với cha hiện tại ném SameParentException")
    void assignMotherSameAsCurrentFatherThrows() {
        // ID 5 đang có father = 3; gán mother = 3 => vi phạm BR2
        assertThrows(SameParentException.class, () -> service.assignMother(5, 3));
    }

    @Test
    @Order(17)
    @DisplayName("BR2 — BR2 được kiểm tra TRƯỚC BR6 (thông báo chính xác hơn)")
    void sameParentCheckedBeforeGender() {
        // ID 5 đang có father = 3 (MALE) và mother = 4 (FEMALE).
        // Gán mother = 3 vi phạm cả BR2 (trùng cha) lẫn BR6 (MALE làm mẹ).
        // Service phải ném SameParentException (BR2) chứ không phải
        // InvalidParentGenderException (BR6) để thông báo chính xác hơn.
        SameParentException ex = assertThrows(SameParentException.class,
                () -> service.assignMother(5, 3));
        assertNotNull(ex.getMessage());
    }

    // ============================================================
    // NHÓM 8 — Gán cha/mẹ thành công
    // ============================================================

    @Test
    @Order(18)
    @DisplayName("assignFather(5, 1) thành công — fatherId được cập nhật trong DB")
    void assignFatherSucceeds() {
        service.assignFather(5, 1);
        Person updated = service.getPersonById(5);
        assertNotNull(updated.getFather());
        assertEquals(1, updated.getFather().getId());
    }

    @Test
    @Order(19)
    @DisplayName("assignMother(5, 4) thành công — motherId được cập nhật trong DB")
    void assignMotherSucceeds() {
        service.assignMother(5, 4);
        Person updated = service.getPersonById(5);
        assertNotNull(updated.getMother());
        assertEquals(4, updated.getMother().getId());
    }

    // ============================================================
    // NHÓM 9 — Validation dữ liệu
    // ============================================================

    @Test
    @Order(20)
    @DisplayName("TC02 — addPerson với fullName null hoặc rỗng ném IllegalArgumentException")
    void addPersonWithBlankNameThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addPerson(new Person(null, 1995, Gender.MALE, null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.addPerson(new Person("   ", 1995, Gender.MALE, null)));
    }

    @Test
    @Order(21)
    @DisplayName("TC02 — addPerson với fullName dài hơn 100 ký tự ném IllegalArgumentException")
    void addPersonWithTooLongNameThrows() {
        String longName = "A".repeat(101);
        assertThrows(IllegalArgumentException.class,
                () -> service.addPerson(new Person(longName, 1995, Gender.MALE, null)));
    }

    @Test
    @Order(22)
    @DisplayName("TC03 — addPerson với birthYear = 999 ném InvalidBirthYearException")
    void addPersonWithTooOldBirthYearThrows() {
        assertThrows(InvalidBirthYearException.class,
                () -> service.addPerson(new Person("Test Năm Sinh Quá Cũ", 999, Gender.MALE, null)));
    }

    @Test
    @Order(23)
    @DisplayName("TC04 — addPerson với birthYear tương lai ném InvalidBirthYearException")
    void addPersonWithFutureBirthYearThrows() {
        int nextYear = Year.now().getValue() + 1;
        assertThrows(InvalidBirthYearException.class,
                () -> service.addPerson(new Person("Test Năm Sinh Tương Lai", nextYear, Gender.MALE, null)));
    }

    @Test
    @Order(24)
    @DisplayName("TC03B — addPerson chấp nhận birthYear ở biên hợp lệ (1000 và năm hiện tại)")
    void addPersonAcceptsBoundaryBirthYears() {
        int currentYear = Year.now().getValue();
        Person lower = null;
        Person upper = null;
        try {
            lower = service.addPerson(new Person("Test Năm Sinh Biên Dưới", 1000, Gender.MALE, null));
            upper = service.addPerson(new Person("Test Năm Sinh Biên Trên", currentYear, Gender.FEMALE, null));

            assertTrue(lower.getId() > 0, "Person biên dưới phải được tạo thành công");
            assertTrue(upper.getId() > 0, "Person biên trên phải được tạo thành công");
            assertEquals(1000, lower.getBirthYear());
            assertEquals(currentYear, upper.getBirthYear());
        } finally {
            if (upper != null && upper.getId() > 0) {
                service.deletePerson(upper.getId());
            }
            if (lower != null && lower.getId() > 0) {
                service.deletePerson(lower.getId());
            }
        }
    }

    @Test
    @Order(25)
    @DisplayName("TC01 — addPerson hợp lệ sinh ID và query lại được trong DB")
    void addPersonCreatesRecord() {
        Person added = null;
        try {
            added = service.addPerson(new Person("Nguyễn Văn Mới", 1995, Gender.MALE, null));
            assertTrue(added.getId() > 0, "ID phải lớn hơn 0");

            Person queried = service.getPersonById(added.getId());
            assertNotNull(queried);
            assertEquals("Nguyễn Văn Mới", queried.getFullName());
            assertEquals(1995, queried.getBirthYear());
            assertEquals(Gender.MALE, queried.getGender());
        } finally {
            if (added != null && added.getId() > 0) {
                service.deletePerson(added.getId());
            }
        }
    }

    @Test
    @Order(26)
    @DisplayName("addPerson với gender null ném IllegalArgumentException")
    void addPersonWithNullGenderThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addPerson(new Person("Test Không Giới Tính", 1995, null, null)));
    }

    @Test
    @Order(27)
    @DisplayName("addPerson với photoPath null hoặc rỗng vẫn hợp lệ (photoPath là tuỳ chọn)")
    void addPersonAcceptsOptionalPhotoPath() {
        Person withNull = null;
        Person withEmpty = null;
        try {
            withNull = service.addPerson(new Person("Test Ảnh Null", 1995, Gender.MALE, null));
            withEmpty = service.addPerson(new Person("Test Ảnh Rỗng", 1995, Gender.MALE, "   "));
            assertTrue(withNull.getId() > 0);
            assertTrue(withEmpty.getId() > 0);
        } finally {
            if (withEmpty != null && withEmpty.getId() > 0) {
                service.deletePerson(withEmpty.getId());
            }
            if (withNull != null && withNull.getId() > 0) {
                service.deletePerson(withNull.getId());
            }
        }
    }

    @Test
    @Order(28)
    @DisplayName("TC12 — assignFather với parentId không tồn tại ném PersonNotFoundException")
    void assignFatherWithNonExistentParentThrows() {
        assertThrows(PersonNotFoundException.class, () -> service.assignFather(5, 99999));
    }

    @Test
    @Order(29)
    @DisplayName("TC14 — deletePerson với leaf node (ID 7) thành công")
    void deleteLeafNodeSucceeds() {
        service.deletePerson(7);
        assertThrows(PersonNotFoundException.class, () -> service.getPersonById(7));
    }

    // ============================================================
    // NHÓM 10 — clearFather / clearMother
    // ============================================================

    @Test
    @Order(30)
    @DisplayName("TC-CLEAR-01 — clearFather(5) gỡ quan hệ cha thành công")
    void clearFatherRemovesRelation() {
        service.clearFather(5);
        assertNull(service.getPersonById(5).getFather());
    }

    @Test
    @Order(31)
    @DisplayName("TC-CLEAR-02 — clearMother(5) gỡ quan hệ mẹ thành công")
    void clearMotherRemovesRelation() {
        service.clearMother(5);
        assertNull(service.getPersonById(5).getMother());
    }

    @Test
    @Order(32)
    @DisplayName("TC-CLEAR-03 — clearFather với childId không tồn tại ném PersonNotFoundException")
    void clearFatherWithNonExistentChildThrows() {
        assertThrows(PersonNotFoundException.class, () -> service.clearFather(99999));
    }

    @Test
    @Order(33)
    @DisplayName("TC-CLEAR-04 — clearMother với childId không tồn tại ném PersonNotFoundException")
    void clearMotherWithNonExistentChildThrows() {
        assertThrows(PersonNotFoundException.class, () -> service.clearMother(99999));
    }

    @Test
    @Order(34)
    @DisplayName("TC-CLEAR-05 — Sau khi gỡ quan hệ thì mới xóa được người cha")
    void canDeleteParentAfterClearingRelation() {
        Person[] handles = new Person[2]; // [0] = cha tạm, [1] = con tạm
        try {
            handles[0] = service.addPerson(new Person("Cha Tạm TC05", 1970, Gender.MALE, null));
            handles[1] = service.addPerson(new Person("Con Tạm TC05", 1995, Gender.MALE, null));
            Person tempParent = handles[0];
            Person tempChild = handles[1];

            service.assignFather(tempChild.getId(), tempParent.getId());

            // Trước khi gỡ quan hệ: bị chặn bởi BR4
            assertThrows(PersonInUseException.class, () -> service.deletePerson(tempParent.getId()));

            // Sau khi gỡ quan hệ: xóa được
            service.clearFather(tempChild.getId());
            assertDoesNotThrow(() -> service.deletePerson(tempParent.getId()));
        } finally {
            for (Person p : handles) {
                if (p != null && p.getId() > 0) {
                    try {
                        service.deletePerson(p.getId());
                    } catch (Exception ignored) {
                        // dọn dẹp là thao tác phụ trợ
                    }
                }
            }
        }
    }

    // ============================================================
    // NHÓM 11 — BR5 (quy tắc cảnh báo)
    // ============================================================

    @Test
    @Order(35)
    @DisplayName("BR5 — checkBirthYearWarning trả về danh sách rỗng khi cha/mẹ sinh sớm hơn con")
    void birthYearWarningEmptyWhenValid() {
        Person child = new Person("Con", 2000, Gender.MALE, null);
        Person father = new Person("Cha", 1970, Gender.MALE, null);
        Person mother = new Person("Mẹ", 1972, Gender.FEMALE, null);

        assertTrue(service.checkBirthYearWarning(child, father, mother).isEmpty());
    }

    @Test
    @Order(36)
    @DisplayName("BR5 — checkBirthYearWarning cảnh báo khi cha sinh không sớm hơn con")
    void birthYearWarningReportsViolation() {
        Person child = new Person("Con", 2000, Gender.MALE, null);
        Person father = new Person("Cha", 2000, Gender.MALE, null);
        Person mother = new Person("Mẹ", 2005, Gender.FEMALE, null);

        List<String> warnings = service.checkBirthYearWarning(child, father, mother);
        assertEquals(2, warnings.size(), "Phải có 2 cảnh báo (cha + mẹ): " + warnings);
    }

    @Test
    @Order(37)
    @DisplayName("BR5 — checkBirthYearWarning với child null trả về danh sách rỗng")
    void birthYearWarningWithNullChild() {
        assertTrue(service.checkBirthYearWarning(null, null, null).isEmpty());
    }

    // ============================================================
    // NHÓM 12 — searchPersons
    // ============================================================

    @Test
    @Order(38)
    @DisplayName("searchPersons('Tổ') trả về các thành viên chứa từ khóa")
    void searchPersonsFindsByKeyword() {
        List<Person> results = service.searchPersons("Tổ");
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Phải tìm thấy ít nhất một kết quả");
        assertTrue(results.stream().allMatch(p -> p.getFullName().contains("Tổ")));
    }

    @Test
    @Order(39)
    @DisplayName("searchPersons(null) trả về toàn bộ danh sách")
    void searchPersonsWithNullReturnsAll() {
        assertEquals(TestDatabase.SAMPLE_RECORD_COUNT, service.searchPersons(null).size());
    }

    // ============================================================
    // NHÓM 13 — updatePerson
    // ============================================================

    @Test
    @Order(40)
    @DisplayName("updatePerson chỉ cập nhật thông tin cơ bản, giữ nguyên cha/mẹ")
    void updatePersonKeepsParents() {
        Person original = service.getPersonById(5);

        Person updated = new Person("Nguyễn Văn Con Mới", 1986, Gender.MALE, null);
        updated.setId(5);
        updated.setFather(original.getFather());
        updated.setMother(original.getMother());

        service.updatePerson(updated);

        Person verify = service.getPersonById(5);
        assertEquals("Nguyễn Văn Con Mới", verify.getFullName());
        assertEquals(1986, verify.getBirthYear());
        assertNotNull(verify.getFather());
        assertEquals(3, verify.getFather().getId());
        assertNotNull(verify.getMother());
        assertEquals(4, verify.getMother().getId());
    }

    @Test
    @Order(41)
    @DisplayName("updatePerson với id <= 0 ném IllegalArgumentException")
    void updatePersonWithInvalidIdThrows() {
        Person p = new Person("Không Có Id", 1995, Gender.MALE, null);
        assertThrows(IllegalArgumentException.class, () -> service.updatePerson(p));
    }

    @Test
    @Order(42)
    @DisplayName("updatePerson với id không tồn tại ném PersonNotFoundException")
    void updatePersonWithNonExistentIdThrows() {
        Person p = new Person("Không Tồn Tại", 1995, Gender.MALE, null);
        p.setId(99999);
        assertThrows(PersonNotFoundException.class, () -> service.updatePerson(p));
    }

    // ============================================================
    // NHÓM 14 — Constructor & null-safety
    // ============================================================

    @Test
    @Order(43)
    @DisplayName("Constructor với repository null ném NullPointerException")
    void constructorRejectsNullRepository() {
        assertThrows(NullPointerException.class, () -> new FamilyTreeService(null));
    }

    @Nested
    @DisplayName("Kiểm tra cô lập dữ liệu")
    class DataIsolation {

        @Test
        @DisplayName("Bảng person luôn có đúng 7 bản ghi mẫu sau mỗi test")
        void databaseIsRestored() {
            assertEquals(TestDatabase.SAMPLE_RECORD_COUNT, TestDatabase.countPersons(TEST_DB_NAME),
                    "CSDL phải được trả về đúng 7 bản ghi mẫu sau khi chạy test");
        }
    }
}
