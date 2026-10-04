# Fix Delete for products already used in orders

Previously, deleting a purchased product failed because `order_item` retained a foreign key to it. The updated admin Delete action archives the product, hides it from the catalog and search, and blocks new purchases. Past order records remain intact. The stored product row is intentionally retained.

## Apply to the existing Windows project

1. Stop Spring Boot with Ctrl+C and stop Vite with Ctrl+C. From PowerShell, back up the local H2 database before the schema change:

```powershell
$root = "C:\dev\Northshop-Java-Full-Stack-Upgrade\Northshop-Java-Full-Stack-Upgrade"
Copy-Item "$root\Ecommerce-Backend\ecomdb.mv.db" "$root\Ecommerce-Backend\ecomdb-before-archive.mv.db"
```

2. Download `Northshop-Product-Removal-Patch.zip` into Downloads and extract it over the project root:

```powershell
Expand-Archive -LiteralPath "$env:USERPROFILE\Downloads\Northshop-Product-Removal-Patch.zip" -DestinationPath $root -Force
cd "$root\Ecommerce-Backend"
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Use the same terminal where your existing JWT_SECRET is set; set it again privately if you opened a new terminal. Wait for `Started EcomProjApplication` and leave this terminal open. The new test class `ProductArchiveTest` should pass.

3. In another PowerShell window, confirm health, then rerun the updated acceptance script only on your disposable demo database:

```powershell
cd $root
Invoke-RestMethod http://localhost:8080/actuator/health
$env:ADMIN_USERNAME = "northshop_admin_2026"
$env:ADMIN_PASSWORD = "<your actual existing admin password>"
py scripts/acceptance_test.py
```

4. Restart Vite in its terminal with `npm run dev`. Log in as admin, select **Show test records**, open the purchased smoke-test product that previously failed to delete, click **Delete**, and accept the dialog. It should disappear from the catalog after a refresh. Open My orders with the customer who bought it and confirm the historical line item remains. Do not publish the backup database file or any credentials.
