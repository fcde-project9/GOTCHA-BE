-- Apple 로그인 지원을 위해 oauth_access_token → social_revoke_token 컬럼 이름 변경
-- 기존: Google access_token만 저장
-- 변경: Google access_token + Apple refresh_token 통합 (소셜 연동 해제용)
ALTER TABLE users RENAME COLUMN oauth_access_token TO social_revoke_token;
