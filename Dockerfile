ARG VERSION=latest
FROM sonarqube:${VERSION}

COPY target/sonar-clover-plugin.jar /opt/sonarqube/extensions/plugins/
ADD https://github.com/Inform-Software/sonar-groovy/releases/download/1.8/sonar-groovy-plugin-1.8.jar /opt/sonarqube/extensions/plugins/
