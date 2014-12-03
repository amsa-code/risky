#!/bin/bash
set -e
if [ $# -eq 0 ]; then
    echo "Usage: ./release.sh <VERSION>"
    exit 1
fi
VERSION=$1
git checkout master
git pull
mvn versions:set -DnewVersion=$VERSION
mvn clean install 
git commit -am "prepare for release $VERSION"
git push
git tag -a $VERSION -m "$VERSION"
git push origin $VERSION
git checkout $VERSION
mvn deploy -Dmode=test -Drisky.repo=amsa-maven -Drisky.repo.url=http://sardevc.amsa.gov.au:8081/artifactory/libs-releases-local
git checkout master
mvn versions:set -DnewVersion=$VERSION.1-SNAPSHOT
git commit -am "setting versions to snapshot"
git push

## could not get release plugin to work for git push!
#GIT_USERNAME=$2
#GIT_PASSWORD=$3
#DRY_RUN=false
#git pull
#mvn release:clean
#mvn -B -X release:prepare -DreleaseVersion=$VERSION -DautoVersionSubmodules=true -Dtag=$VERSION -DdryRun=#$DRY_RUN -Dusername=$GIT_USERNAME -Dpassword=$GIT_PASSWORD
#mvn release:perform -X -Darguments='-Drisky.repo=amsa-maven -Dmode=test -Drisky.repo.url=http://sardevc.amsa.gov.au:8081/artifactory/libs-releases-local' -DdryRun=$DRY_RUN -Dusername=$GIT_USERNAME -#Dpassword=$GIT_PASSWORD
