import { apiClient } from './client';
import { PaymentSubmitRequest, PaymentSubmitResponse } from './types';

export async function submitPaymentMethod(
  request: PaymentSubmitRequest,
): Promise<PaymentSubmitResponse> {
  const response = await apiClient.post('payments/checkout/submit', {
    json: request,
  });

  return response.json<PaymentSubmitResponse>();
}
