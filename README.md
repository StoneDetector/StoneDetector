# StoneDetector - Finding Structural Clones and Subclones in Java Source Code

If you just want to try out StoneDetector, you can also have a look at the tool's interactive website: [stonedetector.fmi.uni-jena.de](https://stonedetector.fmi.uni-jena.de).

## StoneDetector Quick Start
* Adjust StoneDetector's configuration file under `config/default.properties`
* Build StoneDetector (requires at least JDK11)
```
./gradlew jar
```
* Run StoneDetector
```
java -Xms8G -Xmx8G -jar build/libs/StoneDetector.jar -x --directory="path/to/Java/Folder" --error-file=errors.txt 
```
* StoneDetector with [BigCloneEval](https://github.com/jeffsvajlenko/BigCloneEval):
 Use `BCE_runner` as tool runner script

## Docker Image

The StoneDetector tool is available as [Docker image](https://hub.docker.com/r/stonedetector/stonedetector). Get started with Docker [here](https://docs.docker.com/get-started/) and follow the following tutorial on how to use it.

You can create the image on your own, using the [Dockerfile](Dockerfile) which comes with this repository:
```
docker build -t stonedetector .
```
or use its prebuilt version  (though be aware of its size: 1.59GB):
```
docker pull stonedetector/stonedetector
```
Note that you may need to execute the docker commands using sudo privileges (i.e., `sudo docker`). Having pulled or generated the docker image, create a new container:
```
docker run -itd --name stonedetector stonedetector /bin/bash
```
Aferwards, attach to a bash shell in the container:
```
docker exec -it stonedetector /bin/bash
```
Inside the container, you can run StoneDetector on a directory, which will identify code clones in Java source code files contained, e.g., in directory `test`:
```
./run.sh test
```
which will result in the following output denoting the clone pair in file `F.java` in the `test` directory:
```
test,F.java,5,24,test,F.java,27,46
```
You can also replicate StoneDetector's results on the [BigCloneEval](https://github.com/jeffsvajlenko/BigCloneEval) benchmark:
```
./run.sh run_benchmark.sh
```
Note that the latter requires quite some time due to the benchmark's size and depending on your machine. The results will be written into the tool evaluation report named `BigCloneEval_Report.txt`. For more information about the report's format or the benchmark, we refer to [BigCloneEval](https://github.com/jeffsvajlenko/BigCloneEval).

## How to use StoneDetector

For convenience reasons, we provide the shell script `run.sh` for running StoneDetector on a directory, given by the script's argument. StoneDetector will identify all code clones in Java source code files contained in the directory. You may also explicitly run the StoneDetector using the command:
```
java -Xms8G -Xmx8G  -jar build/libs/StoneDetector.jar -x --directory="path/to/Java/Folder" --error-file=errors.txt 
```
where StoneDetector will look for code clones in directory `path/to/Java/Folder` and you are able to specifically configure the tool's JVM heap size, error logging, etc.

### Output format

StoneDetector prints detected code clones by default onto the screen. Each line specifies a single clone pair using the format `directory1,filename1,startline1,endline1,directory2,filename2,startline2,endline2`, where `directory1,filename1,startline1,endline1` specifies the location of the one code fragment and `directory2,filename2,startline2,endline2` specifies the location of the other code fragment. Note that the order of the code fragments in the clone pair is not significant. For example, `test,F.java,5,24,test,F.java,27,46` denotes the clone pair which is formed by the two code fragments between lines 5 to 24 and lines 27 to 46, respectively, in file `test/F.java`.

### Configuration

The StoneDetector tool provides various configuration parameters, which allow you to play with its code clone detection capabilities. The tool's configuration parameters are defined in the file `config/default.properties`. 
| Parameter | Default | Description |
| --------- | ------- | ----------- |
| THREADSIZE | 3 |Number of parallel threads which are used for code clone detection |
| MINFUNCTIONSIZE | 15 |Minimal length of code lines for a code fragment to be considered |
| THRESHOLD | 0.3f |The threshold value used for comparing description sets (max difference) |
| SPLITTING | false |Whether or not split nodes are used in description sets (detection of subclones/blocks) |
| METRIC | LCS     |Metric which is used to compare description sets (LCS, Levenshtein, etc.) |
| USEHASH | true |Whether or not description sets are additionally encoded as hash values |
| USMD5 | false |Switch between MD5 or 4-byte prime number hash encoding for description sets | 
| USEFUNCTIONNAMES | true |Whether or not method names are kept in description sets or normalized |
| OUTPUT | true |Whether or not detected clone pairs are printed to the screen |

## Playing with the implementation

Further configuration parameters of the tool are defined in its implementation, see file [Environment.java](src/main/java/org/fsu/codeclones/Environment.java). The entry point of the tool, which is invoked when issuing scripts `run.sh` and `run_benchmark.sh` or executing the tool explicitly, is located at [SpoonBigCloneBenchDriver.java](src/main/java/org/dlr/foobar/SpoonBigCloneBenchDriver.java).

The StoneDetector implementation consists of four main components:
 * Java source code parser and control flow graph generation based upon [Spoon](https://github.com/INRIA/spoon): See [SpoonBigCloneBenchDriver.java](src/main/java/org/dlr/foobar/SpoonBigCloneBenchDriver.java)
 * Dominator tree construction based upon [WALA](https://github.com/wala/WALA): See [DominatorTree.java](src/main/java/org/fsu/codeclones/DominatorTree.java)
 * Description sets encoding: See [Encoder.java](src/main/java/org/fsu/codeclones/Encoder.java),[HashEncoder.java](src/main/java/org/fsu/codeclones/HashEncoder.java),  and in particular[CompletePathEncoder.java](src/main/java/org/fsu/codeclones/CompletePathEncoder.java)
 * Metrics implementation: See [LCS.java](src/main/java/org/fsu/codeclones/LCS.java), [HammingDistance.java](src/main/java/org/fsu/codeclones/HammingDistance.java), [LevenShtein.java](src/main/java/org/fsu/codeclones/LevenShtein.java), etc.

## I want to know more

That's great. Our [ICSME'21](https://www.computer.org/csdl/proceedings-article/icsme/2021/288200a070/1yNh4Mp9yE0) paper and presentation on [YouTube](https://youtu.be/GirClq1CA8w) is a good introduction into the technology behind StoneDetector. Don't hesitate to contact us if you have any questions:
 * Wolfram Amme: Wolfram.Amme@uni-jena.de 
 * André Schäfer: Andre.Schaefer@uni-jena.de 
 * Thomas Heinze: Thomas.Heinze@dhge.de 

Here are links to the [Spoon](https://github.com/INRIA/spoon) and [WALA](https://github.com/wala/WALA) projects, which StoneDetector has been built upon.
