FROM openjdk:8-jdk-alpine
COPY /build/libs/hcxproviderconsumer-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
EXPOSE 8095