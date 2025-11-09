CREATE TABLE user_identity (
    identity_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hashed_ci VARCHAR(64) NULL COMMENT '탈퇴 시 null',
    is_foreigner BOOLEAN NOT NULL,
    gender CHAR(1) NOT NULL,
    birthdate DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    UNIQUE KEY uq_hased_ci (hashed_ci)
)
    ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci
    COMMENT='본인인증 정보';
