FROM openjdk:11-jre

LABEL org.opencontainers.image.source=https://github.com/provenance-io/service-asset-onboarding

ADD ./build/libs/service*.jar /service-asset-onboarding.jar
RUN chmod +x /service-asset-onboarding.jar
ADD ./docker/configure /configure

EXPOSE 8080/tcp

ENTRYPOINT ["/configure"]
CMD ["java", "$JAVA_OPTS", "-jar", "/service-asset-onboarding.jar"]
