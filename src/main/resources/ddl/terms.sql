CREATE TABLE terms (
    terms_key VARCHAR(32) NOT NULL PRIMARY KEY,
    version INT NOT NULL,
    display_order INT NOT NULL UNIQUE COMMENT '노출 순서 (음수면 노출하지 않음)',
    is_mandatory BOOLEAN NOT NULL,
    title VARCHAR(64) NOT NULL,
    content_link VARCHAR(256) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
)
    ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci
    COMMENT='서비스 이용 약관';
