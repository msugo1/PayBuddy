import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CardPaymentForm } from './CardPaymentForm';

describe('CardPaymentForm', () => {
  it('should render all form fields', () => {
    const mockOnSubmit = vi.fn();
    render(<CardPaymentForm onSubmit={mockOnSubmit} />);

    expect(screen.getByLabelText('카드번호')).toBeInTheDocument();
    expect(screen.getByLabelText('만료월 (MM)')).toBeInTheDocument();
    expect(screen.getByLabelText('만료년도 (YY)')).toBeInTheDocument();
    expect(screen.getByLabelText('CVC')).toBeInTheDocument();
    expect(screen.getByLabelText('카드 소유자명')).toBeInTheDocument();
    expect(screen.getByLabelText('할부 개월 수')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /결제하기/ })).toBeInTheDocument();
  });

  it('should format card number input with hyphens', async () => {
    const user = userEvent.setup();
    const mockOnSubmit = vi.fn();
    render(<CardPaymentForm onSubmit={mockOnSubmit} />);

    const cardNumberInput = screen.getByLabelText('카드번호') as HTMLInputElement;

    await user.type(cardNumberInput, '4111111111111111');

    await waitFor(() => {
      expect(cardNumberInput.value).toBe('4111-1111-1111-1111');
    });
  });

  it('should submit form with valid data', async () => {
    const user = userEvent.setup();
    const mockOnSubmit = vi.fn();
    render(<CardPaymentForm onSubmit={mockOnSubmit} />);

    await user.type(screen.getByLabelText('카드번호'), '4111111111111111');
    await user.type(screen.getByLabelText('만료월 (MM)'), '12');
    await user.type(screen.getByLabelText('만료년도 (YY)'), '25');
    await user.type(screen.getByLabelText('CVC'), '123');
    await user.type(screen.getByLabelText('카드 소유자명'), '홍길동');

    await user.click(screen.getByRole('button', { name: /결제하기/ }));

    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalled();
      const callArgs = mockOnSubmit.mock.calls[0][0];
      expect(callArgs).toEqual({
        cardNumber: '4111-1111-1111-1111',
        expiryMonth: '12',
        expiryYear: '25',
        cvc: '123',
        cardHolderName: '홍길동',
        installmentMonths: 0,
      });
    });
  });

  it('should show validation error for invalid card number', async () => {
    const user = userEvent.setup();
    const mockOnSubmit = vi.fn();
    render(<CardPaymentForm onSubmit={mockOnSubmit} />);

    await user.type(screen.getByLabelText('카드번호'), '1234567812345678');
    await user.click(screen.getByRole('button', { name: /결제하기/ }));

    await waitFor(() => {
      expect(screen.getByText(/유효하지 않은 카드번호입니다/)).toBeInTheDocument();
    });

    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it('should show validation error for invalid expiry month', async () => {
    const user = userEvent.setup();
    const mockOnSubmit = vi.fn();
    render(<CardPaymentForm onSubmit={mockOnSubmit} />);

    await user.type(screen.getByLabelText('카드번호'), '4111111111111111');
    await user.type(screen.getByLabelText('만료월 (MM)'), '13');
    await user.click(screen.getByRole('button', { name: /결제하기/ }));

    await waitFor(() => {
      expect(screen.getByText(/올바른 월을 입력해주세요/)).toBeInTheDocument();
    });

    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it('should disable form when isLoading is true', () => {
    const mockOnSubmit = vi.fn();
    render(<CardPaymentForm onSubmit={mockOnSubmit} isLoading={true} />);

    expect(screen.getByLabelText('카드번호')).toBeDisabled();
    expect(screen.getByLabelText('만료월 (MM)')).toBeDisabled();
    expect(screen.getByRole('button', { name: /처리 중.../ })).toBeDisabled();
  });

  it('should remove non-numeric characters from CVC', async () => {
    const user = userEvent.setup();
    const mockOnSubmit = vi.fn();
    render(<CardPaymentForm onSubmit={mockOnSubmit} />);

    const cvcInput = screen.getByLabelText('CVC') as HTMLInputElement;

    await user.type(cvcInput, '1a2b3');

    await waitFor(() => {
      expect(cvcInput.value).toBe('123');
    });
  });

  it('should remove non-numeric characters from expiry fields', async () => {
    const user = userEvent.setup();
    const mockOnSubmit = vi.fn();
    render(<CardPaymentForm onSubmit={mockOnSubmit} />);

    const monthInput = screen.getByLabelText('만료월 (MM)') as HTMLInputElement;
    const yearInput = screen.getByLabelText('만료년도 (YY)') as HTMLInputElement;

    await user.type(monthInput, '1a2');
    await user.type(yearInput, '2b5');

    await waitFor(() => {
      expect(monthInput.value).toBe('12');
      expect(yearInput.value).toBe('25');
    });
  });
});
