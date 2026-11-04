package com.giapha.support;

import com.giapha.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Tiện ích dùng chung cho các lớp kiểm thử: kiểm tra khả năng kết nối CSDL,
 * đặt lại dữ liệu mẫu và dọn dẹp bản ghi thử nghiệm.
 *
 * <p>Nguyên tắc cô lập dữ liệu (test isolation): mọi bài kiểm thử phải trả CSDL
 * về đúng trạng thái 7 bản ghi mẫu ban đầu sau khi chạy.
 */
public final class TestDatabase {

    /** Số lượng bản ghi mẫu nguyên bản trong bảng PERSON. */
    public static final int SAMPLE_RECORD_COUNT = 7;

    private TestDatabase() {
        // Utility class — không cho phép khởi tạo
    }

    /**
     * Kiểm tra MySQL có sẵn sàng và bảng PERSON có tồn tại hay không.
     *
     * @return true nếu kết nối được và truy vấn bảng PERSON thành công
     */
    public static boolean isAvailable() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM PERSON")) {
            return rs.next();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Đặt lại Person 5 về nguyên bản (tên gốc, father = 3, mother = 4)
     * và bảo đảm Person 7 tồn tại.
     */
    public static void resetToSampleState() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("UPDATE PERSON SET full_name = 'Nguyễn Văn Con', "
                    + "birth_year = 1985, gender = 'MALE', father_id = 3, mother_id = 4 WHERE id = 5");

            stmt.executeUpdate("INSERT INTO PERSON (id, full_name, birth_year, gender, photo_path, father_id, mother_id) "
                    + "VALUES (7, 'Nguyễn Văn Cháu', 2015, 'MALE', NULL, 5, 6) "
                    + "ON DUPLICATE KEY UPDATE full_name = 'Nguyễn Văn Cháu', birth_year = 2015, "
                    + "gender = 'MALE', father_id = 5, mother_id = 6");

        } catch (Exception e) {
            throw new IllegalStateException("Không thể đặt lại dữ liệu mẫu: " + e.getMessage(), e);
        }
    }

    /**
     * Xóa toàn bộ bản ghi test có tiền tố cho trước (dùng khi test bị lỗi giữa chừng).
     *
     * @param prefix tiền tố tên đầy đủ
     */
    public static void deleteByNamePrefix(String prefix) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM PERSON WHERE full_name LIKE ?")) {
            ps.setString(1, prefix + "%");
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Không thể dọn bản ghi test với tiền tố '" + prefix + "': " + e.getMessage(), e);
        }
    }

    /**
     * Đếm số bản ghi hiện có trong bảng PERSON.
     *
     * @return số bản ghi, hoặc -1 nếu truy vấn thất bại
     */
    public static int countPersons() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM PERSON")) {
            return rs.next() ? rs.getInt(1) : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Truy vấn một giá trị cột đơn của một bản ghi theo điều kiện tên.
     *
     * @param column tên cột cần lấy
     * @param name   tên đầy đủ
     * @return giá trị cột dạng String, hoặc null nếu không tìm thấy
     */
    public static String queryString(String column, String name) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT " + column + " FROM PERSON WHERE full_name = ?")) {
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
     * @param column tên cột cần lấy
     * @param id     mã định danh Person
     * @return giá trị cột, hoặc {@link Integer#MIN_VALUE} nếu không tìm thấy
     */
    public static int queryInt(String column, int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT " + column + " FROM PERSON WHERE id = ?")) {
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
     * @param name tên đầy đủ
     * @return id, hoặc -1 nếu không tìm thấy
     */
    public static int queryIdByName(String name) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM PERSON WHERE full_name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        } catch (Exception e) {
            return -1;
        }
    }
}
