import { http, HttpResponse } from 'msw';
import type { PaymentSubmitResponse } from '../api/types';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/v1';

export const handlers = [
  // POST /payments/checkout/submit
  http.post(`${API_BASE_URL}/payments/checkout/submit`, async ({ request }) => {
    const body = await request.json() as { payment_key: string; payment_method: unknown };

    const response: PaymentSubmitResponse = {
      payment_key: body.payment_key,
      next_action: {
        type: 'none',
      },
    };

    return HttpResponse.json(response, { status: 200 });
  }),
];
