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
import com.giapha.view.PeopleListView;
import com.giapha.view.PersonFormView;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Controller điều phối luồng nghiệp vụ giữa giao diện JavaFX và FamilyTreeService.
 * Tuân thủ nghiêm ngặt nguyên tắc kiến trúc phân tầng:
 * - Chỉ phụ thuộc duy nhất vào FamilyTreeService.
 * - Không truy cập trực tiếp Repository hay JDBC.
 * - Không duplicate quy tắc nghiệp vụ/validation.
 * - Bắt và xử lý các custom exception để hiển thị thông báo thân thiện bằng tiếng Việt.
 */
public class PersonController {

    private final FamilyTreeService familyTreeService;

    // Tham chiếu tới các View hiện hành (nếu có)
    private PeopleListView peopleListView;
    private PersonFormView personFormView;

    // Thành viên được chọn trên danh sách — dùng làm ngữ cảnh mặc định
    // cho các handler không tham số (handleEdit / handleDelete / handleViewDetail).
    private Person selectedPerson;

    // Callbacks điều hướng màn hình
    private Runnable navigateToPeopleList;
    private Consumer<Person> navigateToPersonDetail;
    private Consumer<Person> navigateToFamilyTree;

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
        if (personFormView == null) {
            showWarningAlert("Thông báo", "Không tìm thấy giao diện nhập liệu thành viên.");
            return;
        }

