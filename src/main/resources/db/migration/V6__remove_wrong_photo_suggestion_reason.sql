-- WRONG_PHOTO 제안 사유 제거 (피그마 디자인 변경에 따라 옵션에서 제외)
-- 1) WRONG_PHOTO만 단독으로 가진 제안 → 의미 없는 빈 reasons가 남지 않도록 제안 자체를 삭제
--    (shop_suggestion_reasons는 ON DELETE CASCADE이므로 자동 정리됨)
DELETE FROM shop_suggestions
WHERE id IN (
    SELECT suggestion_id
    FROM shop_suggestion_reasons
    GROUP BY suggestion_id
    HAVING COUNT(*) = 1 AND MAX(reason) = 'WRONG_PHOTO'
);

-- 2) 다른 사유와 함께 등록된 WRONG_PHOTO 행만 제거
DELETE FROM shop_suggestion_reasons WHERE reason = 'WRONG_PHOTO';
