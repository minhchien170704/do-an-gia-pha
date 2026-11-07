package com.giapha.view;

import com.giapha.model.Gender;
import com.giapha.model.Person;
import com.giapha.support.FxTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử {@link PersonAvatar}: ảnh thật, file thiếu, file hỏng, không có ảnh.
 *
 * <p><b>Mục tiêu chính:</b> đảm bảo component KHÔNG BAO GIỜ ném exception ra giao diện
 * khi file ảnh không tồn tại, bị hỏng, hoặc sai định dạng.
 *
 * <p><b>Lưu ý kỹ thuật:</b> ảnh được nạp ở chế độ background. Vì vậy KHÔNG được
 * {@code sleep} trên JavaFX Application Thread (sẽ chặn việc nạp ảnh).
 * Test dựng toàn bộ avatar trên FX thread, thoát khỏi FX thread, chờ nạp xong
 * rồi mới kiểm tra.
 *
 * <p><b>Cách chạy riêng:</b> {@code mvn test -Pjavafx-tests -Dtest=AvatarFallbackTest}
 */
@Tag("javafx")
@DisplayName("PersonAvatar — cơ chế fallback ảnh đại diện")
class AvatarFallbackTest {

    /** PNG 1x1 mã hóa base64 (không dùng java.awt để tương thích module system). */
    private static final String PNG_1PX_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

    private static Path validPng;
    private static Path brokenFile;
    private static Path txtFile;

    // Avatar được dựng trên FX thread, kiểm tra sau khi nạp ảnh nền hoàn tất
    private static PersonAvatar av1, av2, av3, av4, av5, av6, av7, av8, av9;

    @BeforeAll
    static void setup() throws Exception {
        Path base = Paths.get(System.getProperty("user.dir"));
        Path imgDir = base.resolve("images");
        Files.createDirectories(imgDir);

        // Ảnh hợp lệ
        validPng = imgDir.resolve("_avatar_test_ok.png");
        Files.write(validPng, Base64.getDecoder().decode(PNG_1PX_BASE64));

        // File "hỏng" — nội dung không phải ảnh
        brokenFile = imgDir.resolve("_avatar_test_broken.png");
        Files.writeString(brokenFile, "day khong phai mot file anh hop le");

        // File sai định dạng
        txtFile = imgDir.resolve("_avatar_test.txt");
        Files.writeString(txtFile, "text");

        FxTestSupport.initToolkit();

        buildAvatars();

        // Chờ ảnh nền nạp xong (ở thread chính, KHÔNG chặn FX thread).
        // Quan trọng: KHÔNG gọi Platform.exit() trước khi ảnh nạp xong.
        Thread.sleep(2000);
    }

    @AfterAll
    static void teardown() {
        // Dọn dẹp
        try {
            Files.deleteIfExists(validPng);
            Files.deleteIfExists(brokenFile);
            Files.deleteIfExists(txtFile);
        } catch (Exception ignored) {
            // Dọn dẹp là thao tác phụ trợ
        }
        FxTestSupport.shutdownToolkit();
    }

    /**
     * Dựng toàn bộ avatar trên FX thread. KHÔNG sleep ở đây.
     */
    private static void buildAvatars() throws Exception {
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        FxTestSupport.runOnFxThread(() -> {
            try {
                Person withPhoto = new Person("Nguyễn Văn An", 1950, Gender.MALE,
                        "images/_avatar_test_ok.png");
                av1 = new PersonAvatar(withPhoto, 70);

                Person missing = new Person("Trần Thị Bình", 1952, Gender.FEMALE,
                        "images/_khong_ton_tai_9999.png");
                av2 = new PersonAvatar(missing, 70);

                Person broken = new Person("Lê Văn Cường", 1955, Gender.MALE,
                        "images/_avatar_test_broken.png");
                av3 = new PersonAvatar(broken, 70);

                Person noPhoto = new Person("Phạm Thị Dung", 1958, Gender.FEMALE, null);
                av4 = new PersonAvatar(noPhoto, 70);

                Person emptyPath = new Person("Hoàng Văn Em", 1960, Gender.MALE, "   ");
                av5 = new PersonAvatar(emptyPath, 70);

                av6 = new PersonAvatar(null, 70);

                Person wrongExt = new Person("Đặng Văn Phúc", 1962, Gender.MALE,
                        "images/_avatar_test.txt");
                av7 = new PersonAvatar(wrongExt, 70);

                av8 = new PersonAvatar(noPhoto, 60, validPng.toFile());
                av9 = new PersonAvatar(noPhoto, 60, new File("khong/ton/tai.png"));
            } catch (Throwable t) {
                errorRef.set(t);
            }
        });

        if (errorRef.get() != null) {
            throw new IllegalStateException("Exception không mong đợi khi dựng avatar: " + errorRef.get());
        }
    }

    // ============================================================
    // TEST CASES
    // ============================================================

    @Test
    @DisplayName("TC-AV-01 — ảnh thật → hiển thị ảnh thật")
    void realImageIsDisplayed() throws Exception {

        assertNotNull(av1);
        assertTrue(av1.isShowingRealImage(), "TC-AV-01: phải hiển thị ảnh thật");
    }

