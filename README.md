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
Inside the container, you can run StoneDetector. Clone
```
./run.sh test
```
StoneDetector will print all detected code clones onto the screen, where each line specifies a single clone pair using the format `directory,filename,startline,endline,directory,filename,endline`. Where cf#_subdirectory,cf#_filename,cf#_startline,cf#_endline specifies one of the code fragments. And the order of the code fragments in the clone pair does not matter.
For example, `selected,102353.java,10,20,default,356923.java,20,30` denotes the clone pair 

Describe the output of the tool

Describe how to reproduce the benchmark

Also describe how to mount another directory into the
container and run the clone analysis on this directory

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

Unfortunately, StoneDetector's code has not been cleaned up yet, though, we are planning to do so in the near future ...
