import { http, HttpResponse } from 'msw';
import type { PaymentReadyResponse } from '../api/types';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/v1';

export const handlers = [
  // POST /payments/ready
  http.post(`${API_BASE_URL}/payments/ready`, async () => {
    const response: PaymentReadyResponse = {
      paymentKey: `pay_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`,
      checkoutUrl: 'http://localhost:3001/widget?paymentKey=test123',
      expiresAt: new Date(Date.now() + 10 * 60 * 1000).toISOString(),
    };

    return HttpResponse.json(response, { status: 200 });
  }),

  // POST /payments/confirm
  http.post(`${API_BASE_URL}/payments/confirm`, async ({ request }) => {
    const body = await request.json() as { payment_key: string };

    const response = {
      payment_id: `pm_${Date.now()}`,
      payment_key: body.payment_key,
      order_id: 'order-test-123',
      status: 'DONE',
      total_amount: 10000,
      approved_at: new Date().toISOString(),
    };

    return HttpResponse.json(response, { status: 200 });
  }),

  // Error case: 400 Bad Request
  http.post(`${API_BASE_URL}/payments/ready-error`, async () => {
    return HttpResponse.json(
      {
        type: 'about:blank',
        title: 'Bad Request',
        status: 400,
        detail: 'Invalid payment amount',
        error_code: 'INVALID_AMOUNT',
      },
      {
        status: 400,
        headers: {
          'Content-Type': 'application/problem+json',
        },
      }
    );
  }),
];
