FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY . .
RUN mvn -B -pl portroyal_be -am package -DskipTests

FROM eclipse-temurin:21-jre

ENV TZ="Europe/Rome"

WORKDIR /app
COPY --from=build /workspace/portroyal_be/target/portroyal_be-*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
