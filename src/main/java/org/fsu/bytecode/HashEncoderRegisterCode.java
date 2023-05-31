package org.fsu.bytecode;

import com.ibm.wala.util.debug.Assertions;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.fsu.codeclones.*;
import soot.Unit;
import soot.util.dot.DotGraph;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Stream;

public class HashEncoderRegisterCode extends Encoder<Unit> {
    static int counter = 1;

    private int[] encoding = null;
    public static FileBasedConfiguration config;

    private static class Node {
        private ArrayList<Node> nextNodes;
        private String text;
        private int index;

        public Node(String text, int index){
            this.text = text;
            this.index = index;
            this.nextNodes = new ArrayList<Node>();
        }

        public String getCompleteNodeName(){
            return this.index + ": " + this.text;
        }

        public void setNextNode(Node node){
            this.nextNodes.add(node);
        }

        public void setNextNodeAll(ArrayList<Node> nodes){
            for (Node node : nodes)
                setNextNode(node);
        }

        public String getText(){
            return this.text;
        }

        public int getIndex() { return this.index; }

        public boolean searchForNode(String text){
            for (Node node : nextNodes){
                if (node.getText().equals(text))
                    return true;
            }
            return false;
        }

        public Node getNextNode(String text){
            for (Node node : nextNodes){
                if (node.getText().equals(text))
                    return node;
            }
            return null;
        }

        public Node copyNode(){
            Node returnNode = new Node(this.text, this.index);
            returnNode.setNextNodeAll(this.nextNodes);
            return returnNode;
        }
    }

    public HashEncoderRegisterCode(){}


    public HashEncoderRegisterCode(int[] node) {
        this.encoding = node;
    }


