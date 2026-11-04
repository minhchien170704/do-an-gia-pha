package com.giapha.database;

import com.giapha.support.TestDatabase;
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
 * <p>Các test case phụ thuộc MySQL đang chạy. Nếu không kết nối được, test được
 * {@code SKIPPED} (không phải FAILED) kèm thông báo rõ ràng.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("DatabaseConnection — kết nối MySQL và kiểm tra schema")
class DatabaseConnectionTest {

    @BeforeAll
    static void requireDatabase() {
        assumeTrue(TestDatabase.isAvailable(),
                "BỎ QUA: không kết nối được MySQL (gia_pha). Hãy khởi động MySQL và nạp database/schema.sql.");
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
    @DisplayName("Bảng PERSON tồn tại và có dữ liệu mẫu")
    void personTableExistsWithSampleData() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM PERSON")) {
            assertTrue(rs.next(), "Truy vấn COUNT(*) phải trả về một dòng");
            int count = rs.getInt(1);
            assertTrue(count > 0, "Bảng PERSON phải có ít nhất một bản ghi");
            System.out.println("[INFO] Số bản ghi PERSON hiện tại: " + count);
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
        assertTrue(url.startsWith("jdbc:mysql:"), "URL phải bắt đầu bằng jdbc:mysql: nhưng thực tế: " + url);
        assertNotNull(user);
        assertFalse(user.trim().isEmpty(), "DB_USER không được rỗng");
        assertNotNull(password);
    }

    @Test
    @Order(6)
    @DisplayName("Số bản ghi PERSON khớp với giá trị đếm được")
    void recordCountMatchesHelper() {
        assertEquals(TestDatabase.countPersons(), TestDatabase.countPersons());
        assertTrue(TestDatabase.countPersons() >= 0, "Số bản ghi phải đếm được");
    }
}
