package org.dlr.foobar;

import fr.inria.controlflow.NaiveExceptionControlFlowStrategy;
import fr.inria.controlflow.ControlFlowBuilder;
import fr.inria.controlflow.ControlFlowGraph;
import fr.inria.controlflow.ControlFlowNode;
import com.ibm.wala.util.graph.dominators.Dominators;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.AbstractGraph;
import org.dlr.foobar.stringencoding.StringEncoder;
import org.dlr.foobar.stringencoding.TokenSeparator;
import org.fsu.codeclones.DominatorTree;
import org.fsu.codeclones.AbstractEncoder;
import org.fsu.codeclones.CompletePathEncoder;
import org.fsu.codeclones.Encoder;
import org.fsu.codeclones.MetricKind;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtMethod;
import spoon.processing.AbstractProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import java.nio.file.Path;
import java.io.IOException;
import java.io.FileWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class SpoonDriver extends AbstractProcessor<CtClass> {

  class MethodEntry {

    List<List<Encoder>> encoding;
    String parentDirectoryName, fileName;
    int begin, end;

    MethodEntry(CtExecutable method, Path file, List<List<Encoder>> encoding) {
      this.encoding = encoding;
      parentDirectoryName = file.getParent().getFileName().toString();
      fileName = file.getFileName().toString();
      begin = method.getPosition().getLine();
      end = method.getPosition().getEndLine();
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (other == null || getClass() != other.getClass()) {
        return false;
      }
      MethodEntry otherEntry = (MethodEntry) other;
      return begin == otherEntry.begin && end == otherEntry.end &&
          parentDirectoryName.equals(otherEntry.parentDirectoryName) &&
          fileName.equals(otherEntry.fileName);
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append(parentDirectoryName);
      builder.append(",");
      builder.append(fileName);
      builder.append(",");
      builder.append(begin);
      builder.append(",");
      builder.append(end);
      return builder.toString();
    } 

  }

  final static Logger logger =
      LoggerFactory.getLogger(SpoonDriver.class);
  final static StopWatch watch = new StopWatch();

  final ArrayList<MethodEntry> entries;
  File inputFile;

  StringEncoder stringEncoder;

  final boolean splitting, output, abstrakt, stringencoding, exceptions;
  final double threshold;
  final String directory;
  final int min, max;

  SpoonDriver(
      String workingDirectory,
      boolean stringencoding,
      boolean exceptions,
      double threshold,
      int max,
      int min,
      boolean splitting,
      boolean abstrakt,
      boolean output) {
    directory = workingDirectory;
    entries = new ArrayList<MethodEntry>();
    this.stringencoding = stringencoding;
    this.exceptions = exceptions;
    this.threshold = threshold;
    this.max = max;
    this.min = min;
    this.splitting = splitting;
    this.abstrakt = abstrakt;
    this.output = output;
  }

  public static void main(String args[]) {

    // TODO: simplify handling of command line interface

    // defining the command line parser using Apache Commons CLI
    // cf. https://commons.apache.org/proper/commons-cli
    Options options = new Options();
    options.addOption(new Option("i", "input", true,
        "Java source input file path"));
    options.addOption(new Option("d", "directory",
        true, "Working directory for input/output files"));
    options.addOption(new Option("o", "output", false,
        "Write ASTs, CFGs and dominator trees to dot files"));
    options.addOption(new Option("s", "splitting", false,
        "Switch on path splitting"));
    options.addOption(new Option("x", "exceptions", false,
        "Enable Spoon's naive exceptional control flow strategy"));
    OptionGroup group = new OptionGroup();
    group.addOption(new Option("a", "abstract", false,
        "Switch on complete/abstract encoding"));
    group.addOption(new Option("e", "stringencoding", false,
        "String encoding (project Juliane Sperling)"));
    options.addOptionGroup(group);
    options.addOption(new Option("t", "threshold", true,
        "Threshold value for clone detection"));
    options.addOption(new Option("max", "maxlines", true,
        "Maxmimum number of lines for methods"));
    options.addOption(new Option("min", "minlines", true,
        "Minimum number of lines for methods"));
    options.addOption(new Option("c", "config", true,
        "Use configuration file instead of command line interface"));
    options.addOption(new Option("h", "help", false,
        "Print help"));
    HelpFormatter formatter = new HelpFormatter();
    String command = "./gradlew --args=\"--input=HelloWorld.java\"";

    try {
      // parsing command line arguments
      CommandLineParser parser = new DefaultParser();
      CommandLine cmd = parser.parse(options, args);
      if (cmd.hasOption("help")) {
        formatter.printHelp(command, options);
      } else {
        SpoonDriver driver = null;
        if (cmd.hasOption("config")) {
          if (cmd.hasOption("output") ||
	      cmd.hasOption("splitting") ||
              cmd.hasOption("exceptions") ||
	      cmd.hasOption("stringencoding") ||
	      cmd.hasOption("abstract") ||
	      cmd.hasOption("threshold") ||
	      cmd.hasOption("maxlines") ||
	      cmd.hasOption("minlines")) {
            throw new ParseException(
                "Use either command line interface or confguration file");
	  }
	  String configFile = cmd.getOptionValue("config");
	  if (configFile != null) {
            if (new File(configFile).isFile()) {
              try {
                Configurations configurations = new Configurations();
                PropertiesConfiguration config = configurations.properties(
                    new File(configFile));
                driver  = new SpoonDriver(
                    (cmd.hasOption("directory")
                        ? cmd.getOptionValue("directory")
                        : "."),
                    (config.getString("strategy",
                        "completepaths").equalsIgnoreCase(
                            "stringencoding")
                        ? true : false),
                    config.getBoolean("exceptions", false),
  	  	    config.getDouble("threshold", 0.1),
                    config.getInteger("max", 10000),
		    config.getInteger("min", 5),
                    config.getBoolean("splitting", false),
                    (config.getString("strategy",
                        "completepaths").equalsIgnoreCase("abstract")
                        ? true : false),
                    config.getBoolean("output", false));
	      } catch(ConfigurationException e) {
                System.err.println("ERROR: Unable to read " + configFile);
                formatter.printHelp(command, options);
                System.exit(1);
	      }
	    } else {
              System.err.println("ERROR: Unable to access " + configFile);
              formatter.printHelp(command, options);
              System.exit(1);
	    }
	  } else {
            formatter.printHelp(command, options);
            return;
	  }
	} else {
	  driver = new SpoonDriver(
              (cmd.hasOption("directory")
                  ? cmd.getOptionValue("directory")
                  : "."),
              cmd.hasOption("stringencoding"),
              cmd.hasOption("exceptions"),
	      (cmd.hasOption("threshold")
                  ? Double.parseDouble(cmd.getOptionValue("threshold")) 
                  : 0.1),
              (cmd.hasOption("maxlines")
                  ? Integer.parseInt(cmd.getOptionValue("maxlines"))
                  : 10000),
              (cmd.hasOption("minlines")
                  ? Integer.parseInt(cmd.getOptionValue("minlines"))
                  : 5),
              cmd.hasOption("splitting"),
              cmd.hasOption("abstract"),
              cmd.hasOption("output"));
	}
	// calling the Spoon driver
        String inputFile = cmd.getOptionValue("input");
	if (inputFile != null) {
          inputFile = driver.directory + File.separator + inputFile;
	  if (new File(inputFile).isFile()) {
            driver.process(inputFile);
            driver.detect();
	  } else {
            System.err.println("ERROR: Unable to access " + inputFile);
            formatter.printHelp(command, options);
            System.exit(1);
	  }
	} else {
          formatter.printHelp(command, options);
        }
      }
    } catch (ParseException e) {
      System.err.println(e.getMessage());
      formatter.printHelp(command, options);
      System.exit(1);
    }

  }

  void detect() {

    logger.info("Detecting clones using metric {} with threshold {} ...",
        (this.abstrakt ? "EUCLIDEAN" : "HAMMINGMODIFIED"),
        threshold);

    watch.start();

    Encoder encoder =
        this.abstrakt ? new AbstractEncoder() : new CompletePathEncoder();
    MetricKind metric =
        this.abstrakt ? MetricKind.EUCLIDEAN : MetricKind.HAMMINGMODIFIED;

    if (entries.size() >= 1) {

      for (int i = 0; i < entries.size()-1; i++) {

        for (int j = i+1; j < entries.size(); j++) {

          boolean comparison = encoder.areTwoDescriptionSetsSimilar(
              entries.get(i).encoding,
              entries.get(j).encoding,
              metric,
              false,
              true,
              (float) threshold);

	  if (comparison) {
            logger.info("Found code clone pair: {},{}",
                entries.get(i), entries.get(j));
            StringBuilder builder = new StringBuilder();
	    builder.append(entries.get(i));
	    builder.append(",");
	    builder.append(entries.get(j));
	    System.out.println(builder.toString());
          }

        }

      }
 
    }

    logger.info("Finished clone detection in {}ms", watch.getTime());

    watch.reset();

  }

  public void process(CtClass element) {

    logger.info("Processing type '{}' ...", element.getSimpleName());

    for (Object m: element.getTypeMembers()) {

      if (m instanceof CtMethod ||
          (m instanceof CtConstructor &&
              !(((CtConstructor) m).getPosition() instanceof NoSourcePosition))) {

        int length = ((CtExecutable) m).getPosition().getEndLine() -
            ((CtExecutable) m).getPosition().getLine();
	if (length > min && length < max) {
          process((CtExecutable) m, element);
        } else {
          logger.info("Skipping method '{}' due to number of lines (min={},max={})",
              ((CtExecutable) m).getSimpleName(), min, max);
	}

      }
    }
 
  }
  
  void process(CtExecutable m, CtType type) {

    logger.info("Creating control flow graph for method '{}'",
        m.getSimpleName());

    List<List<ControlFlowNode>> paths;
    List<List<Encoder>> encoding = null;
    DominatorTree domtree;
    ControlFlowGraph cfg;

    try {

      // building the control flow graph using the Spoon library
      // cf. https://spoon.gforge.inria.fr
      ControlFlowBuilder builder = new ControlFlowBuilder();
      if (this.exceptions) {
        EnumSet<NaiveExceptionControlFlowStrategy.Options> options;
        options = EnumSet.of(
            NaiveExceptionControlFlowStrategy.Options.ReturnWithoutFinalizers);
        builder.setExceptionControlFlowStrategy(
            new NaiveExceptionControlFlowStrategy(options));
      }
      cfg = builder.build(m);
      cfg.setName(type.getSimpleName()
          + "_" + m.getSimpleName() + "_cfg");
      cfg.simplify();

    } catch (Exception|Error e) {
	e.printStackTrace();
      logger.warn("Creating control flow graph for method '{}' failed!",
          m.getSimpleName());
      logger.debug("Debugging information: ",
          ExceptionUtils.getStackTrace(e));
      return;
    }

    if (this.output) {
      try {
        FileWriter writer = new FileWriter(
          this.directory + File.separator + cfg.getName() + ".dot");
        writer.write(cfg.toGraphVisText());
        writer.close();
        logger.info("DOT file '{}' successfully written",
            this.directory + File.separator + cfg.getName() + ".dot");
      } catch (IOException e) {
        logger.warn("Could not write DOT file '{}'",
            this.directory + File.separator + cfg.getName() + ".dot");
        logger.debug("Debugging information: ",
            ExceptionUtils.getStackTrace(e));
      }
    }

    logger.info("Creating dominator tree for method '{}'",
        m.getSimpleName());

    try {

      // building the dominator tree using the WALA library
      // cf. https://github.com/wala/WALA
      AbstractGraph<ControlFlowNode> dom = Dominators.make(
          (Graph<ControlFlowNode>) cfg,
          cfg.entry()).dominatorTree();
      domtree = new DominatorTree(dom);
      domtree.setName(type.getSimpleName()
        + "_" + m.getSimpleName() + "_domtree");

    } catch (Exception|Error e) {
	e.printStackTrace();
      logger.warn("Creating dominator tree for method '{}' failed!",
          m.getSimpleName());
      logger.debug("Debugging information: ",
          ExceptionUtils.getStackTrace(e));
      return;
    }

    if (this.output) {
      try {
        FileWriter writer = new FileWriter(
            this.directory + File.separator + domtree.getName() + ".dot");
        writer.write(domtree.toGraphVisText());
        writer.close();
        logger.info("DOT file '{}' successfully written",
            this.directory + File.separator + domtree.getName() + ".dot");
      } catch (IOException e) {
        logger.warn("Could not write DOT file '{}'",
            this.directory + File.separator + domtree.getName() + ".dot");
        logger.debug("Debugging information: ",
            ExceptionUtils.getStackTrace(e));
      }
    }

    try {

      // building the paths sets from the dominator tree
      if (this.splitting) {
        logger.info("Creating path set with split strategy for method '{}'",
            m.getSimpleName());
        paths = domtree.makePathToBeginOrMergeSet();
        logger.debug("Path set: {}", paths);
      } else {
        logger.info("Creating path set with default strategy for method '{}'",
            m.getSimpleName());
        paths = domtree.makePathToBeginSet();
        logger.debug("Path set: {}", paths);
      }

    } catch (Exception|Error e) {
      logger.warn("Creating path set for method '{}' failed!",
          m.getSimpleName());
      logger.debug("Debugging information: ", 
          ExceptionUtils.getStackTrace(e));
      return;
    }

    // Switch for project Juliane Sperling

    if (this.stringencoding) {

      try {

        logger.info("Using string encoding for path set for method '{}'",
            m.getSimpleName());
        encoding = stringEncoder.encode(paths);
	logger.info("Encoded path set: {}", encoding);

      } catch (Exception|Error e) {
        logger.warn("String encoding for path set for method '{}' failed!",
            m.getSimpleName());
        logger.debug("Debugging information: ",
            ExceptionUtils.getStackTrace(e));
      }


    } else {

      try {

        logger.info("Encoding path set for method '{}' using strategy {}",
            m.getSimpleName(),
            this.abstrakt ? "abstract" : "complete paths");
        Encoder encoder =
            this.abstrakt
            ? new AbstractEncoder()
            : new CompletePathEncoder();
        encoding = encoder.encodeDescriptionSet(paths);
        logger.debug("Encoded path set: {}", encoding);

      } catch (Exception|Error e) {
        logger.warn("Encoding path set for method '{}' failed!",
            m.getSimpleName());
        logger.debug("Debugging information: ",
            ExceptionUtils.getStackTrace(e));
        return;
      }

 
    }

    // TODO: add further steps of processing here

    entries.add(new MethodEntry(m, inputFile.toPath(), encoding));

  }

  void process(String inputFile) {

    logger.info("Parsing Java source file '{}' ...", inputFile);

    watch.start();

    this.inputFile = new File(inputFile);

    try {

      // configuring the Spoon library to read from file inputFile
      // cf. https://spoon.gforge.inria.fr/
      Launcher myLauncher = new Launcher();

      if (this.stringencoding) {
        stringEncoder =
   	    new StringEncoder(new TokenSeparator(myLauncher.getEnvironment()));
      }

      myLauncher.addInputResource(inputFile);
      myLauncher.addProcessor(this);
      myLauncher.buildModel();
      myLauncher.process();

    } catch (Exception|Error e) {
      logger.warn("Parsing file '{}' failed!", inputFile);
      logger.debug("Debugging information: {}",
          ExceptionUtils.getStackTrace(e));
    }

    logger.info("Finished parsing in {}ms", watch.getTime());

    watch.reset();

  }

}
