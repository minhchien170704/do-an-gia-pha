package com.giapha.view;

import com.giapha.controller.PersonController;
import com.giapha.model.Gender;
import com.giapha.model.Person;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Màn hình Chi tiết thành viên — Person Detail Profile View.
 * Lấy danh sách con thực từ MySQL thông qua Controller (không còn dùng SampleData).
 */
public class PersonDetailView {

    private final BorderPane root;
    private final PersonController controller;
    private final Person person;

    private Runnable onBack;
    private java.util.function.Consumer<Person> onEdit;
    private java.util.function.Consumer<Person> onViewTree;

    public PersonDetailView(PersonController controller, Person person) {
        this.controller = controller;
        this.person = person;
        root = new BorderPane();
        root.getStyleClass().add("content-area");

        // Top Navigation Link
        VBox topBar = createTopBar();
        root.setTop(topBar);

        // Center Content ScrollPane
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-transparent");

        VBox contentBox = new VBox(18);
        contentBox.setMaxWidth(720);
        contentBox.setPadding(new Insets(6, 0, 32, 0));

        // 1. Profile Hero Card (Avatar + Tên + Năm sinh/Giới tính + Actions)
        VBox heroCard = createProfileHeroCard();

        // 2. Thông tin cá nhân
        VBox infoCard = createPersonalInfoCard();

        // 3. Quan hệ gia đình (Cha, Mẹ, Các con từ MySQL)
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

        // Avatar: hiển thị ảnh thật (bo tròn), tự fallback về chữ cái đầu nếu
        // không có ảnh hoặc file ảnh không tồn tại / bị hỏng.
        PersonAvatar avatar = new PersonAvatar(person, 70);

        // Tên & Năm sinh · Giới tính
        VBox nameBox = new VBox(5);
        Label nameLabel = new Label(person.getFullName());
        nameLabel.getStyleClass().add("profile-name");

        String genderText = person.getGender().getDisplayName();
        Label metaLabel = new Label("Sinh năm " + person.getBirthYear() + "  ·  " + genderText);
        metaLabel.getStyleClass().add("profile-subtitle");

        nameBox.getChildren().addAll(nameLabel, metaLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Actions: Chỉnh sửa + Xem cây gia phả
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

        String genderText = person.getGender().getDisplayName();
        addDetailRow(grid, 2, "Giới tính", genderText);

        // Ảnh đại diện: hiển thị thumbnail thật thay vì chỉ in đường dẫn.
        // Nếu không có ảnh / file không tồn tại → hiển thị dòng thông báo.
        Label lblPhoto = new Label("Ảnh đại diện");
        lblPhoto.getStyleClass().add("detail-label");
        lblPhoto.setMinWidth(120);
        grid.add(lblPhoto, 0, 3);

        PersonAvatar photoAvatar = new PersonAvatar(person, 48);
        if (photoAvatar.isShowingRealImage()) {
            grid.add(photoAvatar, 1, 3);
        } else {
            Label lblNoPhoto = new Label("Chưa có ảnh");
            lblNoPhoto.getStyleClass().add("empty-text");
            grid.add(lblNoPhoto, 1, 3);
        }

        card.getChildren().addAll(sectionTitle, grid);
        return card;
    }

    /**
     * Tạo card Quan hệ gia đình.
     * Lấy danh sách con thực từ MySQL: gọi controller.getAllPersons() và lọc theo father/mother ID.
     */
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

        // Lấy danh sách con từ MySQL qua Controller
        List<Person> children = getChildrenFromDB();
        Label childrenTitle = new Label("Các con" + (!children.isEmpty() ? " (" + children.size() + ")" : ""));
        childrenTitle.getStyleClass().add("section-title");
        childrenTitle.getStyleClass().add("section-title-sm");

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

                String cGender = child.getGender().getDisplayName();
                Label genderBadge = new Label(cGender);
                genderBadge.getStyleClass().add(child.getGender().getBadgeCssClass());

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                childRow.getChildren().addAll(nameLabel, yearLabel, spacer, genderBadge);
                childrenSection.getChildren().add(childRow);
            }
        }

        card.getChildren().addAll(sectionTitle, parentsRow, sep, childrenTitle, childrenSection);
        return card;
    }

    /**
     * Lấy danh sách con trực tiếp của person từ MySQL.
     * Gọi controller.getAllPersons() (findAll — fully linked graph) rồi lọc theo father_id/mother_id.
     */
    private List<Person> getChildrenFromDB() {
        List<Person> allPersons = controller.getAllPersons();
        if (allPersons == null || allPersons.isEmpty()) return List.of();

        return allPersons.stream()
                .filter(p -> {
                    boolean isFatherOf = p.getFather() != null && p.getFather().getId() == person.getId();
                    boolean isMotherOf = p.getMother() != null && p.getMother().getId() == person.getId();
                    return isFatherOf || isMotherOf;
                })
                .collect(Collectors.toList());
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
