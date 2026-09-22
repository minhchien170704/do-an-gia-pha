package com.giapha.exception;

/**
 * Ném ra khi việc gán quan hệ cha/mẹ tạo ra chu trình (vòng lặp) trong cây gia phả.
 * Kế thừa RuntimeException để xử lý lỗi nghiệp vụ ở tầng Service/Controller mà không cần bắt buộc khai báo throws.
 */
public class CycleDetectedException extends RuntimeException {

    public CycleDetectedException(String message) {
        super(message);
    }

    public CycleDetectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
