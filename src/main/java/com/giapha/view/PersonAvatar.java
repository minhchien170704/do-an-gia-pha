package com.giapha.view;

import com.giapha.model.Gender;
import com.giapha.model.Person;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
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
 * <p>Cơ chế fallback (KHÔNG BAO GIỚ ném exception ra giao diện):
 * <ol>
 *     <li>Nếu {@code photoPath} hợp lệ, file tồn tại và đọc được → hiển thị ảnh thật (bo tròn).</li>
 *     <li>Nếu file không tồn tại, không đọc được, hoặc file hỏng → hiển thị chữ cái đầu
 *         của họ tên trên nền tròn màu theo giới tính.</li>
 * </ol>
 *
 * <p>Ảnh được nạp ở chế độ background loading; nếu nạp lỗi (file hỏng) thì component
 * tự chuyển sang placeholder. Việc này giúp UI không bị treo hay văng lỗi.
 *
 * <p><b>Về màu sắc:</b> component KHÔNG dùng {@code setStyle()} để gán màu nữa.
 * Mọi màu đều do CSS class đảm nhiệm (xem {@code style.css}):
 * <ul>
 *     <li>Container dùng class {@code person-avatar} (hoàn toàn trong suốt).</li>
 *     <li>Nền tròn của placeholder dùng {@code avatar-disc} + {@code avatar-male}
 *         hoặc {@code avatar-female}.</li>
 *     <li>Chữ cái đầu dùng {@code avatar-initial} + {@code avatar-male}
 *         hoặc {@code avatar-female} (màu chữ) + {@code avatar-initial-sm/md/lg} (cỡ chữ).</li>
 *     <li>Vòng tròn viền quanh ảnh thật dùng {@code avatar-ring} + {@code avatar-ring-male}
 *         hoặc {@code avatar-ring-female}.</li>
 * </ul>
 *
 * <p>Nhờ vậy màu nằm trên các <i>child</i> nên vẫn được sao chép đúng khi
 * {@code PersonFormView} cập nhật khung xem trước bằng cách thay thế children.
 */
public class PersonAvatar extends StackPane {

    /** Kích thước chuẩn của avatar trong danh sách / chi tiết. */
    public static final int DEFAULT_SIZE = 70;

    /** Kích thước avatar nhỏ dùng cho node cây gia phả. */
    public static final int SMALL_SIZE = 34;

    /** Ngưỡng kích thước để chọn cỡ chữ của chữ cái đầu. */
    private static final int SMALL_TEXT_MAX = 40;
    private static final int MEDIUM_TEXT_MAX = 55;

    /** CSS class áp dụng cho placeholder theo giới tính. */
    private static final String CLASS_INITIAL = "avatar-initial";
    private static final String CLASS_DISC = "avatar-disc";
    private static final String CLASS_MALE = "avatar-male";
    private static final String CLASS_FEMALE = "avatar-female";

    /** CSS class áp dụng cho vòng tròn viền quanh ảnh thật. */
    private static final String CLASS_RING = "avatar-ring";
    private static final String CLASS_RING_MALE = "avatar-ring-male";
    private static final String CLASS_RING_FEMALE = "avatar-ring-female";

    /** CSS class áp dụng cho cỡ chữ của chữ cái đầu. */
    private static final String CLASS_TEXT_SM = "avatar-initial-sm";
    private static final String CLASS_TEXT_MD = "avatar-initial-md";
    private static final String CLASS_TEXT_LG = "avatar-initial-lg";

    private Person person;
    private final int size;

    /** Khung xoay tròn chứa ảnh (null khi hiển thị placeholder). */
    private ImageView imageView;

    /** Nhãn chữ cái đầu dùng làm placeholder. */
    private Label initialLabel;

    /** Nền tròn của placeholder — luôn nằm trong scene graph để CSS áp dụng đúng. */
    private Circle disc;

    /** Vòng tròn viền quanh ảnh thật — luôn nằm trong scene graph để CSS áp dụng đúng. */
    private Circle ring;

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

        buildNodes();

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

        buildNodes();

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
     * Cập nhật nội dung hiển thị của avatar tại chỗ, không cần tạo component mới.
     *
     * <p><b>Vì sao cần method này?</b> Trước đây, để cập nhật khung xem trước ảnh,
     * form phải tạo một {@code PersonAvatar} mới rồi <b>lấp toàn bộ children</b> sang
     * avatar cũ (và sao chép {@code getStyle()}) — cách làm này rò rỉ trạng thái nội
     * bộ của component ra ngoài, giòn và khó bảo trì. Nay việc cập nhật được thực
     * hiện <b>tại chỗ</b> bên trong component: node nào cần hiện/ẩn thì chỉ đổi
     * {@code visible}, không thêm/bớt node — nhờ đó CSS áp dụng đúng và ổn định.
     *
     * @param person    thành viên mới (dùng cho tên + giới tính của placeholder), có thể null
     * @param imageFile file ảnh cần hiển thị, có thể null → hiển thị placeholder
     */
    public void update(Person person, File imageFile) {
        this.person = person;

        // Bắt đầu từ trạng thái placeholder cho mọi trường hợp.
        showPlaceholderState();

        // Cập nhật lại nội dung chữ cái đầu (tên có thể đã thay đổi).
        initialLabel.setText(extractInitial());

        // Cập nhật lại class theo giới tính (màu có thể đã thay đổi).
        disc.getStyleClass().removeAll(CLASS_MALE, CLASS_FEMALE);
        disc.getStyleClass().add(genderClass());
        initialLabel.getStyleClass().removeAll(CLASS_MALE, CLASS_FEMALE);
        initialLabel.getStyleClass().add(genderClass());
        ring.getStyleClass().removeAll(CLASS_RING_MALE, CLASS_RING_FEMALE);
        ring.getStyleClass().add(genderRingClass());

        if (imageFile == null || !imageFile.isFile() || !imageFile.canRead()) {
            return; // Không có ảnh hợp lệ → giữ placeholder
        }

        try {
            Image image = new Image(imageFile.toURI().toString(), true);
            if (!image.isError()) {
                attachImage(image);
            }
        } catch (Exception ex) {
            // File hỏng → giữ placeholder, tuyệt đối không ném exception ra UI
        }
    }

