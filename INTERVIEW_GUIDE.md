# Northshop interview guide

## Two-minute explanation

“I upgraded my original Spring Boot and React ecommerce project into a storefront where users can browse products, add them to a bag, place a demo order, and see order history. Admins can maintain the product catalog and upload images. The frontend is React 18 with Vite, React Router, Axios, and a responsive design. The backend is Spring Boot 3.3.3 with controller, service, repository, DTO, and security layers. MySQL stores users, products, orders, and line items. Spring Security authenticates users with BCrypt passwords and signed JWTs. Public signups always get USER, while an admin account is explicitly bootstrapped with environment supplied credentials. The most important change was checkout: my old client tried to update product stock using the admin product endpoint. Now the client sends product IDs and quantities to an authenticated order endpoint. A transactional service locks each product, validates stock, calculates prices using database values, creates an order with price snapshots, and reduces stock together. This avoids partial cart updates and lets normal users order without admin privileges. I separated image delivery from product DTOs to keep catalog responses smaller and added validation, structured errors, a focused stock test, and setup documentation. It is a demo order flow without actual payment or shipping, and the next production step would be schema migrations and paginated catalog queries.”

## Request and security flow

1. React Router renders a page. Axios sends an HTTP request to the configured API URL and adds the session token if present.
2. Spring Security checks public/catalog routes, otherwise the JWT filter parses the token and loads the user from the database. The role is taken from current database authorities.
3. A controller validates input and passes it to a service. The service applies business rules and calls Spring Data JPA repositories.
4. For orders, the transaction locks each requested product in ascending ID order, rejects invalid stock, snapshots prices, saves order items, and commits inventory updates together.
5. The response DTO goes back through Axios; React updates the UI. On a 401, the client clears its session token.

JWTs are signed with an environment supplied HS256 key and expire after ten hours. BCrypt hashes passwords. Authorization is enforced on the backend; UI route guards improve navigation only. Logout clears the browser token but does not revoke an already copied JWT. CORS is configured for specified origins. Registration never accepts a client supplied role.

## Database and Java concepts

`User (1) → (many) CustomerOrder (1) → (many) OrderItem (many) → (1) Product`. Order items snapshot names and unit prices. Enums represent USER/ADMIN and PLACED. Constructors support dependency injection and entity creation. Encapsulation keeps order item ownership in `CustomerOrder.addItem`. Interfaces are used through JPA repositories. `Optional` handles missing rows; collections, a sorted `TreeMap`, `merge`, streams, lambdas, records, `BigDecimal`, `Instant`, and `LocalDate` appear where they have a domain purpose. A custom `ApiException` and global advice produce client errors. Transactions and pessimistic row locks protect the stock invariant. No arbitrary inheritance hierarchy was introduced.

## Design decisions and limits

- Existing product routes and multipart upload structure were preserved for compatibility.
- Product DTOs omit image bytes; the image endpoint streams the binary content separately.
- Server-side order placement handles stock and prices; the cart is only a local UI draft.
- H2 demo profile is useful for local learning; MySQL is the target data store.
- Hibernate `ddl-auto=update` keeps the legacy schema approachable but is not a migration strategy. Migrations and a data backup are needed before production changes.
- Deleting purchased products is restricted by order item foreign keys; use product archiving in a later release.
- No payment is charged, no shipment is created, and no email is sent.

## 35 project-specific questions and concise answers

1. **Explain your project.** A React storefront with Spring Boot catalog, JWT users/admin, and transactional demo orders.
2. **Why Spring Boot?** It provides REST, dependency injection, security, validation, JPA integration, and test support in one coherent backend.
3. **Explain your architecture.** Controller receives HTTP, service applies rules, repository manages persistence, DTOs define the API contract.
4. **Why a service layer?** Checkout and product rules need to be shared and tested independently of HTTP details.
5. **How does authentication work?** Login asks AuthenticationManager to verify a BCrypt password, then returns a signed JWT.
6. **How does JWT work here?** The client sends a Bearer token; the filter verifies signature/expiry and loads current user authorities.
7. **How are passwords stored?** BCrypt salted one-way hashes, never plaintext.
8. **How are frontend and backend connected?** Axios uses `VITE_API_URL` and sends JSON or multipart requests to REST endpoints.
9. **Explain the relationships.** User has orders; order has items; each item references a product and snapshots its name and unit price.
10. **Why DTOs?** They validate requests and prevent exposing entities, password fields, and image bytes in general JSON.
11. **How does global exception handling work?** `@RestControllerAdvice` maps business and validation exceptions to HTTP status and a structured error body.
12. **How does Spring Security protect APIs?** A SecurityFilterChain permits catalog reads, requires ADMIN for product writes, and requires login for orders.
13. **What happens after clicking Place order?** Axios posts IDs/quantities; Spring authenticates, validates, locks products, computes total, saves order, commits stock, and returns a DTO.
14. **Why not trust cart prices?** Browser data is editable or stale; prices must come from the database at checkout.
15. **Why use `@Transactional`?** All stock decrements and order inserts must succeed or roll back together.
16. **Why lock product rows?** Concurrent checkouts must not both accept the last unit.
17. **Why lock in ascending ID order?** A consistent order reduces deadlock risk for multi-product carts.
18. **What happens on insufficient stock?** The service throws a 409 business error and the whole transaction rolls back.
19. **Why `BigDecimal`?** Binary floating-point cannot reliably represent decimal currency values.
20. **Why snapshot unit prices?** A later product price change must not rewrite the historical order amount.
21. **How does admin creation work?** Optional environment credentials create a distinct ADMIN account; signup always creates USER.
22. **What was wrong with first-user admin?** A public visitor could become an administrator before the owner registers.
23. **How do protected React routes work?** React checks the current role for navigation; the backend independently rejects unauthorized requests.
24. **Where is the token stored?** Session storage; sign out clears it, but a stolen valid token stays valid until expiry.
25. **Why is CORS configured?** The dev frontend and API run on different origins; only configured frontend origins are allowed.
26. **How are images handled?** Multipart upload validates MIME type and size; image bytes are delivered through a separate URL.
27. **Why separate image bytes from product JSON?** Product lists stay lighter and the browser can fetch images independently.
28. **How is form validation done?** HTML gives immediate feedback and Bean Validation enforces server-side rules through `@Valid`.
29. **What if a product was removed from a local cart?** The cart detects missing items and blocks checkout until adjusted.
30. **What does your test cover?** It verifies an order exceeding stock is rejected without saving or changing quantity.
31. **How would you scale to 100,000 users?** Add indexed, paginated catalog queries, external image storage/CDN, connection pool tuning, and monitored load tests.
32. **Why not add Redis or Kafka?** The current domain flow is synchronous and transactional; those systems add operating cost without a concrete need.
33. **How would you handle product deletion after a sale?** Archive it from the storefront while retaining the foreign key for history.
34. **What would you improve for production security?** Add HTTPS, secure token lifecycle/refresh or server sessions, rate limits, audited admin actions, and reviewed migrations.
35. **What is the biggest limitation?** Checkout records a demo order and inventory change but does not charge money or arrange fulfillment.
