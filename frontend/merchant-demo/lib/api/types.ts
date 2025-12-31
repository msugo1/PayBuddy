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

// PaymentConfirmRequest
export type PaymentConfirmRequest = {
  payment_key: string;
};

// NextAction
export type NextAction =
  | {
      type: 'challenge_3ds2';
      acs_url: string;
      creq: string;
      challenge_window_size: string;
    }
  | {
      type: 'await_deposit';
      virtual_account: {
        bank_code: string;
        bank_name: string;
        account_number: string;
        depositor_name: string | null;
        expiry_date: string;
      };
    }
  | {
      type: 'none';
    };

// PaymentConfirmResponse
export type PaymentConfirmResponse = {
  payment_id: string;
  payment_key: string;
  order_id: string;
  status: string;
  total_amount: number;
  supply_amount: number;
  vat_amount: number;
  requested_at: string;
  approved_at: string | null;
  receipt_url: string | null;
  next_action: NextAction | null;
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
