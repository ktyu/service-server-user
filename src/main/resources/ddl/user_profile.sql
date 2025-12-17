CREATE TABLE user_profile (
    service_user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nickname VARCHAR(64) NOT NULL UNIQUE,
    nickname_updated_at DATETIME NOT NULL,
    terms_agreements JSON NOT NULL,
    terms_agreements_updated_at DATETIME NOT NULL,
    image_url VARCHAR(256) NULL,
    region VARCHAR(16) NULL,
    interest_fields JSON NULL,
    interest_level VARCHAR(8) NULL,
    issue_note VARCHAR(256) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME NULL,
    CONSTRAINT chk_interest_fields_is_array
        CHECK (JSON_TYPE(interest_fields) = 'ARRAY'),
    INDEX idx_interest_fields ((CAST(interest_fields AS CHAR(32) ARRAY))),
    INDEX idx_terms_agreements ((CAST(JSON_KEYS(terms_agreements) AS CHAR(32) ARRAY)))
)
    ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci
    COMMENT='사용자의 변경 가능한 프로필 정보';
