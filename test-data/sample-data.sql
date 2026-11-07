-- ============================================================
-- BỘ DỮ LIỆU MẪU MỞ RỘNG — 30 NGƯỜI / 5 THẾ HỆ
-- Dự án: Quản lý Sơ đồ Gia phả (do-an-gia-pha)
-- Database: gia_pha — Bảng: person
--
-- CẢNH BÁO:
--   Script này XÓA toàn bộ dữ liệu đang có trong bảng person
--   (kể cả 7 bản ghi mẫu nguyên bản của database/schema.sql)
--   rồi nạp lại 30 bản ghi mới.
--   Khôi phục dữ liệu gốc: chạy lại  mysql < database/schema.sql
--
-- Cách nạp:
--   mysql -u root -p < test-data/sample-data.sql
--   hoặc: mysql -u root -p gia_pha < test-data/sample-data.sql
--
-- CẤU TRÚC GIA PHẢ (5 thế hệ, họ Nguyễn):
--
--   Thế hệ 1 (2 người)  : Nguyễn Văn Tổ (1850) — Trần Thị Tổ (1855)
--   Thế hệ 2 (4 người)  : Nguyễn Văn Cụ, Nguyễn Văn Nhị (+ 2 người vợ)
--   Thế hệ 3 (8 người)  : Đại, Tị, Tuấn, Thắng (+ 4 người vợ)
--   Thế hệ 4 (8 người)  : Hùng, Khoa, Sơn, Trí (+ 4 người vợ)
--   Thế hệ 5 (8 người)  : An, Bình, Cường, Dũng, Giang, Hà, Huy, Khang
--
--   Người không có cha/mẹ trong bộ dữ liệu = người kết hôn ngoài dòng họ.
--   Năm sinh luôn thỏa mãn: cha/mẹ sinh trước con (BR5 không cảnh báo).
-- ============================================================

USE gia_pha;

-- Tắt kiểm tra FK tạm thời để xóa sạch dữ liệu cũ (FK person tự tham chiếu)
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM person;
SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE person AUTO_INCREMENT = 1;

-- ------------------------------------------------------------
-- THẾ HỆ 1 — 2 người
-- ------------------------------------------------------------
INSERT INTO person (id, full_name, birth_year, gender, photo_path, father_id, mother_id) VALUES
(1,  'Nguyễn Văn Tổ',      1850, 'MALE',   NULL, NULL, NULL),
(2,  'Trần Thị Tổ',        1855, 'FEMALE', NULL, NULL, NULL);

-- ------------------------------------------------------------
-- THẾ HỆ 2 — 4 người (2 con của thế hệ 1 + 2 người vợ)
-- ------------------------------------------------------------
INSERT INTO person (id, full_name, birth_year, gender, photo_path, father_id, mother_id) VALUES
(3,  'Nguyễn Văn Cụ',      1880, 'MALE',   NULL, 1,    2),
(4,  'Phạm Thị Mẹ Cụ',    1884, 'FEMALE', NULL, NULL, NULL),
(5,  'Nguyễn Văn Nhị',     1883, 'MALE',   NULL, 1,    2),
(6,  'Lý Thị Mẹ Nhị',      1887, 'FEMALE', NULL, NULL, NULL);

-- ------------------------------------------------------------
-- THẾ HỆ 3 — 8 người (4 con của thế hệ 2 + 4 người vợ)
-- ------------------------------------------------------------
INSERT INTO person (id, full_name, birth_year, gender, photo_path, father_id, mother_id) VALUES
(7,  'Nguyễn Văn Đại',     1905, 'MALE',   NULL, 3,    4),
(8,  'Đỗ Thị Mẹ Đại',     1909, 'FEMALE', NULL, NULL, NULL),
(9,  'Nguyễn Văn Tị',      1908, 'MALE',   NULL, 3,    4),
(10, 'Hoàng Thị Mẹ Tị',    1912, 'FEMALE', NULL, NULL, NULL),
(11, 'Nguyễn Văn Tuấn',    1910, 'MALE',   NULL, 5,    6),
(12, 'Bùi Thị Mẹ Tuấn',   1914, 'FEMALE', NULL, NULL, NULL),
(13, 'Nguyễn Văn Thắng',   1912, 'MALE',   NULL, 5,    6),
(14, 'Mai Thị Mẹ Thắng',  1916, 'FEMALE', NULL, NULL, NULL);

-- ------------------------------------------------------------
-- THẾ HỆ 4 — 8 người (4 con của thế hệ 3 + 4 người vợ)
-- ------------------------------------------------------------
INSERT INTO person (id, full_name, birth_year, gender, photo_path, father_id, mother_id) VALUES
(15, 'Nguyễn Văn Hùng',    1935, 'MALE',   NULL, 7,    8),
(16, 'Vũ Thị Mẹ Hùng',    1939, 'FEMALE', NULL, NULL, NULL),
(17, 'Nguyễn Văn Khoa',    1938, 'MALE',   NULL, 7,    8),
(18, 'Đinh Thị Mẹ Khoa',  1941, 'FEMALE', NULL, NULL, NULL),
(19, 'Nguyễn Văn Sơn',     1936, 'MALE',   NULL, 11,   12),
(20, 'Trịnh Thị Mẹ Sơn',  1940, 'FEMALE', NULL, NULL, NULL),
(21, 'Nguyễn Văn Trí',     1940, 'MALE',   NULL, 11,   12),
(22, 'Chu Thị Mẹ Trí',     1943, 'FEMALE', NULL, NULL, NULL);

-- ------------------------------------------------------------
-- THẾ HỆ 5 — 8 người (8 cháu nội của thế hệ 4)
-- ------------------------------------------------------------
INSERT INTO person (id, full_name, birth_year, gender, photo_path, father_id, mother_id) VALUES
(23, 'Nguyễn Văn An',      1960, 'MALE',   NULL, 15,   16),
(24, 'Nguyễn Văn Bình',    1962, 'MALE',   NULL, 15,   16),
(25, 'Nguyễn Văn Cường',  1965, 'MALE',   NULL, 17,   18),
(26, 'Nguyễn Văn Dũng',    1968, 'MALE',   NULL, 17,   18),
(27, 'Nguyễn Văn Giang',   1963, 'MALE',   NULL, 19,   20),
(28, 'Nguyễn Văn Hà',      1966, 'FEMALE', NULL, 19,   20),
(29, 'Nguyễn Văn Huy',     1967, 'MALE',   NULL, 21,   22),
(30, 'Nguyễn Văn Khang',   1970, 'MALE',   NULL, 21,   22);

-- Đồng bộ lại giá trị AUTO_INCREMENT về 31 để các bản ghi thêm mới không trùng id
ALTER TABLE person AUTO_INCREMENT = 31;
