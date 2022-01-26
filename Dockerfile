ARG VERSION=latest
FROM sonarqube:${VERSION}

COPY target/sonar-clover-plugin.jar /opt/sonarqube/extensions/plugins/