        String fullName = personFormView.getFullNameInput();
        String birthYearStr = personFormView.getBirthYearInput();
        Gender gender = personFormView.getGenderInput();
        Person father = personFormView.getSelectedFather();
        Person mother = personFormView.getSelectedMother();

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
        // luồng lưu thực tế từ PersonFormView đi qua (không phải chỉ ở handleAdd()).
        handleAdd(newPerson);
    }

    /**
     * Overloaded: Xử lý thêm đối tượng Person cụ thể vào hệ thống.
     *
     * <p><b>Đây là phương thức lưu THỰC SỰ của luồng UI</b> — {@link PersonFormView}
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

        if (navigateToPeopleList != null) {
            navigateToPeopleList.run();
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
        return (personFormView == null) ? null : personFormView.getSelectedPhotoSourcePath();
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
        if (personFormView != null && personFormView.getEditingPerson() != null) {
            target = personFormView.getEditingPerson();
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
        String newFullName = personFormView != null ? personFormView.getFullNameInput() : target.getFullName();
        String newBirthYearStr = personFormView != null ? personFormView.getBirthYearInput() : String.valueOf(target.getBirthYear());
        Gender newGender = personFormView != null ? personFormView.getGenderInput() : target.getGender();
        // Ảnh đại diện: luôn giữ đường dẫn TƯƠNG ĐỐI đã lưu trong DB.
        // Nếu người dùng chọn ảnh mới, ảnh đó được copy + sinh đường dẫn mới trong
        // handleEdit(Person, Person, Person) trước khi ghi xuống DB.
        String newPhotoPath = personFormView != null
                ? personFormView.getExistingPhotoPath()
                : target.getPhotoPath();
        Person newFather = personFormView != null ? personFormView.getSelectedFather() : target.getFather();
        Person newMother = personFormView != null ? personFormView.getSelectedMother() : target.getMother();

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
        // thực tế từ PersonFormView đi qua. Ở đây KHÔNG kiểm tra lại để tránh
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
     * <p><b>Đây là phương thức lưu THỰC SỰ của luồng UI</b> — {@link PersonFormView}
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

        StringBuilder successMsg = new StringBuilder("Đã cập nhật thành công thông tin thành viên.");
        StringBuilder errorMsg = new StringBuilder();

        // Bước 1: Copy ảnh đại diện mới (nếu người dùng đã chọn ảnh khác).
        // Chỉ copy FILE — đường dẫn được ghi vào DB ở Bước 4 bằng updatePhotoPath()
        // (chỉ cập nhật cột photo_path), tách khỏi bản ghi thông tin cá nhân.
        String newPhotoPath = null;
        String imageError = null;
        String sourcePhoto = getSelectedPhotoSourcePath();
        if (sourcePhoto != null && !sourcePhoto.trim().isEmpty()) {
            try {
                newPhotoPath = ImageManager.copyImageToAppDirectory(sourcePhoto, personId);
            } catch (IOException e) {
                imageError = "Không thể lưu ảnh vào thư mục images (" + e.getMessage() + ").";
            } catch (IllegalArgumentException e) {
                imageError = "Định dạng ảnh không hợp lệ (" + e.getMessage() + ").";
            }
        }

        // Bước 2: BR5 — cảnh báo năm sinh cha/mẹ không nhỏ hơn năm sinh con.
        // Chỉ hỏi khi quan hệ cha/mẹ THỰC SỰ thay đổi so với bản gốc, và chỉ hỏi
        // với các quan hệ MỚI được chọn.
        if (fatherChanged(originalFather, newFather) || motherChanged(originalMother, newMother)) {
            Person effectiveFather = newFather != null ? newFather : originalFather;
            Person effectiveMother = newMother != null ? newMother : originalMother;
            if (!confirmBirthYearWarning(personToUpdate, effectiveFather, effectiveMother)) {
                // Người dùng chọn Hủy — KHÔNG lưu gì cả, kể cả ảnh đã copy.
                if (newPhotoPath != null && !newPhotoPath.equals(originalPhotoPath)) {
                    ImageManager.deleteImageQuietly(newPhotoPath);
                }
                return; // Người dùng chọn Hủy — KHÔNG lưu gì cả
            }
        }

        // Bước 3: Cập nhật NGUYÊN TỬ — kiểm tra toàn bộ BR trước, rồi ghi MỘT lần.
        // photo_path KHÔNG tham gia vào bản ghi này — giữ nguyên đường dẫn cũ,
        // việc cập nhật ảnh được thực hiện riêng ở Bước 4.
        try {
            familyTreeService.updatePersonWithParents(personToUpdate, newFather, newMother);

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
        } catch (SelfParentException e) {
            errorMsg.append("\n• Không thể tự gán mình làm cha/mẹ: ").append(e.getMessage());
        } catch (CycleDetectedException e) {
            errorMsg.append("\n• Lỗi chu trình khi gán cha/mẹ: ").append(e.getMessage());
        } catch (InvalidParentGenderException e) {
            errorMsg.append("\n• Lỗi giới tính vai trò cha/mẹ: ").append(e.getMessage());
        } catch (SameParentException e) {
            errorMsg.append("\n• Cha và Mẹ không thể là cùng một người: ").append(e.getMessage());
        } catch (PersonNotFoundException e) {
            errorMsg.append("\n• Không tìm thấy cha, mẹ hoặc con chỉ định: ").append(e.getMessage());
        } catch (InvalidBirthYearException e) {
            showErrorAlert("Lỗi năm sinh", e.getMessage());
            if (newPhotoPath != null && !newPhotoPath.equals(originalPhotoPath)) {
                ImageManager.deleteImageQuietly(newPhotoPath);
            }
            return;
        } catch (IllegalArgumentException e) {
            showErrorAlert("Lỗi dữ liệu", e.getMessage());
            if (newPhotoPath != null && !newPhotoPath.equals(originalPhotoPath)) {
                ImageManager.deleteImageQuietly(newPhotoPath);
            }
            return;
        } catch (Exception e) {
            showErrorAlert("Lỗi cập nhật", "Không thể cập nhật thông tin thành viên: " + e.getMessage());
            if (newPhotoPath != null && !newPhotoPath.equals(originalPhotoPath)) {
                ImageManager.deleteImageQuietly(newPhotoPath);
            }
            return;
        }

        // Bước 4: Cập nhật ảnh đại diện — CHỈ ghi cột photo_path.
        // Tách khỏi Bước 3 để lỗi ảnh không làm hỏng việc cập nhật thông tin
        // thành viên đã thành công.
        if (newPhotoPath != null) {
            try {
                familyTreeService.updatePhotoPath(personId, newPhotoPath);
                personToUpdate.setPhotoPath(newPhotoPath);
                successMsg.append("\n• Đã cập nhật ảnh đại diện thành công.");

                // Dọn file ảnh CŨ nếu đường dẫn đã đổi (ảnh mới có tên file khác).
                if (originalPhotoPath != null && !originalPhotoPath.equals(newPhotoPath)) {
                    ImageManager.deleteImageQuietly(originalPhotoPath);
                }
            } catch (Exception e) {
                errorMsg.append("\n• Không thể cập nhật ảnh đại diện trong cơ sở dữ liệu: ")
                        .append(e.getMessage());
            }
        } else if (imageError != null) {
            errorMsg.append("\n• ").append(imageError);
        }

        // Bước 5: Báo cáo kết quả
        if (errorMsg.length() > 0) {
            showErrorAlert("Cảnh báo quan hệ", successMsg.toString() + "\n" + errorMsg.toString());
        } else {
            showInfoAlert("Thành công", successMsg.toString());
        }

        if (navigateToPeopleList != null) {
            navigateToPeopleList.run();
        }
    }

    /**
     * Xử lý xóa thành viên khỏi gia phả (UC03).
     */
    public void handleDelete() {
        Person target = selectedPerson;
        if (target == null && peopleListView != null) {
            target = peopleListView.getSelectedPerson();
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
            if (peopleListView != null) {
                peopleListView.refresh();
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
        if (target == null && peopleListView != null) {
            target = peopleListView.getSelectedPerson();
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
            if (navigateToPersonDetail != null) {
                navigateToPersonDetail.accept(person);
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
        } else if (peopleListView != null && peopleListView.getSelectedPerson() != null) {
            idToFocus = peopleListView.getSelectedPerson().getId();
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
            // FamilyTreeView tự tải toàn bộ danh sách (đồ thị liên kết đầy đủ) để dựng
            // sơ đồ 2D và danh sách phả hệ — vì vậy Controller KHÔNG cần truy vấn
            // thêm getAncestors()/getDescendants() ở đây (tránh truy vấn thừa).
            if (navigateToFamilyTree != null) {
                navigateToFamilyTree.accept(focusPerson);
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

    public void setPeopleListView(PeopleListView peopleListView) {
        this.peopleListView = peopleListView;
    }

    public void setPersonFormView(PersonFormView personFormView) {
        this.personFormView = personFormView;
    }

    public void setSelectedPerson(Person selectedPerson) {
        this.selectedPerson = selectedPerson;
    }

    public void setNavigateToPeopleList(Runnable navigateToPeopleList) {
        this.navigateToPeopleList = navigateToPeopleList;
    }

    public void setNavigateToPersonDetail(Consumer<Person> navigateToPersonDetail) {
        this.navigateToPersonDetail = navigateToPersonDetail;
    }

    public void setNavigateToFamilyTree(Consumer<Person> navigateToFamilyTree) {
        this.navigateToFamilyTree = navigateToFamilyTree;
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
