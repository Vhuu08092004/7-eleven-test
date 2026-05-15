# 🚀 Hướng dẫn Deploy toàn bộ dự án lên Render

## 📋 Tổng quan kiến trúc

```
┌─────────────────────────────────────────────────────────────┐
│                      RENDER                                 │
│                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────┐  │
│  │ PostgreSQL  │    │   Redis     │    │  Backend (API)  │  │
│  │  Database   │    │  (Optional) │    │  Spring Boot    │  │
│  │  Free Tier  │    │  Free Tier  │    │  Free Tier      │  │
│  └─────────────┘    └─────────────┘    └────────┬────────┘  │
│                                                  │          │
│                                                  ▼          │
│                                         ┌─────────────────┐  │
│                                         │  Disk (Uploads) │  │
│                                         │  1GB            │  │
│                                         └─────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    NETLIFY / VERCEL                        │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │                 Frontend (React)                    │    │
│  │  Kết nối API: https://seven-eleven-api.onrender.com │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

---

## 🎯 CÁCH 1: Deploy tự động với render.yaml (Khuyến nghị)

### Bước 1: Push code lên GitHub
```bash
git add .
git commit -m "Add render.yaml for deployment"
git push origin main
```

### Bước 2: Connect Render với GitHub
1. Login [render.com](https://render.com)
2. Click **New +** → **Blueprint**
3. Upload file `render.yaml` hoặc connect GitHub repo
4. Render sẽ tự động deploy theo thứ tự:
   - PostgreSQL → Backend

### Bước 3: Cập nhật CORS cho Frontend
Sau khi deploy Backend xong, cập nhật biến `CORS_ORIGINS`:
```
https://your-frontend.netlify.app
```

---

## 🎯 CÁCH 2: Deploy thủ công từng phần

### PHẦN 1: PostgreSQL Database

#### Tạo Database:
1. Login [render.com](https://render.com)
2. **Dashboard** → **New +** → **PostgreSQL**
3. Điền thông tin:
   ```
   Name: seven-eleven-db
   Database: seveneleven
   User: postgres
   Plan: Free
   Region: Singapore
   ```
4. Click **Create Database**
5. Đợi ~2-3 phút để khởi tạo
6. Copy các giá trị từ tab **Connections**:
   - `Internal Database URL` → `DB_HOST`
   - `Password` → `DB_PASSWORD`

#### Lưu ý:
- Database free tier sẽ **tự động sleep** sau 90 phút không dùng
- Lần đầu truy cập sau sleep sẽ mất ~30 giây khởi động lại
- Nếu cần production, nâng lên Starter plan ($7/tháng)

---

### PHẦN 2: Redis (Tùy chọn - Không khuyến nghị cho Free Tier)

#### Tại sao không nên dùng Redis Free Tier?
- Chỉ có **1 connection max**
- Backend Spring Boot dùng nhiều connection cho cache
- Sẽ gây lỗi `RedisConnectionFailureException`

#### Cách tắt Redis hoàn toàn:
```properties
# Trong application.properties (đã cấu hình sẵn)
REDIS_ENABLED=false
CACHE_TYPE=simple
```

---

### PHẦN 3: Backend API

#### Tạo Web Service:
1. **New +** → **Web Service**
2. Connect GitHub repo của bạn
3. Cấu hình:
   ```
   Name: seven-eleven-api
   Language: Java
   Branch: main
   Region: Singapore
   Plan: Free
   ```

#### Build Settings:
```
Root Directory: (để trống hoặc chọn backend)

Build Command: ./mvnw package -DskipTests
                   hoặc: mvn package -DskipTests

