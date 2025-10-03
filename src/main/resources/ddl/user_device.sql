CREATE TABLE user_device (
    service_user_id BIGINT NOT NULL,
    custom_device_id CHAR(36) NOT NULL,
    device_model VARCHAR(32) NOT NULL,
    os_type VARCHAR(8) NOT NULL,
    os_version VARCHAR(32) NOT NULL,
    app_version VARCHAR(8) NOT NULL,
    access_token_issued_at BIGINT NOT NULL,
    refresh_token_issued_at BIGINT NOT NULL,
    last_login_at DATETIME NOT NULL COMMENT '단말에서 마지막 로그인한 시각',
    push_token_type VARCHAR(32) NOT NULL,
    push_token VARCHAR(1024) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '단말에서 최초 로그인한 시각',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '최종 사용 시각(로그인/토큰 갱신)',
    deleted_at DATETIME NULL,
    PRIMARY KEY (service_user_id, custom_device_id)
)
    ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci
    COMMENT='사용자의 디바이스 정보';