    @Test
    @DisplayName("TC-AV-02 — file ảnh không tồn tại → fallback placeholder")
    void missingFileFallsBack() throws Exception {
        assertNotNull(av2);
        assertFalse(av2.isShowingRealImage(), "TC-AV-02: phải fallback placeholder");
    }

    @Test
    @DisplayName("TC-AV-03 — file ảnh hỏng → fallback placeholder, KHÔNG ném exception")
    void brokenFileFallsBack() throws Exception {
        assertNotNull(av3);
        assertFalse(av3.isShowingRealImage(), "TC-AV-03: phải fallback placeholder");
    }

    @Test
    @DisplayName("TC-AV-04 — không có đường dẫn ảnh → fallback placeholder")
    void noPhotoPathFallsBack() throws Exception {
        assertNotNull(av4);
        assertFalse(av4.isShowingRealImage(), "TC-AV-04: phải fallback placeholder");
    }

    @Test
    @DisplayName("TC-AV-05 — đường dẫn ảnh rỗng → fallback placeholder")
    void blankPhotoPathFallsBack() throws Exception {
        assertNotNull(av5);
        assertFalse(av5.isShowingRealImage(), "TC-AV-05: phải fallback placeholder");
    }

    @Test
    @DisplayName("TC-AV-06 — person null → fallback placeholder, không ném exception")
    void nullPersonFallsBack() throws Exception {
        assertNotNull(av6);
        assertFalse(av6.isShowingRealImage(), "TC-AV-06: phải fallback placeholder");
    }

    @Test
    @DisplayName("TC-AV-07 — file không phải ảnh (txt) → fallback placeholder")
    void nonImageFileFallsBack() throws Exception {
        assertNotNull(av7);
        assertFalse(av7.isShowingRealImage(), "TC-AV-07: phải fallback placeholder");
    }

    @Test
    @DisplayName("TC-AV-08 — constructor với file cụ thể → hiển thị ảnh thật")
    void constructorWithSpecificFileDisplaysImage() throws Exception {
        assertNotNull(av8);
        assertTrue(av8.isShowingRealImage(), "TC-AV-08: phải hiển thị ảnh thật");
    }

    @Test
    @DisplayName("TC-AV-09 — constructor với file không tồn tại → fallback")
    void constructorWithMissingFileFallsBack() throws Exception {
        assertNotNull(av9);
        assertFalse(av9.isShowingRealImage(), "TC-AV-09: phải fallback placeholder");
    }

    @Test
    @DisplayName("TC-AV-10 — kích thước avatar đúng đặc tả")
    void avatarSizeMatchesSpecification() throws Exception {
        assertNotNull(av1);
        assertEqualsInt(70, av1.getAvatarSize(), "TC-AV-10: kích thước avatar phải bằng 70");
    }

    @Test
    @DisplayName("TC-AV-11 — resolveImageFile xử lý đường dẫn sai / null / hợp lệ")
    void resolveImageFileHandlesEdgeCases() throws Exception {
        assertNull(PersonAvatar.resolveImageFile("khong/ton/tai.png"),
                "TC-AV-11: đường dẫn sai → null");
        assertNull(PersonAvatar.resolveImageFile(null),
                "TC-AV-11b: null → null");
        assertNull(PersonAvatar.resolveImageFile("   "),
                "TC-AV-11c: chuỗi rỗng → null");
        assertNotNull(PersonAvatar.resolveImageFile("images/_avatar_test_ok.png"),
                "TC-AV-11d: đường dẫn hợp lệ → khác null");
    }

    @Test
    @DisplayName("TC-AV-12 — ảnh thật được render bằng ImageView")
    void realImageUsesImageView() throws Exception {
        assertNotNull(av1);
        boolean hasImageView = av1.getChildren().stream().anyMatch(n -> n instanceof ImageView);
        assertTrue(hasImageView, "TC-AV-12: avatar chứa ImageView khi hiển thị ảnh thật");
    }

    @Test
    @DisplayName("TC-AV-13 — placeholder vẫn là Label (chữ cái đầu)")
    void placeholderUsesLabel() throws Exception {
        assertNotNull(av4);
        boolean hasLabel = av4.getChildren().stream().anyMatch(n -> n instanceof Label);
        assertTrue(hasLabel, "TC-AV-13: avatar fallback chứa Label (chữ cái đầu)");
    }

    @Test
    @DisplayName("TC-AV-14 — ảnh thật được bo tròn (có clip hình tròn)")
    void realImageIsClippedToCircle() throws Exception {
        assertNotNull(av1);
        boolean hasClip = av1.getChildren().stream()
                .filter(n -> n instanceof ImageView)
                .anyMatch(n -> ((ImageView) n).getClip() != null);
        assertTrue(hasClip, "TC-AV-14: ảnh thật được bo tròn bằng clip Circle");
    }

    // ============================================================
    // GĐ2B.3 — update() cập nhật tại chỗ, không lấp children
    // ============================================================

