# StoneDetector - Finding Structural Clones and Subclones in Java Source Code

If you just want to try StoneDetector, you can also have a look at the tool's interactive website: [stonedetector.fmi.uni-jena.de](https://stonedetector.fmi.uni-jena.de).

### Run StoneDetector
* Adjust the configuration file under config

* Compile StoneDetector ( Java 11 )
```
./gradlew jar
```

* Run StoneDetector
```
java -Xms4G -Xmx4G  -jar build/libs/StoneDetector.jar -x --directory="path/to/Java/Folder" --error-file=errors.txt 
```

### StoneDetector with BigCloneEval
 Use BCE_runner as tool runner script for BigCloneEval.

## Docker Image

The StoneDetector tool is also available as [Docker image](https://hub.docker.com/r/stonedetector/stonedetector). Get started with Docker [here](https://docs.docker.com/get-started/) and follow the following tutorial on how to use it.

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
Note that the latter requires quite some time due to the benchmark's size and depending on your configuration. The results will be written into the tool evaluation report named `BigCloneEval_Report.txt`. For more information about the report's format or the benchmark, we refer to [BigCloneEval](https://github.com/jeffsvajlenko/BigCloneEval).

## How to use StoneDetector

For convenience reasons, we also provide the shell script `run.sh` for running StoneDetector on a directory, given by the script's argument. StoneDetector will identify all code clones in Java source code files contained in the directory.

### Output format

StoneDetector prints detected code clones onto the screen. Each line specifies a single clone pair using the format `directory1,filename1,startline1,endline1,directory2,filename2,startline2,endline2`, where `directory1,filename1,startline1,endline1` specifies the location of the one code fragment and `directory2,filename2,startline2,endline2` specifies the location of the other code fragment. Note that the order of the code fragments in the clone pair is not significant. For example, `test,F.java,5,24,test,F.java,27,46` denotes the clone pair which is formed by the two code fragments between lines 5 to 24 and lines 27 to 46, respectively, in file `test/F.java`.

## Playing with the implementation
The entry point of ..., which is invoked when issuing any command starting with pub run ... is located at bin/dart. The driver that executes the static analysis, and iteratively executes the hybrid dynamic/static analysis is lib/dart (more specifically the method analyze()).

The StoneDetector implementation consists of four main components.

Instrumentation framework for the dynamic analysis
See libs/.

Control flow graph construction
See flow/ and in particular /lib/src/cfg/.

Static call graph construction, data flow and dependence analysis
See flow/.

also add information on configuration and configuration parameters ... and tutorial on how to built the system and run the benchmark

## I want to know more

That's great. Our [ICSME'21](https://www.computer.org/csdl/proceedings-article/icsme/2021/288200a070/1yNh4Mp9yE0) paper and presentation on [YouTube](https://youtu.be/GirClq1CA8w) is a good introduction into the technology behind StoneDetector. Don't hesitate to contact us if you have any questions:
 * Wolfram Amme: Wolfram.Amme@uni-jena.de 
 * André Schäfer: Andre.Schaefer@uni-jena.de 
 * Thomas Heinze: Thomas.Heinze@dhge.de 

Here are links to the [Spoon](https://github.com/INRIA/spoon) and [WALA](https://github.com/wala/WALA) projects, which StoneDetector has been built upon.
