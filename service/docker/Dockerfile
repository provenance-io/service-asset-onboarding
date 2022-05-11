FROM openjdk:11

LABEL org.opencontainers.image.source=https://github.com/provenance-io/service-asset-onboarding

ADD ./build/libs/*.jar /service-asset-onboarding.jar
RUN chmod +x /service-asset-onboarding.jar
ADD ./docker/configure /configure

EXPOSE 8080/tcp

ENTRYPOINT ./docker-entrypoint.sh /service-asset-onboarding.jar
