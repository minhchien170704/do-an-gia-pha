package com.giapha.repository;

import com.giapha.database.DatabaseConnection;
import com.giapha.model.Gender;
import com.giapha.model.Person;

import java.sql.*;
import java.util.*;

/**
 * Cài đặt JDBC cho interface PersonRepository.
 * Chịu trách nhiệm thực thi các truy vấn SQL, map ResultSet thành đối tượng Person,
 * và quản lý lưu trữ quan hệ cha/mẹ (father_id, mother_id).
 *
 * <p><b>Quyền sở hữu Connection:</b>
 * <ul>
 *     <li>Repository <b>tự mở</b> Connection thì repository <b>chịu trách nhiệm đóng</b> nó.</li>
 *     <li>Repository <b>nhận</b> Connection từ bên ngoài (constructor có tham số) thì
 *         repository <b>không đóng</b> Connection đó — quyền sở hữu thuộc về bên ngoài.</li>
 *     <li>Repository <b>không bao giờ</b> tự ý chuyển sang database khác khi Connection
 *         được cung cấp đã đóng — thay vào đó ném {@link SQLException} rõ ràng, tránh
 *         tình huống âm thầm ghi/đọc nhầm database chính.</li>
 * </ul>
 */
public class JdbcPersonRepository implements PersonRepository {

    /**
     * Connection do bên ngoài cung cấp (có thể {@code null}).
     * Nếu khác {@code null}, repository dùng connection này cho MỌI thao tác
     * và KHÔNG BAO GIỜ đóng nó.
     */
    private final Connection externalConnection;

    /**
     * Constructor mặc định. Mỗi thao tác sẽ lấy kết nối mới từ {@link DatabaseConnection}
     * và tự đóng kết nối đó khi thao tác kết thúc.
     */
    public JdbcPersonRepository() {
        this.externalConnection = null;
    }

    /**
     * Constructor nhận Connection từ ngoài (hỗ trợ dependency injection hoặc testing).
     *
     * <p><b>Quyền sở hữu:</b> connection truyền vào được coi là tài nguyên dùng chung,
     * repository chỉ MƯỢN và <b>không đóng</b> nó. Khi connection này đã đóng,
     * mọi thao tác sẽ ném {@link SQLException} thay vì im lặng kết nối sang database khác.
     *
     * @param connection kết nối JDBC dùng chung, hoặc {@code null} để repository tự quản lý
     */
    public JdbcPersonRepository(Connection connection) {
        this.externalConnection = connection;
    }

    /**
     * Lấy kết nối cơ sở dữ liệu cho một thao tác.
     *
     * <p>Nếu có connection do bên ngoài cung cấp và connection đó còn mở → trả về chính nó.
     * Nếu connection bên ngoài đã đóng → ném {@link SQLException} (không tự chuyển sang
     * database khác, tránh ghi/đọc nhầm dữ liệu).
     * Nếu không có connection bên ngoài → mở connection mới từ {@link DatabaseConnection};
     * connection này do repository quản lý và sẽ được đóng bởi {@link #closeIfOwned(Connection)}.
     *
     * @return Connection để thực hiện thao tác
     * @throws SQLException nếu không lấy được connection
     */
    protected Connection getConnection() throws SQLException {
        if (externalConnection != null) {
            if (externalConnection.isClosed()) {
                throw new SQLException("Connection được cung cấp bên ngoài đã đóng. "
                        + "Không thể tự động kết nối lại — hãy truyền một Connection còn mở.");
            }
            return externalConnection;
        }
        return DatabaseConnection.getConnection();
    }

    /**
     * Đóng Connection nếu repository sở hữu nó.
     *
     * <p>Connection do bên ngoài cung cấp được BỎ QUA — quyền đóng thuộc về bên ngoài.
     *
     * @param conn connection vừa dùng xong (có thể {@code null})
     */
    private void closeIfOwned(Connection conn) {
        if (conn == null || conn == externalConnection) {
            return;
        }
        try {
            conn.close();
        } catch (SQLException ignored) {
            // Không ném — đóng connection là thao tác dọn dẹp, không được làm hỏng thao tác chính.
        }
    }

