package org.fsu.bytecode;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.AbstractGraph;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.dominators.Dominators;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.dlr.foobar.SpoonBigCloneBenchDriver;
import org.fsu.codeclones.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.baf.Baf;
import soot.baf.BafBody;
import soot.jimple.JimpleBody;
import soot.options.Options;
import soot.tagkit.*;
import soot.toolkits.graph.*;
import soot.util.cfgcmd.CFGToDotGraph;
import soot.util.dot.DotGraph;

import java.io.*;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import soot.*;

public class ByteCodePathExtraction {
    // true > create dot graph for every method long enough

    static final Object monitorAddPathes=new Object();
    static final Object monitorEncoder=new Object();
    static int poolSize= Environment.THREADSIZE;
    static ForkJoinPool myPool = new ForkJoinPool(poolSize);
    final static Logger logger =
            LoggerFactory.getLogger(SpoonBigCloneBenchDriver.class);


    public static List<MethodTuple> extractPathes(String inputFolder, int minLine, EncoderKind pathExtractionMethod, EncoderKind pathEncoderKind,FileBasedConfiguration configuration)
    {
        /*
        inputFolder: working Directory -> Test (traverse)
        minLine: Enviroment.MINSIZE = 15 (default setting)
        pathExtractionMethod: Enviroment.PATHSINSETS = EncoderKind.UNSPLITTING
        pathExtractionKind: Environment.TECHNIQUE = EncoderKind.HASH (how to compare the different paths)
        configuration: how to compare the hash values

        return: All Methods (from all classes) in the current directory and sub Directories

        The Directory has to have a main Folder and inside more Folder(s) for this to work
        Only if this is the case the normal Extract method is going to be called
         */
        HashEncoderByteCode.config=configuration;
        HashEncoderRegisterCode.config=configuration;
        File dir = new File(inputFolder);
        // check if directory exists
        if (!dir.isDirectory())
            Assertions.UNREACHABLE();
        File[] directoryListing = dir.listFiles();
        SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");

        Date start = new Date(System.currentTimeMillis());
        List<MethodTuple> methodTupleList = new ArrayList<>();

        // traverse all files in the working directory
        for (File file: directoryListing) {
            if (file.isDirectory())
            {
                // if there is a directory inside -> call extract function again for the currentFile
                // methodTupleList will contain all Methods from every subdirectory in Test
                methodTupleList.addAll(extract(file.getPath(),minLine,pathExtractionMethod, pathEncoderKind, configuration));
            }
        }
        Date end = new Date(System.currentTimeMillis());
        logger.info("(ByteCodePathExtraction) START: " + formatter.format(start));
        logger.info("(ByteCodePathExtraction) END: " + formatter.format(end));
        return methodTupleList;
    }

