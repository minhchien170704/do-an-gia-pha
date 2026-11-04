package com.giapha.view;

import com.giapha.model.Gender;
import com.giapha.model.Person;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.Label;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Thành phần hiển thị ảnh đại diện của một thành viên gia phả.
 *
 * <p>Được dùng lại ở nhiều màn hình: danh sách thành viên, chi tiết hồ sơ,
 * node của cây gia phả và form thêm/sửa.
 *
 * <p>Cơ chế fallback (KHÔNG BAO GIỜ ném exception ra giao diện):
 * <ol>
 *     <li>Nếu {@code photoPath} hợp lệ, file tồn tại và đọc được → hiển thị ảnh thật (bo tròn).</li>
 *     <li>Nếu file không tồn tại, không đọc được, hoặc file hỏng → hiển thị chữ cái đầu
 *         của họ tên trên nền tròn màu theo giới tính.</li>
 * </ol>
 *
 * <p>Ảnh được nạp ở chế độ background loading; nếu nạp lỗi (file hỏng) thì component
 * tự chuyển sang placeholder. Việc này giúp UI không bị treo hay văng lỗi.
 */
public class PersonAvatar extends StackPane {

    /** Kích thước chuẩn của avatar trong danh sách / chi tiết. */
    public static final int DEFAULT_SIZE = 70;

    /** Kích thước avatar nhỏ dùng cho node cây gia phả. */
    public static final int SMALL_SIZE = 34;

    private static final String MALE_BG = "#EBF0F7";
    private static final String MALE_BORDER = "#3D5A80";
    private static final String FEMALE_BG = "#F3EBF0";
    private static final String FEMALE_BORDER = "#7A4D6E";

    private final Person person;
    private final int size;

    /** Khung xoay tròn chứa ảnh (null khi hiển thị placeholder). */
    private ImageView imageView;

    /** Nhãn chữ cái đầu dùng làm placeholder. */
    private Label initialLabel;

    public PersonAvatar(Person person) {
        this(person, DEFAULT_SIZE);
    }

    /**
     * Tạo avatar cho một thành viên với kích thước tuỳ chỉnh.
     *
     * @param person thành viên cần hiển thị ảnh (có thể null → hiển thị placeholder)
     * @param size   kích thước cạnh của avatar (px)
     */
    public PersonAvatar(Person person, int size) {
        this.person = person;
        this.size = size;

        getStyleClass().add("person-avatar");
        setPrefSize(size, size);
        setMinSize(size, size);
        setMaxSize(size, size);

        buildPlaceholder();

        // Thử nạp ảnh thật — nếu thất bại thì giữ nguyên placeholder
        try {
            loadImageIfAvailable();
        } catch (Exception ex) {
            // Ảnh lỗi → giữ placeholder, tuyệt đối không ném exception ra UI
        }
    }

    /**
     * Tạo avatar hiển thị một file ảnh cụ thể (dùng cho xem trước ảnh vừa chọn
     * trong form, trước khi ảnh được copy và lưu vào DB).
     *
     * <p>Nếu file không tồn tại hoặc bị hỏng → tự fallback về placeholder.
     *
     * @param person      thành viên (dùng cho tên và giới tính placeholder)
     * @param size        kích thước avatar (px)
     * @param imageFile   file ảnh cụ thể cần hiển thị (có thể null)
     */
    public PersonAvatar(Person person, int size, File imageFile) {
        this.person = person;
        this.size = size;

        getStyleClass().add("person-avatar");
        setPrefSize(size, size);
        setMinSize(size, size);
        setMaxSize(size, size);

        buildPlaceholder();

        try {
            if (imageFile != null && imageFile.isFile() && imageFile.canRead()) {
                Image image = new Image(imageFile.toURI().toString(), true);
                if (!image.isError()) {
                    attachImage(image);
                }
            }
        } catch (Exception ex) {
            // File hỏng → giữ placeholder
        }
    }

    /**
     * Dựng phần placeholder: nền tròn + chữ cái đầu của họ tên.
     */
    private void buildPlaceholder() {
        boolean isMale = person == null || person.getGender() == Gender.MALE;
        String bg = isMale ? MALE_BG : FEMALE_BG;
        String border = isMale ? MALE_BORDER : FEMALE_BORDER;

        setStyle("-fx-background-color: " + bg + "; "
                + "-fx-background-radius: " + (size / 2) + "; "
                + "-fx-border-color: " + border + "; "
                + "-fx-border-radius: " + (size / 2) + "; "
                + "-fx-border-width: 1.5;");

        initialLabel = new Label(extractInitial());
        initialLabel.setStyle("-fx-font-size: " + Math.max(11, size / 3) + "px; "
                + "-fx-font-weight: bold; -fx-text-fill: " + border + ";");
        getChildren().setAll(initialLabel);
    }

