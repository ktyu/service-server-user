CREATE TABLE user_identity (
    service_user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hashed_ci VARCHAR(64) NULL COMMENT '탈퇴 시 null',
    is_foreigner BOOLEAN NOT NULL,
    gender CHAR(1) NOT NULL,
    birthdate DATE NOT NULL,
    kakao_uuid CHAR(36) NULL COMMENT '탈퇴 시 null',
    apple_uuid CHAR(36) NULL COMMENT '탈퇴 시 null',
    naver_uuid CHAR(36) NULL COMMENT '탈퇴 시 null',
    google_uuid CHAR(36) NULL COMMENT '탈퇴 시 null',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    UNIQUE KEY uq_hased_ci (hashed_ci),
    UNIQUE KEY uq_kakao_uuid (kakao_uuid),
    UNIQUE KEY uq_apple_uuid (apple_uuid),
    UNIQUE KEY uq_naver_uuid (naver_uuid),
    UNIQUE KEY uq_google_uuid (google_uuid)
)
    ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci
    COMMENT='사용자의 본인인증 결과 및 소셜 계정 매핑 정보';
