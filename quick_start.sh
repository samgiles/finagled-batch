#!/bin/sh

MVN=`which mvn`

if [ "$MVN" = "" ]; then
	echo "Ensure maven (mvn) is installed and available on your PATH\n"
	exit 1
fi

mvn package

echo '#!/bin/sh\njava -jar target/finagled_batch-0.0.1-SNAPSHOT-jar-with-dependencies.jar' > run.sh
chmod +x run.sh
