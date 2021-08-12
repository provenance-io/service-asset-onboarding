FROM openjdk:11-jre-slim

ADD build/libs/service*.jar /service-asset-onboarding.jar
ADD ./docker/docker-entrypoint.sh /docker-entrypoint.sh

RUN apt-get update && apt-get install -y procps

EXPOSE 8080/tcp

ENTRYPOINT ./docker-entrypoint.sh /service-asset-onboarding.jar
