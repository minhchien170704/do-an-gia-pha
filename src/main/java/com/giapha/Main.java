package com.giapha;

import com.giapha.controller.PersonController;
import com.giapha.model.Person;
import com.giapha.repository.JdbcPersonRepository;
import com.giapha.service.FamilyTreeService;
import com.giapha.view.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * Entry point cho ứng dụng Sơ đồ gia phả.
 * Khởi tạo tầng backend (Repository → Service → Controller) một lần duy nhất,
 * sau đó inject PersonController vào mọi View để toàn bộ CRUD đi qua MySQL.
 */
public class Main extends Application {

    private Stage primaryStage;
    private Scene scene;
    private BorderPane mainLayout;

    private HBox navItemPeople;
    private HBox navItemTree;

    /**
     * Controller duy nhất — được inject vào mọi View.
     * Giữ tham chiếu ổn định trong suốt vòng đời ứng dụng.
     */
    private PersonController controller;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Sơ đồ gia phả — Quản lý gia phả dòng họ");
        primaryStage.setMinWidth(980);
        primaryStage.setMinHeight(640);

        // Khởi tạo Repository → Service → Controller
        if (!initBackend()) {
            return; // initBackend đã hiện lỗi và gọi Platform.exit()
        }

        mainLayout = new BorderPane();
        mainLayout.getStyleClass().add("root");

        // Đăng ký callback điều hướng để Controller có thể navigate sau CRUD
        controller.setNavigateToPeopleList(this::showPeopleList);
        controller.setNavigateToPersonForm(this::showPersonForm);
        controller.setNavigateToPersonDetail(this::showPersonDetail);
        controller.setNavigateToFamilyTree(this::showFamilyTree);

        // Sidebar trái nhất quán
        VBox sidebar = createSidebar();
        mainLayout.setLeft(sidebar);

        // Mặc định mở PeopleListView (đọc từ MySQL)
        showPeopleList();

        scene = new Scene(mainLayout, 1200, 750);
        String cssPath = getClass().getResource("/css/style.css") != null
                ? getClass().getResource("/css/style.css").toExternalForm()
                : null;
        if (cssPath != null) {
            scene.getStylesheets().add(cssPath);
        }

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Khởi tạo JdbcPersonRepository → FamilyTreeService → PersonController.
     * Nếu MySQL không kết nối được, hiện Alert và thoát.
     *
     * @return true nếu thành công, false nếu lỗi (đã gọi Platform.exit())
     */
    private boolean initBackend() {
        try {
            JdbcPersonRepository repository = new JdbcPersonRepository();
            FamilyTreeService service = new FamilyTreeService(repository);
            controller = new PersonController(service);
            return true;
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi khởi tạo ứng dụng");
            alert.setHeaderText("Không thể kết nối cơ sở dữ liệu");
            alert.setContentText(
                    "Lỗi: " + e.getMessage()
                    + "\n\nVui lòng kiểm tra:\n"
                    + "  • MySQL/MariaDB đang chạy trên cổng 3306\n"
                    + "  • Database 'gia_pha' đã được tạo (chạy database/schema.sql)\n"
                    + "  • Tài khoản 'root' có quyền truy cập\n"
                    + "  • Có thể override qua biến môi trường DB_URL, DB_USER, DB_PASSWORD"
            );
            alert.showAndWait();
            Platform.exit();
            return false;
        }
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(230);
        sidebar.setMinWidth(230);
        sidebar.setMaxWidth(230);

        // Brand
        VBox brand = new VBox(3);
        brand.getStyleClass().add("sidebar-brand");

        Label brandSubtitle = new Label("HỆ THỐNG");
        brandSubtitle.getStyleClass().add("sidebar-title");

        Label brandTitle = new Label("Sơ đồ Gia phả");
        brandTitle.getStyleClass().add("sidebar-app-name");

        brand.getChildren().addAll(brandSubtitle, brandTitle);

        // Section Label
        Label navLabel = new Label("DANH MỤC QUẢN LÝ");
        navLabel.getStyleClass().add("nav-section-label");

        // Nav Items
        navItemPeople = createNavItem("Thành viên dòng họ", this::showPeopleList);
        navItemTree = createNavItem("Cây gia phả trực quan", () -> showFamilyTree(null));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Footer
        VBox footer = new VBox(2);
        footer.getStyleClass().add("sidebar-footer");

        Label ver = new Label("Đồ án OOP • Phiên bản 1.0");
        ver.getStyleClass().add("sidebar-version");

        footer.getChildren().add(ver);

        sidebar.getChildren().addAll(brand, navLabel, navItemPeople, navItemTree, spacer, footer);
        return sidebar;
    }

    private HBox createNavItem(String title, Runnable action) {
        HBox item = new HBox();
        item.getStyleClass().add("nav-item");
        item.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(title);
        label.getStyleClass().add("nav-item-label");

        item.getChildren().add(label);
        item.setOnMouseClicked(e -> action.run());
        return item;
    }

    private void setActiveNav(HBox activeItem) {
        if (navItemPeople != null) {
            navItemPeople.getStyleClass().remove("nav-item-active");
        }
        if (navItemTree != null) {
            navItemTree.getStyleClass().remove("nav-item-active");
        }
        if (activeItem != null && !activeItem.getStyleClass().contains("nav-item-active")) {
            activeItem.getStyleClass().add("nav-item-active");
        }
    }

    /**
     * Hiển thị màn hình danh sách, inject controller, cập nhật view reference trên controller.
     */
    private void showPeopleList() {
        setActiveNav(navItemPeople);

        PeopleListView view = new PeopleListView(controller);
        view.setOnAddPerson(() -> showPersonForm(null));
        view.setOnEditPerson(this::showPersonForm);
        view.setOnViewDetail(this::showPersonDetail);
        view.setOnViewTree(this::showFamilyTree);

        // Cập nhật view reference để controller.handleDelete() có thể gọi view.refresh()
        controller.setPeopleListView(view);

        mainLayout.setCenter(view.getRoot());
    }

    /**
     * Hiển thị form thêm/sửa, inject controller, cập nhật view reference trên controller.
     */
    private void showPersonForm(Person personToEdit) {
        setActiveNav(navItemPeople);

        PersonFormView view = new PersonFormView(controller, personToEdit);
        view.setOnCancel(this::showPeopleList);

        // Cập nhật view reference để controller có thể đọc input nếu cần
        controller.setPersonFormView(view);

        mainLayout.setCenter(view.getRoot());
    }

    /**
     * Hiển thị chi tiết một thành viên, inject controller để lấy danh sách con từ DB.
     */
    private void showPersonDetail(Person person) {
        setActiveNav(navItemPeople);

        PersonDetailView view = new PersonDetailView(controller, person);
        view.setOnBack(this::showPeopleList);
        view.setOnEdit(this::showPersonForm);
        view.setOnViewTree(this::showFamilyTree);

        mainLayout.setCenter(view.getRoot());
    }

    /**
     * Hiển thị cây gia phả, inject controller để lấy tổ tiên/hậu duệ từ DB.
     */
    private void showFamilyTree(Person focusPerson) {
        setActiveNav(navItemTree);

        FamilyTreeView view = new FamilyTreeView(controller, focusPerson);
        view.setOnBack(this::showPeopleList);
        view.setOnViewPerson(this::showPersonDetail);

        mainLayout.setCenter(view.getRoot());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
