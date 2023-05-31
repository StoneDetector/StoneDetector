#!/bin/sh

cd BigCloneEval/commands
./detectClones -m=100000 -o="clones.csv" -r="../../BCE_runner"
./clearClones -t=1
./importClones -c="clones.csv" -t=1
./evaluateTool -t=1 --mil=15 -o="../../BigCloneEval_Report.txt"
