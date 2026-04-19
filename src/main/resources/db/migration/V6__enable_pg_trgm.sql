CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_shops_name_trgm ON shops USING gin (name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_shops_address_name_trgm ON shops USING gin (address_name gin_trgm_ops);
