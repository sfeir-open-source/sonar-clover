run-test: ## Allows to run all unit tests
	@docker run --mount type=bind,src=$$(pwd),target=/usr/src -w /usr/src maven:alpine mvn test

build-package: ## Allows to build artifacts
	@docker run --mount type=bind,src=$$(pwd),target=/usr/src -w /usr/src maven:alpine mvn package

quality-analysis: build-package ## Allows to run static quality analyis
	@docker run --mount type=bind,src=$$(pwd),target=/usr/src -w /usr/src maven:alpine mvn sonar:sonar \
	-Dsonar.host.url=$$SONAR_HOST_URL \
	-Dsonar.login=$$SONAR_TOKEN \
	-Dsonar.projectVersion=$$CURRENT_VERSION \
	-Dsonar.projectKey=$$SONAR_PROJECT_KEY \
  	-Dsonar.organization=$$SONAR_ORGANIZATION \
	-Dsonar.analysis.buildNumber=$$TRAVIS_BUILD_NUMBER \
	-Dsonar.analysis.pipeline=$$TRAVIS_BUILD_NUMBER \
	-Dsonar.analysis.sha1=$$TRAVIS_COMMIT  \
	-Dsonar.analysis.repository=$$TRAVIS_REPO_SLUG

deploy-package: ## Allows to deploy artifacts to our registry
	@echo "Not implemented yet"


.DEFAULT_GOAL := help
.PHONY: test help
help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'
