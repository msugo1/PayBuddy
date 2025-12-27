import { http, HttpResponse } from 'msw';
import type { PaymentReadyResponse } from '../api/types';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/v1';

export const handlers = [
  // POST /payments/ready
  http.post(`${API_BASE_URL}/payments/ready`, async ({ request }) => {
    const body = await request.json() as { total_amount: number; order_id: string };

    const response: PaymentReadyResponse = {
      payment_key: `pay_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`,
      checkout_url: 'http://localhost:3001/widget?paymentKey=test123',
      expires_at: new Date(Date.now() + 10 * 60 * 1000).toISOString(),
      order_id: body.order_id,
      total_amount: body.total_amount,
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
