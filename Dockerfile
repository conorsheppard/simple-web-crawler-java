# Build stage with full JDK
FROM eclipse-temurin:23-jdk AS builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -q -DskipTests

# Runtime stage with only JRE
FROM eclipse-temurin:23-jre
ENV ENVIRONMENT=prod
WORKDIR /app
COPY --from=builder /app/target/simple-web-crawler-java-1.0-SNAPSHOT.jar simple-web-crawler-java.jar
ENTRYPOINT ["java", "-jar", "simple-web-crawler-java.jar"]
