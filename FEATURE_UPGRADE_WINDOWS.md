# Northshop feature upgrade on Windows

This patch adds order cancellation, admin order status, optional test payment, and an optional AI shopping assistant. It preserves your existing H2 data. Stop the backend with Ctrl+C before copying its database.

## 1. Apply

Run in PowerShell from your existing project:

    $root = "C:\dev\Northshop-Java-Full-Stack-Upgrade\Northshop-Java-Full-Stack-Upgrade"
    cd "$root\Ecommerce-Backend"
    Copy-Item ".\ecomdb.mv.db" ".\ecomdb-before-order-features.mv.db"
    Expand-Archive -LiteralPath "$env:USERPROFILE\Downloads\Northshop-Orders-Payment-AI-Patch.zip" -DestinationPath $root -Force

Skip Copy-Item if the H2 file does not exist. Keep both database files and credentials out of GitHub.

## 2. Build and run

In the backend PowerShell window, set your **existing** JWT_SECRET, ADMIN_USERNAME, ADMIN_EMAIL and ADMIN_PASSWORD values. Changing bootstrap variables does not reset an existing admin password.

    cd "$root\Ecommerce-Backend"
    .\mvnw.cmd clean test
    .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"

Wait for Tomcat started on port 8080. In a second PowerShell window:

    cd "$root"
    Invoke-RestMethod http://localhost:8080/actuator/health
    cd "$root\Ecommerce-Frontend"
    npm ci
    npm run build
    npm run dev

Open http://localhost:5173. If clean test fails, stop and share the first [ERROR] block.

## 3. Test without provider keys

1. Sign in as a customer and buy one product with Place unpaid order.
2. Open My orders. Confirm PLACED / NOT REQUIRED; cancel that order and verify product stock increases by exactly the ordered quantity.
3. Refresh. The cancelled order remains in history and repeated cancellation cannot restore stock again.
4. Sign in as admin, open Manage orders, and move a different order to SHIPPED then DELIVERED. A customer must not reach the admin API.
5. From the project root, run py scripts/acceptance_test.py with the working admin credentials in that PowerShell window. It writes Testing records to the demo database.

## 4. Optional Razorpay test checkout

Create your own Razorpay account and generate **test-mode** API keys. Set RAZORPAY_TEST_KEY_ID (starting with rzp_test_) and RAZORPAY_TEST_KEY_SECRET in the **backend** PowerShell window, then restart it. No real money moves in test mode. Never paste secrets into the frontend, README, chat or GitHub.

As a customer, add an item and select Pay in test mode. Use a documented Razorpay test payment method. A captured payment shows PLACED / PAID. Dismiss checkout to see PENDING_PAYMENT; retry, check payment status, or cancel the pending test order when no payment is processing. Pending orders reserve stock.

## 5. Optional AI shopping assistant

Set OPENROUTER_API_KEY and OPENROUTER_MODEL in the backend window and restart. Choose a model available to your account; provider usage may be charged. Sign in, click Ask AI and ask about products in the catalog. Verify generated names and prices against product pages. Without both variables, this endpoint returns 503 and the store continues to work.

## Limits

Paid orders have no refund flow and cannot be cancelled. Admin shipping statuses are manual. There is no real payment, shipping carrier, webhook, automatic pending-order expiry, or guarantee that AI output is correct. The provider integrations must be checked with your test credentials; the frontend build was verified in this workspace.