    private static List<MethodTuple> extract(String directoryPath, int minLine, EncoderKind pathExtractionMethod, EncoderKind pathEncoderKind, FileBasedConfiguration configuration)
    {
        /*
        For a directory > traverse all files inside and get all methods inside the file
         */
        HashSet<String> classAndMethodNames= new HashSet<String>();
        //Count Variables
        AtomicInteger classes= new AtomicInteger();
        AtomicInteger classesProcessed= new AtomicInteger();
        int classesWithException=0;

        JarClassList jarClassList = new JarClassList();

        // look into the jar files and add all subclasses into jarClassList
        getHashTableOfJarsAndClassnames(directoryPath,jarClassList);

        //Set Soot ClassPath
        Scene.v().setSootClassPath(System.getProperty("java.home") + "/lib/jce.jar:" + System.getProperty("java.home") + "/lib/rt.jar:");

        List<String> keys = new ArrayList<>();
        int count=0;

        List<MethodTuple> methodTupleList = new ArrayList<>();

        // get config settings:

        int pathEditSetting = configuration.getInt("pathExtractionMode");
        if (pathEditSetting < 1 || pathEditSetting > 3){
            // unknown Setting
            System.out.println("****************************");
            System.out.println("Please check the ConfigRegisterCodePatterns File: pathEditSetting has to be between 1 and 3");
            System.exit(0);
        }

        while (true) {
            // while jarClassList has entries increment count and extract the classes
            int ret=addClassesToSoot(jarClassList, keys, count);
            // processClasses(jarClassList);

            if (ret==0)
                break; // no more classes to process

            final List<String> finalKeys = keys;
            try {
                myPool.submit(() -> {
                    finalKeys.parallelStream().forEach(p -> {
                        int number = jarClassList.getIndex(p) + 1;
                        logger.info("Working on " + number + "/" + jarClassList.size() + ":" + p);

                        // Iterate over Class Files in Jar
                        List<MethodTuple> internMethodTupleList = new ArrayList<>();
                        List<MethodTuple> subFunctions = new ArrayList<>();

                        int startLine;
                        int endLine;
                        int currentStart;
                        int currentEnd;
                        boolean added;
                        MethodTuple currentTuple;

                        for (String className : jarClassList.get(p)) {
                            classes.getAndIncrement();
                            // Extract Class
                            List<MethodTuple> methodTuples = classExtraction(p, className, jarClassList, minLine, pathExtractionMethod, pathEncoderKind, configuration);

                            // check subfunctions
                            if (methodTuples != null) {
                                if (pathEditSetting == 1) {
                                    synchronized (ByteCodePathExtraction.monitorAddPathes) {
                                        methodTupleList.addAll(methodTuples);
                                    }
                                }
                                else {
                                    for (MethodTuple methodTuple : methodTuples) {
                                        startLine = methodTuple.info.startLine;
                                        endLine = methodTuple.info.endLine;
                                        added = false;

                                        for (int i = 0; i < internMethodTupleList.size(); i++) {
                                            currentTuple = internMethodTupleList.get(i);
                                            currentStart = currentTuple.info.startLine;
                                            currentEnd = currentTuple.info.endLine;

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
                                                for (c = i; c < internMethodTupleList.size(); c++) {
                                                    currentEnd = internMethodTupleList.get(c).info.endLine;
                                                    if (endLine > currentEnd) {
                                                        removeLater.add(internMethodTupleList.get(c));
                                                    } else
                                                        break;

                                                }
                                                for (MethodTuple entry : removeLater) {
                                                    subFunctions.add(entry);
                                                    methodTuple.encodePathSet.addAll(currentTuple.encodePathSet);
                                                    internMethodTupleList.remove(entry);
                                                }
                                                internMethodTupleList.add(i, methodTuple);
                                                added = true;
                                                break;
                                            } else if (endLine < currentEnd) {
                                                internMethodTupleList.add(i, methodTuple);
                                                added = true;
                                                break;
                                            }
                                        }
                                        if (!added) {
                                            internMethodTupleList.add(methodTuple);
                                        }
                                    }
                                }
                            }
                            classesProcessed.getAndIncrement();
                        }
                        if (pathEditSetting == 2)
                            synchronized (ByteCodePathExtraction.monitorAddPathes) {
                                methodTupleList.addAll(internMethodTupleList);
                            }
                        else if (pathEditSetting == 3)
                            synchronized (ByteCodePathExtraction.monitorAddPathes) {
                                methodTupleList.addAll(internMethodTupleList);
                                methodTupleList.addAll(subFunctions);
                            }
                    });
                }).get();
            }catch (InterruptedException | ExecutionException e){e.printStackTrace();}

            count = count + ret;
            keys=new ArrayList<>();
        }
        logger.info("classes total: "+ classes+" classes processed: "+classesProcessed+" classes with exception: "+classesWithException);
        return methodTupleList;
    }

    private static List<MethodTuple> classExtraction(String key, String className, JarClassList jarClassList,int minLine, EncoderKind pathExtractionMethod, EncoderKind pathEncoderKind, FileBasedConfiguration configuration)
    {
        /*
        Returns a List of Methods from a Class (except abstract or native)
        key: absolute Path to the current jar file
         */
        // load class with body
        SootClass sootClass = Scene.v().loadClass(className, SootClass.HIERARCHY);
        if (sootClass.isInterface())
            return null;

        // get List of methods from class
        List<SootMethod> listSootMethods = sootClass.getMethods();

        // iterate over the methods
        List<MethodTuple> methodTuples=new ArrayList<>();

        for (SootMethod sootMethod : listSootMethods) {
            if (sootMethod.isNative() || sootMethod.isAbstract())
                continue;

            // extract method
            MethodTuple methodTuple = extractMethod(sootMethod, className, jarClassList, key, minLine, pathExtractionMethod, pathEncoderKind, configuration);
            if (methodTuple != null)
                methodTuples.add(methodTuple);
        }

        return methodTuples;
    }

    private static MethodTuple extractMethod(SootMethod sootMethod, String className, JarClassList jarClassList, String key, int minLine, EncoderKind pathExtractionMethod, EncoderKind pathEncoderKind,FileBasedConfiguration configuration)
    {
        /* returns the current SootMethod as a MethodTuple there the Paths in the CFG are encoded according to the
        Environment Variables
         */
        // Use the Tag created by Stubber to get the actual Start and End Line

        AnnotationInterfaceContent annotationInterfaceContent;

        if (Environment.STUBBERPROCESSING) {
            annotationInterfaceContent = getStartAndEndLineFromTag(sootMethod);

            if (annotationInterfaceContent == null || annotationInterfaceContent.getLength() < minLine)
                return null;
        }
        else {
            annotationInterfaceContent = new AnnotationInterfaceContent("",sootMethod.getSignature(),0,0);
        }
        // choose two single methods to be extracted only
        // TODO

        /*if (annotationInterfaceContent.getStartLine() != 102 && annotationInterfaceContent.getStartLine() != 184){
            System.out.println("Selection on!, BytecodePathExtraction, extractMethod");
            return null;
        }*/

        try {
            sootMethod.retrieveActiveBody();
        }
        catch (Exception ex) {
            logger.error(ex.getMessage());
            return null;
        }

        Body b;
        if (Environment.USEREGISTERCODE){
            b = sootMethod.getActiveBody();
        }
        else {
            JimpleBody jimpleBody=(JimpleBody)sootMethod.getActiveBody();
            BafBody bafBody = Baf.v().newBody(jimpleBody);
            PackManager.v().getPack("bop").apply(bafBody);
            PackManager.v().getPack("tag").apply(bafBody);
            if (Options.v().validate()) {
                bafBody.validate();
            }
            b=bafBody;
        }

        /*if (annotationInterfaceContent.getLength() > b.getUnits().size()){
            // try to ignore Functions there exists inner function
            return null;
        }*/
        // create the Controlflowgraph/ Unitgraph

        //unitPatchingChain.insertAfter(ug.getHeads(),startUnit);
        //CFGGraphPicture("tmp",ug,null);
        //logger.info(number+"/"+jarClassList.size()+" "+className+" "+sootMethod.getName()+"\n");

        // create special Name for method
        String classAndMethodName=jarClassList.toString(key)+","+annotationInterfaceContent.getStartLine()+","+annotationInterfaceContent.getEndLine();

        // create the ControlFlowGraph
        UnitGraph controlFlowUnitGraph;
        //synchronized (StandardEncoderByteCode.monitorEncoder) {
        int exceptionCounter;
        if (configuration.getBoolean("exceptionMode")){
            controlFlowUnitGraph = new ControlFlowExceptionalUnitGraph(b, configuration);
            exceptionCounter = ((ControlFlowExceptionalUnitGraph) controlFlowUnitGraph).exceptionCount;
        }
        else {
            controlFlowUnitGraph = new ControlFlowBriefUnitGraph(b, configuration);
            exceptionCounter = ((ControlFlowBriefUnitGraph) controlFlowUnitGraph).exceptionCount;
        }

        if (configuration.getBoolean("createCFGGraph")) {
            System.out.println("create CFG Graph Picture!");
            CFGGraphPicture(classAndMethodName + "_cfg", controlFlowUnitGraph, null, b);
        }
        //}
        //create the dominatortree
        boolean setDominatorGraphPicture = configuration.getBoolean("createDominatorTreeGraph");
        AbstractGraph<Unit> dom = Dominators.make(
                (Graph<Unit>) controlFlowUnitGraph,
                controlFlowUnitGraph.getHeads().get(0)).dominatorTree();
        DominatorTreeUnitGraph dominatorTreeUnitGraph = new DominatorTreeUnitGraph(classAndMethodName, dom, controlFlowUnitGraph.getHeads(), sootMethod, setDominatorGraphPicture);

        List<List<Unit>> pathes=null;
        if (pathExtractionMethod==EncoderKind.UNSPLITTING)
            pathes = dominatorTreeUnitGraph.getFullPathes();
        else if (pathExtractionMethod==EncoderKind.SPLITTING)
            pathes = dominatorTreeUnitGraph.getSplitPathes();
        else
            Assertions.UNREACHABLE("Unimplemented path extraction method in ByteCode path extraction.");
        //return null;
        List<List<Encoder>> encodedPathes=null;
        if (pathEncoderKind==EncoderKind.HASH) {
            if (Environment.USEREGISTERCODE){
                if (configuration.getBoolean("countExceptions")){
                    encodedPathes = HashEncoderRegisterCode.encodeDescriptionSetStatic(pathes);
                    List<Encoder> exceptionCounterEncoded = new ArrayList<>();
                    List<Integer> encodedNode= new ArrayList<>();

                    //Code[] codes = StandardEncoderRegisterCode.encodeParameterCount(exceptionCounter);
                    encodedNode.add(exceptionCounter);
                    //Arrays.stream(codes).forEach(i->encodedNode.add(1));

                    exceptionCounterEncoded.add(new HashEncoderRegisterCode(encodedNode.stream().mapToInt(Integer::intValue).toArray()));
                    encodedPathes.add(0, exceptionCounterEncoded);
                }
                else{
                    encodedPathes = HashEncoderRegisterCode.encodeDescriptionSetStatic(pathes);
                }
            }
            else{ // Environment.UseRegisterCode==false
                if (configuration.getBoolean("countExceptions")){
                    encodedPathes = HashEncoderByteCode.encodeDescriptionSetStatic(pathes);
                    List<Encoder> exceptionCounterEncoded = new ArrayList<>();
                    List<Integer> encodedNode= new ArrayList<>();

                    //Code[] codes = StandardEncoderRegisterCode.encodeParameterCount(exceptionCounter);
                    encodedNode.add(exceptionCounter);
                    //Arrays.stream(codes).forEach(i->encodedNode.add(1));

                    exceptionCounterEncoded.add(new HashEncoderByteCode(encodedNode.stream().mapToInt(Integer::intValue).toArray()));
                    encodedPathes.add(0, exceptionCounterEncoded);
                }
                else {
                    encodedPathes = HashEncoderByteCode.encodeDescriptionSetStatic(pathes);
                }
            }
        }
        else {
            Assertions.UNREACHABLE("Unimplemented path encoder in ByteCode path extraction.");
        }
        if (Environment.STUBBERPROCESSING) {
            return new MethodTuple(encodedPathes, jarClassList.getSubFolder(key), jarClassList.getName(key), annotationInterfaceContent.getStartLine(), annotationInterfaceContent.getEndLine());
        }
        return new MethodTuple(encodedPathes,jarClassList.getSubFolder(key), sootMethod.getSignature()/*.replace("<","{").replace(">","}")*/
                , annotationInterfaceContent.getStartLine(),annotationInterfaceContent.getEndLine() );
    }

    private static void CFGGraphPicture(String name, UnitGraph ug, List<Unit> colorUnits, Body b)
    {
        // TODO
        CFGToDotGraph cfgtodot = new CFGToDotGraph();
        //cfgtodot.setExceptionalControlFlowAttr("color","lightgray");
        //DotGraph dg = cfgtodot.drawCFG((ExceptionalGraph<?>) ug);
        DotGraph dg = cfgtodot.drawCFG(ug, b);

        if (colorUnits!=null) {
            Iterator<Unit> it = ug.iterator();
            int i = 0;
            while (it.hasNext()) {
                Unit unit = (Unit) it.next();
                if (colorUnits.contains(unit)) {
                    dg.getNode(i + "").setAttribute("color", "red");
                    dg.getNode(i + "").setAttribute("style", "dotted");
                    dg.getNode(i + "").setAttribute("style", "rounded");
                }
                i++;
            }
        }
        dg.plot(name + DotGraph.DOT_EXTENSION);
        MutableGraph g = null;
        try {
            //System.out.println(System.getProperty("user.dir"));
            BufferedInputStream bi = new BufferedInputStream(new FileInputStream(name+DotGraph.DOT_EXTENSION));
            g = Parser.read(bi);
            Graphviz.fromGraph(g).render(Format.PNG).toFile(new File(name+".png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static AnnotationInterfaceContent getStartAndEndLineFromTag(SootMethod sootMethod) {
        List<Tag> tagList=sootMethod.getTags();
        if (tagList.size()!=0)
        {
            Tag t =tagList.get(0);
            for (Tag tag : tagList) {
                t=tag;
                if (t instanceof VisibilityAnnotationTag) {
                    VisibilityAnnotationTag visibilityAnnotationTag = (VisibilityAnnotationTag) t;
                    ArrayList<AnnotationTag> annotationTagArrayList = visibilityAnnotationTag.getAnnotations();
                    if (annotationTagArrayList.size() == 1) {
                        AnnotationTag annotationTag = annotationTagArrayList.get(0);
                        String type = annotationTag.getType();
                        if (annotationTag.getType().contains("BCBIdentifierOriginalSourceCode")) {
                            Collection<AnnotationElem> annotationElemCollection = annotationTag.getElems();
                            String subFolder="";
                            String name="";
                            int startLine=0;
                            int endLine=0;
                            AnnotationElem annotationElem = annotationElemCollection.toArray(new AnnotationElem[0])[0];
                            if (annotationElem.getName().equals("SubFolder")) {
                                subFolder = ((AnnotationStringElem) annotationElem).getValue();
                            }
                            annotationElem = annotationElemCollection.toArray(new AnnotationElem[0])[1];
                            if (annotationElem.getName().equals("FileName")) {
                                name = ((AnnotationStringElem) annotationElem).getValue();
                            }
                            annotationElem = annotationElemCollection.toArray(new AnnotationElem[0])[2];
                            if (annotationElem.getName().equals("StartLine")) {
                                startLine = ((AnnotationIntElem) annotationElem).getValue();
                            }
                            annotationElem = annotationElemCollection.toArray(new AnnotationElem[0])[3];
                            if (annotationElem.getName().equals("EndLine")) {
                                endLine = ((AnnotationIntElem) annotationElem).getValue();
                            }
                            if (!subFolder.equals("") && !name.equals(""))
                                return new AnnotationInterfaceContent(subFolder, name, startLine, endLine);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static int addClassesToSoot(JarClassList jarClassList, List<String> keys, int count) {
        /*
        jarClassList: List of subclasses in every .jar file (jarName, classList, name, subFolder)
        keys: empty List at the beginning
        count: current count

        Basically iterate over every element in the jarClassList, for every class create Soot Class
         */

        // Reset Soot
        G.reset();

        //Set Soot options
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().ignore_resolution_errors();
        Options.v().ignore_classpath_errors();
        Options.v().ignore_resolving_levels();
        Options.v().set_wrong_staticness(Options.wrong_staticness_ignore);

        String s=Scene.v().defaultClassPath();
        Scene.v().setSootClassPath(Scene.v().defaultClassPath());
        List<String> dirs= new ArrayList<>();

        int c=-1;
        int retCount=0;
        // key is the absolute path to the file f.e.: /home/hanno/CodeCloner/dominator4java/SPOON/Test/3/selected/342029.jar
        // iterate over every entry in the jarClassList (jar files)
        for (String key:jarClassList) {
            c++;
            if (c<count)
                continue;
            if (retCount==400)
                break;
            if (key.endsWith(".class"))
            {
                List l = Arrays.asList(key.split("/"));
                l=new LinkedList<>(l);
                l.remove(l.size()-1);
                String tm=String.join("/",l)+"/";
                dirs.add(tm);
                Scene.v().extendSootClassPath(tm);
            }
            else {
                dirs.add(key);
                Scene.v().extendSootClassPath(key);
            }
            keys.add(key);
            retCount++;

            // iterate over every class in the current .jar file and add Soot class
            for (String className :jarClassList.get(key)) {
                // Add Class as basic class to soot
                Scene.v().addBasicClass(className, SootClass.SIGNATURES);
            }
        }
        Options.v().set_process_dir(dirs);
        Options.v().set_whole_program(true);
        Options.v().set_exclude(new ArrayList<String>());
        Scene scene = Scene.v();
        try {
            Field f = scene.getClass().getDeclaredField("excludedPackages"); //NoSuchFieldException
            f.setAccessible(true);
            f.set(scene,new LinkedList<>());
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        try {
            // try to load necessary Classes
            Scene.v().loadNecessaryClasses();
        }
        catch (Exception e){
            e.printStackTrace();
            return retCount;
        }
        return retCount;
    }


    private static void getHashTableOfJarsAndClassnames(String directoryPath,JarClassList jarClassList)
    {
        /*
        directoryPath: Path of current Directory
        jarClassList: Empty Jar List
         */
        File dir = new File(directoryPath);
        if (!dir.isDirectory())
            Assertions.UNREACHABLE();
        File[] directoryListing = dir.listFiles();
        // traverse every File in the current Directory
        // call recursively for subdirectories
        assert directoryListing != null;
        for (File file: directoryListing) {
            if (file.isDirectory())
            {
                getHashTableOfJarsAndClassnames(file.getAbsolutePath(),jarClassList);
            }
        }
        // iterate over every .jar file found in every subdirectory
        for (File file: directoryListing) {
            if (file.getName().endsWith("jar")) {
                JarFile jar = null;
                // check if the file is indeed a .jar file
                try {
                    jar = new JarFile(file.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                    Assertions.UNREACHABLE();
                }
                List<String> classList = new ArrayList<String>();
                // iterate over every jar entry in the jar file > get classes
                for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements(); ) {
                    JarEntry entry = entries.nextElement();

                    if (entry.getName().endsWith(".class") ) {
                        // change "_430580/StubClass/Exception_14.class" to "_430580.StubClass.Exception_14"
                        if (entry.getName().contains("/")) {
                            classList.add(entry.getName().replace(".class", "").replace("/", ".")/*.substring(entry.getName().lastIndexOf("/")+1)*/);
                        }
                        else
                            classList.add(entry.getName().replace(".class", ""));
                    }
                }
                // for every jar file look at the class list and save the name of the jar file, the classList
                // the name (after changing it) and the path to the file
                if (classList.size() > 0) {
                    String[] splittetPath=dir.getPath().split("/");
                    String subFolder=splittetPath[splittetPath.length-1];
                    String name=file.getName().replace(".jar",".java");
                    // jar.getName is the complete path to the jar file: JAR NAME: /home/hanno/CodeCloner/dominator4java/SPOON/Test/3/selected/430580.jar
                    // name is only then end and the type us replaced with java: FILE NAME: 430580.java
                    // splittedPath: ELEMENT: split path to super Folder
                    // classList: contains all found class files in the current jar file
                    jarClassList.add(jar.getName(), classList,name,subFolder);
                }
            }
            else if (file.getName().endsWith(".class")) {
                List<String> classList = new ArrayList<String>();
                classList.add(file.getName().replace(".class", ""));

                // for every jar file look at the class list and save the name of the jar file, the classList
                // the name (after changing it) and the path to the file
                if (classList.size() > 0) {
                    String[] splittetPath=dir.getPath().split("/");
                    String subFolder=splittetPath[splittetPath.length-1];
                    String name=file.getName().replace(".jar",".java");
                    // jar.getName is the complete path to the jar file: JAR NAME: /home/hanno/CodeCloner/dominator4java/SPOON/Test/3/selected/430580.jar
                    // name is only then end and the type us replaced with java: FILE NAME: 430580.java
                    // splittedPath: ELEMENT: split path to super Folder
                    // classList: contains all found class files in the current jar file
                    jarClassList.add(file.getAbsolutePath(), classList,name,subFolder);
                }
            }
        }
    }
}
