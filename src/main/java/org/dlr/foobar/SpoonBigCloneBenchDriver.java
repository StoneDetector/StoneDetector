package org.dlr.foobar;

import fr.inria.controlflow.NaiveExceptionControlFlowStrategy;
import fr.inria.controlflow.ControlFlowBuilder;
import fr.inria.controlflow.ControlFlowGraph;
import fr.inria.controlflow.ControlFlowNode;
import com.ibm.wala.util.graph.dominators.Dominators;
import com.ibm.wala.util.graph.AbstractGraph;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.fsu.bytecode.ByteCodePathExtraction;
import org.fsu.bytecode.HashEncoderRegisterCode;
import org.fsu.codeclones.*;
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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.EnumSet;
import java.util.concurrent.*;
import java.util.stream.Collectors;

// TODO: add time to logging ...

public class SpoonBigCloneBenchDriver extends AbstractProcessor<CtClass> {
    static boolean saveOutput = false;

    static FileBasedConfiguration config = null;
    public static int pathExtractionMode = 1;
    public static boolean encodeAsInRegistercode = false;

    static String outputString = "";
    static String outputFileName = "/home/hanno/CodeCloner/dominator4java/SPOON/resultFiles/analysize/";

    static int countClones = 0;

  static Object monitorAddPaths = new Object();
  static Object monitorOutput = new Object();

  final static Logger logger =
      LoggerFactory.getLogger(SpoonBigCloneBenchDriver.class);

   
  private int successAST, successCFG, successDom, totalMethods,
      totalFiles, successPath;
  private StringBuilder errorLog;
  private boolean errors, output, skipclones, exceptions;
  public static boolean bytecode=false;
    
  //private Path currentFile;
  private static final ThreadLocal<Path> currentFile = new ThreadLocal<>();
  private String workingDirectory, outputDirectory;

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

  public static void main(String[] args) {
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
      // TODO
      int folder = 13;
      //String workingDirectory = "/home/hanno/CodeCloner/BigCloneEval/ijadataset/bcb_reduced/" + folder;
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
            Environment.BYTECODEBASED= configuration.getBoolean("BYTECODEBASEDCLONEDETECTION");
            Environment.USEREGISTERCODE= configuration.getBoolean("REGISTERCODE_STACKCODE");
            Environment.STUBBERPROCESSING=configuration.getBoolean("STUBBERPROCESSING");
            if ( Environment.BYTECODEBASED && Environment.USEREGISTERCODE)
            {
                Environment.BREMOVESMALLPATHES =0.4f;
                Environment.BPATHESDIFF =0.3f;
                Environment.WIDTHLOWERNO=5;
                //Environment.WIDTHUPPERFAKTOR=1.5F;
                Environment.MINNODESNO=3;
                Environment.MAXDIFFNODESNO=7;
            }
            else {
                Environment.BREMOVESMALLPATHES =0.3f;
                Environment.BPATHESDIFF =0.3f;
                //Environment.THRESHOLD=0.15F;
                Environment.WIDTHLOWERNO=3;
                //Environment.WIDTHUPPERFAKTOR=1.3F;


            }

        }catch (NoSuchElementException ex)
        {
            ex.printStackTrace();
        }

      int poolSize=Environment.THREADSIZE;

