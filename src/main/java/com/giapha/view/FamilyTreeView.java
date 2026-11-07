package com.giapha.view;

import com.giapha.controller.PersonController;
import com.giapha.model.Gender;
import com.giapha.model.Person;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
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

    /** Sơ đồ cây gia phả 2D (vẽ bằng Pane + Line). */
    private final FamilyTreeDiagram diagram;

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

        // --- Tab 1: Sơ đồ 2D (khởi tạo TRƯỚC toolbar vì toolbar tham chiếu diagram) ---
        diagram = new FamilyTreeDiagram();
        diagram.setOnViewPerson(p -> {
            if (onViewPerson != null) onViewPerson.accept(p);
        });

        // Toolbar: Selector + Legend + Zoom + Xuất/In
        HBox toolbar = createToolbar();

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("tree-tab-pane");

        Tab tabDiagram = new Tab("Sơ đồ 2D", diagram);
        tabDiagram.setClosable(false);
        tabDiagram.getStyleClass().add("tree-tab");

        // --- Tab 2: Cây dạng danh sách (TreeView cũ) ---
        treeView = new TreeView<>();
        treeView.setShowRoot(true);
        treeView.getStyleClass().add("tree-view");
        VBox.setVgrow(treeView, Priority.ALWAYS);
        setupTreeCellFactory();

        Tab tabTree = new Tab("Danh sách phả hệ", treeView);
        tabTree.setClosable(false);
        tabTree.getStyleClass().add("tree-tab");

        tabPane.getTabs().addAll(tabDiagram, tabTree);

        // Vẽ sơ đồ 2D sau khi khung cửa sổ đã có kích thước thực
        javafx.application.Platform.runLater(this::layoutDiagram);
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == tabDiagram) {
                layoutDiagram();
            }
        });

        centerContent.getChildren().addAll(toolbar, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
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
     * Cập nhật sơ đồ 2D dựa trên người trung tâm hiện tại.
     */
    private void layoutDiagram() {
        if (diagram == null || cboFocusPerson == null) return;
        Person focus = cboFocusPerson.getValue();
        if (focus == null) return;
        diagram.setData(allPersons, focus);
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

        // --- Điều khiển zoom sơ đồ 2D ---
        HBox zoomBox = new HBox(6);
        zoomBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnZoomOut = new Button("−");
        btnZoomOut.getStyleClass().add("btn-secondary");
        btnZoomOut.setPrefWidth(38);
        btnZoomOut.setTooltip(new javafx.scene.control.Tooltip("Thu nhỏ"));
        btnZoomOut.setOnAction(e -> diagram.zoomBy(1 / 1.2));

        Label lblZoom = new Label("100%");
        lblZoom.getStyleClass().add("stat-label");
        lblZoom.setMinWidth(52);
        lblZoom.setAlignment(Pos.CENTER);

        Button btnZoomIn = new Button("+");
        btnZoomIn.getStyleClass().add("btn-secondary");
        btnZoomIn.setPrefWidth(38);
        btnZoomIn.setTooltip(new javafx.scene.control.Tooltip("Phóng to"));
        btnZoomIn.setOnAction(e -> diagram.zoomBy(1.2));

        Button btnZoomReset = new Button("Vừa khung");
        btnZoomReset.getStyleClass().add("btn-secondary");
        btnZoomReset.setTooltip(new javafx.scene.control.Tooltip("Đưa toàn bộ sơ đồ vào khung nhìn"));
        btnZoomReset.setOnAction(e -> diagram.resetZoom());

        zoomBox.getChildren().addAll(new Label("Zoom:"), btnZoomOut, lblZoom, btnZoomIn, btnZoomReset);

        // --- Xuất ảnh & in ---
        HBox exportBox = new HBox(6);
        exportBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnExportPng = new Button("🖼 Xuất PNG");
        btnExportPng.getStyleClass().add("btn-secondary");
        btnExportPng.setTooltip(new javafx.scene.control.Tooltip("Lưu sơ đồ cây gia phả thành file PNG"));
        btnExportPng.setOnAction(e -> handleExportPng());

        Button btnPrint = new Button("🖨 In");
        btnPrint.getStyleClass().add("btn-secondary");
        btnPrint.setTooltip(new javafx.scene.control.Tooltip("In sơ đồ cây gia phả qua máy in"));
        btnPrint.setOnAction(e -> handlePrint());

        exportBox.getChildren().addAll(btnExportPng, btnPrint);

        HBox legend = createLegendPills();

        toolbar.getChildren().addAll(lblSelect, cboFocusPerson, spacer, zoomBox, exportBox, legend);

        // Cập nhật nhãn % khi zoom thay đổi
        diagram.contentScaleProperty().addListener((obs, oldVal, newVal) -> {
            lblZoom.setText(Math.round(newVal.doubleValue() * 100) + "%");
        });

        return toolbar;
    }

    /**
     * Xuất sơ đồ cây gia phả ra file PNG.
     *
     * <p>Dùng {@code Node.snapshot()} trên lớp nội dung của sơ đồ (với viewport
     * bằng đúng kích thước nội dung nên ảnh không bị cắt), rồi ghi file PNG bằng
     * {@link com.giapha.util.PngWriter}.
     */
    private void handleExportPng() {
        javafx.scene.image.WritableImage image = diagram.renderFullImage();
        if (image == null) {
            showAlert(Alert.AlertType.WARNING, "Không thể xuất ảnh",
                    "Sơ đồ cây gia phả hiện đang trống — không có dữ liệu để xuất.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Xuất sơ đồ cây gia phả");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Ảnh PNG (*.png)", "*.png"));
        chooser.setInitialFileName(defaultExportFileName());

        File file = chooser.showSaveDialog(root.getScene() == null ? null : root.getScene().getWindow());
        if (file == null) {
            return; // Người dùng đã hủy
        }

        // Đảm bảo đuôi file là .png
        if (!file.getName().toLowerCase().endsWith(".png")) {
            file = new File(file.getParentFile(), file.getName() + ".png");
        }

        try {
            com.giapha.util.PngWriter.write(image, file);
            showAlert(Alert.AlertType.INFORMATION, "Xuất PNG thành công",
                    "Đã lưu sơ đồ cây gia phả tại:\n" + file.getAbsolutePath());
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Xuất PNG thất bại",
                    "Không thể ghi file PNG:\n" + ex.getMessage());
        }
    }

    /**
     * In sơ đồ cây gia phả bằng {@link PrinterJob} của JavaFX (không thêm dependency).
     *
     * <p>Sơ đồ được đưa vào một {@code Group} với kích thước bằng kích thước
     * nội dung, sau đó in trang đầu tiên qua {@code PrinterJob}.
     */
    private void handlePrint() {
        javafx.scene.image.WritableImage image = diagram.renderFullImage();
        if (image == null) {
            showAlert(Alert.AlertType.WARNING, "Không thể in",
                    "Sơ đồ cây gia phả hiện đang trống — không có dữ liệu để in.");
            return;
        }

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            showAlert(Alert.AlertType.ERROR, "Không tìm thấy máy in",
                    "Không phát hiện được máy in nào trên hệ thống. "
                            + "Vui lòng cài đặt máy in hoặc dùng chức năng Xuất PNG.");
            return;
        }

        if (!confirmPrint(job)) {
            return; // Người dùng đã hủy
        }

        try {
            // Đóng gói sơ đồ dưới dạng một Node có thể in được
            javafx.scene.Node printable = buildPrintableNode(job);
            boolean printed = job.printPage(printable);
            if (printed) {
                showAlert(Alert.AlertType.INFORMATION, "Đã gửi lệnh in",
                        "Sơ đồ cây gia phả đã được gửi tới máy in.");
            } else {
                showAlert(Alert.AlertType.WARNING, "Không in được",
                        "Máy in từ chối yêu cầu in. Vui lòng kiểm tra lại cài đặt máy in.");
            }
        } catch (RuntimeException ex) {
            showAlert(Alert.AlertType.ERROR, "In thất bại",
                    "Không thể in sơ đồ cây gia phả:\n" + ex.getMessage());
        }
    }

    /**
     * Dựng Node in được từ ảnh đã chụp của sơ đồ.
     *
     * <p>Ảnh được đặt trong một {@code Group} có kích thước vừa khung giấy A4 ngang,
     * canh giữa, để bản in không bị cắt.
     */
    private javafx.scene.Node buildPrintableNode(PrinterJob job) {
        javafx.scene.image.WritableImage image = diagram.renderFullImage();
        javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);
        imageView.setPreserveRatio(true);

        PageLayout pageLayout = job.getJobSettings() != null
                ? job.getJobSettings().getPageLayout()
                : null;
        double pageWidth = pageLayout != null ? pageLayout.getPrintableWidth() : 794.0;  // A4 ngang (pt)
        double pageHeight = pageLayout != null ? pageLayout.getPrintableHeight() : 561.0;
        double margin = 24;

        double maxW = Math.max(100, pageWidth - margin * 2);
        double maxH = Math.max(100, pageHeight - margin * 2);

        imageView.setFitWidth(maxW);
        imageView.setFitHeight(maxH);

        // Bọc ảnh trong một StackPane để đặt được kích thước trang
        javafx.scene.layout.StackPane page = new javafx.scene.layout.StackPane(imageView);
        page.setPadding(new Insets(margin));
        page.setPrefWidth(pageWidth);
        page.setPrefHeight(pageHeight);
        page.setMinWidth(pageWidth);
        page.setMinHeight(pageHeight);
        page.setBackground(new javafx.scene.layout.Background(
                new javafx.scene.layout.BackgroundFill(
                        javafx.scene.paint.Color.WHITE, null, null)));

        return page;
    }

    /**
     * Hiển thị hộp thoại xác nhận in.
     *
     * <p>Trước tiên thử mở hộp thoại cấu hình in chuẩn của JavaFX
     * ({@code PrinterJob.showPrintDialog}). Nếu hệ thống không hỗ trợ (không có máy in
     * hoặc nền tảng không cung cấp hộp thoại), hiển thị hộp thoại xác nhận đơn giản
     * của ứng dụng.
     *
     * @param job lệnh in
     * @return {@code true} nếu người dùng xác nhận in
     */
    private boolean confirmPrint(PrinterJob job) {
        try {
            if (job.showPrintDialog(root.getScene() == null ? null : root.getScene().getWindow())) {
                return true;
            }
            // Hộp thoại hệ thống bị đóng bằng nút Hủy → hủy luôn
            return false;
        } catch (Throwable unsupported) {
            // Nền tảng không hỗ trợ hộp thoại in → dùng hộp thoại nội bộ
        }

        final boolean[] confirmed = {false};

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Xác nhận in sơ đồ cây gia phả");
        dialog.setHeaderText("In sơ đồ cây gia phả?");
        dialog.setContentText("Sơ đồ sẽ được gửi tới máy in mặc định của hệ thống.");
        dialog.initOwner(root.getScene() == null ? null : root.getScene().getWindow());

        ButtonType btnPrint = new ButtonType("In", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnPrint, ButtonType.CANCEL);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton != null && dialogButton.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                confirmed[0] = true;
                return ButtonType.OK;
            }
            return null;
        });

        dialog.showAndWait();
        return confirmed[0];
    }

    /**
     * Tên file mặc định khi xuất PNG, dựa trên tên người trung tâm.
     */
    private String defaultExportFileName() {
        Person focus = cboFocusPerson != null ? cboFocusPerson.getValue() : null;
        String base = focus != null ? focus.getFullName() : "gia_pha";
        base = base.replaceAll("[^\\p{L}\\p{N}]+", "_").replaceAll("^_+|_+$", "");
        if (base.isEmpty()) base = "gia_pha";
        return "gia_pha_" + base + ".png";
    }

    /**
     * Hiển thị hộp thoại thông báo.
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.initOwner(root.getScene() == null ? null : root.getScene().getWindow());
        alert.showAndWait();
    }

    private HBox createLegendPills() {        HBox box = new HBox(12);
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

        // Đồng bộ sơ đồ 2D với người trung tâm hiện tại
        layoutDiagram();
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
                String genderStr = person.getGender().getDisplayName();
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

            Label genderBadge = new Label(person.getGender().getDisplayName());
            genderBadge.getStyleClass().add(person.getGender().getBadgeCssClass());

            card.getChildren().addAll(nameLabel, detailLabel, genderBadge);
            return card;
        }
    }
}
