FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY services/notification-service/.mvn/ ./.mvn/
COPY services/notification-service/mvnw ./
COPY services/notification-service/pom.xml ./

RUN chmod +x ./mvnw

RUN ./mvnw dependency:go-offline

COPY services/notification-service/src/ ./src/
COPY shared/protos/ /shared/protos/

RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar ./app.jar

EXPOSE 8084
EXPOSE 9091

ENTRYPOINT ["java", "-jar", "app.jar"]