package com.giapha.exception;

import com.giapha.model.Gender;

/**
 * Ném ra khi vi phạm quy tắc nghiệp vụ **BR6 — GENDER ROLE**:
 * {@code father} bắt buộc phải có giới tính {@link Gender#MALE},
 * {@code mother} bắt buộc phải có giới tính {@link Gender#FEMALE}.
 *
 * <p>Ví dụ: gán một người nữ làm cha, hoặc gán một người nam làm mẹ.
 *
 * <p>Kế thừa {@link RuntimeException} để tầng Service/Controller xử lý lỗi nghiệp vụ
 * mà không cần bắt buộc khai báo {@code throws}.
 */
public class InvalidParentGenderException extends RuntimeException {

    public InvalidParentGenderException(String message) {
        super(message);
    }

    public InvalidParentGenderException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Tạo ngoại lệ với thông điệp chuẩn cho trường hợp giới tính sai vai trò.
     *
     * @param role         vai trò cần gán ("Cha" hoặc "Mẹ")
     * @param requiredGender giới tính bắt buộc của vai trò đó
     * @param actualGender   giới tính thực tế của người được gán
     * @return ngoại lệ mới
     */
    public static InvalidParentGenderException forRole(String role,
                                                        Gender requiredGender,
                                                        Gender actualGender) {
        return new InvalidParentGenderException(role + " phải có giới tính là "
                + requiredGender + " nhưng thực tế là " + actualGender);
    }
}
