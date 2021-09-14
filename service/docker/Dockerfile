FROM openjdk:11-jre

ADD service/build/libs/service*.jar /service-asset-onboarding.jar
ADD service/docker/configure /configure

EXPOSE 8080/tcp

ENTRYPOINT ["/configure"]
CMD ["java", "$JAVA_OPTS", "-jar", "/service-asset-onboarding.jar"]