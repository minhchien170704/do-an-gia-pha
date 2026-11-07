package com.giapha.view;

import com.giapha.controller.FormDataSource;
import com.giapha.controller.PersonController;
import com.giapha.model.Gender;
import com.giapha.model.Person;
import com.giapha.util.ImageManager;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Form Thêm/Sửa thành viên — Person Form View.
 * Nạp danh sách cha/mẹ từ MySQL thông qua PersonController.
 * Thao tác lưu gọi controller.handleAdd() hoặc controller.handleEdit(),
 * controller xử lý validation nghiệp vụ và điều hướng sau khi thành công.
 *
 * <p>Lớp này hiện thực {@link FormDataSource} — abstraction mà
 * {@link PersonController} dùng để đọc dữ liệu người dùng đang nhập.
 * Nhờ đó Controller không cần biết (và không import) lớp View cụ thể nào.
 */
public class PersonFormView implements FormDataSource {

    private final BorderPane root;

    private TextField txtFullName;
    private TextField txtBirthYear;
    private ComboBox<Gender> cboGender;
    private TextField txtPhotoPath;
    private Button btnChoosePhoto;
    private Label lblPhotoName;
    private ComboBox<Person> cboFather;
    private ComboBox<Person> cboMother;

    /**
     * Khung xem trước ảnh đại diện (dùng chung component {@link PersonAvatar}).
     */
    private PersonAvatar avatarPreview;

    /**
     * Đường dẫn file ảnh nguồn do người dùng chọn qua FileChooser.
     * Được xử lý (copy vào thư mục images) bởi PersonController.
     */
    private String selectedPhotoSourcePath;

    /**
     * File ảnh gốc đang hiển thị trong khung xem trước (đường dẫn tuyệt đối).
     */
    private File selectedPhotoFile;

    private final PersonController controller;
    private final Person editingPerson;

    private Runnable onCancel;

    public PersonFormView(PersonController controller) {
        this(controller, null);
    }

    public PersonFormView(PersonController controller, Person personToEdit) {
        this.controller = controller;
        this.editingPerson = personToEdit;
        root = new BorderPane();
        root.getStyleClass().add("content-area");

        // Đăng ký chính form này với Controller.
        // Nhờ vậy Controller có thể ĐỌC dữ liệu người dùng đang nhập (họ tên, năm sinh,
        // giới tính, cha/mẹ đã chọn) khi xử lý lưu — bao gồm cả bước kiểm tra BR5
        // (cảnh báo năm sinh cha/mẹ) trong luồng lưu thực tế từ giao diện.
        // Controller chỉ biết interface FormDataSource, không biết lớp View này.
        controller.setFormDataSource(this);

        VBox topHeader = createHeader();
        root.setTop(topHeader);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox formContainer = new VBox(20);
        formContainer.setMaxWidth(620);
        formContainer.setAlignment(Pos.TOP_CENTER);
        formContainer.setPadding(new Insets(10, 0, 32, 0));

        VBox formCard = buildFormCard();
        formContainer.getChildren().add(formCard);

        HBox centerWrapper = new HBox(formContainer);
        centerWrapper.setAlignment(Pos.TOP_CENTER);
        scrollPane.setContent(centerWrapper);

        root.setCenter(scrollPane);

        // Nạp danh sách cha/mẹ từ MySQL
        refreshComboBoxes();

        // Nếu là chế độ sửa, điền dữ liệu hiện tại vào form
        if (editingPerson != null) {
            fillFormData();
        }

        updateAvatarPreview();
    }

