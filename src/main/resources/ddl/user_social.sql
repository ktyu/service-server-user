CREATE TABLE user_social (
    social_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    social_uuid CHAR(36) NOT NULL,
    social_type VARCHAR(8) NOT NULL,
    sub VARCHAR(256) NOT NULL,
    social_access_token VARCHAR(1024) NULL,
    social_id_token VARCHAR(4096) NULL,
    social_refresh_token VARCHAR(1024) NULL,
    email VARCHAR(256) NULL,
    is_email_verified BOOLEAN NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '연결 상태 최종 확인 시간',
    UNIQUE KEY uq_social_uuid (social_uuid),
    UNIQUE KEY uq_social_type_and_sub (social_type, sub)
)
    ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci
    COMMENT='소셜 로그인 계정 정보';
