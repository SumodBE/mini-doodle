# STAGE 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# 1. Copy the "Engine" (Gradle files)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 2. Copy the "Parts" (All modules)
# We copy the folders exactly as they are named in your structure
COPY mini-doodle-service mini-doodle-service
COPY mini-doodle-service-specs mini-doodle-service-specs

# 3. Grant permission to run the build script
RUN chmod +x gradlew

# 4. Build only the service module
# This will automatically trigger the build of 'specs' if 'service' depends on it
RUN ./gradlew :mini-doodle-service:bootJar -x test

# STAGE 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# The JAR will be located in the build folder of the service sub-module
COPY --from=build /app/mini-doodle-service/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]