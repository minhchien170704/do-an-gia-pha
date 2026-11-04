package com.giapha.exception;

/**
 * Ném ra khi năm sinh không hợp lệ (birthYear &lt; 1000 hoặc birthYear &gt; năm hiện tại).
 * Kế thừa RuntimeException để xử lý lỗi nghiệp vụ ở tầng Service/Controller mà không cần bắt buộc khai báo throws.
 */
public class InvalidBirthYearException extends RuntimeException {

    public InvalidBirthYearException(String message) {
        super(message);
    }

    public InvalidBirthYearException(String message, Throwable cause) {
        super(message, cause);
    }
}