      if (Environment.BYTECODEBASED) {
          outputFileName += "resultBytecode_" + folder;

        configFileName=Environment.USEREGISTERCODE? "config/Patterns/ConfigRegisterCodePatterns":"config/Patterns/ConfigByteCodePatterns";
        params = new Parameters();
        builder =
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                        .configure(params.properties()
                                .setFileName(configFileName));
        try {
          config = builder.getConfiguration();
        } catch (ConfigurationException e) {
          e.printStackTrace();
        }

        SpoonBigCloneBenchDriver.bytecode=true;
        SimpleDateFormat f= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
        Date startB = new Date(System.currentTimeMillis());
        SpoonBigCloneBenchDriver.outputTuples.addAll(ByteCodePathExtraction.extractPathes(workingDirectory,Environment.MINSIZE,Environment.PATHSINSETS,Environment.TECHNIQUE,config));
        Date endB = new Date(System.currentTimeMillis());
        logger.info(f.format(startB));
        logger.info(f.format(endB));
      }
      else {
          outputFileName += "resultSourcecode_" + folder;

          // init config
          configFileName = "config/Patterns/ConfigSourceCodePatterns";
          params = new Parameters();
          builder =
                  new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                          .configure(params.properties()
                                  .setFileName(configFileName));
          try {
              config = builder.getConfiguration();
          } catch (ConfigurationException e) {
              e.printStackTrace();
          }

          // init pathExtractionMode
          // TODO
          if (config != null){
              int pathExtractionModeConfig = config.getInt("pathExtractionMode");
              if (0 < pathExtractionModeConfig && pathExtractionModeConfig < 4){
                  pathExtractionMode = pathExtractionModeConfig;
              }
              else {
                  try {
                      throw new Exception("pathExtractionMode has Unknown Value: " + pathExtractionModeConfig);
                  }
                  catch (Exception ignored) {}
              }
              if (config.getBoolean("encodeAsInRegistercode")){
                  encodeAsInRegistercode = true;
              }
          }

            // traversing the benchmark directory and calling the Spoon driver

            ForkJoinPool myPool = new ForkJoinPool(poolSize);
            myPool.submit(() -> {
                try {
                    Files.walk(Paths.get(workingDirectory))
                            .collect(Collectors.toList())
                            .parallelStream()
                            .filter(Files::isRegularFile)
                            .forEach(driver::process);
                } catch (IOException e) {
                    System.out.println("ERROR: Unable to access " + workingDirectory);
                    formatter.printHelp(command, options);
                    System.exit(1);
                }
            }).get();

      // logging
      logger.info("Successfully created AST for {} out of {} files",
          driver.successAST, driver.totalFiles);
      logger.info("Successfully created CFG for {} out of {} methods",
          driver.successCFG, driver.totalMethods);
      logger.info("Successfully created DomTree for {} out of {} methods",
          driver.successDom, driver.totalMethods);
      logger.info("Successfully encoded paths for {} out of {} methods",
          driver.successPath, driver.totalMethods);
      }
      long end1=System.nanoTime();

