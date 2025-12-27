export type Product = {
  id: string;
  name: string;
  description: string;
  price: number;
  image: string;
  scenario: 'no-3ds' | '3ds-required' | 'min-amount-fail' | 'installment-only' | 'payment-fail';
};

export const MOCK_PRODUCTS: Product[] = [
  {
    id: 'prod-001',
    name: '일반 상품',
    description: '3DS 인증 없이 결제되는 일반 상품입니다.',
    price: 10000,
    image: '/images/product-1.jpg',
    scenario: 'no-3ds',
  },
  {
    id: 'prod-002',
    name: '고가 상품',
    description: '3DS 인증이 필요한 고가 상품입니다.',
    price: 50000,
    image: '/images/product-2.jpg',
    scenario: '3ds-required',
  },
  {
    id: 'prod-003',
    name: '최소 금액 테스트',
    description: '결제 최소 금액(1,000원) 미만으로 실패하는 상품입니다.',
    price: 500,
    image: '/images/product-3.jpg',
    scenario: 'min-amount-fail',
  },
  {
    id: 'prod-004',
    name: '할부 전용 상품',
    description: '할부 결제만 가능한 상품입니다.',
    price: 100000,
    image: '/images/product-4.jpg',
    scenario: 'installment-only',
  },
  {
    id: 'prod-005',
    name: '결제 실패 테스트',
    description: '의도적으로 결제가 실패하는 상품입니다.',
    price: 20000,
    image: '/images/product-5.jpg',
    scenario: 'payment-fail',
  },
];

export function getProductById(id: string): Product | undefined {
  return MOCK_PRODUCTS.find((product) => product.id === id);
}
