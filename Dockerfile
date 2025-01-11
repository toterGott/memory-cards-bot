FROM eclipse-temurin:23-jdk-alpine

COPY build/libs/memory-cards-0.0.1-SNAPSHOT.jar /app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "/app.jar"]