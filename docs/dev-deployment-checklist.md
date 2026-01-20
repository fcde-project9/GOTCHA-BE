# Dev 환경 배포 준비 체크리스트

## 개요

Dev 환경(AWS EC2 t3.micro)으로 배포하기 위해 완료해야 할 모든 작업 목록입니다.

**작업일:** 2026-01-20
**브랜치:** dev (또는 refactor/migrate-to-aws-s3)
**대상 환경:** Dev EC2 (t3.micro)

---

## ✅ 완료된 작업

### 1. 코드 변경
- [x] GCP Cloud SQL 의존성 제거 (`build.gradle`)
- [x] S3 Prefix 환경변수화 (`application-dev.yml`, `application-prod.yml`)
- [x] S3FileUploadService 기본값 제거
- [x] GCS → 클라우드 스토리지 주석 변경
- [x] 테스트 파일 메시지 업데이트

### 2. CI/CD 파이프라인
- [x] cicd-prod.yml: t3.small 메모리 최적화 (400MB → 1536MB)
- [x] cicd-prod.yml: AWS_S3_PREFIX 환경변수 추가
- [x] cicd-dev.yml: GCP Cloud Run → AWS EC2 배포 방식 변경

### 3. 문서화
- [x] MIGRATION-GUIDE.md 작성
- [x] migration-dev-s3-structure.sql 작성
- [x] migration-prod-s3-structure.sql 작성
- [x] docs/aws-setup-guide.md 업데이트
- [x] docs/aws-ssm-setup-dev.md 작성
- [x] docs/github-secrets-setup-dev.md 작성

### 4. Git 커밋
- [x] Commit 1: GCP to AWS S3 migration
- [x] Commit 2: t3.small instance optimization

---

## ⏳ 남은 작업

### Step 1: AWS SSM Parameter Store 설정 (필수)

**문서:** `docs/aws-ssm-setup-dev.md`

**총 26개 파라미터 등록 필요:**

| 카테고리 | 개수 | 파라미터 경로 |
|----------|------|--------------|
| Database | 3 | `/gotcha/dev/database/*` |
| CORS | 1 | `/gotcha/dev/cors/*` |
| JWT | 3 | `/gotcha/dev/jwt/*` |
| Kakao API | 3 | `/gotcha/dev/kakao/*` |
| OAuth Common | 3 | `/gotcha/dev/oauth/*` |
| OAuth Kakao | 2 | `/gotcha/dev/oauth/kakao/*` |
| OAuth Google | 2 | `/gotcha/dev/oauth/google/*` |
| OAuth Naver | 2 | `/gotcha/dev/oauth/naver/*` |
| AWS S3 | 4 | `/gotcha/dev/aws/s3/*` |
| AWS Region | 1 | `/gotcha/dev/aws/region` |
| Default Images | 2 | `/gotcha/dev/user/*`, `/gotcha/dev/shop/*` |

**빠른 시작:**
```bash
# 1. docs/aws-ssm-setup-dev.md 문서 열기
# 2. 전체 스크립트 섹션의 값들 수정
# 3. 스크립트 실행
# 4. 확인
aws ssm get-parameters-by-path --path "/gotcha/dev" --recursive --region ap-northeast-2
```

**체크:**
- [ ] Database 파라미터 (3개)
- [ ] CORS 파라미터 (1개)
- [ ] JWT 파라미터 (3개)
- [ ] Kakao API 파라미터 (3개)
- [ ] OAuth Common 파라미터 (3개)
- [ ] OAuth Kakao 파라미터 (2개)
- [ ] OAuth Google 파라미터 (2개)
- [ ] OAuth Naver 파라미터 (2개)
- [ ] AWS S3 파라미터 (4개) - **특히 AWS_S3_PREFIX=dev/ 확인**
- [ ] AWS Region 파라미터 (1개)
- [ ] Default Images 파라미터 (2개)

---

### Step 2: S3 기본 이미지 업로드 (필수)

**목적:** Default 이미지가 S3에 존재해야 애플리케이션이 정상 작동

**필요 파일:**
- `dev/defaults/profile-default-join.png`
- `dev/defaults/shop-default.png`

**업로드 방법:**

