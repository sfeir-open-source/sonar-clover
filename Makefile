run-test: ## Allows to run all unit tests
	@docker run --mount type=bind,src=$$(pwd),target=/usr/src -w /usr/src maven:alpine mvn test --batch-mode

build-package: ## Allows to build artifacts
	@./mvnw package --batch-mode

quality-analysis: build-package ## Allows to run static quality analyis
	@./mvnw sonar:sonar \
	-Dsonar.host.url=$$SONAR_HOST_URL \
	-Dsonar.login=$$SONAR_TOKEN \
	-Dsonar.projectKey=$$SONAR_PROJECT_KEY \
  	-Dsonar.organization=sfeir-open-source \
	-Dsonar.analysis.buildNumber=$$TRAVIS_BUILD_NUMBER \
	-Dsonar.analysis.pipeline=$$TRAVIS_BUILD_NUMBER \
	-Dsonar.analysis.sha1=$$TRAVIS_COMMIT  \
	-Dsonar.analysis.repository=$$TRAVIS_REPO_SLUG --batch-mode

run-integration-platform: build-package ## Allows to run local integrations test with docker
	@docker network create sonar || \
	docker build --build-arg VERSION=$$VERSION --tag test-instance . && \
	docker run -p 9000:9000 --name sonar-instance --net sonar --rm test-instance

run-integration-test: ## Allows to push a report in integration platform
	@docker run --mount type=bind,src=$$(pwd)/its/integration,target=/usr/src -w /usr/src --net sonar maven:alpine \
	 mvn clean clover:setup test clover:aggregate clover:clover sonar:sonar -Dsonar.sources=src -Dsonar.host.url=http://sonar-instance:9000 --batch-mode

deploy-package: ## Allows to deploy artifacts to our registry
	@./mvnw deploy --settings github.settings.xml --batch-mode


.DEFAULT_GOAL := help run-integration-platform
.PHONY: test help
help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'