    static public List<List<Encoder>> encodeDescriptionSetStatic(List<List<Unit>> it) {
        /*
        it: List of Paths (List<List<Unit>>)

        each unit will be encoded with one or multiple integers
         */
        // TODO
        //HashEncoderRegisterCode.createDotGraph(it, "/home/hanno/CodeCloner/dominator4java/SPOON/subTest/output.dot");

        List<List<String[]>> encodedPathStrings = new ArrayList<>();
        List<List<String[]>> encodedPathStringsNEW = new ArrayList<>();

        List<List<Encoder>> encodedPaths = new ArrayList<>();
        int longestPath=0;

        List<List<Encoder>> encodedPathsNEW = new ArrayList<>();

        boolean virtNodes = config.getBoolean("virtualNodes");
        boolean ifNodeSimplify = config.getBoolean("ifNodeSimplify");

        boolean parameterNodes = config.getBoolean("parameterNodes");
        boolean params;
        boolean stringBuilder = config.getBoolean("stringBuilder");
        boolean println = config.getBoolean("println");
        boolean positionInStringBuilder = false;

        if (config.getBoolean("removeSimilarPaths") && it.size()<20) { it = checkForSimilarPaths(it); }


        for (List<Unit> path : it) {
            List<String[]> encodedPathString = new ArrayList<>();
            List<Encoder> encodedPath = new ArrayList<>();
            List<Unit> virtualNodes = new ArrayList<>();
            int parameterCount = 0;
            params = parameterNodes;

            for (Unit unit :path) {
                if (!unit.toString().contains("@this") && parameterNodes && params){
                    if (unit.toString().contains("@parameter")){
                        parameterCount += 1;
                        continue;
                    }
                    // add encoded Parameter Count to result graph TODO
                    ArrayList<Integer> tempNode = new ArrayList<>();

                    Code[] c = StandardEncoderRegisterCode.encodeParameterCount(parameterCount);

                    encodedPathString.add(codeListToStringList(c, new ArrayList<>(), false));

                    Arrays.stream(c).forEach(i->tempNode.add(i.name().hashCode()));
                    encodedPath.add(new HashEncoderRegisterCode(tempNode.stream().mapToInt(Integer::intValue).toArray()));
                    params = false;
                }
                // if VirtualNodes flag is set, unite all virtual Nodes
                if (!virtNodes) {
                    // test for $
                    if (unit.toString().startsWith("$")) {
                        virtualNodes.add(unit);
                        continue;
                    }
                }
                // all names of called functions in the current statement
                List<String> names= new ArrayList<>();

                Code[] codes = StandardEncoderRegisterCode.encodeUnit(unit,config,names);

                List<Integer> encodedNode = new ArrayList<>();
                if (codes!=null && codes.length > 0) {
                    boolean cond = false;
                    if (!stringBuilder && Arrays.equals(codes, new Code[]{Code.ASSIGN, Code.STRING})){
                        names = new ArrayList<>();
                        positionInStringBuilder = true;
                    }
                    else if (virtualNodes.size()>0 && !config.getBoolean("ignoreVirtualNodes")) {
                        Code[] virtualCodes;
                        if (positionInStringBuilder){
                            positionInStringBuilder = false;
                            virtualCodes= StandardEncoderRegisterCode.encodeVirtualUnits(virtualNodes, config, new ArrayList<>());
                        }
                        else {
                            virtualCodes= StandardEncoderRegisterCode.encodeVirtualUnits(virtualNodes, config, names);
                        }
                        if (virtualCodes.length>0) {
                            /*codes = Stream.concat(Arrays.stream(codes), Arrays.stream(virtualCodes))
                                    .toArray(size -> (Code[]) Array.newInstance(Code.class, size));*/
                            codes = Stream.concat(Arrays.stream(virtualCodes), Arrays.stream(codes))
                                    .toArray(size -> (Code[]) Array.newInstance(Code.class, size));
                        }

                        // check for patterns in codes containing virtual Nodes
                        // check condition
                        if (codes.length > 1 && ifNodeSimplify){
                            ArrayList<Code> newCodes = new ArrayList<>();
                            boolean first = true;
                            for (Code c : codes){
                                if (cond){
                                    if (first){
                                        first = false;
                                        newCodes.add(Code.COND);
                                    }
                                    newCodes.add(c);
                                    continue;
                                }
                                if (c == Code.COND){
                                    ArrayList<Integer> tempNode = new ArrayList<>();
                                    Arrays.stream(newCodes.toArray(new Code[0])).forEach(i->tempNode.add(i.name().hashCode()));
                                    names.forEach(i->tempNode.add(i.hashCode()));

                                    encodedPath.add(new HashEncoderRegisterCode(tempNode.stream().mapToInt(Integer::intValue).toArray()));
                                    encodedPathString.add(codeListToStringList(newCodes.toArray(new Code[0]), names, false));

                                    names = new ArrayList<>();
                                    newCodes = new ArrayList<>();
                                    cond = true;
                                }
                                else {
                                    newCodes.add(c);
                                }
                            }
                            codes = newCodes.toArray(new Code[0]);
                        }
                        virtualNodes = new ArrayList<>();
                    }
                    if (!println && names.contains("println")){
                        continue;
                    }

                    encodedPathString.add(codeListToStringList(codes, names, false));

                    Arrays.stream(codes).forEach(i->encodedNode.add(i.name().hashCode()));
                    if (names.size() > 0) {
                        names.forEach(i->encodedNode.add(i.hashCode()));
                    }
                    // encodedNode is a list of Integer > Hash values of current Node(s)
                    encodedPath.add(new HashEncoderRegisterCode(encodedNode.stream().mapToInt(Integer::intValue).toArray()));
                }
            }
            if (encodedPath.size()>longestPath) { longestPath = encodedPath.size(); }
            encodedPaths.add(encodedPath);

            encodedPathStrings.add(encodedPathString);
        }

        // add every sufficient long path to encodedPathsNEW and return it
        for (int i = 0; i < encodedPaths.size(); i++){
            List<Encoder> path = encodedPaths.get(i);
            if (path.size() < Environment.BREMOVESMALLPATHES * longestPath) { continue; }
            encodedPathsNEW.add(path);
            encodedPathStringsNEW.add(encodedPathStrings.get(i));
        }

        if (config.getBoolean("createEncodedDotGraph")){
            System.out.println("Creating Code[] Dot Graph Separate...");
            counter += 1;
            createDotCodeListGraphSeparate(encodedPathStringsNEW, "outputEncoding_separate" + it.get(0).get(0).toString().split("\\.")[0].split("_")[1] + ".dot");
        }
        //printEncodedPaths(encodedPathsNEW);

        return encodedPathsNEW;
    }

    private static String[] codeListToStringList(Code[] codes, List<String> names, boolean cond){
        ArrayList<String> result = new ArrayList<>();
        for (Code c : codes){
            result.add(c.toString());
        }
        /*if (names.size() > 0 && !cond){
            result.add(names.get(0));
        }*/
        result.addAll(names);
        return result.toArray(new String[0]);
    }

