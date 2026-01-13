# Build Stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run Stage
FROM quay.io/wildfly/wildfly:38.0.1.Final-jdk21

# Copy the WAR file from the build stage
COPY --from=build /app/target/secureteam-access.war /opt/jboss/wildfly/standalone/deployments/

# Expose HTTP and Admin ports
EXPOSE 8080 9990

# Start WildFly in standalone mode
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]
