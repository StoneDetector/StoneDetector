#!/bin/sh

cd BigCloneEval/commands
./registerTool -n="StoneDetector" -d "StoneDetector"
./detectClones -m=10000 -o="clones.csv" -r="../../BCE_runner"
./clearClones -t=1
./importClones -c="clones.csv" -t=1
./evaluateTool -t=1 -o="../../BigCloneEval_Report.txt"