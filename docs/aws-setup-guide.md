# AWS Production 환경 설정 가이드

이 문서는 GOTCHA Backend를 AWS에 배포하기 위한 설정 가이드입니다.

## 목차
1. [AWS 리소스 설정](#aws-리소스-설정)
2. [GitHub Secrets 설정](#github-secrets-설정)
3. [EC2 초기 설정](#ec2-초기-설정)
4. [DNS 설정](#dns-설정)
5. [배포 프로세스](#배포-프로세스)

---

## AWS 리소스 설정

### 1. ECR (Elastic Container Registry)

Docker 이미지를 저장할 ECR 리포지토리를 생성합니다.

```bash
# ECR 리포지토리 생성
aws ecr create-repository \
  --repository-name gotcha-be-prod \
  --region ap-northeast-2
```

**생성 후 확인사항:**
- Repository URI: `{AWS_ACCOUNT_ID}.dkr.ecr.ap-northeast-2.amazonaws.com/gotcha-be-prod`
- 이 URI를 GitHub Secrets의 `ECR_REPOSITORY`에 입력

### 2. RDS (PostgreSQL)

#### 2.1. RDS 인스턴스 생성
- Engine: PostgreSQL (최신 버전 권장)
- Instance class: `db.t3.micro` (프리티어) 또는 `db.t4g.micro`
- Storage: 20GB (프리티어)
- Multi-AZ: 비활성화 (비용 절감)
- Public access: No
- VPC: EC2와 같은 VPC
- Database name: `gotcha_prod`

#### 2.2. 보안 그룹 설정

**중요**: EC2와 RDS를 같은 VPC (기본 VPC 사용)에 생성하고, 보안 그룹으로 접근을 제어합니다.

##### 단계 1: EC2 보안 그룹 ID 확인

AWS Console > EC2 > 인스턴스 선택 > 보안 탭에서 보안 그룹 ID 확인
예: `sg-0123456789abcdef0`

##### 단계 2: RDS 보안 그룹 인바운드 규칙 추가

**AWS Console 사용:**
1. RDS Console > 데이터베이스 선택
2. "연결 및 보안" 탭 > VPC 보안 그룹 클릭
3. "인바운드 규칙" 탭 > "인바운드 규칙 편집"
4. "규칙 추가" 클릭:
   - **유형**: PostgreSQL
   - **프로토콜**: TCP
   - **포트 범위**: 5432
   - **소스**: Custom → EC2 보안 그룹 ID 입력 (예: `sg-0123456789abcdef0`)
   - **설명**: Allow from EC2
5. "규칙 저장"

**AWS CLI 사용:**
```bash
# RDS 보안 그룹에 EC2 접근 허용 규칙 추가
aws ec2 authorize-security-group-ingress \
  --group-id <RDS_보안그룹_ID> \
  --protocol tcp \
  --port 5432 \
  --source-group <EC2_보안그룹_ID> \
  --region ap-northeast-2
```

##### 단계 3: 연결 테스트 (EC2에서)

```bash
# EC2에 SSH 접속
ssh -i your-key.pem ec2-user@<EC2_IP>

# PostgreSQL 클라이언트 설치
sudo yum install postgresql15 -y  # Amazon Linux
# 또는
sudo apt install postgresql-client -y  # Ubuntu

# RDS 연결 테스트
psql -h <RDS_엔드포인트> -U postgres -d gotcha_prod
```

**성공 시**: `gotcha_prod=>` 프롬프트 표시
**실패 시**: 아래 트러블슈팅 참고

##### 트러블슈팅

| 증상 | 원인 | 해결 |
|------|------|------|
| `Connection timed out` | 보안 그룹 규칙 미설정 | RDS 보안 그룹에 EC2 보안 그룹 ID 추가 |
| `Connection refused` | RDS 실행 안 됨 | RDS 인스턴스 상태 확인 |
| `password authentication failed` | 잘못된 비밀번호 | RDS 마스터 비밀번호 확인 |
| `could not translate host name` | 잘못된 엔드포인트 | RDS 엔드포인트 주소 재확인 |

#### 2.3. 연결 정보
```
DB_URL_PROD=jdbc:postgresql://{RDS_ENDPOINT}:5432/gotcha_prod
DB_USERNAME_PROD={마스터 사용자명}
DB_PASSWORD_PROD={마스터 비밀번호}
```

### 3. S3 (이미지 저장소)

#### 3.1. S3 버킷 생성
```bash
aws s3api create-bucket \
  --bucket gotcha-prod-files \
  --region ap-northeast-2 \
  --create-bucket-configuration LocationConstraint=ap-northeast-2
```

#### 3.2. CORS 설정
버킷에 CORS 정책 추가 (프론트엔드에서 이미지 업로드용)

```json
[
    {
        "AllowedHeaders": ["*"],
        "AllowedMethods": ["GET", "PUT", "POST", "DELETE"],
        "AllowedOrigins": ["https://yourdomain.com"],
        "ExposeHeaders": ["ETag"]
    }
]
```

#### 3.3. 퍼블릭 액세스 설정
- 이미지 조회를 위해 GetObject는 퍼블릭 허용 필요
- Bucket Policy 예시:
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "PublicReadGetObject",
            "Effect": "Allow",
            "Principal": "*",
            "Action": "s3:GetObject",
            "Resource": "arn:aws:s3:::gotcha-prod-files/*"
        }
    ]
}
```

#### 3.4. Default 이미지 업로드 (필수)

애플리케이션에서 사용할 기본 프로필 이미지를 S3에 업로드해야 합니다.

**⚠️ 중요:** `defaults` 폴더는 애플리케이션 API를 통한 업로드가 차단되어 있습니다. 보안상의 이유로 **관리자가 AWS Console 또는 AWS CLI를 통해서만** 업로드할 수 있습니다.

**업로드할 파일:**
- `defaults/profile-default-join.png` - 회원가입 시 기본 프로필 이미지

**AWS Console에서 업로드:**
1. S3 Console > gotcha-prod-files 버킷 선택
2. "폴더 생성" 클릭 → 이름: `defaults`
3. `defaults` 폴더 들어가기
4. "업로드" 클릭 → 기본 프로필 이미지 파일 업로드
5. 업로드 완료 후 파일 선택 → 객체 URL 복사
   - 예: `https://gotcha-prod-files.s3.ap-northeast-2.amazonaws.com/defaults/profile-default-join.png`
6. 이 URL을 GitHub Secrets에 `USER_DEFAULT_PROFILE_IMAGE_URL_PROD`로 등록

**AWS CLI로 업로드:**
```bash
# 기본 프로필 이미지 업로드
aws s3 cp profile-default-join.png s3://gotcha-prod-files/defaults/profile-default-join.png \
  --acl public-read \
  --region ap-northeast-2

# 업로드된 파일 URL
echo "https://gotcha-prod-files.s3.ap-northeast-2.amazonaws.com/defaults/profile-default-join.png"
```

**S3 폴더 구조:**
```
gotcha-prod-files/
├── prod/
│   ├── defaults/
│   │   ├── profile-default-join.png   (관리자만 AWS Console/CLI로 수동 업로드)
│   │   └── shop-default.png
│   ├── profiles/      (사용자가 API를 통해 업로드, 자동 생성)
│   ├── reviews/       (사용자가 API를 통해 업로드, 자동 생성)
│   └── shops/         (사용자가 API를 통해 업로드, 자동 생성)
└── dev/
    ├── defaults/
    ├── profiles/
    ├── reviews/
    └── shops/
```

**허용된 업로드 폴더 (API):**
- `profiles`, `reviews`, `shops`만 애플리케이션 API(`/api/files/upload`)를 통해 업로드 가능
- `defaults`는 보안상 API 접근 차단됨

### 4. EC2 인스턴스

#### 4.1. EC2 인스턴스 생성
- AMI: Amazon Linux 2023 또는 Ubuntu 22.04 LTS
- Instance type: `t3.micro` (프리티어) 또는 `t3.small`
- Storage: 20GB gp3 (프리티어)
- Security Group:
  - SSH (22): 본인 IP
  - HTTP (80): 0.0.0.0/0
  - HTTPS (443): 0.0.0.0/0
  - Custom TCP (8080): 0.0.0.0/0 (또는 ALB 사용 시 ALB 보안 그룹)

#### 4.2. Key Pair
- 새로운 Key Pair 생성 또는 기존 사용
- `.pem` 파일 다운로드 후 GitHub Secrets에 등록

---

## GitHub Secrets 설정

GitHub 리포지토리의 Settings > Secrets and variables > Actions에서 다음 Secrets를 추가합니다.

### AWS 관련 Secrets

| Secret Name | 설명 | 예시 |
|-------------|------|------|
| `AWS_ACCESS_KEY_ID` | AWS IAM 액세스 키 | `AKIAIOSFODNN7EXAMPLE` |
| `AWS_SECRET_ACCESS_KEY` | AWS IAM 시크릿 키 | `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY` |
| `AWS_REGION` | AWS 리전 | `ap-northeast-2` |
| `ECR_REPOSITORY` | ECR 리포지토리 이름 | `gotcha-be-prod` |
| `AWS_S3_BUCKET_NAME` | S3 버킷 이름 | `gotcha-prod-files` |
| `AWS_S3_PREFIX` | S3 폴더 prefix (환경별 구분용) | `prod/` (prod) / `dev/` (dev) |
| `AWS_S3_ACCESS_KEY_ID` | S3 전용 IAM 액세스 키 (선택) | `AKIAIOSFODNN7EXAMPLE` |
| `AWS_S3_SECRET_ACCESS_KEY` | S3 전용 IAM 시크릿 키 (선택) | `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY` |

### EC2 SSH 관련 Secrets

| Secret Name | 설명 | 예시 |
|-------------|------|------|
| `EC2_HOST` | EC2 퍼블릭 IP 또는 도메인 | `ec2-3-34-xxx-xxx.ap-northeast-2.compute.amazonaws.com` |
| `EC2_USER` | EC2 사용자명 | `ec2-user` (Amazon Linux) / `ubuntu` (Ubuntu) |
| `EC2_SSH_KEY` | EC2 SSH Private Key (.pem 파일 내용 전체) | `-----BEGIN RSA PRIVATE KEY-----\n...` |

### 데이터베이스 관련 Secrets

| Secret Name | 설명 | 예시 |
|-------------|------|------|
| `DB_URL_PROD` | RDS PostgreSQL JDBC URL | `jdbc:postgresql://gotcha-prod.xxx.ap-northeast-2.rds.amazonaws.com:5432/gotcha_prod` |
| `DB_USERNAME_PROD` | RDS 사용자명 | `postgres` |
| `DB_PASSWORD_PROD` | RDS 비밀번호 | `your-secure-password` |

### 애플리케이션 관련 Secrets

| Secret Name | 설명 | 예시 |
|-------------|------|------|
| `CORS_ALLOWED_ORIGINS_PROD` | CORS 허용 도메인 (쉼표 구분) | `https://gotcha.com,https://www.gotcha.com` |
| `JWT_SECRET_PROD` | JWT 서명 키 (최소 256비트) | `your-very-secure-jwt-secret-key-here` |
| `JWT_ACCESS_TOKEN_VALIDITY` | 액세스 토큰 만료 시간 (ms) | `3600000` (1시간) |
| `JWT_REFRESH_TOKEN_VALIDITY` | 리프레시 토큰 만료 시간 (ms) | `1209600000` (14일) |
| `OAUTH2_REDIRECT_URI_PROD` | OAuth2 리다이렉트 URI | `https://gotcha.com/oauth/callback` |
| `OAUTH2_ALLOWED_REDIRECT_URIS_PROD` | OAuth2 허용 리다이렉트 URI (쉼표 구분) | `https://gotcha.com/oauth/callback` |
| `USER_DEFAULT_PROFILE_IMAGE_URL_PROD` | 기본 프로필 이미지 URL | `https://gotcha-prod-files.s3.ap-northeast-2.amazonaws.com/defaults/profile-default.png` |

### OAuth2 Provider Secrets (Dev와 공통)

| Secret Name | 설명 |
|-------------|------|
| `KAKAO_CLIENT_ID` | 카카오 REST API 키 |
| `KAKAO_CLIENT_SECRET` | 카카오 Client Secret |
| `KAKAO_REST_API_KEY` | 카카오 지도 API 키 |
| `KAKAO_API_BASE_URL` | 카카오 API Base URL |
| `GOOGLE_CLIENT_ID` | 구글 OAuth2 Client ID |
| `GOOGLE_CLIENT_SECRET` | 구글 OAuth2 Client Secret |
| `NAVER_CLIENT_ID` | 네이버 OAuth2 Client ID |
| `NAVER_CLIENT_SECRET` | 네이버 OAuth2 Client Secret |

---

## EC2 초기 설정

EC2 인스턴스 생성 후 최초 1회 실행해야 하는 설정입니다.

### 1. SSH 접속

```bash
# 로컬에서 EC2 접속
ssh -i your-key.pem ec2-user@{EC2_PUBLIC_IP}
```

### 2. Docker 설치

#### Amazon Linux 2023
```bash
sudo yum update -y
sudo yum install docker -y
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user
```

#### Ubuntu 22.04
```bash
sudo apt update
sudo apt install docker.io -y
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ubuntu
```

**재로그인 필요**: `exit` 후 다시 SSH 접속

### 3. AWS CLI 설치 및 설정

```bash
# AWS CLI v2 설치 (Amazon Linux는 기본 설치됨)
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# AWS 자격증명 설정
aws configure
# AWS Access Key ID: (GitHub Actions가 ECR 접근하는 키와 동일하게)
# AWS Secret Access Key: (GitHub Actions가 ECR 접근하는 키와 동일하게)
# Default region name: ap-northeast-2
# Default output format: json
```

### 4. ECR 로그인 테스트

```bash
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin {AWS_ACCOUNT_ID}.dkr.ecr.ap-northeast-2.amazonaws.com
```

### 5. 방화벽 설정 (선택)

```bash
# Amazon Linux
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
```

---

## DNS 설정

### 1. 도메인 구매
- Route 53 또는 외부 도메인 제공자에서 도메인 구매

### 2. Route 53 설정 (Route 53 사용 시)

#### A 레코드 생성
```
Name: gotcha.com (또는 api.gotcha.com)
Type: A
Value: {EC2_ELASTIC_IP}
TTL: 300
```

#### CNAME 레코드 생성 (선택)
```
Name: www.gotcha.com
Type: CNAME
Value: gotcha.com
TTL: 300
```

### 3. SSL 인증서 설정

#### 옵션 A: ALB + ACM 사용 (권장)
1. AWS Certificate Manager(ACM)에서 SSL 인증서 발급
2. Application Load Balancer 생성
3. ALB에 SSL 인증서 적용
4. ALB Target Group에 EC2 인스턴스 등록

#### 옵션 B: Let's Encrypt (무료)
```bash
# Nginx 설치
sudo yum install nginx -y  # Amazon Linux
sudo apt install nginx -y  # Ubuntu

# Certbot 설치
sudo yum install certbot python3-certbot-nginx -y  # Amazon Linux
sudo apt install certbot python3-certbot-nginx -y  # Ubuntu

# SSL 인증서 발급
sudo certbot --nginx -d gotcha.com -d www.gotcha.com

# 자동 갱신 설정
sudo crontab -e
# 추가: 0 0 1 * * certbot renew --quiet
```

### 4. Nginx 리버스 프록시 설정 (옵션 B 사용 시)

`/etc/nginx/conf.d/gotcha.conf` 생성:
```nginx
server {
    listen 80;
    server_name gotcha.com www.gotcha.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name gotcha.com www.gotcha.com;

    # 파일 업로드 크기 제한 (50MB) - 프로필 사진 등 대용량 이미지 지원
    client_max_body_size 50M;

    ssl_certificate /etc/letsencrypt/live/gotcha.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/gotcha.com/privkey.pem;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
sudo systemctl restart nginx
```

---

## 배포 프로세스

### 자동 배포 (GitHub Actions)

`main` 브랜치에 push하면 자동으로 배포됩니다.

```bash
# 로컬에서 main 브랜치로 머지
git checkout main
git merge dev
git push origin main
```

### 배포 과정
1. GitHub Actions가 트리거됨
2. Docker 이미지 빌드
3. ECR에 이미지 푸시
4. EC2에 SSH 접속
5. ECR에서 최신 이미지 pull
6. 기존 컨테이너 중지 및 제거
7. 새 컨테이너 실행

### 수동 배포 (EC2에서 직접)

```bash
# EC2에 SSH 접속
ssh -i your-key.pem ec2-user@{EC2_HOST}

# ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin {AWS_ACCOUNT_ID}.dkr.ecr.ap-northeast-2.amazonaws.com

# 최신 이미지 pull
docker pull {AWS_ACCOUNT_ID}.dkr.ecr.ap-northeast-2.amazonaws.com/gotcha-be-prod:latest

# 기존 컨테이너 중지 및 제거
docker stop gotcha-be-prod
docker rm gotcha-be-prod

# 새 컨테이너 실행
docker run -d \
  --name gotcha-be-prod \
  -p 8080:8080 \
  --restart unless-stopped \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://..." \
  -e SPRING_DATASOURCE_USERNAME="..." \
  -e SPRING_DATASOURCE_PASSWORD="..." \
  # ... (다른 환경변수들)
  {AWS_ACCOUNT_ID}.dkr.ecr.ap-northeast-2.amazonaws.com/gotcha-be-prod:latest
```

### 로그 확인

```bash
# 컨테이너 로그 실시간 확인
docker logs -f gotcha-be-prod

# 최근 100줄 확인
docker logs --tail 100 gotcha-be-prod
```

### 롤백

```bash
# 특정 버전으로 롤백
docker pull {AWS_ACCOUNT_ID}.dkr.ecr.ap-northeast-2.amazonaws.com/gotcha-be-prod:{COMMIT_SHA}
docker stop gotcha-be-prod
docker rm gotcha-be-prod
docker run -d --name gotcha-be-prod ... {AWS_ACCOUNT_ID}.dkr.ecr.ap-northeast-2.amazonaws.com/gotcha-be-prod:{COMMIT_SHA}
```

---

## 모니터링 및 유지보수

### 헬스체크

```bash
curl http://localhost:8080/actuator/health
# 또는
curl https://gotcha.com/actuator/health
```

### 디스크 용량 확인

```bash
df -h
docker system df
```

### 불필요한 이미지 정리

```bash
docker image prune -a -f
```

### 데이터베이스 백업

```bash
# RDS 자동 백업 활성화 (AWS Console)
# 또는 수동 스냅샷 생성
aws rds create-db-snapshot \
  --db-instance-identifier gotcha-prod \
  --db-snapshot-identifier gotcha-prod-snapshot-$(date +%Y%m%d)
```

---

## 트러블슈팅

### 1. SSH 접속 안 됨
- EC2 보안 그룹에서 22번 포트 인바운드 규칙 확인
- EC2 인스턴스가 실행 중인지 확인
- SSH 키 권한 확인: `chmod 600 your-key.pem`

### 2. Docker 이미지 pull 실패
- EC2에서 AWS CLI 자격증명 확인: `aws configure list`
- ECR 로그인 재시도
- IAM 권한 확인 (ECR 읽기 권한 필요)

### 3. 컨테이너 실행 안 됨
- 환경변수 확인: `docker logs gotcha-be-prod`
- 포트 충돌 확인: `sudo netstat -tulpn | grep 8080`
- RDS 연결 확인: EC2에서 RDS 엔드포인트로 telnet 테스트

### 4. 502 Bad Gateway (Nginx 사용 시)
- 백엔드 서버 상태 확인: `curl http://localhost:8080/actuator/health`
- Nginx 설정 확인: `sudo nginx -t`
- Nginx 재시작: `sudo systemctl restart nginx`

---

## 비용 예상 (프리티어 이후)

| 서비스 | 스펙 | 월 예상 비용 |
|--------|------|-------------|
| EC2 | t3.micro (750시간) | $7-10 |
| RDS | db.t3.micro (750시간) | $15-20 |
| S3 | 10GB 저장, 1000 요청/월 | $1-2 |
| ECR | 500MB 저장 | $0.5 |
| 데이터 전송 | 10GB/월 | $1-2 |
| **총합** | | **$25-35/월** |

프리티어 기간(12개월): 대부분 무료

---

## 참고 자료

- [AWS ECR 공식 문서](https://docs.aws.amazon.com/ecr/)
- [AWS RDS PostgreSQL](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_PostgreSQL.html)
- [AWS S3 CORS 설정](https://docs.aws.amazon.com/AmazonS3/latest/userguide/cors.html)
- [GitHub Actions - AWS 배포](https://github.com/aws-actions)
- [Let's Encrypt](https://letsencrypt.org/)
