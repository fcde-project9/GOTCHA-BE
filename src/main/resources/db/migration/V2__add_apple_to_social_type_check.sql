-- Apple 소셜 로그인 지원을 위해 social_type CHECK 제약 조건에 APPLE 추가
ALTER TABLE users DROP CONSTRAINT users_social_type_check;
ALTER TABLE users ADD CONSTRAINT users_social_type_check
    CHECK (social_type IN ('KAKAO', 'GOOGLE', 'NAVER', 'APPLE'));
