# GCP → AWS S3 마이그레이션 가이드

## 개요

GCP에서 AWS로 마이그레이션하며 S3 폴더 구조를 환경별(dev/prod)로 분리했습니다.

**작업일:** 2026-01-20
**브랜치:** dev

---

## 변경 사항

### 1. S3 폴더 구조 변경

**변경 전:**
```text
gotcha-prod-files/
├── defaults/
├── profiles/
├── reviews/
└── shops/
```

**변경 후:**
```text
gotcha-prod-files/
├── prod/
│   ├── defaults/
│   ├── profiles/
│   ├── reviews/
│   └── shops/
└── dev/
    ├── defaults/
    ├── profiles/
    ├── reviews/
    └── shops/
```

### 2. 코드 변경

- ✅ `build.gradle`: GCP Cloud SQL 의존성 제거
- ✅ `application-dev.yml`, `application-prod.yml`: `aws.s3.prefix`를 환경변수로 변경
- ✅ `S3FileUploadService`: prefix 기본값 제거 (환경변수 필수)
- ✅ GCS 관련 주석 → "클라우드 스토리지"로 통일

### 3. 데이터베이스 변경

- users 테이블: `profile_image_url` 경로 업데이트
- shops 테이블: `main_image_url` 경로 업데이트
- review_images 테이블: `image_url` 경로 업데이트

---

## 배포 순서

### Phase 1: Dev 환경 (먼저 테스트)

#### 1단계: DBeaver에서 DB 업데이트

```bash
# 프로젝트 루트의 SQL 스크립트 실행
# migration-dev-s3-structure.sql
```

**실행 순서:**
1. DBeaver에서 `gotcha_dev` database 연결
2. SQL 스크립트 1단계 실행 → 현재 상태 확인
3. SQL 스크립트 2단계 실행 → 변경 미리보기
4. SQL 스크립트 3단계 실행 → 트랜잭션 시작 (BEGIN)
5. 결과 확인 후 `COMMIT;` 실행 (문제 있으면 `ROLLBACK;`)
6. SQL 스크립트 4단계 실행 → 최종 확인

#### 2단계: 환경변수 설정

**⚠️ 중요: 아래 가이드 문서를 참조하여 설정하세요:**
- **AWS SSM Parameter Store**: `docs/aws-ssm-setup-dev.md` 참조 (총 26개 파라미터)
- **GitHub Secrets**: `docs/github-secrets-setup-dev.md` 참조 (총 4개 Secret)

#### 3단계: 코드 커밋 & 배포

```bash
git add .
git commit -m "Migrate: GCP to AWS S3 with env-based folder structure"
git push origin dev
```

#### 4단계: Dev 환경 테스트

- [ ] API 정상 동작 확인
- [ ] 파일 업로드 테스트 (profiles, reviews, shops)
- [ ] 기존 이미지 조회 테스트
- [ ] 기본 이미지 조회 테스트

---

### Phase 2: Prod 환경 (테스트 통과 후)

#### 1단계: DBeaver에서 DB 업데이트

```bash
# 프로젝트 루트의 SQL 스크립트 실행
# migration-prod-s3-structure.sql
```

**실행 순서:** Dev와 동일

#### 2단계: 환경변수 설정

**⚠️ Prod 환경은 이미 SSM Parameter Store에 환경변수가 등록되어 있습니다.**

**업데이트 필요 항목:**
- SSM Parameter Store에서 `/gotcha/prod/aws/s3/prefix` 값을 `prod/`로 설정
```bash
aws ssm put-parameter \
  --name "/gotcha/prod/aws/s3/prefix" \
  --value "prod/" \
  --type "String" \
  --overwrite \
  --region ap-northeast-2
```

#### 3단계: Main 브랜치 머지

```bash
git checkout main
git merge dev
git push origin main
```

#### 4단계: Prod 배포 & 테스트

- [ ] API 정상 동작 확인
- [ ] 파일 업로드 테스트
- [ ] 기존 이미지 조회 테스트

---

### Phase 3: 정리 (모든 테스트 통과 후)

#### S3 기존 파일 삭제

