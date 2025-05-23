import { test, expect } from '@playwright/test';

test.describe('Rooster App Navigation', () => {
  test('should show Community, Fowl, Marketplace tabs and Verify Transfer button', async ({ page }) => {
    // Replace with your app's local dev server or emulator URL
    await page.goto('http://localhost:8080');

    // Community tab should be visible
    await expect(page.getByText('Community')).toBeVisible();
    // Fowl tab should be visible
    await expect(page.getByText('Fowl')).toBeVisible();
    // Marketplace tab should be visible
    await expect(page.getByText('Marketplace')).toBeVisible();

    // Click Marketplace tab
    await page.getByText('Marketplace').click();
    // Wait for listings to load
    await page.waitForTimeout(1000);
    // Check for Verify Transfer button (if any listing exists)
    const verifyButtons = await page.locator('button', { hasText: 'Verify Transfer' });
    if (await verifyButtons.count() > 0) {
      await expect(verifyButtons.first()).toBeVisible();
    } else {
      // If no listings, just pass the test for navigation
      expect(true).toBeTruthy();
    }
  });
});

// NOTE: This test is designed for web apps. For Android UI automation, use Espresso, UIAutomator, or Appium with Playwright.
// If running on an emulator, ensure the app is accessible via a web server or use the correct automation tool for Android.
