'use client';

import { useSearchParams } from 'next/navigation';
import { useEffect, useState, Suspense } from 'react';
import { confirmPayment } from '@/lib/api/payments';
import { ApiError } from '@/lib/api/types';

type PaymentStatus = 'loading' | 'success' | 'error';

function SuccessContent() {
  const searchParams = useSearchParams();
  const paymentKey = searchParams.get('paymentKey');
  const amount = searchParams.get('amount');

  const [status, setStatus] = useState<PaymentStatus>('loading');
  const [errorMessage, setErrorMessage] = useState<string>('');
  const [paymentInfo, setPaymentInfo] = useState<any>(null);

  useEffect(() => {
    if (!paymentKey) {
      setStatus('error');
      setErrorMessage('결제 정보가 없습니다');
      return;
    }

    const processPayment = async () => {
      try {
        // confirm API 호출
        const response = await confirmPayment({
          payment_key: paymentKey,
        });

        // 금액 검증 (쿼리 파라미터로 amount가 있는 경우)
        if (amount && parseInt(amount, 10) !== response.total_amount) {
          setStatus('error');
          setErrorMessage('결제 금액이 일치하지 않습니다');
          return;
        }

        setPaymentInfo(response);
        setStatus('success');
      } catch (err) {
        if (err instanceof ApiError) {
          setErrorMessage(err.errorResponse.detail || err.errorResponse.title);
        } else {
          setErrorMessage('결제 처리 중 오류가 발생했습니다');
        }
        setStatus('error');
      }
    };

    processPayment();
  }, [paymentKey, amount]);

  if (status === 'loading') {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="mb-4 inline-block h-12 w-12 animate-spin rounded-full border-4 border-blue-600 border-t-transparent"></div>
          <h2 className="text-xl font-semibold">결제 처리 중...</h2>
          <p className="mt-2 text-gray-600">잠시만 기다려주세요</p>
        </div>
      </div>
    );
  }

  if (status === 'error') {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50">
        <div className="max-w-md rounded-lg bg-white p-8 text-center shadow-md">
          <div className="mb-4 text-6xl">❌</div>
          <h1 className="mb-2 text-2xl font-bold text-red-600">결제 실패</h1>
          <p className="mb-6 text-gray-600">{errorMessage}</p>
          <a
            href="/merchant"
            className="inline-block rounded-md bg-gray-600 px-6 py-3 text-white transition-colors hover:bg-gray-700"
          >
            상품 목록으로 돌아가기
          </a>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50">
      <div className="max-w-md rounded-lg bg-white p-8 shadow-md">
        <div className="mb-4 text-center text-6xl">✅</div>
        <h1 className="mb-6 text-center text-2xl font-bold text-green-600">결제 완료</h1>

        <div className="space-y-4 rounded-lg bg-gray-50 p-4">
          <div className="flex justify-between">
            <span className="text-gray-600">주문번호</span>
            <span className="font-semibold">{paymentInfo?.order_id}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-600">결제 금액</span>
            <span className="text-xl font-bold">
              {paymentInfo?.total_amount?.toLocaleString()}원
            </span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-600">상태</span>
            <span className="rounded-full bg-green-100 px-3 py-1 text-xs font-semibold text-green-800">
              {paymentInfo?.status}
            </span>
          </div>
          {paymentInfo?.approved_at && (
            <div className="flex justify-between">
              <span className="text-gray-600">승인 시각</span>
              <span className="text-sm">
                {new Date(paymentInfo.approved_at).toLocaleString('ko-KR')}
              </span>
            </div>
          )}
        </div>

        <div className="mt-6 text-center">
          <a
            href="/merchant"
            className="inline-block rounded-md bg-blue-600 px-6 py-3 text-white transition-colors hover:bg-blue-700"
          >
            상품 목록으로 돌아가기
          </a>
        </div>
      </div>
    </div>
  );
}

export default function SuccessPage() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-screen items-center justify-center">
          <div className="inline-block h-12 w-12 animate-spin rounded-full border-4 border-blue-600 border-t-transparent"></div>
        </div>
      }
    >
      <SuccessContent />
    </Suspense>
  );
}
