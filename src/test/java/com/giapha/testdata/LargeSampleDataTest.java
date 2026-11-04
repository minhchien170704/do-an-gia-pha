package com.giapha.testdata;

import com.giapha.database.DatabaseConnection;
import com.giapha.exception.CycleDetectedException;
import com.giapha.exception.InvalidBirthYearException;
import com.giapha.exception.InvalidParentGenderException;
import com.giapha.exception.PersonInUseException;
import com.giapha.exception.SameParentException;
import com.giapha.exception.SelfParentException;
import com.giapha.model.Gender;
import com.giapha.model.Person;
import com.giapha.repository.JdbcPersonRepository;
import com.giapha.service.FamilyTreeService;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bộ kiểm thử nghiệp vụ trên BỘ DỮ LIỆU MẪU MỞ RỘNG (30 người / 5 thế hệ)
 * nằm trong {@code test-data/sample-data.sql}.
 *
 * <p>Khác với các bộ test khác, bộ test này dùng một CSDL RIÊNG
 * ({@code gia_pha_test}) để không ảnh hưởng tới dữ liệu mẫu 7 bản ghi của
 * database chính {@code gia_pha}. Script SQL được nạp tự động từ file
 * {@code test-data/sample-data.sql}, nên tập tin này vừa là dữ liệu mẫu
 * vừa là nguồn kiểm thử — luôn đồng bộ.
 *
 * <p>Kết quả thực tế của từng test case được ghi lại và in ra dưới dạng bảng
 * Markdown để đồng bộ với {@code test-data/test-cases.md}.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Bộ dữ liệu mẫu 30 người / 5 thế hệ")
public class LargeSampleDataTest {

    /** Tên database riêng dùng cho bộ test này. */
    private static final String TEST_DB_NAME = "gia_pha_test";

    /**
     * Hậu tố bắt buộc của database test.
     * <p>Ràng buộc an toàn: chỉ database có tên KẾT THÚC bằng hậu tố này mới được
     * phép DROP. Mọi tên khác bị từ chối và báo lỗi — để không bao giờ xóa nhầm
     * database chính {@code gia_pha}.
     */
    private static final String REQUIRED_DB_SUFFIX = "_test";

    /**
     * System property cho phép giữ lại database test sau khi chạy xong.
     * <p>Đặt bằng {@code -Dkeep.test.db=true} (mặc định là {@code false} → tự DROP).
     */
    private static final String KEEP_DB_PROPERTY = "keep.test.db";

    private static final String TEST_DB_URL =
            "jdbc:mysql://localhost:3306/" + TEST_DB_NAME
                    + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    /** Đường dẫn tới script SQL dữ liệu mẫu. */
    private static final Path SAMPLE_SQL = Paths.get("test-data", "sample-data.sql");

    /** Bảng kết quả từng test case (mã, mô tả, kết quả mong đợi, kết quả thực tế, trạng thái). */
    private static final List<String[]> RESULTS = new ArrayList<>();

    private static FamilyTreeService service;

    // ============================================================
    // SETUP — tạo database riêng và nạp dữ liệu mẫu
    // ============================================================

    /**
     * Repository trừ tới database riêng của bộ test, mỗi lần gọi tạo Connection mới.
     * Việc ghi đè {@code getConnection()} tránh việc dùng chung database chính.
     */
    private static final class TestRepo extends JdbcPersonRepository {
        @Override
        protected Connection getConnection() throws SQLException {
            return DatabaseConnection.getConnection(
                    TEST_DB_URL, DatabaseConnection.getUser(), DatabaseConnection.getPassword());
        }
    }

