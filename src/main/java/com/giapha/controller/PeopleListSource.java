package com.giapha.controller;

import com.giapha.model.Person;

/**
 * Nguồn dữ liệu danh sách thành viên — abstraction cho màn hình danh sách.
 *
 * <p><b>Mục đích (Dependency Inversion):</b> {@link PersonController} cần biết
 * thành viên nào đang được chọn trên bảng và cần yêu cầu làm mới danh sách sau
 * khi xóa. Thay vì phụ thuộc vào lớp concrete {@code PeopleListView}, Controller
 * phụ thuộc vào interface này.
 *
 * <p>Interface này <b>không</b> thuộc {@code Navigator}: chọn dòng và làm mới bảng
 * là thao tác trên <b>trạng thái màn hình danh sách</b>, không phải hành vi điều hướng
 * sang màn hình khác. Tách riêng giữ mỗi interface chỉ có một trách nhiệm.
 *
 * @see PeopleListView
 */
public interface PeopleListSource {

    /**
     * Thành viên đang được chọn trên bảng danh sách.
     *
     * @return đối tượng Person được chọn, hoặc null nếu chưa chọn dòng nào
     */
    Person getSelectedPerson();

    /**
     * Yêu cầu làm mới danh sách — tải lại dữ liệu từ CSDL để đồng bộ với
     * các thay đổi vừa thực hiện (ví dụ sau khi xóa một thành viên).
     */
    void refresh();
}
