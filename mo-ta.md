````md
# 7-Eleven Vietnam – Fresher Java Engineer Technical Test

Author: Vinh Quach Huu

---

# 1. Objective

Build a mini retail management system including:

1. Admin Product Management
    - List products
    - View product detail
    - Create product
    - Update product
    - Delete product

2. User Order Screen
    - Browse products
    - Create order

3. Admin Order Management
    - View orders
    - View order details

Main goal:
- Demonstrate backend engineering skills
- Build clean and maintainable architecture
- Deliver production-like quality
- Show understanding of REST API, database design, validation, Redis caching, JWT security, Docker, and testing

---

# 2. Tech Stack

## Backend
- Java 21
- Spring Boot 3
- Spring Security
- Spring Data JPA
- PostgreSQL
- Redis
- Flyway Migration
- JWT Authentication
- Lombok
- MapStruct
- Spring Validation
- Swagger / OpenAPI
- Docker

## Frontend
- React + Vite
- TailwindCSS
- Axios
- React Router

---

# 3. Architecture Style

Architecture:
- Feature-based layered architecture
- Production-oriented
- Clean and maintainable
- Avoid overengineering

Main focus:
- Business flow
- API quality
- Clean structure
- Scalability mindset

---

# 4. Backend Structure

```text
src/main/java/com/seveneleven

 
|
|__ controller
|
|
|__service
|
|
|__repository
|
|
___config


___repository

__ mapper (use mapstruct)

__ dto

__ exception


___ common

 ...
 depenence on what is lack of
│
└── SevenElevenApplication.java
````

---

# 5. Development Standards

## Use Lombok

Reduce boilerplate code.

Use:

* @Getter
* @Setter
* @Builder
* @SuperBuilder (because we will define a entity call base entity for many entity)
* @RequiredArgsConstructor
* @NoArgsConstructor
* @AllArgsConstructor
...

---

## Use MapStruct

Map:

* Entity -> Response DTO
* Request DTO -> Entity

Avoid manual mapper code.

Example:

```java
@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductResponse toResponse(Product product);

    Product toEntity(ProductRequest request);
}
```

---

# 6. Security Design

## Authentication

Use:

* JWT Access Token
* Refresh Token

---

## Authorization

Use Role-based Access Control:

### Roles

* ROLE_ADMIN
* ROLE_USER

---

## Initial Accounts

When application starts:

Automatically create:

### Admin Account

```text
email: admin@7eleven.com
password: Admin@123
role: ROLE_ADMIN
```

### User Account

```text
email: user@7eleven.com
password: User@123
role: ROLE_USER
```

Use Flyway seed data.
Sample Product Seed Data

Automatically seed 10–15 sample products using Flyway migration.

Purpose:

Help reviewers test quickly
Avoid manual data creation
Improve demo experience

Example products:

Coca Cola
Pepsi
7UP
Red Bull
Aquafina Water
Nescafe Coffee
Lay’s Chips
Oreo Cookies
Sandwich
Instant Noodles
Green Tea
Milk Tea
Orange Juice
Chocolate Bar
Energy Drink

Each product should include:

name
description
price
stock
image_url

Example:

INSERT INTO products (
    name,
    description,
    price,
    stock,
    image_url
)
VALUES (
    'Coca Cola',
    '330ml soft drink',
    15000,
    100,
    'https://...'
);
Flyway Migration Files
resources/db/migration

V1__create_users_table.sql
V2__create_products_table.sql
V3__create_orders_table.sql
V4__create_order_items_table.sql
V5__insert_default_users.sql
V6__insert_sample_products.sql

Purpose of migration:

Automatically setup database
Create default accounts
Seed sample data
Make project runnable immediately after docker startup
---

# 7. Authentication Flow

## Login Response

```json
{
  "accessToken": "jwt-access-token",
  "refreshToken": "uuid-refresh-token"
}
```

---

## Refresh Token Design

Refresh token:

* UUID only
* Store in Redis

Example:

```text
refresh:uuid -> userId
```

TTL:

* 7 days

---

## Refresh Flow

1. Client sends refresh token
2. Backend checks Redis
3. Get userId
4. Verify user exists
5. Generate new access token
6. Generate new refresh token
7. Replace old token in Redis

---

# 8. Redis Design

## Use Redis For

### Refresh Token Storage

```text
refresh:uuid
```

---

### Product Search Cache

Example key:

```text
products:page:0:size:10:keyword:coca
```

TTL:

* 5 minutes

---

## Cache Eviction

When:

* create product
* update product
* delete product

Must clear related cache.

Example:

```java
@CacheEvict(
    value = "products",
    allEntries = true
)
```

---

# 9. Database Design

## Users

| Column     | Type      |
| ---------- | --------- |
| id         | BIGINT    |
| email      | VARCHAR   |
| password   | VARCHAR   |
| role       | VARCHAR   |
| created_at | TIMESTAMP |
| updated_at | TIMESTAMP |
| isDeleted  | Boolean   |
---

## Product

| Column      | Type      |
| ----------- | --------- |
| id          | BIGINT    |
| name        | VARCHAR   |
| description | TEXT      |
| price       | DECIMAL   |
| stock       | INTEGER   |
| image_url   | VARCHAR   |
| created_at  | TIMESTAMP |
| updated_at  | TIMESTAMP |
| isDeleted  | Boolean   |


---

## Orders

| Column      | Type      |
| ----------- | --------- |
| id          | BIGINT    |
| total_price | DECIMAL   |
| status      | VARCHAR   |
| created_at  | TIMESTAMP |
| updated_at | TIMESTAMP |
| isDeleted  | Boolean   |


---

## Order Items

| Column     | Type    |
| ---------- | ------- |
| id         | BIGINT  |
| order_id   | BIGINT  |
| product_id | BIGINT  |
| quantity   | INTEGER |
| price      | DECIMAL |
| created_at  | TIMESTAMP |
| updated_at | TIMESTAMP |
| isDeleted  | Boolean   |


---


# 10. Flyway Migration

Use Flyway for:

* schema creation
* seed data

Migration files:

```text
resources/db/migration

