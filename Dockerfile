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

# Heap proporcional al limite de memoria del contenedor; ante un
# OutOfMemoryError el proceso termina y Docker lo reinicia (mejor que quedar
# vivo pero inservible).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

# 8080: API · 9091: actuator en produccion (solo red interna, ver docker-compose.prod.yml)
EXPOSE 8080 9091
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
