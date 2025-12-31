import Link from 'next/link';
import { Product } from '@/lib/mock-products';

type ProductCardProps = {
  product: Product;
};

export function ProductCard({ product }: ProductCardProps) {
  return (
    <Link
      href={`/merchant/product/${product.id}`}
      className="block rounded-lg border border-gray-200 p-6 transition-shadow hover:shadow-lg"
    >
      <div className="mb-4 flex h-48 items-center justify-center rounded-md bg-gray-100">
        <span className="text-4xl">📦</span>
      </div>
      <h3 className="mb-2 text-lg font-semibold">{product.name}</h3>
      <p className="mb-4 text-sm text-gray-600">{product.description}</p>
      <div className="flex items-center justify-between">
        <span className="text-xl font-bold">{product.price.toLocaleString()}원</span>
        <span className="rounded-full bg-blue-100 px-3 py-1 text-xs text-blue-800">
          {product.scenario}
        </span>
      </div>
    </Link>
  );
}
