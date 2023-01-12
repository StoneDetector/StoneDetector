package org.dlr.foobar;

import fr.inria.controlflow.NaiveExceptionControlFlowStrategy;
import fr.inria.controlflow.ControlFlowBuilder;
import fr.inria.controlflow.ControlFlowGraph;
import fr.inria.controlflow.ControlFlowNode;
import com.ibm.wala.util.graph.dominators.Dominators;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.AbstractGraph;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.ex.ConversionException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.fsu.codeclones.CompletePathEncoder;
import org.fsu.codeclones.AbstractEncoder;
import org.fsu.codeclones.HashEncoder;
import org.fsu.codeclones.DominatorTree;
import org.fsu.codeclones.Encoder;
import org.fsu.codeclones.MetricKind;
import org.fsu.codeclones.EncoderKind;
import org.fsu.codeclones.Environment;
import org.fsu.codeclones.SortedMultisetPathEncoder;
import spoon.Launcher;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.cli.*;
import spoon.support.reflect.declaration.CtConstructorImpl;
import spoon.support.reflect.declaration.CtMethodImpl;
import spoon.reflect.declaration.CtClass;
import spoon.processing.AbstractProcessor;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.*;
import java.util.EnumSet;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: add time to logging ...

public class SpoonBigCloneBenchDriver extends AbstractProcessor<CtClass> {

  static Object monitorAddPathes=new Object();
  static Object monitorOutput=new Object();

  final static Logger logger =
      LoggerFactory.getLogger(SpoonBigCloneBenchDriver.class);

   
  private int successAST, successCFG, successDom, totalMethods,
      totalFiles, successPath;
  private StringBuilder errorLog;
  private boolean errors, output, skipclones, exceptions;
    
  //private Path currentFile;
  private static ThreadLocal<Path> currentFile = new ThreadLocal<>();
  private String workingDirectory, outputDirectory;

  class MethodTuple {
    List<List<Encoder>> encodePathSet;
    MethodInfo info;

    MethodTuple(CtExecutable m, List<List<Encoder>> encodePathSet) {
      this.encodePathSet = encodePathSet;
      this.info = new MethodInfo(m);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      return this.info.equals(((MethodTuple) o).info);
    }
  }
    class MethodInfo {
      String subDir;
      String fileName;
      int startLine;
      int endLine;

      MethodInfo(CtExecutable m) {
        this.subDir = currentFile.get().getParent().getFileName().toString();
        this.fileName = currentFile.get().getFileName().toString();
        this.startLine = m.getPosition().getLine();
        this.endLine = m.getPosition().getEndLine();
        for (CtAnnotation annotation : m.getAnnotations())
	{	
  	    if (annotation.getPosition().getLine()<this.startLine)
    		this.startLine=annotation.getPosition().getLine();
	}	
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodInfo that = (MethodInfo) o;
        return startLine == that.startLine &&
                endLine == that.endLine &&
                Objects.equals(subDir, that.subDir) &&
                Objects.equals(fileName, that.fileName);
      }
    }
  

  private static final ArrayList<MethodTuple> outputTuples = new ArrayList<MethodTuple>();
  private MethodTuple[] outputTuplesArray;

  public SpoonBigCloneBenchDriver(String workingDirectory) {
    errorLog = new StringBuilder();
    totalMethods = 0;
    totalFiles = 0;
    successPath = 0;
    successAST = 0;
    successCFG = 0;
    successDom = 0;
    this.workingDirectory = workingDirectory;
  }

  void setErrors(boolean errors) {this.errors = errors;}
  void setOutput(boolean output) {this.output = output;}
  void setOutputDir(String outputDirectory) {this.outputDirectory = outputDirectory;}

