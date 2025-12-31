'use client';

import { useSearchParams } from 'next/navigation';
import { useState, Suspense } from 'react';

function MockACSContent() {
  const searchParams = useSearchParams();
  const creq = searchParams.get('creq');
  const [isProcessing, setIsProcessing] = useState(false);

  const handleAuthentication = (success: boolean) => {
    setIsProcessing(true);

    // 실제 ACS에서 생성하는 cres (Challenge Response) 시뮬레이션
    const cres = {
      transStatus: success ? 'Y' : 'N', // Y: 인증 성공, N: 실패
      eci: success ? '05' : '07', // Electronic Commerce Indicator
      authenticationValue: success ? 'mock_cavv_success_12345' : '',
      messageVersion: '2.1.0',
      acsTransID: `mock_acs_${Date.now()}`,
    };

    // 3DS 페이지(부모 iframe)에 결과 전달
    window.parent.postMessage(
      {
        type: '3DS_RESULT',
        success: success,
        cres: JSON.stringify(cres),
      },
      '*' // 프로덕션에서는 origin 지정 필요
    );

    // 시각적 피드백
    setTimeout(() => {
      setIsProcessing(false);
    }, 500);
  };

  if (!creq) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-red-600">잘못된 접근입니다</h1>
          <p className="mt-2 text-gray-600">Challenge Request가 없습니다</p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 p-4">
      <div className="w-full max-w-md rounded-lg bg-white p-8 shadow-xl">
        {/* Mock 카드사 로고 */}
        <div className="mb-6 flex items-center justify-center">
          <div className="rounded-full bg-blue-600 p-4">
            <svg className="h-8 w-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
            </svg>
          </div>
        </div>

        {/* 헤더 */}
        <div className="mb-6 text-center">
          <h1 className="text-2xl font-bold text-gray-800">카드사 본인 인증</h1>
          <p className="mt-2 text-sm text-gray-600">
            Mock ACS - 테스트 환경
          </p>
        </div>

        {/* 정보 표시 */}
        <div className="mb-6 rounded-md bg-gray-50 p-4">
          <p className="text-sm text-gray-600">
            <span className="font-semibold">Challenge Request:</span>
          </p>
          <p className="mt-1 break-all text-xs text-gray-500">{creq}</p>
        </div>

        {/* 안내 메시지 */}
        <div className="mb-6 rounded-md border border-blue-200 bg-blue-50 p-4">
          <p className="text-sm text-blue-800">
            💡 실제 환경에서는 OTP 입력, 생체 인증 등이 진행됩니다.
          </p>
          <p className="mt-1 text-xs text-blue-600">
            테스트를 위해 성공/실패 버튼을 제공합니다.
          </p>
        </div>

        {/* 인증 버튼 */}
        <div className="space-y-3">
          <button
            onClick={() => handleAuthentication(true)}
            disabled={isProcessing}
            className="w-full rounded-lg bg-green-600 px-6 py-4 text-lg font-semibold text-white transition-colors hover:bg-green-700 disabled:bg-gray-400"
          >
            {isProcessing ? '처리 중...' : '✅ 인증 성공'}
          </button>

          <button
            onClick={() => handleAuthentication(false)}
            disabled={isProcessing}
            className="w-full rounded-lg bg-red-600 px-6 py-4 text-lg font-semibold text-white transition-colors hover:bg-red-700 disabled:bg-gray-400"
          >
            {isProcessing ? '처리 중...' : '❌ 인증 실패'}
          </button>
        </div>

        {/* 푸터 */}
        <div className="mt-6 text-center text-xs text-gray-500">
          <p>이 페이지는 3DS 인증을 시뮬레이션합니다</p>
          <p className="mt-1">프로덕션 환경에서는 실제 카드사 ACS가 사용됩니다</p>
        </div>
      </div>
    </div>
  );
}

export default function MockACSPage() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-screen items-center justify-center">
          <div className="inline-block h-12 w-12 animate-spin rounded-full border-4 border-blue-600 border-t-transparent"></div>
        </div>
      }
    >
      <MockACSContent />
    </Suspense>
  );
}
