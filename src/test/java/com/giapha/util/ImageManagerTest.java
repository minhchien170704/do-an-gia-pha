package com.giapha.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử tầng tiện ích {@link ImageManager} — không phụ thuộc JavaFX hay CSDL.
 */
@DisplayName("ImageManager — kiểm tra định dạng, copy và dọn ảnh đại diện")
class ImageManagerTest {

    // ============================================================
    // isAllowedImage
    // ============================================================

    @Test
    @DisplayName("isAllowedImage chấp nhận png / jpg / jpeg (không phân biệt hoa thường)")
    void acceptsAllowedExtensions() {
        assertTrue(ImageManager.isAllowedImage("a.png"));
        assertTrue(ImageManager.isAllowedImage("a.PNG"));
        assertTrue(ImageManager.isAllowedImage("a.jpg"));
        assertTrue(ImageManager.isAllowedImage("a.JpG"));
        assertTrue(ImageManager.isAllowedImage("a.jpeg"));
        assertTrue(ImageManager.isAllowedImage("C:\\Users\\test\\anh\\avatar.png"));
    }

    @Test
    @DisplayName("isAllowedImage từ chối định dạng khác và đường dẫn rỗng/null")
    void rejectsDisallowedExtensions() {
        assertFalse(ImageManager.isAllowedImage("a.gif"));
        assertFalse(ImageManager.isAllowedImage("a.bmp"));
        assertFalse(ImageManager.isAllowedImage("a.txt"));
        assertFalse(ImageManager.isAllowedImage("a.png.exe"));
        assertFalse(ImageManager.isAllowedImage("noextension"));
        assertFalse(ImageManager.isAllowedImage(""));
        assertFalse(ImageManager.isAllowedImage("   "));
        assertFalse(ImageManager.isAllowedImage(null));
    }

    // ============================================================
    // getExtension
    // ============================================================

    @Test
    @DisplayName("getExtension trả về đuôi file chữ thường")
    void getExtensionReturnsLowercase() {
        assertEquals("png", ImageManager.getExtension("a.png"));
        assertEquals("jpg", ImageManager.getExtension("a.JPG"));
        assertEquals("jpeg", ImageManager.getExtension("dir/sub/a.JPEG"));
    }

    @Test
    @DisplayName("getExtension trả về null khi không xác định được đuôi file")
    void getExtensionReturnsNullWhenUndetermined() {
        assertNull(ImageManager.getExtension(null));
        assertNull(ImageManager.getExtension("noextension"));
        assertNull(ImageManager.getExtension("trailingdot."));
        assertNull(ImageManager.getExtension("dir.png/file"));
    }

    // ============================================================
    // getImageDirectory
    // ============================================================

    @Test
    @DisplayName("getImageDirectory tạo thư mục images nếu chưa tồn tại")
    void getImageDirectoryCreatesFolder(@TempDir Path tempDir) throws IOException {
        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toAbsolutePath().toString());

            Path dir = ImageManager.getImageDirectory();
            assertNotNull(dir);
            assertTrue(Files.isDirectory(dir), "Thư mục images phải được tạo");
            assertEquals("images", dir.getFileName().toString());
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    // ============================================================
    // copyImageToAppDirectory
    // ============================================================

    @Test
    @DisplayName("copyImageToAppDirectory copy ảnh và trả về đường dẫn tương đối đúng định dạng")
    void copyImageReturnsRelativePath(@TempDir Path tempDir) throws IOException {
        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toAbsolutePath().toString());

            Path source = tempDir.resolve("source.png");
            Files.write(source, "fake-png-content".getBytes(StandardCharsets.UTF_8));

            String relative = ImageManager.copyImageToAppDirectory(source.toString(), 42);

            assertNotNull(relative);
            assertEquals("images/person_42.png", relative);
            assertTrue(Files.isRegularFile(tempDir.resolve("images").resolve("person_42.png")),
                    "File ảnh phải được copy vào thư mục images");
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    @DisplayName("copyImageToAppDirectory ghi đè ảnh cũ khi cập nhật lại cùng một người")
    void copyImageOverwritesExistingFile(@TempDir Path tempDir) throws IOException {
        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toAbsolutePath().toString());

            Path source = tempDir.resolve("source.jpg");
            Files.write(source, "new-content".getBytes(StandardCharsets.UTF_8));

            String relative = ImageManager.copyImageToAppDirectory(source.toString(), 7);
            assertEquals("images/person_7.jpg", relative);

            Path target = tempDir.resolve("images").resolve("person_7.jpg");
            assertTrue(Files.isRegularFile(target));
            assertEquals("new-content", Files.readString(target, StandardCharsets.UTF_8));
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    @DisplayName("copyImageToAppDirectory ném IllegalArgumentException với định dạng không hợp lệ")
    void copyImageRejectsDisallowedExtension(@TempDir Path tempDir) throws IOException {
        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toAbsolutePath().toString());

            Path source = tempDir.resolve("source.gif");
            Files.write(source, "gif".getBytes(StandardCharsets.UTF_8));

            assertThrows(IllegalArgumentException.class,
                    () -> ImageManager.copyImageToAppDirectory(source.toString(), 1));
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    @DisplayName("copyImageToAppDirectory ném IOException khi file nguồn không tồn tại")
    void copyImageThrowsWhenSourceMissing(@TempDir Path tempDir) throws IOException {
        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toAbsolutePath().toString());

            Path missing = tempDir.resolve("khong-ton-tai.png");
            assertThrows(IOException.class,
                    () -> ImageManager.copyImageToAppDirectory(missing.toString(), 1));
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    // ============================================================
    // deleteImageQuietly
    // ============================================================

    @Test
    @DisplayName("deleteImageQuietly xóa file ảnh trong thư mục images")
    void deleteImageRemovesFile(@TempDir Path tempDir) throws IOException {
        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toAbsolutePath().toString());

            Path source = tempDir.resolve("source.png");
            Files.write(source, "png".getBytes(StandardCharsets.UTF_8));
            String relative = ImageManager.copyImageToAppDirectory(source.toString(), 3);

            Path target = tempDir.resolve("images").resolve("person_3.png");
            assertTrue(Files.exists(target));

            ImageManager.deleteImageQuietly(relative);
            assertFalse(Files.exists(target), "File ảnh phải bị xóa");
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    @DisplayName("deleteImageQuietly KHÔNG xóa file ngoài thư mục images")
    void deleteImageRefusesOutsideImagesFolder(@TempDir Path tempDir) throws IOException {
        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toAbsolutePath().toString());

            Path secret = tempDir.resolve("khong-duoc-xoa.txt");
            Files.writeString(secret, "quan trong");

            ImageManager.deleteImageQuietly("khong-duoc-xoa.txt");

            assertTrue(Files.exists(secret), "File ngoài thư mục images không được phép xóa");
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    @DisplayName("deleteImageQuietly bỏ qua đường dẫn null / rỗng và không ném exception")
    void deleteImageIgnoresBlankPaths() {
        assertDoesNotThrowAny(() -> {
            ImageManager.deleteImageQuietly(null);
            ImageManager.deleteImageQuietly("");
            ImageManager.deleteImageQuietly("   ");
            ImageManager.deleteImageQuietly("khong/ton/tai/anh.png");
        });
    }

    private static void assertDoesNotThrowAny(Runnable action) {
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(action::run);
    }
}
