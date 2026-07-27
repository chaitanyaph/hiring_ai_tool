# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src src
RUN mvn -q clean package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S cadence && adduser -S cadence -G cadence
COPY --from=build /workspace/target/candidate-service-*.jar app.jar
RUN mkdir -p /app/resume-uploads && chown -R cadence:cadence /app
USER cadence

EXPOSE 8085
ENV JAVA_OPTS=""

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8085/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