    @Override
    public List<Person> findAll() {
        String sql = "SELECT id, full_name, birth_year, gender, photo_path, father_id, mother_id FROM person";
        List<Person> list = new ArrayList<>();
        Map<Integer, Integer> fatherIdMap = new HashMap<>();
        Map<Integer, Integer> motherIdMap = new HashMap<>();
        Map<Integer, Person> personMap = new HashMap<>();

        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Person person = mapRowBasic(rs);
                    int id = person.getId();
                    list.add(person);
                    personMap.put(id, person);

                    int fatherId = rs.getInt("father_id");
                    if (!rs.wasNull() && fatherId > 0) {
                        fatherIdMap.put(id, fatherId);
                    }

                    int motherId = rs.getInt("mother_id");
                    if (!rs.wasNull() && motherId > 0) {
                        motherIdMap.put(id, motherId);
                    }
                }
            }

            // Liên kết quan hệ cha/mẹ trong bộ nhớ cho danh sách findAll
            for (Person p : list) {
                Integer fId = fatherIdMap.get(p.getId());
                if (fId != null) {
                    p.setFather(personMap.get(fId));
                }

                Integer mId = motherIdMap.get(p.getId());
                if (mId != null) {
                    p.setMother(personMap.get(mId));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi truy vấn danh sách Person: " + e.getMessage(), e);
        } finally {
            closeIfOwned(conn);
        }

        return list;
    }

    @Override
    public Person findById(int id) {
        String sql = "SELECT p.id, p.full_name, p.birth_year, p.gender, p.photo_path, p.father_id, p.mother_id, "
                + "f.full_name AS f_name, f.birth_year AS f_birth_year, f.gender AS f_gender, f.photo_path AS f_photo_path, "
                + "m.full_name AS m_name, m.birth_year AS m_birth_year, m.gender AS m_gender, m.photo_path AS m_photo_path "
                + "FROM person p "
                + "LEFT JOIN person f ON p.father_id = f.id "
                + "LEFT JOIN person m ON p.mother_id = m.id "
                + "WHERE p.id = ?";

        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapRowWithImmediateParents(rs);
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm Person theo id = " + id + ": " + e.getMessage(), e);
        } finally {
            closeIfOwned(conn);
        }

        return null;
    }

    @Override
    public List<Person> search(String keyword) {
        String sql = "SELECT p.id, p.full_name, p.birth_year, p.gender, p.photo_path, p.father_id, p.mother_id, "
                + "f.full_name AS f_name, f.birth_year AS f_birth_year, f.gender AS f_gender, f.photo_path AS f_photo_path, "
                + "m.full_name AS m_name, m.birth_year AS m_birth_year, m.gender AS m_gender, m.photo_path AS m_photo_path "
                + "FROM person p "
                + "LEFT JOIN person f ON p.father_id = f.id "
                + "LEFT JOIN person m ON p.mother_id = m.id "
                + "WHERE p.full_name LIKE ?";

        List<Person> result = new ArrayList<>();
        String searchPattern = "%" + (keyword != null ? keyword.trim() : "") + "%";

        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, searchPattern);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.add(mapRowWithImmediateParents(rs));
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm kiếm Person theo từ khóa: " + e.getMessage(), e);
        } finally {
            closeIfOwned(conn);
        }

        return result;
    }

    @Override
    public int save(Person person) {
        String sql = "INSERT INTO person (full_name, birth_year, gender, photo_path, father_id, mother_id) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                setPersonParameters(ps, person);

                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int generatedId = keys.getInt(1);
                        person.setId(generatedId);
                        return generatedId;
                    }
                }
            }

            throw new SQLException("Thêm Person thất bại, không nhận được generated id.");

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lưu Person: " + e.getMessage(), e);
        } finally {
            closeIfOwned(conn);
        }
    }

    @Override
    public void update(Person person) {
        String sql = "UPDATE person SET full_name = ?, birth_year = ?, gender = ?, photo_path = ?, "
                + "father_id = ?, mother_id = ? WHERE id = ?";

        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {

                setPersonParameters(ps, person);
                ps.setInt(7, person.getId());

                ps.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cập nhật Person id = " + person.getId() + ": " + e.getMessage(), e);
        } finally {
            closeIfOwned(conn);
        }
    }

    @Override
    public void updatePhotoPath(int id, String photoPath) {
        String sql = "UPDATE person SET photo_path = ? WHERE id = ?";

        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (photoPath != null && !photoPath.trim().isEmpty()) {
                    ps.setString(1, photoPath.trim());
                } else {
                    ps.setNull(1, Types.VARCHAR);
                }
                ps.setInt(2, id);
                ps.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cập nhật ảnh đại diện của Person id = " + id + ": " + e.getMessage(), e);
        } finally {
            closeIfOwned(conn);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM person WHERE id = ?";

        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, id);
                ps.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi xóa Person id = " + id + ": " + e.getMessage(), e);
        } finally {
            closeIfOwned(conn);
        }
    }

    // ============================================================
    // HELPER MAPPING METHODS
    // ============================================================

    private Person mapRowBasic(ResultSet rs) throws SQLException {
        Person person = new Person();
        person.setId(rs.getInt("id"));
        person.setFullName(rs.getString("full_name"));
        person.setBirthYear(rs.getInt("birth_year"));

        String genderStr = rs.getString("gender");
        if (genderStr != null) {
            try {
                person.setGender(Gender.valueOf(genderStr.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        person.setPhotoPath(rs.getString("photo_path"));
        return person;
    }

    private Person mapRowWithImmediateParents(ResultSet rs) throws SQLException {
        Person person = mapRowBasic(rs);

        int fatherId = rs.getInt("father_id");
        if (!rs.wasNull() && fatherId > 0) {
            Person father = new Person();
            father.setId(fatherId);
            father.setFullName(rs.getString("f_name"));
            father.setBirthYear(rs.getInt("f_birth_year"));
            String fGenderStr = rs.getString("f_gender");
            if (fGenderStr != null) {
                try {
                    father.setGender(Gender.valueOf(fGenderStr.trim().toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                }
            }
            father.setPhotoPath(rs.getString("f_photo_path"));
            person.setFather(father);
        }

        int motherId = rs.getInt("mother_id");
        if (!rs.wasNull() && motherId > 0) {
            Person mother = new Person();
            mother.setId(motherId);
            mother.setFullName(rs.getString("m_name"));
            mother.setBirthYear(rs.getInt("m_birth_year"));
            String mGenderStr = rs.getString("m_gender");
            if (mGenderStr != null) {
                try {
                    mother.setGender(Gender.valueOf(mGenderStr.trim().toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                }
            }
            mother.setPhotoPath(rs.getString("m_photo_path"));
            person.setMother(mother);
        }

        return person;
    }

    private void setPersonParameters(PreparedStatement ps, Person person) throws SQLException {
        ps.setString(1, person.getFullName());
        ps.setInt(2, person.getBirthYear());
        ps.setString(3, person.getGender() != null ? person.getGender().name() : null);
        ps.setString(4, person.getPhotoPath());

        if (person.getFather() != null && person.getFather().getId() > 0) {
            ps.setInt(5, person.getFather().getId());
        } else {
            ps.setNull(5, Types.INTEGER);
        }

        if (person.getMother() != null && person.getMother().getId() > 0) {
            ps.setInt(6, person.getMother().getId());
        } else {
            ps.setNull(6, Types.INTEGER);
        }
    }
}
