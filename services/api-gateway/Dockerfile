FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY services/api-gateway/.mvn/ ./.mvn/
COPY services/api-gateway/mvnw ./
COPY services/api-gateway/pom.xml ./

RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline

COPY services/api-gateway/src/ ./src/

RUN ./mvnw clean package -DskipTests


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]