V1__create_users_table.sql
V2__create_products_table.sql
V3__create_orders_table.sql
V4__create_order_items_table.sql
V5__insert_default_users.sql
V6__insert_sample_products.sql
```

Seed sample products:

* Coca Cola
* Pepsi
* Snack
* Coffee
* Water

---

# 11. Base Response Design & Base Entity Design & Base Pagination

All APIs should return standardized response.

Example:

```json
{
  "success": true,
  "message": "Success",
  "traceId": "8fd8d93a-f2...",
  "data": {}
}
```
All entity 
{
    id : big int
    createdAt: Instant
    updatedAt: Instant
    isDeleted: boolean // for soft delete
}

---

# 12. Trace ID System

Each request: (remember to config for each request the traceId will follow it end-to-end to log it on console for easy to trace error)

* generate UUID traceId
* store in MDC
* return traceId in BaseResponse

Example:

```java
MDC.put("traceId", UUID.randomUUID().toString());
```

Purpose:

* debugging
* request tracking
* production-style logging

---

# 13. Backend Features

## Product APIs

### Create Product

POST /api/products

ROLE_ADMIN only

---

### Get Products

GET /api/products?page=0&size=10&keyword=coca

Supports:

* pagination
* search
* sorting
* Redis cache

---

### Get Product Detail

GET /api/products/{id}
Support redis cache
---

### Update Product

PUT /api/products/{id}

ROLE_ADMIN only

---

### Delete Product

DELETE /api/products/{id}  // soft delete change status on isDelete

ROLE_ADMIN only

---

## Order APIs

### Create Order

POST /api/orders

ROLE_USER only

---

### Get Orders

GET /api/orders

ROLE_ADMIN only

---

### Get Order Detail

GET /api/orders/{id}

ROLE_ADMIN only

---

# 14. Transaction Management

Use @Transactional for order creation.

Flow:

1. Validate stock
2. Create order
3. Create order items
4. Reduce stock
5. Save transaction

Example:

```java
@Transactional
public OrderResponse createOrder(OrderRequest request)
```

---

# 15. Validation

Use:

* @NotBlank
* @NotNull
* @Positive
* @Min

Example:

```java
@NotBlank
private String name;

