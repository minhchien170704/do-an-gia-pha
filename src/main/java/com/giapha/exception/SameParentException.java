package com.giapha.exception;

/**
 * Ném ra khi vi phạm quy tắc nghiệp vụ **BR2 — FATHER != MOTHER**:
 * cha và mẹ của cùng một người không được là cùng một Person.
 *
 * <p>Ví dụ: gán {@code fatherId = 5} cho một người con đang có {@code motherId = 5}.
 *
 * <p>Kế thừa {@link RuntimeException} để tầng Service/Controller xử lý lỗi nghiệp vụ
 * mà không cần bắt buộc khai báo {@code throws}.
 */
public class SameParentException extends RuntimeException {

    public SameParentException(String message) {
        super(message);
    }

    public SameParentException(String message, Throwable cause) {
        super(message, cause);
    }
}
