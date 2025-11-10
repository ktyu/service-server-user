CREATE TABLE user_identity_mapping (
    service_user_id BIGINT NOT NULL,
    identity_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME NULL,
    PRIMARY KEY (service_user_id, identity_id),
    UNIQUE KEY uq_identity_id (identity_id)
)
    ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci
    COMMENT='사용자와 본인인증 연결 정보';
