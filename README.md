Sonar Clover 
===========
[![Build Status](https://travis-ci.org/sfeir-open-source/sonar-clover.svg?branch=master)](https://travis-ci.org/sfeir-open-source/sonar-clover)

## Description / Features
It provides the ability to feed SonarQube with code coverage data coming from Atlassian Clover or it's new open source version: [OpenClover](http://openclover.org/).

## Usage
To display code coverage data:

1. Prior to the SonarQube analysis, execute your unit tests and generate the [Clover report](http://openclover.org/doc/manual/latest/maven--quick-start-guide.html).

1. Import this report while running the SonarQube analysis by setting the sonar.clover.reportPath (using prior version to sonarQube version 6)
 or sonar.coverageReportPaths (sonarQube v7 or higher, FYI with this version, only the xml format is supported) property to the path to the Clover report. 
The path may be absolute or relative to the project base directory.

## 
