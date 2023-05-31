package org.fsu.codeclones;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.ibm.wala.util.debug.Assertions;
import fr.inria.controlflow.ControlFlowNode;
import fr.inria.controlflow.BranchKind;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.dlr.foobar.SpoonBigCloneBenchDriver;
import soot.util.dot.DotGraph;

public class HashEncoder extends Encoder<ControlFlowNode>{
	static int counter = 0;

	FileBasedConfiguration config = null;

    long [] encoding_low;
    long [] encoding_high;
    int [] encoding;
    
    int number;
    

    public HashEncoder() {}

	public HashEncoder(FileBasedConfiguration configuration){
		config = configuration;
	}

	public HashEncoder(int[] path) {
	this.encoding = path;
    }
    
   // contructor used in StringEncoding
    public HashEncoder(long[] low, long[] high) {
		this.encoding_low = low;
		this.encoding_high = high;
    }

    
  	public List<List<Encoder>> encodeDescriptionSet(List<List<ControlFlowNode>> it){
		if (config != null && config.getBoolean("encodeAsInRegistercode")){
			return encodeDescriptionSet_registercode(it);
		}
		else {
			return encodeDescriptionSet_sourcecode(it);
		}
    }

	public List<List<Encoder>> encodeDescriptionSet_registercode(List<List<ControlFlowNode>> it){
		List<List<Encoder>>  s  = new ArrayList<>();
		List<List<String[]>> encodedPathsStrings = new ArrayList<>();

		int longestPath = 0;

		// set the config boolean
		boolean println = true;
		boolean specialInvoke = true;
		boolean ifNodeSimplify = false;
		boolean absolutePathLength = true;
		boolean relativePathLength = false;
		boolean parameterNodes = false;
		if (config != null){
			if (!config.getBoolean("println")){ println = false; }
			if (!config.getBoolean("specialInvoke")) { specialInvoke = false; }
			if (config.getBoolean("ifNodeSimplify")) { ifNodeSimplify = true; }
			if (!config.getBoolean("absolutePathLength")) { absolutePathLength = false; }
			if (config.getBoolean("relativePathLength")) { relativePathLength = true; }
			if (config.getBoolean("removeSimilarPaths") && it.size()<20) { it = checkForSimilarPaths(it, config); }
			if (config.getBoolean("parameterNodes")) { parameterNodes = true; }
		}

		for (List<ControlFlowNode> m : it) {

			List<String[]> encodedPathString = new ArrayList<>();

			if (!absolutePathLength || m.size() > Environment.MINNODESNO){ //Modified-Unsorted Splitting 1 Modified-Unsorted Unsplitting 3, LCS Splitting > 1 LCS Unsplitting > 3 Levenshtein Spliiting > 1, Levenshtein Unsplitting > 2
				List<Encoder> encodedPath = new ArrayList<>();
				int size=0;
				for(ControlFlowNode k: m){
					if(k.getStatement()!=null || k.getKind() == BranchKind.TRY || k.getKind() == BranchKind.FINALLY ||
							k.getKind() == BranchKind.CATCH){
						size++;
					}
				}
				if(size != 0){
					if(Environment.MD5){
						long [] hashedPath_low = new long[size];
						long [] hashedPath_high = new long[size];
						int i=0;
						for(ControlFlowNode k: m){
							//System.out.println("Node: " + k);
							if(k.getStatement()!=null || k.getKind() == BranchKind.TRY||k.getKind() == BranchKind.FINALLY ||
									k.getKind() == BranchKind.CATCH){
								CompletePathEncoder tmp = new CompletePathEncoder(k);
								MessageDigest md=null;
								try{
									md = MessageDigest.getInstance("MD5");
								}
								catch(NoSuchAlgorithmException e){
									e.printStackTrace();
								}
								md.update(tmp.encodingAsString().getBytes());
								byte[] digest = md.digest();
								ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
								buffer.put(digest, 0, digest.length/2);
								buffer.flip();//need flip
								hashedPath_low[i] =   buffer.getLong();
								buffer = ByteBuffer.allocate(Long.BYTES);
								buffer.put(digest, digest.length/2, digest.length/2);
								buffer.flip();//need flip
								hashedPath_high[i]=buffer.getLong();
								i++;
							}
						}
						encodedPath.add(new HashEncoder(hashedPath_low, hashedPath_high));
					}else{
						ArrayList<Integer> hashedPath = new ArrayList<>();
						int nodeCounter= 0;
						for(ControlFlowNode k: m){
							if (nodeCounter == 1 && parameterNodes){
								nodeCounter = 2;
								ArrayList<Code> tmpParameter = CompletePathEncoder.encodeParameterNode(k);
								if (tmpParameter.size() == 1){
									for (Code c : tmpParameter){
										hashedPath.add((c.name().hashCode()));
									}
									encodedPathString.add(codeListToStringList(tmpParameter, new ArrayList<>()));
									continue;
								}
							}
							nodeCounter += 1;
							if(k.getStatement()!=null || k.getKind() == BranchKind.TRY||k.getKind() == BranchKind.FINALLY ||
									k.getKind() == BranchKind.CATCH){
								CompletePathEncoder tmp = new CompletePathEncoder(k, config);
								if (tmp.opKind.size() == 0){
									continue;
								}
								if (!println && tmp.functionNames.contains("println")){
									continue;
								}
								if (!specialInvoke && tmp.functionNames.contains("<init>")){
									continue;
								}
								if (ifNodeSimplify && tmp.opKind.get(0).equals(Code.COND)){
									ArrayList<Code> saveCodes = new ArrayList<>();
									ArrayList<Code> tempCodes = new ArrayList<>();
									int counter = 0;

									for (Code c : tmp.opKind){
										if (counter <= 1){
											counter += 1;
											if (counter == 2){
												if (c != Code.COMP && c != Code.EQ && c != Code.NE && c != Code.LE &&
														c != Code.LT && c != Code.GE && c != Code.GT){
													hashedPath.add(c.name().hashCode());
													tempCodes.add(c);
												}
												else {
													saveCodes.add(c);
												}
											}
											else {
												saveCodes.add(c);
											}
										}
										else {
											hashedPath.add(c.name().hashCode());
											tempCodes.add(c);
										}
									}
									for (String name : tmp.functionNames){
										hashedPath.add(name.hashCode());
									}
									encodedPathString.add(codeListToStringList(tempCodes, tmp.functionNames));
									encodedPath.add(new HashEncoder(hashedPath.stream().mapToInt(i -> i).toArray()));

									hashedPath = new ArrayList<>();

									tmp.functionNames = new ArrayList<>();
									tmp.opKind = saveCodes;
								}
								for (Code c : tmp.opKind){
									hashedPath.add((c.name().hashCode()));
								}
								for (String name : tmp.functionNames){
									hashedPath.add(name.hashCode());
								}
								encodedPathString.add(codeListToStringList(tmp.opKind, tmp.functionNames));
							}
							encodedPath.add(new HashEncoder(hashedPath.stream().mapToInt(i -> i).toArray()));
							hashedPath = new ArrayList<>();
						}
					}
					if (encodedPath.size() > longestPath) { longestPath = encodedPath.size(); }
					s.add(encodedPath);
				}
			}
			if (encodedPathString.size() > 0){
				encodedPathsStrings.add(encodedPathString);
			}
		}

		List<List<String[]>> encodedPathStringsNEW = new ArrayList<>();
		List<List<Encoder>>  new_s  = new ArrayList<>();

		if (relativePathLength){
			// compare relative Path lengths
			for (int i = 0; i < s.size(); i++){
				List<Encoder> path = s.get(i);
				if (path.size() < Environment.BREMOVESMALLPATHES * longestPath) { continue; }
				new_s.add(path);
				encodedPathStringsNEW.add(encodedPathsStrings.get(i));
			}
		}
		else {
			new_s = s;
			encodedPathStringsNEW = encodedPathsStrings;
		}

		if (config != null && config.getBoolean("createEncodedDotGraph") && !Environment.MD5){
			String resultDirectory = config.getString("graphResultDirectory");
			try {
				createDotCodeListGraphSeparate(encodedPathStringsNEW, "/home/hanno/CodeCloner/dominator4java/SPOON/" + resultDirectory + "/encodedGraph_" + HashEncoder.counter + ".dot");
				counter += 1;
				System.out.println("Creating Code[] Dot Graph Separate and writing it to " + resultDirectory + "...");
			}
			catch (Exception ignored){}
		}
		return new_s;
	}