    private static void createDotGraph(List<List<Unit>> paths, String outputFileName) {
        DotGraph dotGraph=new DotGraph("dotGraph.dot");
        HashMap<Unit,String> mapping= new HashMap<>();

        String headText = paths.get(0).get(0).toString();
        Node headNode = new Node(headText, 1);

        int i = 1;
        String name = i + "_" + headText;
        dotGraph.drawNode(name);
        mapping.put(paths.get(0).get(0), name);
        i++;

        for (List<Unit> path : paths){
            Node currentNode = headNode;
            Unit formerUnit = path.get(0);
            int c = 0;
            for (Unit unit : path){
                if (c == 0){ c++; continue; }
                String unitText = unit.toString();
                assert currentNode != null;
                if (currentNode.searchForNode(unitText)){
                    currentNode = Objects.requireNonNull(currentNode.getNextNode(unitText));
                }
                else {
                    currentNode.setNextNode(new Node(unitText, i));
                    currentNode = currentNode.getNextNode(unitText);

                    name = i + "_" + unitText;
                    dotGraph.drawNode(name);
                    mapping.put(unit, name);

                    dotGraph.drawEdge(mapping.get(formerUnit), mapping.get(unit));

                    i++;
                }
                formerUnit = unit;
            }
        }
        dotGraph.plot(outputFileName);
    }

    private static void createDotCodeListGraphSeparate(List<List<String[]>> paths, String outputFileName) {
        /*
        create for every path a separate chain -> so dont unite all paths since this is
        technically not correct for 2 or more paths
         */
        //printCodeGraph(paths);
        DotGraph dotGraph=new DotGraph("dotGraph_separatePaths.dot");
        int i = 1;
        String name;

        for (List<String[]> path : paths){
            String formerCodeList = null;

            int c = 0;
            for (String[] codeList : path){
                StringBuilder unitText = new StringBuilder();
                for (String singleCode : codeList){
                    unitText.append(", ");
                    unitText.append(singleCode);
                }
                name = i + "_" + unitText;
                dotGraph.drawNode(name);
                if (c != 0) {
                    dotGraph.drawEdge(formerCodeList, name);
                }
                formerCodeList = name;
                c++;
                i++;
            }
        }
        dotGraph.plot(outputFileName);
    }

    private static void printPath(List<List<Unit>> pathOfPaths){
        int i = 1;
        for (List<Unit> path : pathOfPaths){
            if (path.size() <= 1){
                continue;
            }
            System.out.println("********* PATH " + i + " ************");
            i += 1;
            int u = 1;
            for (Unit unit : path){
                System.out.print("Unit " + u + ": ");
                u += 1;
                System.out.println(unit.toString());
            }
            System.out.println("********** END **********");
        }
    }

    private static void printEncodedPaths(List<List<Encoder>> pathOfEncoders){
        int i = 1;
        for (List<Encoder> path : pathOfEncoders){
            if (path.size() <= 1){
                continue;
            }
            System.out.println("********* PATH " + i + " ************");
            i += 1;
            printSingleEncodedPath(path);

            System.out.println("********** END **********");
        }
    }

    private static void printSingleEncodedPath(List<Encoder> path){
        int u = 1;
        for (Encoder unit : path){
            System.out.print("Unit " + u + ": ");
            u += 1;
            for (int enc : unit.getEncoding()){
                System.out.print(enc + ", ");
            }
            System.out.print("\n");
        }
    }

    public static void printCodeGraph(List<List<Code[]>> paths){
        System.out.println("Path Count: " + paths.size());
        int counter = 1;
        for (List<Code[]> path : paths){
            System.out.println("Path: " + counter + ", with Length: " + path.size() + "\n");
            int unitCounter = 1;
            for (Code[] codeList : path){
                System.out.print("Unit: " + unitCounter + ": ");
                for (Code c : codeList){
                    System.out.print(c + ", ");
                }
                System.out.print("\n");
                unitCounter++;
            }
            System.out.println("\n");
            counter++;
        }
    }

    public static List<List<Unit>> checkForSimilarPaths(List<List<Unit>> paths){
        /*
        add every path different from
        has to be sorted though (longest to shortest)
         */
        // sort paths
        List<List<Unit>> sortedPaths = new ArrayList<>();
        for (int c=0;c<paths.size();c++){
            int maxLength = 0;
            List<Unit> maxPath = new ArrayList<>();

            for (List<Unit> path : paths){
                if (sortedPaths.contains(path)){ continue; }
                if (path.size() > maxLength){
                    maxLength = path.size();
                    maxPath = path;
                }
            }
            sortedPaths.add(maxPath);
        }
        List<List<Unit>> newPaths = new ArrayList<>();
        boolean add;
        for (int c1 = 0; c1 < paths.size(); c1++){
            add = true;
            List<Unit> path1 = sortedPaths.get(c1);
            for (int c2 = 0; c2 < newPaths.size(); c2++){
                if (c1 != c2){
                    List<Unit> path2 = newPaths.get(c2);
                    int diff = similarPaths(path1, path2);
                    if (diff <= config.getInt("minPathDiff")){
                        add = false;
                        break;
                    }
                }
            }
            if (add) { newPaths.add(path1); }
        }
        return newPaths;
    }

