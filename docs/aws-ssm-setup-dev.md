# AWS SSM Parameter Store Setup - Dev Environment

## 개요

Dev 환경 배포를 위해 AWS SSM Parameter Store에 등록해야 하는 모든 환경변수 목록입니다.

**작업일:** 2026-01-20
**환경:** Dev (gotcha_dev database, dev/ S3 prefix)
**리전:** ap-northeast-2
**총 파라미터 개수:** 26개

---

## 필수 파라미터 목록 (총 26개)

### 1. Database (3개)

```bash
# Database URL
aws ssm put-parameter \
  --name "/gotcha/dev/database/url" \
  --value "jdbc:postgresql://[RDS_ENDPOINT]:5432/gotcha_dev" \
  --type "SecureString" \
  --region ap-northeast-2

# Database Username
aws ssm put-parameter \
  --name "/gotcha/dev/database/username" \
  --value "[DB_USERNAME]" \
  --type "SecureString" \
  --region ap-northeast-2

# Database Password
aws ssm put-parameter \
  --name "/gotcha/dev/database/password" \
  --value "[DB_PASSWORD]" \
  --type "SecureString" \
  --region ap-northeast-2
```

**값 확인 방법:**
- RDS_ENDPOINT: AWS Console → RDS → gotcha-prod → Endpoint
- DB_USERNAME, DB_PASSWORD: RDS 생성 시 설정한 값 (prod와 동일)

---

### 2. CORS (1개)

```bash
# CORS Allowed Origins
aws ssm put-parameter \
  --name "/gotcha/dev/cors/allowed-origins" \
  --value "https://dev.gotcha.example.com,http://localhost:3000" \
  --type "String" \
  --region ap-northeast-2
```

**값 입력:**
- Dev 프론트엔드 도메인 + localhost 추가

---

### 3. JWT (3개)

```bash
# JWT Secret
aws ssm put-parameter \
  --name "/gotcha/dev/jwt/secret" \
  --value "[GENERATE_NEW_SECRET]" \
  --type "SecureString" \
  --region ap-northeast-2

# JWT Access Token Validity (밀리초)
aws ssm put-parameter \
  --name "/gotcha/dev/jwt/access-token-validity" \
  --value "3600000" \
  --type "String" \
  --region ap-northeast-2

# JWT Refresh Token Validity (밀리초)
aws ssm put-parameter \
  --name "/gotcha/dev/jwt/refresh-token-validity" \
  --value "604800000" \
  --type "String" \
  --region ap-northeast-2
```

**JWT Secret 생성 방법:**
```bash
openssl rand -base64 64
```

---

### 4. Kakao API (3개)

```bash
# Kakao REST API Key
aws ssm put-parameter \
  --name "/gotcha/dev/kakao/rest-api-key" \
  --value "[KAKAO_REST_API_KEY]" \
  --type "SecureString" \
  --region ap-northeast-2

# Kakao Admin Key
aws ssm put-parameter \
  --name "/gotcha/dev/kakao/admin-key" \
  --value "[KAKAO_ADMIN_KEY]" \
  --type "SecureString" \
  --region ap-northeast-2

# Kakao API Base URL
aws ssm put-parameter \
  --name "/gotcha/dev/kakao/api-base-url" \
  --value "https://dapi.kakao.com" \
  --type "String" \
  --region ap-northeast-2
```

**값 확인 방법:**
- Kakao Developers Console에서 발급
- Dev용 별도 앱 생성 권장 (prod와 분리)

---

### 5. OAuth - Common (3개)

```bash
# OAuth Redirect URI
aws ssm put-parameter \
  --name "/gotcha/dev/oauth/redirect-uri" \
  --value "https://dev.gotcha.example.com/oauth2/callback" \
  --type "String" \
  --region ap-northeast-2

# OAuth Allowed Redirect URIs (콤마 구분)
aws ssm put-parameter \
  --name "/gotcha/dev/oauth/allowed-redirect-uris" \
  --value "https://dev.gotcha.example.com/oauth2/callback,http://localhost:3000/oauth2/callback" \
  --type "String" \
  --region ap-northeast-2

# OAuth Cookie Encryption Key
aws ssm put-parameter \
  --name "/gotcha/dev/oauth/cookie-encryption-key" \
  --value "[GENERATE_NEW_KEY]" \
  --type "SecureString" \
  --region ap-northeast-2
```

