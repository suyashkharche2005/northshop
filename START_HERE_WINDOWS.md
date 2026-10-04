# Northshop: finish your local catalog and acceptance test on Windows

Your existing H2 demo database and accounts can stay in place. The catalog seed adds missing products; it does not delete orders or reset stock.

## 1. Apply the catalog patch

Stop the backend and Vite with Ctrl+C. Download `Northshop-Catalog-Finalization-Patch.zip` and place it in Downloads. From a new PowerShell window:

```powershell
$root = "C:\dev\Northshop-Java-Full-Stack-Upgrade\Northshop-Java-Full-Stack-Upgrade"
Expand-Archive -LiteralPath "$env:USERPROFILE\Downloads\Northshop-Catalog-Finalization-Patch.zip" -DestinationPath $root -Force
```

The patch replaces source files and adds bundled images/scripts. It does not contain `ecomdb.mv.db`, so the local database remains in place.

## 2. Test and start the backend

```powershell
cd "$root\Ecommerce-Backend"
$env:JWT_SECRET="<YOUR_EXISTING_LONG_JWT_SECRET>"
$env:ADMIN_USERNAME="northshop_admin_2026"
$env:ADMIN_EMAIL="northshop_admin_2026@example.com"
$env:ADMIN_PASSWORD="<YOUR_EXISTING_ADMIN_PASSWORD>"
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Use your actual existing values in this terminal. Wait for `Started EcomProjApplication`; keep the window open. The log should say that the demo catalog added up to 12 products. Later restarts should report them as already present.

## 3. Verify catalog and run acceptance checks

Open another PowerShell window:

```powershell
cd "C:\dev\Northshop-Java-Full-Stack-Upgrade\Northshop-Java-Full-Stack-Upgrade"
Invoke-RestMethod http://localhost:8080/actuator/health
(Invoke-RestMethod http://localhost:8080/api/products | Where-Object category -ne "Testing").Count
$env:ADMIN_USERNAME="northshop_admin_2026"
$env:ADMIN_PASSWORD="<YOUR_EXISTING_ADMIN_PASSWORD>"
py scripts/acceptance_test.py
```

Health should be `UP`, the non-testing product count should be at least `12`, and acceptance should end with `ALL ACCEPTANCE CHECKS PASSED`. The acceptance script creates additional disposable test users, products, and orders; its `Testing` category is hidden from the public storefront. Send the first failure if it does not finish.

## 4. Open the storefront

Open a third PowerShell window:

```powershell
cd "C:\dev\Northshop-Java-Full-Stack-Upgrade\Northshop-Java-Full-Stack-Upgrade\Ecommerce-Frontend"
npm ci
npm run dev
```

Open `http://localhost:5173`. You should see twelve illustrated products in **Desk & Tech**, **Home & Living**, and **Everyday Carry**. Test search for `mug`, category buttons, price sort, product images, add to bag, customer checkout, and My orders. On a mobile-width browser window, confirm cards form one column. The admin can tick **Show test records** to inspect old smoke-test products.

## MySQL development database

The automatic catalog seed runs only with the `demo` profile. If you later test your own MySQL development database, start the backend with MySQL credentials and run `py scripts/seed_catalog.py` from the project root after setting the admin username/password in that same terminal. It uses the real admin API to upload images, skips existing names, and does not delete products. Do not use sample prices as commercial inventory.
