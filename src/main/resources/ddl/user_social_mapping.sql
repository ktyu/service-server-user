CREATE TABLE user_social_mapping (
    service_user_id BIGINT NOT NULL,
    social_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (service_user_id, social_id),
    UNIQUE KEY uq_social_id (social_id)
)
    ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci
    COMMENT='사용자와 소셜 로그인 계정 연결 정보';
