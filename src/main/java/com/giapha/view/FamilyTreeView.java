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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Màn hình Cây gia phả — Family Tree View.
 * Dữ liệu tổ tiên/hậu duệ lấy từ MySQL thông qua Controller (không còn dùng SampleData).
 *
 * Sử dụng controller.getAllPersons() (tương đương findAll()) để có đồ thị liên kết đầy đủ:
 * person.getFather().getFather()... → tổ tiên nhiều thế hệ đều có đầy đủ trong bộ nhớ.
 */
public class FamilyTreeView {

    private final BorderPane root;
    private final PersonController controller;
    private final TreeView<TreeNodeItem> treeView;
    private ComboBox<Person> cboFocusPerson;

    /**
     * Danh sách toàn bộ thành viên từ MySQL — đồ thị liên kết đầy đủ (findAll).
     * buildAncestorNodes() duyệt person.getFather() đệ quy qua đây.
     */
    private List<Person> allPersons;

    private Runnable onBack;
    private java.util.function.Consumer<Person> onViewPerson;

    public FamilyTreeView(PersonController controller, Person focusPerson) {
        this.controller = controller;
        root = new BorderPane();
        root.getStyleClass().add("content-area");

        // Tải toàn bộ danh sách từ MySQL (đồ thị đầy đủ)
        allPersons = controller.getAllPersons();
        if (allPersons == null) allPersons = List.of();

        // Top Header
        VBox topHeader = createHeader();
        root.setTop(topHeader);

        // Center Content
        VBox centerContent = new VBox(16);

        // Toolbar: Selector + Legend
        HBox toolbar = createToolbar();

        // Tree Card Container
        VBox treeCard = new VBox(14);
        treeCard.getStyleClass().add("card");

        Label cardTitle = new Label("Cấu trúc huyết thống dòng họ");
        cardTitle.getStyleClass().add("section-title");

        treeView = new TreeView<>();
        treeView.setShowRoot(true);
        treeView.getStyleClass().add("tree-view");
        VBox.setVgrow(treeView, Priority.ALWAYS);

        setupTreeCellFactory();

        treeCard.getChildren().addAll(cardTitle, treeView);
        VBox.setVgrow(treeCard, Priority.ALWAYS);

        centerContent.getChildren().addAll(toolbar, treeCard);
        VBox.setVgrow(centerContent, Priority.ALWAYS);

        root.setCenter(centerContent);

        // Chọn người trung tâm ban đầu
        Person initialPerson = resolveInitialPerson(focusPerson);
        if (initialPerson != null) {
            cboFocusPerson.setValue(initialPerson);
            buildTree(initialPerson);
        }
    }

    /**
     * Tìm person phù hợp để hiển thị ban đầu:
     * - Nếu focusPerson không null → tìm bản tương ứng trong allPersons theo ID
     *   (đảm bảo person từ đồ thị đầy đủ, không phải object 1-level từ search)
     * - Nếu null → mặc định chọn người có cha/mẹ đầu tiên hoặc người đầu tiên trong list
     */
    private Person resolveInitialPerson(Person focusPerson) {
        if (allPersons.isEmpty()) return null;

        if (focusPerson != null && focusPerson.getId() > 0) {
            // Tìm trong allPersons để có đồ thị đầy đủ (ancestor chain)
            return allPersons.stream()
                    .filter(p -> p.getId() == focusPerson.getId())
                    .findFirst()
                    .orElse(allPersons.get(0));
        }

        // Default: ưu tiên người có cả cha lẫn mẹ (thế hệ trung tâm)
        return allPersons.stream()
                .filter(p -> p.getFather() != null && p.getMother() != null)
                .findFirst()
                .orElse(allPersons.get(0));
    }

