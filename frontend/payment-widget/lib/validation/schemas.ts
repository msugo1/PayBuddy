import { z } from 'zod';
import { isValidCardNumber, cleanCardNumber } from './card-validator';

export const cardPaymentSchema = z.object({
  cardNumber: z
    .string()
    .min(1, '카드번호를 입력해주세요')
    .refine((val) => {
      const cleaned = cleanCardNumber(val);
      return isValidCardNumber(cleaned);
    }, '유효하지 않은 카드번호입니다'),

  expiryMonth: z
    .string()
    .min(1, '만료월을 입력해주세요')
    .regex(/^(0[1-9]|1[0-2])$/, '올바른 월을 입력해주세요 (01-12)'),

  expiryYear: z
    .string()
    .min(1, '만료년도를 입력해주세요')
    .regex(/^\d{2}$/, '년도는 2자리로 입력해주세요')
    .refine((val) => {
      const year = parseInt(val, 10);
      const currentYear = new Date().getFullYear() % 100;
      return year >= currentYear;
    }, '만료된 카드입니다'),

  cvc: z
    .string()
    .min(1, 'CVC를 입력해주세요')
    .regex(/^\d{3,4}$/, 'CVC는 3-4자리 숫자입니다'),

  cardHolderName: z
    .string()
    .min(1, '카드 소유자명을 입력해주세요')
    .max(50, '카드 소유자명은 50자 이내로 입력해주세요'),

  installmentMonths: z
    .number()
    .int('할부 개월수는 정수여야 합니다')
    .min(0, '할부 개월수는 0 이상이어야 합니다')
    .max(36, '할부 개월수는 최대 36개월입니다'),
});

export type CardPaymentFormData = z.infer<typeof cardPaymentSchema>;