      if (!driver.skipclones) {
        driver.outputTuplesArray= SpoonBigCloneBenchDriver.outputTuples.toArray(new MethodTuple[SpoonBigCloneBenchDriver.outputTuples.size()]);

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
          executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
        }
      }
      long end2=System.nanoTime();
      logger.info("Time create pathes= "+TimeUnit.MILLISECONDS.convert(end1-start, TimeUnit.NANOSECONDS));
      logger.info("Time find clones= "+TimeUnit.MILLISECONDS.convert(end2-end1, TimeUnit.NANOSECONDS));

      logger.info("--- Numbers of Clones: " + countClones);

      if (saveOutput){
          logger.info("Write Result to File " + outputFileName + "...");
          try {
              PrintWriter writer = new PrintWriter(outputFileName);
              writer.print(outputString);
              writer.close();
          } catch (IOException e) {
              e.printStackTrace();
          }
      }
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
      Encoder myEncoder;
      if (!bytecode) {
          if (Environment.TECHNIQUE == EncoderKind.COMPLETEPATH)
              myEncoder = new CompletePathEncoder();
          else if (Environment.TECHNIQUE == EncoderKind.HASH)
              myEncoder = new HashEncoder();
          else if (Environment.TECHNIQUE == EncoderKind.MULTISET)
              myEncoder = new SortedMultisetPathEncoder();
          else
              myEncoder = new AbstractEncoder();
      }
      else {
          if (Environment.TECHNIQUE == EncoderKind.COMPLETEPATH)
              myEncoder = new CompletePathEncoder();
          else if (Environment.TECHNIQUE == EncoderKind.HASH)
              myEncoder = new HashEncoderRegisterCode();
          else if (Environment.TECHNIQUE == EncoderKind.MULTISET)
              myEncoder = new SortedMultisetPathEncoder();
          else
              myEncoder = new AbstractEncoder();
      }

      for (int countIndex1=start;countIndex1<outputTuplesArray.length;countIndex1=countIndex1+add) {
          MethodTuple cf_1=outputTuplesArray[countIndex1];

          for (int coundIndex2=countIndex1+1;coundIndex2<outputTuplesArray.length;coundIndex2++) {
              MethodTuple cf_2=outputTuplesArray[coundIndex2];

              if (!(Environment.BYTECODEBASED && !Environment.STUBBERPROCESSING))
              if((cf_1.info.endLine-cf_1.info.startLine+1)<Environment.MINSIZE ||  (cf_2.info.endLine-cf_2.info.startLine+1)<Environment.MINSIZE)
                  continue;
              // this statement is useless
              // if (cf_1.info.fileName.equals("1359154.java") && cf_2.info.fileName.equals("1359154.java")) { String s=""; }
              if(myEncoder.areTwoDescriptionSetsSimilar(cf_1.encodePathSet,
                                      cf_2.encodePathSet,
                                      Environment.METRIC,
                                      Environment.SORTED, // true=>sorted
                                      Environment.RELATIVE, // true => relativ
                                      Environment.THRESHOLD)) {
                                                          //Levenstein Splitting und Unsplitting  0.35
                                                          //LCS Splitting und Unsplitting 0.3F
                   if(Environment.OUTPUT) {
                        if ((cf_2.info.subDir.equals(cf_1.info.subDir) && cf_1.info.fileName.equals(cf_2.info.fileName)) &&
                                ((cf_1.info.startLine > cf_2.info.startLine && cf_1.info.endLine < cf_2.info.endLine) ||
                                        (cf_2.info.startLine > cf_1.info.startLine && cf_2.info.endLine < cf_1.info.endLine)))
                            continue;
                        StringBuilder builder = new StringBuilder();
                        builder.append(cf_1.info.subDir)
                                .append(",")
                                .append(cf_1.info.fileName);
                       if (!(Environment.BYTECODEBASED && !Environment.STUBBERPROCESSING))
                                builder.append(",")
                                .append(cf_1.info.startLine)
                                .append(",")
                                .append(cf_1.info.endLine);
                       String part1 =builder.toString();
                               builder = new StringBuilder();
                       builder.append(cf_2.info.subDir)
                                .append(",")
                                .append(cf_2.info.fileName);
                       if (!(Environment.BYTECODEBASED && !Environment.STUBBERPROCESSING))
                           builder.append(",")
                                .append(cf_2.info.startLine)
                                .append(",")
                                .append(cf_2.info.endLine);
                       String part2=builder.toString();
                       builder = new StringBuilder();
                       if (!part1.equals(part2)) {
                           builder.append(part1)
                                   .append(",")
                                   .append(part2);

                           synchronized (monitorOutput) {
                              String outp = builder.toString();
                              countClones += 1;
                              System.out.println(outp);
                               if (saveOutput) { outputString = outputString.concat(outp + "\n"); }
                           }
                       }
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
          // make director
          new File(dir).mkdirs();
      }
      // TODO
      List<MethodTuple> tmpList= new ArrayList<>();
      for (Object m : type.getTypeMembers()) {
          try {
              List<List<Encoder>> l=extractGraphs(type, m, dir);
              if (l != null){
                  tmpList.add(new MethodTuple((CtExecutable) m,l,SpoonBigCloneBenchDriver.currentFile.get()));
              }
          } catch (Throwable e) {
              if (errors) { reportErrors(e); }
          }
      }
      if (!skipclones && pathExtractionMode != 1) {
          // get current method output and search for Subfunctions
          ArrayList<MethodTuple> methodOutputs = new ArrayList<>();
          ArrayList<MethodTuple> subFunctions = new ArrayList<>();
          for (MethodTuple currentTuple : tmpList) {
              int startLine = currentTuple.info.startLine;
              int endLine = currentTuple.info.endLine;
              boolean added = false;

              for (int i = 0; i < methodOutputs.size(); i++) {
                  MethodTuple methodTuple = methodOutputs.get(i);
                  int currentStart = methodTuple.info.startLine;
                  int currentEnd = methodTuple.info.endLine;

                  if (startLine > currentStart && endLine < currentEnd) {
                      // first case -> between currentStart and currentEnd
                      // So it is obviously a subfunction
                      subFunctions.add(methodTuple);
                      currentTuple.encodePathSet.addAll(methodTuple.encodePathSet);
                      added = true;
                      break;
                  } else if (startLine < currentStart && endLine > currentEnd) {
                      // second case -> subfunction is already in list
                      ArrayList<MethodTuple> removeLater = new ArrayList<>();
                      // can be over more than one line
                      int c;
                      for (c = i; c < methodOutputs.size(); c++) {
                          currentEnd = methodOutputs.get(c).info.endLine;
                          if (endLine > currentEnd) {
                              removeLater.add(methodOutputs.get(c));
                          } else
                              break;

                      }
                      for (MethodTuple entry : removeLater) {
                          subFunctions.add(entry);
                          currentTuple.encodePathSet.addAll(methodTuple.encodePathSet);
                          methodOutputs.remove(entry);
                      }
                      methodOutputs.add(i, methodTuple);
                      added = true;
                      break;
                  } else if (endLine < currentEnd) {
                      methodOutputs.add(i, methodTuple);
                      added = true;
                      break;
                  }
              }
              if (!added) {
                  methodOutputs.add(currentTuple);
              }
          }
          synchronized (monitorAddPaths){
              outputTuples.addAll(methodOutputs);
              if (pathExtractionMode == 3){
                  outputTuples.addAll(subFunctions);
              }
          }
      }
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
        MethodInfo info = new MethodInfo((CtExecutable) m);
        if ((info.endLine - info.startLine + 1) < Environment.MINSIZE){
          return null;
        }
        // TODO
        /*if (info.startLine != 100 && info.startLine != 272){
            System.out.println("Sourcecode line chooser activate!");
            return null;
        }*/
      }
    try {
       
      // building the control flow graph using the Spoon library
      // cf. https://spoon.gforge.inria.fr
      String cfg_name = methodID(type, (CtExecutable) m) + "_cfg";
      ControlFlowGraph cfg = makeCFG((CtExecutable) m, cfg_name);
      successCFG++;

        // write the cfg and the domtree
        if (config.getBoolean("createCFGGraph")){
            String resultDirectory = config.getString("graphResultDirectory");
            System.out.println("Writing CFG Graph to Directory " + resultDirectory + "...");
            writeToPath(resultDirectory + "/" + cfg_name + ".dot", cfg.toGraphVisText());
        }
      // building the dominator tree using the WALA library
      // cf. https://github.com/wala/WALA
      String domtree_name = methodID(type, (CtExecutable) m) + "_domtree";
      DominatorTree domtree = makeDomTree(cfg, domtree_name);

        if (config.getBoolean("createDominatorTreeGraph")){
            String resultDirectory = config.getString("graphResultDirectory");
            System.out.println("Writing Dominator-tree Graph to Directory " + resultDirectory + "...");
            writeToPath(resultDirectory + "/" + domtree_name + ".dot", domtree.toGraphVisText());
        }

      successDom++;
      String encodePathSet_name = methodID(type, (CtExecutable) m) + "_encodePathSet";

      //System.out.println("Dom Tree created");
      //!!!!!!System.out.println(cfg);!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
      List<List<Encoder>> encodePathSet = domtree.encodePathSet(Environment.PATHSINSETS, Environment.TECHNIQUE, Environment.SETORDER);

      successPath++;

      if (!skipclones && pathExtractionMode == 1) {
        synchronized (monitorAddPaths) {
            this.outputTuples.add(new MethodTuple((CtExecutable) m, encodePathSet,this.currentFile.get()));
        }
      }
      if (output) {
          System.out.println("Writing EncodedPathSet to output directory!");
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
    if (this.exceptions || config.getBoolean("finalNodes")) {
        EnumSet<NaiveExceptionControlFlowStrategy.Options> options;
        options = EnumSet.of(NaiveExceptionControlFlowStrategy.Options.ReturnWithoutFinalizers);
        builder.setExceptionControlFlowStrategy(new NaiveExceptionControlFlowStrategy(options, config, exceptions));
    }
    ControlFlowGraph cfg = builder.build(m, config);
    cfg.setName(name);
    cfg.simplify();
    return cfg;
  }
    
  DominatorTree makeDomTree(ControlFlowGraph cfg, String name) {
      AbstractGraph<ControlFlowNode> dom = Dominators.make(cfg, cfg.entry()).dominatorTree();
      DominatorTree domtree = new DominatorTree(dom, config);
      domtree.setName(name);
      return domtree;
  }

  void writeToPath(String path, String written) {
      try {
          FileWriter fw = new FileWriter(path);
          fw.write(written);
          fw.close();
      }
      catch (IOException ignored) {}
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
      logger.warn("Could not write error file {}", errorFile);
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
