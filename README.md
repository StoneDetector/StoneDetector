# StoneDetector - Finding Structural Clones and Subclones in Java Source Code

If you just want to try out StoneDetector, you can also have a look at the tool's interactive website: [stonedetector.fmi.uni-jena.de](https://stonedetector.fmi.uni-jena.de).

## StoneDetector Quick Start
* Adjust StoneDetector's configuration file under `config/default.properties`
* Build StoneDetector using the Gradle wrapper (requires at least JDK 17)
```
./gradlew jar
```
* Run StoneDetector (requires at least JRE 17)
```
java -Xms8G -Xmx8G -jar build/libs/StoneDetector.jar -x --directory="path/to/Java/Folder" --error-file=errors.txt 
```
* StoneDetector with [BigCloneEval](https://github.com/jeffsvajlenko/BigCloneEval):
  Use `BCE_runner` as tool runner script

## Docker Image

The StoneDetector tool is available as [Docker image](https://hub.docker.com/r/stonedetector/stonedetector). Get started with Docker [here](https://docs.docker.com/get-started/) and follow the following tutorial on how to use it.

You can create the image on your own, using the [Dockerfile](Dockerfile) which comes with this repository:
```
docker build -t stonedetector/stonedetector .
```
or use its prebuilt version  (though be aware of its size: 1.59GB):
```
docker pull stonedetector/stonedetector
```
Note that you may need to execute the docker commands using sudo privileges (i.e., `sudo docker`). Having pulled or generated the docker image, create a new container:
```
docker run -itd --name stonedetector stonedetector/stonedetector /bin/bash
```
Aferwards, attach to a bash shell in the container:
```
docker exec -it stonedetector /bin/bash
```
Inside the container, you can run StoneDetector on a directory, which will identify code clones in Java source code files contained, e.g., in directory `test`:
```
./run.sh test
```
which will result in the following output denoting the clone pair in file `Example.java` in the `test` directory:
```
test,Example.java,4,18,test,Example.java,19,33
```
If you want to run StoneDetector on your own files, just mount a local directory when creating the container and proceed
as described above, e.g.:
```
docker run -itd -v path/to/Java/Folder:/StoneDetector/javafiles --name stonedetector stonedetector/stonedetector /bin/bash
docker exec -it stonedetector /bin/bash
./run.sh javafiles
```

You can also replicate StoneDetector's results on the [BigCloneEval](https://github.com/jeffsvajlenko/BigCloneEval) benchmark:
```
./run_benchmark.sh
```
Note that the latter requires quite some time due to the benchmark's size and depending on your machine. The results will be written into the tool evaluation report named `BigCloneEval_Report.txt`. For more information about the report's format or the benchmark, we refer to [BigCloneEval](https://github.com/jeffsvajlenko/BigCloneEval).

## How to use StoneDetector

For convenience reasons, we provide the shell script `run.sh` for running StoneDetector on a directory, given by the script's argument. StoneDetector will identify all code clones in Java source code files contained in the directory. You may also explicitly run the StoneDetector using the command:
```
java -Xms8G -Xmx8G  -jar build/libs/StoneDetector.jar -x --directory="path/to/Java/Folder" --error-file=errors.txt 
```
where StoneDetector will look for code clones in directory `path/to/Java/Folder` and you are able to specifically configure the tool's JVM heap size, error logging, etc.

### Output format

StoneDetector prints detected code clones by default onto the screen. Each line specifies a single clone pair using the format `directory1,filename1,startline1,endline1,directory2,filename2,startline2,endline2`, where `directory1,filename1,startline1,endline1` specifies the location of the one code fragment and `directory2,filename2,startline2,endline2` specifies the location of the other code fragment. Note that the order of the code fragments in the clone pair is not significant.
For example, `test,Example.java,4,18,test,Example.java,19,33` denotes the clone pair which is formed by the two code fragments between lines 4 to 18 and lines 19 to 33, respectively, in file `test/Example.java`.

Clone output is sorted deterministically. If parsing, control-flow construction, or path encoding is incomplete, StoneDetector writes the available diagnostics to `--error-file`, emits no partial clone result, and exits with status 2.

### Project classpaths and source sets

By default, StoneDetector scans every Java file below `--directory` independently in Spoon's no-classpath mode. For a built project whose source needs project types to parse correctly, pass the compiled classpath explicitly instead of making StoneDetector invoke or guess a build system:

```text
java -jar build/libs/StoneDetector.jar \
  --directory=/path/to/project \
  --source-file-list=/path/to/module.sources \
  --classpath-file=/path/to/module.compile-classpath \
  --error-file=errors.txt
```

`--source-root` is repeatable and selects the source directories for one source set; relative roots are resolved below `--directory`. Roots are resolved to their real filesystem location and must remain within the real working tree. When the option is omitted, StoneDetector scans `--directory` as before. Overlapping or aliased roots do not analyze the same file twice.

`--source-file-list` is an optional UTF-8 manifest containing one Java source per nonblank line. It is an authoritative alternative to recursive `--source-root` discovery and cannot be combined with `--source-root`. Relative entries are resolved below `--directory`; absolute entries are accepted only when their real paths remain within that working tree. StoneDetector validates the complete manifest before analysis, rejects missing, unreadable, non-Java, malformed, or escaping entries, deduplicates aliases by real file identity, and analyzes the selected files in deterministic normalized order. This lets build tools provide an exact source set without teaching StoneDetector about a build system or forcing intentionally invalid compiler fixtures into clone analysis.

`--classpath-file` is an optional UTF-8 file with one compiled classpath entry per nonblank line. Relative entries are resolved from the classpath file's directory. Entries must already exist as directories or files. StoneDetector passes them directly to Spoon and preserves per-file analysis; it does not run Maven, Gradle, `javac`, or dependency discovery. Include the source set's compiled output when its own compiled types are needed. Invoke StoneDetector separately for source sets or modules that require different classpaths.

`--analysis-threads` sets the maximum number of source files whose Spoon models may be analyzed concurrently. It must be a positive integer and defaults to `THREADSIZE`. Use `--analysis-threads=1` to avoid concurrent source-model memory use on constrained machines or compiler-intensive corpora. Source assignment is deterministic, failures are not retried, and this option does not change clone-comparison parallelism.

A classpath is not a Java module path. StoneDetector therefore parses `module-info.java` descriptors in no-classpath mode even when `--classpath-file` is present. Module descriptors remain syntax-checked and counted, but they contain no methods for clone extraction.

Without `--classpath-file`, the existing no-classpath behavior remains available. In either mode, an incomplete parse or analysis remains a failure rather than producing partial clone output.

### Configuration

The StoneDetector tool provides various configuration parameters, which allow you to play with its code clone detection capabilities. The tool's configuration parameters are defined in the file `config/default.properties`.
| Parameter | Default | Description |
| --------- | ------- | ----------- |
| THREADSIZE | 3 |Default number of parallel source-analysis workers and number of code-clone comparison workers |
| MINFUNCTIONSIZE | 15 |Minimal length of code lines for a code fragment to be considered |
| THRESHOLD | 0.3f |The threshold value used for comparing description sets (max difference) |
| SPLITTING | false |Whether or not split nodes are used in description sets (detection of subclones/blocks) |
| METRIC | LCS     |Metric which is used to compare description sets (LCS, Levenshtein, etc.) |
| USEHASH | true |Whether or not description sets are additionally encoded as hash values |
| USMD5 | false |Switch between MD5 or 4-byte prime number hash encoding for description sets | 
| USEFUNCTIONNAMES | true |Whether or not method names are kept in description sets or normalized |
| OUTPUT | true |Whether or not detected clone pairs are printed to the screen |
|UPPERFACTOR|1.7f|Factor that indicates up to which difference in the number of paths of path sets they are compared with each other.|
|BYTECODEBASEDCLONEDETECTION|false|Whether or not to perform clone detection within jar or class files.|
|REGISTERCODE_STACKCODE|true|If true, bytecode-based clone detection is performed using register code, otherwise using stack code|
|STUBBERPROCESSING|true| When working with generated files of the [Stubber](https://github.com/andre-schaefer-94/Stubber) tool, then the annotations can be read out.|

### Building

StoneDetector is written in Java and can be built using the included [Gradle](https://gradle.org) wrapper. StoneDetector requires at least Java 17.

Source analysis uses Java 25 language compliance. The end-to-end regression suite covers modern syntax through Java 25, including records and sealed types, text blocks, pattern matching and guarded switch expressions, record and unnamed patterns, module imports, compact source files, flexible constructor bodies, Markdown documentation comments, and primitive patterns. Support means that these sources complete StoneDetector's AST, control-flow, dominator-tree, and path-encoding pipeline; clone encodings continue to normalize syntax according to StoneDetector's structural model rather than preserving every source-level distinction.

To build the tool, run:
```
./gradlew jar
```
in the projects top-level directory, which will generate the JAR file `build/libs/StoneDetector.jar`. This file can then be used to run the tool (see above).

## Playing with the implementation

Further configuration parameters of the tool are defined in its implementation, see file [Environment.java](src/main/java/org/fsu/codeclones/Environment.java). The entry point of the tool, which is invoked when issuing scripts `run.sh` and `run_benchmark.sh` or executing the tool explicitly, is located at [SpoonBigCloneBenchDriver.java](src/main/java/org/dlr/foobar/SpoonBigCloneBenchDriver.java).

The StoneDetector implementation consists of four main components:
* Java source code parser and control flow graph generation based upon [Spoon](https://github.com/INRIA/spoon): See [SpoonBigCloneBenchDriver.java](src/main/java/org/dlr/foobar/SpoonBigCloneBenchDriver.java)
* Dominator tree construction based upon [WALA](https://github.com/wala/WALA): See [DominatorTree.java](src/main/java/org/fsu/codeclones/DominatorTree.java)
* Description sets encoding: See [Encoder.java](src/main/java/org/fsu/codeclones/Encoder.java), [HashEncoder.java](src/main/java/org/fsu/codeclones/HashEncoder.java),  and in particular[CompletePathEncoder.java](src/main/java/org/fsu/codeclones/CompletePathEncoder.java)
* Metrics implementation: See [LCS.java](src/main/java/org/fsu/codeclones/LCS.java), [HammingDistance.java](src/main/java/org/fsu/codeclones/HammingDistance.java), [LevenShtein.java](src/main/java/org/fsu/codeclones/LevenShtein.java), etc.

## I want to know more

That's great. Our [ICSME'21](https://www.computer.org/csdl/proceedings-article/icsme/2021/288200a070/1yNh4Mp9yE0) paper and presentation on [YouTube](https://youtu.be/GirClq1CA8w) is a good introduction into the technology behind StoneDetector. Don't hesitate to contact us if you have any questions:
* Wolfram Amme: Wolfram.Amme@uni-jena.de
* André Schäfer: Andre.Schaefer@uni-jena.de
* Thomas Heinze: Thomas.Heinze@dhge.de

Here are links to the [Spoon](https://github.com/INRIA/spoon) and [WALA](https://github.com/wala/WALA) projects, which StoneDetector has been built upon.
