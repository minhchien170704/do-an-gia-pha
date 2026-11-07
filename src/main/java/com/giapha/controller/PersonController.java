package com.giapha.controller;

import com.giapha.exception.CycleDetectedException;
import com.giapha.exception.InvalidBirthYearException;
import com.giapha.exception.InvalidParentGenderException;
import com.giapha.exception.PersonInUseException;
import com.giapha.exception.PersonNotFoundException;
import com.giapha.exception.SameParentException;
import com.giapha.exception.SelfParentException;
import com.giapha.model.Gender;
import com.giapha.model.Person;
import com.giapha.service.FamilyTreeService;
import com.giapha.util.ImageManager;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Controller điều phối luồng nghiệp vụ giữa giao diện JavaFX và FamilyTreeService.
 * Tuân thủ nghiêm ngặt nguyên tắc kiến trúc phân tầng:
 * - Chỉ phụ thuộc duy nhất vào FamilyTreeService.
 * - Không truy cập trực tiếp Repository hay JDBC.
 * - KHÔNG import bất kỳ lớp nào trong {@code com.giapha.view.*}: mọi tương tác
 *   với giao diện đi qua các abstraction {@link FormDataSource},
 *   {@link PeopleListSource} và {@link Navigator}.
 * - Không duplicate quy tắc nghiệp vụ/validation.
 * - Bắt và xử lý các custom exception để hiển thị thông báo thân thiện bằng tiếng Việt.
 */
public class PersonController {

    private final FamilyTreeService familyTreeService;

    // ===== Abstraction tới giao diện (không phụ thuộc lớp View concrete) =====

    /**
     * Nguồn dữ liệu form nhập liệu — Controller đọc họ tên / năm sinh / giới tính /
     * cha / mẹ / ảnh từ đây khi xử lý lưu.
     */
    private FormDataSource formDataSource;

    /**
     * Nguồn dữ liệu màn hình danh sách — cung cấp thành viên đang chọn và
     * nhận yêu cầu làm mới sau khi xóa.
     */
    private PeopleListSource peopleListSource;

    /**
     * Bộ điều hướng màn hình — Controller chỉ yêu cầu chuyển màn hình,
     * không tự khởi tạo hay quản lý View/Scene.
     */
    private Navigator navigator;

    // Thành viên được chọn trên danh sách — dùng làm ngữ cảnh mặc định
    // cho các handler không tham số (handleEdit / handleDelete / handleViewDetail).
    private Person selectedPerson;

    /**
     * Khởi tạo PersonController với FamilyTreeService.
     *
     * @param familyTreeService dịch vụ quản lý cây gia phả
     */
    public PersonController(FamilyTreeService familyTreeService) {
        this.familyTreeService = Objects.requireNonNull(familyTreeService, "familyTreeService không được để null");
    }

    // ============================================================
    // CÁC PHƯƠNG THỨC PUBLIC API BẮT BUỘC THEO CLASS DIAGRAM
    // ============================================================

    /**
     * Xử lý sự kiện thêm thành viên mới từ giao diện (UC01).
     *
     * <p>Đọc dữ liệu từ form, kiểm tra BR5 (năm sinh cha/mẹ) và hiển thị Alert cảnh báo
     * cho phép người dùng chọn tiếp tục hoặc hủy, sau đó gọi {@link #handleAdd(Person)}.
     */
    public void handleAdd() {
        if (formDataSource == null) {
            showWarningAlert("Thông báo", "Không tìm thấy giao diện nhập liệu thành viên.");
            return;
        }

        String fullName = formDataSource.getFullNameInput();
        String birthYearStr = formDataSource.getBirthYearInput();
        Gender gender = formDataSource.getGenderInput();
        Person father = formDataSource.getSelectedFather();
        Person mother = formDataSource.getSelectedMother();

        if (fullName == null || fullName.trim().isEmpty()) {
            showWarningAlert("Thông báo", "Họ và tên không được để trống.");
            return;
        }

        if (birthYearStr == null || birthYearStr.trim().isEmpty()) {
            showWarningAlert("Thông báo", "Năm sinh không được để trống.");
            return;
        }

        int birthYear;
        try {
            birthYear = Integer.parseInt(birthYearStr.trim());
        } catch (NumberFormatException e) {
            showWarningAlert("Thông báo", "Năm sinh phải là số nguyên hợp lệ.");
            return;
        }

        if (gender == null) {
            showWarningAlert("Thông báo", "Vui lòng chọn giới tính.");
            return;
        }

        // Thành viên mới chưa có ảnh trong DB — ảnh sẽ được copy và sinh đường dẫn
        // tương đối trong handleAdd(Person) sau khi có id.
        Person newPerson = new Person(
                fullName.trim(),
                birthYear,
                gender,
                null
        );
        newPerson.setFather(father);
        newPerson.setMother(mother);

        // BR5: cảnh báo nếu năm sinh cha/mẹ không nhỏ hơn năm sinh con.
        // Việc kiểm tra này nằm trong handleAdd(Person) — đúng là phương thức mà
        // luồng lưu thực tế từ form nhập liệu đi qua (không phải chỉ ở handleAdd()).
        handleAdd(newPerson);
    }

