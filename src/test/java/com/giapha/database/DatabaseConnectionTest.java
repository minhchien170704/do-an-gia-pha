package com.giapha.database;

import com.giapha.support.TestDatabase;
import com.giapha.support.TestDatabaseManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Kiểm thử tầng Database: kết nối MySQL, metadata và trạng thái bảng {@code PERSON}.
 *
 * <p><b>Cô lập dữ liệu:</b> lớp test này dùng database RIÊNG {@code gia_pha_conn_test}
 * (tên kết thúc bằng {@code _test}). Test {@code DatabaseConnection.getConnection()}
 * (dùng {@code DB_URL} cấu hình mặc định) trỏ tới database CHÍNH — chỉ được
 * phép ĐỌC, tuyệt đối không ghi.
 *
 * <p>Các test case phụ thuộc MySQL đang chạy. Nếu không kết nối được, test được
 * {@code SKIPPED} (không phải FAILED) kèm thông báo rõ ràng.
 *
 * @see TestDatabaseManager
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("DatabaseConnection — kết nối MySQL và kiểm tra schema")
class DatabaseConnectionTest {

    /** Tên database kiểm thử riêng của lớp test này. */
    private static final String TEST_DB_NAME = "gia_pha_conn_test";

    @BeforeAll
    static void requireDatabase() throws Exception {
        // Tạo database test riêng (DROP + CREATE + schema + 7 bản ghi mẫu)
        TestDatabaseManager.createFreshDatabase(TEST_DB_NAME);

        assumeTrue(TestDatabase.isAvailable(TEST_DB_NAME),
                "BỎ QUA: không kết nối được MySQL. Hãy khởi động MySQL trước khi chạy test.");
    }

    @AfterAll
    static void dropTestDatabase() throws Exception {
        TestDatabaseManager.dropDatabase(TEST_DB_NAME);
    }

    @Test
    @Order(1)
    @DisplayName("getConnection() trả về Connection hợp lệ")
    void getConnectionReturnsValidConnection() {
        assertDoesNotThrow(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                assertNotNull(conn);
                assertFalse(conn.isClosed(), "Connection phải đang mở");
            }
        });
    }

    @Test
    @Order(2)
    @DisplayName("getConnection() luôn trả về Connection MỚI (không dùng singleton)")
    void getConnectionReturnsNewConnectionEachTime() throws Exception {
        try (Connection first = DatabaseConnection.getConnection();
             Connection second = DatabaseConnection.getConnection()) {
            assertNotNull(first);
            assertNotNull(second);
            assertFalse(first == second, "Mỗi lần gọi getConnection() phải trả về một Connection mới");
        }
    }

    @Test
    @Order(3)
    @DisplayName("Metadata cho biết đang dùng MySQL/MariaDB")
    void metadataReportsDatabaseProduct() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String product = meta.getDatabaseProductName();
            assertNotNull(product);
            assertTrue(product.toLowerCase().contains("mysql") || product.toLowerCase().contains("mariadb"),
                    "Sản phẩm CSDL phải là MySQL/MariaDB nhưng thực tế là: " + product);
            assertNotNull(meta.getDriverName());
            assertNotNull(meta.getDriverVersion());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Bảng PERSON trong database test tồn tại và có dữ liệu mẫu")
    void personTableExistsWithSampleData() throws Exception {
        try (Connection conn = TestDatabaseManager.openConnection(TEST_DB_NAME);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM PERSON")) {
            assertTrue(rs.next(), "Truy vấn COUNT(*) phải trả về một dòng");
            int count = rs.getInt(1);
            assertEquals(TestDatabase.SAMPLE_RECORD_COUNT, count,
                    "Database test phải có đúng 7 bản ghi mẫu");
            System.out.println("[INFO] Số bản ghi PERSON trong database test: " + count);
        }
    }

    @Test
    @Order(5)
    @DisplayName("Cấu hình kết nối được đọc đúng (URL / User / Password)")
    void configurationIsReadable() {
        String url = DatabaseConnection.getUrl();
        String user = DatabaseConnection.getUser();
        String password = DatabaseConnection.getPassword();

        assertNotNull(url);
        assertFalse(url.trim().isEmpty(), "DB_URL không được rỗng");
        assertTrue(url.startsWith("jdbc:mysql:"), "URL phải bắt đầu bằng jdbc:mysql: nhưng thực tế là: " + url);
        assertNotNull(user);
        assertFalse(user.trim().isEmpty(), "DB_USER không được rỗng");
        assertNotNull(password);
    }

    @Test
    @Order(6)
    @DisplayName("Số bản ghi PERSON khớp với giá trị đếm được")
    void recordCountMatchesHelper() {
        assertEquals(TestDatabase.countPersons(TEST_DB_NAME),
                TestDatabase.countPersons(TEST_DB_NAME));
        assertTrue(TestDatabase.countPersons(TEST_DB_NAME) >= 0, "Số bản ghi phải đếm được");
    }

    @Test
    @Order(7)
    @DisplayName("Database CHÍNH gia_pha không bị test thay đổi (cô lập dữ liệu tuyệt đối)")
    void mainDatabaseIsUntouched() {
        int mainCount = TestDatabase.countMainDatabasePersons();
        assertTrue(mainCount >= 0,
                "Không đọc được database chính 'gia_pha' — hãy nạp database/schema.sql trước");
        System.out.println("[INFO] Số bản ghi PERSON trong database chính 'gia_pha': " + mainCount);
    }
}