```bash
# ⚠️ 주의: Prod 환경 완전히 확인 후 실행!

# 기존 defaults 폴더 삭제
aws s3 rm s3://gotcha-prod-files/defaults/ --recursive

# 기존 profiles 폴더 삭제
aws s3 rm s3://gotcha-prod-files/profiles/ --recursive

# 기존 빈 dev 폴더 삭제
aws s3 rm s3://gotcha-prod-files/dev/defaults/ --recursive
aws s3 rm s3://gotcha-prod-files/dev/profiles/ --recursive
aws s3 rm s3://gotcha-prod-files/dev/shops/ --recursive
```

---

## 환경변수 목록

### 필수 환경변수

| 변수명 | Dev | Prod |
|--------|-----|------|
| `AWS_S3_BUCKET_NAME` | `gotcha-prod-files` | `gotcha-prod-files` |
| `AWS_S3_PREFIX` | `dev/` | `prod/` |
| `AWS_REGION` | `ap-northeast-2` | `ap-northeast-2` |
| `AWS_ACCESS_KEY_ID` | (IAM Key) | (IAM Key) |
| `AWS_SECRET_ACCESS_KEY` | (IAM Secret) | (IAM Secret) |
| `USER_DEFAULT_PROFILE_IMAGE_URL` | `https://[S3_BUCKET].s3.[REGION].amazonaws.com/dev/defaults/profile-default-join.png` | `https://[S3_BUCKET].s3.[REGION].amazonaws.com/prod/defaults/profile-default-join.png` |
| `SHOP_DEFAULT_IMAGE_URL` | `https://[S3_BUCKET].s3.[REGION].amazonaws.com/dev/defaults/shop-default.png` | `https://[S3_BUCKET].s3.[REGION].amazonaws.com/prod/defaults/shop-default.png` |

---

## 롤백 방법

### 코드 롤백

```bash
# 이전 커밋으로 되돌리기
git revert HEAD
git push origin dev
```

### DB 롤백

```sql
-- dev/ → 원본 경로로 복원
BEGIN;

UPDATE users
SET profile_image_url = REPLACE(profile_image_url, '/dev/profiles/', '/profiles/')
WHERE profile_image_url LIKE '%/dev/profiles/%';

UPDATE users
SET profile_image_url = REPLACE(profile_image_url, '/dev/defaults/', '/defaults/')
WHERE profile_image_url LIKE '%/dev/defaults/%';

-- 확인 후 COMMIT
COMMIT;
```

---

## 파일 위치

| 파일 | 설명 |
|------|------|
| `migration-dev-s3-structure.sql` | Dev DB 마이그레이션 스크립트 |
| `migration-prod-s3-structure.sql` | Prod DB 마이그레이션 스크립트 |
| `docs/aws-setup-guide.md` | AWS 환경변수 설정 가이드 (업데이트됨) |
| `docs/aws-ssm-setup-dev.md` | **Dev 환경 SSM Parameter Store 설정 가이드** |
| `docs/github-secrets-setup-dev.md` | **Dev 환경 GitHub Secrets 설정 가이드** |
| `MIGRATION-GUIDE.md` | 이 문서 |

---

## 체크리스트

### Dev 환경
- [ ] DB 업데이트 완료 (gotcha_dev)
- [ ] 환경변수 설정 완료 (AWS_S3_PREFIX=dev/)
- [ ] 코드 커밋 & 배포
- [ ] 파일 업로드 테스트
- [ ] 기존 이미지 조회 테스트

### Prod 환경
- [ ] DB 업데이트 완료 (postgres)
- [ ] 환경변수 설정 완료 (AWS_S3_PREFIX=prod/)
- [ ] Main 브랜치 머지
- [ ] 배포 & 테스트

### 정리
- [ ] S3 기존 파일 삭제
- [ ] 마이그레이션 가이드 문서 보관

---

## 트러블슈팅

### Q. 파일 업로드 시 403 Forbidden
**A.** S3 버킷 권한 확인. IAM 사용자가 해당 prefix에 대한 PutObject 권한 필요.

### Q. 기존 이미지가 404 Not Found
**A.** DB 업데이트가 제대로 되었는지 확인. URL 경로에 `dev/` 또는 `prod/` prefix가 포함되어야 함.

### Q. 환경변수 AWS_S3_PREFIX가 비어있음
**A.** 배포 환경에 환경변수 추가. EC2는 docker run -e, GitHub Actions는 Secrets 설정.

---

## 참고

- AWS S3 Console: <https://s3.console.aws.amazon.com/s3/buckets/gotcha-prod-files>
- DBeaver 다운로드: <https://dbeaver.io/download/>
