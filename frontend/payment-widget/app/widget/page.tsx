'use client';

import { useSearchParams, useRouter } from 'next/navigation';
import { useState } from 'react';
import { CardPaymentForm } from '@/components/checkout/CardPaymentForm';
import { CardPaymentFormData } from '@/lib/validation/schemas';
import { submitPaymentMethod } from '@/lib/api/payments';
import { ApiError } from '@/lib/api/types';
import { cleanCardNumber } from '@/lib/validation/card-validator';

export default function WidgetPage() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const paymentKey = searchParams.get('paymentKey');

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!paymentKey) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-red-600">잘못된 접근입니다</h1>
          <p className="mt-2 text-gray-600">paymentKey가 필요합니다</p>
        </div>
      </div>
    );
  }

  const handleSubmit = async (data: CardPaymentFormData) => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await submitPaymentMethod({
        payment_key: paymentKey,
        payment_method: {
          type: 'CARD',
          card: {
            card_number: cleanCardNumber(data.cardNumber),
            expiry_month: data.expiryMonth,
            expiry_year: data.expiryYear,
            cvc: data.cvc,
            card_holder_name: data.cardHolderName,
            installment_months: data.installmentMonths,
          },
        },
      });

      // NextAction 처리
      if (response.next_action.type === 'challenge_3ds2') {
        // 3DS 인증 필요
        const { acs_url, creq, challenge_window_size } = response.next_action;
        // TODO: Task 5에서 3DS 페이지 구현
        window.top!.location.href = `/3ds?acsUrl=${encodeURIComponent(acs_url)}&creq=${encodeURIComponent(creq)}&size=${challenge_window_size}&paymentKey=${paymentKey}`;
      } else if (response.next_action.type === 'none') {
        // 3DS 불필요 → successUrl로 이동
        const successUrl = process.env.NEXT_PUBLIC_SUCCESS_URL || 'http://localhost:3000/success';
        window.top!.location.href = `${successUrl}?paymentKey=${paymentKey}`;
      }
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.errorResponse.detail || err.errorResponse.title);
      } else {
        setError('결제 처리 중 오류가 발생했습니다');
      }
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="mx-auto max-w-md">
        <div className="rounded-lg bg-white p-6 shadow-md">
          <h1 className="mb-6 text-2xl font-bold">카드 정보 입력</h1>

          {error && (
            <div className="mb-4 rounded-md bg-red-50 p-4">
              <p className="text-sm text-red-800">{error}</p>
            </div>
          )}

          <CardPaymentForm onSubmit={handleSubmit} isLoading={isLoading} />
        </div>

        <div className="mt-4 rounded-lg bg-blue-50 p-4">
          <h3 className="mb-2 text-sm font-semibold text-blue-800">테스트 카드번호</h3>
          <ul className="space-y-1 text-xs text-blue-700">
            <li>• 4111-1111-1111-1111 (3DS 없음)</li>
            <li>• 5555-5555-5555-4444 (3DS 필수)</li>
          </ul>
        </div>
      </div>
    </div>
  );
}