    /**
     * Overloaded: Xử lý thêm đối tượng Person cụ thể vào hệ thống.
     *
     * <p><b>Đây là phương thức lưu THỰC SỰ của luồng UI</b> — form nhập liệu
     * gọi phương thức này khi người dùng bấm "Lưu thành viên". Vì vậy kiểm tra
     * BR5 (cảnh báo năm sinh cha/mẹ) được đặt ở ĐÂY để mọi lần lưu từ giao diện
     * đều có cảnh báo, không chỉ riêng lời gọi từ {@link #handleAdd()}.
     *
     * @param person đối tượng thành viên cần thêm
     * @return đối tượng Person đã được gán id, hoặc null nếu thao tác thất bại
     */
    public Person handleAdd(Person person) {
        // BR5 — cảnh báo năm sinh cha/mẹ không nhỏ hơn năm sinh con.
        // Chỉ hỏi khi thực sự có quan hệ cha/mẹ được gán.
        if (person != null && (person.getFather() != null || person.getMother() != null)) {
            if (!confirmBirthYearWarning(person, person.getFather(), person.getMother())) {
                return null; // Người dùng chọn Hủy
            }
        }

        // ===== BƯỚC 1 — Lưu thông tin thành viên vào DB =====
        // Lỗi ở bước này = thành viên KHÔNG được tạo → báo lỗi và dừng.
        Person added;
        try {
            added = familyTreeService.addPerson(person);
        } catch (InvalidBirthYearException e) {
            showErrorAlert("Lỗi năm sinh", e.getMessage());
            return null;
        } catch (PersonNotFoundException e) {
            showErrorAlert("Không tìm thấy", "Phụ huynh chỉ định không tồn tại: " + e.getMessage());
            return null;
        } catch (SelfParentException e) {
            showErrorAlert("Lỗi quan hệ", "Không thể tự gán mình làm cha hoặc mẹ: " + e.getMessage());
            return null;
        } catch (CycleDetectedException e) {
            showErrorAlert("Lỗi chu trình", "Thao tác tạo chu trình trong cây gia phả: " + e.getMessage());
            return null;
        } catch (SameParentException e) {
            showErrorAlert("Lỗi quan hệ", "Cha và Mẹ không thể là cùng một người: " + e.getMessage());
            return null;
        } catch (InvalidParentGenderException e) {
            showErrorAlert("Lỗi giới tính vai trò", e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            showErrorAlert("Lỗi dữ liệu", e.getMessage());
            return null;
        } catch (Exception e) {
            showErrorAlert("Lỗi", "Không thể thêm thành viên: " + e.getMessage());
            return null;
        }

        // ===== BƯỚC 2 — Xử lý ảnh đại diện (tách biệt hoàn toàn với BƯỚC 1) =====
        // Thành viên ĐÃ được lưu thành công vào DB. Lỗi ở bước này KHÔNG được
        // báo như "thêm thành viên thất bại" — chỉ báo ảnh chưa lưu được.
        String imageErrorMessage = persistImageAfterAdd(added);

        if (imageErrorMessage == null) {
            showInfoAlert("Thành công",
                    "Đã thêm thành viên \"" + added.getFullName() + "\" vào gia phả thành công.");
        } else {
            showWarningAlert("Đã thêm thành viên nhưng ảnh đại diện chưa được lưu",
                    "Thành viên \"" + added.getFullName() + "\" đã được thêm vào gia phả thành công.\n\n"
                            + "Tuy nhiên ảnh đại diện chưa được lưu: " + imageErrorMessage
                            + "\n\nBạn có thể chọn lại ảnh sau bằng chức năng Sửa.");
        }

        if (navigator != null) {
            navigator.navigateToPeopleList();
        }
        return added;
    }

    /**
     * Lấy đường dẫn file ảnh NGUỒN do người dùng chọn trong form.
     *
     * <p>Được tách thành phương thức riêng để tầng kiểm thử có thể kiểm tra
     * luồng xử lý ảnh (copy file, cập nhật {@code photo_path}) mà không cần
     * khởi tạo JavaFX Toolkit.
     *
     * @return đường dẫn tuyệt đối của ảnh nguồn, hoặc null nếu không chọn ảnh
     */
    protected String getSelectedPhotoSourcePath() {
        return (formDataSource == null) ? null : formDataSource.getSelectedPhotoSourcePath();
    }

    /**
     * Xử lý ảnh đại diện sau khi thành viên đã được lưu thành công vào DB.
     *
     * <p>Ảnh được copy vào thư mục {@code images/} và đường dẫn tương đối được ghi
     * bằng {@link FamilyTreeService#updatePhotoPath(int, String)} — chỉ cập nhật
     * <b>duy nhất</b> cột {@code photo_path}, KHÔNG ghi đè các cột khác.
     *
     * @param added thành viên vừa được thêm (đã có id)
     * @return {@code null} nếu không có ảnh nào cần xử lý hoặc xử lý thành công;
     *         ngược lại là thông điệp mô tả lỗi để hiển thị cho người dùng
     */
    private String persistImageAfterAdd(Person added) {
        String sourcePhoto = getSelectedPhotoSourcePath();
        if (sourcePhoto == null || sourcePhoto.trim().isEmpty()) {
            return null; // Người dùng không chọn ảnh — không phải lỗi
        }

        String newPhotoPath;
        try {
            newPhotoPath = ImageManager.copyImageToAppDirectory(sourcePhoto, added.getId());
        } catch (IOException e) {
            return "Không thể lưu ảnh vào thư mục images (" + e.getMessage() + ").";
        } catch (IllegalArgumentException e) {
            return "Định dạng ảnh không hợp lệ (" + e.getMessage() + ").";
        }

        try {
            // Chỉ cập nhật cột photo_path — không đụng tới họ tên / năm sinh /
            // giới tính / quan hệ cha mẹ.
            familyTreeService.updatePhotoPath(added.getId(), newPhotoPath);
            added.setPhotoPath(newPhotoPath);
            return null;
        } catch (Exception e) {
            // Thành viên đã lưu thành công → KHÔNG báo "thêm thất bại".
            // Ảnh trong DB vẫn giữ giá trị cũ (null), không phá vỡ dữ liệu.
            return "Không thể cập nhật đường dẫn ảnh trong cơ sở dữ liệu (" + e.getMessage() + ").";
        }
    }

    /**
     * Xử lý chỉnh sửa thông tin thành viên (UC02).
     * Tuân thủ quy tắc kiến trúc nghiêm ngặt:
     * - updatePerson() chỉ cập nhật các trường cơ bản (fullName, birthYear, gender, photoPath).
     * - Việc thay đổi cha/mẹ được tách bạch và thực thi thông qua assignFather() / assignMother().
     */
    public void handleEdit() {
        Person target = null;
        if (formDataSource != null && formDataSource.getEditingPerson() != null) {
            target = formDataSource.getEditingPerson();
        } else if (selectedPerson != null) {
            target = selectedPerson;
        }

        if (target == null || target.getId() <= 0) {
            showWarningAlert("Thông báo", "Không có thông tin thành viên hợp lệ để chỉnh sửa.");
            return;
        }

        int personId = target.getId();

        // 1. Tải bản gốc hiện tại từ Service để biết cha/mẹ ban đầu
        Person original;
        try {
            original = familyTreeService.getPersonById(personId);
        } catch (PersonNotFoundException e) {
            showErrorAlert("Không tìm thấy", "Không tìm thấy thành viên có ID = " + personId);
            return;
        }

        Person originalFather = original.getFather();
        Person originalMother = original.getMother();

        // 2. Đọc các giá trị mới từ form
        String newFullName = formDataSource != null ? formDataSource.getFullNameInput() : target.getFullName();
        String newBirthYearStr = formDataSource != null ? formDataSource.getBirthYearInput() : String.valueOf(target.getBirthYear());
        Gender newGender = formDataSource != null ? formDataSource.getGenderInput() : target.getGender();
        // Ảnh đại diện: luôn giữ đường dẫn TƯƠNG ĐỐI đã lưu trong DB.
        // Nếu người dùng chọn ảnh mới, ảnh đó được copy + sinh đường dẫn mới trong
        // handleEdit(Person, Person, Person) trước khi ghi xuống DB.
        String newPhotoPath = formDataSource != null
                ? formDataSource.getExistingPhotoPath()
                : target.getPhotoPath();
        Person newFather = formDataSource != null ? formDataSource.getSelectedFather() : target.getFather();
        Person newMother = formDataSource != null ? formDataSource.getSelectedMother() : target.getMother();

        if (newFullName == null || newFullName.trim().isEmpty()) {
            showWarningAlert("Thông báo", "Họ và tên không được để trống.");
            return;
        }

        int newBirthYear;
        try {
            newBirthYear = Integer.parseInt(newBirthYearStr.trim());
        } catch (NumberFormatException e) {
            showWarningAlert("Thông báo", "Năm sinh phải là số nguyên hợp lệ.");
            return;
        }

        if (newGender == null) {
            showWarningAlert("Thông báo", "Vui lòng chọn giới tính.");
            return;
        }

        // 3. Chuẩn bị cập nhật các trường cơ bản, GIỮ NGUYÊN cha/mẹ gốc để không bị xóa quan hệ trong updatePerson
        Person basicUpdatePerson = new Person(
                newFullName.trim(),
                newBirthYear,
                newGender,
                (newPhotoPath != null && !newPhotoPath.trim().isEmpty()) ? newPhotoPath.trim() : null
        );
        basicUpdatePerson.setId(personId);
        basicUpdatePerson.setFather(originalFather);
        basicUpdatePerson.setMother(originalMother);

        // Lưu ý: kiểm tra BR5 (cảnh báo năm sinh) được thực hiện BÊN TRONG
        // handleEdit(Person, Person, Person) — đúng là phương thức mà luồng lưu
        // thực tế từ form nhập liệu đi qua. Ở đây KHÔNG kiểm tra lại để tránh
        // hiện cảnh báo hai lần khi gọi lồng nhau.
        handleEdit(basicUpdatePerson, newFather, newMother);
    }

    /**
     * Overloaded: Thực thi quy trình cập nhật thông tin và gán quan hệ cha mẹ.
     *
     * <p><b>Tính nguyên tử:</b> toàn bộ thay đổi (thông tin cá nhân + {@code father_id}
     * + {@code mother_id}) được kiểm tra trước rồi ghi trong MỘT lần UPDATE qua
     * {@link FamilyTreeService#updatePersonWithParents(Person, Person, Person)}.
     * Nhờ vậy nếu một quan hệ không hợp lệ thì KHÔNG có thay đổi nào được ghi —
     * dữ liệu không ở trạng thái "cập nhật một nửa".
     *
     * <p><b>Đây là phương thức lưu THỰC SỰ của luồng UI</b> — form nhập liệu
     * gọi phương thức này khi người dùng bấm "Lưu thay đổi".
     *
     * @param personToUpdate thông tin thành viên cập nhật (chứa cha mẹ gốc)
     * @param newFather      cha mới được chọn từ ComboBox, {@code null} = gỡ cha
     * @param newMother      mẹ mới được chọn từ ComboBox, {@code null} = gỡ mẹ
     */
    public void handleEdit(Person personToUpdate, Person newFather, Person newMother) {
        int personId = personToUpdate.getId();
        Person originalFather = personToUpdate.getFather();
        Person originalMother = personToUpdate.getMother();
        String originalPhotoPath = personToUpdate.getPhotoPath();

        // ===== BƯỚC 0 — Giữ nguyên đường dẫn ảnh đang lưu trong DB =====
        // Nếu caller truyền vào một Person mà KHÔNG kèm photo_path (null),
        // đường dẫn ảnh đang lưu trong DB vẫn phải được giữ nguyên.
        // Việc cập nhật ảnh chỉ đi qua updatePhotoPath() ở Bước 4 — không bao giờ
        // đi qua bản ghi UPDATE của thông tin cá nhân.
        if (originalPhotoPath == null) {
            try {
                originalPhotoPath = familyTreeService.getPersonById(personId).getPhotoPath();
                personToUpdate.setPhotoPath(originalPhotoPath);
            } catch (PersonNotFoundException ignored) {
                // Không tìm thấy → để nguyên null, các bước sau sẽ báo lỗi phù hợp.
            }
        }

        // ===== BƯỚC 1 — BR5: cảnh báo năm sinh cha/mẹ không nhỏ hơn năm sinh con =====
        // Chỉ hỏi khi quan hệ cha/mẹ THỰC SỰ thay đổi so với bản gốc, và chỉ hỏi
        // với các quan hệ MỚI được chọn.
        //
        // LƯU Ý THỨ TỰ: bước này chạy TRƯỚC khi copy ảnh. Nếu người dùng chọn
        // "Hủy" thì chưa có file nào bị ghi đè → không cần dọn dẹp gì.
        if (fatherChanged(originalFather, newFather) || motherChanged(originalMother, newMother)) {
            Person effectiveFather = newFather != null ? newFather : originalFather;
            Person effectiveMother = newMother != null ? newMother : originalMother;
            if (!confirmBirthYearWarning(personToUpdate, effectiveFather, effectiveMother)) {
                // Người dùng chọn Hủy — KHÔNG lưu gì cả, kể cả ảnh đã copy.
                return;
            }
        }

        // ===== BƯỚC 2 — Cập nhật NGUYÊN TỬ — kiểm tra toàn bộ BR trước, rồi ghi MỘT lần =====
        // photo_path KHÔNG tham gia vào bản ghi này — giữ nguyên đường dẫn cũ,
        // việc cập nhật ảnh được thực hiện riêng ở Bước 4.
        //
        // ẢNH CHƯA ĐƯỢC COPY TẠI ĐÂY. Đây là điểm mấu chốt: `ImageManager` đặt tên
        // file `person_<id>.<đuôi>` và copy bằng REPLACE_EXISTING, nên nếu copy ảnh
        // TRƯỚC khi service chạy thì ảnh cùng đuôi sẽ GHI ĐÈ thẳng ảnh cũ trên
        // đĩa. Khi service sau đó từ chối, ảnh cũ đã mất dù DB không đổi.
        try {
            familyTreeService.updatePersonWithParents(personToUpdate, newFather, newMother);
        } catch (SelfParentException e) {
            // Service đã TỪ CHỐI quan hệ → KHÔNG ghi bất cứ thứ gì, ảnh cũ giữ nguyên.
            showErrorAlert("Lỗi quan hệ", "Không thể tự gán mình làm cha hoặc mẹ: " + e.getMessage());
            return;
        } catch (CycleDetectedException e) {
            showErrorAlert("Lỗi chu trình", "Thao tác tạo chu trình trong cây gia phả: " + e.getMessage());
            return;
        } catch (InvalidParentGenderException e) {
            showErrorAlert("Lỗi giới tính vai trò", e.getMessage());
            return;
        } catch (SameParentException e) {
            showErrorAlert("Lỗi quan hệ", "Cha và Mẹ không thể là cùng một người: " + e.getMessage());
            return;
        } catch (PersonNotFoundException e) {
            showErrorAlert("Không tìm thấy", "Phụ huynh chỉ định không tồn tại: " + e.getMessage());
            return;
        } catch (InvalidBirthYearException e) {
            showErrorAlert("Lỗi năm sinh", e.getMessage());
            return;
        } catch (IllegalArgumentException e) {
            showErrorAlert("Lỗi dữ liệu", e.getMessage());
            return;
        } catch (Exception e) {
            showErrorAlert("Lỗi cập nhật", "Không thể cập nhật thông tin thành viên: " + e.getMessage());
            return;
        }

        // ===== BƯỚC 3 — CHỈ SAU KHI cập nhật thành công mới dựng thông điệp thành công =====
        StringBuilder successMsg = new StringBuilder("Đã cập nhật thành công thông tin thành viên.");
        if (fatherChanged(originalFather, newFather)) {
            successMsg.append(newFather != null
                    ? "\n• Đã cập nhật cha thành công."
                    : "\n• Đã gỡ quan hệ cha thành công.");
        }
        if (motherChanged(originalMother, newMother)) {
            successMsg.append(newMother != null
                    ? "\n• Đã cập nhật mẹ thành công."
                    : "\n• Đã gỡ quan hệ mẹ thành công.");
        }

        // ===== BƯỚC 4 — Xử lý ảnh đại diện — CHỈ SAU KHI thông tin đã cập nhật thành công =====
        // Tách khỏi Bước 2 để lỗi ảnh không làm hỏng việc cập nhật thông tin
        // thành viên đã thành công.
        String photoErrorMessage = null;
        String sourcePhoto = getSelectedPhotoSourcePath();
        if (sourcePhoto != null && !sourcePhoto.trim().isEmpty()) {
            String newPhotoPath = null;
            try {
                // Copy ảnh mới — chỉ khi thông tin thành viên đã lưu thành công.
                newPhotoPath = ImageManager.copyImageToAppDirectory(sourcePhoto, personId);
            } catch (IOException e) {
                photoErrorMessage = "Không thể lưu ảnh vào thư mục images (" + e.getMessage() + ").";
            } catch (IllegalArgumentException e) {
                photoErrorMessage = "Định dạng ảnh không hợp lệ (" + e.getMessage() + ").";
            }

            if (newPhotoPath != null) {
                try {
                    // Chỉ cập nhật cột photo_path — không đụng tới họ tên / năm sinh /
                    // giới tính / quan hệ cha mẹ (đã lưu ở Bước 2).
                    familyTreeService.updatePhotoPath(personId, newPhotoPath);
                    personToUpdate.setPhotoPath(newPhotoPath);

                    // Dọn file ảnh CŨ nếu đường dẫn đã đổi (ảnh mới có tên file khác).
                    if (originalPhotoPath != null && !originalPhotoPath.equals(newPhotoPath)) {
                        ImageManager.deleteImageQuietly(originalPhotoPath);
                    }
                } catch (Exception e) {
                    photoErrorMessage = "Không thể cập nhật ảnh đại diện trong cơ sở dữ liệu (" + e.getMessage() + ").";
                    // Ảnh mới đã copy vào images/ nhưng chưa lưu được đường dẫn vào DB.
                    // Nếu đường dẫn khác ảnh gốc → dọn để không để lại file không được
                    // tham chiếu. Nếu BẰNG ảnh gốc (cùng đuôi) → KHÔNG xóa, vì DB vẫn
                    // đang trỏ tới chính file này.
                    discardCopiedImage(newPhotoPath, originalPhotoPath);
                }
            }
        }

        // ===== BƯỚC 5 — Báo cáo kết quả =====
        if (photoErrorMessage != null) {
            showWarningAlert("Đã cập nhật thông tin nhưng ảnh đại diện chưa được lưu",
                    successMsg.toString() + "\n\n"
                            + "Tuy nhiên ảnh đại diện chưa được lưu: " + photoErrorMessage
                            + "\n\nBạn có thể chọn lại ảnh sau bằng chức năng Sửa.");
        } else {
            showInfoAlert("Thành công", successMsg.toString());
        }

        if (navigator != null) {
            navigator.navigateToPeopleList();
        }
    }

    /**
     * Dọn file ảnh vừa copy khi bước ghi ảnh đại diện vào CSDL thất bại.
     *
     * <p>Chỉ xóa khi đường dẫn mới KHÁC đường dẫn ảnh gốc đang lưu trong DB —
     * nếu bằng nhau (cùng đuôi file) thì xóa sẽ làm mất chính file mà DB đang trỏ tới.
     *
     * @param newPhotoPath     đường dẫn tương đối của ảnh vừa copy (có thể null)
     * @param originalPhotoPath đường dẫn ảnh gốc đang lưu trong DB (có thể null)
     */
    private void discardCopiedImage(String newPhotoPath, String originalPhotoPath) {
        if (newPhotoPath != null && !newPhotoPath.equals(originalPhotoPath)) {
            ImageManager.deleteImageQuietly(newPhotoPath);
        }
    }

    /**
     * Xử lý xóa thành viên khỏi gia phả (UC03).
     */
    public void handleDelete() {
        Person target = selectedPerson;
        if (target == null && peopleListSource != null) {
            target = peopleListSource.getSelectedPerson();
        }

        if (target == null) {
            showWarningAlert("Thông báo", "Vui lòng chọn một thành viên trong danh sách để xóa.");
            return;
        }

        handleDelete(target.getId());
    }

    /**
     * Overloaded: Xóa thành viên theo mã định danh (id).
     *
     * @param id mã thành viên cần xóa
     */
    public void handleDelete(int id) {
        // Lấy đường dẫn ảnh TRƯỚC khi xóa bản ghi — sau khi xóa thì không còn
        // đọc được photo_path nữa.
        String photoPath = null;
        try {
            Person target = familyTreeService.getPersonById(id);
            photoPath = target.getPhotoPath();
        } catch (PersonNotFoundException e) {
            // Không tìm thấy người cần xóa → không có gì để dọn ảnh.
            photoPath = null;
        }

        try {
            familyTreeService.deletePerson(id);

            // Chỉ xóa file ảnh SAU khi xóa DB thành công.
            // Nếu xóa DB thất bại (PersonInUseException / SQLException) → ảnh được giữ nguyên.
            // deleteImageQuietly() không ném exception nên lỗi xóa file KHÔNG làm
            // đảo ngược kết quả xóa DB.
            ImageManager.deleteImageQuietly(photoPath);

            showInfoAlert("Thành công", "Đã xóa thành viên có ID = " + id + " khỏi hệ thống gia phả.");
            if (peopleListSource != null) {
                peopleListSource.refresh();
            }
        } catch (PersonInUseException e) {
            showErrorAlert("Không thể xóa thành viên",
                    "Thành viên này hiện đang đóng vai trò làm cha hoặc mẹ của người khác trong hệ thống gia phả.\n"
                    + "Vui lòng thay đổi hoặc xóa các mối quan hệ con cái trước khi thực hiện xóa.");
        } catch (PersonNotFoundException e) {
            showErrorAlert("Không tìm thấy", "Thành viên với ID = " + id + " không tồn tại trong hệ thống.");
        } catch (Exception e) {
            showErrorAlert("Lỗi xóa", "Không thể xóa thành viên: " + e.getMessage());
        }
    }

    /**
     * Tìm kiếm thành viên theo từ khóa họ tên (UC04).
     *
     * @param keyword từ khóa tìm kiếm họ tên
     * @return danh sách kết quả phù hợp
     */
    public List<Person> handleSearch(String keyword) {
        try {
            return familyTreeService.searchPersons(keyword);
        } catch (Exception e) {
            showErrorAlert("Lỗi tìm kiếm", "Không thể tìm kiếm thành viên: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Lấy toàn bộ danh sách thành viên từ Service (dùng findAll — đồ thị liên kết đầy đủ).
     * Được gọi bởi các View để nạp dữ liệu từ MySQL thay cho SampleData.
     *
     * @return danh sách tất cả Person đã liên kết cha/mẹ, hoặc danh sách rỗng nếu lỗi
     */
    public List<Person> getAllPersons() {
        try {
            return familyTreeService.getAllPersons();
        } catch (Exception e) {
            showErrorAlert("Lỗi tải dữ liệu", "Không thể tải danh sách thành viên: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Xử lý xem chi tiết hồ sơ thành viên (UC06).
     */
    public void handleViewDetail() {
        Person target = selectedPerson;
        if (target == null && peopleListSource != null) {
            target = peopleListSource.getSelectedPerson();
        }

        if (target == null) {
            showWarningAlert("Thông báo", "Vui lòng chọn một thành viên để xem chi tiết.");
            return;
        }

        handleViewDetail(target.getId());
    }

    /**
     * Overloaded: Xem chi tiết thành viên theo ID.
     *
     * @param id mã thành viên cần xem
     * @return đối tượng Person đầy đủ thông tin
     */
    public Person handleViewDetail(int id) {
        try {
            Person person = familyTreeService.getPersonById(id);
            if (navigator != null) {
                navigator.navigateToPersonDetail(person);
            }
            return person;
        } catch (PersonNotFoundException e) {
            showErrorAlert("Không tìm thấy", "Không tìm thấy thông tin thành viên có ID = " + id);
        } catch (Exception e) {
            showErrorAlert("Lỗi", "Không thể tải hồ sơ chi tiết: " + e.getMessage());
        }
        return null;
    }

    /**
     * Gán cha với childId và fatherId cụ thể.
     *
     * @param childId  mã con
     * @param fatherId mã cha
     */
    public void handleAssignFather(int childId, int fatherId) {
        try {
            familyTreeService.assignFather(childId, fatherId);
            showInfoAlert("Thành công", "Đã gán cha thành công cho thành viên ID = " + childId);
        } catch (SelfParentException e) {
            showErrorAlert("Lỗi quan hệ", "Không thể tự gán mình làm cha: " + e.getMessage());
        } catch (CycleDetectedException e) {
            showErrorAlert("Lỗi chu trình", "Gán cha tạo chu trình vòng lặp trong cây gia phả: " + e.getMessage());
        } catch (PersonNotFoundException e) {
            showErrorAlert("Không tìm thấy", "Thành viên con hoặc cha không tồn tại: " + e.getMessage());
        } catch (InvalidParentGenderException e) {
            showErrorAlert("Lỗi giới tính vai trò", e.getMessage());
        } catch (SameParentException e) {
            showErrorAlert("Lỗi quan hệ", "Cha và Mẹ không thể là cùng một người: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            showErrorAlert("Lỗi dữ liệu", e.getMessage());
        } catch (Exception e) {
            showErrorAlert("Lỗi", "Không thể gán cha: " + e.getMessage());
        }
    }

    /**
     * Gán mẹ với childId và motherId cụ thể.
     *
     * @param childId  mã con
     * @param motherId mã mẹ
     */
    public void handleAssignMother(int childId, int motherId) {
        try {
            familyTreeService.assignMother(childId, motherId);
            showInfoAlert("Thành công", "Đã gán mẹ thành công cho thành viên ID = " + childId);
        } catch (SelfParentException e) {
            showErrorAlert("Lỗi quan hệ", "Không thể tự gán mình làm mẹ: " + e.getMessage());
        } catch (CycleDetectedException e) {
            showErrorAlert("Lỗi chu trình", "Gán mẹ tạo chu trình vòng lặp trong cây gia phả: " + e.getMessage());
        } catch (PersonNotFoundException e) {
            showErrorAlert("Không tìm thấy", "Thành viên con hoặc mẹ không tồn tại: " + e.getMessage());
        } catch (InvalidParentGenderException e) {
            showErrorAlert("Lỗi giới tính vai trò", e.getMessage());
        } catch (SameParentException e) {
            showErrorAlert("Lỗi quan hệ", "Cha và Mẹ không thể là cùng một người: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            showErrorAlert("Lỗi dữ liệu", e.getMessage());
        } catch (Exception e) {
            showErrorAlert("Lỗi", "Không thể gán mẹ: " + e.getMessage());
        }
    }

    /**
     * Gỡ quan hệ cha với childId cụ thể.
     *
     * @param childId mã thành viên con cần gỡ quan hệ cha
     */
    public void handleClearFather(int childId) {
        try {
            familyTreeService.clearFather(childId);
            showInfoAlert("Thành công", "Đã gỡ quan hệ cha thành công cho thành viên ID = " + childId);
        } catch (PersonNotFoundException e) {
            showErrorAlert("Không tìm thấy", "Thành viên với ID = " + childId + " không tồn tại: " + e.getMessage());
        } catch (Exception e) {
            showErrorAlert("Lỗi", "Không thể gỡ quan hệ cha: " + e.getMessage());
        }
    }

    /**
     * Gỡ quan hệ mẹ với childId cụ thể.
     *
     * @param childId mã thành viên con cần gỡ quan hệ mẹ
     */
    public void handleClearMother(int childId) {
        try {
            familyTreeService.clearMother(childId);
            showInfoAlert("Thành công", "Đã gỡ quan hệ mẹ thành công cho thành viên ID = " + childId);
        } catch (PersonNotFoundException e) {
            showErrorAlert("Không tìm thấy", "Thành viên với ID = " + childId + " không tồn tại: " + e.getMessage());
        } catch (Exception e) {
            showErrorAlert("Lỗi", "Không thể gỡ quan hệ mẹ: " + e.getMessage());
        }
    }

    /**
     * Xử lý xem cây gia phả trực quan (UC09).
     * Bắt buộc xác thực sự tồn tại của focusPerson trước khi hiển thị.
     */
    public void handleViewTree() {
        int idToFocus;
        if (selectedPerson != null) {
            idToFocus = selectedPerson.getId();
        } else if (peopleListSource != null && peopleListSource.getSelectedPerson() != null) {
            idToFocus = peopleListSource.getSelectedPerson().getId();
        } else {
            showWarningAlert("Thông báo", "Vui lòng chọn một thành viên làm trung tâm cây gia phả.");
            return;
        }

        handleViewTree(idToFocus);
    }

    /**
     * Overloaded: Xem cây gia phả với focusId cụ thể.
     *
     * @param focusId mã thành viên trung tâm cây gia phả
     * @return đối tượng Person trung tâm nếu tìm thấy
     */
    public Person handleViewTree(int focusId) {
        try {
            // Bước 1: Xác thực thành viên tồn tại theo sequence-family-tree.puml
            Person focusPerson = familyTreeService.getPersonById(focusId);

            // Bước 2: Điều hướng sang màn hình cây gia phả.
            // Màn hình cây gia phả tự tải toàn bộ danh sách (đồ thị liên kết đầy đủ) để dựng
            // sơ đồ 2D và danh sách phả hệ — vì vậy Controller KHÔNG cần truy vấn
            // thêm getAncestors()/getDescendants() ở đây (tránh truy vấn thừa).
            if (navigator != null) {
                navigator.navigateToFamilyTree(focusPerson);
            }
            return focusPerson;
        } catch (PersonNotFoundException e) {
            showErrorAlert("Không tìm thấy", "Không tìm thấy thành viên có ID = " + focusId);
        } catch (Exception e) {
            showErrorAlert("Lỗi hiển thị cây gia phả", "Không thể tải cấu trúc cây: " + e.getMessage());
        }
        return null;
    }

    // ============================================================
    // CÁC HÀM TIỆN ÍCH HỖ TRỢ CONTEXT & GIAO DIỆN
    // ============================================================

    /**
     * Kiểm tra BR5 và hiển thị Alert cảnh báo cho phép người dùng chọn
     * tiếp tục hoặc hủy thao tác.
     *
     * <p>BR5 là quy tắc mức CẢNH BÁO: nếu năm sinh của cha/mẹ không nhỏ hơn
     * năm sinh của con, ứng dụng KHÔNG chặn thao tác — chỉ hỏi lại người dùng.
     *
     * @param child  thành viên con (dùng để lấy năm sinh và họ tên)
     * @param father cha được gán (có thể null)
     * @param mother mẹ được gán (có thể null)
     * @return true nếu nên tiếp tục (không có cảnh báo, hoặc người dùng chọn Tiếp tục);
     *         false nếu người dùng chọn Hủy
     */
    private boolean confirmBirthYearWarning(Person child, Person father, Person mother) {
        List<String> warnings = familyTreeService.checkBirthYearWarning(child, father, mother);

        if (warnings.isEmpty()) {
            return true;
        }

        // Nếu chạy ngoài JavaFX Application Thread (ví dụ trong test tự động) →
        // không thể hiển thị Alert, nên cho phép tiếp tục thao tác.
        // Việc kiểm tra này phải đặt TRƯỚC khi tạo Alert, vì tạo Alert ngoài
        // FX thread sẽ ném IllegalStateException (JavaFX toolkit chưa khởi tạo).
        if (!javafx.application.Platform.isFxApplicationThread()) {
            System.out.println("[CẢNH BÁO BR5] " + String.join(" | ", warnings));
            return true;
        }

        StringBuilder message = new StringBuilder();
        message.append("Cảnh báo về năm sinh trong gia phả:\n\n");
        for (String warning : warnings) {
            message.append("• ").append(warning).append("\n");
        }
        message.append("\nBạn vẫn muốn tiếp tục thao tác này không?");

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Cảnh báo năm sinh (BR5)");
        alert.setHeaderText("Năm sinh của cha/mẹ không nhỏ hơn năm sinh của con");
        alert.setContentText(message.toString());
        ButtonType continueButton = new ButtonType("Tiếp tục", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(continueButton, cancelButton);

        return alert.showAndWait().filter(continueButton::equals).isPresent();
    }

    /**
     * Kiểm tra quan hệ cha có thay đổi so với bản gốc hay không.
     *
     * @param originalFather cha gốc (có thể null)
     * @param newFather      cha mới (có thể null)
     * @return true nếu quan hệ cha thay đổi
     */
    private boolean fatherChanged(Person originalFather, Person newFather) {
        if (newFather == null && originalFather == null) {
            return false;
        }
        if (newFather == null || originalFather == null) {
            return true;
        }
        return newFather.getId() != originalFather.getId();
    }

    /**
     * Kiểm tra quan hệ mẹ có thay đổi so với bản gốc hay không.
     *
     * @param originalMother mẹ gốc (có thể null)
     * @param newMother      mẹ mới (có thể null)
     * @return true nếu quan hệ mẹ thay đổi
     */
    private boolean motherChanged(Person originalMother, Person newMother) {
        if (newMother == null && originalMother == null) {
            return false;
        }
        if (newMother == null || originalMother == null) {
            return true;
        }
        return newMother.getId() != originalMother.getId();
    }

    /**
     * Đăng ký nguồn dữ liệu form nhập liệu hiện hành.
     *
     * <p>Được gọi bởi form khi được khởi tạo. Controller chỉ đọc dữ liệu qua
     * interface này — không cần biết lớp View cụ thể.
     *
     * @param formDataSource nguồn dữ liệu form, hoặc null để gỡ đăng ký
     */
    public void setFormDataSource(FormDataSource formDataSource) {
        this.formDataSource = formDataSource;
    }

    /**
     * Đăng ký nguồn dữ liệu màn hình danh sách hiện hành.
     *
     * @param peopleListSource nguồn dữ liệu danh sách, hoặc null để gỡ đăng ký
     */
    public void setPeopleListSource(PeopleListSource peopleListSource) {
        this.peopleListSource = peopleListSource;
    }

    /**
     * Đăng ký bộ điều hướng màn hình.
     *
     * @param navigator bộ điều hướng, hoặc null để gỡ đăng ký
     */
    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }

    public void setSelectedPerson(Person selectedPerson) {
        this.selectedPerson = selectedPerson;
    }

    private void showInfoAlert(String title, String content) {
        showAlert(Alert.AlertType.INFORMATION, title, content);
    }

    private void showWarningAlert(String title, String content) {
        showAlert(Alert.AlertType.WARNING, title, content);
    }

    private void showErrorAlert(String title, String content) {
        showAlert(Alert.AlertType.ERROR, title, content);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        if (!javafx.application.Platform.isFxApplicationThread()) {
            System.out.println("[" + type + "] " + title + ": " + content);
            return;
        }
        try {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        } catch (Throwable t) {
            System.out.println("[" + type + "] " + title + ": " + content);
        }
    }
}
