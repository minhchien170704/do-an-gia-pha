package com.giapha;

import com.giapha.model.Person;
import com.giapha.view.*;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * Entry point cho ứng dụng Sơ đồ gia phả.
 * Quản lý bố cục ứng dụng với Sidebar điều hướng nhất quán và chuyển đổi màn hình mượt mà.
 */
public class Main extends Application {

    private Stage primaryStage;
    private Scene scene;
    private BorderPane mainLayout;

    private HBox navItemPeople;
    private HBox navItemTree;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Sơ đồ gia phả — Quản lý gia phả dòng họ");
        primaryStage.setMinWidth(980);
        primaryStage.setMinHeight(640);

        mainLayout = new BorderPane();
        mainLayout.getStyleClass().add("root");

        // Sidebar trái nhất quán
        VBox sidebar = createSidebar();
        mainLayout.setLeft(sidebar);

        // Mặc định mở PeopleListView
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

        // Nav Item: Thành viên
        navItemPeople = createNavItem("Thành viên dòng họ", () -> showPeopleList());

        // Nav Item: Cây gia phả
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

    private void showPeopleList() {
        setActiveNav(navItemPeople);

        PeopleListView view = new PeopleListView();
        view.setOnAddPerson(() -> showPersonForm(null));
        view.setOnEditPerson(this::showPersonForm);
        view.setOnViewDetail(this::showPersonDetail);
        view.setOnViewTree(this::showFamilyTree);

        mainLayout.setCenter(view.getRoot());
    }

    private void showPersonForm(Person personToEdit) {
        setActiveNav(navItemPeople);

        PersonFormView view = new PersonFormView(personToEdit);
        view.setOnSave(this::showPeopleList);
        view.setOnCancel(this::showPeopleList);

        mainLayout.setCenter(view.getRoot());
    }

    private void showPersonDetail(Person person) {
        setActiveNav(navItemPeople);

        PersonDetailView view = new PersonDetailView(person);
        view.setOnBack(this::showPeopleList);
        view.setOnEdit(this::showPersonForm);
        view.setOnViewTree(this::showFamilyTree);

        mainLayout.setCenter(view.getRoot());
    }

    private void showFamilyTree(Person focusPerson) {
        setActiveNav(navItemTree);

        FamilyTreeView view = new FamilyTreeView(focusPerson);
        view.setOnBack(this::showPeopleList);
        view.setOnViewPerson(this::showPersonDetail);

        mainLayout.setCenter(view.getRoot());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
