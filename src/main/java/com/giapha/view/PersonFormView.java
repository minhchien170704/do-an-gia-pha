package com.giapha.view;

import com.giapha.model.Gender;
import com.giapha.model.Person;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.util.List;

/**
 * Màn hình Thêm / Chỉnh sửa thành viên — Add/Edit Person Form.
 * Tuân thủ cấu trúc tối giản, phân cấp rõ ràng, khoảng cách thoáng đãng.
 */
public class PersonFormView {

    private final BorderPane root;
    private final TextField txtFullName;
    private final TextField txtBirthYear;
    private final ComboBox<Gender> cboGender;
    private TextField txtPhotoPath;
    private final ComboBox<Person> cboFather;
    private final ComboBox<Person> cboMother;

    // Avatar preview
    private StackPane avatarPreview;
    private Label avatarLetter;

    private final SampleData data = SampleData.getInstance();
    private final Person editingPerson;

    private Runnable onSave;
    private Runnable onCancel;

    public PersonFormView() {
        this(null);
    }

    public PersonFormView(Person personToEdit) {
        this.editingPerson = personToEdit;
        root = new BorderPane();
        root.getStyleClass().add("content-area");

        // Top Header
        VBox topHeader = createHeader();
        root.setTop(topHeader);

        // Center Content ScrollPane
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox formContainer = new VBox(20);
        formContainer.setMaxWidth(620);
        formContainer.setAlignment(Pos.TOP_CENTER);
        formContainer.setPadding(new Insets(10, 0, 32, 0));

        VBox formCard = new VBox(16);
        formCard.getStyleClass().add("card");
        formCard.setPadding(new Insets(24, 28, 28, 28));

        // ================= SECTION 1: THÔNG TIN CÁ NHÂN =================
        Label secPersonal = new Label("Thông tin cá nhân");
        secPersonal.getStyleClass().add("section-title");

        // Avatar preview + Photo Path
        HBox photoSection = createPhotoSection();

        // Họ và tên
        VBox nameGroup = new VBox(4);
        Label lblName = new Label("Họ và tên *");
        lblName.getStyleClass().add("form-label");
        txtFullName = new TextField();
        txtFullName.getStyleClass().add("form-field");
        txtFullName.setPromptText("Nhập họ và tên đầy đủ");
        txtFullName.textProperty().addListener((obs, oldVal, newVal) -> updateAvatarPreview());
        nameGroup.getChildren().addAll(lblName, txtFullName);

        // Năm sinh & Giới tính đặt cạnh nhau cân đối
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
            @Override
            public String toString(Gender gender) {
                if (gender == null) return "";
                return gender == Gender.MALE ? "Nam" : "Nữ";
            }

            @Override
            public Gender fromString(String string) {
                return "Nam".equals(string) ? Gender.MALE : Gender.FEMALE;
            }
        });
        cboGender.valueProperty().addListener((obs, oldVal, newVal) -> updateAvatarPreview());
        genderGroup.getChildren().addAll(lblGender, cboGender);
        HBox.setHgrow(genderGroup, Priority.ALWAYS);

        birthAndGender.getChildren().addAll(yearGroup, genderGroup);

        // Separator
        Separator sep = new Separator();
        sep.getStyleClass().add("form-separator");

        // ================= SECTION 2: QUAN HỆ GIA ĐÌNH =================
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

        // Nút hành động
        HBox buttonBar = new HBox(12);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(12, 0, 0, 0));

        Button btnCancel = new Button("Hủy");
        btnCancel.getStyleClass().add("btn-secondary");
        btnCancel.setOnAction(e -> {
            if (onCancel != null) onCancel.run();
        });

        String saveText = editingPerson != null ? "Lưu thay đổi" : "Lưu thành viên";
        Button btnSave = new Button(saveText);
        btnSave.getStyleClass().add("btn-primary");
        btnSave.setOnAction(e -> handleSave());

        buttonBar.getChildren().addAll(btnCancel, btnSave);

        // Gom toàn bộ vào card
        formCard.getChildren().addAll(
                secPersonal,
                photoSection,
                nameGroup,
                birthAndGender,
                sep,
                secFamily,
                fatherGroup,
                motherGroup,
                buttonBar
        );

        formContainer.getChildren().add(formCard);

        HBox centerWrapper = new HBox(formContainer);
        centerWrapper.setAlignment(Pos.TOP_CENTER);
        scrollPane.setContent(centerWrapper);

        root.setCenter(scrollPane);

        // Load ComboBoxes
        refreshComboBoxes();

        // Fill existing data if editing
        if (editingPerson != null) {
            fillFormData();
        }

        updateAvatarPreview();
    }

    private VBox createHeader() {
        Button btnBack = new Button("← Quay lại");
        btnBack.getStyleClass().add("back-link");
        btnBack.setOnAction(e -> {
            if (onCancel != null) onCancel.run();
        });

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

        // Avatar placeholder preview (64x64)
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
        txtPhotoPath.setPromptText("Đường dẫn ảnh (VD: avatar.jpg — tùy chọn)");

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
                if (person == null) return "";
                String genderStr = person.getGender() == Gender.MALE ? "Nam" : "Nữ";
                return person.getFullName() + " (" + person.getBirthYear() + ", " + genderStr + ")";
            }

            @Override
            public Person fromString(String string) {
                return null;
            }
        });
    }

    private void refreshComboBoxes() {
        List<Person> males = data.getMales();
        List<Person> females = data.getFemales();

        if (editingPerson != null) {
            males.removeIf(p -> p.getId() == editingPerson.getId());
            females.removeIf(p -> p.getId() == editingPerson.getId());
        }

        cboFather.setItems(FXCollections.observableArrayList(males));
        cboMother.setItems(FXCollections.observableArrayList(females));
    }

    private void fillFormData() {
        if (editingPerson == null) return;
        txtFullName.setText(editingPerson.getFullName());
        txtBirthYear.setText(String.valueOf(editingPerson.getBirthYear()));
        cboGender.setValue(editingPerson.getGender());
        txtPhotoPath.setText(editingPerson.getPhotoPath() != null ? editingPerson.getPhotoPath() : "");
        cboFather.setValue(editingPerson.getFather());
        cboMother.setValue(editingPerson.getMother());
    }

    private void handleSave() {
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
            showAlert("Năm sinh phải là số nguyên hợp lệ.");
            return;
        }

        if (editingPerson != null) {
            editingPerson.setFullName(name.trim());
            editingPerson.setBirthYear(birthYear);
            editingPerson.setGender(gender);
            editingPerson.setPhotoPath(txtPhotoPath.getText().trim().isEmpty() ? null : txtPhotoPath.getText().trim());
            editingPerson.setFather(cboFather.getValue());
            editingPerson.setMother(cboMother.getValue());
        } else {
            Person newPerson = new Person(
                    name.trim(),
                    birthYear,
                    gender,
                    txtPhotoPath.getText().trim().isEmpty() ? null : txtPhotoPath.getText().trim()
            );
            newPerson.setFather(cboFather.getValue());
            newPerson.setMother(cboMother.getValue());
            data.addPerson(newPerson);
        }

        if (onSave != null) onSave.run();
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

    public void setOnSave(Runnable handler) {
        this.onSave = handler;
    }

    public void setOnCancel(Runnable handler) {
        this.onCancel = handler;
    }

    public Person getEditingPerson() {
        return editingPerson;
    }

    public String getFullNameInput() {
        return txtFullName != null ? txtFullName.getText() : "";
    }

    public String getBirthYearInput() {
        return txtBirthYear != null ? txtBirthYear.getText() : "";
    }

    public Gender getGenderInput() {
        return cboGender != null ? cboGender.getValue() : null;
    }

    public String getPhotoPathInput() {
        return txtPhotoPath != null ? txtPhotoPath.getText() : null;
    }

    public Person getSelectedFather() {
        return cboFather != null ? cboFather.getValue() : null;
    }

    public Person getSelectedMother() {
        return cboMother != null ? cboMother.getValue() : null;
    }
}
