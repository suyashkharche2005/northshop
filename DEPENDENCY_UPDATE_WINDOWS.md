# Northshop frontend dependency audit update (Windows)

This patch updates Vite to 6.4.3 and React Router DOM to 7.18.4, with a new npm lockfile. It does not change the backend or your local H2 database.

1. Stop Vite with Ctrl+C. Download `Northshop-Dependency-Audit-Patch.zip` into Downloads and extract it over the existing project root:

```powershell
$root = "C:\dev\Northshop-Java-Full-Stack-Upgrade\Northshop-Java-Full-Stack-Upgrade"
Expand-Archive -LiteralPath "$env:USERPROFILE\Downloads\Northshop-Dependency-Audit-Patch.zip" -DestinationPath $root -Force
cd "$root\Ecommerce-Frontend"
npm ci
npm audit
npm run build
npm run dev
```

2. The audit should report `found 0 vulnerabilities`. Open `http://localhost:5173` with the backend still running. Check catalog images, open a product, add to bag, sign in, and open My orders. Also check that an admin can open Add product and a customer is redirected away from `/admin/products/new`.

3. If `npm audit` or the browser flow differs, send the first error and `node --version`. Do not run `npm audit fix --force`.