    /**
     * Kiểm tra an toàn trước khi DROP bất kỳ database nào.
     *
     * <p>Chỉ chấp nhận tên database KẾT THÚC bằng {@value #REQUIRED_DB_SUFFIX}
     * (ví dụ {@code gia_pha_test}). Nếu không khớp → ném {@link IllegalStateException}
     * và KHÔNG thực hiện DROP.
     *
     * @param dbName tên database cần kiểm tra
     * @throws IllegalStateException nếu tên database không kết thúc bằng hậu tố bắt buộc
     */
    private static void assertSafeToDrop(String dbName) {
        if (dbName == null || dbName.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Từ chối DROP database: tên database rỗng. "
                            + "Chỉ được DROP database có tên kết thúc bằng \"" + REQUIRED_DB_SUFFIX + "\".");
        }

        String normalized = dbName.trim();
        if (!normalized.toLowerCase(Locale.ROOT).endsWith(REQUIRED_DB_SUFFIX)) {
            throw new IllegalStateException(
                    "Từ chối DROP database \"" + normalized + "\": tên database bắt buộc phải kết thúc bằng \""
                            + REQUIRED_DB_SUFFIX + "\" để bảo đảm không xóa nhầm dữ liệu chính.");
        }
    }

    /**
     * Kiểm tra người dùng có yêu cầu giữ lại database test hay không.
     *
     * @return true nếu cần giữ lại (không DROP)
     */
    private static boolean shouldKeepTestDatabase() {
        return Boolean.parseBoolean(System.getProperty(KEEP_DB_PROPERTY, "false"));
    }

    @BeforeAll
    static void setup() throws Exception {
        // 0. Kiểm tra an toàn TRƯỚC khi DROP (ràng buộc bắt buộc)
        assertSafeToDrop(TEST_DB_NAME);

        // 1. Tạo database riêng — DROP + CREATE ở đầu mỗi lần chạy
        String rootUrl = "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        try (Connection conn = DriverManager.getConnection(rootUrl,
                DatabaseConnection.getUser(), DatabaseConnection.getPassword());
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP DATABASE IF EXISTS " + TEST_DB_NAME);
            stmt.executeUpdate("CREATE DATABASE " + TEST_DB_NAME
                    + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }

        // 2. Tạo bảng PERSON giống hệt database/schema.sql
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL,
                DatabaseConnection.getUser(), DatabaseConnection.getPassword());
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                    CREATE TABLE PERSON (
                        id          INT AUTO_INCREMENT PRIMARY KEY,
                        full_name   VARCHAR(100) NOT NULL,
                        birth_year  INT NOT NULL,
                        gender      ENUM('MALE','FEMALE') NOT NULL,
                        photo_path  VARCHAR(255) NULL,
                        father_id   INT NULL,
                        mother_id   INT NULL,
                        CONSTRAINT fk_father FOREIGN KEY (father_id) REFERENCES PERSON(id) ON DELETE RESTRICT,
                        CONSTRAINT fk_mother FOREIGN KEY (mother_id) REFERENCES PERSON(id) ON DELETE RESTRICT,
                        CONSTRAINT chk_birth_year CHECK (birth_year >= 1000 AND birth_year <= 2100),
                        CONSTRAINT chk_father_not_mother CHECK (
                            father_id IS NULL OR mother_id IS NULL OR father_id <> mother_id)
                    )
                    """);
            stmt.executeUpdate("CREATE INDEX idx_person_name ON PERSON(full_name)");
            stmt.executeUpdate("""
                    CREATE TRIGGER trg_check_not_self_parent_update
                    BEFORE UPDATE ON PERSON
                    FOR EACH ROW
                    BEGIN
                        IF (NEW.father_id IS NOT NULL AND NEW.father_id = NEW.id) OR
                           (NEW.mother_id IS NOT NULL AND NEW.mother_id = NEW.id) THEN
                            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Một người không thể là cha hoặc mẹ của chính mình';
                        END IF;
                    END
                    """);
        }

        // 3. Nạp script dữ liệu mẫu
        loadSampleData();

        service = new FamilyTreeService(new TestRepo());
    }

    /**
     * Đọc và thực thi script {@code test-data/sample-data.sql} vào database riêng.
     * Câu lệnh {@code USE gia_pha;} trong script được thay bằng database riêng của bộ test.
     */
    private static void loadSampleData() throws IOException, SQLException {
        String script = Files.readString(SAMPLE_SQL, StandardCharsets.UTF_8);
        script = script.replace("USE gia_pha;", "USE " + TEST_DB_NAME + ";");

        // Bỏ comment dòng để tách câu lệnh
        StringBuilder cleaned = new StringBuilder();
        for (String line : script.split("\r?\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("--")) {
                cleaned.append('\n');
            } else {
                cleaned.append(line).append('\n');
            }
        }

        try (Connection conn = DriverManager.getConnection(TEST_DB_URL,
                DatabaseConnection.getUser(), DatabaseConnection.getPassword());
             Statement stmt = conn.createStatement()) {

            for (String rawStatement : cleaned.toString().split(";")) {
                String sql = rawStatement.trim();
                if (sql.isEmpty()) {
                    continue;
                }
                // Bỏ các lệnh USE (đã trỏ đúng database) và lệnh ALTER AUTO_INCREMENT không cần thiết
                if (sql.toUpperCase().startsWith("USE ")) {
                    continue;
                }
                stmt.execute(sql);
            }
        }
    }

    /**
     * Dọn dẹp sau khi toàn bộ test case đã chạy xong.
     *
     * <p>Hành vi:
     * <ul>
     *     <li>Nếu {@code -Dkeep.test.db=true} → GIỮ LẠI database test, chỉ in thông báo.</li>
     *     <li>Nếu không → kiểm tra an toàn {@link #assertSafeToDrop(String)} rồi DROP database test.</li>
     * </ul>
     *
     * <p>Việc kiểm tra an toàn được thực hiện LẠI ở đây (không chỉ ở {@code @BeforeAll})
     * để bảo đảm không có đường nào gây DROP nhầm database chính {@code gia_pha},
     * kể cả khi {@code TEST_DB_NAME} bị thay đổi trong tương lai.
     */
    @AfterAll
    static void tearDown() {
        if (shouldKeepTestDatabase()) {
            System.out.println("[LargeSampleDataTest] Giữ lại database test \"" + TEST_DB_NAME
                    + "\" (do -D" + KEEP_DB_PROPERTY + "=true).");
            return;
        }

        try {
            // Kiểm tra an toàn — ném IllegalStateException nếu tên DB không kết thúc bằng "_test"
            assertSafeToDrop(TEST_DB_NAME);

            String rootUrl = "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
            try (Connection conn = DriverManager.getConnection(rootUrl,
                    DatabaseConnection.getUser(), DatabaseConnection.getPassword());
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DROP DATABASE IF EXISTS " + TEST_DB_NAME);
            }
            System.out.println("[LargeSampleDataTest] Đã DROP database test \"" + TEST_DB_NAME + "\".");
        } catch (IllegalStateException e) {
            // Tên DB không an toàn → báo lỗi rõ ràng, KHÔNG DROP
            System.err.println("[LargeSampleDataTest] LỖI AN TOÀN: " + e.getMessage());
            throw e;
        } catch (SQLException e) {
            System.err.println("[LargeSampleDataTest] Không thể DROP database test \"" + TEST_DB_NAME
                    + "\": " + e.getMessage());
        }
    }

    // ============================================================
    // CÁC TEST CASE
    // ============================================================

    @Test
    @Order(1)
    @DisplayName("TC-01: Nạp bộ dữ liệu mẫu thành công")
    void tc01_loadSampleData() {
        int count = service.getAllPersons().size();
        record("TC-01", "Nạp bộ dữ liệu mẫu 30 người / 5 thế hệ",
                "30 bản ghi", count + " bản ghi", count == 30);
        assertEquals(30, count);
    }

    @Test
    @Order(2)
    @DisplayName("TC-02: Cấu trúc gia phả có đúng 5 thế hệ")
    void tc02_fiveGenerations() {
        int maxDepth = 0;
        for (Person p : service.getAllPersons()) {
            int depth = depthOf(p.getId());
            maxDepth = Math.max(maxDepth, depth);
        }
        // Thế hệ 1 = 1; mỗi bước lên cha hoặc mẹ tăng thêm 1
        record("TC-02", "Số thế hệ lớn nhất trong cây gia phả",
                "5 thế hệ", maxDepth + " thế hệ", maxDepth == 5);
        assertEquals(5, maxDepth);
    }

    @Test
    @Order(3)
    @DisplayName("TC-03: Truy vấn hậu duệ của thủ tổ")
    void tc03_descendantsOfRoot() {
        List<Person> descendants = service.getDescendants(1);
        record("TC-03", "Số hậu duệ của \"Nguyễn Văn Tổ\" (id=1)",
                "18 người", descendants.size() + " người", descendants.size() == 18);
        assertEquals(18, descendants.size());
    }

    @Test
    @Order(4)
    @DisplayName("TC-04: Truy vấn tổ tiên của thế hệ 5")
    void tc04_ancestorsOfFifthGeneration() {
        List<Person> ancestors = service.getAncestors(23);
        record("TC-04", "Số tổ tiên của \"Nguyễn Văn An\" (id=23)",
                "8 người", ancestors.size() + " người", ancestors.size() == 8);
        assertEquals(8, ancestors.size());
    }

    @Test
    @Order(5)
    @DisplayName("TC-05: BR1 — không được tự gán làm cha của chính mình")
    void tc05_selfParent() {
        String message = assertThrows(SelfParentException.class,
                () -> service.assignFather(7, 7)).getMessage();
        record("TC-05", "BR1 — assignFather(7, 7)",
                "Ném SelfParentException", message, true);
    }

    @Test
    @Order(6)
    @DisplayName("TC-06: BR2 — cha và mẹ không được là cùng một người")
    void tc06_sameParent() {
        // id 16 (Vũ Thị Mẹ Hùng) là mẹ của id 23 -> gán làm cha sẽ vi phạm BR2
        String message = assertThrows(SameParentException.class,
                () -> service.assignFather(23, 16)).getMessage();
        record("TC-06", "BR2 — assignFather(23, 16) trong khi 16 là mẹ của 23",
                "Ném SameParentException", message, true);
    }

    @Test
    @Order(7)
    @DisplayName("TC-07: BR3 — không được tạo chu trình vòng lặp")
    void tc07_cycle() {
        // id 23 là cháu nội của id 1 -> gán 1 làm cha của 23 không tạo chu trình,
        // nhưng gán 23 làm cha của 1 sẽ tạo chu trình
        String message = assertThrows(CycleDetectedException.class,
                () -> service.assignFather(1, 23)).getMessage();
        record("TC-07", "BR3 — assignFather(1, 23) trong khi 23 là hậu duệ của 1",
                "Ném CycleDetectedException", message, true);
    }

    @Test
    @Order(8)
    @DisplayName("TC-08: BR4 — không được xóa người đang làm cha/mẹ")
    void tc08_personInUse() {
        String message = assertThrows(PersonInUseException.class,
                () -> service.deletePerson(3)).getMessage();
        record("TC-08", "BR4 — deletePerson(3) khi id 3 đang làm cha của 2 người",
                "Ném PersonInUseException", message, true);
    }

    @Test
    @Order(9)
    @DisplayName("TC-09: BR5 — cảnh báo năm sinh cha/mẹ không nhỏ hơn con")
    void tc09_birthYearWarning() {
        Person child = new Person("Nguyễn Văn Thử", 1900, Gender.MALE, null);
        Person father = new Person("Nguyễn Văn Cha Bảo", 1910, Gender.MALE, null);
        father.setId(90001);
        List<String> warnings = service.checkBirthYearWarning(child, father, null);
        record("TC-09", "BR5 — checkBirthYearWarning(con 1900, cha 1910)",
                "1 cảnh báo", warnings.size() + " cảnh báo", warnings.size() == 1);

        // Dữ liệu mẫu không có vi phạm BR5
        int violations = 0;
        for (Person p : service.getAllPersons()) {
            if (p.getFather() != null && p.getFather().getBirthYear() >= p.getBirthYear()) {
                violations++;
            }
            if (p.getMother() != null && p.getMother().getBirthYear() >= p.getBirthYear()) {
                violations++;
            }
        }
        record("TC-09b", "BR5 — số vi phạm trong bộ dữ liệu mẫu",
                "0 vi phạm", violations + " vi phạm", violations == 0);
        assertEquals(0, violations);
    }

    @Test
    @Order(10)
    @DisplayName("TC-10: BR6 — giới tính của cha/mẹ phải đúng vai trò")
    void tc06_parentGender() {
        // id 28 là FEMALE -> không thể làm cha
        String message = assertThrows(InvalidParentGenderException.class,
                () -> service.assignFather(23, 28)).getMessage();
        record("TC-10", "BR6 — assignFather(23, 28) trong khi id 28 là FEMALE",
                "Ném InvalidParentGenderException", message, true);

        // id 23 là MALE -> không thể làm mẹ
        String message2 = assertThrows(InvalidParentGenderException.class,
                () -> service.assignMother(29, 23)).getMessage();
        record("TC-10b", "BR6 — assignMother(29, 23) trong khi id 23 là MALE",
                "Ném InvalidParentGenderException", message2, true);
    }

    @Test
    @Order(11)
    @DisplayName("TC-11: Chấp nhận năm sinh từ 1000 trở lên")
    void tc11_minBirthYearAccepted() {
        Person p = new Person("Nguyễn Văn Cổ", 1000, Gender.MALE, null);
        Person added = service.addPerson(p);
        record("TC-11", "addPerson với birthYear = 1000",
                "Thành công, id > 0", "id = " + added.getId(), added.getId() > 0);
        assertTrue(added.getId() > 0);
        service.deletePerson(added.getId());
    }

    @Test
    @Order(12)
    @DisplayName("TC-12: Từ chối năm sinh nhỏ hơn 1000")
    void tc12_birthYearTooSmall() {
        Person p = new Person("Nguyễn Văn Quá Cổ", 999, Gender.MALE, null);
        String message = assertThrows(InvalidBirthYearException.class,
                () -> service.addPerson(p)).getMessage();
        record("TC-12", "addPerson với birthYear = 999",
                "Ném InvalidBirthYearException", message, true);
    }

    @Test
    @Order(13)
    @DisplayName("TC-13: Từ chối năm sinh trong tương lai")
    void tc13_birthYearInFuture() {
        int future = Year.now().getValue() + 1;
        Person p = new Person("Nguyễn Văn Tương Lai", future, Gender.MALE, null);
        String message = assertThrows(InvalidBirthYearException.class,
                () -> service.addPerson(p)).getMessage();
        record("TC-13", "addPerson với birthYear = " + future + " (năm hiện tại + 1)",
                "Ném InvalidBirthYearException", message, true);
    }

    @Test
    @Order(14)
    @DisplayName("TC-14: Tìm kiếm thành viên theo họ tên")
    void tc14_search() {
        List<Person> result = service.searchPersons("Nguyễn Văn");
        record("TC-14", "searchPersons(\"Nguyễn Văn\")",
                "19 kết quả", result.size() + " kết quả", result.size() == 19);
        assertEquals(19, result.size());

        List<Person> empty = service.searchPersons("Không Tồn Tại XYZ");
        record("TC-14b", "searchPersons(\"Không Tồn Tại XYZ\")",
                "0 kết quả", empty.size() + " kết quả", empty.isEmpty());
        assertTrue(empty.isEmpty());
    }

    @Test
    @Order(15)
    @DisplayName("TC-15: Từ chối họ tên rỗng")
    void tc15_emptyName() {
        Person p = new Person("   ", 1990, Gender.MALE, null);
        String message = assertThrows(IllegalArgumentException.class,
                () -> service.addPerson(p)).getMessage();
        record("TC-15", "addPerson với fullName = \"   \" (chỉ khoảng trắng)",
                "Ném IllegalArgumentException", message, true);
    }

    @Test
    @Order(16)
    @DisplayName("TC-16: Từ chối họ tên dài trên 100 ký tự")
    void tc16_nameTooLong() {
        Person p = new Person("A".repeat(101), 1990, Gender.MALE, null);
        String message = assertThrows(IllegalArgumentException.class,
                () -> service.addPerson(p)).getMessage();
        record("TC-16", "addPerson với fullName dài 101 ký tự",
                "Ném IllegalArgumentException", message, true);
    }

    @Test
    @Order(17)
    @DisplayName("TC-17: Xóa được thành viên lá (không có con)")
    void tc17_deleteLeaf() {
        service.deletePerson(30);
        int remaining = service.getAllPersons().size();
        record("TC-17", "deletePerson(30) — thành viên lá",
                "Xóa thành công, còn 29 bản ghi", remaining + " bản ghi còn lại", remaining == 29);
        assertEquals(29, remaining);
    }

    @Test
    @Order(18)
    @DisplayName("TC-18: Gỡ quan hệ cha/mẹ rồi mới xóa được")
    void tc18_clearRelationsThenDelete() {
        service.clearFather(23);
        service.clearFather(24);
        service.deletePerson(15);
        boolean stillThere = service.getAllPersons().stream().anyMatch(p -> p.getId() == 15);
        record("TC-18", "clearFather(23), clearFather(24) rồi deletePerson(15)",
                "Xóa thành công", stillThere ? "Vẫn còn trong DB" : "Đã xóa khỏi DB", !stillThere);
        assertFalse(stillThere);
    }

    @Test
    @Order(19)
    @DisplayName("TC-19: Ràng buộc CHECK của CSDL chặn năm sinh không hợp lệ")
    void tc19_dbCheckConstraint() {
        String error = null;
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL,
                DatabaseConnection.getUser(), DatabaseConnection.getPassword());
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO PERSON (full_name, birth_year, gender) VALUES (?, ?, ?)")) {
            ps.setString(1, "Nguyễn Văn Vi Phạm");
            ps.setInt(2, 500);
            ps.setString(3, "MALE");
            ps.executeUpdate();
        } catch (SQLException e) {
            error = e.getMessage();
        }
        boolean blocked = error != null;
        record("TC-19", "INSERT trực tiếp birth_year = 500 (bỏ qua tầng Service)",
                "CSDL từ chối (SQLException)", blocked ? "SQLException: " + shorten(error) : "Không bị chặn",
                blocked);
        assertTrue(blocked);
    }

    @Test
    @Order(20)
    @DisplayName("TC-20: Trigger CSDL chặn tự làm cha/mẹ chính mình")
    void tc20_dbSelfParentTrigger() {
        String error = null;
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL,
                DatabaseConnection.getUser(), DatabaseConnection.getPassword());
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE PERSON SET father_id = id WHERE id = 23")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            error = e.getMessage();
        }
        boolean blocked = error != null;
        record("TC-20", "UPDATE PERSON SET father_id = id (bỏ qua tầng Service)",
                "CSDL từ chối (SQLException)", blocked ? "SQLException: " + shorten(error) : "Không bị chặn",
                blocked);
        assertTrue(blocked);
    }

    // ============================================================
    // HELPER
    // ============================================================

    /**
     * Tính chiều sâu (số thế hệ) của một Person trong cây gia phả.
     * Thế hệ 1 = 1; mỗi bước lên cha hoặc mẹ tăng thêm 1.
     */
    private int depthOf(int personId) {
        Person p = service.getPersonById(personId);
        int fatherDepth = 0;
        int motherDepth = 0;
        if (p.getFather() != null) {
            fatherDepth = depthOf(p.getFather().getId());
        }
        if (p.getMother() != null) {
            motherDepth = depthOf(p.getMother().getId());
        }
        return 1 + Math.max(fatherDepth, motherDepth);
    }

    private static String shorten(String s) {
        if (s == null) {
            return "null";
        }
        String oneLine = s.replaceAll("\\s+", " ").trim();
        return oneLine.length() > 90 ? oneLine.substring(0, 90) + "..." : oneLine;
    }

    /**
     * Ghi nhận kết quả một test case và in ra bảng Markdown.
     */
    private static synchronized void record(String code, String description,
                                            String expected, String actual, boolean passed) {
        RESULTS.add(new String[]{code, description, expected, actual, passed ? "PASS" : "FAIL"});
        System.out.printf("| %s | %s | %s | %s | %s |%n",
                code, description, expected, actual, passed ? "PASS" : "FAIL");
    }

    @Test
    @Order(21)
    @DisplayName("TC-21: An toàn DROP — chỉ chấp nhận tên DB kết thúc bằng \"_test\"")
    void tc21_safeDropGuard() {
        // 1. Tên hợp lệ → không ném exception
        assertDoesNotThrow(() -> assertSafeToDrop("gia_pha_test"));
        record("TC-21a", "assertSafeToDrop(\"gia_pha_test\") — tên hợp lệ",
                "Không ném exception", "Không ném exception", true);

        // 2. Tên database CHÍNH → phải bị từ chối
        String message = assertThrows(IllegalStateException.class,
                () -> assertSafeToDrop("gia_pha")).getMessage();
        record("TC-21b", "assertSafeToDrop(\"gia_pha\") — tên database chính",
                "Ném IllegalStateException", message, true);

        // 3. Tên rỗng / null → phải bị từ chối
        assertThrows(IllegalStateException.class, () -> assertSafeToDrop(""));
        assertThrows(IllegalStateException.class, () -> assertSafeToDrop(null));
        record("TC-21c", "assertSafeToDrop(\"\") và assertSafeToDrop(null)",
                "Ném IllegalStateException", "Cả hai đều ném IllegalStateException", true);

        // 4. Tên chứa "_test" ở giữa nhưng không kết thúc → vẫn phải bị từ chối
        assertThrows(IllegalStateException.class, () -> assertSafeToDrop("test_gia_pha"));
        record("TC-21d", "assertSafeToDrop(\"test_gia_pha\") — chứa \"_test\" nhưng không kết thúc",
                "Ném IllegalStateException", "Ném IllegalStateException", true);
    }

    /**
     * In toàn bộ bảng kết quả ra stdout (được dùng để đồng bộ test-cases.md).
     */
    @Test
    @Order(99)
    @DisplayName("In bảng kết quả thực tế")
    void printReport() {
        System.out.println();
        System.out.println("===== BẢNG KẾT QUẢ THỰC TẾ =====");
        System.out.println("| Mã | Mô tả | Kết quả mong đợi | Kết quả thực tế | Trạng thái |");
        System.out.println("|---|---|---|---|---|");
        for (String[] row : RESULTS) {
            System.out.printf("| %s | %s | %s | %s | %s |%n",
                    row[0], row[1], row[2], row[3], row[4]);
        }
        System.out.println("===== TỔNG KẾT: " + RESULTS.size() + " test case =====");
        System.out.println("Các id trong bộ dữ liệu mẫu: " + Arrays.toString(new int[]{1, 23, 30}));
    }
}
