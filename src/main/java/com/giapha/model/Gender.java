package com.giapha.model;

/**
 * Giới tính của một người trong gia phả.
 *
 * <p><b>Lưu ý về tên hằng số:</b> giá trị {@code MALE} / {@code FEMALE} được lưu
 * thẳng vào cột {@code gender} (kiểu {@code ENUM}) của bảng {@code person}.
 * Vì vậy <b>không được</b> đổi tên các hằng số này — chỉ có thể bổ sung
 * method hỗ trợ hiển thị.
 */
public enum Gender {
    MALE,
    FEMALE;

    /**
     * Nhãn hiển thị tiếng Việt của giới tính.
     *
     * <p>Dùng để thống nhất toàn bộ văn bản hiển thị trong giao diện, tránh
     * việc lặp chuỗi {@code "Nam"} / {@code "Nữ"} ở nhiều nơi (dễ sai khi đổi nhãn).
     *
     * @return {@code "Nam"} nếu là nam, {@code "Nữ"} nếu là nữ
     */
    public String getDisplayName() {
        return this == MALE ? "Nam" : "Nữ";
    }

    /**
     * CSS class dùng cho badge hiển thị giới tính trong giao diện.
     *
     * <p>Tồn tại để View <b>không phải suy luận ngược</b> giới tính từ nhãn
     * hiển thị (ví dụ so sánh chuỗi {@code "Nam".equals(item)}) — cách làm đó
     * rất dễ vỡ khi đổi nhãn hoặc đổi ngôn ngữ.
     *
     * @return {@code "badge-male"} nếu là nam, {@code "badge-female"} nếu là nữ
     */
    public String getBadgeCssClass() {
        return this == MALE ? "badge-male" : "badge-female";
    }
}
