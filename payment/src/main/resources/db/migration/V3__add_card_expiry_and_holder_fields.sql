ALTER TABLE payment ADD COLUMN card_expiry_month INT;
ALTER TABLE payment ADD COLUMN card_expiry_year INT;
ALTER TABLE payment ADD COLUMN card_holder_name VARCHAR(50);
