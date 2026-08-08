# Stage 1: Build the application
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper and pom.xml first
# Docker caches this layer — dependencies only re-downloaded if pom.xml changes
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the jar, skip tests (tests run in CI separately)
RUN ./mvnw clean package -DskipTests

# Stage 2: Run the application
# Smaller image — only JRE, not full JDK
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy only the built jar from Stage 1
COPY --from=builder /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]