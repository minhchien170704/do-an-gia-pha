package com.giapha.support;

import com.giapha.database.DatabaseConnection;
import com.giapha.repository.JdbcPersonRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

/**
 * Tiện ích dùng chung để QUẢN LÝ CSDL RIÊNG cho các lớp kiểm thử.
 *
 * <p><b>Mục tiêu — cô lập dữ liệu tuyệt đối:</b> mọi lớp test có thay đổi dữ liệu
 * đều phải dùng database riêng có tên kết thúc bằng {@value #REQUIRED_DB_SUFFIX}
 * (ví dụ {@code gia_pha_service_test}). Database chính {@code gia_pha} — chứa dữ
 * liệu thật của người dùng — <b>không bao giờ</b> bị test ghi vào.
 *
 * <p><b>Vòng đời một database test:</b>
 * <ol>
 *     <li>{@link #createFreshDatabase(String)} — {@code DROP} + {@code CREATE} + tạo bảng
 *         {@code person} (schema giống hệt {@code database/schema.sql}) + nạp 7 bản ghi mẫu.
 *         Được gọi ở {@code @BeforeAll} của mỗi lớp test → mỗi lần chạy đều sạch hoàn toàn.</li>
 *     <li>{@link #dropDatabase(String)} — xóa database test. Được gọi ở {@code @AfterAll}.
 *         Có thể bỏ qua bằng cờ {@code -Dkeep.test.db=true}.</li>
 * </ol>
 *
 * <p><b>Ràng buộc an toàn:</b> trước khi {@code DROP}, tên database bắt buộc phải
 * kết thúc bằng {@value #REQUIRED_DB_SUFFIX}. Nếu không khớp, {@link #assertSafeToDrop(String)}
 * ném {@link IllegalStateException} và KHÔNG thực hiện {@code DROP}.
 *
 * <p><b>Cấu hình:</b>
 * <ul>
 *     <li>{@code -Dkeep.test.db=true} — giữ lại database test sau khi chạy (để debug).</li>
 *     <li>{@code DB_URL} / {@code DB_USER} / {@code DB_PASSWORD} — thông tin kết nối MySQL
 *         (dùng chung với ứng dụng). Chỉ tên database được thay thế, host/port giữ nguyên.</li>
 * </ul>
 */
public final class TestDatabaseManager {

    /**
     * Hậu tố bắt buộc của database test.
     * <p>Chỉ database có tên kết thúc bằng hậu tố này mới được phép {@code DROP}.
     */
    public static final String REQUIRED_DB_SUFFIX = "_test";

    /**
     * System property cho phép giữ lại database test sau khi chạy xong.
     * <p>Đặt bằng {@code -Dkeep.test.db=true} (mặc định {@code false} → tự {@code DROP}).
     */
    public static final String KEEP_DB_PROPERTY = "keep.test.db";

    /** Đường dẫn tới script SQL dữ liệu mẫu mở rộng (30 người / 5 thế hệ). */
    public static final Path SAMPLE_SQL = Paths.get("test-data", "sample-data.sql");

    private TestDatabaseManager() {
        // Utility class — không cho phép khởi tạo
    }

