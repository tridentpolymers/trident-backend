FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
COPY data ./data

RUN mvn -q -DskipTests package


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/trident-backend-0.0.1-SNAPSHOT.jar app.jar
COPY --from=build /app/data ./data

EXPOSE 8001

CMD ["java", "-jar", "app.jar"]