# Build stage
FROM node:20-alpine AS builder
WORKDIR /app

# Vì context đã là ./frontend nên ta copy trực tiếp file trong đó
COPY package*.json ./
RUN npm ci

COPY . ./
RUN npm run build

# Production stage
FROM nginx:alpine

# Copy nginx config (file này nằm cùng cấp Dockerfile)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built files
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]