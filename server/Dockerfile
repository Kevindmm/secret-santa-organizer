# ============================================
# BUILD STAGE - using Alpine for smaller image
# ============================================
FROM maven:3.9-eclipse-temurin-25-alpine AS builder
WORKDIR /app
COPY pom.xml .
# Download dependencies first (cached layer)
RUN mvn dependency:go-offline -B
COPY src ./src
# Build the application
RUN mvn clean package -DskipTests -B

# ============================================
# RUNTIME STAGE - minimal JRE Alpine image
# ============================================
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Install timezone data (optional, for proper datetime handling)
RUN apk add --no-cache tzdata

# Copy the built jar from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]