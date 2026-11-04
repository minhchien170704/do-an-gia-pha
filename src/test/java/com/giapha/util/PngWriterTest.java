package com.giapha.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử {@link PngWriter} — bộ ghi PNG thuần Java (không dùng {@code javax.imageio}).
 *
 * <p>Bộ test này chạy ở chế độ headless, KHÔNG cần JavaFX toolkit: sử dụng
 * {@link PngWriter#encodePng(int, int, byte[])} với dữ liệu pixel thô.
 */
@DisplayName("PngWriter — ghi ảnh ra định dạng PNG chuẩn")
class PngWriterTest {

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
    };

    // ============================================================
    // Cấu trúc file PNG
    // ============================================================

    @Test
    @DisplayName("encodePng xuất đúng chữ ký PNG 8 byte ở đầu file")
    void pngSignatureIsCorrect() throws IOException {
        byte[] png = PngWriter.encodePng(1, 1, buildRawPixels(1, 1));
        assertTrue(png.length > 8, "File PNG phải có dữ liệu");
        byte[] signature = new byte[8];
        System.arraycopy(png, 0, signature, 0, 8);
        assertArrayEquals(PNG_SIGNATURE, signature, "Chữ ký PNG không đúng");
    }

    @Test
    @DisplayName("encodePng chứa đủ 3 khối IHDR, IDAT, IEND theo đúng thứ tự")
    void containsRequiredChunks() throws IOException {
        byte[] png = PngWriter.encodePng(2, 2, buildRawPixels(2, 2));

        assertTrue(indexOfChunk(png, "IHDR") >= 0, "Phải có khối IHDR");
        assertTrue(indexOfChunk(png, "IDAT") >= 0, "Phải có khối IDAT");
        assertTrue(indexOfChunk(png, "IEND") >= 0, "Phải có khối IEND");

        int ihdr = indexOfChunk(png, "IHDR");
        int idat = indexOfChunk(png, "IDAT");
        int iend = indexOfChunk(png, "IEND");
        assertTrue(ihdr < idat, "IHDR phải đứng trước IDAT");
        assertTrue(idat < iend, "IDAT phải đứng trước IEND");
    }

    @Test
    @DisplayName("Khối IHDR chứa kích thước, bit depth 8 và color type 6 (RGBA)")
    void ihdrContainsCorrectMetadata() throws IOException {
        int width = 4;
        int height = 3;
        byte[] png = PngWriter.encodePng(width, height, buildRawPixels(width, height));

        int ihdr = indexOfChunk(png, "IHDR");
        assertTrue(ihdr >= 0, "Phải có khối IHDR");

        // Chunk layout: length(4) + type(4) + data + crc(4)
        int dataStart = ihdr + 8;
        byte[] ihdrData = new byte[13];
        System.arraycopy(png, dataStart, ihdrData, 0, 13);

        assertEquals(width, readInt(ihdrData, 0), "Chiều rộng trong IHDR");
        assertEquals(height, readInt(ihdrData, 4), "Chiều cao trong IHDR");
        assertEquals(8, ihdrData[8], "Bit depth phải là 8");
        assertEquals(6, ihdrData[9], "Color type phải là 6 (RGBA)");
        assertEquals(0, ihdrData[10], "Compression method phải là 0");
        assertEquals(0, ihdrData[11], "Filter method phải là 0");
        assertEquals(0, ihdrData[12], "Interlace method phải là 0");
    }

    @Test
    @DisplayName("Khối IEND nằm ở cuối file và có độ dài dữ liệu bằng 0")
    void iendIsLastAndEmpty() throws IOException {
        byte[] png = PngWriter.encodePng(1, 1, buildRawPixels(1, 1));

        int iend = indexOfChunk(png, "IEND");
        assertTrue(iend > 0, "Phải có khối IEND");
        assertEquals(0, readInt(png, iend), "Độ dài dữ liệu IEND phải bằng 0");
        assertEquals(png.length, iend + 12,
                "IEND phải là khối cuối cùng (4 length + 4 type + 0 data + 4 crc)");
    }

    // ============================================================
    // CRC
    // ============================================================

    @Test
    @DisplayName("CRC32 của mỗi khối được tính đúng theo đặc tả PNG")
    void chunkCrcIsValid() throws IOException {
        byte[] png = PngWriter.encodePng(3, 3, buildRawPixels(3, 3));

        for (String type : new String[]{"IHDR", "IDAT", "IEND"}) {
            int offset = indexOfChunk(png, type);
            assertTrue(offset >= 0, "Thiếu khối " + type);

            int dataLength = readInt(png, offset);
            int crcOffset = offset + 8 + dataLength;
            int expectedCrc = readInt(png, crcOffset);

            java.util.zip.CRC32 crc = new java.util.zip.CRC32();
            crc.update(png, offset + 4, 4 + dataLength);

            assertEquals(expectedCrc, (int) crc.getValue(),
                    "CRC của khối " + type + " không đúng");
        }
    }

    // ============================================================
    // Dữ liệu pixel
    // ============================================================

    @Test
    @DisplayName("Dữ liệu IDAT giải nén được về đúng dữ liệu pixel thô")
    void idatDecompressesToOriginalPixels() throws IOException {
        int width = 5;
        int height = 4;
        byte[] raw = buildRawPixels(width, height);

        byte[] png = PngWriter.encodePng(width, height, raw);
        byte[] decompressed = inflateIdat(png);

        assertArrayEquals(raw, decompressed, "Dữ liệu pixel sau giải nén phải khớp với dữ liệu gốc");
    }

    @Test
    @DisplayName("Mỗi dòng dữ liệu pixel bắt đầu bằng filter byte 0 (None)")
    void eachRowStartsWithFilterByteZero() throws IOException {
        int width = 3;
        int height = 2;
        byte[] raw = buildRawPixels(width, height);

        byte[] decompressed = inflateIdat(PngWriter.encodePng(width, height, raw));
        int rowLength = 1 + width * 4;

        for (int row = 0; row < height; row++) {
            assertEquals(0, decompressed[row * rowLength],
                    "Filter byte của dòng " + row + " phải bằng 0");
        }
    }

    @Test
    @DisplayName("Ảnh 1x1 tạo ra file PNG hợp lệ (byte đầu là chữ ký PNG)")
    void singlePixelImageIsValid() throws IOException {
        byte[] png = PngWriter.encodePng(1, 1, buildRawPixels(1, 1));
        assertTrue(png.length > 8);
        assertEquals((byte) 0x89, png[0]);
        assertEquals('P', png[1]);
    }

    // ============================================================
    // Trường hợp lỗi
    // ============================================================

    @Test
    @DisplayName("encodePng ném IOException khi kích thước ảnh không hợp lệ")
    void throwsOnInvalidDimensions() {
        byte[] raw = buildRawPixels(1, 1);
        assertThrows(IOException.class, () -> PngWriter.encodePng(0, 1, raw));
        assertThrows(IOException.class, () -> PngWriter.encodePng(1, -1, raw));
    }

    @Test
    @DisplayName("encodePng ném IOException khi dữ liệu pixel sai kích thước")
    void throwsOnMismatchedPixelData() {
        assertThrows(IOException.class, () -> PngWriter.encodePng(4, 4, new byte[] {0, 1, 2}));
        assertThrows(IOException.class, () -> PngWriter.encodePng(4, 4, null));
    }

    // ============================================================
    // HELPER
    // ============================================================

    /**
     * Tạo dữ liệu pixel thô: mỗi dòng gồm 1 byte filter = 0, sau đó là các pixel RGBA.
     */
    private static byte[] buildRawPixels(int width, int height) {
        byte[] raw = new byte[height * (1 + width * 4)];
        int index = 0;
        for (int y = 0; y < height; y++) {
            raw[index++] = 0;
            for (int x = 0; x < width; x++) {
                raw[index++] = (byte) (x * 20);
                raw[index++] = (byte) (y * 20);
                raw[index++] = (byte) 0x80;
                raw[index++] = (byte) 0xFF;
            }
        }
        return raw;
    }

    /**
     * Tìm offset của khối PNG theo loại (trả về offset của trường length).
     */
    private static int indexOfChunk(byte[] png, String type) {
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i <= png.length - typeBytes.length; i++) {
            boolean match = true;
            for (int j = 0; j < typeBytes.length; j++) {
                if (png[i + j] != typeBytes[j]) {
                    match = false;
                    break;
                }
            }
            if (match && i >= 4) {
                return i - 4;
            }
        }
        return -1;
    }

    /**
     * Đọc số nguyên 4 byte big-endian từ mảng byte.
     */
    private static int readInt(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }

    /**
     * Trích xuất và giải nén dữ liệu khối IDAT.
     */
    private static byte[] inflateIdat(byte[] png) throws IOException {
        int idat = indexOfChunk(png, "IDAT");
        assertTrue(idat >= 0, "Thiếu khối IDAT");
        int dataLength = readInt(png, idat);

        byte[] compressed = new byte[dataLength];
        System.arraycopy(png, idat + 8, compressed, 0, dataLength);

        Inflater inflater = new Inflater();
        try (InputStream in = new ByteArrayInputStream(compressed)) {
            inflater.setInput(in.readAllBytes());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            while (!inflater.finished()) {
                int n = inflater.inflate(buffer);
                if (n == 0) {
                    break;
                }
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } catch (java.util.zip.DataFormatException e) {
            throw new IOException("Không giải nén được dữ liệu IDAT: " + e.getMessage(), e);
        } finally {
            inflater.end();
        }
    }

    @Test
    @DisplayName("ZipEntry/Inflater là class chuẩn của java.base — không cần dependency ngoài")
    void usesOnlyJavaBase() {
        assertNotNull(ZipEntry.class);
        assertNotNull(Inflater.class);
    }
}
