CREATE TABLE IF NOT EXISTS inventory (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id  UUID NOT NULL UNIQUE,
    quantity    INT NOT NULL CHECK (quantity >= 0),
    min_stock   INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version     BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS purchase_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id      UUID NOT NULL,
    quantity        INT NOT NULL,
    unit_price      NUMERIC(12,2) NOT NULL,
    total_amount    NUMERIC(12,2) NOT NULL,
    correlation_id  VARCHAR(100),
    purchased_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS outbox_events (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id VARCHAR(100) NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload      TEXT NOT NULL,
    published    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_inventory_product    ON inventory(product_id);
CREATE INDEX IF NOT EXISTS idx_purchase_product     ON purchase_history(product_id);
CREATE INDEX IF NOT EXISTS idx_purchase_date        ON purchase_history(purchased_at DESC);
CREATE INDEX IF NOT EXISTS idx_outbox_unpublished   ON outbox_events(published) WHERE published = FALSE;
