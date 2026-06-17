# Stage 1: Compile the application cleanly inside a Maven container
FROM maven:3.9.6-eclipse-temurin-17 AS build-stage
WORKDIR /app

# Copy configuration and code layers
COPY pom.xml .
COPY src ./src

# Download dependencies and compile the war file
RUN mvn clean package -DskipTests

# Stage 2: Spin up Tomcat and copy the compiled output over
FROM tomcat:10-jdk17-temurin
WORKDIR /usr/local/tomcat

# Grab the WAR file built in Stage 1
COPY --from=build-stage /app/target/dashboard-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/dashboard.war

EXPOSE 8080
CMD ["catalina.sh", "run"]
