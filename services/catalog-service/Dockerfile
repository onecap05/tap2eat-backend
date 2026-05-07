FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY services/catalog-service/pom.xml .

RUN mvn -B dependency:go-offline

COPY services/catalog-service/src ./src

RUN mvn -B clean package -DskipTests


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]