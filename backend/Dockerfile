# ------------------ Build Stage ------------------
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# ------------------ Run Stage ------------------
FROM quay.io/wildfly/wildfly:38.0.1.Final-jdk21

USER root

# Installer le driver PostgreSQL
RUN mkdir -p /opt/jboss/wildfly/modules/org/postgresql/main
ADD https://jdbc.postgresql.org/download/postgresql-42.7.3.jar \
    /opt/jboss/wildfly/modules/org/postgresql/main/postgresql-42.7.3.jar
COPY module.xml /opt/jboss/wildfly/modules/org/postgresql/main/
RUN chown -R jboss:root /opt/jboss/wildfly/modules/org/postgresql

# Copier l'entrypoint
COPY entrypoint.sh /opt/jboss/wildfly/
RUN chmod +x /opt/jboss/wildfly/entrypoint.sh

USER jboss

# Copier le WAR
COPY --from=build /app/target/secureteam-access.war \
    /opt/jboss/wildfly/standalone/deployments/

EXPOSE 8080 9990

ENTRYPOINT ["/opt/jboss/wildfly/entrypoint.sh"]