@Positive
private BigDecimal price;
```

---

# 16. Exception Handling

Create:

* ResourceNotFoundException
* BadRequestException
* UnauthorizedException
* GlobalExceptionHandler

Return clean error responses.

---

# 17. Pagination

Use Spring Pageable.

Example:

```java
Page<Product> findByNameContainingIgnoreCase(
    String keyword,
    Pageable pageable
);
```

Keep implementation simple and maintainable.

---

# 18. Swagger / OpenAPI

Access:

```text
/swagger-ui/index.html
```

---

# 19. Frontend Features

## Login Page

* login with email/password
* refresh token flow

---

## Product Management

* Product table
* Search
* Pagination
* Create/Edit modal
* Delete confirmation

---

## User Order Screen

* Product listing
* Quantity selector
* Order summary
* Submit order

---

## Admin Order Screen

* Order list
* Order detail

---

# 20. Testing

Minimum:

* Create order success
* Insufficient stock
* Login success

Tools:

* JUnit 5
* Mockito

---

# 21. Docker Setup

Need:

* PostgreSQL
* Redis
* Backend
* Frontend

Run project:

```bash
docker compose up -d
```

Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

Frontend:

```text
http://localhost:5173
```

---

# 22. Final ZIP Structure

```text
seven-eleven-test/

├── backend/
├── frontend/
├── docs/
│   ├── erd.png
│   └── architecture.png
│
├── postman/
│   └── seven-eleven.postman_collection.json
│
├── screenshots/
│
├── README.md
└── docker-compose.yml
```

---

# 23. Important Engineering Mindset

This assignment should demonstrate:

* Solve the problem, not the task
* Deliver maintainable code
* Build production-like APIs
* Focus on clean architecture
* Keep implementation practical
* Avoid overengineering

Main priority:

* Clean backend
* Correct business flow
* Good API design
* Easy-to-review project structure

---

# 24. Recommended Timeline

## Day 1

* Setup backend
* Security
* Flyway
* Database design

## Day 2

* Product CRUD
* Redis cache
* Pagination
* Validation

## Day 3

* Order flow
* Transaction
* Frontend

## Day 4

* Swagger
* Testing
* Docker
* Cleanup

## Day 5

* README
* ERD
* Screenshots
* Final polish
* ZIP submission

---

# 25. Final Goal

The goal is NOT:

* Complex architecture
* Fancy UI
* Too many technologies

The goal IS:

* Production mindset
* Clean engineering
* Practical problem solving
* Delivering quality software

```

Ngoài ra mình có vài đề xuất thêm để tăng khả năng pass rất mạnh:

- FE nhớ làm loading + empty state + toast notification → reviewer sẽ thấy app “có chăm chút”.
- Trong Swagger nhớ config JWT Authorize button → nhìn rất production.
- Log request với traceId vào console → cực kỳ ăn điểm backend.
- Seed khoảng 10–15 sản phẩm → reviewer test nhanh hơn nhiều.
- Trong README thêm đúng 3 dòng:
  - `docker compose up -d`
  - link swagger
  - account test
  => reviewer cực thích vì chạy nhanh.
- Nếu còn thời gian:
  - thêm refresh token rotation
  - thêm soft delete product
  - thêm audit fields (`createdBy`, `updatedBy`)
  => nhìn rất enterprise.

Cái quan trọng nhất:
> Reviewer mở project lên phải chạy được ngay trong 2–3 phút.

Đó là thứ giúp pass mạnh hơn cả “kiến trúc phức tạp”.
```
