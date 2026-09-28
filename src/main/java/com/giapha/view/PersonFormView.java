package com.giapha.view;

import com.giapha.controller.PersonController;
import com.giapha.model.Gender;
import com.giapha.model.Person;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Form Thêm/Sửa thành viên — Person Form View.
 * Nạp danh sách cha/mẹ từ MySQL thông qua PersonController.
 * Thao tác lưu gọi controller.handleAdd() hoặc controller.handleEdit(),
 * controller xử lý validation nghiệp vụ và điều hướng sau khi thành công.
 */
public class PersonFormView {

    private final BorderPane root;

    private TextField txtFullName;
    private TextField txtBirthYear;
    private ComboBox<Gender> cboGender;
    private TextField txtPhotoPath;
    private ComboBox<Person> cboFather;
    private ComboBox<Person> cboMother;

    private StackPane avatarPreview;
    private Label avatarLetter;

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
            @Override public String toString(Gender g) { return g == null ? "" : g == Gender.MALE ? "Nam" : "Nữ"; }
            @Override public Gender fromString(String s) { return "Nam".equals(s) ? Gender.MALE : Gender.FEMALE; }
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

        avatarPreview = new StackPane();
        avatarPreview.setPrefSize(60, 60);
        avatarPreview.setMinSize(60, 60);
        avatarPreview.setMaxSize(60, 60);
        avatarPreview.setStyle("-fx-background-color: #EBF0F7; -fx-background-radius: 30; "
                + "-fx-border-color: #CBD5E1; -fx-border-radius: 30; -fx-border-width: 1.5;");

        avatarLetter = new Label("?");
        avatarLetter.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #3D5A80;");
        avatarPreview.getChildren().add(avatarLetter);

        VBox photoInputBox = new VBox(4);
        Label lblPhoto = new Label("Ảnh đại diện");
        lblPhoto.getStyleClass().add("form-label");

        txtPhotoPath = new TextField();
        txtPhotoPath.getStyleClass().add("form-field");
        txtPhotoPath.setPromptText("Đường dẫn ảnh (tùy chọn)");

        photoInputBox.getChildren().addAll(lblPhoto, txtPhotoPath);
        HBox.setHgrow(photoInputBox, Priority.ALWAYS);

        box.getChildren().addAll(avatarPreview, photoInputBox);
        return box;
    }

    private void updateAvatarPreview() {
        if (avatarPreview == null || avatarLetter == null) return;

        String name = txtFullName != null ? txtFullName.getText().trim() : "";
        Gender gender = cboGender != null ? cboGender.getValue() : null;

        String letter = "?";
        if (!name.isEmpty()) {
            int lastSpace = name.lastIndexOf(' ');
            if (lastSpace >= 0 && lastSpace + 1 < name.length()) {
                letter = name.substring(lastSpace + 1, lastSpace + 2).toUpperCase();
            } else {
                letter = name.substring(0, 1).toUpperCase();
            }
        }
        avatarLetter.setText(letter);

        if (gender == Gender.FEMALE) {
            avatarPreview.setStyle("-fx-background-color: #F3EBF0; -fx-background-radius: 30; "
                    + "-fx-border-color: #7A4D6E; -fx-border-radius: 30; -fx-border-width: 1.5;");
            avatarLetter.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #7A4D6E;");
        } else {
            avatarPreview.setStyle("-fx-background-color: #EBF0F7; -fx-background-radius: 30; "
                    + "-fx-border-color: #3D5A80; -fx-border-radius: 30; -fx-border-width: 1.5;");
            avatarLetter.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #3D5A80;");
        }
    }

    private void setupPersonComboBox(ComboBox<Person> comboBox) {
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Person person) {
                if (person == null) return "— Không có —";
                String genderStr = person.getGender() == Gender.MALE ? "Nam" : "Nữ";
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
        if (txtPhotoPath != null) {
            txtPhotoPath.setText(editingPerson.getPhotoPath() != null ? editingPerson.getPhotoPath() : "");
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
     * Xử lý lưu: validate input ở View, sau đó gọi Controller để lưu vào MySQL.
     *
     * Chế độ thêm: controller.handleAdd(Person) → Service → Repository → MySQL
     * Chế độ sửa: controller.handleEdit(Person, newFather, newMother) → so sánh, assignFather/clearFather nếu cần
     *
     * Điều hướng sau khi thành công do Controller xử lý qua navigateToPeopleList callback.
     * Nếu có lỗi nghiệp vụ (SelfParentException, CycleDetectedException, v.v.),
     * Controller hiện Alert và User ở lại form để sửa.
     */
    private void handleSave() {
        // --- Validation View-layer ---
        String name = txtFullName.getText();
        String yearStr = txtBirthYear.getText();
        Gender gender = cboGender.getValue();

        if (name == null || name.trim().isEmpty()) {
            showAlert("Vui lòng nhập họ và tên.");
            return;
        }
        if (yearStr == null || yearStr.trim().isEmpty()) {
            showAlert("Vui lòng nhập năm sinh.");
            return;
        }
        if (gender == null) {
            showAlert("Vui lòng chọn giới tính.");
            return;
        }

        int birthYear;
        try {
            birthYear = Integer.parseInt(yearStr.trim());
        } catch (NumberFormatException e) {
            showAlert("Năm sinh phải là số nguyên hợp lệ (VD: 1990).");
            return;
        }

        String photoPath = (txtPhotoPath != null && txtPhotoPath.getText() != null
                && !txtPhotoPath.getText().trim().isEmpty())
                ? txtPhotoPath.getText().trim() : null;

        Person newFather = cboFather.getValue();    // null = chọn "— Không có —"
        Person newMother = cboMother.getValue();    // null = chọn "— Không có —"

        if (editingPerson != null) {
            // --- Chế độ sửa ---
            // Truyền father/mother gốc (từ editingPerson) để Controller so sánh với newFather/newMother
            // và quyết định cần gọi assignFather / clearFather / assignMother / clearMother
            Person updated = new Person(name.trim(), birthYear, gender, photoPath);
            updated.setId(editingPerson.getId());
            updated.setFather(editingPerson.getFather());    // cha/mẹ gốc từ DB
            updated.setMother(editingPerson.getMother());   // để controller so sánh
            controller.handleEdit(updated, newFather, newMother);
        } else {
            // --- Chế độ thêm mới ---
            Person newPerson = new Person(name.trim(), birthYear, gender, photoPath);
            newPerson.setFather(newFather);
            newPerson.setMother(newMother);
            controller.handleAdd(newPerson);
        }

        // Navigation sau khi thành công được controller xử lý (không cần gọi onSave ở đây)
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

    /**
     * No-op — Navigation sau save được xử lý bởi PersonController qua navigateToPeopleList.
     * Giữ lại để tương thích nếu PersonController no-arg version cần.
     */
    public void setOnSave(Runnable handler) {
        // intentionally no-op
    }

    public Person getEditingPerson() { return editingPerson; }
    public String getFullNameInput() { return txtFullName != null ? txtFullName.getText() : ""; }
    public String getBirthYearInput() { return txtBirthYear != null ? txtBirthYear.getText() : ""; }
    public Gender getGenderInput() { return cboGender != null ? cboGender.getValue() : null; }
    public String getPhotoPathInput() { return txtPhotoPath != null ? txtPhotoPath.getText() : null; }
    public Person getSelectedFather() { return cboFather != null ? cboFather.getValue() : null; }
    public Person getSelectedMother() { return cboMother != null ? cboMother.getValue() : null; }
}