    /**
     * Dựng sẵn toàn bộ node con của avatar.
     *
     * <p>Tất cả node được đưa vào scene graph NGAY TỪ ĐẦU (kể cả khi đang ở chế độ
     * placeholder) để đảm bảo JavaFX CSS áp dụng màu cho chúng đúng một lần.
     * Việc chuyển chế độ chỉ đổi {@code visible} — không thêm/bớt node.
     *
     * <p>Thứ tự vẽ: {@code imageView → disc → ring → initialLabel}
     * (node sau nằm đè lên node trước).
     */
    private void buildNodes() {
        // Ảnh thật — ẩn cho tới khi nạp xong
        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        imageView.setVisible(false);

        // Bo tròn ảnh bằng clip hình tròn
        Circle clip = new Circle(size / 2.0);
        imageView.setClip(clip);

        // Nền tròn của placeholder — màu do CSS class quyết định theo giới tính
        disc = new Circle(Math.max(1.0, size / 2.0 - 1));
        disc.getStyleClass().add(CLASS_DISC);
        disc.getStyleClass().add(genderClass());
        disc.setMouseTransparent(true);
        disc.setVisible(false);

        // Vòng tròn viền quanh ảnh thật — màu do CSS class quyết định theo giới tính
        ring = new Circle(Math.max(1.0, size / 2.0 - 1));
        ring.getStyleClass().add(CLASS_RING);
        ring.getStyleClass().add(genderRingClass());
        ring.setMouseTransparent(true);
        ring.setVisible(false);

        // Chữ cái đầu — màu chữ theo giới tính, cỡ chữ theo kích thước avatar
        initialLabel = new Label(extractInitial());
        initialLabel.getStyleClass().add(CLASS_INITIAL);
        initialLabel.getStyleClass().add(genderClass());
        initialLabel.getStyleClass().add(textSizeClass());
        initialLabel.setMouseTransparent(true);

        getChildren().setAll(imageView, disc, ring, initialLabel);

        showPlaceholderState();
    }

    /**
     * Chuyển sang chế độ hiển thị placeholder (chữ cái đầu trên nền tròn).
     */
    private void showPlaceholderState() {
        if (imageView != null) {
            imageView.setImage(null);
            imageView.setVisible(false);
        }
        disc.setVisible(true);
        ring.setVisible(false);
        initialLabel.setVisible(true);
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
     * Áp dụng ảnh đã nạp lên component: hiển thị ImageView, ẩn placeholder.
     *
     * @param image đối tượng Image hợp lệ
     */
    private void applyImage(Image image) {
        if (image == null || image.isError() || image.getWidth() <= 0) {
            return;
        }

        imageView.setImage(image);
        imageView.setVisible(true);

        disc.setVisible(false);
        ring.setVisible(true);
        initialLabel.setVisible(false);
    }

    /**
     * Quay lại hiển thị placeholder (chữ cái đầu).
     */
    private void showPlaceholder() {
        showPlaceholderState();
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
        return imageView != null && imageView.getImage() != null;
    }

    /**
     * Lấy kích thước của avatar.
     *
     * @return kích thước cạnh (px)
     */
    public int getAvatarSize() {
        return size;
    }

    /**
     * CSS class theo giới tính cho placeholder (nền tròn + chữ cái đầu).
     * Người không xác định giới tính được xem là nam (nhất quán với hành vi cũ).
     */
    private String genderClass() {
        return isFemale() ? CLASS_FEMALE : CLASS_MALE;
    }

    /**
     * CSS class theo giới tính cho vòng tròn viền quanh ảnh thật.
     */
    private String genderRingClass() {
        return isFemale() ? CLASS_RING_FEMALE : CLASS_RING_MALE;
    }

    /**
     * Kiểm tra thành viên có giới tính nữ hay không.
     */
    private boolean isFemale() {
        return person != null && person.getGender() == Gender.FEMALE;
    }

    /**
     * CSS class quy định cỡ chữ của chữ cái đầu theo kích thước avatar.
     */
    private String textSizeClass() {
        if (size <= SMALL_TEXT_MAX) {
            return CLASS_TEXT_SM;
        }
        if (size <= MEDIUM_TEXT_MAX) {
            return CLASS_TEXT_MD;
        }
        return CLASS_TEXT_LG;
    }
}
