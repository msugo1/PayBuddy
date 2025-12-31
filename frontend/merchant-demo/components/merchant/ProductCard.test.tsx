import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ProductCard } from './ProductCard';
import type { Product } from '@/lib/mock-products';

describe('ProductCard', () => {
  const mockProduct: Product = {
    id: 'test-001',
    name: '테스트 상품',
    description: '테스트 상품 설명입니다',
    price: 10000,
    image: '/test.jpg',
    scenario: 'no-3ds',
  };

  it('should render product information correctly', () => {
    render(<ProductCard product={mockProduct} />);

    expect(screen.getByText('테스트 상품')).toBeInTheDocument();
    expect(screen.getByText('테스트 상품 설명입니다')).toBeInTheDocument();
    expect(screen.getByText('10,000원')).toBeInTheDocument();
    expect(screen.getByText('no-3ds')).toBeInTheDocument();
  });

  it('should render as a link to product detail page', () => {
    render(<ProductCard product={mockProduct} />);

    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', '/merchant/product/test-001');
  });

  it('should format price with locale string', () => {
    const expensiveProduct: Product = {
      ...mockProduct,
      price: 1234567,
    };

    render(<ProductCard product={expensiveProduct} />);

    expect(screen.getByText('1,234,567원')).toBeInTheDocument();
  });

  it('should display different scenarios correctly', () => {
    const scenarios: Array<Product['scenario']> = [
      'no-3ds',
      '3ds-required',
      'min-amount-fail',
      'installment-only',
      'payment-fail',
    ];

    scenarios.forEach((scenario) => {
      const product: Product = {
        ...mockProduct,
        scenario,
      };

      const { unmount } = render(<ProductCard product={product} />);
      expect(screen.getByText(scenario)).toBeInTheDocument();
      unmount();
    });
  });

  it('should render product icon', () => {
    render(<ProductCard product={mockProduct} />);

    expect(screen.getByText('📦')).toBeInTheDocument();
  });
});
