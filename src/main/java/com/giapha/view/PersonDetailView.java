package com.giapha.view;

import com.giapha.model.Gender;
import com.giapha.model.Person;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Màn hình Chi tiết thành viên — Person Detail Profile View.
 * Bố cục Profile gọn gàng, làm nổi bật Thông tin cá nhân và Quan hệ gia đình.
 */
public class PersonDetailView {

    private final BorderPane root;
    private final SampleData data = SampleData.getInstance();
    private final Person person;

    private Runnable onBack;
    private java.util.function.Consumer<Person> onEdit;
    private java.util.function.Consumer<Person> onViewTree;

    public PersonDetailView(Person person) {
        this.person = person;
        root = new BorderPane();
        root.getStyleClass().add("content-area");

        // Top Navigation Link
        VBox topBar = createTopBar();
        root.setTop(topBar);

        // Center Content ScrollPane
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox contentBox = new VBox(18);
        contentBox.setMaxWidth(720);
        contentBox.setPadding(new Insets(6, 0, 32, 0));

        // 1. Profile Hero Card (Avatar + Tên + Năm sinh/Giới tính + Actions)
        VBox heroCard = createProfileHeroCard();

        // 2. Thông tin cá nhân
        VBox infoCard = createPersonalInfoCard();

        // 3. Quan hệ gia đình (Cha, Mẹ, Các con)
        VBox familyCard = createFamilyRelationsCard();

        contentBox.getChildren().addAll(heroCard, infoCard, familyCard);

        HBox centerWrapper = new HBox(contentBox);
        centerWrapper.setAlignment(Pos.TOP_CENTER);
        scrollPane.setContent(centerWrapper);

        root.setCenter(scrollPane);
    }

    private VBox createTopBar() {
        Button btnBack = new Button("← Quay lại danh sách");
        btnBack.getStyleClass().add("back-link");
        btnBack.setOnAction(e -> {
            if (onBack != null) onBack.run();
        });

        VBox box = new VBox(btnBack);
        box.setPadding(new Insets(0, 0, 10, 0));
        return box;
    }

