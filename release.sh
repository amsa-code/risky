#!/bin/bash
set -e
VERSION=$1
DRY_RUN=true
git pull
mvn release:clean
mvn -B release:prepare -DreleaseVersion=$VERSION -DautoVersionSubmodules=true -Dtag=$VERSION -DdryRun=$DRY_RUN
mvn release:perform -Darguments='-Drisky.repo=amsa-maven -Dmode=dev -Drisky.repo.url=http://sardevc.amsa.gov.au:8081/artifactory/libs-snapshots-local' -DdryRun=$DRY_RUN
