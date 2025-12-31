'use client';

import { useSearchParams } from 'next/navigation';
import { useEffect, useRef, useState, Suspense } from 'react';

const TIMEOUT_MS = 180000; // 3 minutes

function ThreeDSContent() {
  const searchParams = useSearchParams();
  const acsUrl = searchParams.get('acsUrl');
  const creq = searchParams.get('creq');
  const size = searchParams.get('size') || '02';
  const paymentKey = searchParams.get('paymentKey');

  const formRef = useRef<HTMLFormElement>(null);
  const [isTimeout, setIsTimeout] = useState(false);

  useEffect(() => {
    if (!acsUrl || !creq) {
      return;
    }

    // Auto-submit form to ACS
    if (formRef.current) {
      formRef.current.submit();
    }

    // Timeout handler
    const timer = setTimeout(() => {
      setIsTimeout(true);
    }, TIMEOUT_MS);

    return () => clearTimeout(timer);
  }, [acsUrl, creq]);

  if (!acsUrl || !creq || !paymentKey) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-red-600">잘못된 접근입니다</h1>
          <p className="mt-2 text-gray-600">3DS 인증 정보가 없습니다</p>
        </div>
      </div>
    );
  }

  const getSizeStyle = (sizeCode: string) => {
    switch (sizeCode) {
      case '01':
        return { width: '250px', height: '400px' };
      case '02':
        return { width: '390px', height: '400px' };
      case '03':
        return { width: '500px', height: '600px' };
      case '04':
        return { width: '600px', height: '400px' };
      case '05':
        return { width: '100%', height: '100vh' };
      default:
        return { width: '390px', height: '400px' };
    }
  };

  const iframeStyle = getSizeStyle(size);

  if (isTimeout) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50">
        <div className="max-w-md rounded-lg bg-white p-8 text-center shadow-md">
          <div className="mb-4 text-6xl">⏱️</div>
          <h1 className="mb-2 text-2xl font-bold text-red-600">인증 시간 초과</h1>
          <p className="mb-6 text-gray-600">
            3DS 인증 시간이 초과되었습니다.
            <br />
            다시 시도해주세요.
          </p>
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
    <div className="flex min-h-screen flex-col items-center justify-center bg-gray-50 p-4">
      <div className="mb-4 text-center">
        <h1 className="text-2xl font-bold">3DS 인증</h1>
        <p className="mt-2 text-gray-600">카드사 인증을 진행해주세요</p>
      </div>

      {/* Hidden form for auto-submit */}
      <form
        ref={formRef}
        method="POST"
        action={acsUrl}
        target="threeds-iframe"
        style={{ display: 'none' }}
      >
        <input type="hidden" name="creq" value={creq} />
      </form>

      {/* ACS iframe */}
      <iframe
        name="threeds-iframe"
        title="3DS Challenge"
        style={{
          ...iframeStyle,
          border: '1px solid #e5e7eb',
          borderRadius: '0.5rem',
          backgroundColor: 'white',
        }}
      />

      <div className="mt-4 text-center text-sm text-gray-500">
        <p>인증 창이 표시되지 않으면 새로고침해주세요</p>
        <p className="mt-1">최대 {TIMEOUT_MS / 1000 / 60}분 이내 완료해주세요</p>
      </div>
    </div>
  );
}

export default function ThreeDSPage() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-screen items-center justify-center">
          <div className="inline-block h-12 w-12 animate-spin rounded-full border-4 border-blue-600 border-t-transparent"></div>
        </div>
      }
    >
      <ThreeDSContent />
    </Suspense>
  );
}