    private VBox buildFormCard() {
        VBox formCard = new VBox(16);
        formCard.getStyleClass().add("card");
        formCard.setPadding(new Insets(24, 28, 28, 28));

        // --- Thông tin cá nhân ---
        Label secPersonal = new Label("Thông tin cá nhân");
        secPersonal.getStyleClass().add("section-title");

        HBox photoSection = createPhotoSection();

        VBox nameGroup = new VBox(4);
        Label lblName = new Label("Họ và tên *");
        lblName.getStyleClass().add("form-label");
        txtFullName = new TextField();
        txtFullName.getStyleClass().add("form-field");
        txtFullName.setPromptText("Nhập họ và tên đầy đủ");
        txtFullName.textProperty().addListener((obs, o, n) -> updateAvatarPreview());
        nameGroup.getChildren().addAll(lblName, txtFullName);

        HBox birthAndGender = new HBox(16);

        VBox yearGroup = new VBox(4);
        Label lblYear = new Label("Năm sinh *");
        lblYear.getStyleClass().add("form-label");
        txtBirthYear = new TextField();
        txtBirthYear.getStyleClass().add("form-field");
        txtBirthYear.setPromptText("VD: 1990");
        yearGroup.getChildren().addAll(lblYear, txtBirthYear);
        HBox.setHgrow(yearGroup, Priority.ALWAYS);

        VBox genderGroup = new VBox(4);
        Label lblGender = new Label("Giới tính *");
        lblGender.getStyleClass().add("form-label");
        cboGender = new ComboBox<>(FXCollections.observableArrayList(Gender.values()));
        cboGender.getStyleClass().add("form-field");
        cboGender.setMaxWidth(Double.MAX_VALUE);
        cboGender.setPromptText("Chọn giới tính");
        cboGender.setConverter(new StringConverter<>() {
            @Override public String toString(Gender g) { return g == null ? "" : g.getDisplayName(); }
            @Override public Gender fromString(String s) {
                for (Gender g : Gender.values()) {
                    if (g.getDisplayName().equals(s)) {
                        return g;
                    }
                }
                return Gender.FEMALE;
            }
        });
        cboGender.valueProperty().addListener((obs, o, n) -> updateAvatarPreview());
        genderGroup.getChildren().addAll(lblGender, cboGender);
        HBox.setHgrow(genderGroup, Priority.ALWAYS);

        birthAndGender.getChildren().addAll(yearGroup, genderGroup);

        // --- Quan hệ gia đình ---
        Separator sep = new Separator();
        sep.getStyleClass().add("form-separator");

        Label secFamily = new Label("Quan hệ gia đình");
        secFamily.getStyleClass().add("section-title");

        VBox fatherGroup = new VBox(4);
        Label lblFather = new Label("Cha");
        lblFather.getStyleClass().add("form-label");
        cboFather = new ComboBox<>();
        cboFather.getStyleClass().add("form-field");
        cboFather.setMaxWidth(Double.MAX_VALUE);
        cboFather.setPromptText("Chọn cha (tùy chọn)");
        setupPersonComboBox(cboFather);
        fatherGroup.getChildren().addAll(lblFather, cboFather);

        VBox motherGroup = new VBox(4);
        Label lblMother = new Label("Mẹ");
        lblMother.getStyleClass().add("form-label");
        cboMother = new ComboBox<>();
        cboMother.getStyleClass().add("form-field");
        cboMother.setMaxWidth(Double.MAX_VALUE);
        cboMother.setPromptText("Chọn mẹ (tùy chọn)");
        setupPersonComboBox(cboMother);
        motherGroup.getChildren().addAll(lblMother, cboMother);

        // --- Buttons ---
        HBox buttonBar = new HBox(12);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(12, 0, 0, 0));

        Button btnCancel = new Button("Hủy");
        btnCancel.getStyleClass().add("btn-secondary");
        btnCancel.setOnAction(e -> { if (onCancel != null) onCancel.run(); });

        String saveText = editingPerson != null ? "Lưu thay đổi" : "Lưu thành viên";
        Button btnSave = new Button(saveText);
        btnSave.getStyleClass().add("btn-primary");
        btnSave.setOnAction(e -> handleSave());

        buttonBar.getChildren().addAll(btnCancel, btnSave);

        formCard.getChildren().addAll(
                secPersonal, photoSection, nameGroup, birthAndGender,
                sep, secFamily, fatherGroup, motherGroup,
                buttonBar
        );

