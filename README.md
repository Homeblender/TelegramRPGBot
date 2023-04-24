# TelegramRPGBot

## How to deploy

### Requirements

- Jdk 17
- Postgresql
- Maven
- Doker
____
### Set up properties

>Create telegram bot

- Using BotFather create telegram bot and save username and token

>Set environment variables in application.properties

- Add database connection properties
- Add telegram bot username and token to properties
____
### Database

>Run file sql/script.sql in your postgresql database to create database
____
### Build

>To build, run the command in the root of the project:
```
mvn spring-boot:build-image
```
____
### Run

>Run application using command:
```
docker run -p 8080:8080 spring-boot-docker:telegramBot
```
 
