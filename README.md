# 7-Eleven Vietnam - Retail Management System

A mini retail management system built with Spring Boot 3 (Java 21) backend and React + Vite frontend.

## Quick Start

```bash
# Start all services
docker compose up -d

# Wait for services to be ready (~30 seconds)
```

Then access:
- **Frontend**: http://localhost:5173
- **Swagger API**: http://localhost:8080/swagger-ui.html

## Test Accounts

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@7eleven.com | Admin@123 |
| User | user@7eleven.com | User@123 |

## Project Structure

```
seven-eleven-test/
├── backend/              # Spring Boot 3 + Java 21
│   ├── src/main/java/com/seveneleven
│   │   ├── controller/  # REST Controllers
│   │   ├── service/     # Business Logic
│   │   ├── repository/   # Data Access
│   │   ├── entity/      # JPA Entities
│   │   ├── dto/         # Data Transfer Objects
│   │   ├── config/      # Configuration
│   │   ├── security/    # JWT Security
│   │   └── exception/   # Exception Handling
│   └── src/main/resources/
│       └── db/migration/ # Flyway Migrations
├── frontend/             # React + Vite + TailwindCSS
│   └── src/
│       ├── pages/       # Page Components
│       ├── components/  # Reusable Components
│       ├── services/    # API Services
│       └── context/     # React Context
└── docker-compose.yml
```

## Features

### Backend
- JWT Authentication (Access + Refresh tokens)
- Role-based Access Control (Admin/User)
- Redis caching for products
- PostgreSQL with Flyway migrations
- RESTful API with Swagger documentation
- Trace ID for request tracking
- Input validation
- Global exception handling
- Unit tests (JUnit 5 + Mockito)

### Frontend
- Login with JWT authentication
- Token refresh interceptor
- Product management (Admin)
- Order management (Admin)
- Product browsing (User)
- Shopping cart (User)
- Toast notifications
- Loading states
- Empty states

## API Endpoints

### Authentication
- `POST /api/auth/login` - Login
- `POST /api/auth/refresh` - Refresh token
- `GET /api/auth/me` - Get current user

### Products (Public: GET, Admin: POST/PUT/DELETE)
- `GET /api/products` - List products (paginated, searchable)
- `GET /api/products/{id}` - Get product detail
- `POST /api/products` - Create product
- `PUT /api/products/{id}` - Update product
- `DELETE /api/products/{id}` - Delete product

### Orders
- `POST /api/orders` - Create order (User)
- `GET /api/orders` - List orders (Admin)
- `GET /api/orders/{id}` - Get order detail (Admin)

## Tech Stack

| Backend | Frontend |
|---------|----------|
| Java 21 | React 18 |
| Spring Boot 3 | Vite |
| Spring Security | TailwindCSS |
| Spring Data JPA | Axios |
| PostgreSQL | React Router |
| Redis | Sonner (Toast) |
| Flyway | Lucide Icons |
| JWT | |
| Lombok | |
| MapStruct | |
| Swagger/OpenAPI | |

## Development

### Prerequisites
- Java 21
- Node.js 20
- Docker & Docker Compose

### Backend (Local)
```bash
cd backend
./mvnw spring-boot:run
```

### Frontend (Local)
```bash
cd frontend
npm install
npm run dev
```

### Run Tests
```bash
cd backend
./mvnw test
```

## Sample Products

The system seeds 15 sample products including:
- Coca Cola, Pepsi, 7UP (Beverages)
- Red Bull, Energy Drink (Energy Drinks)
- Aquafina Water, Green Tea, Milk Tea, Orange Juice
- Nescafe Coffee
- Lay's Chips, Oreo Cookies, Chocolate Bar
- Instant Noodles, Sandwich

## Author

Vin

 Quach Huu - Fresher Java Engineer Technical Test