**Cookie Encryption Key 생성:**
```bash
openssl rand -base64 32
```

---

### 6. OAuth - Kakao (2개)

```bash
# Kakao Client ID
aws ssm put-parameter \
  --name "/gotcha/dev/oauth/kakao/client-id" \
  --value "[KAKAO_CLIENT_ID]" \
  --type "SecureString" \
  --region ap-northeast-2

# Kakao Client Secret
aws ssm put-parameter \
  --name "/gotcha/dev/oauth/kakao/client-secret" \
  --value "[KAKAO_CLIENT_SECRET]" \
  --type "SecureString" \
  --region ap-northeast-2
```

---

### 7. OAuth - Google (2개)

```bash
# Google Client ID
aws ssm put-parameter \
  --name "/gotcha/dev/oauth/google/client-id" \
  --value "[GOOGLE_CLIENT_ID]" \
  --type "SecureString" \
  --region ap-northeast-2

# Google Client Secret
aws ssm put-parameter \
  --name "/gotcha/dev/oauth/google/client-secret" \
  --value "[GOOGLE_CLIENT_SECRET]" \
  --type "SecureString" \
  --region ap-northeast-2
```

---

### 8. OAuth - Naver (2개)

```bash
# Naver Client ID
aws ssm put-parameter \
  --name "/gotcha/dev/oauth/naver/client-id" \
  --value "[NAVER_CLIENT_ID]" \
  --type "SecureString" \
  --region ap-northeast-2

# Naver Client Secret
aws ssm put-parameter \
  --name "/gotcha/dev/oauth/naver/client-secret" \
  --value "[NAVER_CLIENT_SECRET]" \
  --type "SecureString" \
  --region ap-northeast-2
```

---

### 9. AWS S3 (4개)

```bash
# S3 Bucket Name
aws ssm put-parameter \
  --name "/gotcha/dev/aws/s3/bucket-name" \
  --value "gotcha-prod-files" \
  --type "String" \
  --region ap-northeast-2

# S3 Prefix (⚠️ 반드시 dev/ 로 설정)
aws ssm put-parameter \
  --name "/gotcha/dev/aws/s3/prefix" \
  --value "dev/" \
  --type "String" \
  --region ap-northeast-2

# S3 Access Key ID
aws ssm put-parameter \
  --name "/gotcha/dev/aws/s3/access-key-id" \
  --value "[S3_IAM_ACCESS_KEY]" \
  --type "SecureString" \
  --region ap-northeast-2

# S3 Secret Access Key
aws ssm put-parameter \
  --name "/gotcha/dev/aws/s3/secret-access-key" \
  --value "[S3_IAM_SECRET_KEY]" \
  --type "SecureString" \
  --region ap-northeast-2
```

**값 확인:**
- Bucket: `gotcha-prod-files` (prod와 동일 버킷)
- Prefix: **반드시 `dev/`** (prod는 `prod/`)
- IAM Key: S3 접근 권한이 있는 IAM 사용자 (prod와 동일 또는 별도)

---

### 10. AWS Region (1개)

```bash
# AWS Region
aws ssm put-parameter \
  --name "/gotcha/dev/aws/region" \
  --value "ap-northeast-2" \
  --type "String" \
  --region ap-northeast-2
```

---

### 11. Default Images (2개)

```bash
# User Default Profile Image URL
aws ssm put-parameter \
  --name "/gotcha/dev/user/default-profile-image-url" \
  --value "https://gotcha-prod-files.s3.ap-northeast-2.amazonaws.com/dev/defaults/profile-default-join.png" \
  --type "String" \
  --region ap-northeast-2

# Shop Default Image URL
aws ssm put-parameter \
  --name "/gotcha/dev/shop/default-image-url" \
  --value "https://gotcha-prod-files.s3.ap-northeast-2.amazonaws.com/dev/defaults/shop-default.png" \
  --type "String" \
  --region ap-northeast-2
```

