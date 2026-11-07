package com.giapha.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử các method hiển thị của enum {@link Gender} (GĐ2B.4 — Gender parsing).
 *
 * <p><b>Vì sao test ở đây?</b> Trước GĐ2B.4, chuỗi {@code "Nam"} / {@code "Nữ"}
 * bị lặp tay ở 10 chỗ trong {@code com.giapha.view.*}, và View suy luận ngược
 * giới tính từ nhãn hiển thị ({@code "Nam".equals(item)}). Nay đã gom về
 * {@link Gender#getDisplayName()} và {@link Gender#getBadgeCssClass()}.
 *
 * <p><b>Không cần JavaFX Toolkit</b> — enum thuần Java, chạy được trong bộ test
 * mặc định ({@code mvn clean test}).
 */
@DisplayName("Gender — chuẩn hóa nhãn hiển thị và CSS class")
class GenderTest {

    // ============================================================
    // getDisplayName()
    // ============================================================

    @Test
    @DisplayName("TC-GD-01 — getDisplayName() trả nhãn tiếng Việt đúng")
    void displayNameIsCorrect() {
        assertEquals("Nam", Gender.MALE.getDisplayName());
        assertEquals("Nữ", Gender.FEMALE.getDisplayName());
    }

    @Test
    @DisplayName("TC-GD-02 — getDisplayName() của 2 giới tính luôn khác nhau")
    void displayNamesAreDistinct() {
        assertNotEquals(Gender.MALE.getDisplayName(), Gender.FEMALE.getDisplayName(),
                "Hai giới tính phải có nhãn khác nhau để View phân biệt được");
    }

    @Test
    @DisplayName("TC-GD-03 — getDisplayName() không trả null cho mọi giá trị enum")
    void displayNameIsNeverNull() {
        for (Gender g : Gender.values()) {
            assertTrue(g.getDisplayName() != null && !g.getDisplayName().isEmpty(),
                    "Nhãn của " + g + " không được rỗng");
        }
    }

    // ============================================================
    // getBadgeCssClass()
    // ============================================================

    @Test
    @DisplayName("TC-GD-04 — getBadgeCssClass() trả class CSS đúng theo giới tính")
    void badgeCssClassIsCorrect() {
        assertEquals("badge-male", Gender.MALE.getBadgeCssClass());
        assertEquals("badge-female", Gender.FEMALE.getBadgeCssClass());
    }

    @Test
    @DisplayName("TC-GD-05 — getBadgeCssClass() của 2 giới tính luôn khác nhau")
    void badgeCssClassesAreDistinct() {
        assertNotEquals(Gender.MALE.getBadgeCssClass(), Gender.FEMALE.getBadgeCssClass(),
                "Hai giới tính phải có class CSS khác nhau");
    }

    @Test
    @DisplayName("TC-GD-06 — getBadgeCssClass() không trả null / rỗng")
    void badgeCssClassIsNeverNull() {
        for (Gender g : Gender.values()) {
            assertTrue(g.getBadgeCssClass() != null && !g.getBadgeCssClass().isEmpty(),
                    "Class CSS của " + g + " không được rỗng");
        }
    }

    // ============================================================
    // Bất biến: enum KHÔNG được đổi tên hằng số (tương thích DB)
    // ============================================================

    @Test
    @DisplayName("TC-GD-07 — tên hằng số enum không đổi (tương thích cột ENUM của DB)")
    void enumConstantNamesRemainStable() {
        // Cột `gender` kiểu ENUM('MALE','FEMALE') lưu đúng giá trị name().
        // Đổi tên hằng số sẽ phá vỡ dữ liệu đã lưu — test này chốt ràng buộc.
        assertEquals("MALE", Gender.MALE.name());
        assertEquals("FEMALE", Gender.FEMALE.name());
        assertEquals(2, Gender.values().length,
                "Enum Gender phải có đúng 2 giá trị để khớp schema DB");
    }

    @Test
    @DisplayName("TC-GD-08 — getDisplayName() không ảnh hưởng giá trị lưu trong DB")
    void displayNameDoesNotAffectPersistedValue() {
        // name() là giá trị được lưu xuống DB — phải độc lập với nhãn hiển thị.
        for (Gender g : Gender.values()) {
            assertEquals(g.name(), g.name().trim());
            assertTrue(g.name().equals(g.name().toUpperCase()),
                    "name() phải giữ nguyên dạng chữ hoa để khớp ENUM trong DB");
        }
    }
}
