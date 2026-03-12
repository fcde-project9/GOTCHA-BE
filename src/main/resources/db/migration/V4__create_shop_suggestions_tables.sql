-- 가게 정보 수정 제안 테이블
CREATE TABLE IF NOT EXISTS shop_suggestions (
    id           BIGSERIAL PRIMARY KEY,
    shop_id      BIGINT NOT NULL,
    suggester_id BIGINT NOT NULL,
    created_at   TIMESTAMP NOT NULL,
    updated_at   TIMESTAMP NOT NULL,
    CONSTRAINT fk_shop_suggestions_shop FOREIGN KEY (shop_id) REFERENCES shops(id),
    CONSTRAINT fk_shop_suggestions_suggester FOREIGN KEY (suggester_id) REFERENCES users(id)
);

-- 제안 이유 목록 테이블 (@ElementCollection)
-- suggestion_id ON DELETE CASCADE: JPQL 벌크 삭제 시 DB 레벨에서 자동 정리
CREATE TABLE IF NOT EXISTS shop_suggestion_reasons (
    suggestion_id BIGINT NOT NULL,
    reason        VARCHAR(50) NOT NULL,
    CONSTRAINT fk_suggestion_reasons_suggestion FOREIGN KEY (suggestion_id) REFERENCES shop_suggestions(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_suggestion_reasons_suggestion_id ON shop_suggestion_reasons(suggestion_id);