**⚠️ 주의:** S3에 dev/defaults/ 폴더에 이미지 파일이 먼저 업로드되어 있어야 합니다.

---

## 전체 스크립트 (한 번에 실행)

모든 값을 준비한 후 아래 스크립트를 수정하여 사용하세요.

```bash
#!/bin/bash

# AWS Region
REGION="ap-northeast-2"

# Database
aws ssm put-parameter --name "/gotcha/dev/database/url" --value "jdbc:postgresql://[RDS_ENDPOINT]:5432/gotcha_dev" --type "SecureString" --region $REGION
aws ssm put-parameter --name "/gotcha/dev/database/username" --value "[DB_USERNAME]" --type "SecureString" --region $REGION
aws ssm put-parameter --name "/gotcha/dev/database/password" --value "[DB_PASSWORD]" --type "SecureString" --region $REGION

# CORS
aws ssm put-parameter --name "/gotcha/dev/cors/allowed-origins" --value "https://dev.gotcha.example.com,http://localhost:3000" --type "String" --region $REGION

# JWT
aws ssm put-parameter --name "/gotcha/dev/jwt/secret" --value "[JWT_SECRET]" --type "SecureString" --region $REGION
aws ssm put-parameter --name "/gotcha/dev/jwt/access-token-validity" --value "3600000" --type "String" --region $REGION
aws ssm put-parameter --name "/gotcha/dev/jwt/refresh-token-validity" --value "604800000" --type "String" --region $REGION

# Kakao API
aws ssm put-parameter --name "/gotcha/dev/kakao/rest-api-key" --value "[KAKAO_REST_KEY]" --type "SecureString" --region $REGION
aws ssm put-parameter --name "/gotcha/dev/kakao/admin-key" --value "[KAKAO_ADMIN]" --type "SecureString" --region $REGION
aws ssm put-parameter --name "/gotcha/dev/kakao/api-base-url" --value "https://dapi.kakao.com" --type "String" --region $REGION

# OAuth - Common
aws ssm put-parameter --name "/gotcha/dev/oauth/redirect-uri" --value "https://dev.gotcha.example.com/oauth2/callback" --type "String" --region $REGION
aws ssm put-parameter --name "/gotcha/dev/oauth/allowed-redirect-uris" --value "https://dev.gotcha.example.com/oauth2/callback,http://localhost:3000/oauth2/callback" --type "String" --region $REGION
aws ssm put-parameter --name "/gotcha/dev/oauth/cookie-encryption-key" --value "[OAUTH_COOKIE_KEY]" --type "SecureString" --region $REGION

# OAuth - Kakao
aws ssm put-parameter --name "/gotcha/dev/oauth/kakao/client-id" --value "[KAKAO_CLIENT_ID]" --type "SecureString" --region $REGION
aws ssm put-parameter --name "/gotcha/dev/oauth/kakao/client-secret" --value "[KAKAO_CLIENT_SECRET]" --type "SecureString" --region $REGION

# OAuth - Google
aws ssm put-parameter --name "/gotcha/dev/oauth/google/client-id" --value "[GOOGLE_CLIENT_ID]" --type "SecureString" --region $REGION
aws ssm put-parameter --name "/gotcha/dev/oauth/google/client-secret" --value "[GOOGLE_CLIENT_SECRET]" --type "SecureString" --region $REGION

# OAuth - Naver
aws ssm put-parameter --name "/gotcha/dev/oauth/naver/client-id" --value "[NAVER_CLIENT_ID]" --type "SecureString" --region $REGION
aws ssm put-parameter --name "/gotcha/dev/oauth/naver/client-secret" --value "[NAVER_CLIENT_SECRET]" --type "SecureString" --region $REGION

# AWS S3
aws ssm put-parameter --name "/gotcha/dev/aws/s3/bucket-name" --value "gotcha-prod-files" --type "String" --region $REGION
aws ssm put-parameter --name "/gotcha/dev/aws/s3/prefix" --value "dev/" --type "String" --region $REGION
aws ssm put-parameter --name "/gotcha/dev/aws/s3/access-key-id" --value "[S3_ACCESS_KEY]" --type "SecureString" --region $REGION
aws ssm put-parameter --name "/gotcha/dev/aws/s3/secret-access-key" --value "[S3_SECRET_KEY]" --type "SecureString" --region $REGION

# AWS Region
aws ssm put-parameter --name "/gotcha/dev/aws/region" --value "ap-northeast-2" --type "String" --region $REGION

# Default Images
aws ssm put-parameter --name "/gotcha/dev/user/default-profile-image-url" --value "https://gotcha-prod-files.s3.ap-northeast-2.amazonaws.com/dev/defaults/profile-default-join.png" --type "String" --region $REGION
aws ssm put-parameter --name "/gotcha/dev/shop/default-image-url" --value "https://gotcha-prod-files.s3.ap-northeast-2.amazonaws.com/dev/defaults/shop-default.png" --type "String" --region $REGION

echo "✅ All SSM parameters created successfully!"
```

