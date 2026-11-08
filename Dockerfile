# ---------- Etapa 1: compilacion ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Etapa 2: imagen final ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Punto de montaje del EFS (docker run -v /mnt/efs:/app/efs)
RUN mkdir -p /app/efs
VOLUME /app/efs

COPY --from=build /build/target/tickets-service.jar app.jar

ENV EFS_PATH=/app/efs \
    AWS_REGION=us-east-1 \
    S3_AUTO_UPLOAD=true

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
