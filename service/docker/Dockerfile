FROM openjdk:11

LABEL org.opencontainers.image.source=https://github.com/provenance-io/service-asset-onboarding

ADD ./build/libs/*.jar /service-asset-onboarding.jar
ADD ./docker/docker-entrypoint.sh /docker-entrypoint.sh

EXPOSE 8080/tcp

ENTRYPOINT ./docker-entrypoint.sh /service-asset-onboarding.jar
