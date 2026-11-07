package com.giapha.support;

import com.giapha.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Tiện ích dùng chung cho các lớp kiểm thử: đếm bản ghi, truy vấn giá trị và
 * dọn bản ghi thử nghiệm trong database KIỂM THỬ.
 *
 * <p><b>Cô lập dữ liệu:</b> mọi phương thức ở đây đều nhận {@code dbName} làm tham số
 * và chỉ truy vấn database có tên kết thúc bằng {@code _test}. Database chính
 * {@code gia_pha} (dữ liệu thật của người dùng) <b>không bao giờ</b> bị đụng tới.
 *
 * <p>Việc tạo/xóa database test do {@link TestDatabaseManager} đảm nhiệm.
 *
 * @see TestDatabaseManager
 */
public final class TestDatabase {

    /** Số lượng bản ghi mẫu nguyên bản trong bảng person. */
    public static final int SAMPLE_RECORD_COUNT = 7;

    private TestDatabase() {
        // Utility class — không cho phép khởi tạo
    }

    /**
     * Kiểm tra database kiểm thử có sẵn sàng và bảng person có tồn tại hay không.
     *
     * @param dbName tên database kiểm thử
     * @return true nếu kết nối được và truy vấn bảng person thành công
     */
    public static boolean isAvailable(String dbName) {
        try (Connection conn = TestDatabaseManager.openConnection(dbName);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM person")) {
            return rs.next();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Đặt lại database kiểm thử về trạng thái 7 bản ghi mẫu nguyên bản.
     *
     * @param dbName tên database kiểm thử
     */
    public static void resetToSampleState(String dbName) {
        try (Connection conn = TestDatabaseManager.openConnection(dbName);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("UPDATE person SET full_name = 'Nguyễn Văn Con', "
                    + "birth_year = 1985, gender = 'MALE', father_id = 3, mother_id = 4 WHERE id = 5");

            stmt.executeUpdate("INSERT INTO person (id, full_name, birth_year, gender, photo_path, father_id, mother_id) "
                    + "VALUES (7, 'Nguyễn Văn Cháu', 2015, 'MALE', NULL, 5, 6) "
                    + "ON DUPLICATE KEY UPDATE full_name = 'Nguyễn Văn Cháu', birth_year = 2015, "
                    + "gender = 'MALE', father_id = 5, mother_id = 6");

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Không thể đặt lại dữ liệu mẫu trong \"" + dbName + "\": " + e.getMessage(), e);
        }
    }

    /**
     * Xóa toàn bộ bản ghi test có tiền tố cho trước.
     *
     * @param dbName tên database kiểm thử
     * @param prefix tiền tố tên đầy đủ
     */
    public static void deleteByNamePrefix(String dbName, String prefix) {
        try (Connection conn = TestDatabaseManager.openConnection(dbName);
             PreparedStatement ps = conn.prepareStatement("DELETE FROM person WHERE full_name LIKE ?")) {
            ps.setString(1, prefix + "%");
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Không thể dọn bản ghi test với tiền tố '" + prefix + "': " + e.getMessage(), e);
        }
    }

    /**
     * Đếm số bản ghi hiện có trong bảng person của database kiểm thử.
     *
     * @param dbName tên database kiểm thử
     * @return số bản ghi, hoặc -1 nếu truy vấn thất bại
     */
    public static int countPersons(String dbName) {
        try (Connection conn = TestDatabaseManager.openConnection(dbName);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM person")) {
            return rs.next() ? rs.getInt(1) : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Truy vấn một giá trị cột đơn của một bản ghi theo điều kiện tên.
     *
     * @param dbName tên database kiểm thử
     * @param column tên cột cần lấy
     * @param name   tên đầy đủ
     * @return giá trị cột dạng String, hoặc null nếu không tìm thấy
     */
    public static String queryString(String dbName, String column, String name) {
        try (Connection conn = TestDatabaseManager.openConnection(dbName);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT " + column + " FROM person WHERE full_name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Truy vấn số nguyên của một cột theo id.
     *
     * @param dbName tên database kiểm thử
     * @param column tên cột cần lấy
     * @param id     mã định danh Person
     * @return giá trị cột, hoặc {@link Integer#MIN_VALUE} nếu không tìm thấy
     */
    public static int queryInt(String dbName, String column, int id) {
        try (Connection conn = TestDatabaseManager.openConnection(dbName);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT " + column + " FROM person WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : Integer.MIN_VALUE;
            }
        } catch (Exception e) {
            return Integer.MIN_VALUE;
        }
    }

    /**
     * Truy vấn id của một Person theo tên đầy đủ.
     *
     * @param dbName tên database kiểm thử
     * @param name   tên đầy đủ
     * @return id, hoặc -1 nếu không tìm thấy
     */
    public static int queryIdByName(String dbName, String name) {
        try (Connection conn = TestDatabaseManager.openConnection(dbName);
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM person WHERE full_name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Kiểm tra database CHÍNH {@code gia_pha} có tồn tại và truy vấn được hay không.
     *
     * <p>Chỉ dùng để <b>kiểm chứng</b> rằng dữ liệu chính còn nguyên vẹn sau khi
     * chạy test — KHÔNG dùng để ghi dữ liệu.
     *
     * @return số bản ghi trong bảng person của database chính, hoặc -1 nếu không truy vấn được
     */
    public static int countMainDatabasePersons() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM person")) {
            return rs.next() ? rs.getInt(1) : -1;
        } catch (Exception e) {
            return -1;
        }
    }
}
