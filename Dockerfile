FROM openjdk:23-jdk-slim
EXPOSE 8080
WORKDIR /app
COPY temp-messages.jar ./temp-messages.jar
ENV SPRING_PROFILES_ACTIVE=production
ENV VAADIN_PRODUCTIONMODE=true
ENTRYPOINT ["java", "-jar", "temp-messages.jar"]