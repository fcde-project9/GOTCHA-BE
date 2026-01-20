# GitHub Secrets Setup - Dev Environment

## 개요

Dev 환경 CI/CD를 위해 GitHub Repository Secrets에 등록해야 하는 값 목록입니다.

**작업일:** 2026-01-20
**환경:** Dev
**파일:** `.github/workflows/cicd-dev.yml`

---

## GitHub Secrets 등록 경로

GitHub Repository → Settings → Secrets and variables → Actions → New repository secret

---

## 필수 Secrets 목록

### 1. AWS 공통 (3개)

| Secret 이름 | 값 | 설명 |
|-------------|-----|------|
| `AWS_ACCESS_KEY_ID` | [IAM Access Key] | AWS IAM 사용자 Access Key (prod와 공유) |
| `AWS_SECRET_ACCESS_KEY` | [IAM Secret Key] | AWS IAM 사용자 Secret Key (prod와 공유) |
| `AWS_REGION` | `ap-northeast-2` | AWS 리전 (prod와 공유) |

**⚠️ 주의:** 이미 prod 배포를 위해 등록되어 있다면 추가 등록 불필요

---

### 2. ECR Dev (1개)

| Secret 이름 | 값 | 설명 |
|-------------|-----|------|
| `ECR_REPOSITORY_DEV` | `gotcha-be-dev` | Dev 환경 ECR 리포지토리 이름 |

**ECR Repository 생성 방법:**
```bash
aws ecr create-repository \
  --repository-name gotcha-be-dev \
  --region ap-northeast-2
```

---

### 3. EC2 Dev (3개)

| Secret 이름 | 값 | 설명 |
|-------------|-----|------|
| `EC2_HOST_DEV` | [Dev EC2 Public IP or Domain] | Dev EC2 인스턴스 접속 주소 |
| `EC2_USER_DEV` | `ubuntu` | EC2 SSH 사용자 이름 (기본값: ubuntu) |
| `EC2_SSH_KEY_DEV` | [SSH Private Key] | Dev EC2 SSH Private Key 전체 내용 |

**EC2_HOST_DEV 확인 방법:**
```bash
# AWS Console에서 확인
AWS Console → EC2 → Instances → Dev 인스턴스 선택 → Public IPv4 address

# 또는 AWS CLI로 확인 (Instance ID 필요)
aws ec2 describe-instances \
  --instance-ids i-xxxxxxxxxxxxxxxxx \
  --query "Reservations[0].Instances[0].PublicIpAddress" \
  --output text \
  --region ap-northeast-2
```

**EC2_SSH_KEY_DEV 입력 방법:**
1. Dev EC2 생성 시 다운로드한 `.pem` 파일 열기
2. 파일 전체 내용 복사 (-----BEGIN RSA PRIVATE KEY----- 부터 -----END RSA PRIVATE KEY----- 까지)
3. GitHub Secret에 붙여넣기

```
-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEA...
...
-----END RSA PRIVATE KEY-----
```

---

## 전체 Secrets 목록 (Dev 전용)

Dev 환경을 위해 **새로 추가**해야 하는 Secrets:

| # | Secret 이름 | 값 | Type |
|---|-------------|-----|------|
| 1 | `ECR_REPOSITORY_DEV` | `gotcha-be-dev` | Repository secret |
| 2 | `EC2_HOST_DEV` | [Dev EC2 IP] | Repository secret |
| 3 | `EC2_USER_DEV` | `ubuntu` | Repository secret |
| 4 | `EC2_SSH_KEY_DEV` | [Dev SSH Key] | Repository secret |

**⚠️ 주의:** `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`은 이미 prod를 위해 등록되어 있다면 추가 불필요

---

## Prod Secrets와 비교

### Prod 환경 Secrets (참고용)

| Secret 이름 | Prod 값 | Dev 값 |
|-------------|---------|--------|
| `AWS_ACCESS_KEY_ID` | [공통] | [공통] |
| `AWS_SECRET_ACCESS_KEY` | [공통] | [공통] |
| `AWS_REGION` | [공통] | [공통] |
| `ECR_REPOSITORY` | `gotcha-be-prod` | - |
| `ECR_REPOSITORY_DEV` | - | `gotcha-be-dev` |
| `EC2_HOST` | [Prod IP] | - |
| `EC2_HOST_DEV` | - | [Dev IP] |
| `EC2_USER` | `ubuntu` | - |
| `EC2_USER_DEV` | - | `ubuntu` |
| `EC2_SSH_KEY` | [Prod Key] | - |
| `EC2_SSH_KEY_DEV` | - | [Dev Key] |

