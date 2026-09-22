package com.giapha.exception;

/**
 * Ném ra khi cố gắng gán một người làm cha hoặc mẹ của chính họ (childId == parentId).
 * Kế thừa RuntimeException để xử lý lỗi nghiệp vụ ở tầng Service/Controller mà không cần bắt buộc khai báo throws.
 */
public class SelfParentException extends RuntimeException {

    public SelfParentException(String message) {
        super(message);
    }

    public SelfParentException(String message, Throwable cause) {
        super(message, cause);
    }
}
