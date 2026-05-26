# Build from repo root: docker build -f config-server/Dockerfile .
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY discovery-server ./discovery-server
COPY config-server ./config-server
COPY player-service ./player-service
COPY comment-service ./comment-service
RUN mvn -B -pl config-server -am package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=build /app/config-server/target/*.jar /app/app.jar
COPY config-repo /app/config-repo
ENV SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCH_LOCATIONS=file:/app/config-repo
RUN chown -R spring:spring /app
USER spring:spring
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
