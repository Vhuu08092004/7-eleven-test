# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy from backend folder (Dockerfile is in backend/, build context is root)
COPY backend/pom.xml .
COPY backend/src ./src

RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create uploads directory
RUN mkdir -p /app/uploads

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# UPLOAD_PATH: /app/uploads (Render disk mount)
ENTRYPOINT ["sh", "-c", "java -jar app.jar -Dupload.path=${UPLOAD_PATH:-/app/uploads}"]