    public static int similarPaths(List<Unit> path1, List<Unit> path2){
        /*
        Test for 2 path2 if they are similar -> one can be ignored later (only insignificant difference)
         */
        // make sure path 1 is the shorter one
        if (path1.size() > path2.size()){
            List<Unit> tmp = path1;
            path1 = path2;
            path2 = tmp;
        }
        Unit start1;
        Unit start2;
        for (int counter = 0; counter < path1.size(); counter++){
            start1 = path1.get(counter);
            start2 = path2.get(counter);
            if (start1 != start2){
                return path1.size() - counter;
            }
        }
        return 0;
    }

    @Override
    public List<List<Encoder>> encodeDescriptionSet(List<List<Unit>> it) {
        return HashEncoderRegisterCode.encodeDescriptionSetStatic(it);
    }

    @Override
    public boolean isPathInDescriptionSet(List<Encoder> path, List<List<Encoder>> set, MetricKind metric, boolean relativ, float threshold) {
        return false;
    }

    @Override
    public boolean areTwoDescriptionSetsSimilar(List<List<Encoder>> set1, List<List<Encoder>> set2, MetricKind metric, boolean sorted, boolean relativ, float threshold) {
        HammingDistance.RELATIV = relativ;
        HammingDistance.THRESHOLD = threshold;
        HammingDistance.SORTED = sorted;

        if(set1.size()==0 || set2.size()==0){
            return false;
        }

        // ensure set1 has the same or less amount of paths
        if(set1.size() > set2.size()){ // it will check if the smaller encode is equivalent to the larger encode
            List<List<Encoder>> tmp = set1;
            set1 = set2;
            set2 = tmp;
        }

        if(set2.size() > Environment.WIDTHLOWERNO && set1.size() *  Environment.WIDTHUPPERFAKTOR < set2.size())
            return false;

        if(metric == MetricKind.LCS){
            ModifiedHammingDistance.RELATIV = relativ;
            ModifiedHammingDistance.THRESHOLD = threshold;
            float medium = 0.0F;

            /*if(Environment.MD5){
                for (List<Encoder> path : set1){
                    medium += LCS.findPathWithLCS(((HashEncoder)path.get(0)).getEncoding_low(),((HashEncoder)path.get(0)).getEncoding_high() , set2);
                    if(medium / set1.size() > threshold)
                        return false;
                }
            }else{*/
                float size = (float) set1.size();
                float scaling = config.getFloat("scaling");

                if (config.getBoolean("countExceptions")){
                    int excCount1 = set1.get(0).get(0).getEncoding()[0];
                    int excCount2 = set2.get(0).get(0).getEncoding()[0];

                    float result = (float) Math.abs(excCount1 - excCount2);

                    size += (scaling / (1 + result));
                }
                for (List<Encoder> path : set1){
                    medium += LCS.findPathWithLCS(path, set2);
                    if(medium / size > threshold){
                        return false;
                    }
                }
            //}
            return medium / set1.size() <= threshold;
        }

        if(metric == MetricKind.LEVENSHTEIN){
            ModifiedHammingDistance.RELATIV = relativ;
            ModifiedHammingDistance.THRESHOLD = threshold;
            float medium = 0.0F;

            /*if(Environment.MD5){
                for (List<Encoder> path : set1){
                    medium += LevenShtein.findPathWithLEVENSHTEIN(((HashEncoder)path.get(0)).getEncoding_low(),((HashEncoder)path.get(0)).getEncoding_high() , set2);
                    if(medium / set1.size() > threshold)
                        return false;
                }
            }else{*/
                for (List<Encoder> path : set1){
                    medium += LevenShtein.findPathWithLEVENSHTEIN(path.get(0).getEncoding(), set2);
                    if(medium / set1.size() > threshold)
                        return false;
                }
            //}
            if(medium / set1.size() <= threshold)
                return true;
            else
                return false;
        }
        Assertions.UNREACHABLE("Unknown metric.");
        return false;
    }

    @Override
    public int[] getEncoding() {
        return this.encoding;
    }

    @Override
    public int getNumberOfEncodings() {
        return this.encoding.length;
    }
}