  public static void main(String args[]) {
    // defining the command line parser using Apache Commons CLI
    // cf. https://commons.apache.org/proper/commons-cli
    Options options = new Options();

    // flag for exception mapping strategy
    Option optionX = new Option("x", "exceptions", false,
        "Enable Spoon's naive exception control flow strategy");
    options.addOption(optionX);

    // source directory option
    Option option = new Option("d", "directory",
        true, "working directory for bigclonebench");
    option.setRequired(true);
    options.addOption(option);
    options.addOption(new Option("e", "error-file", true,
        "Write errors to file"));
    options.addOption(new Option("s", "skipclones", false,
        "Skip clone detection, just generate encoding"));
    // output directory option
    options.addOption(new Option("o", "out",
            true, "basedir for output of ControlFlowGraph, DominatorTree"));

    // help option
    options.addOption(new Option("h", "help", false,
        "Print help"));
    HelpFormatter formatter = new HelpFormatter();
    String command = "./gradlew --args=\"--directory=dataset [--out=out_basedir]\"";

    try {
      // parsing command line arguments
      CommandLineParser parser = new DefaultParser();
      CommandLine cmd = parser.parse(options, args);
      if (cmd.hasOption("help")) {
        formatter.printHelp(command, options);
        System.exit(0);
      }
      String workingDirectory = cmd.getOptionValue("directory");
      logger.info("Traversing working directory {} ...", workingDirectory);
      long start=System.nanoTime();
      SpoonBigCloneBenchDriver driver = new SpoonBigCloneBenchDriver(workingDirectory);
      driver.skipclones = cmd.hasOption("skipclones");
      driver.setErrors(cmd.hasOption("error-file"));
      driver.setOutput(cmd.hasOption("out"));
      driver.exceptions = cmd.hasOption("exceptions");
      if (cmd.hasOption("out"))
        driver.setOutputDir(cmd.getOptionValue("out"));

      FileBasedConfiguration configuration = null;
      String configFileName="config/default.properties";
      Parameters params = new Parameters();
      FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
              new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                      .configure(params.properties()
                              .setFileName(configFileName));
      try {
        configuration = builder.getConfiguration();
      } catch (ConfigurationException e) {
        e.printStackTrace();
      }
      try {
        Environment.THREADSIZE = configuration.getInt("THREADSIZE");
        Environment.METRIC = configuration.getString("METRIC").equals("NW") ? MetricKind.NW : configuration.getString("METRIC").equals("LEVENSHTEIN") ? MetricKind.LEVENSHTEIN : MetricKind.LCS;
        Environment.PATHSINSETS = configuration.getBoolean("SPLITTING") ? EncoderKind.SPLITTING : EncoderKind.UNSPLITTING;
        Environment.TECHNIQUE = configuration.getBoolean("USEHASH") ? EncoderKind.HASH : EncoderKind.COMPLETEPATH;
        Environment.MD5 = configuration.getBoolean("USEMD5");
        Environment.THRESHOLD = configuration.getFloat("THRESHOLD");
        Environment.MINSIZE = configuration.getInt("MINFUNCTIONSIZE");
        Environment.SUPPORTCALLNAMES = configuration.getBoolean("USEFUNCTIONNAMES");
        Environment.WIDTHUPPERFAKTOR = configuration.getFloat("UPPERFACTOR");
        Environment.MINNODESNO = configuration.getBoolean("SPLITTING") ? 1 : 3;
        Environment.OUTPUT = configuration.getBoolean("OUTPUT");
      }catch (NoSuchElementException|ConversionException ex)
      {
        ex.printStackTrace();
      }

      // traversing the benchmark directory and calling the Spoon driver
      int poolSize=Environment.THREADSIZE;
      ForkJoinPool myPool = new ForkJoinPool(poolSize);
      myPool.submit(() -> {
                try {
                  Files.walk(Paths.get(workingDirectory))
                          .collect(Collectors.toList())
                          .parallelStream()
                          .filter(Files::isRegularFile)
                          .forEach(driver::process);
			  /*.forEach(path -> {
                            SpoonBigCloneBenchDriver d = new SpoonBigCloneBenchDriver(workingDirectory);
                            driver.skipclones = cmd.hasOption("skipclones");
                            driver.setErrors(cmd.hasOption("error-file"));
                            driver.setOutput(cmd.hasOption("out"));
                            driver.exceptions = cmd.hasOption("exceptions");
                            d.process(path);
                          });*/
                } catch (IOException e) {
                  System.out.println("ERROR: Unable to access " + workingDirectory);
                  formatter.printHelp(command, options);
                  System.exit(1);
                }
              }
      ).get();

      long end1=System.nanoTime();

      // logging
      logger.info("Successfully created AST for {} out of {} files",
          driver.successAST, driver.totalFiles);
      logger.info("Successfully created CFG for {} out of {} methods",
          driver.successCFG, driver.totalMethods);
      logger.info("Successfully created DomTree for {} out of {} methods",
          driver.successDom, driver.totalMethods);
      logger.info("Successfully encoded paths for {} out of {} methods",
          driver.successPath, driver.totalMethods);
      //System.out.println("Successfully created AST for "+driver.successAST+" out of "+driver.totalFiles+" files");

      //System.out.println("Successfully created CFG for "+driver.successCFG+" out of "+driver.totalMethods+" methods");
      //System.out.println("Successfully created DomTree for "+driver.successDom+" out of "+driver.totalMethods+" methods");
      //System.out.println("Successfully encoded paths for "+driver.successPath+" out of "+driver.totalMethods+" methods");

      if (!driver.skipclones) {
        driver.outputTuplesArray= driver.outputTuples.toArray(new MethodTuple[driver.outputTuples.size()]);
        //driver.detectClones(0,1);


        ThreadPoolExecutor executor =(ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize);
        for (int i =0;i<poolSize;i++)
        {
          int finalI1 = i;
          executor.submit(() -> {
            driver.detectClones(finalI1,poolSize);
            return null;
          });
        }
        executor.shutdown();
        try {
          //System.out.println("startwait");
          executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
          //System.out.println("end");
        } catch (InterruptedException e) {
        }
      }
      long end2=System.nanoTime();
      logger.info("Time create pathes= "+TimeUnit.SECONDS.convert(end1-start, TimeUnit.NANOSECONDS));
      logger.info("Time find clones= "+TimeUnit.SECONDS.convert(end2-end1, TimeUnit.NANOSECONDS));
      // write errors
      if (driver.errors) {
        driver.logErrors(cmd.getOptionValue("error-file"));
      }
      
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      formatter.printHelp(command, options);
      System.exit(1);
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
  }
    // Preparation for clone detection ->changes must be compiles with ./gradlew jar in SPOON
    
  void detectClones(int start, int add) {
      Encoder myEncoder = null;
      if(Environment.TECHNIQUE == EncoderKind.COMPLETEPATH)    
	  myEncoder = new CompletePathEncoder();
      else if(Environment.TECHNIQUE == EncoderKind.HASH)    
	  myEncoder = new HashEncoder();
      else if (Environment.TECHNIQUE == EncoderKind.MULTISET)
	  myEncoder = new SortedMultisetPathEncoder();
      else
	  myEncoder = new AbstractEncoder();


      for (int countIndex1=start;countIndex1<outputTuplesArray.length;countIndex1=countIndex1+add)
      {
          MethodTuple cf_1=outputTuplesArray[countIndex1];

          for (int coundIndex2=countIndex1+1;coundIndex2<outputTuplesArray.length;coundIndex2++)
          {
            MethodTuple cf_2=outputTuplesArray[coundIndex2];

/*      int i=-1;
      for (MethodTuple cf_1: this.outputTuples) {
            i=i+1;
            if (i<start || i>end)
              continue;
            boolean visited = false;
            for (MethodTuple cf_2: this.outputTuples) {
                if(cf_1.equals(cf_2)) {
                  visited = true;
                  continue;
                }
                if (!visited)
                  continue;*/
	    
                if((cf_1.info.endLine - cf_1.info.startLine + 1) < Environment.MINSIZE ||  (cf_2.info.endLine - cf_2.info.startLine + 1) <Environment.MINSIZE)
                  continue;
                //if(cf_1.encodePathSet.size() == 0){
                //  System.out.println(cf_1.info.fileName + " " +
                //cf_1.info.startLine);
                  //}

                //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                if(myEncoder.areTwoDescriptionSetsSimilar(cf_1.encodePathSet,
                                      cf_2.encodePathSet,
                                      Environment.METRIC,
                                      Environment.SORTED, // true=>sorted
                                      Environment.RELATIVE, // true => relativ
                                      Environment.THRESHOLD)) {
                                                          //Levenstein Splitting und Unsplitting  0.35
                                                          //LCS Splitting und Unsplitting 0.3F
                    if(Environment.OUTPUT){
                          if ((cf_2.info.subDir.equals(cf_1.info.subDir) && cf_1.info.fileName.equals(cf_2.info.fileName)) &&
                                  ((cf_1.info.startLine>cf_2.info.startLine && cf_1.info.endLine<cf_2.info.endLine)||
                                          (cf_2.info.startLine>cf_1.info.startLine && cf_2.info.endLine<cf_1.info.endLine)))
                            continue;
			StringBuilder builder = new StringBuilder();
                          String part1=builder.append(cf_1.info.subDir)
                                  .append(",")
                                  .append(cf_1.info.fileName)
                                  .append(",")
                                  .append(cf_1.info.startLine)
                                  .append(",")
                                  .append(cf_1.info.endLine).toString();
                          builder=new StringBuilder();
                          String part2=builder.append(cf_2.info.subDir)
                                  .append(",")
                                  .append(cf_2.info.fileName)
                                  .append(",")
                                  .append(cf_2.info.startLine)
                                  .append(",")
                                  .append(cf_2.info.endLine).toString();
                          builder=new StringBuilder();
                                if (!part1.equals(part2))  {

                                  builder.append(cf_1.info.subDir)
                                          .append(",")
                                          .append(cf_1.info.fileName)
                                          .append(",")
                                          .append(cf_1.info.startLine)
                                          .append(",")
                                          .append(cf_1.info.endLine)
                                          .append(",")
                                          .append(cf_2.info.subDir)
                                          .append(",")
                                          .append(cf_2.info.fileName)
                                          .append(",")
                                          .append(cf_2.info.startLine)
                                          .append(",")
                                          .append(cf_2.info.endLine);

                                  synchronized (monitorOutput) {
                                    System.out.println(builder.toString());
                                  }
                                }
                              builder = null;
                          }
                        
                    }
	
      }
    }
      
  }

  public void process(CtClass type) {
    String dir = "";
    if (output) {
      // directory that all the graphs of the type are written to
      dir = convertToOutputDirectory(currentFile.get());
      // make directory
      new File(dir).mkdirs();
    }

    for (Object m : type.getTypeMembers()) {
      try {
        extractGraphs(type, m, dir);
      } catch (Throwable e) {
        if (errors)
          reportErrors(e);
      }
    }
    //return encodePathSet;
  }

  public void setSkipClones(boolean skipClones)
  {
    this.skipclones=skipClones;
  }
  public List<List<Encoder>> extractGraphs(CtType type, Object m, String dir)
  {
	
      if ((m instanceof CtMethodImpl) && ((CtMethodImpl)m).isAbstract())
	  return null;
	      
      if (!(m instanceof CtMethodImpl) && !(m instanceof CtConstructorImpl)) {
	  return null;
      }
      if (m instanceof CtConstructor && ((CtConstructor) m).getPosition() instanceof NoSourcePosition) {
	  return null;
      }

      if (!skipclones) {
        MethodInfo info = new MethodInfo((CtExecutable) m); //

        if ((info.endLine - info.startLine + 1) < Environment.MINSIZE) //
          return null;
      }
    try {
       
      // building the control flow graph using the Spoon library
      // cf. https://spoon.gforge.inria.fr
      String cfg_name = methodID(type, (CtExecutable) m) + "_cfg";
      ControlFlowGraph cfg = makeCFG((CtExecutable) m, cfg_name);
      successCFG++;
      
      //System.out.println(cfg_name);
      //System.out.println(cfg);
      // building the dominator tree using the WALA library
      // cf. https://github.com/wala/WALA
      String domtree_name = methodID(type, (CtExecutable) m) + "_domtree";
      DominatorTree domtree = makeDomTree(cfg, domtree_name);
      successDom++;
      String encodePathSet_name = methodID(type, (CtExecutable) m) + "_encodePathSet";
      //!!!!!!System.out.println(cfg);!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
      List<List<Encoder>> encodePathSet = domtree.encodePathSet(Environment.PATHSINSETS, Environment.TECHNIQUE, Environment.SETORDER);

      successPath++;

      if (!skipclones) {
        synchronized (monitorAddPathes) {
          //for (List<List<Encoder>> pathset : encodePathSet)
            this.outputTuples.add(new MethodTuple((CtExecutable) m, encodePathSet));
        }
      }

      // write the cfg and the domtree
      if (output) {
        writeToPath(dir + cfg_name + ".dot", cfg.toGraphVisText());
        writeToPath(dir + domtree_name + ".dot", domtree.toGraphVisText());
        writeToPath(dir + encodePathSet_name + ".txt", encodePathSet.toString());
      }
      return encodePathSet;

    } catch (Throwable e) {
      if (errors)
        reportErrors(e);
    } finally {
      totalMethods++;
    }
    return null;
  }

  void process(Path inputFile) {
    logger.info("Parsing Java source file {} ...", inputFile);
    currentFile.set(inputFile);

    try {
      // configuring the Spoon library to read from file inputFile
      // cf. https://spoon.gforge.inria.fr/
      Launcher myLauncher = new Launcher();
      myLauncher.addInputResource(inputFile.toString());

      myLauncher.addProcessor(this);
      myLauncher.buildModel();
      myLauncher.process();

      successAST++;
      //System.out.println("fin "+inputFile);

    } catch (Throwable e) {
      if (errors)
        reportErrors(e);
    } finally {
      totalFiles++;
    }
  }

  String methodID(CtType type, CtExecutable m) {
    return type.getSimpleName()
            + "_" + m.getSimpleName()
            + "_" + m.getPosition().getLine()
            + "_" + m.getPosition().getEndLine();
  }

  ControlFlowGraph makeCFG(CtExecutable m, String name) {
    ControlFlowBuilder builder = new ControlFlowBuilder();
    if (this.exceptions) {
        EnumSet<NaiveExceptionControlFlowStrategy.Options> options;
        options = EnumSet.of(NaiveExceptionControlFlowStrategy.Options.ReturnWithoutFinalizers);
        builder.setExceptionControlFlowStrategy(new NaiveExceptionControlFlowStrategy(options));
    }
    ControlFlowGraph cfg = builder.build( m);
    cfg.setName(name);
    cfg.simplify();
    return cfg;
  }
    
  DominatorTree makeDomTree(ControlFlowGraph cfg, String name) {
    AbstractGraph<ControlFlowNode> dom = Dominators.make(
            (Graph<ControlFlowNode>) cfg,
            cfg.entry()).dominatorTree();
    DominatorTree domtree = new DominatorTree(dom);
    domtree.setName(name);
    return domtree;
  }

  void writeToPath(String path, String written) {
    try {
      FileWriter fw = new FileWriter(path);
      fw.write(written);
      fw.close();
    } catch (IOException ignored) {}
  }

  void reportErrors(Throwable e) {
    errorLog.append(currentFile.get().toString());
    errorLog.append(":\n");
    errorLog.append(ExceptionUtils.getStackTrace(e));
    errorLog.append("\n");
  }

  void logErrors(String errorFile) {
    try {
      Path file = Paths.get(errorFile);
      Files.write(file, Collections.singleton(errorLog.toString()), StandardCharsets.UTF_8);
    } catch (IOException e) {
      logger.warn("Could not write error file {}",
          errorFile);
    }
  }

  String convertToOutputDirectory(Path source) {
    // converts the dataset source path into a directory within the output basedir
    // strip workingDir (leading) and .java (tail)
    String temp = source.toString()
            .substring(workingDirectory.length(), source.toString().length() - ".java".length());
    // add outputDir to the front
    return outputDirectory
            + (outputDirectory.endsWith(File.separator) ? "" : File.separator)
            + temp
            + File.separator;
  }
}
