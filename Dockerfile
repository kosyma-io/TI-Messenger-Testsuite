FROM maven:3.6.3-openjdk-17-slim
#FROM maven:3.9.9-eclipse-temurin-23-alpine
WORKDIR /testsuite
COPY --chown=user:group . /testsuite
RUN mvn clean compile test-compile