    @Test
    @DisplayName("TC-AV-15 — update() với ảnh hợp lệ → hiển thị ảnh thật")
    void updateWithValidImageDisplaysImage() throws Exception {
        AtomicReference<PersonAvatar> ref = new AtomicReference<>();
        FxTestSupport.runOnFxThread(() -> {
            PersonAvatar av = new PersonAvatar(null, 60);
            ref.set(av);
            av.update(new Person("Nguyễn Văn Update", 1990, Gender.MALE, null), validPng.toFile());
        });
        Thread.sleep(1200);

        PersonAvatar av = ref.get();
        assertNotNull(av, "TC-AV-15: avatar phải được dựng");
        assertTrue(av.isShowingRealImage(),
                "TC-AV-15: sau update() với file ảnh hợp lệ phải hiển thị ảnh thật");
    }

    @Test
    @DisplayName("TC-AV-16 — update() với file không tồn tại → fallback placeholder, KHÔNG ném exception")
    void updateWithMissingFileFallsBack() throws Exception {
        AtomicReference<PersonAvatar> ref = new AtomicReference<>();
        FxTestSupport.runOnFxThread(() -> {
            PersonAvatar av = new PersonAvatar(null, 60);
            ref.set(av);
            av.update(new Person("Trần Thị Update", 1992, Gender.FEMALE, null),
                    new File("khong/ton/tai.png"));
        });
        Thread.sleep(800);

        PersonAvatar av = ref.get();
        assertNotNull(av, "TC-AV-16: avatar phải được dựng");
        assertFalse(av.isShowingRealImage(),
                "TC-AV-16: file ảnh không tồn tại → phải fallback placeholder");
    }

    @Test
    @DisplayName("TC-AV-17 — update() không thêm/bớt node (giữ nguyên cấu trúc scene graph)")
    void updateKeepsSameNodes() throws Exception {
        AtomicReference<PersonAvatar> ref = new AtomicReference<>();
        AtomicReference<Integer> beforeCount = new AtomicReference<>();
        AtomicReference<Integer> afterCount = new AtomicReference<>();
        AtomicReference<Object> beforeImage = new AtomicReference<>();
        AtomicReference<Object> afterImage = new AtomicReference<>();

        FxTestSupport.runOnFxThread(() -> {
            PersonAvatar av = new PersonAvatar(null, 60);
            ref.set(av);
            beforeCount.set(av.getChildren().size());
            beforeImage.set(av.getChildren().stream()
                    .filter(n -> n instanceof ImageView)
                    .findFirst().orElse(null));

            av.update(new Person("Lê Văn Update", 1995, Gender.MALE, null), null);

            afterCount.set(av.getChildren().size());
            afterImage.set(av.getChildren().stream()
                    .filter(n -> n instanceof ImageView)
                    .findFirst().orElse(null));
        });

        PersonAvatar av = ref.get();
        assertNotNull(av, "TC-AV-17: avatar phải được dựng");
        assertEqualsInt(beforeCount.get(), afterCount.get(),
                "TC-AV-17: update() không được thêm/bớt node — chỉ đổi visible");
        assertTrue(beforeImage.get() == afterImage.get(),
                "TC-AV-17: update() phải tái sử dụng ĐÚNG ImageView cũ, không tạo node mới");
    }

    @Test
    @DisplayName("TC-AV-18 — update() đổi giới tính → đổi class CSS của placeholder")
    void updateChangesGenderCssClass() throws Exception {
        AtomicReference<PersonAvatar> ref = new AtomicReference<>();
        AtomicReference<Boolean> maleBefore = new AtomicReference<>();
        AtomicReference<Boolean> maleAfter = new AtomicReference<>();

        FxTestSupport.runOnFxThread(() -> {
            PersonAvatar av = new PersonAvatar(
                    new Person("Phạm Thị Dung", 1998, Gender.FEMALE, null), 60);
            ref.set(av);

            maleBefore.set(av.getStyleClass().contains("avatar-female")
                    || hasClass(av, "avatar-female"));

            av.update(new Person("Phạm Văn Hùng", 1998, Gender.MALE, null), null);

            maleAfter.set(av.getStyleClass().contains("avatar-male")
                    || hasClass(av, "avatar-male"));
        });

        PersonAvatar av = ref.get();
        assertNotNull(av, "TC-AV-18: avatar phải được dựng");
        assertTrue(Boolean.TRUE.equals(maleBefore.get()),
                "TC-AV-18: ban đầu nữ → placeholder phải có class avatar-female");
        assertTrue(Boolean.TRUE.equals(maleAfter.get()),
                "TC-AV-18: sau update() nam → placeholder phải có class avatar-male");
    }

    /**
     * Kiểm tra một node con nào đó có chứa class CSS cụ thể hay không.
     */
    private static boolean hasClass(PersonAvatar avatar, String cssClass) {
        return avatar.getChildren().stream()
                .filter(n -> n instanceof javafx.scene.control.Label)
                .anyMatch(n -> ((javafx.scene.control.Label) n).getStyleClass().contains(cssClass));
    }

    private static void assertEqualsInt(int expected, int actual, String message) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
    }
}

