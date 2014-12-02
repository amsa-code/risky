#!/bin/bash
set -e
if [ $# -eq 0 ]; then
    echo "Usage: ./release.sh <VERSION>"
    exit 1
fi
VERSION=$1
DRY_RUN=false
git pull
mvn release:clean
mvn -B release:prepare -DreleaseVersion=$VERSION -DautoVersionSubmodules=true -Dtag=$VERSION -DdryRun=$DRY_RUN
mvn release:perform -Darguments='-Drisky.repo=amsa-maven -Dmode=test -Drisky.repo.url=http://sardevc.amsa.gov.au:8081/artifactory/libs-releases-local' -DdryRun=$DRY_RUN
