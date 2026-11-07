package com.giapha.view;

import com.giapha.controller.PeopleListSource;
import com.giapha.controller.PersonController;
import com.giapha.model.Gender;
import com.giapha.model.Person;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Màn hình danh sách thành viên gia phả — People List View.
 * Tải dữ liệu thực từ MySQL thông qua PersonController (không còn dùng SampleData).
 * FilteredList client-side cho phép tìm kiếm tức thì trên tập đã nạp.
 *
 * <p>Lớp này hiện thực {@link PeopleListSource} — abstraction cho biết thành viên
 * đang được chọn và yêu cầu làm mới danh sách, dùng bởi
 * {@link PersonController} mà không cần biết lớp View cụ thể nào.
 */
public class PeopleListView implements PeopleListSource {

    private final BorderPane root;
    private final TableView<Person> tableView;
    private TextField searchField;

    /** ObservableList nạp từ MySQL — FilteredList phủ lên để tìm kiếm client-side */
    private final ObservableList<Person> allData = FXCollections.observableArrayList();
    private final FilteredList<Person> filteredData;

    private final PersonController controller;

    // Stat labels to update dynamically
    private Label valTotal;
    private Label valGenerations;
    private Label valMales;
    private Label valFemales;

    private Runnable onAddPerson;
    private java.util.function.Consumer<Person> onEditPerson;
    private java.util.function.Consumer<Person> onViewDetail;
    private java.util.function.Consumer<Person> onViewTree;

    public PeopleListView(PersonController controller) {
        this.controller = controller;
        root = new BorderPane();
        root.getStyleClass().add("content-area");

        // Top section: Page Title + Stats Cards
        VBox topSection = new VBox(20);
        topSection.setPadding(new Insets(0, 0, 16, 0));

        VBox header = createPageHeader();
        HBox statCards = createStatsSection();
        topSection.getChildren().addAll(header, statCards);
        root.setTop(topSection);

        // Center section: Toolbar + Table in Card
        VBox centerSection = new VBox(14);

        HBox toolbar = createToolbar();
        tableView = createTable();

        filteredData = new FilteredList<>(allData, p -> true);
        tableView.setItems(filteredData);

        VBox tableCard = new VBox(tableView);
        tableCard.getStyleClass().add("card");
        VBox.setVgrow(tableView, Priority.ALWAYS);
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        centerSection.getChildren().addAll(toolbar, tableCard);
        VBox.setVgrow(centerSection, Priority.ALWAYS);
        root.setCenter(centerSection);

        // Bottom status info
        HBox footerBar = createFooterBar();
        root.setBottom(footerBar);

        // Cập nhật stats khi dữ liệu thay đổi
        allData.addListener((ListChangeListener<Person>) c -> updateStats());

        // Tải dữ liệu thực từ MySQL
        reloadData();
    }

    /**
     * Tải lại toàn bộ danh sách thành viên từ MySQL thông qua Controller.
     * Được gọi nội bộ khi khởi tạo và sau mỗi thao tác CRUD để đồng bộ UI với database.
     */
    private void reloadData() {
        List<Person> people = controller.getAllPersons();
        allData.setAll(people != null ? people : List.of());
        updateStats();
    }

    private VBox createPageHeader() {
        Label title = new Label("Thành viên gia đình");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Quản lý và tra cứu thông tin các thế hệ trong dòng họ");
        subtitle.getStyleClass().add("page-subtitle");

        VBox box = new VBox(4, title, subtitle);
        return box;
    }

    private HBox createStatsSection() {
        HBox statsBox = new HBox(16);
        statsBox.setAlignment(Pos.CENTER_LEFT);

        VBox cardTotal = createStatCard("Tổng thành viên", "0");
        valTotal = (Label) cardTotal.getChildren().get(0);

        VBox cardGens = createStatCard("Số thế hệ", "0");
        valGenerations = (Label) cardGens.getChildren().get(0);

        VBox cardMales = createStatCard("Thành viên Nam", "0");
        valMales = (Label) cardMales.getChildren().get(0);

        VBox cardFemales = createStatCard("Thành viên Nữ", "0");
        valFemales = (Label) cardFemales.getChildren().get(0);

        HBox.setHgrow(cardTotal, Priority.ALWAYS);
        HBox.setHgrow(cardGens, Priority.ALWAYS);
        HBox.setHgrow(cardMales, Priority.ALWAYS);
        HBox.setHgrow(cardFemales, Priority.ALWAYS);

        statsBox.getChildren().addAll(cardTotal, cardGens, cardMales, cardFemales);
        return statsBox;
    }

