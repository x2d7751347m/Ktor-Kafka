FROM eclipse-temurin:17-jdk-alpine
EXPOSE 8080
VOLUME /tmp
RUN mkdir build/libs/extracted
ARG EXTRACTED=build/libs/extracted
COPY ${EXTRACTED}/dependencies/ ./
COPY ${EXTRACTED}/spring-boot-loader/ ./
COPY ${EXTRACTED}/snapshot-dependencies/ ./
COPY ${EXTRACTED}/application/ ./
ENTRYPOINT ["java","org.springframework.boot.loader.JarLauncher"]