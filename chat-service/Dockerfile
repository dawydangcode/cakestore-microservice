FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/chat-service-0.0.1-SNAPSHOT.jar chat-service.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "chat-service.jar"]