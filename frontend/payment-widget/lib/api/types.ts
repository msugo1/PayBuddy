// PaymentMethod
export type CardPaymentMethod = {
  card_number: string;
  expiry_month: string;
  expiry_year: string;
  cvc: string;
  card_holder_name: string;
  installment_months: number;
};

export type PaymentMethod = {
  type: 'CARD';
  card: CardPaymentMethod;
};

// PaymentSubmitRequest
export type PaymentSubmitRequest = {
  payment_key: string;
  payment_method: PaymentMethod;
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
      type: 'none';
    };

// PaymentSubmitResponse
export type PaymentSubmitResponse = {
  payment_key: string;
  next_action: NextAction;
};

// Error Response
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
