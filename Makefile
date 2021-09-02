DOCKER_IMG=openjdk:11.0.11-jdk

run-test: ## Allows to run all unit tests
	@docker run --mount type=bind,src=$$(pwd),target=/usr/src -w /usr/src $(DOCKER_IMG) ./mvnw test --batch-mode

build-package: ## Allows to build artifacts
	@docker run --mount type=bind,src=$$(pwd),target=/usr/src -w /usr/src $(DOCKER_IMG) ./mvnw package --batch-mode

quality-analysis: build-package ## Allows to run static quality analyis
	@docker run --mount type=bind,src=$$(pwd),target=/usr/src -w /usr/src \
	-e SONAR_TOKEN -e GITHUB_REF -e GITHUB_RUN_NUMBER -e GITHUB_RUN_ID -e GITHUB_SHA -e GITHUB_REPOSITORY \
	$(DOCKER_IMG) ./mvnw sonar:sonar --settings github.settings.xml --batch-mode

run-integration-platform: build-package ## Allows to run local integrations test with docker
	@docker network create sonar || \
	docker build --build-arg VERSION=$$VERSION --tag test-instance . && \
	docker run -p 9000:9000 --name sonar-instance --net sonar --rm test-instance

run-integration-test: ## Allows to push a report in integration platform
	@docker run --mount type=bind,src=$$(pwd)/its/integration,target=/usr/src -w /usr/src --net sonar $(DOCKER_IMG) \
	 ./mvnw clean clover:setup test clover:aggregate clover:clover sonar:sonar -Dsonar.sources=src -Dsonar.host.url=http://sonar-instance:9000 \
	 -Dsonar.login=admin -Dsonar.password=admin --batch-mode

run-groovy-test: ## Allows to push groovyproject a report in integration platform
	@docker run --mount type=bind,src=$$(pwd)/its/groovyproject,target=/usr/src -w /usr/src --net sonar $(DOCKER_IMG) \
	 ./mvnw clean clover:setup test clover:aggregate clover:clover sonar:sonar -Dsonar.sources=src -Dsonar.host.url=http://sonar-instance:9000 \
	 -Dsonar.login=admin -Dsonar.password=admin --batch-mode

run-plugin-test: ## Allows to push plugin a report in integration platform
	@docker run --mount type=bind,src=$$(pwd)/its/plugin,target=/usr/src -w /usr/src --net sonar $(DOCKER_IMG) \
	 ./mvnw clean clover:setup test clover:aggregate clover:clover sonar:sonar -Dsonar.sources=src -Dsonar.host.url=http://sonar-instance:9000 \
	 -Dsonar.login=admin -Dsonar.password=admin --batch-mode

run-its-tests: run-integration-test run-groovy-test run-plugin-test ## Allows to push reports in integration platform

deploy-package: ## Allows to deploy artifacts to our registry
	@docker run --mount type=bind,src=$$(pwd),target=/usr/src -w /usr/src -e GPG_PASSPHRASE -e ARTIFACT_REGISTRY_USER -e ARTIFACT_REGISTRY_PASSWORD $(DOCKER_IMG) \
	sh -c "echo \"$$GPG_PRIVATE_KEY\" > ~/secret.txt && \
	gpg -v --batch --import < ~/secret.txt && \
    ./mvnw deploy --settings github.settings.xml --batch-mode"


.DEFAULT_GOAL := help

.PHONY: test help
help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'
