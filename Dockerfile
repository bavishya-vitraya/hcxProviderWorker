FROM openjdk:16-jdk-alpine
COPY /build/libs/hcxproviderconsumer-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
