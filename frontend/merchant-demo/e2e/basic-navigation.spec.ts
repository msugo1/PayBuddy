import { test, expect } from '@playwright/test';

test('should display merchant page with products', async ({ page }) => {
  await page.goto('/merchant');

  // Check title
  await expect(page.locator('h1')).toContainText('PayBuddy 테스트 상점');

  // Check that products are displayed
  await expect(page.locator('text=일반 상품')).toBeVisible();
  await expect(page.locator('text=고가 상품')).toBeVisible();

  // Check that at least 5 products exist
  const productCards = page.locator('a[href^="/merchant/product/"]');
  await expect(productCards).toHaveCount(5);
});

test('should navigate to product detail page', async ({ page }) => {
  await page.goto('/merchant');

  // Click on first product
  await page.locator('text=일반 상품').click();

  // Should navigate to detail page
  await expect(page).toHaveURL(/\/merchant\/product\//);

  // Check product details are visible
  await expect(page.locator('h1')).toContainText('일반 상품');
  await expect(page.locator('button:has-text("결제하기")')).toBeVisible();
});
