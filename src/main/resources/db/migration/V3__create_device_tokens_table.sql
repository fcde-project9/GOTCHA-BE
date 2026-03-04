-- 네이티브 푸시(APNS/FCM) 디바이스 토큰 저장 테이블
CREATE TABLE device_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_token VARCHAR(200) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_device_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_device_tokens_device_token UNIQUE (device_token),
    CONSTRAINT ck_device_tokens_platform CHECK (platform IN ('IOS', 'ANDROID'))
);

-- 사용자별 토큰 조회 성능 향상을 위한 인덱스
CREATE INDEX idx_device_tokens_user_id ON device_tokens(user_id);

-- 플랫폼별 조회를 위한 인덱스
CREATE INDEX idx_device_tokens_platform ON device_tokens(platform);