    private VBox createProfileHeroCard() {
        VBox card = new VBox();
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20, 24, 20, 24));

        HBox hero = new HBox(18);
        hero.setAlignment(Pos.CENTER_LEFT);

        // Avatar placeholder circle (72x72)
        StackPane avatar = new StackPane();
        avatar.setPrefSize(70, 70);
        avatar.setMinSize(70, 70);
        avatar.setMaxSize(70, 70);

        boolean isMale = person.getGender() == Gender.MALE;
        String bgCol = isMale ? "#EBF0F7" : "#F3EBF0";
        String borderCol = isMale ? "#3D5A80" : "#7A4D6E";

        avatar.setStyle("-fx-background-color: " + bgCol + "; "
                + "-fx-background-radius: 35; "
                + "-fx-border-color: " + borderCol + "; "
                + "-fx-border-radius: 35; "
                + "-fx-border-width: 1.5;");

        String initial = "?";
        if (person.getFullName() != null && !person.getFullName().trim().isEmpty()) {
            String name = person.getFullName().trim();
            int lastSpace = name.lastIndexOf(' ');
            if (lastSpace >= 0 && lastSpace + 1 < name.length()) {
                initial = name.substring(lastSpace + 1, lastSpace + 2).toUpperCase();
            } else {
                initial = name.substring(0, 1).toUpperCase();
            }
        }
        Label initialLabel = new Label(initial);
        initialLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + borderCol + ";");
        avatar.getChildren().add(initialLabel);

        // Tên & Năm sinh · Giới tính
        VBox nameBox = new VBox(5);
        Label nameLabel = new Label(person.getFullName());
        nameLabel.getStyleClass().add("profile-name");

        String genderText = isMale ? "Nam" : "Nữ";
        Label metaLabel = new Label("Sinh năm " + person.getBirthYear() + "  ·  " + genderText);
        metaLabel.getStyleClass().add("profile-subtitle");

        nameBox.getChildren().addAll(nameLabel, metaLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Actions: Chỉnh sửa (secondary) + Xem cây gia phả (primary)
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnEdit = new Button("Chỉnh sửa");
        btnEdit.getStyleClass().add("btn-secondary");
        btnEdit.setOnAction(e -> {
            if (onEdit != null) onEdit.accept(person);
        });

        Button btnTree = new Button("Xem cây gia phả");
        btnTree.getStyleClass().add("btn-primary");
        btnTree.setOnAction(e -> {
            if (onViewTree != null) onViewTree.accept(person);
        });

        actions.getChildren().addAll(btnEdit, btnTree);

        hero.getChildren().addAll(avatar, nameBox, spacer, actions);
        card.getChildren().add(hero);
        return card;
    }

    private VBox createPersonalInfoCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20, 24, 20, 24));

        Label sectionTitle = new Label("Thông tin cá nhân");
        sectionTitle.getStyleClass().add("section-title");

        GridPane grid = new GridPane();
        grid.setHgap(32);
        grid.setVgap(12);

        addDetailRow(grid, 0, "Họ và tên", person.getFullName());
        addDetailRow(grid, 1, "Năm sinh", String.valueOf(person.getBirthYear()));

        String genderText = person.getGender() == Gender.MALE ? "Nam" : "Nữ";
        addDetailRow(grid, 2, "Giới tính", genderText);

        if (person.getPhotoPath() != null && !person.getPhotoPath().trim().isEmpty()) {
            addDetailRow(grid, 3, "Ảnh đại diện", person.getPhotoPath().trim());
        }

        card.getChildren().addAll(sectionTitle, grid);
        return card;
    }

    private VBox createFamilyRelationsCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20, 24, 20, 24));

        Label sectionTitle = new Label("Quan hệ gia đình");
        sectionTitle.getStyleClass().add("section-title");

        // Cha & Mẹ (2 cards cạnh nhau)
        HBox parentsRow = new HBox(16);

        VBox cardFather = createParentCard("Cha", person.getFather());
        VBox cardMother = createParentCard("Mẹ", person.getMother());

        HBox.setHgrow(cardFather, Priority.ALWAYS);
        HBox.setHgrow(cardMother, Priority.ALWAYS);

        parentsRow.getChildren().addAll(cardFather, cardMother);

        // Separator
        Separator sep = new Separator();
        sep.getStyleClass().add("form-separator");

        // Các con
        List<Person> children = data.getChildren(person);
        Label childrenTitle = new Label("Các con" + (!children.isEmpty() ? " (" + children.size() + ")" : ""));
        childrenTitle.getStyleClass().add("section-title");
        childrenTitle.setStyle("-fx-font-size: 13px;");

        VBox childrenSection = new VBox(8);
        if (children.isEmpty()) {
            Label emptyLbl = new Label("Chưa có thông tin");
            emptyLbl.getStyleClass().add("empty-text");
            childrenSection.getChildren().add(emptyLbl);
        } else {
            for (Person child : children) {
                HBox childRow = new HBox(12);
                childRow.setAlignment(Pos.CENTER_LEFT);
                childRow.getStyleClass().add("person-mini-card");

                Label nameLabel = new Label("• " + child.getFullName());
                nameLabel.getStyleClass().add("parent-card-name");

                Label yearLabel = new Label("Sinh năm " + child.getBirthYear());
                yearLabel.getStyleClass().add("parent-card-meta");

                String cGender = child.getGender() == Gender.MALE ? "Nam" : "Nữ";
                Label genderBadge = new Label(cGender);
                genderBadge.getStyleClass().add(child.getGender() == Gender.MALE ? "badge-male" : "badge-female");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                childRow.getChildren().addAll(nameLabel, yearLabel, spacer, genderBadge);
                childrenSection.getChildren().add(childRow);
            }
        }

        card.getChildren().addAll(sectionTitle, parentsRow, sep, childrenTitle, childrenSection);
        return card;
    }

    private VBox createParentCard(String roleTitle, Person parent) {
        VBox box = new VBox(6);
        box.getStyleClass().add("parent-card");

        Label lblRole = new Label(roleTitle);
        lblRole.getStyleClass().add("parent-card-header");

        if (parent != null) {
            Label lblName = new Label(parent.getFullName());
            lblName.getStyleClass().add("parent-card-name");

            Label lblYear = new Label("Sinh năm " + parent.getBirthYear());
            lblYear.getStyleClass().add("parent-card-meta");

            box.getChildren().addAll(lblRole, lblName, lblYear);
        } else {
            Label lblEmpty = new Label("Chưa có thông tin");
            lblEmpty.getStyleClass().add("empty-text");
            box.getChildren().addAll(lblRole, lblEmpty);
        }

        return box;
    }

    private void addDetailRow(GridPane grid, int row, String label, String value) {
        Label lblKey = new Label(label);
        lblKey.getStyleClass().add("detail-label");
        lblKey.setMinWidth(120);

        Label lblValue = new Label(value);
        lblValue.getStyleClass().add("detail-value");

        grid.add(lblKey, 0, row);
        grid.add(lblValue, 1, row);
    }

    public BorderPane getRoot() {
        return root;
    }

    public void setOnBack(Runnable handler) {
        this.onBack = handler;
    }

    public void setOnEdit(java.util.function.Consumer<Person> handler) {
        this.onEdit = handler;
    }

    public void setOnViewTree(java.util.function.Consumer<Person> handler) {
        this.onViewTree = handler;
    }
}
