import { apiClient } from './client';
import { PaymentReadyRequest, PaymentReadyResponse } from './types';

export async function readyPayment(
  request: PaymentReadyRequest,
  idempotencyKey: string,
): Promise<PaymentReadyResponse> {
  const response = await apiClient.post('payments/ready', {
    json: request,
    headers: {
      'Idempotency-Key': idempotencyKey,
    },
  });

  return response.json<PaymentReadyResponse>();
}

export function generateIdempotencyKey(): string {
  return `idem_${Date.now()}_${Math.random().toString(36).substring(2, 15)}`;
}

export function calculateVAT(totalAmount: number): { supplyAmount: number; vatAmount: number } {
  // 총액 = 공급가액 + 부가세
  // 부가세 = 총액 / 11 (10% VAT)
  const vatAmount = Math.floor(totalAmount / 11);
  const supplyAmount = totalAmount - vatAmount;

  return { supplyAmount, vatAmount };
}