#### 방법 1: AWS Console 사용
1. https://s3.console.aws.amazon.com/s3/buckets/gotcha-prod-files
2. `dev/defaults/` 폴더 생성
3. 이미지 파일 업로드

#### 방법 2: AWS CLI 사용
```bash
# Prod에서 Dev로 복사
aws s3 cp s3://gotcha-prod-files/prod/defaults/profile-default-join.png \
           s3://gotcha-prod-files/dev/defaults/profile-default-join.png \
           --region ap-northeast-2

aws s3 cp s3://gotcha-prod-files/prod/defaults/shop-default.png \
           s3://gotcha-prod-files/dev/defaults/shop-default.png \
           --region ap-northeast-2
```

**확인:**
```bash
aws s3 ls s3://gotcha-prod-files/dev/defaults/ --region ap-northeast-2
```

**체크:**
- [ ] `dev/defaults/profile-default-join.png` 업로드
- [ ] `dev/defaults/shop-default.png` 업로드
- [ ] S3에서 파일 존재 확인

---

### Step 3: ECR Repository 생성 (필수)

**리포지토리 이름:** `gotcha-be-dev`

**생성 방법:**
```bash
aws ecr create-repository \
  --repository-name gotcha-be-dev \
  --region ap-northeast-2
```

**생성 확인:**
```bash
aws ecr describe-repositories \
  --repository-names gotcha-be-dev \
  --region ap-northeast-2
```

**체크:**
- [ ] ECR Repository `gotcha-be-dev` 생성 완료
- [ ] Repository URI 확인 (예: `123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/gotcha-be-dev`)

---

### Step 4: GitHub Secrets 설정 (필수)

**문서:** `docs/github-secrets-setup-dev.md`

**총 4개 Secrets 등록 필요:**

| Secret 이름 | 값 | 설명 |
|-------------|-----|------|
| `ECR_REPOSITORY_DEV` | `gotcha-be-dev` | Dev ECR 리포지토리 이름 |
| `EC2_HOST_DEV` | [Dev EC2 Public IP] | Dev EC2 접속 주소 |
| `EC2_USER_DEV` | `ubuntu` | EC2 SSH 사용자 |
| `EC2_SSH_KEY_DEV` | [SSH Private Key 전체] | Dev EC2 SSH Key |

**등록 경로:**
GitHub Repository → Settings → Secrets and variables → Actions → New repository secret

**EC2 정보 확인:**
```bash
# EC2 Public IP 확인
aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=gotcha-dev" \
  --query "Reservations[0].Instances[0].PublicIpAddress" \
  --output text \
  --region ap-northeast-2
```

**체크:**
- [ ] `ECR_REPOSITORY_DEV` 등록
- [ ] `EC2_HOST_DEV` 등록
- [ ] `EC2_USER_DEV` 등록
- [ ] `EC2_SSH_KEY_DEV` 등록

---

### Step 5: 코드 커밋 & 푸시 (마지막)

**현재 브랜치:** refactor/migrate-to-aws-s3

**커밋 전 확인:**
```bash
./gradlew build -x test
```

**커밋 & 푸시:**
```bash
# 새로 생성된 문서 파일 추가
git add docs/aws-ssm-setup-dev.md
git add docs/github-secrets-setup-dev.md
git add docs/dev-deployment-checklist.md
git add MIGRATION-GUIDE.md

# 커밋
git commit -m "Docs: Add comprehensive AWS setup guides for dev environment"

# dev 브랜치로 머지 또는 직접 푸시
git checkout dev
git merge refactor/migrate-to-aws-s3
git push origin dev
```

**체크:**
- [ ] 빌드 성공 확인
- [ ] 변경사항 커밋
- [ ] dev 브랜치 푸시
- [ ] GitHub Actions 워크플로우 실행 확인

---

### Step 6: 배포 테스트

**GitHub Actions 모니터링:**
- GitHub Repository → Actions → 최신 워크플로우 확인
- "CI/CD Pipeline (Development)" 워크플로우 상태 확인

**예상 실행 단계:**
1. ✅ Checkout code
2. ✅ Configure AWS credentials
3. ✅ Login to Amazon ECR
4. ✅ Build and Push Docker Image
5. ✅ Deploy to EC2
6. ✅ Cleanup SSH key

