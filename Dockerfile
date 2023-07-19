FROM eclipse-temurin:17-jdk-alpine as build
WORKDIR /x2d7751347m/ktor-kafka

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN ./mvnw install -DskipTests
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
ARG DEPENDENCY=/x2d7751347m/ktor-kafka/target/dependency
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /ktor-kafka/lib
COPY --from=build ${DEPENDENCY}/META-INF /ktor-kafka/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /ktor-kafka
ENTRYPOINT ["java","-cp","ktor-kafka:ktor-kafka/lib/*","hello.Application"]