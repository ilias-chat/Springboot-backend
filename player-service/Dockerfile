# Build from repo root: docker build -f player-service/Dockerfile .
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY discovery-server ./discovery-server
COPY config-server ./config-server
COPY player-service ./player-service
COPY comment-service ./comment-service
COPY src ./src
RUN mvn -B -pl player-service -am package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=build /app/player-service/target/*.jar /app/app.jar
RUN chown spring:spring /app/app.jar
USER spring:spring
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
