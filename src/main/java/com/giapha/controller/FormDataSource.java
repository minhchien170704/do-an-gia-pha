package com.giapha.controller;

import com.giapha.model.Gender;
import com.giapha.model.Person;

/**
 * Nguồn dữ liệu form thành viên — abstraction cho lớp giao diện nhập liệu.
 *
 * <p><b>Mục đích (Dependency Inversion):</b> {@link PersonController} cần đọc dữ liệu
 * người dùng đang nhập (họ tên, năm sinh, giới tính, cha/mẹ đã chọn, ảnh) khi xử lý
 * lưu. Thay vì phụ thuộc trực tiếp vào lớp concrete {@code PersonFormView},
 * Controller phụ thuộc vào interface này — nhờ vậy:
 * <ul>
 *     <li>Controller <b>không</b> import bất kỳ lớp nào trong {@code com.giapha.view.*}.</li>
 *     <li>Có thể kiểm thử Controller bằng fake implementation, không cần JavaFX Toolkit.</li>
 *     <li>Không truyền JavaFX {@code Node}/{@code Control} qua interface — chỉ có
 *         kiểu dữ liệu đơn giản ({@link String}, {@link Gender}, {@link Person}).</li>
 * </ul>
 *
 * <p><b>Quy ước:</b> mọi giá trị trả về đều là <b>dữ liệu thô</b> do người dùng nhập,
 * chưa qua validation. Việc kiểm tra hợp lệ (tên rỗng, năm sinh không phải số,
 * giới tính chưa chọn) thuộc trách nhiệm của Controller — interface này KHÔNG
 * chứa logic nghiệp vụ.
 *
 * @see PersonFormView
 */
public interface FormDataSource {

    /**
     * Họ và tên do người dùng nhập (chưa trim, chưa kiểm tra rỗng).
     *
     * @return họ và tên, hoặc chuỗi rỗng/null nếu chưa nhập
     */
    String getFullNameInput();

    /**
     * Năm sinh do người dùng nhập dưới dạng <b>văn bản thô</b>.
     *
     * <p>Trả về {@link String} (không phải {@code int}) để Controller tự phân tích
     * và hiển thị thông báo lỗi "Năm sinh phải là số nguyên hợp lệ." khi người dùng
     * gõ sai — việc chuyển đổi chuỗi sang số là trách nhiệm của tầng điều phối,
     * không phải của tầng giao diện.
     *
     * @return năm sinh dạng chuỗi, hoặc chuỗi rỗng/null nếu chưa nhập
     */
    String getBirthYearInput();

    /**
     * Giới tính đang được chọn.
     *
     * @return giới tính, hoặc null nếu người dùng chưa chọn
     */
    Gender getGenderInput();

    /**
     * Cha đang được chọn trong danh sách.
     *
     * @return đối tượng cha, hoặc null nếu chưa chọn (bao gồm trường hợp gỡ cha)
     */
    Person getSelectedFather();

    /**
     * Mẹ đang được chọn trong danh sách.
     *
     * @return đối tượng mẹ, hoặc null nếu chưa chọn (bao gồm trường hợp gỡ mẹ)
     */
    Person getSelectedMother();

    /**
     * Đường dẫn file ảnh <b>nguồn</b> mà người dùng vừa chọn qua FileChooser.
     *
     * <p>Đường dẫn tuyệt đối. Trả về null nghĩa là người dùng KHÔNG chọn ảnh mới —
     * khi đó ảnh đại diện hiện tại của thành viên được giữ nguyên.
     *
     * @return đường dẫn tuyệt đối tới ảnh nguồn, hoặc null
     */
    String getSelectedPhotoSourcePath();

    /**
     * Đường dẫn ảnh đại diện hiện tại đã lưu trong CSDL (đường dẫn tương đối).
     *
     * <p>Dùng khi chỉnh sửa mà người dùng không chọn ảnh mới — giữ nguyên ảnh cũ.
     *
     * @return đường dẫn tương đối trong DB, hoặc null nếu thành viên chưa có ảnh
     */
    String getExistingPhotoPath();

    /**
     * Thành viên đang được chỉnh sửa trong form.
     *
     * @return đối tượng Person cần sửa, hoặc null nếu form đang ở chế độ thêm mới
     */
    Person getEditingPerson();
}
