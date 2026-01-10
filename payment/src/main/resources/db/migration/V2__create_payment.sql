CREATE TABLE payment (
    id VARCHAR(26) PRIMARY KEY,
    payment_key VARCHAR(26) NOT NULL,
    merchant_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    original_amount BIGINT NOT NULL,
    effective_promotions JSONB,

    -- CardPaymentDetails
    card_masked_number VARCHAR(20),
    card_bin VARCHAR(10),
    card_brand VARCHAR(20),
    card_issuer_code VARCHAR(20),
    card_acquirer_code VARCHAR(20),
    card_type VARCHAR(20),
    card_owner_type VARCHAR(20),
    card_issued_country VARCHAR(10),
    card_product_code VARCHAR(20),
    card_expiry_month INT,
    card_expiry_year INT,
    card_holder_name VARCHAR(50),
    installment_months INT,
    installment_interest_free BOOLEAN,

    -- PaymentResult
    approval_number VARCHAR(50),
    approved_at TIMESTAMPTZ,
    error_code VARCHAR(50),
    failure_reason VARCHAR(500),

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_payment_payment_key ON payment(payment_key);
CREATE INDEX idx_payment_merchant_id ON payment(merchant_id);
