package com.giapha.view;

import com.giapha.model.Gender;
import com.giapha.model.Person;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Kiểm thử {@link PersonAvatar}: ảnh thật, file thiếu, file hỏng, không có ảnh.
 *
 * <p>Mục tiêu chính: đảm bảo component KHÔNG BAO GIỜ ném exception ra giao diện
 * khi file ảnh không tồn tại, bị hỏng, hoặc sai định dạng.
 *
 * <p>Lưu ý kỹ thuật: ảnh được nạp ở chế độ background. Vì vậy KHÔNG được
 * {@code sleep} trên JavaFX Application Thread (sẽ chặn việc nạp ảnh).
 * Test dựng toàn bộ avatar trong {@link #startup(Platform, AvatarFallbackTest, AtomicInteger)},
 * thoát khỏi FX thread, chờ nạp xong rồi mới kiểm tra.
 */
public class AvatarFallbackTest {

    private static int passed = 0;
    private static int failed = 0;
    private static final CountDownLatch LATCH = new CountDownLatch(1);

    /** PNG 1x1 mã hóa base64 (không dùng java.awt để tương thích module system). */
    private static final String PNG_1PX_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

    // Avatar được dựng trên FX thread, kiểm tra sau khi nạp ảnh nền hoàn tất
    private static PersonAvatar av1, av2, av3, av4, av5, av6, av7, av8, av9;

    public static void main(String[] args) throws Exception {
        Path base = Paths.get(System.getProperty("user.dir"));
        Path imgDir = base.resolve("images");
        Files.createDirectories(imgDir);

        // Ảnh hợp lệ
        Path validPng = imgDir.resolve("_avatar_test_ok.png");
        Files.write(validPng, Base64.getDecoder().decode(PNG_1PX_BASE64));

        // File "hỏng" — nội dung không phải ảnh
        Path brokenFile = imgDir.resolve("_avatar_test_broken.png");
        Files.writeString(brokenFile, "day khong phai mot file anh hop le");

        // File sai định dạng
        Path txtFile = imgDir.resolve("_avatar_test.txt");
        Files.writeString(txtFile, "text");

        AtomicInteger errors = new AtomicInteger(0);

        Platform.startup(() -> startup(validPng, txtFile, errors));

        LATCH.await();

        // Chờ ảnh nền nạp xong (ở thread chính, KHÔNG chặn FX thread).
        // Quan trọng: KHÔNG gọi Platform.exit() trước khi ảnh nạp xong,
        // vì toolkit bị đóng sẽ huỷ tiến trình nạp ảnh nền.
        Thread.sleep(2000);

        runAssertions();

        // Dọn dẹp
        try {
            Files.deleteIfExists(validPng);
            Files.deleteIfExists(brokenFile);
            Files.deleteIfExists(txtFile);
        } catch (Exception ignored) {
            // Dọn dẹp là thao tác phụ trợ
        }

        Platform.exit();

        System.out.println("==================================================");
        System.out.println("  TỔNG KẾT PERSONAVATAR: " + passed + " PASSED, " + failed + " FAILED  ");
        System.out.println("==================================================");
        if (failed > 0 || errors.get() > 0) {
            System.exit(1);
        }
    }

    /**
     * Dựng toàn bộ avatar trên FX thread. KHÔNG sleep ở đây.
     */
    private static void startup(Path validPng, Path txtFile, AtomicInteger errors) {
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
            errors.incrementAndGet();
            System.err.println("[FAIL] Exception không mong đợi khi dựng avatar: " + t);
            t.printStackTrace();
        } finally {
            LATCH.countDown();
            // KHÔNG gọi Platform.exit() ở đây — cần giữ toolkit sống để ảnh nền nạp xong.
        }
    }

    /**
     * Kiểm tra kết quả sau khi ảnh nền đã nạp xong.
     */
    private static void runAssertions() {
        // TC-AV-01: có ảnh thật → hiển thị ảnh thật
        check(av1 != null && av1.isShowingRealImage(),
                "TC-AV-01: ảnh thật → hiển thị ảnh thật");

        // TC-AV-02: file ảnh không tồn tại → fallback placeholder
        check(av2 != null && !av2.isShowingRealImage(),
                "TC-AV-02: file ảnh không tồn tại → fallback placeholder");

        // TC-AV-03: file ảnh hỏng → fallback placeholder, KHÔNG ném exception
        check(av3 != null && !av3.isShowingRealImage(),
                "TC-AV-03: file ảnh hỏng → fallback placeholder");

        // TC-AV-04: không có đường dẫn ảnh → fallback placeholder
        check(av4 != null && !av4.isShowingRealImage(),
                "TC-AV-04: không có đường dẫn ảnh → fallback placeholder");

        // TC-AV-05: đường dẫn ảnh rỗng → fallback placeholder
        check(av5 != null && !av5.isShowingRealImage(),
                "TC-AV-05: đường dẫn ảnh rỗng → fallback placeholder");

        // TC-AV-06: person null → fallback placeholder, không ném exception
        check(av6 != null && !av6.isShowingRealImage(),
                "TC-AV-06: person null → fallback placeholder");

        // TC-AV-07: file không phải ảnh (txt) → fallback placeholder
        check(av7 != null && !av7.isShowingRealImage(),
                "TC-AV-07: file không phải ảnh → fallback placeholder");

        // TC-AV-08: constructor với file cụ thể (xem trước trong form)
        check(av8 != null && av8.isShowingRealImage(),
                "TC-AV-08: constructor với file cụ thể → hiển thị ảnh thật");

        // TC-AV-09: constructor với file không tồn tại → fallback
        check(av9 != null && !av9.isShowingRealImage(),
                "TC-AV-09: constructor với file không tồn tại → fallback");

        // TC-AV-10: kích thước avatar đúng đặc tả
        check(av1 != null && av1.getAvatarSize() == 70,
                "TC-AV-10: kích thước avatar = " + (av1 == null ? "?" : av1.getAvatarSize())
                        + " (mong đợi 70)");

        // TC-AV-11: resolveImageFile xử lý đường dẫn sai / null / hợp lệ
        check(PersonAvatar.resolveImageFile("khong/ton/tai.png") == null,
                "TC-AV-11: resolveImageFile với đường dẫn sai → null");
        check(PersonAvatar.resolveImageFile(null) == null,
                "TC-AV-11b: resolveImageFile với null → null");
        check(PersonAvatar.resolveImageFile("images/_avatar_test_ok.png") != null,
                "TC-AV-11c: resolveImageFile với đường dẫn hợp lệ → khác null");

        // TC-AV-12: ảnh thật được render bằng ImageView
        boolean hasImageView = av1 != null
                && av1.getChildren().stream().anyMatch(n -> n instanceof ImageView);
        check(hasImageView,
                "TC-AV-12: avatar chứa ImageView khi hiển thị ảnh thật");

        // TC-AV-13: placeholder vẫn là Label (chữ cái đầu)
        boolean hasLabel = av4 != null
                && av4.getChildren().stream().anyMatch(n -> n instanceof Label);
        check(hasLabel,
                "TC-AV-13: avatar fallback chứa Label (chữ cái đầu)");

        // TC-AV-14: ảnh thật được bo tròn (có clip hình tròn)
        boolean hasClip = av1 != null
                && av1.getChildren().stream()
                .filter(n -> n instanceof ImageView)
                .anyMatch(n -> ((ImageView) n).getClip() != null);
        check(hasClip,
                "TC-AV-14: ảnh thật được bo tròn bằng clip Circle");
    }

    private static void check(boolean condition, String name) {
        if (condition) {
            passed++;
            System.out.println("[PASS] " + name);
        } else {
            failed++;
            System.err.println("[FAIL] " + name);
        }
    }
}
