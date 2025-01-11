FROM eclipse-temurin:23-jdk-alpine

COPY 'build/libs/memory-cards-[0-9]*.jar' /app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "/app.jar"]