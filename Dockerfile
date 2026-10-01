FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -q -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S oncologia && adduser -S oncologia -G oncologia
COPY --from=build /app/target/*.jar app.jar
USER oncologia

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
