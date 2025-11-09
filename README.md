# Service Server
모바일 앱을 위한 인증 · 사용자 · 약관 도메인을 담당하는 Spring Boot 기반 백엔드 서비스입니다. Kotlin으로 작성되었고 JWT/JWE 기반 자체 토큰, 소셜 로그인 연동, 디바이스 관리, 약관 버전 관리를 제공합니다.

`com.service.api` 패키지 아래에 REST API, 외부 소셜 사이트 연동, 토큰 발급 및 검증, 약관 관리 등 핵심 로직이 구성되어 있습니다.

## API 명세 문서
- ### [API 명세 문서 바로가기](https://ktyu.github.io/service-server-user/swagger-ui.html)
  - `docs/swagger-ui.html` 파일을 GitHub Pages로 제공
- ### (참고) API 요청 공통 헤더
| Header | 설명                         | 비고 |
| --- |----------------------------| --- |
| `X-Service-Custom-Device-Id` | 디바이스 고유 UUID               | 36자 UUID, 잘못된 값은 400 반환 |
| `X-Service-Device-Model` | 단말 모델명                     | ALB 레벨에서 필수 검증 |
| `X-Service-Os-Type` | iOS`/`Android` 등 지원 OS     | 열거형 외 값은 400 반환 |
| `X-Service-Os-Version` | OS 버전 문자열                  | Access/Refresh 토큰과 매칭 검증에 사용 |
| `X-Service-App-Version` | 세 자리 세그먼트 버전(e.g. `1.2.3`) | 미지원 버전은 426 UPGRADE REQUIRED |

## AWS 인프라 설계 (프리티어 활용)
![AWS 인프라 아키텍처](docs/infra_aws_architecture.png)
- Public Subnet에 ALB, NAT Gateway, Bastion Host를 두어 외부 트래픽과 운영 접속을 분리합니다.
  - internet inbound/outbound 모두 가능
- Default Subnet의 Auto Scaling 그룹(EC2)이 ALB 트래픽을 받고, Lambda가 동일 VPC에서 보조 작업을 수행합니다.
  - NAT Gateway를 경유하여 internet outbound 가능
- Private Subnet에는 CMS와 RDS를 배치하여 직접적인 외부 접근을 모두 차단하고 VPC 내부에서만 접근 가능하게 합니다.
  - internet inbound/outbound 모두 불가
- S3, SQS, Managed Lambda와는 VPC Endpoint 또는 Internet Gateway를 통해 보안 통신을 구성합니다.

## 서비스 개요
- 여러 소셜 사이트를 이용한 사용자의 회원가입 및 로그인을 지원합니다.
- 모든 API 요청은 `ApiHeaderContextFilter`를 통해 단말 공통 메타데이터를 검증한 뒤 ThreadLocal 컨텍스트에 주입합니다.
- 자체 발급한 JWT(Access) + AES-GCM 기반 JWE(Refresh)로 인증을 확인하고 갱신합니다.

## 기술 스택
- Spring Boot 3.2, Kotlin 1.9, Gradle Kotlin DSL
- Spring MVC + Spring WebFlux HTTP Interface Client
- MySQL 8.4, Spring Data JPA, Hibernate 6

## 아키텍처 하이라이트
- `ApiHeaderContextFilter`가 디바이스 UUID, OS, 앱 버전을 검증하고 업그레이드 요구 정책을 적용합니다.
- `AuthorizationInterceptor`가 `@Auth` 지정 API에 대해 토큰 유효성, 디바이스 상태, DB 버전 동기화 여부를 검사합니다.
- `UserService`가 소셜 식별자와 약관 동의 정보를 검증하고 `user_identity`/`user_profile`을 생성·갱신하여 가입/로그인 흐름을 단일 진입점에서 처리합니다.
- `DeviceService`가 토큰 발급·재발급·폐기를 담당하며 AES-GCM 기반 Refresh Token 암호화와 비밀키 로테이션(sha256 파생)을 지원합니다.
- `SocialService`가 Kakao OpenAPI와 통신하여 액세스 토큰 상태, 이메일 인증 여부를 동기화하고 향후 타 소셜 확장을 대비한 인터페이스를 정의합니다.
- `TermsService`가 DB 약관 버전과 사용자가 동의한 버전을 비교하여 필수 약관 누락을 방지합니다.

## 주요 패키지
- `controller`: 인증(`AuthController`), 회원(`UserController`), 약관(`TermsController`), 헬스체크 및 웹뷰 리다이렉션 엔드포인트를 제공합니다.
- `service`: 사용자 생성, 디바이스 관리, 약관 검증 로직과 Kakao 연동을 포함한 도메인 서비스 계층입니다.
- `persistence`: JPA 엔티티(`user_identity`, `user_profile`, `user_social`, `user_device`, `terms`), Repository, Mapper로 DB와 도메인 모델을 연결합니다.
- `filter`: `ApiHeaderContextFilter` 등 공통 Servlet 필터가 요청 컨텍스트를 구성합니다.
- `client`: Spring 6 HTTP Interface(`KapiKakaoComClient`)로 Kakao API를 호출합니다.
- `common`: 예외, enum, 요청 컨텍스트 등 공통 유틸리티와 상수를 정의합니다.

## 데이터 스키마
- 테이블 정의서는 `src/main/resources/ddl` 디렉터리에 SQL로 분리되어 있습니다.
- `user_*` 테이블이 사용자 식별, 소셜 계정, 디바이스 이력을 담당하고 `terms` 테이블이 약관 버전을 관리합니다.
- 모든 타임스탬프는 `DATETIME`(Asia/Seoul)을 기본으로 하며, 삭제는 `deleted_at` Soft Delete 컬럼으로 표현합니다.

## 환경 변수 및 Spring 설정
- `SERVICE_ACCESS_TOKEN_SECRET`, `SERVICE_REFRESH_TOKEN_SECRET`: 토큰 서명/암호화 키 소재; 미설정 시 기본 문자열을 사용하므로 운영 환경에서는 반드시 Secret Manager 등으로 주입합니다.
- `API_KAPI_KAKAO_COM_APP_ID`, `API_KAPI_KAKAO_COM_APP_ADMIN_KEY`: Kakao 연동용 프로퍼티로 `application.yml` 기본값을 덮어써야 합니다.
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`: 로컬 기본값은 `application.yml`에 정의되어 있으며, 환경별 프로파일에서 재정의할 수 있습니다.
- `SPRING_PROFILES_ACTIVE`: `local`, `dev`, `prod` 등 환경별 설정을 분리할 때 사용합니다. 필요 시 `application-<profile>.yml`을 생성하여 관리하세요.