    private VBox createStatCard(String labelText, String initialVal) {
        VBox card = new VBox(4);
        card.getStyleClass().add("stat-card");

        Label valLabel = new Label(initialVal);
        valLabel.getStyleClass().add("stat-value");

        Label titleLabel = new Label(labelText);
        titleLabel.getStyleClass().add("stat-label");

        card.getChildren().addAll(valLabel, titleLabel);
        return card;
    }

    private void updateStats() {
        List<Person> list = allData;
        int total = list.size();
        long males = list.stream().filter(p -> p.getGender() == Gender.MALE).count();
        long females = list.stream().filter(p -> p.getGender() == Gender.FEMALE).count();
        int gens = calculateGenerations(list);

        if (valTotal != null) valTotal.setText(String.valueOf(total));
        if (valGenerations != null) valGenerations.setText(String.valueOf(gens));
        if (valMales != null) valMales.setText(String.valueOf(males));
        if (valFemales != null) valFemales.setText(String.valueOf(females));
    }

    private int calculateGenerations(List<Person> people) {
        if (people == null || people.isEmpty()) return 0;
        Map<Integer, Integer> depthMap = new HashMap<>();
        for (Person p : people) {
            getDepth(p, depthMap);
        }
        return depthMap.values().stream().mapToInt(Integer::intValue).max().orElse(1);
    }

    private int getDepth(Person p, Map<Integer, Integer> depthMap) {
        if (p == null) return 0;
        if (depthMap.containsKey(p.getId())) return depthMap.get(p.getId());
        int fDepth = p.getFather() != null ? getDepth(p.getFather(), depthMap) : 0;
        int mDepth = p.getMother() != null ? getDepth(p.getMother(), depthMap) : 0;
        int depth = Math.max(fDepth, mDepth) + 1;
        depthMap.put(p.getId(), depth);
        return depth;
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setId("searchField");
        searchField.setPromptText("Tìm kiếm theo họ tên...");
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String keyword = (newVal == null) ? "" : newVal.trim().toLowerCase();
            filteredData.setPredicate(person -> {
                if (keyword.isEmpty()) return true;
                return person.getFullName() != null
                        && person.getFullName().toLowerCase().contains(keyword);
            });
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAdd = new Button("+ Thêm thành viên");
        btnAdd.getStyleClass().add("btn-primary");
        btnAdd.setOnAction(e -> {
            if (onAddPerson != null) onAddPerson.run();
        });

        Button btnEdit = new Button("Sửa");
        btnEdit.getStyleClass().add("btn-secondary");
        btnEdit.setOnAction(e -> {
            Person selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (onEditPerson != null) onEditPerson.accept(selected);
            } else {
                showAlert("Vui lòng chọn một thành viên trong bảng để sửa.");
            }
        });

