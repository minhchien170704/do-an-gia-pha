package com.giapha.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;

/**
 * Tiện ích quản lý ảnh đại diện của thành viên gia phả.
 *
 * <p>Chịu trách nhiệm:
 * <ul>
 *     <li>Kiểm tra định dạng file được phép (chỉ png / jpg / jpeg).</li>
 *     <li>Copy ảnh vào thư mục {@code images/} nằm cạnh thư mục làm việc của ứng dụng.</li>
 *     <li>Sinh đường dẫn tương đối để lưu vào cơ sở dữ liệu (không phụ thuộc vị trí cài đặt).</li>
 * </ul>
 *
 * <p>Ảnh được đặt tên lại theo mã định danh của thành viên để tránh trùng lặp
 * khi cùng một người được cập nhật ảnh nhiều lần.
 */
public final class ImageManager {

    /** Tên thư mục chứa ảnh đại diện, nằm cạnh thư mục làm việc của ứng dụng. */
    public static final String IMAGE_DIR_NAME = "images";

    /** Các định dạng ảnh được phép sử dụng. */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg");

    /** Bộ lọc cho FileChooser. */
    public static final String IMAGE_FILTER =
            "Ảnh đại diện (*.png, *.jpg, *.jpeg)";

    /**
     * Private constructor để ngăn chặn khởi tạo đối tượng (utility class).
     */
    private ImageManager() {
    }

    /**
     * Kiểm tra một đường dẫn file có phải định dạng ảnh được phép hay không.
     * So sánh không phân biệt hoa thường (PNG / Png / png đều hợp lệ).
     *
     * @param filePath đường dẫn file cần kiểm tra
     * @return true nếu có đuôi file nằm trong danh sách cho phép
     */
    public static boolean isAllowedImage(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return false;
        }
        String extension = getExtension(filePath);
        return extension != null && ALLOWED_EXTENSIONS.contains(extension);
    }

    /**
     * Trả về định dạng (đuôi file) của một đường dẫn, không phân biệt hoa thường.
     *
     * @param filePath đường dẫn file
     * @return đuôi file không có dấu chấm, hoặc null nếu không xác định được
     */
    public static String getExtension(String filePath) {
        if (filePath == null) {
            return null;
        }
        int lastDot = filePath.lastIndexOf('.');
        int lastSeparator = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        if (lastDot <= lastSeparator || lastDot == filePath.length() - 1) {
            return null;
        }
        return filePath.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Lấy thư mục chứa ảnh đại diện, tự tạo nếu chưa tồn tại.
     * Thư mục nằm cạnh thư mục làm việc hiện tại của ứng dụng.
     *
     * @return đường dẫn tuyệt đối tới thư mục images
     * @throws IOException nếu không tạo được thư mục
     */
    public static Path getImageDirectory() throws IOException {
        Path dir = Paths.get(System.getProperty("user.dir")).resolve(IMAGE_DIR_NAME);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir;
    }

    /**
     * Copy ảnh đã chọn vào thư mục images và trả về đường dẫn TƯƠNG ĐỐI để lưu vào DB.
     *
     * <p>Tên file được đặt theo mã định danh thành viên: {@code person_<id>.<đuôi>}.
     * Nếu copy thất bại, ảnh cũ (nếu có) vẫn được giữ nguyên trong DB.
     *
     * @param sourceFilePath đường dẫn tuyệt đối tới ảnh nguồn do FileChooser trả về
     * @param personId       mã định danh của thành viên, dùng để đặt tên file
     * @return đường dẫn tương đối (ví dụ {@code images/person_7.png}) để lưu vào DB
     * @throws IllegalArgumentException nếu định dạng ảnh không được phép
     * @throws IOException              nếu copy file thất bại
     */
    public static String copyImageToAppDirectory(String sourceFilePath, int personId) throws IOException {
        if (!isAllowedImage(sourceFilePath)) {
            throw new IllegalArgumentException(
                    "Định dạng ảnh không được hỗ trợ. Chỉ chấp nhận các định dạng: png, jpg, jpeg.");
        }

        Path source = Paths.get(sourceFilePath);
        if (!Files.isRegularFile(source)) {
            throw new IOException("Không tìm thấy file ảnh: " + sourceFilePath);
        }

        String extension = getExtension(sourceFilePath);
        String fileName = "person_" + personId + "." + extension;

        Path imageDir = getImageDirectory();
        Path target = imageDir.resolve(fileName);

        // Copy thay thế (REPLACE_EXISTING) để ảnh mới ghi đè ảnh cũ cùng thành viên
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

        return IMAGE_DIR_NAME + "/" + fileName;
    }

    /**
     * Xóa file ảnh đại diện đang lưu trong thư mục images.
     * Dùng khi xóa thành viên hoặc khi thay ảnh (dọn ảnh cũ không còn dùng).
     *
     * <p>Phương thức này KHÔNG ném ngoại lệ: việc dọn file là thao tác phụ trợ,
     * lỗi xóa file không được làm hỏng luồng nghiệp vụ chính.
     *
     * @param relativePath đường dẫn tương đối đã lưu trong DB
     */
    public static void deleteImageQuietly(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return;
        }
        try {
            Path base = Paths.get(System.getProperty("user.dir"));
            Path target = base.resolve(relativePath).normalize();

            // Chỉ xóa file nằm trong thư mục images/ để tránh xóa nhầm file ngoài
            Path imageDir = base.resolve(IMAGE_DIR_NAME).normalize();
            if (target.startsWith(imageDir) && Files.isRegularFile(target)) {
                Files.deleteIfExists(target);
            }
        } catch (Exception ignored) {
            // Dọn dẹp file là thao tác phụ trợ — bỏ qua lỗi để không ảnh hưởng luồng chính
        }
    }
}
