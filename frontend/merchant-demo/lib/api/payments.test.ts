import { describe, it, expect } from 'vitest';
import { readyPayment, generateIdempotencyKey, calculateVAT } from './payments';

describe('readyPayment', () => {
  it('should successfully call ready API and return payment key', async () => {
    const request = {
      order_id: 'order-123',
      total_amount: 10000,
    };
    const idempotencyKey = generateIdempotencyKey();

    const response = await readyPayment(request, idempotencyKey);

    expect(response).toHaveProperty('payment_key');
    expect(response).toHaveProperty('checkout_url');
    expect(response).toHaveProperty('expires_at');
    expect(response.order_id).toBe('order-123');
    expect(response.total_amount).toBe(10000);
    expect(response.payment_key).toMatch(/^pay_/);
  });

  it('should include Idempotency-Key header', async () => {
    const request = {
      order_id: 'order-456',
      total_amount: 20000,
    };
    const idempotencyKey = 'idem_test_12345';

    const response = await readyPayment(request, idempotencyKey);

    // If this succeeds, the header was properly sent (MSW handler expects it)
    expect(response).toBeDefined();
  });
});

describe('generateIdempotencyKey', () => {
  it('should generate unique keys with idem_ prefix', () => {
    const key1 = generateIdempotencyKey();
    const key2 = generateIdempotencyKey();

    expect(key1).toMatch(/^idem_/);
    expect(key2).toMatch(/^idem_/);
    expect(key1).not.toBe(key2);
  });

  it('should include timestamp', () => {
    const key = generateIdempotencyKey();
    const parts = key.split('_');

    expect(parts.length).toBeGreaterThanOrEqual(2);
    expect(Number.isNaN(Number(parts[1]))).toBe(false);
  });
});

describe('calculateVAT', () => {
  it('should calculate VAT correctly for standard amount', () => {
    const result = calculateVAT(11000);

    expect(result.vatAmount).toBe(1000); // 11000 / 11 = 1000
    expect(result.supplyAmount).toBe(10000); // 11000 - 1000 = 10000
  });

  it('should calculate VAT correctly for 10000 won', () => {
    const result = calculateVAT(10000);

    expect(result.vatAmount).toBe(909); // Math.floor(10000 / 11)
    expect(result.supplyAmount).toBe(9091); // 10000 - 909
  });

  it('should handle edge case with 0', () => {
    const result = calculateVAT(0);

    expect(result.vatAmount).toBe(0);
    expect(result.supplyAmount).toBe(0);
  });

  it('should floor VAT amount', () => {
    const result = calculateVAT(100);

    expect(result.vatAmount).toBe(9); // Math.floor(100 / 11)
    expect(result.supplyAmount).toBe(91);
  });

  it('should maintain total amount relationship', () => {
    const totalAmount = 50000;
    const result = calculateVAT(totalAmount);

    expect(result.vatAmount + result.supplyAmount).toBe(totalAmount);
  });
});
