# (C) 2023 GoodData Corporation
FROM openjdk:11-jre-slim-bullseye as builder
WORKDIR /app
COPY ./build/libs/*.jar /app/organization-api-service.jar
RUN java -Djarmode=layertools -jar /app/organization-api-service.jar extract

FROM openjdk:11-jre-slim-bullseye
RUN apt-get update && apt-get install -y curl

ENV SPRING_PROFILES_ACTIVE="k8s"
RUN groupadd -g 1112 organization-api-service && \
    useradd -r -u 1112 -g organization-api-service organization-api-service
USER organization-api-service

WORKDIR /app
COPY --from=builder /app/dependencies/ /app
COPY --from=builder /app/spring-boot-loader/ /app
COPY --from=builder /app/snapshot-dependencies/ /app
COPY --from=builder /app/application/ /app

ENTRYPOINT exec java $JAVA_OPTS -Duser.home=${HOME} -Djava.security.egd=file:/dev/./urandom org.springframework.boot.loader.JarLauncher

