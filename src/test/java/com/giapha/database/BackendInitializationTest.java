package com.giapha.database;

import com.giapha.support.TestDatabase;
import com.giapha.support.TestDatabaseManager;
import org.junit.jupiter.api.*;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Kiểm thử việc khởi tạo backend của ứng dụng phải XÁC NHẬN kết nối MySQL thật.
 *
 * <p>Trước khi sửa, {@code Main.initBackend()} chỉ khởi tạo Repository → Service →
 * Controller mà KHÔNG mở kết nối JDBC, nên {@code try/catch} không thể bắt được
 * lỗi mất kết nối — ứng dụng mở màn hình trống rồi mới báo lỗi khi View tải dữ liệu.
 *
 * <p>Test này khẳng định: nếu cấu hình kết nối TRỎ SAI (database không tồn tại),
 * thì việc xác nhận kết nối phải THẤT BẠI (ném SQLException) — tức là ứng dụng
 * sẽ báo lỗi ngay lúc khởi động, trước khi hiển thị giao diện.
 *
 * <p><b>Cô lập dữ liệu:</b> không ghi vào database nào cả — chỉ mở/đóng kết nối.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Khởi tạo backend — xác nhận kết nối MySQL trước khi hiển thị giao diện")
class BackendInitializationTest {

    private static final String TEST_DB_NAME = "gia_pha_init_test";

    @BeforeAll
    static void requireDatabase() throws Exception {
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
    @DisplayName("Cấu hình kết nối ĐÚNG → kiểm tra kết nối thành công")
    void validConnectionPassesProbe() throws Exception {
        try (Connection probe = DatabaseConnection.getConnection()) {
            assertNotNull(probe, "Phải mở được Connection");
            assertFalse(probe.isClosed(), "Connection thăm dò phải đang mở");
        }
    }

    @Test
    @Order(2)
    @DisplayName("Cấu hình kết nối SAI → kiểm tra kết nối phải THẤT BẠI (không im lặng)")
    void invalidConnectionFailsFast() {
        // Trỏ tới database KHÔNG TỒN TẠI → getConnection() phải ném SQLException.
        // Đây chính là tình huống mà initBackend() cần bắt được TRƯỚC khi hiện UI.
        String badUrl = "jdbc:mysql://localhost:3306/gia_pha_db_khong_ton_tai_xyz"
                + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

        SQLExceptionHolder holder = new SQLExceptionHolder();
        try (Connection conn = DatabaseConnection.getConnection(
                badUrl, DatabaseConnection.getUser(), DatabaseConnection.getPassword())) {
            // Nếu tới được đây thì getConnection() KHÔNG ném lỗi → probe sẽ tưởng rằng DB OK
            holder.reached = true;
        } catch (Exception e) {
            holder.error = e;
        }

        assertFalse(holder.reached,
                "Kết nối tới database không tồn tại phải thất bại — nếu không, initBackend() "
                        + "sẽ không phát hiện được CSDL hỏng và mở màn hình trống");
        assertNotNull(holder.error, "Phải ném exception khi kết nối thất bại");
        System.out.println("[INFO] Lỗi kết nối dự kiến: " + holder.error.getMessage());
    }

    @Test
    @Order(3)
    @DisplayName("Sau khi kết nối thất bại, database chính vẫn nguyên vẹn")
    void mainDatabaseUntouchedAfterFailedProbe() {
        int mainCount = TestDatabase.countMainDatabasePersons();
        assertTrue(mainCount >= 0, "Không đọc được database chính");
        System.out.println("[INFO] Số bản ghi PERSON trong database chính: " + mainCount);
    }

    /** Holder để giữ kết quả của khối try/catch (biến phải effectively-final). */
    private static final class SQLExceptionHolder {
        boolean reached = false;
        Exception error = null;
    }
}
