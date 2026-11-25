# ============================================
# BUILD STAGE - using Alpine for smaller image
# ============================================
FROM maven:3.9-eclipse-temurin-25-alpine AS builder
WORKDIR /app

# 🔧 FIX: Source code is in /server, not in the root
COPY server/pom.xml .
# Download dependencies first (cached layer)
RUN mvn dependency:go-offline -B

# 🔧 FIX: Copy source from /server/src
COPY server/src ./src

# Build the application (tests are compiled and run in CI, not in the image)
RUN mvn clean package -Dmaven.test.skip=true -B

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

# JVM tuned for a 0.1 CPU / 512 MB container:
# - SerialGC and a single compiler tier keep CPU and thread overhead low.
# - The heap stays at 55% to leave room for metaspace, stacks and native memory.
# - ActiveProcessorCount prevents the JVM from sizing pools for the host CPU.
ENTRYPOINT ["java", \
  "-XX:+UseSerialGC", \
  "-XX:MaxRAMPercentage=55", \
  "-XX:MaxMetaspaceSize=128m", \
  "-XX:ReservedCodeCacheSize=64m", \
  "-XX:ActiveProcessorCount=2", \
  "-Xss512k", \
  "-XX:TieredStopAtLevel=1", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "app.jar"]
