FROM gradle:6.3.0-jdk11 AS build

RUN apt update
RUN apt install -y make

COPY --chown=gradle:gradle . /home/gradle/src

WORKDIR /home/gradle/src
RUN gradle jar --no-daemon

RUN git clone https://github.com/jeffsvajlenko/BigCloneEval
RUN wget https://gitlab.com/t.heinze/bigcloneevaldata/-/raw/main/BigCloneBench_BCEvalVersion.tar.gz
RUN wget https://gitlab.com/t.heinze/bigcloneevaldata/-/raw/main/IJaDataset_BCEvalVersion.tar.gz

WORKDIR /home/gradle/src/BigCloneEval
RUN make

FROM openjdk:11-jre-slim

RUN mkdir /StoneDetector
RUN mkdir /StoneDetector/test
RUN mkdir /StoneDetector/config
RUN mkdir /StoneDetector/build
RUN mkdir /StoneDetector/build/libs
COPY --from=build /home/gradle/src/BigCloneEval /StoneDetector/BigCloneEval
COPY --from=build /home/gradle/src/build/libs/*.jar /StoneDetector/build/libs/StoneDetector.jar
COPY --from=build /home/gradle/src/test/F.java /StoneDetector/test/F.java
COPY --from=build /home/gradle/src/config/default.properties /StoneDetector/config/default.properties
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

WORKDIR /StoneDetector
