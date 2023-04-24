FROM openjdk:17.0.2-jdk-slim-buster

COPY target/TelegramRPGBot-0.0.1-SNAPSHOT.jar /demo.jar

CMD ["java","-jar","/demo.jar"]