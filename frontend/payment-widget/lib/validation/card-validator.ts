/**
 * Luhn 알고리즘을 사용한 카드번호 검증
 * @param cardNumber - 카드번호 (숫자만)
 * @returns 유효성 여부
 */
export function isValidCardNumber(cardNumber: string): boolean {
  // 빈 문자열 또는 숫자가 아닌 문자 포함 시 false
  if (!cardNumber || !/^\d+$/.test(cardNumber)) {
    return false;
  }

  // 13~19자리 범위 확인 (일반적인 카드번호 길이)
  const length = cardNumber.length;
  if (length < 13 || length > 19) {
    return false;
  }

  // Luhn 알고리즘
  let sum = 0;
  let isEven = false;

  // 오른쪽부터 왼쪽으로 순회
  for (let i = length - 1; i >= 0; i--) {
    let digit = parseInt(cardNumber[i], 10);

    if (isEven) {
      digit *= 2;
      if (digit > 9) {
        digit -= 9;
      }
    }

    sum += digit;
    isEven = !isEven;
  }

  return sum % 10 === 0;
}

/**
 * 카드번호를 포맷팅 (1234-5678-9012-3456)
 */
export function formatCardNumber(value: string): string {
  const cleaned = value.replace(/\D/g, '');
  const groups = cleaned.match(/.{1,4}/g);
  return groups ? groups.join('-') : cleaned;
}

/**
 * 카드번호에서 숫자만 추출
 */
export function cleanCardNumber(value: string): string {
  return value.replace(/\D/g, '');
}
