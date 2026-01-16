# AWS SSM Parameter Store 설정 가이드 (Console)

## AWS Console에서 파라미터 등록하기

### 1. AWS Console 접속

1. AWS Console 로그인
2. 검색창에 "Systems Manager" 입력하고 선택
3. 왼쪽 메뉴에서 "Parameter Store" 클릭

### 2. 파라미터 등록

각 파라미터를 아래 표를 참고하여 하나씩 등록합니다.

#### 등록 절차 (모든 파라미터 공통)

1. "Create parameter" 버튼 클릭
2. 아래 정보 입력:
   - **Name**: 표의 "Parameter Name" 값
   - **Tier**: `Standard` 선택
   - **Type**: 표의 "Type" 값 선택
   - **Value**: GitHub Secrets에 저장된 실제 값 입력
3. "Create parameter" 버튼 클릭

---

## 등록할 파라미터 목록 (총 25개)

### Database (3개)

| Parameter Name | Type | GitHub Secret 이름 | 설명 |
|---|---|---|---|
| `/gotcha/prod/database/url` | SecureString | `DB_URL_PROD` | DB 연결 URL |
| `/gotcha/prod/database/username` | SecureString | `DB_USERNAME_PROD` | DB 사용자명 |
| `/gotcha/prod/database/password` | SecureString | `DB_PASSWORD_PROD` | DB 비밀번호 |

### CORS (1개)

| Parameter Name | Type | GitHub Secret 이름 | 설명 |
|---|---|---|---|
| `/gotcha/prod/cors/allowed-origins` | String | `CORS_ALLOWED_ORIGINS_PROD` | 허용된 Origin 목록 |

### JWT (3개)

| Parameter Name | Type | GitHub Secret 이름 | 설명 |
|---|---|---|---|
| `/gotcha/prod/jwt/secret` | SecureString | `JWT_SECRET_PROD` | JWT 서명 키 |
| `/gotcha/prod/jwt/access-token-validity` | String | `JWT_ACCESS_TOKEN_VALIDITY` | Access Token 유효시간(ms) |
| `/gotcha/prod/jwt/refresh-token-validity` | String | `JWT_REFRESH_TOKEN_VALIDITY` | Refresh Token 유효시간(ms) |

### Kakao API (3개)

| Parameter Name | Type | GitHub Secret 이름 | 설명 |
|---|---|---|---|
| `/gotcha/prod/kakao/rest-api-key` | SecureString | `KAKAO_REST_API_KEY` | 카카오 REST API 키 |
| `/gotcha/prod/kakao/admin-key` | SecureString | `KAKAO_ADMIN_KEY` | 카카오 Admin 키 |
| `/gotcha/prod/kakao/api-base-url` | String | `KAKAO_API_BASE_URL` | 카카오 API Base URL |

### OAuth - Kakao (2개)

| Parameter Name | Type | GitHub Secret 이름 | 설명 |
|---|---|---|---|
| `/gotcha/prod/oauth/kakao/client-id` | SecureString | `KAKAO_CLIENT_ID` | 카카오 OAuth Client ID |
| `/gotcha/prod/oauth/kakao/client-secret` | SecureString | `KAKAO_CLIENT_SECRET` | 카카오 OAuth Client Secret |

### OAuth - Google (2개)

| Parameter Name | Type | GitHub Secret 이름 | 설명 |
|---|---|---|---|
| `/gotcha/prod/oauth/google/client-id` | SecureString | `GOOGLE_CLIENT_ID` | Google OAuth Client ID |
| `/gotcha/prod/oauth/google/client-secret` | SecureString | `GOOGLE_CLIENT_SECRET` | Google OAuth Client Secret |

### OAuth - Naver (2개)

| Parameter Name | Type | GitHub Secret 이름 | 설명 |
|---|---|---|---|
| `/gotcha/prod/oauth/naver/client-id` | SecureString | `NAVER_CLIENT_ID` | Naver OAuth Client ID |
| `/gotcha/prod/oauth/naver/client-secret` | SecureString | `NAVER_CLIENT_SECRET` | Naver OAuth Client Secret |

### OAuth - Common (3개)

| Parameter Name | Type | GitHub Secret 이름 | 설명 |
|---|---|---|---|
| `/gotcha/prod/oauth/redirect-uri` | String | `OAUTH2_REDIRECT_URI_PROD` | OAuth Redirect URI |
| `/gotcha/prod/oauth/allowed-redirect-uris` | String | `OAUTH2_ALLOWED_REDIRECT_URIS_PROD` | 허용된 Redirect URI 목록 |
| `/gotcha/prod/oauth/cookie-encryption-key` | SecureString | `OAUTH2_COOKIE_ENCRYPTION_KEY_PROD` | Cookie 암호화 키 |