        Button btnDetail = new Button("Chi tiết");
        btnDetail.getStyleClass().add("btn-secondary");
        btnDetail.setOnAction(e -> {
            Person selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (onViewDetail != null) onViewDetail.accept(selected);
            } else {
                showAlert("Vui lòng chọn một thành viên trong bảng để xem chi tiết.");
            }
        });

        Button btnTree = new Button("Cây gia phả");
        btnTree.getStyleClass().add("btn-secondary");
        btnTree.setOnAction(e -> {
            Person selected = tableView.getSelectionModel().getSelectedItem();
            if (onViewTree != null) {
                onViewTree.accept(selected != null ? selected : (!allData.isEmpty() ? allData.get(0) : null));
            }
        });

        Button btnDelete = new Button("Xóa");
        btnDelete.getStyleClass().add("btn-danger-ghost");
        btnDelete.setOnAction(e -> {
            Person selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Xác nhận xóa");
                confirm.setHeaderText("Xóa thành viên: " + selected.getFullName());
                confirm.setContentText("Bạn có chắc chắn muốn xóa thành viên này khỏi danh sách gia phả?");
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        // Xóa qua Controller → Service → MySQL (BR4 PersonInUseException được xử lý trong controller)
                        controller.handleDelete(selected.getId());
                        // refresh() được gọi tự động bởi controller.handleDelete() nếu thành công
                    }
                });
            } else {
                showAlert("Vui lòng chọn một thành viên trong bảng để xóa.");
            }
        });

        toolbar.getChildren().addAll(searchField, spacer, btnAdd, btnEdit, btnDetail, btnTree, btnDelete);
        return toolbar;
    }

    private TableView<Person> createTable() {
        TableView<Person> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("Không có dữ liệu phù hợp"));

        // Cột ảnh đại diện — hiển thị ảnh thật bo tròn, tự fallback về chữ cái đầu
        // nếu thành viên chưa có ảnh hoặc file ảnh không tồn tại / bị hỏng.
        TableColumn<Person, Person> colPhoto = new TableColumn<>("Ảnh");
        colPhoto.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue()));
        colPhoto.setPrefWidth(60);
        colPhoto.setMaxWidth(70);
        colPhoto.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Person item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(new PersonAvatar(item, PersonAvatar.SMALL_SIZE));
                }
            }
        });

        TableColumn<Person, Number> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getId()));
        colId.setPrefWidth(50);
        colId.setMaxWidth(60);

        TableColumn<Person, String> colName = new TableColumn<>("Họ và tên");
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));
        colName.setPrefWidth(220);

        TableColumn<Person, Number> colYear = new TableColumn<>("Năm sinh");
        colYear.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getBirthYear()));
        colYear.setPrefWidth(90);
        colYear.setMaxWidth(110);

        TableColumn<Person, String> colGender = new TableColumn<>("Giới tính");
        colGender.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getGender() == Gender.MALE ? "Nam" : "Nữ"));
        colGender.setPrefWidth(90);
        colGender.setMaxWidth(110);
        colGender.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("Nam".equals(item) ? "badge-male" : "badge-female");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<Person, String> colFather = new TableColumn<>("Cha");
        colFather.setCellValueFactory(d -> {
            Person f = d.getValue().getFather();
            return new SimpleStringProperty(f != null ? f.getFullName() : "—");
        });
        colFather.setPrefWidth(160);

        TableColumn<Person, String> colMother = new TableColumn<>("Mẹ");
        colMother.setCellValueFactory(d -> {
            Person m = d.getValue().getMother();
            return new SimpleStringProperty(m != null ? m.getFullName() : "—");
        });
        colMother.setPrefWidth(160);

        table.getColumns().addAll(List.of(colPhoto, colId, colName, colYear, colGender, colFather, colMother));

        // Double click row to view detail
        table.setRowFactory(tv -> {
            TableRow<Person> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Person p = row.getItem();
                    if (onViewDetail != null) {
                        onViewDetail.accept(p);
                    }
                }
            });
            return row;
        });

        return table;
    }

    private HBox createFooterBar() {
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(10, 4, 0, 4));

        Label countLabel = new Label();
        countLabel.getStyleClass().add("stat-label");
        countLabel.textProperty().bind(
                javafx.beans.binding.Bindings.createStringBinding(
                        () -> "Hiển thị " + filteredData.size() + " trên tổng số " + allData.size() + " thành viên",
                        filteredData, allData
                )
        );

        footer.getChildren().add(countLabel);
        return footer;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public BorderPane getRoot() {
        return root;
    }

    public void setOnAddPerson(Runnable handler) {
        this.onAddPerson = handler;
    }

    public void setOnEditPerson(java.util.function.Consumer<Person> handler) {
        this.onEditPerson = handler;
    }

    public void setOnViewDetail(java.util.function.Consumer<Person> handler) {
        this.onViewDetail = handler;
    }

    public void setOnViewTree(java.util.function.Consumer<Person> handler) {
        this.onViewTree = handler;
    }

    /**
     * Làm mới danh sách bằng cách tải lại từ MySQL.
     * Được gọi bởi PersonController sau thao tác xóa thành công.
     */
    @Override
    public void refresh() {
        reloadData();
    }

    @Override
    public Person getSelectedPerson() {
        return tableView != null ? tableView.getSelectionModel().getSelectedItem() : null;
    }

    public String getSearchKeyword() {
        return searchField != null ? searchField.getText() : "";
    }
}
