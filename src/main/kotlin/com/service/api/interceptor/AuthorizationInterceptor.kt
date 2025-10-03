package com.service.api.interceptor

class AuthorizationInterceptor {

    /** TODO: [토큰 검증 로직]
     * - 토큰의 exp가 지났으면 expired 응답 (토큰 재발급 필요)
     * - 토큰의 service_user_id (다중디바이스 허용이면 +device_id) 로 user_device 조회
     * 	- user_device row가 없으면 invalid 응답 (로그아웃 필요)
     * 	- 토큰의 device_id & device_type과, DB상 user_device의 device_id & device_type이 다르면 invalid 응답 (로그아웃 필요)
     * 	- 토큰의 iat와, DB상 user_device의 access_token_issued_at이 다르면 expired 응답 (토큰 재발급 필요)
     * - 헤더의 device_version & app_version과, DB상 user_device의 device_version & app_version이 다르면 DB갱신 (이 로직은 실패 해도 로그만찍고 다음 로직 진행되어야함)
     * - 문제 없으면 통과
     */
}
