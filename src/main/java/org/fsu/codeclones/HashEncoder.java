package org.fsu.codeclones;


import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.Assertions;
import fr.inria.controlflow.ControlFlowNode;
import fr.inria.controlflow.BranchKind;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtOperatorAssignment;
import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtNewClass;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtFieldAccess;

import spoon.reflect.code.CtArrayRead;
import spoon.reflect.code.CtArrayWrite;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.CtVariableWrite;
import spoon.reflect.code.CtThisAccess;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtAssert;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLiteral;
import spoon.support.reflect.code.CtBinaryOperatorImpl;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.UnaryOperatorKind;

public class HashEncoder extends Encoder{

    
    long [] encoding_low;
    long [] encoding_high;
    int [] encoding;
    
    int number;
    

    public HashEncoder() {        
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

	List<List<Encoder>>  s  = new ArrayList<List<Encoder>>();
	
	//System.out.println("  " + it  + " " + it.size() + "\n\n\n");

	for (List<ControlFlowNode> m : it) {

	    if(m.size() > Environment.MINNODESNO){ //Modified-Unsorted Splitting 1 Modified-Unsorted Unsplitting 3, LCS Splitting > 1 LCS Unsplitting > 3 Levenshtein Spliiting > 1, Levenshtein Unsplitting > 2
		List<Encoder>  l  = new ArrayList<Encoder>();
		int size=0;
		for(ControlFlowNode k: m){
		    // System.out.println(k);
		    if(k.getStatement()!=null || k.getKind() == BranchKind.TRY||k.getKind() == BranchKind.FINALLY)
			size++;
		}
		
		if(size != 0){
		    if(Environment.MD5){
			long [] hashedPath_low = new long[size];
			long [] hashedPath_high = new long[size];
			int i=0;
			for(ControlFlowNode k: m){
			    // System.out.println(k);
			    if(k.getStatement()!=null || k.getKind() == BranchKind.TRY||k.getKind() == BranchKind.FINALLY){  
				CompletePathEncoder tmp = new CompletePathEncoder(k);
				MessageDigest md=null; 
				try{
				    md = MessageDigest.getInstance("MD5");
				}
				catch(NoSuchAlgorithmException e){
				    e.printStackTrace();
				}
				md.update(tmp.asString().getBytes());
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
			int [] hashedPath = new int[size];
			int i=0;
			for(ControlFlowNode k: m){
			    // System.out.println(k);
			    if(k.getStatement()!=null || k.getKind() == BranchKind.TRY||k.getKind() == BranchKind.FINALLY){  
				CompletePathEncoder tmp = new CompletePathEncoder(k);
				hashedPath[i] = tmp.asString().hashCode();
				i++;
			    }
			    
			}
		    
			l.add(new HashEncoder(hashedPath));
		    }
		    
		    s.add(l);
		}
	    }
	}
	//System.out.println(it);
        //System.out.println(s);
	return s;
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
		for (List<Encoder> path : set1){
		    medium += LCS.findPathWithLCS(path.get(0).getEncoding(), set2);
		    if(medium / set1.size() > threshold)
			return false;
		}
	    }
			   if(medium / set1.size() <= threshold)
	       return true;
	   else
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

	 if (metric == MetricKind.NW) {
		 return NW.compareSets(set1, set2);
	 }

   
	Assertions.UNREACHABLE("Wrong metric.");
	
	return false;
    }
    
    


    boolean isPathInDescriptionSet(List<Encoder> path, List<List<Encoder>> set,
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
