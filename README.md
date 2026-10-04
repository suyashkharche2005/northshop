# Northshop — Spring Boot + React storefront

A portfolio ecommerce application built by upgrading the supplied Spring Boot/React project. Customers can browse products, register, sign in, build a bag, place a demo order, and review order history. An administrator can create, edit, and remove catalog products. Orders record actual database prices and reduce stock atomically. **No payment, shipping, or email service is connected.**

## What changed from the original

The source already had a React/Vite catalog, product images stored in MySQL, local cart, and JWT authentication. The first signup was automatically made ADMIN. The client attempted checkout by uploading product images and calling the admin-only product update endpoint once per item; it could partially fail and trusted client stock. This version assigns every public signup USER, supports explicit admin bootstrap, and commits an order and stock changes in one server transaction. It preserves the product endpoints and multipart image upload contract. Product responses use DTOs to avoid embedding image bytes in catalog JSON.

## Stack and architecture

- Java 17, Spring Boot 3.3.3, Spring Web, Spring Security, Spring Data JPA/Hibernate, Bean Validation, JUnit 5/Mockito.
- MySQL for the primary profile. File-backed H2 is available for a local demo profile.
- React 18, React Router 6, Vite 5, Axios, plain responsive CSS.

`React pages → Axios client → SecurityFilterChain/JWT filter → REST controller → service (@Transactional) → JPA repository → MySQL or H2`

Backend packages: `controller`, `service`, `dto`, `model`, `repo`, `security`, `exception`, `config`. Frontend: `src/components` pages, `src/Context` cart/auth state, `src/axios.jsx` API client, `App.jsx` routes, `App.css` design system.

## Data model

| Table | Key fields | Relationship |
| --- | --- | --- |
| `app_user` | id, unique username/email, BCrypt password, role | One user has many orders |
| `product` | id, price, category, availability, stock, image BLOB | One product can appear in many order items |
| `customer_order` | id, customer FK, createdAt, total, status | One order has many items |
| `order_item` | id, order FK, product FK, productName, unitPrice, quantity | Snapshots name and price at checkout |

The product image endpoint is separate from product JSON. Checkout locks product rows in ascending ID order, validates availability/quantity, calculates totals from persisted prices, and commits the order and stock changes together. Historical order items retain product foreign keys, so deleting a product referenced by an order is restricted by the database; archive/soft-delete is a future enhancement.

## Configuration

**Requirements:** JDK 17+, Maven 3.9+, Node 20+, npm, MySQL 8+ (or the H2 demo profile). Backend Java target is 17 so it can build on newer JDKs too.

Create the database once for MySQL:

```sql
CREATE DATABASE ecomdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'ecom_user'@'localhost' IDENTIFIED BY 'your-unique-password';
GRANT ALL PRIVILEGES ON ecomdb.* TO 'ecom_user'@'localhost';
```

Copy `Ecommerce-Backend/.env.example` into your own environment manager or export variables in the terminal. Spring Boot does not load `.env` files automatically. **Do not commit real credentials.**

| Variable | Required | Meaning |
| --- | --- | --- |
| `JWT_SECRET` | Yes | Random secret, at least 32 characters. Changing it invalidates existing tokens. |
| `DB_URL` | MySQL profile | JDBC URL, default `jdbc:mysql://localhost:3306/ecomdb` |
| `DB_USERNAME`, `DB_PASSWORD` | MySQL profile | Database credentials |
| `CORS_ORIGINS` | Optional | Comma-separated frontend origins, defaults to `http://localhost:5173` |
| `ADMIN_USERNAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Optional together | Create a dedicated admin when its username and email are unused; password must have 12+ characters. Existing accounts are never silently promoted. |
| `PORT` | Optional | Backend port, default 8080 |
| `VITE_API_URL` | Optional | Full frontend API root, default `http://localhost:8080/api`; set before frontend build. |