	public List<List<Encoder>> encodeDescriptionSet_sourcecode(List<List<ControlFlowNode>> it){
		List<List<String>> encodedPathsStrings = new ArrayList<>();

		boolean println = true;
		boolean specialInvoke = true;
		boolean parameterNodes = false;
		if (config != null){
			if (!config.getBoolean("println")){ println = false; }
			if (!config.getBoolean("specialInvoke")) { specialInvoke = false; }
			if (config.getBoolean("removeSimilarPaths") && it.size()<20) { it = checkForSimilarPaths(it, config); }
			if (config.getBoolean("parameterNodes")) { parameterNodes = true; }
		}

		List<List<Encoder>>  s  = new ArrayList<>();
		for (List<ControlFlowNode> m : it) {
			if(m.size() > Environment.MINNODESNO){
				List<Encoder>  l  = new ArrayList<>();
				int size=0;
				for(ControlFlowNode k: m){
					if(k.getStatement()!=null || k.getKind() == BranchKind.TRY||k.getKind() == BranchKind.FINALLY)
						size++;
				}
				if(size != 0){
					if(Environment.MD5){
						long [] hashedPath_low = new long[size];
						long [] hashedPath_high = new long[size];
						int i=0;
						for(ControlFlowNode k: m){
							if(k.getStatement()!=null || k.getKind() == BranchKind.TRY||k.getKind() == BranchKind.FINALLY){
								CompletePathEncoder tmp = new CompletePathEncoder(k);
								MessageDigest md=null;
								try{
									md = MessageDigest.getInstance("MD5");
								}
								catch(NoSuchAlgorithmException e){
									e.printStackTrace();
								}
								md.update(tmp.encodingAsString().getBytes());
								byte[] digest = md.digest();
								ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
								buffer.put(digest, 0, digest.length/2);
								buffer.flip();//need flip
								hashedPath_low[i] =   buffer.getLong();
								buffer = ByteBuffer.allocate(Long.BYTES);
								buffer.put(digest, digest.length/2, digest.length/2);
								buffer.flip();//need flip
								hashedPath_high[i]=buffer.getLong();
								i++;
							}
						}
						l.add(new HashEncoder(hashedPath_low, hashedPath_high));
					}else{
						List<String> encodedPathString = new ArrayList<>();
						ArrayList<Integer> hashedPath = new ArrayList<>();
						int nodeCounter = 0;
						//int [] hashedPath = new int[size];
						for(ControlFlowNode k: m){
							if (nodeCounter == 1 && parameterNodes){
								nodeCounter += 1;
								ArrayList<Code> tmpParameter = CompletePathEncoder.encodeParameterNode(k);
								if (tmpParameter.size() == 1){
									for (Code c : tmpParameter){
										hashedPath.add((c.name().hashCode()));
									}
									String[] strList = codeListToStringList(tmpParameter, new ArrayList<>());
									Collections.addAll(encodedPathString, strList);
									continue;
								}
							}
							nodeCounter += 1;
							if(k.getStatement()!=null || k.getKind() == BranchKind.TRY||k.getKind() == BranchKind.FINALLY){
								CompletePathEncoder tmp = new CompletePathEncoder(k);
								if (tmp.opKind.size() == 0){
									continue;
								}
								if (!println && tmp.functionNames.contains("println")){
									continue;
								}
								if (!specialInvoke && tmp.functionNames.contains("<init>")){
									continue;
								}
								//hashedPath[i] = tmp.encodingAsString().hashCode();
								hashedPath.add(tmp.encodingAsString().hashCode());

								String[] strList = codeListToStringList(tmp.opKind, tmp.functionNames);
								Collections.addAll(encodedPathString, strList);
							}
						}
						l.add(new HashEncoder(hashedPath.stream().mapToInt(j -> j).toArray()));
						encodedPathsStrings.add(encodedPathString);
					}
					s.add(l);
				}
			}
		}
		return s;
	}