Start Command: java -jar target/*.jar
```

#### Environment Variables (QUAN TRỌNG):
```env
# Spring Profile
SPRING_PROFILES_ACTIVE: prod

# Database - Lấy từ PostgreSQL đã tạo
DB_HOST: <hostname>.onrender.com
DB_PORT: 5432
DB_NAME: seveneleven
DB_USERNAME: postgres
DB_PASSWORD: <password từ PostgreSQL>

# Redis - DISABLE
REDIS_ENABLED: false
CACHE_TYPE: simple

# JWT - BẮT BUỘC thay đổi!
JWT_SECRET: <tạo secret mới, ít nhất 32 ký tự>
# Ví dụ: abc123XYZ456DefGHI789JklMNO0123456789PqrStUvwXyzAbc

# CORS - Cho phép frontend
CORS_ORIGINS: https://your-site.netlify.app,https://your-site.vercel.app,http://localhost:5173

# Logging - Giảm log cho production
LOGGING_LEVEL_COM_SEVENELEVEN: INFO
```

#### Cấu hình Disk cho Uploads:
1. Sau khi tạo service → **Settings** → **Disks**
2. Click **Add Disk**:
   ```
   Name: uploads
   Mount Path: /app/uploads
   Size: 1GB
   ```
3. Click **Save Changes**

#### Health Check:
```
Path: /actuator/health
```

---

### PHẦN 4: Frontend (Netlify hoặc Vercel)

#### Cách 1: Netlify

1. Upload lên GitHub thư mục `frontend`
2. Login [netlify.com](https://netlify.com)
3. **Add new site** → **Import from Git**
4. Cấu hình:
   ```
   Branch to deploy: main
   Base directory: frontend
   Build command: npm run build
   Publish directory: dist
   ```

5. **Environment Variables**:
   ```
   Key: VITE_API_URL
   Value: https://seven-eleven-api.onrender.com
   ```

6. **netlify.toml** (tạo trong thư mục frontend):
   ```toml
   [build]
     command = "npm run build"
     publish = "dist"

   [[redirects]]
     from = "/*"
     to = "/index.html"
     status = 200

   [build.environment]
     NODE_VERSION = "20"
   ```

#### Cách 2: Vercel

1. Login [vercel.com](https://vercel.com)
2. **Add New** → **Project**
3. Import thư mục `frontend`
4. Cấu hình:
   ```
   Framework Preset: Vite
   Root Directory: ./frontend
   Build Command: npm run build
   Output Directory: dist
   ```

5. **Environment Variables**:
   ```
   Key: VITE_API_URL
   Value: https://seven-eleven-api.onrender.com
   ```

6. Deploy!

---

## 🔧 CẬP NHẬT CORS SAU KHI CÓ FRONTEND URL

Sau khi deploy Frontend thành công, cập nhật Backend:

1. Vào Render Dashboard → **seven-eleven-api** → **Environment**
2. Cập nhật `CORS_ORIGINS`:
   ```
   CORS_ORIGINS: https://your-frontend.netlify.app,https://your-frontend.vercel.app
   ```
3. Click **Save Changes** → Backend sẽ tự redeploy

---

## 📝 CÁC API ENDPOINTS SAU KHI DEPLOY

### Authentication
```
POST /api/auth/login      - Đăng nhập
POST /api/auth/refresh    - Refresh token
GET  /api/auth/me         - Lấy thông tin user hiện tại
```

### Products (Public)
```
GET  /api/products        - Danh sách sản phẩm (public)
GET  /api/products/{id}   - Chi tiết sản phẩm (public)
```

### Products (Admin)
```
POST   /api/products      - Tạo sản phẩm
PUT    /api/products/{id} - Cập nhật sản phẩm
DELETE /api/products/{id} - Xóa sản phẩm
```

### Cart (User)
```
GET    /api/cart          - Xem giỏ hàng
POST   /api/cart          - Thêm vào giỏ
PUT    /api/cart/{id}     - Cập nhật số lượng
DELETE /api/cart/{id}     - Xóa khỏi giỏ
DELETE /api/cart          - Xóa toàn bộ giỏ
```

### Orders (User)
```
POST /api/orders                    - Tạo đơn hàng
POST /api/orders/from-cart          - Tạo đơn từ giỏ
GET  /api/orders/my-orders          - Đơn của tôi
GET  /api/orders/my-orders/{id}     - Chi tiết đơn của tôi
```

### Orders (Admin)
```
GET  /api/orders          - Danh sách đơn hàng
GET  /api/orders/{id}     - Chi tiết đơn
PUT  /api/orders/{id}/status - Cập nhật trạng thái
```

### Upload (Admin)
```
POST /api/uploads/products - Upload ảnh sản phẩm
```

### Documentation
```
GET /swagger-ui.html       - Swagger UI
GET /api-docs             - OpenAPI JSON
```

---

## 🐛 XỬ LÝ SỰ CỐ

### Lỗi "Connection refused" Database
- Kiểm tra PostgreSQL đã sleep chưa (90 phút)
- Kiểm tra `DB_HOST` đúng format: `dpg-xxxxx-xxxxx-xxxxx-xxxxx-xxxxx.onrender.com`

### Lỗi Redis Connection
- Đảm bảo `REDIS_ENABLED=false` và `CACHE_TYPE=simple`

### Lỗi CORS
- Kiểm tra `CORS_ORIGINS` có chứa URL frontend đầy đủ (bao gồm `https://`)

### Lỗi Upload ảnh
- Kiểm tra Disk đã được mount đúng path `/app/uploads`
- Kiểm tra file size không vượt quá 5MB

### Lỗi 404 trên Frontend
- Đảm bảo Netlify/Vercel đã cấu hình rewrite sang index.html

---

## 💡 MẸO TỐI ƯU

1. **Free Tier Sleep**: Backend free tier sleep sau 15 phút không dùng
   - Lần đầu gọi API sẽ mất ~30-40 giây khởi động
   - Khắc phục: Nâng plan hoặc dùng Uptime Robot để ping định kỳ

2. **PostgreSQL Sleep**: Database sleep sau 90 phút
   - Khắc phục: Nâng plan hoặc dùng cron job ping database

3. **Environment Variables**: KHÔNG commit file `.env` lên GitHub
   - Tạo file `.env.example` với template

---

## 📞 LIÊN HỆ HỖ TRỢ

Nếu gặp lỗi, kiểm tra:
1. Render Dashboard → Logs của service gặp lỗi
2. Backend: Kiểm tra tab **Logs**
3. Database: Kiểm tra tab **Metrics**

---
