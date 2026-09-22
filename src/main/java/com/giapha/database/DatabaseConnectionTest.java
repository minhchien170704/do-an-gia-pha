package com.giapha.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Lớp kiểm tra kết nối cơ sở dữ liệu MySQL và kiểm tra trạng thái schema/sample data.
 * Có thể chạy trực tiếp từ dòng lệnh hoặc IDE.
 */
public class DatabaseConnectionTest {

    public static void main(String[] args) {
        System.out.println("=== KIỂM TRA KẾT NỐI DATABASE MYSQL ===");
        System.out.println("URL      : " + DatabaseConnection.getUrl());
        System.out.println("User     : " + DatabaseConnection.getUser());
        System.out.println("Password : " + (DatabaseConnection.getPassword().isEmpty() ? "(trống)" : "********"));

        try (Connection conn = DatabaseConnection.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("[SUCCESS] Kết nối MySQL thành công!");
            System.out.println("DBMS     : " + meta.getDatabaseProductName() + " v" + meta.getDatabaseProductVersion());
            System.out.println("Driver   : " + meta.getDriverName() + " v" + meta.getDriverVersion());

            // Kiểm tra schema và bảng PERSON
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT DATABASE()")) {
                    if (rs.next()) {
                        System.out.println("Database : " + rs.getString(1));
                    }
                }

                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM PERSON")) {
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        System.out.println("Bảng PERSON tồn tại. Số bản ghi hiện có: " + count);
                        if (count == 7) {
                            System.out.println("[SUCCESS] Sample data nguyên bản (7 records) đã sẵn sàng!");
                        }
                    }
                } catch (SQLException e) {
                    System.out.println("[WARNING] Kết nối được DB nhưng bảng PERSON chưa được tạo. Vui lòng chạy file database/schema.sql.");
                }
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] Kết nối tới MySQL thất bại!");
            System.err.println("Mã lỗi  : " + e.getErrorCode());
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("Chi tiết: " + e.getMessage());
            System.err.println("\n--- HƯỚNG DẪN KHẮC PHỤC ---");
            System.err.println("1. Đảm bảo MySQL Server đang chạy trên cổng 3306.");
            System.err.println("2. Đảm bảo Database 'gia_pha' đã được tạo bằng file database/schema.sql.");
            System.err.println("3. Nếu MySQL dùng mật khẩu khác, hãy cấu hình qua biến môi trường hoặc system properties:");
            System.err.println("   -DDB_URL=\"...\" -DDB_USER=\"...\" -DDB_PASSWORD=\"...\"");
        }
    }
}
