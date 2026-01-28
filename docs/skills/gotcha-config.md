# GOTCHA 설정 관리 스킬

## 필수 규칙

### 1. 환경변수화 (하드코딩 금지)

**반드시 환경변수로 관리할 것:**
- URL, 도메인
- API 키, 시크릿
- CORS 허용 origin
- DB 연결 정보
- 외부 서비스 연결 정보

### 2. 환경변수 형식

```yaml
# 필수 환경변수 (기본값 없음)
cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS}

# 선택 환경변수 (기본값 있음) - local 환경만
jwt:
  secret: ${JWT_SECRET:default-for-local-only}
```

### 3. 프로파일별 규칙

| 프로파일 | 기본값 허용 | 예시 |
|---------|------------|------|
| local | O | `${VAR:default}` |
| dev | X | `${VAR}` |
| prod | X | `${VAR}` |

## 파일 구조

```text
src/main/resources/
├── application.yml          # 공통 설정
├── application-local.yml    # 로컬 개발
├── application-dev.yml      # 개발 서버
└── application-prod.yml     # 운영 서버
```

## application.yml (공통)

```yaml
spring:
  application:
    name: gotcha-server
  config:
    import: optional:file:.env[.properties]
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}
  datasource:
    driver-class-name: org.postgresql.Driver
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

server:
  port: ${PORT:8080}

jwt:
  secret: ${JWT_SECRET:default-secret-for-local}
  access-token-validity: ${JWT_ACCESS_TOKEN_VALIDITY:3600000}
  refresh-token-validity: ${JWT_REFRESH_TOKEN_VALIDITY:1209600000}
```

## application-local.yml

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173}

# User Configuration
user:
  default-profile-image-url: ${USER_DEFAULT_PROFILE_IMAGE_URL:https://gotcha-prod-files.s3.ap-northeast-2.amazonaws.com/dev/defaults/profile-default-join.png}

# Shop Configuration
shop:
  default-image-url: ${SHOP_DEFAULT_IMAGE_URL:https://gotcha-prod-files.s3.ap-northeast-2.amazonaws.com/dev/defaults/shop-default.png}
```

## application-dev.yml / application-prod.yml

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate  # prod는 validate, dev는 update
    show-sql: false

cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS}  # 기본값 없음!
```

## .env 파일 (로컬 개발용)

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/gotcha
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=password
JWT_SECRET=my-local-secret-key
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

# 기본 이미지 URL (선택 - application-local.yml에 기본값 있음)
USER_DEFAULT_PROFILE_IMAGE_URL=https://gotcha-default.s3.ap-northeast-2.amazonaws.com/default-profile.png
SHOP_DEFAULT_IMAGE_URL=https://gotcha-default.s3.ap-northeast-2.amazonaws.com/default-shop.png
```

## .gitignore 확인

```text
.env
.env.*
*.env
application-local.yml
application-secret.yml
```

## @Value 사용

```java
@Value("${cors.allowed-origins}")
private String allowedOrigins;

@Value("${jwt.secret}")
private String jwtSecret;

@Value("${jwt.access-token-validity}")
private long accessTokenValidity;
```

## @ConfigurationProperties 사용 (권장)

```java
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {
    private String secret;
    private long accessTokenValidity;
    private long refreshTokenValidity;
}
```

## 새 설정 추가 시 체크리스트

- [ ] application.yml에 기본 구조 정의
- [ ] 환경변수 형식 사용 (`${VAR}` 또는 `${VAR:default}`)
- [ ] local: 기본값 허용
- [ ] dev/prod: 필수 환경변수 (기본값 없음)
- [ ] .env.example 업데이트 (있는 경우)
- [ ] README 또는 문서 업데이트

## 금지 사항

```yaml
# 절대 하지 말 것
cors:
  allowed-origins: https://gotcha.it.com  # 하드코딩 금지!

database:
  password: mypassword123  # 비밀번호 하드코딩 금지!

api:
  key: sk-abc123  # API 키 하드코딩 금지!
```
