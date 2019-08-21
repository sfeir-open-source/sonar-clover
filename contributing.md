## Contributions

All contributions have to be test using a manual (for now) process

## Test

Each contributions should allows us to use this plugin in version LTS and Latest

In order to test with different version of SonarQube, you could use:
```bash
VERSION=<myVersion> make run-integration-platform
``` 
This command will start a sonarQube container with version : myVersion (`lts` or `latest` could be used)

And run 
```bash
make run-integration-test
```
Which will do a mvn build on a test project and upload the report in the sonarQube container created above.
