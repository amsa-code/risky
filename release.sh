#!/bin/bash
set -e
set -x
if [ $# -eq 0 ]; then
    echo "Usage: ./release.sh <VERSION>"
    exit 1
fi
VERSION=$1
git checkout master
git pull
mvn versions:set -DnewVersion=$VERSION -DgenerateBackupPoms=false
mvn clean install 
git commit -am "prepare for release $VERSION"
git push
git tag -a $VERSION -m "$VERSION"
git push origin $VERSION
git checkout $VERSION
mvn deploy -Dmode=test -Drisky.repo=amsa-maven -Drisky.repo.url=http://mvlrep001.infra.amsa.gov.au:8081/artifactory/libs-releases-local
git checkout master
mvn versions:set -DnewVersion=$VERSION.1-SNAPSHOT -DgenerateBackupPoms=false
git commit -am "setting versions to snapshot"
git push
git checkout $VERSION
./generate-site.sh
git checkout master

