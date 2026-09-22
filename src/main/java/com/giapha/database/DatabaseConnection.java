package com.giapha.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Lớp chịu trách nhiệm khởi tạo kết nối JDBC tới cơ sở dữ liệu MySQL.
 * Tuân thủ đúng UML Class Diagram và các yêu cầu kỹ thuật:
 * - Trả về một Connection mới mỗi lần gọi getConnection().
 * - Không duy trì Connection dạng static dài hạn (không dùng connection singleton).
 * - Cấu hình linh hoạt qua biến môi trường hoặc system properties, không hard-code password.
 */
public class DatabaseConnection {

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/gia_pha?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    /**
     * Private constructor để ngăn chặn việc khởi tạo đối tượng (utility class).
     */
    private DatabaseConnection() {
    }

    /**
     * Lấy URL kết nối cơ sở dữ liệu.
     * Ưu tiên System property -> Environment variable -> Giá trị mặc định.
     */
    public static String getUrl() {
        String url = System.getProperty("DB_URL");
        if (url == null || url.trim().isEmpty()) {
            url = System.getenv("DB_URL");
        }
        return (url != null && !url.trim().isEmpty()) ? url.trim() : DEFAULT_URL;
    }

    /**
     * Lấy Username kết nối cơ sở dữ liệu.
     * Ưu tiên System property -> Environment variable -> Giá trị mặc định.
     */
    public static String getUser() {
        String user = System.getProperty("DB_USER");
        if (user == null || user.trim().isEmpty()) {
            user = System.getenv("DB_USER");
        }
        return (user != null && !user.trim().isEmpty()) ? user.trim() : DEFAULT_USER;
    }

    /**
     * Lấy Password kết nối cơ sở dữ liệu.
     * Ưu tiên System property -> Environment variable -> Giá trị mặc định.
     */
    public static String getPassword() {
        String pass = System.getProperty("DB_PASSWORD");
        if (pass == null) {
            pass = System.getenv("DB_PASSWORD");
        }
        return (pass != null) ? pass : DEFAULT_PASSWORD;
    }

    /**
     * Tạo và trả về một Connection mới tới MySQL Database.
     * Caller (như Repository) có trách nhiệm đóng kết nối thông qua try-with-resources.
     *
     * @return đối tượng Connection mới
     * @throws SQLException nếu kết nối thất bại
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getUrl(), getUser(), getPassword());
    }

    /**
     * Tạo và trả về một Connection mới với thông tin cấu hình chỉ định.
     *
     * @param url      đường dẫn JDBC
     * @param user     tên đăng nhập
     * @param password mật khẩu
     * @return đối tượng Connection mới
     * @throws SQLException nếu kết nối thất bại
     */
    public static Connection getConnection(String url, String user, String password) throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
