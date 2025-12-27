// PaymentReadyRequest
export type PaymentReadyRequest = {
  merchant_id: string;
  order_id: string;
  total_amount: number;
  supply_amount: number;
  vat_amount: number;
  redirect_url: string;
};

// PaymentReadyResponse
export type PaymentReadyResponse = {
  paymentKey: string;
  checkoutUrl: string;
  expiresAt: string;
};

// Error Response (Problem JSON)
export type ErrorResponse = {
  type: string;
  title: string;
  status: number;
  detail?: string;
  instance?: string;
  error_code?: string;
  errors?: Record<string, string[]>;
};

export class ApiError extends Error {
  constructor(
    public status: number,
    public errorResponse: ErrorResponse,
  ) {
    super(errorResponse.title);
    this.name = 'ApiError';
  }
}
