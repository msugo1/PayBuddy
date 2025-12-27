'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { cardPaymentSchema, CardPaymentFormData } from '@/lib/validation/schemas';
import { formatCardNumber } from '@/lib/validation/card-validator';

type CardPaymentFormProps = {
  onSubmit: (data: CardPaymentFormData) => void;
  isLoading?: boolean;
};

export function CardPaymentForm({ onSubmit, isLoading = false }: CardPaymentFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
    setValue,
  } = useForm<CardPaymentFormData>({
    resolver: zodResolver(cardPaymentSchema),
    defaultValues: {
      installmentMonths: 0,
    },
  });

  const handleCardNumberChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const formatted = formatCardNumber(e.target.value);
    setValue('cardNumber', formatted, { shouldValidate: true });
  };

  const handleExpiryChange = (e: React.ChangeEvent<HTMLInputElement>, field: 'expiryMonth' | 'expiryYear') => {
    const value = e.target.value.replace(/\D/g, '');
    setValue(field, value, { shouldValidate: true });
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      {/* Card Number */}
      <div>
        <label htmlFor="cardNumber" className="block text-sm font-medium text-gray-700">
          카드번호
        </label>
        <input
          id="cardNumber"
          type="text"
          maxLength={19}
          placeholder="1234-5678-9012-3456"
          {...register('cardNumber')}
          onChange={handleCardNumberChange}
          className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          disabled={isLoading}
        />
        {errors.cardNumber && (
          <p className="mt-1 text-sm text-red-600">{errors.cardNumber.message}</p>
        )}
      </div>

      {/* Expiry Date */}
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label htmlFor="expiryMonth" className="block text-sm font-medium text-gray-700">
            만료월 (MM)
          </label>
          <input
            id="expiryMonth"
            type="text"
            maxLength={2}
            placeholder="12"
            {...register('expiryMonth')}
            onChange={(e) => handleExpiryChange(e, 'expiryMonth')}
            className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            disabled={isLoading}
          />
          {errors.expiryMonth && (
            <p className="mt-1 text-sm text-red-600">{errors.expiryMonth.message}</p>
          )}
        </div>
        <div>
          <label htmlFor="expiryYear" className="block text-sm font-medium text-gray-700">
            만료년도 (YY)
          </label>
          <input
            id="expiryYear"
            type="text"
            maxLength={2}
            placeholder="25"
            {...register('expiryYear')}
            onChange={(e) => handleExpiryChange(e, 'expiryYear')}
            className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            disabled={isLoading}
          />
          {errors.expiryYear && (
            <p className="mt-1 text-sm text-red-600">{errors.expiryYear.message}</p>
          )}
        </div>
      </div>

      {/* CVC */}
      <div>
        <label htmlFor="cvc" className="block text-sm font-medium text-gray-700">
          CVC
        </label>
        <input
          id="cvc"
          type="text"
          maxLength={4}
          placeholder="123"
          {...register('cvc')}
          onChange={(e) => {
            const value = e.target.value.replace(/\D/g, '');
            setValue('cvc', value, { shouldValidate: true });
          }}
          className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          disabled={isLoading}
        />
        {errors.cvc && <p className="mt-1 text-sm text-red-600">{errors.cvc.message}</p>}
      </div>

      {/* Card Holder Name */}
      <div>
        <label htmlFor="cardHolderName" className="block text-sm font-medium text-gray-700">
          카드 소유자명
        </label>
        <input
          id="cardHolderName"
          type="text"
          placeholder="홍길동"
          {...register('cardHolderName')}
          className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          disabled={isLoading}
        />
        {errors.cardHolderName && (
          <p className="mt-1 text-sm text-red-600">{errors.cardHolderName.message}</p>
        )}
      </div>

      {/* Installment Months */}
      <div>
        <label htmlFor="installmentMonths" className="block text-sm font-medium text-gray-700">
          할부 개월 수
        </label>
        <select
          id="installmentMonths"
          {...register('installmentMonths', { valueAsNumber: true })}
          className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          disabled={isLoading}
        >
          <option value={0}>일시불</option>
          {[2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 24, 36].map((months) => (
            <option key={months} value={months}>
              {months}개월
            </option>
          ))}
        </select>
        {errors.installmentMonths && (
          <p className="mt-1 text-sm text-red-600">{errors.installmentMonths.message}</p>
        )}
      </div>

      {/* Submit Button */}
      <button
        type="submit"
        disabled={isLoading}
        className="w-full rounded-md bg-blue-600 px-4 py-3 font-semibold text-white transition-colors hover:bg-blue-700 disabled:bg-gray-400"
      >
        {isLoading ? '처리 중...' : '결제하기'}
      </button>
    </form>
  );
}
