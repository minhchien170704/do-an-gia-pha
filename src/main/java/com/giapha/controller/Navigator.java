package com.giapha.controller;

import com.giapha.model.Person;

/**
 * Điều hướng giữa các màn hình của ứng dụng — abstraction cho composition root.
 *
 * <p><b>Mục đích (Dependency Inversion):</b> {@link PersonController} cần yêu cầu
 * chuyển sang màn hình khác sau khi hoàn tất một thao tác (ví dụ: thêm/sửa xong →
 * quay lại danh sách; xem chi tiết → mở màn hình chi tiết). Thay vì giữ tham chiếu
 * tới JavaFX {@code Scene}/{@code Stage} hay tới lớp View concrete, Controller gọi
 * các hành vi điều hướng thông qua interface này.
 *
 * <p>Việc nối {@code Navigator} với JavaFX thật được thực hiện tại composition root
 * ({@code Main}) — nơi duy nhất được phép biết cả View lẫn cơ chế Scene.
 *
 * <p>Interface này <b>không</b> phải là navigation framework: chỉ gồm 3 hành vi
 * điều hướng tối thiểu mà Controller thực sự cần.
 *
 * @see PersonController
 */
public interface Navigator {

    /**
     * Chuyển tới màn hình danh sách thành viên.
     *
     * <p>Được gọi sau khi thêm/sửa thành viên thành công.
     */
    void navigateToPeopleList();

    /**
     * Chuyển tới màn hình chi tiết của một thành viên.
     *
     * @param person thành viên cần xem chi tiết (đã được xác thực tồn tại)
     */
    void navigateToPersonDetail(Person person);

    /**
     * Chuyển tới màn hình cây gia phả.
     *
     * @param focusPerson thành viên làm trung tâm cây gia phả (đã được xác thực tồn tại)
     */
    void navigateToFamilyTree(Person focusPerson);
}