    /**
     * Gắn listener cho một đối tượng {@link Image} đang nạp nền và áp dụng ảnh
     * vào component ngay khi nạp xong.
     *
     * <p>Dùng {@code progressProperty} ( tín hiệu chuẩn của JavaFX cho ảnh nạp nền )
     * và kiểm tra thêm trạng thái hiện tại, vì ảnh có thể đã nạp xong trước khi
     * listener được đăng ký — dẫn tới ảnh không bao giờ hiển thị.
     *
     * @param image đối tượng Image cần theo dõi
     */
    private void attachImage(Image image) {
        if (image == null || image.isError()) {
            return;
        }

        // Nạp lỗi (file hỏng) → quay về placeholder
        image.errorProperty().addListener((obs, oldVal, newVal) -> {
            if (Boolean.TRUE.equals(newVal)) {
                showPlaceholder();
            }
        });

        // Nạp xong → hiển thị ảnh thật
        image.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() >= 1.0) {
                applyImage(image);
            }
        });

        // Trường hợp ảnh đã nạp xong trước khi gắn listener → áp dụng luôn
        if (image.getProgress() >= 1.0) {
            applyImage(image);
        }
    }

    /**
     * Nạp ảnh thật từ {@code photoPath} nếu file tồn tại và đọc được.
     *
     * <p>Ảnh được nạp ở chế độ nền (background loading) để không chặn luồng UI;
     * khi nạp xong mới áp dụng vào component. Nếu nạp lỗi (file hỏng) thì
     * component tự chuyển về placeholder.
     */
    private void loadImageIfAvailable() {
        String photoPath = person == null ? null : person.getPhotoPath();
        if (photoPath == null || photoPath.trim().isEmpty()) {
            return;
        }

        File imageFile = resolveImageFile(photoPath.trim());
        if (imageFile == null || !imageFile.isFile() || !imageFile.canRead()) {
            return; // File không tồn tại hoặc không đọc được → giữ placeholder
        }

        Image image;
        try {
            image = new Image(imageFile.toURI().toString(), true); // background loading
        } catch (Exception ex) {
            return; // File hỏng / không phải ảnh hợp lệ → giữ placeholder
        }

        if (image.isError()) {
            return; // Lỗi tải ảnh (file hỏng) → giữ placeholder
        }

        attachImage(image);
    }

    /**
     * Áp dụng ảnh đã nạp lên component: tạo ImageView, bo tròn, ẩn placeholder.
     *
     * @param image đối tượng Image hợp lệ
     */
    private void applyImage(Image image) {
        if (image == null || image.isError() || image.getWidth() <= 0) {
            return;
        }

        imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);

        // Bo tròn ảnh bằng clip hình tròn
        Circle clip = new Circle(size / 2.0);
        imageView.setClip(clip);

        getChildren().setAll(imageView);
        setStyle("-fx-background-color: transparent; "
                + "-fx-border-color: " + (person != null && person.getGender() == Gender.FEMALE
                    ? FEMALE_BORDER : MALE_BORDER) + "; "
                + "-fx-border-radius: " + (size / 2) + "; "
                + "-fx-border-width: 1.5;");
    }

    /**
     * Quay lại hiển thị placeholder (chữ cái đầu).
     */
    private void showPlaceholder() {
        imageView = null;
        buildPlaceholder();
    }

    /**
     * Phân giải đường dẫn ảnh trong DB thành File trên đĩa.
     *
     * <p>Đường dẫn trong DB là TƯƠNG ĐỐI (ví dụ {@code images/person_1.png}) tính từ
     * thư mục làm việc của ứng dụng. Nếu không tìm thấy theo đường dẫn tương đối,
     * thử thêm theo đường dẫn tuyệt đối (phòng trường hợp dữ liệu cũ).
     *
     * @param photoPath đường dẫn ảnh lấy từ DB
     * @return File ảnh, hoặc null nếu không phân giải được
     */
    public static File resolveImageFile(String photoPath) {
        if (photoPath == null || photoPath.trim().isEmpty()) {
            return null;
        }
        try {
            Path base = Paths.get(System.getProperty("user.dir"));
            Path relative = base.resolve(photoPath.trim()).normalize();
            if (Files.isRegularFile(relative)) {
                return relative.toFile();
            }
            // Dự phòng: đường dẫn tuyệt đối
            Path absolute = Paths.get(photoPath.trim());
            if (Files.isRegularFile(absolute)) {
                return absolute.toFile();
            }
        } catch (Exception ex) {
            // Đường dẫn lỗi → coi như không có ảnh
        }
        return null;
    }

    /**
     * Lấy chữ cái đầu của họ tên để làm placeholder.
     *
     * @return một ký tự in hoa, hoặc "?" nếu không có tên
     */
    private String extractInitial() {
        if (person == null || person.getFullName() == null || person.getFullName().trim().isEmpty()) {
            return "?";
        }
        String name = person.getFullName().trim();
        int lastSpace = name.lastIndexOf(' ');
        if (lastSpace >= 0 && lastSpace + 1 < name.length()) {
            return name.substring(lastSpace + 1, lastSpace + 2).toUpperCase();
        }
        return name.substring(0, 1).toUpperCase();
    }

    /**
     * Trả về true nếu avatar đang hiển thị ảnh thật (không phải placeholder).
     * Dùng cho mục đích kiểm thử.
     *
     * @return true nếu đang hiển thị ảnh thật
     */
    public boolean isShowingRealImage() {
        return imageView != null;
    }

    /**
     * Lấy kích thước của avatar.
     *
     * @return kích thước cạnh (px)
     */
    public int getAvatarSize() {
        return size;
    }
}