    private VBox createHeader() {
        Button btnBack = new Button("← Quay lại danh sách");
        btnBack.getStyleClass().add("back-link");
        btnBack.setOnAction(e -> {
            if (onBack != null) onBack.run();
        });

        Label title = new Label("Cây gia phả");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Sơ đồ phả hệ trực quan — phân định nhánh tổ tiên và hậu duệ");
        subtitle.getStyleClass().add("page-subtitle");

        VBox header = new VBox(6, btnBack, title, subtitle);
        header.setPadding(new Insets(0, 0, 12, 0));
        return header;
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(16);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label lblSelect = new Label("Người trung tâm:");
        lblSelect.getStyleClass().add("form-label");
        lblSelect.setStyle("-fx-font-size: 13px; -fx-padding: 6 0 0 0;");

        cboFocusPerson = new ComboBox<>(FXCollections.observableArrayList(allPersons));
        cboFocusPerson.getStyleClass().add("form-field");
        cboFocusPerson.setPrefWidth(300);
        cboFocusPerson.setPromptText("Chọn thành viên để xem phả hệ");
        setupPersonComboBox(cboFocusPerson);
        cboFocusPerson.setOnAction(e -> {
            Person selected = cboFocusPerson.getValue();
            if (selected != null) {
                buildTree(selected);
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox legend = createLegendPills();

        toolbar.getChildren().addAll(lblSelect, cboFocusPerson, spacer, legend);
        return toolbar;
    }

    private HBox createLegendPills() {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER_RIGHT);

        Label legFocus = new Label("★ Trọng tâm");
        legFocus.setStyle("-fx-background-color: #EBF0F7; -fx-text-fill: #3D5A80; -fx-padding: 4 10; "
                + "-fx-background-radius: 6; -fx-font-size: 11px; -fx-font-weight: bold; -fx-border-color: #3D5A80; -fx-border-radius: 6;");

        Label legAncestor = new Label("↑ Nhánh Tổ tiên");
        legAncestor.setStyle("-fx-background-color: #F8FAFC; -fx-text-fill: #475569; -fx-padding: 4 10; "
                + "-fx-background-radius: 6; -fx-font-size: 11px; -fx-border-color: #CBD5E1; -fx-border-radius: 6;");

        Label legDescendant = new Label("↓ Nhánh Hậu duệ");
        legDescendant.setStyle("-fx-background-color: #F8FAFC; -fx-text-fill: #475569; -fx-padding: 4 10; "
                + "-fx-background-radius: 6; -fx-font-size: 11px; -fx-border-color: #CBD5E1; -fx-border-radius: 6;");

        box.getChildren().addAll(legFocus, legAncestor, legDescendant);
        return box;
    }

    private void setupTreeCellFactory() {
        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(TreeNodeItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setGraphic(item.renderGraphic());
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 3 0;");
                }
            }
        });
    }

    private void buildTree(Person focusPerson) {
        TreeNodeItem rootData = new TreeNodeItem(TreeNodeItem.Type.ROOT, focusPerson, null,
                "Phả hệ của " + focusPerson.getFullName());
        TreeItem<TreeNodeItem> rootItem = new TreeItem<>(rootData);
        rootItem.setExpanded(true);

        // Ancestors Section
        TreeItem<TreeNodeItem> ancestorsSec = new TreeItem<>(
                new TreeNodeItem(TreeNodeItem.Type.SECTION, null, null, "TỔ TIÊN (TRỰC HỆ TRÊN)")
        );
        ancestorsSec.setExpanded(true);
        buildAncestorNodes(focusPerson, ancestorsSec);

        // Focus Person
        TreeItem<TreeNodeItem> focusItem = new TreeItem<>(
                new TreeNodeItem(TreeNodeItem.Type.FOCUS, focusPerson, "NGƯỜI TRUNG TÂM", null)
        );
        focusItem.setExpanded(true);

        // Descendants Section
        TreeItem<TreeNodeItem> descendantsSec = new TreeItem<>(
                new TreeNodeItem(TreeNodeItem.Type.SECTION, null, null, "HẬU DUỆ (CÁC THẾ HỆ TIẾP THEO)")
        );
        descendantsSec.setExpanded(true);
        buildDescendantNodes(focusPerson, descendantsSec);

        if (!ancestorsSec.getChildren().isEmpty()) {
            rootItem.getChildren().add(ancestorsSec);
        }
        rootItem.getChildren().add(focusItem);
        if (!descendantsSec.getChildren().isEmpty()) {
            rootItem.getChildren().add(descendantsSec);
        }

        treeView.setRoot(rootItem);
    }

    /**
     * Xây dựng cây tổ tiên bằng cách duyệt đệ quy qua person.getFather() và person.getMother().
     * Hoạt động chính xác vì allPersons được tải bằng findAll() — đồ thị liên kết đầy đủ:
     * person.getFather() trỏ tới object cha, object cha cũng có father/mother của nó, v.v.
     */
    private void buildAncestorNodes(Person person, TreeItem<TreeNodeItem> parentNode) {
        Person father = person.getFather();
        Person mother = person.getMother();

        if (father != null) {
            TreeNodeItem fData = new TreeNodeItem(TreeNodeItem.Type.ANCESTOR, father, "Cha", null);
            TreeItem<TreeNodeItem> fItem = new TreeItem<>(fData);
            fItem.setExpanded(true);
            buildAncestorNodes(father, fItem);
            parentNode.getChildren().add(fItem);
        }
        if (mother != null) {
            TreeNodeItem mData = new TreeNodeItem(TreeNodeItem.Type.ANCESTOR, mother, "Mẹ", null);
            TreeItem<TreeNodeItem> mItem = new TreeItem<>(mData);
            mItem.setExpanded(true);
            buildAncestorNodes(mother, mItem);
            parentNode.getChildren().add(mItem);
        }
    }

    /**
     * Xây dựng cây hậu duệ bằng cách tìm những người có father.id hoặc mother.id trùng với person.
     * Tìm trong allPersons (từ MySQL) — không dùng SampleData.
     */
    private void buildDescendantNodes(Person person, TreeItem<TreeNodeItem> parentNode) {
        List<Person> children = allPersons.stream()
                .filter(p -> (p.getFather() != null && p.getFather().getId() == person.getId())
                        || (p.getMother() != null && p.getMother().getId() == person.getId()))
                .collect(Collectors.toList());

        for (Person child : children) {
            TreeNodeItem cData = new TreeNodeItem(TreeNodeItem.Type.DESCENDANT, child, "Con", null);
            TreeItem<TreeNodeItem> cItem = new TreeItem<>(cData);
            cItem.setExpanded(true);
            buildDescendantNodes(child, cItem);
            parentNode.getChildren().add(cItem);
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

    public BorderPane getRoot() {
        return root;
    }

    public void setOnBack(Runnable handler) {
        this.onBack = handler;
    }

    public void setOnViewPerson(java.util.function.Consumer<Person> handler) {
        this.onViewPerson = handler;
    }

    /**
     * Dữ liệu biểu diễn mỗi Node trong cây gia phả.
     */
    public static class TreeNodeItem {
        public enum Type { ROOT, SECTION, FOCUS, ANCESTOR, DESCENDANT }

        private final Type type;
        private final Person person;
        private final String roleLabel;
        private final String titleText;

        public TreeNodeItem(Type type, Person person, String roleLabel, String titleText) {
            this.type = type;
            this.person = person;
            this.roleLabel = roleLabel;
            this.titleText = titleText;
        }

        public javafx.scene.Node renderGraphic() {
            if (type == Type.ROOT) {
                HBox rootBox = new HBox(8);
                rootBox.setAlignment(Pos.CENTER_LEFT);
                rootBox.setStyle("-fx-padding: 4 8;");
                Label title = new Label(titleText);
                title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -primary;");
                rootBox.getChildren().add(title);
                return rootBox;
            }

            if (type == Type.SECTION) {
                HBox secBox = new HBox(8);
                secBox.setAlignment(Pos.CENTER_LEFT);
                secBox.setStyle("-fx-padding: 6 8 2 8;");
                Label lbl = new Label(titleText);
                lbl.getStyleClass().add("tree-section-label");
                secBox.getChildren().add(lbl);
                return secBox;
            }

            // Person Cards (FOCUS, ANCESTOR, DESCENDANT)
            HBox card = new HBox(12);
            card.setAlignment(Pos.CENTER_LEFT);
            card.getStyleClass().add("tree-person-card");

            if (type == Type.FOCUS) {
                card.getStyleClass().add("tree-person-card-focus");
            }

            // Avatar: ảnh thật bo tròn, fallback về chữ cái đầu nếu thiếu ảnh / ảnh hỏng
            card.getChildren().add(new PersonAvatar(person, PersonAvatar.SMALL_SIZE));

            // Role Pill
            if (roleLabel != null) {
                Label rolePill = new Label(roleLabel.toUpperCase());
                if (type == Type.FOCUS) {
                    rolePill.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF; "
                            + "-fx-background-color: -primary; -fx-padding: 2 6; -fx-background-radius: 4;");
                } else {
                    rolePill.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -text-muted; "
                            + "-fx-background-color: #F1EFEB; -fx-padding: 2 6; -fx-background-radius: 4;");
                }
                card.getChildren().add(rolePill);
            }

            // Name
            Label nameLabel = new Label(person.getFullName());
            nameLabel.getStyleClass().add("tree-person-name");
            if (type == Type.FOCUS) {
                nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -primary;");
            }

            // Details: Year + Gender
            Label detailLabel = new Label("Sinh năm " + person.getBirthYear());
            detailLabel.getStyleClass().add("tree-person-detail");

            Label genderBadge = new Label(person.getGender() == Gender.MALE ? "Nam" : "Nữ");
            genderBadge.getStyleClass().add(person.getGender() == Gender.MALE ? "badge-male" : "badge-female");

            card.getChildren().addAll(nameLabel, detailLabel, genderBadge);
            return card;
        }

        public Person getPerson() {
            return person;
        }
    }
}
