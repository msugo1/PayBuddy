CREATE TABLE promotion (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    discount_type VARCHAR(20) NOT NULL,
    discount_value BIGINT NOT NULL,
    max_discount_amount BIGINT,

    card_brand VARCHAR(20),
    card_type VARCHAR(20),
    issuer_code VARCHAR(20),
    min_amount BIGINT,

    valid_from TIMESTAMPTZ NOT NULL,
    valid_until TIMESTAMPTZ NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_promotion_valid_period ON promotion(valid_from, valid_until);
CREATE INDEX idx_promotion_card_brand ON promotion(card_brand);
