Sonar Clover 
===========
[![Build Status](https://travis-ci.org/sfeir-open-source/sonar-clover.svg?branch=master)](https://travis-ci.org/sfeir-open-source/sonar-clover)

## Description / Features
It provides the ability to feed SonarQube with code coverage data coming from Atlassian Clover.

## Usage
To display code coverage data:

1. Prior to the SonarQube analysis, execute your unit tests and generate the Clover report.
1. Import this report while running the SonarQube analysis by setting the sonar.clover.reportPath property to the path to the Clover report. The path may be absolute or relative to the project base directory.