    // ============================================================
    // RÀNG BUỘC AN TOÀN
    // ============================================================

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
    public static void assertSafeToDrop(String dbName) {
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
    public static boolean shouldKeepTestDatabase() {
        return Boolean.parseBoolean(System.getProperty(KEEP_DB_PROPERTY, "false"));
    }

    // ============================================================
    // QUẢN LÝ VÒNG ĐỜI DATABASE TEST
    // ============================================================

    /**
     * Tạo mới hoàn toàn database test: DROP (nếu có) → CREATE → tạo bảng {@code person}
     * → nạp 7 bản ghi mẫu nguyên bản.
     *
     * <p>Được gọi ở {@code @BeforeAll} của mỗi lớp test để bảo đảm mỗi lần chạy
     * đều bắt đầu từ trạng thái sạch, giống hệt sau khi nạp {@code database/schema.sql}.
     *
     * @param dbName tên database test (bắt buộc kết thúc bằng {@value #REQUIRED_DB_SUFFIX})
     * @throws SQLException nếu thao tác SQL thất bại
     */
    public static void createFreshDatabase(String dbName) throws SQLException {
        assertSafeToDrop(dbName);

        try (Connection conn = openRootConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP DATABASE IF EXISTS " + dbName);
            stmt.executeUpdate("CREATE DATABASE " + dbName
                    + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }

        createPersonSchema(dbName);
        insertSampleRecords(dbName);
    }

    /**
     * Xóa database test, sau khi kiểm tra an toàn tên.
     *
     * <p>Nếu {@code -Dkeep.test.db=true} được bật → giữ lại database và chỉ in thông báo.
     *
     * @param dbName tên database test cần xóa
     * @throws SQLException nếu thao tác SQL thất bại
     */
    public static void dropDatabase(String dbName) throws SQLException {
        if (shouldKeepTestDatabase()) {
            System.out.println("[TestDatabaseManager] Giữ lại database test \"" + dbName
                    + "\" (do -D" + KEEP_DB_PROPERTY + "=true).");
            return;
        }

        try {
            // Kiểm tra an toàn — ném IllegalStateException nếu tên DB không kết thúc bằng "_test"
            assertSafeToDrop(dbName);

            try (Connection conn = openRootConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DROP DATABASE IF EXISTS " + dbName);
            }
            System.out.println("[TestDatabaseManager] Đã DROP database test \"" + dbName + "\".");
        } catch (IllegalStateException e) {
            // Tên DB không an toàn → báo lỗi rõ ràng, KHÔNG DROP
            System.err.println("[TestDatabaseManager] LỖI AN TOÀN: " + e.getMessage());
            throw e;
        }
    }

    // ============================================================
    // KẾT NỐI
    // ============================================================

    /**
     * Mở kết nối ở mức server (không chỉ định database) — dùng để CREATE/DROP DATABASE.
     *
     * @return Connection mới (caller chịu trách nhiệm đóng)
     * @throws SQLException nếu kết nối thất bại
     */
    public static Connection openRootConnection() throws SQLException {
        return DriverManager.getConnection(serverLevelUrl(),
                DatabaseConnection.getUser(), DatabaseConnection.getPassword());
    }

    /**
     * Tạo JDBC URL cho database test, giữ nguyên host/port/cơ chế của {@code DB_URL}
     * hiện hành (nếu có).
     *
     * @param dbName tên database test
     * @return JDBC URL đầy đủ
     */
    public static String jdbcUrl(String dbName) {
        String configured = DatabaseConnection.getUrl();

        // Lấy phần "jdbc:mysql://host:port" từ DB_URL hiện hành để giữ nguyên cấu hình
        String prefix = "jdbc:mysql://localhost:3306";
        int schemeEnd = configured.indexOf("//");
        if (schemeEnd > 0) {
            int pathStart = configured.indexOf('/', schemeEnd + 2);
            if (pathStart > 0) {
                prefix = configured.substring(0, pathStart);
            }
        }

        return prefix + "/" + dbName
                + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    }

    /**
     * JDBC URL ở mức server (không chỉ định database) — dùng để CREATE/DROP DATABASE.
     *
     * @return JDBC URL mức server
     */
    public static String serverLevelUrl() {
        String configured = DatabaseConnection.getUrl();
        String prefix = "jdbc:mysql://localhost:3306";
        int schemeEnd = configured.indexOf("//");
        if (schemeEnd > 0) {
            int pathStart = configured.indexOf('/', schemeEnd + 2);
            if (pathStart > 0) {
                prefix = configured.substring(0, pathStart);
            }
        }
        return prefix + "/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    }

    /**
     * Mở kết nối tới database test cụ thể.
     *
     * @param dbName tên database test
     * @return Connection mới (caller chịu trách nhiệm đóng)
     * @throws SQLException nếu kết nối thất bại
     */
    public static Connection openConnection(String dbName) throws SQLException {
        return DatabaseConnection.getConnection(jdbcUrl(dbName),
                DatabaseConnection.getUser(), DatabaseConnection.getPassword());
    }

    /**
     * Tạo repository trỏ tới database test (mỗi lần gọi tạo Connection mới).
     *
     * @param dbName tên database test
     * @return {@link TestPersonRepository} đã cấu hình đúng database test
     */
    public static TestPersonRepository repositoryFor(String dbName) {
        return new TestPersonRepository(dbName);
    }

    // ============================================================
    // SCHEMA & DỮ LIỆU MẪU
    // ============================================================

    /**
     * Tạo bảng {@code person} + index + trigger, giống hệt {@code database/schema.sql}.
     *
     * @param dbName tên database test
     * @throws SQLException nếu thao tác SQL thất bại
     */
    private static void createPersonSchema(String dbName) throws SQLException {
        try (Connection conn = openConnection(dbName);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                    CREATE TABLE person (
                        id          INT AUTO_INCREMENT PRIMARY KEY,
                        full_name   VARCHAR(100) NOT NULL,
                        birth_year  INT NOT NULL,
                        gender      ENUM('MALE','FEMALE') NOT NULL,
                        photo_path  VARCHAR(255) NULL,
                        father_id   INT NULL,
                        mother_id   INT NULL,
                        CONSTRAINT fk_father FOREIGN KEY (father_id) REFERENCES person(id) ON DELETE RESTRICT,
                        CONSTRAINT fk_mother FOREIGN KEY (mother_id) REFERENCES person(id) ON DELETE RESTRICT,
                        CONSTRAINT chk_birth_year CHECK (birth_year >= 1000 AND birth_year <= 2100),
                        CONSTRAINT chk_father_not_mother CHECK (
                            father_id IS NULL OR mother_id IS NULL OR father_id <> mother_id)
                    )
                    """);
            stmt.executeUpdate("CREATE INDEX idx_person_name ON person(full_name)");
            stmt.executeUpdate("""
                    CREATE TRIGGER trg_check_not_self_parent_update
                    BEFORE UPDATE ON person
                    FOR EACH ROW
                    BEGIN
                        IF (NEW.father_id IS NOT NULL AND NEW.father_id = NEW.id) OR
                           (NEW.mother_id IS NOT NULL AND NEW.mother_id = NEW.id) THEN
                            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Một người không thể là cha hoặc mẹ của chính mình';
                        END IF;
                    END
                    """);
        }
    }

    /**
     * Nạp 7 bản ghi mẫu nguyên bản (giống phần cuối {@code database/schema.sql}).
     *
     * @param dbName tên database test
     * @throws SQLException nếu thao tác SQL thất bại
     */
    private static void insertSampleRecords(String dbName) throws SQLException {
        try (Connection conn = openConnection(dbName);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                    INSERT INTO person (id, full_name, birth_year, gender, photo_path, father_id, mother_id)
                    VALUES
                    (1, 'Nguyễn Văn Tổ', 1930, 'MALE', NULL, NULL, NULL),
                    (2, 'Trần Thị Tổ', 1932, 'FEMALE', NULL, NULL, NULL),
                    (3, 'Nguyễn Văn Cha', 1955, 'MALE', NULL, 1, 2),
                    (4, 'Lê Thị Mẹ', 1958, 'FEMALE', NULL, NULL, NULL),
                    (5, 'Nguyễn Văn Con', 1985, 'MALE', NULL, 3, 4),
                    (6, 'Phạm Thị Dâu', 1987, 'FEMALE', NULL, NULL, NULL),
                    (7, 'Nguyễn Văn Cháu', 2015, 'MALE', NULL, 5, 6)
                    """);
        }
    }

    /**
     * Nạp bộ dữ liệu mẫu mở rộng từ {@code test-data/sample-data.sql} (30 người / 5 thế hệ).
     *
     * <p>Câu lệnh {@code USE gia_pha;} trong script được thay bằng database test đích.
     *
     * @param dbName tên database test đích
     * @throws IOException  nếu không đọc được file script
     * @throws SQLException nếu thao tác SQL thất bại
     */
    public static void loadExtendedSampleData(String dbName) throws IOException, SQLException {
        String script = Files.readString(SAMPLE_SQL, StandardCharsets.UTF_8);
        script = script.replace("USE gia_pha;", "USE " + dbName + ";");

        // Bỏ comment dòng để tách câu lệnh
        StringBuilder cleaned = new StringBuilder();
        for (String line : script.split("\r?\n")) {
            if (line.trim().startsWith("--")) {
                cleaned.append('\n');
            } else {
                cleaned.append(line).append('\n');
            }
        }

        try (Connection conn = openConnection(dbName);
             Statement stmt = conn.createStatement()) {

            for (String rawStatement : cleaned.toString().split(";")) {
                String sql = rawStatement.trim();
                if (sql.isEmpty()) {
                    continue;
                }
                // Bỏ lệnh USE (đã trỏ đúng database) và lệnh ALTER AUTO_INCREMENT không cần thiết
                if (sql.toUpperCase(Locale.ROOT).startsWith("USE ")) {
                    continue;
                }
                stmt.execute(sql);
            }
        }
    }
}
