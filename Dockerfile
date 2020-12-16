FROM openjdk:8-alpine

COPY target/uberjar/competite.jar /competite/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/competite/app.jar"]
