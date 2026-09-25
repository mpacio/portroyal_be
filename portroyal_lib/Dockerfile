FROM maven:3.9.8-eclipse-temurin-21

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean compile dependency:copy-dependencies

RUN mkdir -p csv_files

CMD ["java", "-cp", "target/classes:target/dependency/*", "com.matteopaciolla.portroyal.MainClass"]
