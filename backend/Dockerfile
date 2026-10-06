# Dolphin backend - production image (used by Render).
# Build stage: compile the Spring Boot jar. Tests are run locally / in CI, not during the image build.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# Run stage: JRE only, non-root user.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home dolphin
COPY --from=build /app/target/*.jar app.jar
USER dolphin
# Fit the JVM into small instances (Render free tier: 512 MB).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
