FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system easyfinance \
    && useradd --system --gid easyfinance --home-dir /app --shell /usr/sbin/nologin easyfinance

COPY --from=build --chown=easyfinance:easyfinance /workspace/target/easy-finance-backend-*.jar app.jar

USER easyfinance

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
