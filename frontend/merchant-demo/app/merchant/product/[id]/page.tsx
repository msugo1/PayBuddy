'use client';

import { useParams, useRouter } from 'next/navigation';
import { getProductById } from '@/lib/mock-products';

export default function ProductDetailPage() {
  const params = useParams();
  const router = useRouter();
  const product = getProductById(params.id as string);

  if (!product) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="text-center">
          <h1 className="text-2xl font-bold">상품을 찾을 수 없습니다</h1>
          <button
            onClick={() => router.push('/merchant')}
            className="mt-4 text-blue-600 hover:underline"
          >
            상품 목록으로 돌아가기
          </button>
        </div>
      </div>
    );
  }

  const handlePayment = async () => {
    // TODO: T2.6에서 ready API 연동
    console.log('결제 시작:', product);
    alert('결제 기능은 곧 구현됩니다!');
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="border-b bg-white">
        <div className="mx-auto max-w-7xl px-4 py-6">
          <button
            onClick={() => router.push('/merchant')}
            className="mb-4 text-blue-600 hover:underline"
          >
            ← 목록으로 돌아가기
          </button>
          <h1 className="text-3xl font-bold">{product.name}</h1>
        </div>
      </header>

      <main className="mx-auto max-w-4xl px-4 py-8">
        <div className="grid gap-8 md:grid-cols-2">
          {/* Product Image */}
          <div className="flex items-center justify-center rounded-lg bg-gray-100 p-16">
            <span className="text-9xl">📦</span>
          </div>

          {/* Product Info */}
          <div className="flex flex-col justify-between">
            <div>
              <div className="mb-4">
                <span className="inline-block rounded-full bg-blue-100 px-3 py-1 text-sm text-blue-800">
                  {product.scenario}
                </span>
              </div>
              <h2 className="mb-4 text-2xl font-bold">{product.name}</h2>
              <p className="mb-6 text-gray-600">{product.description}</p>
              <div className="mb-8">
                <span className="text-4xl font-bold">{product.price.toLocaleString()}원</span>
              </div>
            </div>

            <button
              onClick={handlePayment}
              className="w-full rounded-lg bg-blue-600 px-6 py-4 text-lg font-semibold text-white transition-colors hover:bg-blue-700"
            >
              구매하기
            </button>
          </div>
        </div>

        {/* Scenario Info */}
        <div className="mt-8 rounded-lg border border-gray-200 bg-white p-6">
          <h3 className="mb-3 font-semibold">테스트 시나리오</h3>
          <ul className="space-y-2 text-sm text-gray-600">
            {product.scenario === 'no-3ds' && (
              <li>✅ 3DS 인증 없이 바로 결제 진행</li>
            )}
            {product.scenario === '3ds-required' && (
              <>
                <li>✅ 3DS 인증 필수</li>
                <li>✅ 인증 완료 후 결제 진행</li>
              </>
            )}
            {product.scenario === 'min-amount-fail' && (
              <li>❌ 최소 금액 미달로 결제 실패</li>
            )}
            {product.scenario === 'installment-only' && (
              <li>✅ 할부 결제만 가능</li>
            )}
            {product.scenario === 'payment-fail' && (
              <li>❌ 의도적 결제 실패</li>
            )}
          </ul>
        </div>
      </main>
    </div>
  );
}
