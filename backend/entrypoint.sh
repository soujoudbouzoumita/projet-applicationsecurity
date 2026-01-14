#!/bin/bash
set -e

echo "Waiting for PostgreSQL to be ready..."
sleep 10

echo "Starting WildFly in admin-only mode to configure datasource..."

# Démarrer WildFly en mode admin-only (sans déployer les applications)
/opt/jboss/wildfly/bin/standalone.sh --admin-only -b 0.0.0.0 &
WILDFLY_PID=$!

# Attendre que le management interface soit prêt
echo "Waiting for WildFly management interface..."
until /opt/jboss/wildfly/bin/jboss-cli.sh --connect --command=":read-attribute(name=server-state)" 2>/dev/null | grep -q "running"; do
    sleep 2
done

echo "Configuring PostgreSQL datasource..."

# Créer le driver et le datasource
/opt/jboss/wildfly/bin/jboss-cli.sh --connect <<EOF
/subsystem=datasources/jdbc-driver=postgresql:add(driver-name=postgresql,driver-module-name=org.postgresql,driver-class-name=org.postgresql.Driver)
data-source add --name=PostgresDS --jndi-name=java:/PostgresDS --driver-name=postgresql --connection-url=${DB_URL} --user-name=${DB_USER} --password=${DB_PASSWORD} --enabled=true --use-ccm=true --min-pool-size=5 --max-pool-size=20 --blocking-timeout-wait-millis=5000
shutdown
EOF

# Attendre que WildFly s'arrête proprement
wait $WILDFLY_PID

echo "Datasource configured successfully. Starting WildFly in normal mode..."

# Démarrer WildFly en mode normal (avec déploiement des applications)
exec /opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0