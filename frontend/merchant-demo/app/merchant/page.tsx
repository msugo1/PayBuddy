import { MOCK_PRODUCTS } from '@/lib/mock-products';
import { ProductCard } from '@/components/merchant/ProductCard';

export default function MerchantPage() {
  return (
    <div className="min-h-screen bg-gray-50">
      <header className="border-b bg-white">
        <div className="mx-auto max-w-7xl px-4 py-6">
          <h1 className="text-3xl font-bold">PayBuddy 테스트 상점</h1>
          <p className="mt-2 text-gray-600">다양한 결제 시나리오를 테스트할 수 있습니다</p>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-4 py-8">
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {MOCK_PRODUCTS.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      </main>
    </div>
  );
}
