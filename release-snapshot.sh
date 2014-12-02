#/bin/bash
set -e
mvn deploy -Drisky.repo=amsa-maven -Dmode=dev -Drisky.repo.url=http://maven.amsa.gov.au:8081/artifactory/libs-snapshots-local