**배포 후 EC2 확인:**
```bash
# EC2 접속
ssh -i [dev-key.pem] ubuntu@[EC2_HOST_DEV]

# Docker 컨테이너 실행 확인
docker ps | grep gotcha-be-dev

# 로그 확인
docker logs gotcha-be-dev

# 헬스체크 (EC2 내부)
curl http://localhost:8080/actuator/health
```

**API 테스트:**
```bash
# 외부에서 API 호출 테스트
curl http://[EC2_HOST_DEV]:8080/actuator/health
```

**체크:**
- [ ] GitHub Actions 워크플로우 성공
- [ ] Docker 컨테이너 실행 중
- [ ] 애플리케이션 로그 정상
- [ ] API 응답 정상

---

## 🚨 문제 발생 시

### 워크플로우 실패: "repository does not exist"
**원인:** ECR Repository 미생성
**해결:** Step 3 다시 실행

### 워크플로우 실패: "parameter not found"
**원인:** SSM Parameter Store 미설정
**해결:** Step 1 다시 실행 후 누락된 파라미터 추가

### 컨테이너 실패: "Cannot connect to database"
**원인:** Database 파라미터 오류
**해결:** SSM Parameter Store에서 `/gotcha/dev/database/*` 값 확인

### 컨테이너 실패: "Access Denied (S3)"
**원인:** S3 Prefix 오류 또는 IAM 권한 부족
**해결:**
- `/gotcha/dev/aws/s3/prefix` 값이 `dev/`인지 확인
- IAM 사용자에 S3 `PutObject`, `GetObject` 권한 있는지 확인

### API 404: "Default image not found"
**원인:** S3에 기본 이미지 미업로드
**해결:** Step 2 다시 실행

---

## 전체 진행 상황 요약

### 완료 비율: 60%

| 단계 | 상태 | 설명 |
|------|------|------|
| 코드 변경 | ✅ 100% | GCP → AWS 마이그레이션 완료 |
| CI/CD 파이프라인 | ✅ 100% | Dev/Prod 모두 AWS EC2 배포 설정 |
| 문서화 | ✅ 100% | 모든 가이드 문서 작성 완료 |
| **SSM Parameters** | ⏳ 0% | **26개 파라미터 등록 필요** |
| **S3 기본 이미지** | ⏳ 0% | **2개 파일 업로드 필요** |
| **ECR Repository** | ⏳ 0% | **Repository 생성 필요** |
| **GitHub Secrets** | ⏳ 0% | **4개 Secret 등록 필요** |
| 배포 테스트 | ⏳ 0% | 위 작업 완료 후 진행 |

---

## 추정 소요 시간

| 작업 | 소요 시간 |
|------|----------|
| SSM Parameters 설정 | 10-15분 |
| S3 기본 이미지 업로드 | 2-3분 |
| ECR Repository 생성 | 1분 |
| GitHub Secrets 설정 | 3-5분 |
| 코드 커밋 & 푸시 | 2분 |
| 배포 & 테스트 | 5-10분 |
| **총 예상 시간** | **약 25-35분** |

---

## 다음 작업 우선순위

1. **Step 1 (가장 중요):** AWS SSM Parameter Store 설정
2. **Step 2:** S3 기본 이미지 업로드
3. **Step 3:** ECR Repository 생성
4. **Step 4:** GitHub Secrets 설정
5. **Step 5:** 코드 커밋 & 푸시
6. **Step 6:** 배포 테스트

**권장:** 1-4단계를 모두 완료한 후 5-6단계 진행

---

## 참고 문서

| 문서 | 경로 |
|------|------|
| SSM Parameter Store 설정 가이드 | `docs/aws-ssm-setup-dev.md` |
| GitHub Secrets 설정 가이드 | `docs/github-secrets-setup-dev.md` |
| 전체 마이그레이션 가이드 | `MIGRATION-GUIDE.md` |
| AWS 환경변수 가이드 | `docs/aws-setup-guide.md` |
| Dev CI/CD 파이프라인 | `.github/workflows/cicd-dev.yml` |
