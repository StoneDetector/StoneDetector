FROM gradle:6.3.0-jdk11 AS build

COPY --chown=gradle:gradle . /home/gradle/src

WORKDIR /home/gradle/src
RUN rm -rf build
RUN gradle jar --no-daemon

RUN git clone https://github.com/jeffsvajlenko/BigCloneEval
RUN wget https://gitlab.com/t.heinze/bigcloneevaldata/-/raw/main/BigCloneBench_BCEvalVersion.tar.gz
RUN wget https://gitlab.com/t.heinze/bigcloneevaldata/-/raw/main/IJaDataset_BCEvalVersion.tar.gz

WORKDIR /home/gradle/src/BigCloneEval
RUN cp src/util/Version.java.template src/util/Version.java
RUN mkdir -p bin/
RUN javac -d bin/ -cp src/:libs/* src/**/*.java

FROM openjdk:11-jre-slim

RUN mkdir /StoneDetector
RUN mkdir /StoneDetector/test
RUN mkdir /StoneDetector/config
RUN mkdir /StoneDetector/build
RUN mkdir /StoneDetector/build/libs
COPY --from=build /home/gradle/src/BigCloneEval /StoneDetector/BigCloneEval
COPY --from=build /home/gradle/src/build/libs/*.jar /StoneDetector/build/libs/StoneDetector.jar
COPY --from=build /home/gradle/src/test/Example.java /StoneDetector/test/Example.java
COPY --from=build /home/gradle/src/config/default.properties /StoneDetector/config/default.properties
COPY --from=build /home/gradle/src/config/Patterns/ConfigByteCodePatterns /StoneDetector/config/Patterns/ConfigByteCodePatterns
COPY --from=build /home/gradle/src/config/Patterns/ConfigRegisterCodePatterns /StoneDetector/config/Patterns/ConfigRegisterCodePatterns
COPY --from=build /home/gradle/src/config/Patterns/ConfigSourceCodePatterns /StoneDetector/config/Patterns/ConfigSourceCodePatterns
COPY --from=build /home/gradle/src/errors.txt /StoneDetector/errors.txt
COPY --from=build /home/gradle/src/README.md /StoneDetector/README.md
COPY --from=build /home/gradle/src/BCE_runner /StoneDetector/BCE_runner
COPY --from=build /home/gradle/src/run_benchmark.sh /StoneDetector/run_benchmark.sh
COPY --from=build /home/gradle/src/run.sh /StoneDetector/run.sh
COPY --from=build /home/gradle/src/BigCloneBench_BCEvalVersion.tar.gz /StoneDetector/BigCloneEval/bigclonebenchdb/BigCloneBench_BCEvalVersion.tar.gz
COPY --from=build /home/gradle/src/IJaDataset_BCEvalVersion.tar.gz /StoneDetector/BigCloneEval/ijadataset/IJaDataset_BCEvalVersion.tar.gz

WORKDIR /StoneDetector/BigCloneEval/bigclonebenchdb
RUN tar zxvf BigCloneBench_BCEvalVersion.tar.gz
WORKDIR /StoneDetector/BigCloneEval/ijadataset
RUN tar zxvf IJaDataset_BCEvalVersion.tar.gz
WORKDIR /StoneDetector/BigCloneEval/commands
RUN ./init
RUN ./registerTool -n="StoneDetector" -d "StoneDetector"

WORKDIR /StoneDetector
RUN chmod +x run.sh
RUN chmod +x run_benchmark.sh
RUN chmod +x BCE_runner