---

## 등록 후 확인 방법

### GitHub UI에서 확인
GitHub Repository → Settings → Secrets and variables → Actions

다음 Secrets이 보여야 합니다:
- ✅ AWS_ACCESS_KEY_ID
- ✅ AWS_SECRET_ACCESS_KEY
- ✅ AWS_REGION
- ✅ ECR_REPOSITORY (prod용)
- ✅ ECR_REPOSITORY_DEV
- ✅ EC2_HOST (prod용)
- ✅ EC2_HOST_DEV
- ✅ EC2_USER (prod용)
- ✅ EC2_USER_DEV
- ✅ EC2_SSH_KEY (prod용)
- ✅ EC2_SSH_KEY_DEV

---

## GitHub Actions에서 사용 방법

`.github/workflows/cicd-dev.yml`에서 사용 예시:

```yaml
env:
  ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
  ECR_REPOSITORY: ${{ secrets.ECR_REPOSITORY_DEV }}
  IMAGE_TAG: ${{ github.sha }}

run: |
  echo "${{ secrets.EC2_SSH_KEY_DEV }}" > private_key.pem
  chmod 600 private_key.pem

  ssh -o StrictHostKeyChecking=no -i private_key.pem ${{ secrets.EC2_USER_DEV }}@${{ secrets.EC2_HOST_DEV }} << 'EOF'
    # 배포 스크립트
  EOF
```

---

## 트러블슈팅

### Q. SSH 연결 실패 (Permission denied)
**A.** EC2_SSH_KEY_DEV에 올바른 Private Key가 등록되었는지 확인.
- Key 파일 전체 내용 (-----BEGIN부터 -----END까지) 복사했는지 확인
- Key 파일이 Dev EC2 인스턴스용인지 확인

### Q. ECR push 실패 (repository does not exist)
**A.** ECR Repository가 먼저 생성되었는지 확인.
```bash
aws ecr describe-repositories \
  --repository-names gotcha-be-dev \
  --region ap-northeast-2
```

### Q. EC2 연결 실패 (Connection timed out)
**A.** EC2_HOST_DEV 값 확인 및 Security Group 설정 확인.
- EC2 인스턴스가 실행 중인지 확인
- Security Group에서 GitHub Actions IP 허용 (또는 0.0.0.0/0 SSH 허용)

---

## 다음 단계

GitHub Secrets 등록 완료 후:

1. **Dev 브랜치에 푸시하여 배포 테스트**
   ```bash
   git push origin dev
   ```

2. **GitHub Actions 로그 확인**
   - GitHub Repository → Actions → 최신 워크플로우 확인
   - 각 Step별 로그 확인

3. **배포 확인**
   ```bash
   # EC2 인스턴스 접속
   ssh -i [dev-key.pem] ubuntu@[EC2_HOST_DEV]

   # Docker 컨테이너 확인
   docker ps

   # 로그 확인
   docker logs gotcha-be-dev
   ```

---

## 체크리스트

### SSM Parameter Store
- [ ] 모든 `/gotcha/dev/*` 파라미터 등록 완료 (총 26개)
- [ ] 파라미터 확인: `aws ssm get-parameters-by-path --path "/gotcha/dev" --recursive`

### ECR Repository
- [ ] ECR Repository 생성: `gotcha-be-dev`
- [ ] 생성 확인: `aws ecr describe-repositories --repository-names gotcha-be-dev`

### GitHub Secrets
- [ ] ECR_REPOSITORY_DEV 등록
- [ ] EC2_HOST_DEV 등록
- [ ] EC2_USER_DEV 등록
- [ ] EC2_SSH_KEY_DEV 등록

### 배포 테스트
- [ ] Dev 브랜치 푸시
- [ ] GitHub Actions 워크플로우 성공
- [ ] EC2 컨테이너 실행 확인
- [ ] API 정상 동작 확인

---

## 참고

- GitHub Actions Secrets 문서: https://docs.github.com/en/actions/security-guides/using-secrets-in-github-actions
- AWS ECR 문서: https://docs.aws.amazon.com/ecr/
- AWS SSM Parameter Store 문서: https://docs.aws.amazon.com/systems-manager/latest/userguide/systems-manager-parameter-store.html
