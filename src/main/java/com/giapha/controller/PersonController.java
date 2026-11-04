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
import com.giapha.view.FamilyTreeView;
import com.giapha.view.PeopleListView;
import com.giapha.view.PersonDetailView;
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
    private PersonDetailView personDetailView;
    private FamilyTreeView familyTreeView;

    // Trạng thái context phục vụ các sự kiện không tham số từ UI
    private Person selectedPerson;
    private String searchKeyword;
    private Integer targetChildId;
    private Integer targetFatherId;
    private Integer targetMotherId;
    private Integer focusPersonId;

    // Callbacks điều hướng màn hình
    private Runnable navigateToPeopleList;
    private Consumer<Person> navigateToPersonForm;
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

        // BR5: cảnh báo nếu năm sinh cha/mẹ không nhỏ hơn năm sinh con
        if (!confirmBirthYearWarning(newPerson, father, mother)) {
            return; // Người dùng chọn Hủy
        }

        handleAdd(newPerson);
    }

    /**
     * Overloaded: Xử lý thêm đối tượng Person cụ thể vào hệ thống.
     *
     * @param person đối tượng thành viên cần thêm
     * @return đối tượng Person đã được gán id, hoặc null nếu thao tác thất bại
     */
    public Person handleAdd(Person person) {
        try {
            Person added = familyTreeService.addPerson(person);

            // Copy ảnh đại diện vào thư mục images (sau khi có id để đặt tên file)
            if (personFormView != null) {
                String newPhotoPath = persistSelectedImage(
                        personFormView.getSelectedPhotoSourcePath(), added.getId());
                if (newPhotoPath != null) {
                    added.setPhotoPath(newPhotoPath);
                    familyTreeService.updatePerson(added);
                }
            }

            showInfoAlert("Thành công", "Đã thêm thành viên \"" + added.getFullName() + "\" vào gia phả thành công.");
            if (navigateToPeopleList != null) {
                navigateToPeopleList.run();
            }
            return added;
        } catch (InvalidBirthYearException e) {
            showErrorAlert("Lỗi năm sinh", e.getMessage());
        } catch (PersonNotFoundException e) {
            showErrorAlert("Không tìm thấy", "Phụ huynh chỉ định không tồn tại: " + e.getMessage());
        } catch (SelfParentException e) {
            showErrorAlert("Lỗi quan hệ", "Không thể tự gán mình làm cha hoặc mẹ: " + e.getMessage());
        } catch (CycleDetectedException e) {
            showErrorAlert("Lỗi chu trình", "Thao tác tạo chu trình trong cây gia phả: " + e.getMessage());
        } catch (SameParentException e) {
            showErrorAlert("Lỗi quan hệ", "Cha và Mẹ không thể là cùng một người: " + e.getMessage());
        } catch (InvalidParentGenderException e) {
            showErrorAlert("Lỗi giới tính vai trò", e.getMessage());
        } catch (IllegalArgumentException e) {
            showErrorAlert("Lỗi dữ liệu", e.getMessage());
        } catch (Exception e) {
            showErrorAlert("Lỗi", "Không thể thêm thành viên: " + e.getMessage());
        }
        return null;
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

        // BR5: cảnh báo nếu năm sinh cha/mẹ không nhỏ hơn năm sinh con.
        // Chỉ kiểm tra khi quan hệ cha/mẹ thực sự thay đổi so với bản gốc.
        if (fatherChanged(originalFather, newFather) || motherChanged(originalMother, newMother)) {
            if (!confirmBirthYearWarning(basicUpdatePerson, newFather, newMother)) {
                return; // Người dùng chọn Hủy
            }
        }

        handleEdit(basicUpdatePerson, newFather, newMother);
    }

    /**
     * Overloaded: Thực thi quy trình cập nhật thông tin và gán quan hệ cha mẹ.
     *
     * @param personToUpdate thông tin thành viên cập nhật (chứa cha mẹ gốc)
     * @param newFather      cha mới được chọn từ ComboBox
     * @param newMother      mẹ mới được chọn từ ComboBox
     */
    public void handleEdit(Person personToUpdate, Person newFather, Person newMother) {
        int personId = personToUpdate.getId();
        Person originalFather = personToUpdate.getFather();
        Person originalMother = personToUpdate.getMother();

        StringBuilder successMsg = new StringBuilder("Đã cập nhật thành công thông tin thành viên.");
        StringBuilder errorMsg = new StringBuilder();

        // Bước 3: Copy ảnh đại diện mới (nếu người dùng đã chọn ảnh khác).
        // Thực hiện TRƯỚC khi updatePerson() để đường dẫn tương đối của ảnh mới
        // được ghi vào DB ngay trong lần lưu đó (không lưu 2 lần).
        if (personFormView != null) {
            String sourcePhoto = personFormView.getSelectedPhotoSourcePath();
            String newPhotoPath = persistSelectedImage(sourcePhoto, personId);
            if (newPhotoPath != null) {
                personToUpdate.setPhotoPath(newPhotoPath);
                successMsg.append("\n• Đã cập nhật ảnh đại diện thành công.");
            }
        }

        // Bước 4: Cập nhật thông tin cơ bản qua updatePerson()
        try {
            familyTreeService.updatePerson(personToUpdate);
        } catch (InvalidBirthYearException e) {
            showErrorAlert("Lỗi năm sinh", e.getMessage());
            return;
        } catch (IllegalArgumentException e) {
            showErrorAlert("Lỗi dữ liệu", e.getMessage());
            return;
        } catch (Exception e) {
            showErrorAlert("Lỗi cập nhật", "Không thể cập nhật thông tin cơ bản: " + e.getMessage());
            return;
        }

        // Bước 4: Kiểm tra và gán/gỡ cha nếu có thay đổi qua assignFather() / clearFather()
        boolean fatherChanged =
                (newFather != null && (originalFather == null || newFather.getId() != originalFather.getId()))
                || (newFather == null && originalFather != null);
        if (fatherChanged) {
            try {
                if (newFather != null) {
                    familyTreeService.assignFather(personId, newFather.getId());
                    successMsg.append("\n• Đã cập nhật cha thành công.");
                } else {
                    familyTreeService.clearFather(personId);
                    successMsg.append("\n• Đã gỡ quan hệ cha thành công.");
                }
            } catch (SelfParentException e) {
                errorMsg.append("\n• Không thể tự gán mình làm cha: ").append(e.getMessage());
            } catch (CycleDetectedException e) {
                errorMsg.append("\n• Lỗi chu trình khi gán cha: ").append(e.getMessage());
            } catch (InvalidParentGenderException e) {
                errorMsg.append("\n• Lỗi giới tính vai trò cha: ").append(e.getMessage());
            } catch (SameParentException e) {
                errorMsg.append("\n• Cha trùng với mẹ hiện tại: ").append(e.getMessage());
            } catch (IllegalArgumentException e) {
                errorMsg.append("\n• Lỗi giới tính/vai trò cha: ").append(e.getMessage());
            } catch (PersonNotFoundException e) {
                errorMsg.append("\n• Không tìm thấy cha hoặc con chỉ định: ").append(e.getMessage());
            } catch (Exception e) {
                errorMsg.append("\n• Không thể cập nhật cha: ").append(e.getMessage());
            }
        }

        // Bước 5: Kiểm tra và gán/gỡ mẹ nếu có thay đổi qua assignMother() / clearMother()
        boolean motherChanged =
                (newMother != null && (originalMother == null || newMother.getId() != originalMother.getId()))
                || (newMother == null && originalMother != null);
        if (motherChanged) {
            try {
                if (newMother != null) {
                    familyTreeService.assignMother(personId, newMother.getId());
                    successMsg.append("\n• Đã cập nhật mẹ thành công.");
                } else {
                    familyTreeService.clearMother(personId);
                    successMsg.append("\n• Đã gỡ quan hệ mẹ thành công.");
                }
            } catch (SelfParentException e) {
                errorMsg.append("\n• Không thể tự gán mình làm mẹ: ").append(e.getMessage());
            } catch (CycleDetectedException e) {
                errorMsg.append("\n• Lỗi chu trình khi gán mẹ: ").append(e.getMessage());
            } catch (InvalidParentGenderException e) {
                errorMsg.append("\n• Lỗi giới tính vai trò mẹ: ").append(e.getMessage());
            } catch (SameParentException e) {
                errorMsg.append("\n• Mẹ trùng với cha hiện tại: ").append(e.getMessage());
            } catch (IllegalArgumentException e) {
                errorMsg.append("\n• Lỗi giới tính/vai trò mẹ: ").append(e.getMessage());
            } catch (PersonNotFoundException e) {
                errorMsg.append("\n• Không tìm thấy mẹ hoặc con chỉ định: ").append(e.getMessage());
            } catch (Exception e) {
                errorMsg.append("\n• Không thể cập nhật mẹ: ").append(e.getMessage());
            }
        }

        // Bước 6: Báo cáo kết quả
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
        try {
            familyTreeService.deletePerson(id);
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
     * Xử lý tìm kiếm thành viên theo từ khóa họ tên (UC04).
     */
    public void handleSearch() {
        String keyword = searchKeyword;
        if (keyword == null && peopleListView != null) {
            keyword = peopleListView.getSearchKeyword();
        }

        handleSearch(keyword);
    }

    /**
     * Overloaded: Tìm kiếm thành viên với từ khóa cụ thể.
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
     * Xử lý gán cha cho người con (UC07).
     */
    public void handleAssignFather() {
        if (targetChildId == null || targetFatherId == null) {
            showWarningAlert("Thông báo", "Vui lòng chọn đầy đủ thông tin người con và người cha.");
            return;
        }

        handleAssignFather(targetChildId, targetFatherId);
    }

    /**
     * Overloaded: Gán cha với childId và fatherId cụ thể.
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
     * Xử lý gán mẹ cho người con (UC08).
     */
    public void handleAssignMother() {
        if (targetChildId == null || targetMotherId == null) {
            showWarningAlert("Thông báo", "Vui lòng chọn đầy đủ thông tin người con và người mẹ.");
            return;
        }

        handleAssignMother(targetChildId, targetMotherId);
    }

    /**
     * Overloaded: Gán mẹ với childId và motherId cụ thể.
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
     * Xử lý gỡ quan hệ cha cho người con từ context giao diện.
     */
    public void handleClearFather() {
        if (targetChildId == null) {
            showWarningAlert("Thông báo", "Vui lòng chọn thông tin người con để gỡ quan hệ cha.");
            return;
        }
        handleClearFather(targetChildId);
    }

    /**
     * Overloaded: Gỡ quan hệ cha với childId cụ thể.
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
     * Xử lý gỡ quan hệ mẹ cho người con từ context giao diện.
     */
    public void handleClearMother() {
        if (targetChildId == null) {
            showWarningAlert("Thông báo", "Vui lòng chọn thông tin người con để gỡ quan hệ mẹ.");
            return;
        }
        handleClearMother(targetChildId);
    }

    /**
     * Overloaded: Gỡ quan hệ mẹ với childId cụ thể.
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
     * Bắt buộc xác thực sự tồn tại của focusPerson trước khi truy vấn tổ tiên và hậu duệ.
     */
    public void handleViewTree() {
        int idToFocus;
        if (focusPersonId != null) {
            idToFocus = focusPersonId;
        } else if (selectedPerson != null) {
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
     * Overloaded: Xem cây gia phả với focusPersonId cụ thể.
     *
     * @param focusId mã thành viên trung tâm cây gia phả
     * @return đối tượng Person trung tâm nếu tìm thấy
     */
    public Person handleViewTree(int focusId) {
        try {
            // Bước 1: Xác thực thành viên tồn tại theo sequence-family-tree.puml
            Person focusPerson = familyTreeService.getPersonById(focusId);

            // Bước 2: Tải tổ tiên và hậu duệ qua Service
            List<Person> ancestors = familyTreeService.getAncestors(focusId);
            List<Person> descendants = familyTreeService.getDescendants(focusId);

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

        // Nếu chạy ngoài JavaFX Application Thread (ví dụ trong test) → cho phép tiếp tục
        if (!javafx.application.Platform.isFxApplicationThread()) {
            return true;
        }

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
     * Copy ảnh đại diện do người dùng chọn vào thư mục images của ứng dụng.
     *
     * <p>Được gọi sau khi thành viên đã được lưu thành công (đã có id) để đặt tên file
     * theo mã định danh. Nếu copy thất bại, ảnh cũ trong DB vẫn được giữ nguyên.
     *
     * @param sourceFilePath đường dẫn tuyệt đối tới ảnh nguồn
     * @param personId       mã định danh của thành viên
     * @return đường dẫn tương đối cần lưu vào DB, hoặc null nếu không có ảnh nào được chọn
     */
    private String persistSelectedImage(String sourceFilePath, int personId) {
        if (sourceFilePath == null || sourceFilePath.trim().isEmpty()) {
            return null;
        }

        if (!ImageManager.isAllowedImage(sourceFilePath)) {
            showErrorAlert("Định dạng ảnh không hợp lệ",
                    "Chỉ chấp nhận các định dạng ảnh: png, jpg, jpeg.");
            return null;
        }

        try {
            return ImageManager.copyImageToAppDirectory(sourceFilePath, personId);
        } catch (IOException e) {
            showErrorAlert("Lỗi ảnh đại diện", "Không thể lưu ảnh vào thư mục images: " + e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            showErrorAlert("Định dạng ảnh không hợp lệ", e.getMessage());
            return null;
        }
    }

    public void setPeopleListView(PeopleListView peopleListView) {
        this.peopleListView = peopleListView;
    }

    public void setPersonFormView(PersonFormView personFormView) {
        this.personFormView = personFormView;
    }

    public void setPersonDetailView(PersonDetailView personDetailView) {
        this.personDetailView = personDetailView;
    }

    public void setFamilyTreeView(FamilyTreeView familyTreeView) {
        this.familyTreeView = familyTreeView;
    }

    public void setSelectedPerson(Person selectedPerson) {
        this.selectedPerson = selectedPerson;
    }

    public void setSearchKeyword(String searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    public void setTargetChildId(Integer targetChildId) {
        this.targetChildId = targetChildId;
    }

    public void setTargetFatherId(Integer targetFatherId) {
        this.targetFatherId = targetFatherId;
    }

    public void setTargetMotherId(Integer targetMotherId) {
        this.targetMotherId = targetMotherId;
    }

    public void setFocusPersonId(Integer focusPersonId) {
        this.focusPersonId = focusPersonId;
    }

    public void setNavigateToPeopleList(Runnable navigateToPeopleList) {
        this.navigateToPeopleList = navigateToPeopleList;
    }

    public void setNavigateToPersonForm(Consumer<Person> navigateToPersonForm) {
        this.navigateToPersonForm = navigateToPersonForm;
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
