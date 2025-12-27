import { describe, it, expect } from 'vitest';
import { cardPaymentSchema } from './schemas';

describe('cardPaymentSchema', () => {
  describe('cardNumber validation', () => {
    it('should accept valid Visa card number', () => {
      const result = cardPaymentSchema.safeParse({
        cardNumber: '4111-1111-1111-1111',
        expiryMonth: '12',
        expiryYear: '25',
        cvc: '123',
        cardHolderName: 'Test User',
        installmentMonths: 0,
      });

      expect(result.success).toBe(true);
    });

    it('should accept valid MasterCard number', () => {
      const result = cardPaymentSchema.safeParse({
        cardNumber: '5555-5555-5555-4444',
        expiryMonth: '12',
        expiryYear: '25',
        cvc: '123',
        cardHolderName: 'Test User',
        installmentMonths: 0,
      });

      expect(result.success).toBe(true);
    });

    it('should reject invalid card number (Luhn check)', () => {
      const result = cardPaymentSchema.safeParse({
        cardNumber: '4111-1111-1111-1112',
        expiryMonth: '12',
        expiryYear: '25',
        cvc: '123',
        cardHolderName: 'Test User',
        installmentMonths: 0,
      });

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].path).toContain('cardNumber');
        expect(result.error.issues[0].message).toContain('유효하지 않은');
      }
    });

    it('should reject empty card number', () => {
      const result = cardPaymentSchema.safeParse({
        cardNumber: '',
        expiryMonth: '12',
        expiryYear: '25',
        cvc: '123',
        cardHolderName: 'Test User',
        installmentMonths: 0,
      });

      expect(result.success).toBe(false);
    });
  });

  describe('expiryMonth validation', () => {
    it('should accept valid months (01-12)', () => {
      for (let month = 1; month <= 12; month++) {
        const monthStr = month.toString().padStart(2, '0');
        const result = cardPaymentSchema.safeParse({
          cardNumber: '4111-1111-1111-1111',
          expiryMonth: monthStr,
          expiryYear: '25',
          cvc: '123',
          cardHolderName: 'Test User',
          installmentMonths: 0,
        });

        expect(result.success).toBe(true);
      }
    });

    it('should reject month 00', () => {
      const result = cardPaymentSchema.safeParse({
        cardNumber: '4111-1111-1111-1111',
        expiryMonth: '00',
        expiryYear: '25',
        cvc: '123',
        cardHolderName: 'Test User',
        installmentMonths: 0,
      });

      expect(result.success).toBe(false);
    });

    it('should reject month 13', () => {
      const result = cardPaymentSchema.safeParse({
        cardNumber: '4111-1111-1111-1111',
        expiryMonth: '13',
        expiryYear: '25',
        cvc: '123',
        cardHolderName: 'Test User',
        installmentMonths: 0,
      });

      expect(result.success).toBe(false);
    });

    it('should reject single digit month without leading zero', () => {
      const result = cardPaymentSchema.safeParse({
        cardNumber: '4111-1111-1111-1111',
        expiryMonth: '1',
        expiryYear: '25',
        cvc: '123',
        cardHolderName: 'Test User',
        installmentMonths: 0,
      });

      expect(result.success).toBe(false);
    });
  });

  describe('expiryYear validation', () => {
    it('should accept current year', () => {
      const currentYear = new Date().getFullYear() % 100;
      const result = cardPaymentSchema.safeParse({
        cardNumber: '4111-1111-1111-1111',
        expiryMonth: '12',
        expiryYear: currentYear.toString().padStart(2, '0'),
        cvc: '123',
        cardHolderName: 'Test User',
        installmentMonths: 0,
      });

      expect(result.success).toBe(true);
    });

    it('should accept future year', () => {
      const futureYear = (new Date().getFullYear() + 5) % 100;
      const result = cardPaymentSchema.safeParse({
        cardNumber: '4111-1111-1111-1111',
        expiryMonth: '12',
        expiryYear: futureYear.toString().padStart(2, '0'),
        cvc: '123',
        cardHolderName: 'Test User',
        installmentMonths: 0,
      });

      expect(result.success).toBe(true);
    });

    it('should reject past year', () => {
      const pastYear = (new Date().getFullYear() - 1) % 100;
      const result = cardPaymentSchema.safeParse({
        cardNumber: '4111-1111-1111-1111',
        expiryMonth: '12',
        expiryYear: pastYear.toString().padStart(2, '0'),
        cvc: '123',
        cardHolderName: 'Test User',
        installmentMonths: 0,
      });

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toContain('만료된');
      }
    });

    it('should reject non-2-digit year', () => {
      const result = cardPaymentSchema.safeParse({
        cardNumber: '4111-1111-1111-1111',
        expiryMonth: '12',
        expiryYear: '2025',
        cvc: '123',
        cardHolderName: 'Test User',
        installmentMonths: 0,
      });

      expect(result.success).toBe(false);
    });
  });

  describe('cvc validation', () => {
    it('should accept 3-digit CVC', () => {
      const result = cardPaymentSchema.safeParse({
        cardNumber: '4111-1111-1111-1111',
        expiryMonth: '12',
        expiryYear: '25',
        cvc: '123',
        cardHolderName: 'Test User',
        installmentMonths: 0,
      });

      expect(result.success).toBe(true);
    });

    it('should accept 4-digit CVC (Amex)', () => {
      const result = cardPaymentSchema.safeParse({
        cardNumber: '3782-8224-6310-005',
        expiryMonth: '12',
        expiryYear: '25',
        cvc: '1234',
        cardHolderName: 'Test User',
        installmentMonths: 0,
      });

      expect(result.success).toBe(true);
    });

    it('should reject 2-digit CVC', () => {
      const result = cardPaymentSchema.safeParse({
        cardNumber: '4111-1111-1111-1111',
        expiryMonth: '12',
        expiryYear: '25',
        cvc: '12',
        cardHolderName: 'Test User',
        installmentMonths: 0,
      });

      expect(result.success).toBe(false);
    });

    it('should reject CVC with letters', () => {
      const result = cardPaymentSchema.safeParse({
        cardNumber: '4111-1111-1111-1111',
        expiryMonth: '12',
        expiryYear: '25',
        cvc: '12a',
        cardHolderName: 'Test User',
        installmentMonths: 0,
      });

      expect(result.success).toBe(false);
    });
  });

  describe('cardHolderName validation', () => {
    it('should accept valid name', () => {
      const result = cardPaymentSchema.safeParse({
        cardNumber: '4111-1111-1111-1111',
        expiryMonth: '12',
        expiryYear: '25',
        cvc: '123',
        cardHolderName: '홍길동',
        installmentMonths: 0,
      });

      expect(result.success).toBe(true);
    });

    it('should accept English name', () => {
      const result = cardPaymentSchema.safeParse({
        cardNumber: '4111-1111-1111-1111',
        expiryMonth: '12',
        expiryYear: '25',
        cvc: '123',
        cardHolderName: 'John Doe',
        installmentMonths: 0,
      });

      expect(result.success).toBe(true);
    });

    it('should reject empty name', () => {
      const result = cardPaymentSchema.safeParse({
        cardNumber: '4111-1111-1111-1111',
        expiryMonth: '12',
        expiryYear: '25',
        cvc: '123',
        cardHolderName: '',
        installmentMonths: 0,
      });

      expect(result.success).toBe(false);
    });

    it('should reject name longer than 50 characters', () => {
      const result = cardPaymentSchema.safeParse({
        cardNumber: '4111-1111-1111-1111',
        expiryMonth: '12',
        expiryYear: '25',
        cvc: '123',
        cardHolderName: 'a'.repeat(51),
        installmentMonths: 0,
      });

      expect(result.success).toBe(false);
    });
  });

  describe('installmentMonths validation', () => {
    it('should accept 0 (일시불)', () => {
      const result = cardPaymentSchema.safeParse({
        cardNumber: '4111-1111-1111-1111',
        expiryMonth: '12',
        expiryYear: '25',
        cvc: '123',
        cardHolderName: 'Test User',
        installmentMonths: 0,
      });

      expect(result.success).toBe(true);
    });

    it('should accept valid installment months (2-36)', () => {
      const validMonths = [2, 3, 6, 12, 24, 36];

      validMonths.forEach((months) => {
        const result = cardPaymentSchema.safeParse({
          cardNumber: '4111-1111-1111-1111',
          expiryMonth: '12',
          expiryYear: '25',
          cvc: '123',
          cardHolderName: 'Test User',
          installmentMonths: months,
        });

        expect(result.success).toBe(true);
      });
    });

    it('should reject negative installment months', () => {
      const result = cardPaymentSchema.safeParse({
        cardNumber: '4111-1111-1111-1111',
        expiryMonth: '12',
        expiryYear: '25',
        cvc: '123',
        cardHolderName: 'Test User',
        installmentMonths: -1,
      });

      expect(result.success).toBe(false);
    });

    it('should reject installment months > 36', () => {
      const result = cardPaymentSchema.safeParse({
        cardNumber: '4111-1111-1111-1111',
        expiryMonth: '12',
        expiryYear: '25',
        cvc: '123',
        cardHolderName: 'Test User',
        installmentMonths: 48,
      });

      expect(result.success).toBe(false);
    });
  });
});
