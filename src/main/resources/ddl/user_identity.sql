CREATE TABLE user_identity (
    service_user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hashed_ci VARCHAR(128) NOT NULL COMMENT '탈퇴 시 값 뒤에 별도 문자열 append',
    mobile_phone_number VARCHAR(16) NOT NULL,
    is_foreigner BOOLEAN NOT NULL,
    gender CHAR(1) NOT NULL,
    birthdate DATE NOT NULL,
    kakao_sub VARCHAR(256) NULL COMMENT '탈퇴 시 null',
    apple_sub VARCHAR(256) NULL COMMENT '탈퇴 시 null',
    naver_sub VARCHAR(256) NULL COMMENT '탈퇴 시 null',
    google_sub VARCHAR(256) NULL COMMENT '탈퇴 시 null',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    UNIQUE KEY uq_hased_ci (hashed_ci),
    UNIQUE KEY uq_kakao_sub (kakao_sub),
    UNIQUE KEY uq_apple_sub (apple_sub),
    UNIQUE KEY uq_naver_sub (naver_sub),
    UNIQUE KEY uq_google_sub (google_sub)
)
    ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci
    COMMENT='사용자의 본인인증 결과 및 소셜 계정 매핑 정보';