Local demo without MySQL: use `--spring.profiles.active=demo`. The H2 database is saved under `Ecommerce-Backend/ecomdb.mv.db`; keep that directory private. The mandatory JWT secret must still be provided. Schema update is used for local development and existing project compatibility; production needs reviewed schema migrations and backups before deployment.

## Run locally

In a terminal, set a real secret and (for MySQL) database credentials. On Windows PowerShell use `$env:JWT_SECRET='...'` and `$env:DB_PASSWORD='...'`.

```bash
cd Ecommerce-Backend
export JWT_SECRET='replace-with-a-long-unique-random-secret-at-least-32-chars'
export DB_USERNAME=ecom_user DB_PASSWORD='your-unique-password'
./mvnw spring-boot:run
```

If the included Maven wrapper cannot bootstrap on your machine, use `mvn spring-boot:run` with Maven installed. For H2 demo: `./mvnw spring-boot:run -Dspring-boot.run.profiles=demo`. In another terminal:

```bash
cd Ecommerce-Frontend
npm ci
npm run dev
```

Open `http://localhost:5173`. Register a normal customer. To create an admin, set the three `ADMIN_*` environment variables and restart the backend. The bootstrap creates that account if its name and email are unused; if the database already contains a user with that identity, choose a distinct admin name/email. It never grants ADMIN to a public signup. Log in with the admin credentials to manage products.

Images are optional for product creation/update. JPEG, PNG, and WebP are accepted up to 5 MB. The default data set is empty: create products as admin. Existing product data is retained by Hibernate's `update` schema strategy. **Back up an existing database before pointing this version at it.** Old first-signup ADMIN accounts retain their stored role; review them manually before deployment.

## API overview

| Method | Path | Access | Result |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | Public | 201 and JWT; USER only |
| POST | `/api/auth/login` | Public | JWT and role |
| GET | `/api/products`, `/api/products/search?keyword=...` | Public | Product DTO list |
| GET | `/api/product/{id}` | Public | Product DTO |
| GET | `/api/product/{id}/image` | Public | Image bytes or 404 |
| POST | `/api/product` | ADMIN | Multipart `product` JSON + optional `imageFile`; 201 |
| PUT | `/api/product/{id}` | ADMIN | Same multipart format; image optional |
| DELETE | `/api/product/{id}` | ADMIN | 204 if unreferenced |
| POST | `/api/orders` | Signed in | `{ "items": [{ "productId": 1, "quantity": 2 }] }`; 201 |
| GET | `/api/orders/mine` | Signed in | Current user's orders |
| GET | `/actuator/health` | Public | Health status |

Protected endpoints require `Authorization: Bearer <jwt>`. Validation errors return JSON with `timestamp`, `status`, and `message`; authentication/authorization failures return HTTP 401/403. Checkout returns 409 when stock is insufficient. The client stores tokens in browser session storage and removes them on sign out/401. Logout is client-side token deletion; tokens remain valid until their ten-hour expiry if copied elsewhere.

## Testing and deployment

Run `mvn test` and `mvn package` inside `Ecommerce-Backend`; run `npm ci && npm run build` inside `Ecommerce-Frontend`. `OrderServiceTest` checks insufficient-stock rejection. Exercise registration, admin bootstrap, CRUD, two competing orders, and order history against a real MySQL database before production deployment. Configure DB, JWT, allowed frontend origin, and API URL on your deployment platform. Serve the Vite `dist` as a static SPA with fallback to `index.html`; run the backend as a Java service. Supply HTTPS at the reverse proxy and store secrets in the platform's secret manager. Do not publish a `.env` file.

## Screenshots

Add your own screenshots after running the application: storefront, product page, bag, order history, admin product editor, and mobile layout.

## Limitations and next improvements

There is no payment, shipment, refund, image object storage, admin order dashboard, password reset, server-side pagination, or schema migration history. Catalog reads currently load complete product lists; for a large catalog use indexed filtering and paginated DTO queries. For production, introduce Flyway migrations after reconciling the existing schema, token revocation/refresh strategy, secure session design, image scanning/storage, and an archived-product model that keeps historic order items valid.
