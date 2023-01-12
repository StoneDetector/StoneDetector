#!/bin/sh

java -Xms3G -Xmx3G  -jar build/libs/StoneDetector.jar -x --directory="$1" --error-file=errors.txt 2>/dev/null
