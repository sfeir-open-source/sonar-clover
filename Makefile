DOCKER_IMG=openjdk:11.0.11-jdk

run-test: ## Allows to run all unit tests
	@docker run --mount type=bind,src=$$(pwd),target=/usr/src -w /usr/src $(DOCKER_IMG) ./mvnw test --batch-mode

build-package: ## Allows to build artifacts
	@docker run --mount type=bind,src=$$(pwd),target=/usr/src -w /usr/src $(DOCKER_IMG) ./mvnw package --batch-mode

quality-analysis: build-package ## Allows to run static quality analyis
	@docker run --mount type=bind,src=$$(pwd),target=/usr/src -w /usr/src $(DOCKER_IMG) ./mvnw sonar:sonar \
	-Dsonar.host.url=https://sonarcloud.io \
	-Dsonar.login=$$SONAR_TOKEN \
	-Dsonar.projectKey=sfeir-open-source_sonar-clover \
  	-Dsonar.organization=sfeir-open-source \
  	-Dsonar.branch.name=$$GITHUB_REF \
	-Dsonar.analysis.buildNumber=$$GITHUB_RUN_NUMBER \
	-Dsonar.analysis.pipeline=$$GITHUB_RUN_ID \
	-Dsonar.analysis.sha1=$$GITHUB_SHA  \
	-Dsonar.analysis.repository=$$GITHUB_REPOSITORY --batch-mode

run-integration-platform: build-package ## Allows to run local integrations test with docker
	@docker network create sonar || \
	docker build --build-arg VERSION=$$VERSION --tag test-instance . && \
	docker run -p 9000:9000 --name sonar-instance --net sonar --rm test-instance

run-integration-test: ## Allows to push a report in integration platform
	@docker run --mount type=bind,src=$$(pwd)/its/integration,target=/usr/src -w /usr/src --net sonar $(DOCKER_IMG) \
	 ./mvnw clean clover:setup test clover:aggregate clover:clover sonar:sonar -Dsonar.sources=src -Dsonar.host.url=http://sonar-instance:9000 --batch-mode

deploy-package: ## Allows to deploy artifacts to our registry
	@docker run --mount type=bind,src=$$(pwd),target=/usr/src -w /usr/src -e GPG_PASSPHRASE -e ARTIFACT_REGISTRY_USER -e ARTIFACT_REGISTRY_PASSWORD $(DOCKER_IMG) \
	sh -c "echo \"$$GPG_PRIVATE_KEY\" > ~/secret.txt && \
	gpg -v --batch --import < ~/secret.txt && \
    ./mvnw deploy --settings github.settings.xml --batch-mode"


.DEFAULT_GOAL := help run-integration-platform
.PHONY: test help
help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'
