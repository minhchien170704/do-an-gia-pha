package com.giapha.repository;

import com.giapha.model.Gender;
import com.giapha.model.Person;
import com.giapha.support.TestDatabase;
import com.giapha.support.TestDatabaseManager;
import com.giapha.support.TestPersonRepository;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Kiểm thử quyền sở hữu Connection của {@link JdbcPersonRepository}.
 *
 * <p><b>Cô lập dữ liệu:</b> dùng database riêng {@code gia_pha_repo_test}
 * (tên kết thúc bằng {@code _test}), được tạo ở {@code @BeforeAll} và xóa ở {@code @AfterAll}.
 * Database chính {@code gia_pha} không bao giờ bị ghi vào.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("JdbcPersonRepository — quyền sở hữu Connection")
class JdbcPersonRepositoryConnectionTest {

    private static final String TEST_DB_NAME = "gia_pha_repo_test";

    @BeforeAll
    static void init() throws Exception {
        TestDatabaseManager.createFreshDatabase(TEST_DB_NAME);
        assumeTrue(TestDatabase.isAvailable(TEST_DB_NAME),
                "BỎ QUA: không kết nối được MySQL. Hãy khởi động MySQL trước khi chạy test.");
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
    // NHÓM 1 — Repository KHÔNG đóng Connection do bên ngoài cung cấp
    // ============================================================

    @Test
    @Order(1)
    @DisplayName("Repository nhận Connection từ bên ngoài KHÔNG đóng Connection đó")
    void externalConnectionIsNotClosed() throws Exception {
        Connection external = TestDatabaseManager.openConnection(TEST_DB_NAME);
        try {
            assertFalse(external.isClosed(), "Connection phải đang mở trước khi dùng");

            JdbcPersonRepository repo = new JdbcPersonRepository(external);
            List<Person> people = repo.findAll();

            assertFalse(external.isClosed(),
                    "Repository KHÔNG được đóng Connection do bên ngoài cung cấp");
            assertEquals(TestDatabase.SAMPLE_RECORD_COUNT, people.size(),
                    "Phải đọc được đúng số bản ghi của database test");
        } finally {
            external.close();
        }
    }

    @Test
    @Order(2)
    @DisplayName("Repository nhận Connection từ bên ngoài vẫn đọc đúng sau nhiều thao tác liên tiếp")
    void externalConnectionSurvivesMultipleOperations() throws Exception {
        Connection external = TestDatabaseManager.openConnection(TEST_DB_NAME);
        try {
            JdbcPersonRepository repo = new JdbcPersonRepository(external);

            repo.findAll();
            repo.findById(1);
            repo.search("Nguyễn");

            Person person = repo.findById(5);
            assertNotNull(person);
            person.setFullName("Đã sửa qua connection dùng chung");
            repo.update(person);

            repo.findAll();

            assertFalse(external.isClosed(),
                    "Connection dùng chung phải vẫn mở sau nhiều thao tác");
            assertEquals("Đã sửa qua connection dùng chung",
                    repo.findById(5).getFullName());
        } finally {
            external.close();
        }
    }

    // ============================================================
    // NHÓM 2 — KHÔNG tự ý chuyển sang database khác khi Connection đã đóng
    // ============================================================

    @Test
    @Order(3)
    @DisplayName("Connection bên ngoài đã đóng → repository ném lỗi, KHÔNG fallback sang DB chính")
    void closedExternalConnectionDoesNotFallBackToMainDatabase() throws Exception {
        Connection external = TestDatabaseManager.openConnection(TEST_DB_NAME);
        JdbcPersonRepository repo = new JdbcPersonRepository(external);

        // Đóng Connection bên ngoài (giả lập bên ngoài tự quản lý vòng đời)
        external.close();
        assertTrue(external.isClosed());

        RuntimeException error = assertThrows(RuntimeException.class, repo::findAll,
                "Phải ném lỗi khi Connection bên ngoài đã đóng");

        assertTrue(error.getMessage().contains("đã đóng"),
                "Thông báo lỗi phải nói rõ Connection đã đóng, thực tế: " + error.getMessage());

        // Quan trọng: phải KHÔNG đọc được dữ liệu từ database chính 'gia_pha'.
        // Nếu fallback xảy ra, findAll() sẽ trả về số bản ghi của DB chính (> 7).
        // Ở đây ta chỉ cần chắc chắn là lỗi được ném, không phải dữ liệu trả về.
        assertNotNull(error);
    }

    @Test
    @Order(4)
    @DisplayName("Repository tự quản lý Connection (constructor mặc định) vẫn hoạt động bình thường")
    void defaultConstructorStillWorks() throws Exception {
        // Constructor mặc định trỏ tới database CHÍNH (gia_pha) — đếm số bản ghi thật
        // của database chính để so sánh (chỉ đọc, không ghi).
        int mainDbCount;
        try (Connection mainConn = com.giapha.database.DatabaseConnection.getConnection();
             java.sql.Statement st = mainConn.createStatement();
             java.sql.ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM person")) {
            assertTrue(rs.next());
            mainDbCount = rs.getInt(1);
        }

        JdbcPersonRepository repo = new JdbcPersonRepository();
        List<Person> people = repo.findAll();

        assertEquals(mainDbCount, people.size(),
                "Constructor mặc định phải đọc từ database chính 'gia_pha'");
        assertTrue(mainDbCount > 0, "Database chính phải có dữ liệu");
    }

    // ============================================================
    // NHÓM 3 — TestPersonRepository (ghi đè getConnection) không bị ảnh hưởng
    // ============================================================

    @Test
    @Order(5)
    @DisplayName("TestPersonRepository ghi đè getConnection() vẫn trỏ đúng database test")
    void testPersonRepositoryStillIsolated() {
        TestPersonRepository repo = TestDatabaseManager.repositoryFor(TEST_DB_NAME);
        List<Person> people = repo.findAll();

        assertEquals(TestDatabase.SAMPLE_RECORD_COUNT, people.size(),
                "TestPersonRepository phải đọc từ database test, không phải database chính");
    }
}
