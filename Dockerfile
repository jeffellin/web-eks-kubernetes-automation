# Created the Dockerfile as requested even though CNB was
# used to produce the image.
FROM eclipse-temurin:17-jre
VOLUME /tmp
COPY target/demo-0.0.1-SNAPSHOT.jar /app.jar
COPY wizexercise.txt /tmp/wizexercise.txt
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
