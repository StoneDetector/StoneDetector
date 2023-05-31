package org.fsu.bytecode;

import com.ibm.wala.util.debug.Assertions;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.fsu.codeclones.*;
import soot.Unit;
import soot.util.dot.DotGraph;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class HashEncoderByteCode extends Encoder<Unit> {

    private int[] encoding=null;
    public static FileBasedConfiguration config;

    public HashEncoderByteCode(){}


    public HashEncoderByteCode(int[] node)
    {
        this.encoding=node;
    }


    static public List<List<Encoder>> encodeDescriptionSetStatic(List<List<Unit>> it) {
        List<List<Encoder>> encodedPathes = new ArrayList<>();
        int longestPath=0;

        if (config.getBoolean("removeSimilarPaths") && it.size()<20) { it = checkForSimilarPaths(it); }
        for (List<Unit> path : it)
        {
            List<Encoder> encodedPath=new ArrayList<>();
	    //if (path.size() > Environment.MINNODESNO) {
            List<Unit> virtualNodes=new ArrayList<>();
                for (Unit unit :path)
                {
                    /*if (unit.toString().startsWith("$")) {
                        virtualNodes.add(unit);
                        continue;
                    }*/
                    Code[] codes=null;
                    List<String> names= new ArrayList<>();
                    codes= StandardEncoderByteCode.encodeUnit(unit,config,names);
                    List<Integer> encodedNode= new ArrayList<>();
                    if (codes!=null) {
                        /*if (virtualNodes.size()>0)
                        {
                            Code[] virtualCodes= StandardEncoderRegisterCode.encodeVirtualUnits(virtualNodes);
                            if (virtualCodes.length>0)
                                codes=Stream.concat(Arrays.stream(codes), Arrays.stream(virtualCodes))
                                        .toArray(size -> (Code[]) Array.newInstance(Code.class, size));
                            virtualNodes=new ArrayList<>();
                        }*/

                        if (!config.getBoolean("println") && names.contains("println")){
                            continue;
                        }

                        Arrays.stream(codes).forEach(i->encodedNode.add(i.name().hashCode()));
                        if (names.size()>0)
                        {
                            names.forEach(i->encodedNode.add(i.hashCode()));
                        }
                        encodedPath.add(new HashEncoderByteCode(encodedNode.stream().mapToInt(Integer::intValue).toArray()));
                    }
                }
            //}
            if (encodedPath.size()>longestPath)
                longestPath=encodedPath.size();
	    //if (encodedPath.size() > Environment.MINNODESNO)
            	encodedPathes.add(encodedPath);

        }
        List<List<Encoder>> encodedPathesNEW = new ArrayList<>();
        for (List<Encoder> path : encodedPathes)
        {
            if (path.size()<Environment.BREMOVESMALLPATHES *longestPath)
                continue;
            encodedPathesNEW.add(path);

        }


        return encodedPathesNEW;
    }
    @Override
    public List<List<Encoder>> encodeDescriptionSet(List<List<Unit>> it) {
        return HashEncoderByteCode.encodeDescriptionSetStatic(it);
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
                int element = 2;
                float size = (float) set1.size();

                float scaling = config.getFloat("scaling");
                if (config.getBoolean("countExceptions")){
                    element = 1;
                    size -= 1;
                }
                for (List<Encoder> path : set1){
                    if (element == 1){
                        // check exception counter
                        int excCount1 = path.get(0).getEncoding()[0];
                        int excCount2 = set2.get(0).get(0).getEncoding()[0];
                        float result = (float) Math.max(0, -scaling * Math.pow((Math.abs(excCount1 - excCount2)), 2)/9 + scaling);
                        // scale size higher with higher set size
                        size += (result) * (0.1 * size + 0.9);

                        element += 1;
                        continue;
                    }
                    medium += LCS.findPathWithLCS(path, set2);
                    if(medium / size > threshold){
                        return false;
                    }
                }
            //}
            if(medium / set1.size() <= threshold)
                return true;
            else
                return false;
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


        Assertions.UNREACHABLE("Wrong metric.");

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
}
