#!/bin/bash
mvn release:prepare release:perform -DskipTests -Dmaven.release.plugin.arguments="-DskipTests -Dspotbugs.skip=true" -DlocalCheckout=true -B

echo =========================================================================
echo At this point you need to log on to https://oss.sonatype.org/
echo =========================================================================
 
