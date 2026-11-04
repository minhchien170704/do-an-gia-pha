package com.giapha.support;

import com.giapha.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Repository trỏ tới một database KIỂM THỬ cụ thể.
 *
 * <p>Ghi đè {@code getConnection()} để mọi truy vấn đi tới database test thay vì
 * database chính. Đây là cơ chế cô lập dữ liệu ở tầng Data Access: kể cả khi
 * lớp test vô tình gọi {@code new JdbcPersonRepository()} thì vẫn không chạm
 * vào dữ liệu thật.
 *
 * @see TestDatabaseManager
 */
public class TestPersonRepository extends com.giapha.repository.JdbcPersonRepository {

    private final String testDbName;

    /**
     * @param testDbName tên database kiểm thử (bắt buộc kết thúc bằng {@code _test})
     */
    public TestPersonRepository(String testDbName) {
        // Gọi super() không tham số — Connection sẽ được tạo lazily trong getConnection()
        this.testDbName = testDbName;
    }

    /**
     * @return tên database kiểm thử mà repository này trỏ tới
     */
    public String getTestDbName() {
        return testDbName;
    }

    @Override
    protected Connection getConnection() throws SQLException {
        return TestDatabaseManager.openConnection(testDbName);
    }
}