### AWS S3 (3개)

| Parameter Name | Type | GitHub Secret 이름 | 설명 |
|---|---|---|---|
| `/gotcha/prod/aws/s3/bucket-name` | String | `AWS_S3_BUCKET_NAME` | S3 버킷 이름 |
| `/gotcha/prod/aws/s3/access-key-id` | SecureString | `AWS_S3_ACCESS_KEY_ID` | S3 Access Key ID |
| `/gotcha/prod/aws/s3/secret-access-key` | SecureString | `AWS_S3_SECRET_ACCESS_KEY` | S3 Secret Access Key |

### Default Images (2개)

| Parameter Name | Type | GitHub Secret 이름 | 설명 |
|---|---|---|---|
| `/gotcha/prod/user/default-profile-image-url` | String | `USER_DEFAULT_PROFILE_IMAGE_URL_PROD` | 기본 프로필 이미지 URL |
| `/gotcha/prod/shop/default-image-url` | String | `SHOP_DEFAULT_IMAGE_URL_PROD` | 기본 샵 이미지 URL |

---

## Type 선택 가이드

- **SecureString**: 비밀번호, API 키, Secret 등 민감한 정보
- **String**: URL, 숫자, 일반 설정값 등 공개되어도 괜찮은 정보

---

## 3. IAM 권한 설정

GitHub Actions에서 SSM 파라미터를 읽을 수 있도록 IAM 권한을 추가해야 합니다.

### 방법 1: 기존 사용자에 권한 추가

1. AWS Console → IAM → Users
2. GitHub Actions에서 사용 중인 사용자 선택 (AWS_ACCESS_KEY_ID에 해당하는 사용자)
3. "Permissions" 탭 → "Add permissions" → "Create inline policy"
4. "JSON" 탭 클릭
5. 아래 정책 붙여넣기 (YOUR_ACCOUNT_ID를 실제 계정 ID로 변경):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ssm:GetParameter",
        "ssm:GetParameters",
        "ssm:GetParametersByPath"
      ],
      "Resource": [
        "arn:aws:ssm:*:YOUR_ACCOUNT_ID:parameter/gotcha/prod/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "kms:Decrypt"
      ],
      "Resource": "*"
    }
  ]
}
```

6. "Review policy"
7. Name: `GotchaSSMReadAccess`
8. "Create policy"

### AWS Account ID 확인 방법

- AWS Console 우측 상단의 계정명 클릭 → 12자리 숫자가 Account ID

---

## 4. 등록 완료 확인

모든 파라미터 등록 후:

1. Parameter Store 메인 화면에서 `/gotcha/prod`로 필터링
2. 총 25개 파라미터가 표시되는지 확인
3. 각 파라미터 클릭하여 값이 정확한지 확인 (SecureString은 "Show" 버튼 클릭)

---

## 5. 마이그레이션 체크리스트

- [ ] AWS SSM에 25개 파라미터 모두 등록 완료
- [ ] 각 파라미터 값 정확성 확인 (GitHub Secrets와 동일한지)
- [ ] IAM 권한 설정 완료
- [ ] GitHub Actions workflow 파일 수정 완료
- [ ] 테스트 배포 성공 확인
- [ ] 애플리케이션 정상 동작 확인
- [ ] GitHub Secrets 정리 (옵션)

---

## 6. 주의사항

1. **파라미터 이름 정확히 입력**: 오타가 있으면 GitHub Actions에서 값을 찾지 못합니다
2. **Type 올바르게 선택**: 민감한 정보는 반드시 SecureString
3. **값 복사 시 공백 주의**: 앞뒤 공백이 포함되지 않도록 주의
4. **GitHub Secrets 보존**: 문제 발생 시 롤백을 위해 일단 유지

---

## 7. 파라미터 수정 방법

나중에 값을 변경해야 할 때:

1. Parameter Store에서 해당 파라미터 클릭
2. 우측 상단 "Edit" 버튼
3. Value 수정
4. "Save changes"
5. 애플리케이션 재배포 필요

---

## 문제 해결

### "Access Denied" 오류 발생 시
→ IAM 권한 설정 확인 (3번 섹션)

### 파라미터를 찾을 수 없다는 오류
→ Parameter Name 오타 확인 (대소문자, 슬래시 등)

### 값이 제대로 전달되지 않는 경우
→ GitHub Actions workflow 파일의 환경변수 이름 확인
