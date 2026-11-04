package com.giapha.util;

import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * Tiện ích ghi ảnh {@link WritableImage} ra file PNG.
 *
 * <p>Hiện thực thuần bằng {@code java.util.zip} (Deflater + CRC32) và
 * {@code java.base} — **không** dùng {@code javax.imageio} / {@code javafx.swing}
 * để giữ cho module {@code com.giapha} không phải khai báo thêm module
 * {@code java.desktop} hay {@code javafx.swing}.
 *
 * <p>Định dạng PNG chuẩn: chữ ký 8 byte, khối IHDR (RGBA 8-bit), khối IDAT
 * (dữ liệu zlib deflate với filter byte {@code 0} cho mỗi dòng), khối IEND.
 */
public final class PngWriter {

    /** Chữ ký PNG. */
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
    };

    private PngWriter() {
        // Utility class — không cho phép khởi tạo
    }

    /**
     * Ghi ảnh ra file PNG.
     *
     * @param image ảnh nguồn
     * @param file  đường dẫn file đích (kết thúc bằng {@code .png})
     * @throws IOException nếu ghi file thất bại
     */
    public static void write(WritableImage image, java.io.File file) throws IOException {
        try (OutputStream out = new java.io.FileOutputStream(file)) {
            write(image, out);
        }
    }

    /**
     * Ghi ảnh ra một {@link OutputStream} dưới dạng PNG.
     *
     * @param image ảnh nguồn
     * @param out   luồng đích
     * @throws IOException nếu ghi thất bại
     */
    public static void write(WritableImage image, OutputStream out) throws IOException {
        byte[] png = toByteArray(image);
        out.write(png);
        out.flush();
    }

    /**
     * Chuyển ảnh thành mảng byte định dạng PNG.
     *
     * @param image ảnh nguồn
     * @return mảng byte PNG hoàn chỉnh
     */
    public static byte[] toByteArray(WritableImage image) throws IOException {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        if (width <= 0 || height <= 0) {
            throw new IOException("Ảnh rỗng — không thể ghi ra file PNG.");
        }

        byte[] raw = extractRawPixels(image, width, height);
        return encodePng(width, height, raw);
    }

    /**
     * Đóng gói dữ liệu pixel RGBA thô thành file PNG hoàn chỉnh.
     *
     * <p>Hàm này thuần túy dựa trên {@code java.base} (không cần JavaFX toolkit),
     * nên có thể được kiểm thử độc lập trong môi trường headless.
     *
     * @param width  chiều rộng ảnh (px)
     * @param height chiều cao ảnh (px)
     * @param raw    dữ liệu pixel thô, mỗi dòng gồm 1 byte filter + width*4 byte RGBA
     * @return mảng byte PNG hoàn chỉnh
     * @throws IOException nếu kích thước hoặc dữ liệu pixel không hợp lệ
     */
    public static byte[] encodePng(int width, int height, byte[] raw) throws IOException {
        if (width <= 0 || height <= 0) {
            throw new IOException("Kích thước ảnh không hợp lệ: " + width + "x" + height);
        }
        int expectedLength = height * (1 + width * 4);
        if (raw == null || raw.length != expectedLength) {
            throw new IOException("Dữ liệu pixel không đúng kích thước mong đợi ("
                    + (raw == null ? 0 : raw.length) + " thay vì " + expectedLength + ")");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(PNG_SIGNATURE);

        ByteArrayOutputStream ihdr = new ByteArrayOutputStream();
        writeInt(ihdr, width);
        writeInt(ihdr, height);
        ihdr.write(8);   // bit depth
        ihdr.write(6);   // color type: RGBA
        ihdr.write(0);   // compression method
        ihdr.write(0);   // filter method
        ihdr.write(0);   // interlace method
        writeChunk(out, "IHDR", ihdr.toByteArray());

        writeChunk(out, "IDAT", deflate(raw));
        writeChunk(out, "IEND", new byte[0]);

        return out.toByteArray();
    }

    /**
     * Trích xuất pixel dạng RGBA, mỗi dòng bắt đầu bằng 1 byte filter = 0.
     */
    private static byte[] extractRawPixels(WritableImage image, int width, int height) {
        PixelReader reader = image.getPixelReader();
        byte[] raw = new byte[height * (1 + width * 4)];
        int index = 0;

        for (int y = 0; y < height; y++) {
            raw[index++] = 0; // filter type 0 (None)
            for (int x = 0; x < width; x++) {
                int argb = reader.getArgb(x, y);
                raw[index++] = (byte) ((argb >> 16) & 0xFF); // R
                raw[index++] = (byte) ((argb >> 8) & 0xFF);  // G
                raw[index++] = (byte) (argb & 0xFF);         // B
                raw[index++] = (byte) ((argb >> 24) & 0xFF); // A
            }
        }
        return raw;
    }

    /**
     * Nén dữ liệu pixel bằng zlib (deflate).
     */
    private static byte[] deflate(byte[] raw) throws IOException {
        Deflater deflater = new Deflater();
        deflater.setInput(raw);
        deflater.finish();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[65536];
        while (!deflater.finished()) {
            int written = deflater.deflate(buffer);
            if (written == 0 && deflater.needsInput()) {
                break;
            }
            out.write(buffer, 0, written);
        }
        deflater.end();
        return out.toByteArray();
    }

    /**
     * Ghi một khối PNG (length + type + data + CRC).
     */
    private static void writeChunk(ByteArrayOutputStream out, String type, byte[] data) throws IOException {
        ByteArrayOutputStream length = new ByteArrayOutputStream();
        writeInt(length, data.length);
        out.write(length.toByteArray());

        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        out.write(typeBytes);
        out.write(data);

        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        ByteArrayOutputStream crcBytes = new ByteArrayOutputStream();
        writeInt(crcBytes, (int) crc.getValue());
        out.write(crcBytes.toByteArray());
    }

    /**
     * Ghi số nguyên 4 byte big-endian.
     */
    private static void writeInt(ByteArrayOutputStream out, int value) {
        out.write((value >>> 24) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>> 8) & 0xFF);
        out.write(value & 0xFF);
    }
}
