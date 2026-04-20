# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and download dependencies first (this layer is cached)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Run
FROM eclipse-temurin:17-jre
WORKDIR /app

# Create a non-root user for security
RUN useradd -m bypassuser
USER bypassuser

COPY --from=build /app/target/bypasstransers-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# Use the PORT environment variable if provided by Render, default to 8080
ENTRYPOINT ["java", "-Xms128m", "-Xmx320m", "-Dserver.port=${PORT:8080}", "-jar", "app.jar"]

