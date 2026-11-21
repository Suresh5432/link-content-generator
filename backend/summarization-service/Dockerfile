FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY backend ./backend
RUN mvn -B -ntp -pl backend/summarization-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S -g 10001 app && adduser -S -D -H -u 10001 -G app app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/backend/summarization-service/target/*.jar /app/service.jar
USER 10001:10001
EXPOSE 8083
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"
HEALTHCHECK --interval=20s --timeout=3s --start-period=45s --retries=5 \
  CMD wget -q -O /dev/null http://127.0.0.1:8083/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/service.jar"]
