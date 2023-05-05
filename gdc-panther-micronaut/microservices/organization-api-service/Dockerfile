# (C) 2023 GoodData Corporation
FROM openjdk:11-jdk as builder
WORKDIR /app
COPY ./build/libs/organization-api-service-0.1-all.jar /app/organization-api-service.jar
RUN jar xf /app/organization-api-service.jar

FROM openjdk:11-jre-slim-bullseye
RUN apt-get update && apt-get install -y curl

ENV SPRING_PROFILES_ACTIVE="k8s"
RUN groupadd -g 1112 organization-api-service && \
    useradd -r -u 1112 -g organization-api-service organization-api-service
USER organization-api-service

WORKDIR /app
COPY --from=builder /app/* /app

RUN ls /app

ENTRYPOINT exec java $JAVA_OPTS -Duser.home=${HOME} -Djava.security.egd=file:/dev/./urandom -jar /app/organization-api-service.jar