	public static List<List<ControlFlowNode>> checkForSimilarPaths(List<List<ControlFlowNode>> paths, FileBasedConfiguration config){
        /*
        add every path different from
        has to be sorted though (longest to shortest)
         */
		// sort paths
		List<List<ControlFlowNode>> sortedPaths = new ArrayList<>();
		for (int c=0;c<paths.size();c++){
			int maxLength = 0;
			List<ControlFlowNode> maxPath = new ArrayList<>();

			for (List<ControlFlowNode> path : paths){
				if (sortedPaths.contains(path)){ continue; }
				if (path.size() > maxLength){
					maxLength = path.size();
					maxPath = path;
				}
			}
			sortedPaths.add(maxPath);
		}
		List<List<ControlFlowNode>> newPaths = new ArrayList<>();
		boolean add;
		for (int c1 = 0; c1 < paths.size(); c1++){
			add = true;
			List<ControlFlowNode> path1 = sortedPaths.get(c1);
			for (int c2 = 0; c2 < newPaths.size(); c2++){
				if (c1 != c2){
					List<ControlFlowNode> path2 = newPaths.get(c2);
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

	public static int similarPaths(List<ControlFlowNode> path1, List<ControlFlowNode> path2){
        /*
        Test for 2 path2 if they are similar -> one can be ignored later (only insignificant difference)
         */
		// make sure path 1 is the shorter one
		if (path1.size() > path2.size()){
			List<ControlFlowNode> tmp = path1;
			path1 = path2;
			path2 = tmp;
		}
		ControlFlowNode start1;
		ControlFlowNode start2;
		for (int counter = 0; counter < path1.size(); counter++){
			start1 = path1.get(counter);
			start2 = path2.get(counter);
			if (start1 != start2){
				return path1.size() - counter;
			}
		}
		return 0;
	}

	private static String[] codeListToStringList(List<Code> codes, List<String> names){
		List<String> result = new ArrayList<>();
		for (Code c : codes){
			result.add(c.toString());
		}
		result.addAll(names);
		return result.toArray(new String[0]);
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

 public boolean areTwoDescriptionSetsSimilar(List<List<Encoder>> set1, List<List<Encoder>> set2,
					       MetricKind metric, boolean sorted, boolean relativ, float threshold){
     

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

	    if(Environment.MD5){
			for (List<Encoder> path : set1){
				medium += LCS.findPathWithLCS(((HashEncoder)path.get(0)).getEncoding_low(),((HashEncoder)path.get(0)).getEncoding_high() , set2);
				if(medium / set1.size() > threshold)
					return false;
			}
	    }else{
			if (SpoonBigCloneBenchDriver.encodeAsInRegistercode){
				for (List<Encoder> path : set1){
					medium += LCS.findPathWithLCS(path, set2);
					if(medium / set1.size() > threshold)
						return false;
				}
			}
			else {
				for (List<Encoder> path : set1){
					medium += LCS.findPathWithLCS(path.get(0).getEncoding(), set2);
					if(medium / set1.size() > threshold)
						return false;
				}
			}
	    }
		if(medium / set1.size() <= threshold)
           return true;
		return false;
       }
	    
       if(metric == MetricKind.LEVENSHTEIN){
	   ModifiedHammingDistance.RELATIV = relativ;
	   ModifiedHammingDistance.THRESHOLD = threshold;
	   float medium = 0.0F;

	   if(Environment.MD5){
	       for (List<Encoder> path : set1){
		   medium += LevenShtein.findPathWithLEVENSHTEIN(((HashEncoder)path.get(0)).getEncoding_low(),((HashEncoder)path.get(0)).getEncoding_high() , set2);
		   if(medium / set1.size() > threshold)
		       return false;
	       }
	   }else{
	       for (List<Encoder> path : set1){
		   medium += LevenShtein.findPathWithLEVENSHTEIN(path.get(0).getEncoding(), set2);
		   if(medium / set1.size() > threshold)
		       return false;
	       }
	   }
	 
	   if(medium / set1.size() <= threshold)
	       return true;
	   else
	       return false;
       }

   
	Assertions.UNREACHABLE("Wrong metric.");
	
	return false;
    }
    
    


    public boolean isPathInDescriptionSet(List<Encoder> path, List<List<Encoder>> set,
					    MetricKind metric, boolean relativ, float threshold){
	System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	return false;
    }
    

    public final long[] getEncoding_low(){
	return encoding_low;
    }

    public final long[] getEncoding_high(){
	return encoding_high;
    }

    public final int[] getEncoding(){
    	return encoding;
    }

    public int getNumberOfEncodings(){
	return number;
    }

}