---

## 파라미터 확인 방법

### 모든 dev 파라미터 조회

```bash
aws ssm get-parameters-by-path \
  --path "/gotcha/dev" \
  --recursive \
  --region ap-northeast-2
```

### 특정 파라미터 값 확인

```bash
aws ssm get-parameter \
  --name "/gotcha/dev/database/url" \
  --with-decryption \
  --region ap-northeast-2
```

---

## 파라미터 수정 방법

```bash
# 기존 파라미터 덮어쓰기 (--overwrite 플래그 추가)
aws ssm put-parameter \
  --name "/gotcha/dev/jwt/secret" \
  --value "[NEW_VALUE]" \
  --type "SecureString" \
  --overwrite \
  --region ap-northeast-2
```

---

## 파라미터 삭제 방법

```bash
# 개별 삭제
aws ssm delete-parameter \
  --name "/gotcha/dev/jwt/secret" \
  --region ap-northeast-2

# 전체 dev 파라미터 삭제 (⚠️ 주의!)
aws ssm delete-parameters \
  --names $(aws ssm get-parameters-by-path --path "/gotcha/dev" --recursive --query "Parameters[].Name" --output text --region ap-northeast-2) \
  --region ap-northeast-2
```

---

## 체크리스트

- [ ] 1. Database 파라미터 (3개)
- [ ] 2. CORS 파라미터 (1개)
- [ ] 3. JWT 파라미터 (3개)
- [ ] 4. Kakao API 파라미터 (3개)
- [ ] 5. OAuth Common 파라미터 (3개)
- [ ] 6. OAuth Kakao 파라미터 (2개)
- [ ] 7. OAuth Google 파라미터 (2개)
- [ ] 8. OAuth Naver 파라미터 (2개)
- [ ] 9. AWS S3 파라미터 (4개)
- [ ] 10. AWS Region 파라미터 (1개)
- [ ] 11. Default Images 파라미터 (2개)
- [ ] 12. 전체 파라미터 확인 (`aws ssm get-parameters-by-path`)
- [ ] 13. GitHub Secrets 추가 (다음 단계)
- [ ] 14. ECR Repository 생성 (다음 단계)

---

## 다음 단계

SSM 파라미터 등록 완료 후:

1. **GitHub Secrets 추가** (`.github/workflows/cicd-dev.yml` 참조)
   - `ECR_REPOSITORY_DEV`: gotcha-be-dev
   - `EC2_HOST_DEV`: Dev EC2 IP 또는 도메인
   - `EC2_USER_DEV`: ubuntu
   - `EC2_SSH_KEY_DEV`: Dev EC2 SSH private key

2. **ECR Repository 생성**
   ```bash
   aws ecr create-repository \
     --repository-name gotcha-be-dev \
     --region ap-northeast-2
   ```

3. **Dev 브랜치 푸시 후 배포 테스트**
   ```bash
   git push origin dev
   ```
