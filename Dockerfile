# ---- Build ----
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN mvn -B package -DskipTests

# ---- Runtime ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=build /app/target/*.jar /app/app.jar
RUN chown spring:spring /app/app.jar

USER spring:spring

# Cloud Run sets PORT; Spring reads server.port from env in application.properties
EXPOSE 8080

# Container-friendly defaults (tune via Cloud Run memory)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
