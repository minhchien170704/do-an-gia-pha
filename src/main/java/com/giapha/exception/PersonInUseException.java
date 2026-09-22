package com.giapha.exception;

/**
 * Ném ra khi cố gắng xóa một Person đang được người khác tham chiếu tới qua quan hệ cha (father_id) hoặc mẹ (mother_id).
 * Kế thừa RuntimeException để xử lý lỗi nghiệp vụ ở tầng Service/Controller mà không cần bắt buộc khai báo throws.
 */
public class PersonInUseException extends RuntimeException {

    public PersonInUseException(String message) {
        super(message);
    }

    public PersonInUseException(String message, Throwable cause) {
        super(message, cause);
    }
}
