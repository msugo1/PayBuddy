import { test, expect } from '@playwright/test';

test('should complete payment without 3DS', async ({ page }) => {
  // Step 1: Go to merchant page and select a product
  await page.goto('/merchant');
  await expect(page.locator('text=PayBuddy 테스트 상점')).toBeVisible();

  // Click on first product (일반 상품 - no 3DS)
  await page.locator('a[href^="/merchant/product/"]').first().click();

  // Should navigate to product detail page
  await expect(page).toHaveURL(/\/merchant\/product\//);

  // Click payment button
  await page.locator('button:has-text("결제하기")').click();

  // Step 2: Fill payment widget (in iframe)
  const widgetFrame = page.frameLocator('iframe[src*="widget"]');
  await widgetFrame.locator('input[id="cardNumber"]').fill('4111111111111111');
  await widgetFrame.locator('input[id="expiryMonth"]').fill('12');
  await widgetFrame.locator('input[id="expiryYear"]').fill('25');
  await widgetFrame.locator('input[id="cvc"]').fill('123');
  await widgetFrame.locator('input[id="cardHolderName"]').fill('홍길동');

  // Submit payment
  await widgetFrame.locator('button:has-text("결제하기")').click();

  // Step 3: Should redirect to success page (skipping 3DS)
  await expect(page).toHaveURL(/\/success/, { timeout: 10000 });

  // Step 4: Check success message
  await expect(page.locator('text=결제가 완료되었습니다')).toBeVisible({ timeout: 10000 });
});
