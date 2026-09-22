-- ============================================================
-- SƠ ĐỒ GIA PHẢ — DATABASE SCHEMA
-- Database: gia_pha
-- Bảng: PERSON
-- Lưu ý: Lệnh DROP DATABASE chỉ dành cho môi trường development/test
-- ============================================================

DROP DATABASE IF EXISTS gia_pha;
CREATE DATABASE gia_pha CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE gia_pha;

CREATE TABLE PERSON (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    full_name   VARCHAR(100) NOT NULL,
    birth_year  INT NOT NULL,
    gender      ENUM('MALE','FEMALE') NOT NULL,
    photo_path  VARCHAR(255) NULL,
    father_id   INT NULL,
    mother_id   INT NULL,

    CONSTRAINT fk_father
        FOREIGN KEY (father_id)
        REFERENCES PERSON(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_mother
        FOREIGN KEY (mother_id)
        REFERENCES PERSON(id)
        ON DELETE RESTRICT,

    -- MySQL/MariaDB yêu cầu biểu thức CHECK phải mang tính xác định (deterministic),
    -- do đó không được dùng hàm CURDATE()/NOW() trong CHECK.
    CONSTRAINT chk_birth_year
        CHECK (birth_year > 1900 AND birth_year <= 2100),

    CONSTRAINT chk_father_not_mother
        CHECK (
            father_id IS NULL
            OR mother_id IS NULL
            OR father_id <> mother_id
        )
);

CREATE INDEX idx_person_name ON PERSON(full_name);

-- MySQL/MariaDB không cho phép tham chiếu cột AUTO_INCREMENT (id) trong mệnh đề CHECK,
-- do đó ràng buộc không tự làm cha/mẹ chính mình (self-parent) được bảo vệ ở mức Database bằng Trigger:
DELIMITER //
CREATE TRIGGER trg_check_not_self_parent_update
BEFORE UPDATE ON PERSON
FOR EACH ROW
BEGIN
    IF (NEW.father_id IS NOT NULL AND NEW.father_id = NEW.id) OR
       (NEW.mother_id IS NOT NULL AND NEW.mother_id = NEW.id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Một người không thể là cha hoặc mẹ của chính mình';
    END IF;
END//
DELIMITER ;

-- ============================================================
-- SAMPLE DATA (7 BẢN GHI NGUYÊN BẢN)
-- ============================================================

INSERT INTO PERSON
(full_name, birth_year, gender, father_id, mother_id)
VALUES
('Nguyễn Văn Tổ', 1930, 'MALE', NULL, NULL),
('Trần Thị Tổ', 1932, 'FEMALE', NULL, NULL),
('Nguyễn Văn Cha', 1955, 'MALE', 1, 2),
('Lê Thị Mẹ', 1958, 'FEMALE', NULL, NULL),
('Nguyễn Văn Con', 1985, 'MALE', 3, 4),
('Phạm Thị Dâu', 1987, 'FEMALE', NULL, NULL),
('Nguyễn Văn Cháu', 2015, 'MALE', 5, 6);