        return formCard;
    }

    private VBox createHeader() {
        Button btnBack = new Button("← Quay lại");
        btnBack.getStyleClass().add("back-link");
        btnBack.setOnAction(e -> { if (onCancel != null) onCancel.run(); });

        String titleText = editingPerson != null ? "Chỉnh sửa thành viên" : "Thêm thành viên";
        Label title = new Label(titleText);
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Nhập thông tin của thành viên trong gia đình");
        subtitle.getStyleClass().add("page-subtitle");

        VBox header = new VBox(6, btnBack, title, subtitle);
        header.setPadding(new Insets(0, 0, 10, 0));
        return header;
    }

    private HBox createPhotoSection() {
        HBox box = new HBox(16);
        box.setAlignment(Pos.CENTER_LEFT);

        // Khung xem trước ảnh — dùng chung component PersonAvatar để hiển thị
        // ảnh thật bo tròn, tự fallback về chữ cái đầu khi thiếu ảnh / ảnh hỏng.
        avatarPreview = new PersonAvatar(null, 60);

        VBox photoInputBox = new VBox(6);
        Label lblPhoto = new Label("Ảnh đại diện");
        lblPhoto.getStyleClass().add("form-label");

        // Nút chọn ảnh từ hệ thống bằng FileChooser
        btnChoosePhoto = new Button("Chọn ảnh...");
        btnChoosePhoto.getStyleClass().add("btn-secondary");
        btnChoosePhoto.setOnAction(e -> handleChoosePhoto());

        // TextField chỉ để hiển thị đường dẫn ảnh đã chọn (không cho gõ tay)
        txtPhotoPath = new TextField();
        txtPhotoPath.getStyleClass().add("form-field");
        txtPhotoPath.setEditable(false);
        txtPhotoPath.setPromptText("Chưa chọn ảnh (png, jpg, jpeg)");

        // Nhãn hiển thị tên file nguồn đã chọn
        lblPhotoName = new Label();
        lblPhotoName.getStyleClass().add("stat-label");
        lblPhotoName.setWrapText(true);

        HBox photoRow = new HBox(8, txtPhotoPath, btnChoosePhoto);
        photoRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(txtPhotoPath, Priority.ALWAYS);

        photoInputBox.getChildren().addAll(lblPhoto, photoRow, lblPhotoName);
        HBox.setHgrow(photoInputBox, Priority.ALWAYS);

        box.getChildren().addAll(avatarPreview, photoInputBox);
        return box;
    }

    /**
     * Mở hộp thoại chọn file để người dùng chọn ảnh đại diện.
     * Chỉ hiển thị các định dạng png / jpg / jpeg và kiểm tra lại sau khi chọn.
     */
    private void handleChoosePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh đại diện");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(ImageManager.IMAGE_FILTER,
                        "*.png", "*.jpg", "*.jpeg"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Tất cả các file", "*.*"));

        File chosen = fileChooser.showOpenDialog(root.getScene() == null ? null : root.getScene().getWindow());
        if (chosen == null) {
            return; // Người dùng bấm Cancel
        }

        if (!ImageManager.isAllowedImage(chosen.getName())) {
            showAlert("Định dạng ảnh không hợp lệ. Chỉ chấp nhận các định dạng: png, jpg, jpeg.");
            return;
        }

        selectedPhotoSourcePath = chosen.getAbsolutePath();
        selectedPhotoFile = chosen;
        txtPhotoPath.setText(chosen.getAbsolutePath());
        lblPhotoName.setText("Đã chọn: " + chosen.getName());
        updateAvatarPreview();
    }

    /**
     * Cập nhật khung xem trước ảnh đại diện.
     *
     * <p>Dùng chung component {@link PersonAvatar}: nếu đã chọn ảnh thì hiển thị ảnh
     * thật bo tròn, ngược lại hiển thị chữ cái đầu của họ tên với màu tương ứng
     * giới tính. File ảnh hỏng / không tồn tại → fallback về placeholder, không ném exception.
     */
    private void updateAvatarPreview() {
        if (avatarPreview == null) return;

        // Dựng Person tạm để PersonAvatar lấy tên + giới tính cho placeholder
        String name = txtFullName != null ? txtFullName.getText().trim() : "";
        Gender gender = cboGender != null ? cboGender.getValue() : null;

        Person previewPerson = new Person(name.isEmpty() ? "?" : name, 1900,
                gender != null ? gender : Gender.MALE, null);

        // Nếu đã chọn ảnh → hiển thị file đó; ngược lại dùng ảnh đang lưu trong DB
        // (trường hợp chỉnh sửa thành viên đã có ảnh).
        File fileToShow = selectedPhotoFile;
        if (fileToShow == null && editingPerson != null) {
            fileToShow = PersonAvatar.resolveImageFile(editingPerson.getPhotoPath());
        }

        PersonAvatar updated = new PersonAvatar(previewPerson, 60, fileToShow);

        // Thay thế nội dung khung xem trước (giữ nguyên vị trí trong layout)
        avatarPreview.getChildren().setAll(updated.getChildren());
        avatarPreview.setStyle(updated.getStyle());
        avatarPreview.setPrefSize(60, 60);
        avatarPreview.setMinSize(60, 60);
        avatarPreview.setMaxSize(60, 60);
    }

    private void setupPersonComboBox(ComboBox<Person> comboBox) {
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Person person) {
                if (person == null) return "— Không có —";
                String genderStr = person.getGender().getDisplayName();
                return person.getFullName() + " (" + person.getBirthYear() + ", " + genderStr + ")";
            }

            @Override
            public Person fromString(String string) {
                return null;
            }
        });
    }

    /**
     * Nạp danh sách cha/mẹ từ MySQL thông qua Controller.
     * Lọc đúng giới tính và loại bỏ bản thân khỏi danh sách.
     * Thêm option null ("— Không có —") ở đầu mỗi danh sách để cho phép bỏ chọn.
     */
    private void refreshComboBoxes() {
        List<Person> allPersons = controller.getAllPersons();
        if (allPersons == null) allPersons = List.of();

        final int editingId = editingPerson != null ? editingPerson.getId() : -1;

        List<Person> males = allPersons.stream()
                .filter(p -> p.getGender() == Gender.MALE && p.getId() != editingId)
                .collect(Collectors.toList());

        List<Person> females = allPersons.stream()
                .filter(p -> p.getGender() == Gender.FEMALE && p.getId() != editingId)
                .collect(Collectors.toList());

        List<Person> fatherOptions = new ArrayList<>();
        fatherOptions.add(null);     // "— Không có —"
        fatherOptions.addAll(males);

        List<Person> motherOptions = new ArrayList<>();
        motherOptions.add(null);     // "— Không có —"
        motherOptions.addAll(females);

        cboFather.setItems(FXCollections.observableArrayList(fatherOptions));
        cboMother.setItems(FXCollections.observableArrayList(motherOptions));
    }

    /**
     * Điền dữ liệu hiện tại vào form khi chỉnh sửa.
     * Khớp theo ID (không phải object identity) vì Person từ ComboBox và từ DB
     * là các instance khác nhau.
     */
    private void fillFormData() {
        if (editingPerson == null) return;
        txtFullName.setText(editingPerson.getFullName());
        txtBirthYear.setText(String.valueOf(editingPerson.getBirthYear()));
        cboGender.setValue(editingPerson.getGender());

        // Nạp ảnh đại diện hiện tại (đường dẫn tương đối trong DB) để hiển thị xem trước
        if (txtPhotoPath != null) {
            String photoPath = editingPerson.getPhotoPath();
            txtPhotoPath.setText(photoPath != null ? photoPath : "");

            if (photoPath != null && !photoPath.trim().isEmpty()) {
                try {
                    Path imagePath = Paths.get(System.getProperty("user.dir")).resolve(photoPath);
                    if (Files.isRegularFile(imagePath)) {
                        selectedPhotoFile = imagePath.toFile();
                        lblPhotoName.setText("Ảnh hiện tại: " + selectedPhotoFile.getName());
                    }
                } catch (Exception ignored) {
                    // Ảnh không tồn tại trên đĩa — vẫn hiển thị đường dẫn trong ô text
                }
            }
        }

        // Khớp cha theo ID
        if (editingPerson.getFather() != null) {
            int fatherId = editingPerson.getFather().getId();
            cboFather.getItems().stream()
                    .filter(p -> p != null && p.getId() == fatherId)
                    .findFirst()
                    .ifPresent(cboFather::setValue);
        }

        // Khớp mẹ theo ID
        if (editingPerson.getMother() != null) {
            int motherId = editingPerson.getMother().getId();
            cboMother.getItems().stream()
                    .filter(p -> p != null && p.getId() == motherId)
                    .findFirst()
                    .ifPresent(cboMother::setValue);
        }
    }

    /**
     * Xử lý lưu: gọi Controller để lưu vào MySQL.
     *
     * <p>Chế độ thêm: {@code controller.handleAdd()} — Controller đọc dữ liệu từ chính
     * form này, validate, kiểm tra BR5 (cảnh báo năm sinh cha/mẹ), rồi
     * Service → Repository → MySQL.
     * <p>Chế độ sửa: {@code controller.handleEdit()} — tương tự, kiểm tra BR5 khi
     * quan hệ cha/mẹ thay đổi, rồi cập nhật NGUYÊN TỬ (thông tin cá nhân + cha + mẹ
     * trong một lần UPDATE).
     *
     * <p>Điều hướng sau khi thành công do Controller xử lý qua navigateToPeopleList callback.
     * Nếu có lỗi nghiệp vụ (SelfParentException, CycleDetectedException, v.v.),
     * Controller hiện Alert và User ở lại form để sửa.
     */
    private void handleSave() {
        if (editingPerson != null) {
            // --- Chế độ sửa ---
            // Controller tự đọc tên / năm sinh / giới tính / cha / mẹ đang chọn trên form,
            // so sánh với bản gốc trong DB, kiểm tra BR5 và cập nhật nguyên tử.
            controller.handleEdit();
        } else {
            // --- Chế độ thêm mới ---
            // Controller tự đọc dữ liệu từ form và kiểm tra BR5 trước khi lưu.
            controller.handleAdd();
        }

        // Navigation sau thành công được controller xử lý (không cần gọi onSave ở đây)
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public BorderPane getRoot() {
        return root;
    }

    public void setOnCancel(Runnable handler) {
        this.onCancel = handler;
    }

    @Override
    public Person getEditingPerson() { return editingPerson; }
    @Override
    public String getFullNameInput() { return txtFullName != null ? txtFullName.getText() : ""; }
    @Override
    public String getBirthYearInput() { return txtBirthYear != null ? txtBirthYear.getText() : ""; }
    @Override
    public Gender getGenderInput() { return cboGender != null ? cboGender.getValue() : null; }
    @Override
    public Person getSelectedFather() { return cboFather != null ? cboFather.getValue() : null; }
    @Override
    public Person getSelectedMother() { return cboMother != null ? cboMother.getValue() : null; }

    /**
     * Trả về đường dẫn tuyệt đối của file ảnh nguồn mà người dùng vừa chọn qua FileChooser.
     *
     * <p>Trả về {@code null} nếu người dùng chưa chọn ảnh mới — khi đó ảnh đại diện
     * hiện tại của thành viên (nếu có) được giữ nguyên.
     *
     * @return đường dẫn tuyệt đối tới ảnh nguồn, hoặc null
     */
    @Override
    public String getSelectedPhotoSourcePath() { return selectedPhotoSourcePath; }

    /**
     * Trả về đường dẫn ảnh đại diện hiện tại đã lưu trong DB (đường dẫn tương đối).
     *
     * <p>Dùng khi chỉnh sửa mà người dùng KHÔNG chọn ảnh mới — giữ nguyên ảnh cũ.
     *
     * @return đường dẫn tương đối trong DB, hoặc null nếu thành viên chưa có ảnh
     */
    @Override
    public String getExistingPhotoPath() {
        return editingPerson != null ? editingPerson.getPhotoPath() : null;
    }
}
