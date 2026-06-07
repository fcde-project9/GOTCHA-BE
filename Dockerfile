FROM gradle:8-jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY src ./src
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre-noble
RUN apt-get update && apt-get install -y --no-install-recommends libheif-examples && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENV TZ=Asia/Seoul
EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]
