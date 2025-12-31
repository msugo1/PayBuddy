import { describe, it, expect } from 'vitest';
import { isValidCardNumber, formatCardNumber, cleanCardNumber } from './card-validator';

describe('isValidCardNumber', () => {
  describe('Valid Card Numbers', () => {
    it('should validate Visa card (4111111111111111)', () => {
      expect(isValidCardNumber('4111111111111111')).toBe(true);
    });

    it('should validate MasterCard (5555555555554444)', () => {
      expect(isValidCardNumber('5555555555554444')).toBe(true);
    });

    it('should validate Amex card (378282246310005)', () => {
      expect(isValidCardNumber('378282246310005')).toBe(true);
    });

    it('should validate Discover card (6011111111111117)', () => {
      expect(isValidCardNumber('6011111111111117')).toBe(true);
    });

    it('should validate 13-digit card', () => {
      expect(isValidCardNumber('4222222222222')).toBe(true);
    });
  });

  describe('Invalid Card Numbers', () => {
    it('should reject empty string', () => {
      expect(isValidCardNumber('')).toBe(false);
    });

    it('should reject card with invalid checksum', () => {
      expect(isValidCardNumber('4111111111111112')).toBe(false);
    });

    it('should reject card with letters', () => {
      expect(isValidCardNumber('411111111111111a')).toBe(false);
    });

    it('should reject card with special characters', () => {
      expect(isValidCardNumber('4111-1111-1111-1111')).toBe(false);
    });

    it('should reject card with spaces', () => {
      expect(isValidCardNumber('4111 1111 1111 1111')).toBe(false);
    });

    it('should reject too short card (12 digits)', () => {
      expect(isValidCardNumber('411111111111')).toBe(false);
    });

    it('should reject too long card (20 digits)', () => {
      expect(isValidCardNumber('41111111111111111111')).toBe(false);
    });

    it('should reject single digit', () => {
      expect(isValidCardNumber('4')).toBe(false);
    });

    it('should accept all zeros (valid Luhn checksum)', () => {
      // Note: 0000000000000000 실제로 Luhn 체크섬은 통과함
      expect(isValidCardNumber('0000000000000000')).toBe(true);
    });
  });

  describe('Edge Cases', () => {
    it('should handle undefined', () => {
      expect(isValidCardNumber(undefined as unknown as string)).toBe(false);
    });

    it('should handle null', () => {
      expect(isValidCardNumber(null as unknown as string)).toBe(false);
    });

    it('should handle whitespace only', () => {
      expect(isValidCardNumber('   ')).toBe(false);
    });

    it('should reject card with mixed valid/invalid characters', () => {
      expect(isValidCardNumber('4111a111b111c111')).toBe(false);
    });
  });
});

describe('formatCardNumber', () => {
  it('should format 16-digit card with hyphens', () => {
    expect(formatCardNumber('4111111111111111')).toBe('4111-1111-1111-1111');
  });

  it('should format partially entered card', () => {
    expect(formatCardNumber('4111')).toBe('4111');
    expect(formatCardNumber('41111')).toBe('4111-1');
    expect(formatCardNumber('411111111')).toBe('4111-1111-1');
  });

  it('should remove non-digit characters before formatting', () => {
    expect(formatCardNumber('4111-1111-1111-1111')).toBe('4111-1111-1111-1111');
    expect(formatCardNumber('4111 1111 1111 1111')).toBe('4111-1111-1111-1111');
    expect(formatCardNumber('4111a1111b1111c1111')).toBe('4111-1111-1111-1111');
  });

  it('should handle empty string', () => {
    expect(formatCardNumber('')).toBe('');
  });

  it('should format 15-digit Amex card', () => {
    expect(formatCardNumber('378282246310005')).toBe('3782-8224-6310-005');
  });

  it('should format 13-digit card', () => {
    expect(formatCardNumber('4222222222222')).toBe('4222-2222-2222-2');
  });

  it('should handle single digit', () => {
    expect(formatCardNumber('4')).toBe('4');
  });
});

describe('cleanCardNumber', () => {
  it('should remove hyphens', () => {
    expect(cleanCardNumber('4111-1111-1111-1111')).toBe('4111111111111111');
  });

  it('should remove spaces', () => {
    expect(cleanCardNumber('4111 1111 1111 1111')).toBe('4111111111111111');
  });

  it('should remove letters', () => {
    expect(cleanCardNumber('4111a1111b1111c1111')).toBe('4111111111111111');
  });

  it('should remove special characters', () => {
    expect(cleanCardNumber('4111@1111#1111$1111')).toBe('4111111111111111');
  });

  it('should keep only digits', () => {
    expect(cleanCardNumber('abc4111def1111ghi1111jkl1111mno')).toBe('4111111111111111');
  });

  it('should handle empty string', () => {
    expect(cleanCardNumber('')).toBe('');
  });

  it('should handle string with no digits', () => {
    expect(cleanCardNumber('abcdefg')).toBe('');
  });
});
