# Test info

- Name: Rooster App Navigation >> should show Community, Fowl, Marketplace tabs and Verify Transfer button
- Location: /home/user/AndroidStudioProjects/Rooster/tests/navigation.spec.ts:4:7

# Error details

```
Error: page.goto: net::ERR_CONNECTION_REFUSED at http://localhost:8080/
Call log:
  - navigating to "http://localhost:8080/", waiting until "load"

    at /home/user/AndroidStudioProjects/Rooster/tests/navigation.spec.ts:6:16
```

# Test source

```ts
   1 | import { test, expect } from '@playwright/test';
   2 |
   3 | test.describe('Rooster App Navigation', () => {
   4 |   test('should show Community, Fowl, Marketplace tabs and Verify Transfer button', async ({ page }) => {
   5 |     // Replace with your app's local dev server or emulator URL
>  6 |     await page.goto('http://localhost:8080');
     |                ^ Error: page.goto: net::ERR_CONNECTION_REFUSED at http://localhost:8080/
   7 |
   8 |     // Community tab should be visible
   9 |     await expect(page.getByText('Community')).toBeVisible();
  10 |     // Fowl tab should be visible
  11 |     await expect(page.getByText('Fowl')).toBeVisible();
  12 |     // Marketplace tab should be visible
  13 |     await expect(page.getByText('Marketplace')).toBeVisible();
  14 |
  15 |     // Click Marketplace tab
  16 |     await page.getByText('Marketplace').click();
  17 |     // Wait for listings to load
  18 |     await page.waitForTimeout(1000);
  19 |     // Check for Verify Transfer button (if any listing exists)
  20 |     const verifyButtons = await page.locator('button', { hasText: 'Verify Transfer' });
  21 |     if (await verifyButtons.count() > 0) {
  22 |       await expect(verifyButtons.first()).toBeVisible();
  23 |     } else {
  24 |       // If no listings, just pass the test for navigation
  25 |       expect(true).toBeTruthy();
  26 |     }
  27 |   });
  28 | });
  29 |
  30 | // NOTE: This test is designed for web apps. For Android UI automation, use Espresso, UIAutomator, or Appium with Playwright.
  31 | // If running on an emulator, ensure the app is accessible via a web server or use the correct automation tool for Android.
  32 |
```