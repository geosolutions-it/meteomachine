mvn clean install eclipse:clean eclipse:eclipse  -DdownloadSources=true  -DdownloadJavadocs=true  -Declipse.addVersionToProjectName=true -Pdao.xstream,kmzprocess -Dmaven.test.skip=true -e -o
