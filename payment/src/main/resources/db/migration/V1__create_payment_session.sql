CREATE TABLE payment_session (
    id VARCHAR(26) PRIMARY KEY,
    merchant_id VARCHAR(14) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    order_line JSONB NOT NULL,
    total BIGINT NOT NULL,
    supply BIGINT NOT NULL,
    vat BIGINT NOT NULL,
    success VARCHAR(128) NOT NULL,
    fail VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expired BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_ongoing_payment_session
ON payment_session (merchant_id, order_id)
WHERE expired = FALSE;
