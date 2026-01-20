-- ========================================
-- S3 폴더 구조 변경: Dev Database (gotcha_dev)
-- ========================================
-- 목적: S3 URL 경로에 dev/ prefix 추가
-- 실행 환경: DBeaver에서 gotcha_dev database 연결 후 실행
-- 작업일: 2026-01-20
-- ========================================

-- ========================================
-- 1단계: 현재 상태 확인
-- ========================================
-- 변경 대상 데이터 확인
SELECT
    'users' AS table_name,
    COUNT(*) AS total_count,
    COUNT(CASE WHEN profile_image_url LIKE '%/profiles/%' THEN 1 END) AS profiles_count,
    COUNT(CASE WHEN profile_image_url LIKE '%/defaults/%' THEN 1 END) AS defaults_count
FROM users
WHERE profile_image_url IS NOT NULL

UNION ALL

SELECT
    'shops' AS table_name,
    COUNT(*) AS total_count,
    COUNT(CASE WHEN main_image_url LIKE '%/shops/%' THEN 1 END) AS shops_count,
    COUNT(CASE WHEN main_image_url LIKE '%/defaults/%' THEN 1 END) AS defaults_count
FROM shops
WHERE main_image_url IS NOT NULL

UNION ALL

SELECT
    'review_images' AS table_name,
    COUNT(*) AS total_count,
    COUNT(CASE WHEN image_url LIKE '%/reviews/%' THEN 1 END) AS reviews_count,
    0 AS unused
FROM review_images
WHERE image_url IS NOT NULL;

-- ========================================
-- 2단계: 변경 사항 미리보기
-- ========================================
-- users 테이블
SELECT
    id,
    nickname,
    profile_image_url AS old_url,
    REPLACE(REPLACE(profile_image_url, '/profiles/', '/dev/profiles/'), '/defaults/', '/dev/defaults/') AS new_url
FROM users
WHERE profile_image_url IS NOT NULL
  AND (profile_image_url LIKE '%/profiles/%' OR profile_image_url LIKE '%/defaults/%')
ORDER BY id;

-- shops 테이블
SELECT
    id,
    name,
    main_image_url AS old_url,
    REPLACE(REPLACE(main_image_url, '/shops/', '/dev/shops/'), '/defaults/', '/dev/defaults/') AS new_url
FROM shops
WHERE main_image_url IS NOT NULL
  AND (main_image_url LIKE '%/shops/%' OR main_image_url LIKE '%/defaults/%')
ORDER BY id;

-- review_images 테이블
SELECT
    id,
    review_id,
    image_url AS old_url,
    REPLACE(image_url, '/reviews/', '/dev/reviews/') AS new_url
FROM review_images
WHERE image_url IS NOT NULL
  AND image_url LIKE '%/reviews/%'
ORDER BY id;

-- ========================================
-- 3단계: 실제 업데이트 (트랜잭션)
-- ========================================
-- ⚠️ 주의: 위의 미리보기를 확인한 후 실행하세요!
-- ⚠️ 문제가 있으면 ROLLBACK; 을 실행하세요!

BEGIN;

-- users 테이블 업데이트
UPDATE users
SET profile_image_url = REPLACE(profile_image_url, '/profiles/', '/dev/profiles/')
WHERE profile_image_url LIKE '%/profiles/%';

UPDATE users
SET profile_image_url = REPLACE(profile_image_url, '/defaults/', '/dev/defaults/')
WHERE profile_image_url LIKE '%/defaults/%';

-- shops 테이블 업데이트
UPDATE shops
SET main_image_url = REPLACE(main_image_url, '/shops/', '/dev/shops/')
WHERE main_image_url LIKE '%/shops/%';

UPDATE shops
SET main_image_url = REPLACE(main_image_url, '/defaults/', '/dev/defaults/')
WHERE main_image_url LIKE '%/defaults/%';

-- review_images 테이블 업데이트
UPDATE review_images
SET image_url = REPLACE(image_url, '/reviews/', '/dev/reviews/')
WHERE image_url LIKE '%/reviews/%';

-- 변경 사항 확인
SELECT 'Updated users:' AS info, COUNT(*) AS count FROM users WHERE profile_image_url LIKE '%/dev/%'
UNION ALL
SELECT 'Updated shops:' AS info, COUNT(*) AS count FROM shops WHERE main_image_url LIKE '%/dev/%'
UNION ALL
SELECT 'Updated review_images:' AS info, COUNT(*) AS count FROM review_images WHERE image_url LIKE '%/dev/%';

-- ⚠️ 결과가 올바르면 COMMIT; 실행
-- ⚠️ 문제가 있으면 ROLLBACK; 실행
-- COMMIT;
-- ROLLBACK;

-- ========================================
-- 4단계: 업데이트 후 최종 확인
-- ========================================
-- (COMMIT 후 실행)
SELECT
    id,
    nickname,
    profile_image_url
FROM users
WHERE profile_image_url IS NOT NULL
ORDER BY id;

SELECT
    id,
    name,
    main_image_url
FROM shops
WHERE main_image_url IS NOT NULL
ORDER BY id;

SELECT
    id,
    review_id,
    image_url
FROM review_images
ORDER BY id;
