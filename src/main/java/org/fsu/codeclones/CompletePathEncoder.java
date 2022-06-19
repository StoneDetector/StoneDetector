package org.fsu.codeclones;


import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Arrays;
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

public class CompletePathEncoder extends Encoder{
    
    List<Code> opKind = new ArrayList<Code>();
    int [] encoding;
    int number = 0; // Number of code elements
    int hashNumber; // Todo: That should been an IntList
    int numberCond = 2; // set number of first begin node
    
    public final Code getKind(){
	
	return 	opKind.get(0);
    }
    
    public CompletePathEncoder(){
    }

    // contructor used in StringEncoding
    public CompletePathEncoder(Code kind, List<Code> opKind) {
        //this.kind = kind;
	this.opKind = opKind;
        number = opKind.size();
	setEncodingArray(opKind.size());
    }

    public CompletePathEncoder(ControlFlowNode n){
	this.encodeNode(n);
    }
    
    private void encodeNode(ControlFlowNode n){
	//System.out.println(n);
	BranchKind branchKind = n.getKind();
	CtElement e = n.getStatement();
	if(branchKind == BranchKind.BRANCH){
	    //kind = Code.COND;
	    opKind.add(Code.COND);
	    
	    if(e == null || e.getClass() == null){
		System.out.println(e);
		return;
	    }
	    if(ht.get(e.getClass().toString())==33){ // local var
		CtLocalVariable lvar = (CtLocalVariable)e;
		if(lvar.getDefaultExpression() != null){
		    opKind.add(Code.ASSIGN);
		    opKind.add(Code.VAR);
		    getOperators(lvar.getDefaultExpression(), opKind);
		}
		else
		    opKind.add(Code.VAR);
	    }
	    else
	    getOperators((CtExpression)e,opKind);
	    number += opKind.size();
	    //System.out.println(opKind);
	    setEncodingArray(opKind.size());
	    return;
	 }

	if(branchKind == BranchKind.TRY){
	    opKind.add(Code.TRY);
	    setEncodingArray(opKind.size());
	    number += opKind.size();
	    return;
	}

	if(branchKind == BranchKind.FINALLY){
	    opKind.add(Code.FINALLY);
	    setEncodingArray(opKind.size());
	    number += opKind.size();
	    return;
	}

	if(e == null || e.getClass() == null){
	    System.out.println(e);
	    return;
	}

	if(ht == null || ht.get(e.getClass().toString()) == null){
	    System.out.println("++++++++++++++++++++++++++++++++++++++++++" +  ht.get(e.getClass().toString()));
	    


	    System.out.println("++++++++++++++++++++++++++++++++++++++++++" +e);
	    System.out.println("++++++++++++++++++++++++++++++++++++++++++" +e.getClass());

	    return;
	}

	//System.out.println("No. " + e.getClass().toString() + " "+ e);
	switch(ht.get(e.getClass().toString())){

       	case 4: // assert statement
	    opKind.add(Code.ASSERT);
	    CtAssert ass = (CtAssert) e;
	    getOperators(ass.getAssertExpression(),opKind);
	    getOperators(ass.getExpression(),opKind);
	    break;
	    
	case 5: // assignment
	    opKind.add(Code.ASSIGN);
	    CtAssignment assign  = ((CtAssignment)e);
      	    getOperators(assign.getAssigned(),opKind);
	    getOperators(assign.getAssignment(),opKind);
	    break;

	case 6: // BinaryOperator
	    CtBinaryOperatorImpl binOp = ((CtBinaryOperatorImpl)e);
	    opKind.add(Code.BINARY); //brauchen wir das? WA
	    getOperators(binOp,opKind);
	    break;

	case 8: // break statement
	    opKind.add(Code.BREAK);
	    break;
	    
	case 11: // catch statement
	    opKind.add(Code.CATCH);
	    break;
	       
	case 17: // construktor call
	    opKind.add(Code.NEW);
	    CtConstructorCall cons  = (CtConstructorCall)e; //Todo: Hashcode WA
	    opKind.add(numberToCode(cons.getArguments().size()));
	    for(CtExpression m: (List<CtExpression>)(cons.getArguments()))
		getOperators(m, opKind);	    
	    break;
	    
	case 18: // continue statement
	    opKind.add(Code.CONTINUE);
	    break;
	    
	case 23: //field read access
	    getOperators((CtExpression)e, opKind);
	    break;
	    
	case  28: // method invocation
	    CtInvocation inv = ((CtInvocation)e);
	    getOperators(inv,opKind);	
	    break;
	    
	case  32: //literal
	    CtLiteral lit = ((CtLiteral)e);
	    opKind.add(Code.VAR); // Redo: WA
	    //getOperators(lit,opKind);
	    break;

	case 33: // local variable definition
            CtLocalVariable lvar = (CtLocalVariable)e;
	    if(lvar.getDefaultExpression() != null){
		opKind.add(Code.ASSIGN);
		//opKind.add(Code.VAR);
		getOperators(lvar.getDefaultExpression(), opKind);;
	    }
	    else
		opKind.add(Code.VAR);
	    break;

	case  37: //operator assign statement
	    opKind.add(Code.ASSIGN);
	    assign  = ((CtAssignment)e);
	    getOperators(assign.getAssigned(),opKind);
	    opKind.add(binaryOperatorToCode( ((CtOperatorAssignment)assign).getKind()));
	    getOperators(assign.getAssigned(),opKind);
	    getOperators(assign.getAssignment(),opKind);
	    break;
	    
	case  38: //return statement
	    opKind.add(Code.RETURN);
	    CtReturn ctReturn = (CtReturn)e;
	    if(ctReturn.getReturnedExpression() != null)
	       	getOperators(ctReturn.getReturnedExpression(), opKind);
	    	//opKind.add(Code.VAR); //Todo:
	    else
		opKind.add(Code.VOID);
	    break;

	case 47: // throw statement
	    opKind.add(Code.THROW);
	    break;
	    
	case 51: // UnaryOperator
	    CtUnaryOperator unOp = ((CtUnaryOperator)e);
	    opKind.add(Code.UNARY);
	    getOperators(unOp, opKind);	    
	    break;
	    
	case 53: // variable read
	    CtVariableRead varRead = ((CtVariableRead)e);
	    opKind.add(Code.EXPR); //Todo: Check thisn WA
	    getOperators(varRead, opKind);
	    break;
	case 57: // class definition ToDo
	    opKind.add(Code.CLASSDEFINITION);
	    break;
	    
	default:
	    opKind.add(Code.EXPR);
	    //System.out.println(e);
	    getOperators((CtExpression)e, opKind);
	}

	//System.out.println(opKind);
	setEncodingArray(opKind.size());
	number += opKind.size();
	
    }
    
    private void setEncodingArray(int length){
	encoding = new int[length];
	for(int i=0; i < encoding.length;i++){
	   if(opKind.get(i) == Code.HASHVALUE)
	       encoding[i] = hashNumber;
	   else
	       encoding[i]= opKind.get(i).ordinal();
	}
    }

     
    public List<List<Encoder>> encodeSortedDescriptionSet(List<List<ControlFlowNode>> it){
	List<List<Encoder>>  s  = new ArrayList<List<Encoder>>();
	int i=0;
	for (List<ControlFlowNode> m : it) {
	    int number = 0;
	    if(m.size() > 3 ){
		List<Encoder>  l  = new ArrayList<Encoder>();
		for(ControlFlowNode k: m){
		    if(k.getStatement()!=null){
			CompletePathEncoder tmp = new CompletePathEncoder(k);
			if(tmp.opKind.size() == 0)
			    System.out.println(k);
			if(l.size() == 0)
			    l.add(tmp);
			else{
			    int j=0;
			    for(Encoder t : l){
				
				if(tmp.getKind().ordinal() >= t.getKind().ordinal())
				    break;   
				j++;
			    }
			    l.add(j,tmp);
			}
			number += tmp.number;
		    } 
	
		    
		}
	    
		// System.out.println(i++ + ": " + l);
	    
		if(l.size() > 0){
		    ((CompletePathEncoder)l.get(0)).number = number; // Store number of code elements in the first encoding node 
		    s.add(l);
		}
	    }
	}
	//System.out.println("Vorher");
	//System.out.println(s);

	return s;
    }
    

    
    public List<List<Encoder>> encodeDescriptionSet(List<List<ControlFlowNode>> it){

	if(HammingDistance.SORTED)
	    return  encodeSortedDescriptionSet(it);
	
	List<List<Encoder>>  s  = new ArrayList<List<Encoder>>();
	
	int i=0;
	
	//System.out.println("  " + it  + " " + it.size() + "\n\n\n");

	for (List<ControlFlowNode> m : it) {
	    int number = 0;
	    int cond = 0;
	    if(m.size() > Environment.MINNODESNO){ //Modified-Unsorted Splitting 1 Modified-Unsorted Unsplitting 3, LCS Splitting > 1 LCS Unsplitting > 3 Levenshtein Spliiting > 1, Levenshtein Unsplitting > 2
		List<Encoder>  l  = new ArrayList<Encoder>();
		for(ControlFlowNode k: m){
		    // System.out.println(k);
		    if(k.getStatement()!=null || k.getKind() == BranchKind.TRY||k.getKind() == BranchKind.FINALLY){  
			CompletePathEncoder tmp = new CompletePathEncoder(k);
			l.add(tmp);
			
				if(k.getKind() == BranchKind.TRY||k.getKind() == BranchKind.FINALLY || k.getKind() == BranchKind.CATCH||
				  k.getKind() == BranchKind.BRANCH)
				    cond++;
			number += tmp.number;
		    } //else
		    //System.out.println(k);
	
		    
		}
     
		// System.out.println(i++ + ": " + l);
	    
		if(l.size() > 0){ 
		    //System.out.println(" Size " + m + " " + m.size());
		    //System.out.println(" Size " + l + " " + l.size());
		    ((CompletePathEncoder)l.get(0)).number = number; // Store number of code elements in the first encoding node
		    ((CompletePathEncoder)l.get(0)).numberCond = cond;
		    s.add(l);
		} 
		//System.out.println("  " +  m + " " + m.size() + "\n\n\n");
		//System.out.println("  " +  l + " " + l.size() + "\n\n\n");
		}
	}

    

	//System.out.println(" Gleich: " +areTwoDescriptionSetsSimilar(s, s, MetricKind.HAMMING, false, true, 0.1F);//);
	
    
	//System.out.println(HammingDistance.findPathWithModifiedHammingDistance(s.get(0),s));
	//System.out.println(HammingDistance.hammingDistanceForTwoPaths(s.get(0), s.get(1),true));
    
	// System.out.println(s);
	return s;
    }

    public String toString(){return opKind.toString();}

    private final Code binaryOperatorToCode(BinaryOperatorKind bkind){
	final int OFFSET = Code.OR.ordinal();
	return Code.values()[OFFSET + bkind.ordinal()];
    }

    private final Code unaryOperatorToCode(UnaryOperatorKind ukind){
	final int OFFSET = Code.POS.ordinal();
	return Code.values()[OFFSET + ukind.ordinal()];
    }

    private final Code numberToCode(int n){
	if(n >10)
	    n=10;
	final int OFFSET = Code.VOID.ordinal();
	return Code.values()[OFFSET + n];
    }

   private void getOperators(CtExpression value, List<Code> list){
	if(value == null)
	    return;

	if(value instanceof CtAssignment){
	    list.add(Code.ASSIGN);
	    CtAssignment assign  = ((CtAssignment) value);
	    getOperators(assign.getAssigned(),list);
	    getOperators(assign.getAssignment(),list);
	    return;
	}

	if(value instanceof CtConditional){
	    CtConditional cond  = ((CtConditional) value);
	    list.add(Code.COND); getOperators(cond.getCondition(),list);
	    getOperators(cond.getThenExpression(),list);
	    getOperators(cond.getElseExpression(),list);
	    return;
	}

	if (value instanceof CtNewArray) {
	    
	    list.add( Code.NEWARRAY);
	    // Redo this? WA
	    /*list.add(Code.HASHVALUE);
	    String clazz = ((CtNewArray)value).getType() + "" + ((CtNewArray)value).getElements();
	    //System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! " + clazz);
	    
	    Integer number = DominatorTree.methodTable.get(clazz);
	    if (number == null ){
	    	number = DominatorTree.hashCounter++;
	    	DominatorTree.methodTable.put(clazz, number);
	    } 
	    
	    hashNumber = number.intValue();
	    //System.out.println(hashNumber);*/
	    return;
	} else if (value instanceof CtLiteral) {
	    list.add(Code.VAR);
	    return;
	    /*if(((CtLiteral)value).getValue() instanceof String){
		list.add(Code.STRING);
		return;
	    }
	    if(((CtLiteral)value).getValue() instanceof Character){
		
		list.add(Code.CHAR);
		return;
	    }
	    if(((CtLiteral)value).getValue() instanceof Integer){
		list.add(Code.INT);
		return;
	    }
	    if(((CtLiteral)value).getValue() instanceof Long){
		list.add(Code.INT);
		return;
	    }
	    if(((CtLiteral)value).getValue() instanceof Float){
		list.add(Code.FLOAT);
		return;
	    }
	    if(((CtLiteral)value).getValue() instanceof Double){
		list.add(Code.FLOAT);
		return;
	    }
	    if(((CtLiteral)value).getValue() instanceof Boolean){
		list.add(Code.BOOLEAN);
		return;
	    }
	    else if(((CtLiteral)value).getValue() == null){
		list.add(Code.NULL);
		return;
		}*/
	}
	else if (value instanceof CtFieldRead) {
	    list.add(Code.FIELDREAD);
	     // Redo this? WA
	    /*list.add(Code.HASHVALUE);
	    String target = ((CtFieldAccess)value).getVariable().toString();
	    //System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! " + target);
	    
	    Integer number = DominatorTree.methodTable.get(target);
	    if (number == null ){
	    	number = DominatorTree.hashCounter++;
	    	DominatorTree.methodTable.put(target, number);
	    } 
	    
	    hashNumber = number.intValue();
	    //System.out.println(hashNumber);*/
	    return;
        } else if (value instanceof CtFieldWrite) {
	    list.add(Code.FIELDWRITE);
	     // Redo this? WA
	    /*list.add(Code.HASHVALUE);
	    String target = ((CtFieldAccess)value).getVariable().toString();
	    //System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! " + target);
	    
	    Integer number = DominatorTree.methodTable.get(target);
	    if (number == null ){
	    	number = DominatorTree.hashCounter++;
	    	DominatorTree.methodTable.put(target, number);
	    } 
	    
	    hashNumber = number.intValue();
	    //System.out.println(hashNumber);
	    //System.out.println(value);*/

	    return;
	} else if (value instanceof CtVariableRead || value instanceof CtVariableWrite) {
	    list.add(Code.VAR);
	    return;
	} else if (value instanceof CtInvocation) {
	    CtInvocation inv= (CtInvocation)value;
	    list.add(Code.CALL);
	    if(Environment.SUPPORTCALLNAMES){
		list.add(Code.HASHVALUE);
		String method = inv.getExecutable().toString().split("\\(")[0];
		//System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! " + method);
	    
		Integer number = DominatorTree.methodTable.get(method);
		if (number == null ){
		    number = DominatorTree.hashCounter++;
		    DominatorTree.methodTable.put(method, number);
		} 
		
		hashNumber = number.intValue();
  	    }
	    list.add(numberToCode(inv.getArguments().size()));
	    for(CtExpression m: (List<CtExpression>)inv.getArguments())
	    	getOperators(m, list);
	    return;
	} else if (value instanceof CtConstructorCall) {
	    list.add(Code.NEW);
	    list.add(numberToCode(((CtConstructorCall)value).getArguments().size()));
	    
	    return;
	    
	}else if (value instanceof CtArrayRead) {
	    list.add(Code.ARRAYREAD); //Redo this. WA
	    //getOperators(((CtArrayRead)value).getIndexExpression(), list);
	    return;
	}else if (value instanceof CtArrayWrite) {
	    list.add(Code.ARRAYWRITE); 
	    //getOperators(((CtArrayWrite)value).getIndexExpression(), list);
	    return;
	}else if (value instanceof CtThisAccess) {
	    
	    //list.add(Code.THIS); 
	    return;
	}else if (value instanceof CtTypeAccess) {
	    list.add(Code.TYPE); 
	    return;
	}
	else if (value instanceof CtBinaryOperator) {
	    list.add(binaryOperatorToCode(((CtBinaryOperator)value).getKind()));
	    getOperators(((CtBinaryOperator)value).getLeftHandOperand(),list);
	    getOperators(((CtBinaryOperator)value).getRightHandOperand(),list);
	    return;
	} else if (value instanceof CtUnaryOperator) {
	    list.add(unaryOperatorToCode(((CtUnaryOperator)value).getKind()));
	    getOperators(((CtUnaryOperator)value).getOperand(),list);
	    return;
	}
       
	Assertions.UNREACHABLE("Cannot find operator");
	//System.out.println("Not knowm");
      
    }
    
    public boolean isPathInDescriptionSet(List<Encoder> path, List<List<Encoder>> set,
				   MetricKind metric, boolean relativ, float threshold){
	HammingDistance.RELATIV = relativ;
	HammingDistance.THRESHOLD = threshold;
	if(metric == MetricKind.HAMMING)
	    return  HammingDistance.findPathWithHammingDistance(path, set);
    
	if(metric == MetricKind.HAMMINGMODIFIED)
	    return ModifiedHammingDistance.findPathWithModifiedHammingDistance(path, set) <= threshold;
	Assertions.UNREACHABLE("Wrong metric.");
	return false;
    }
    
   public boolean areTwoDescriptionSetsSimilar(List<List<Encoder>> set1, List<List<Encoder>> set2,
					       MetricKind metric, boolean sorted, boolean relativ, float threshold){

       HammingDistance.RELATIV = relativ;
       HammingDistance.THRESHOLD = threshold;
       HammingDistance.SORTED = sorted;

       if(set1.size()==0 || set2.size()==0){
	   //System.out.println("LLLLLLLLLLLLLLLLLLLEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEERRRRRRRRRRRRRRRRRRRR");
 	    return false;
       }
       //System.out.println("BBBBBBBBBBBBBBBBBBBBBBBB");
       //System.out.println(set1);
       //System.out.println("GGGGGGGGGGGGGGGGGGGG");
       //System.out.println(set2);

       
       if(set1.size() > set2.size()){ // it will check if the smaller set is equivalent to the larger set
	   List<List<Encoder>> tmp = set1;
	   set1 = set2;
	   set2 = tmp;
       }
       // System.out.println( " Vorher " + set1.size() + " " + set2.size());
       if(set2.size() > Environment.WIDTHLOWERNO && set1.size() *  Environment.WIDTHUPPERFAKTOR < set2.size()) 
       return false;

       //System.out.println( " Nachher " + set1.size() + " " + set2.size());

       //Modified Hamming Unsorted Spliitting  if(set2.size() > 5  && set1.size() * 1.5 < set2.size())

       //Modified Hamming Unsorted UnSpliitting  if(set2.size() > 5  && set1.size() * 1.5 < set2.size())

       //Levenshtein Splitting if(set2.size() >= 6 && set1.size() * 1.5 < set2.size()) 

       //Levenshtein Unsplitting if(set2.size() > 5 && set1.size() * 1.5 < set2.size())

       //LCS Unsplitting if(set2.size() > 5 && set1.size() * 1.5 < set2.size())

       //LCS Splitting if(set2.size() >= 6 && set1.size() * 1.5 < set2.size())


       if(metric == MetricKind.LCS){
	    ModifiedHammingDistance.RELATIV = relativ;
	    ModifiedHammingDistance.THRESHOLD = threshold;
	    float medium = 0.0F;
	    
	    for (List<Encoder> path : set1){
		medium += LCS.findPathWithLCS(path, set2);
		if(medium / set1.size() > threshold)
		    return false;
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
	    
	    for (List<Encoder> path : set1){
		medium += LevenShtein.findPathWithLEVENSHTEIN(path, set2); // Herausnehmen
	    	if(medium / set1.size() > threshold)
		    return false;
	    }
	    
	    if(medium / set1.size() <= threshold)
	        return true;
	    else
		return false;
       }
	  
	if(metric == MetricKind.HAMMING){
	    
	    for (List<Encoder> path : set1)    
		if (HammingDistance.findPathWithHammingDistance(path, set2) == false){
		    return false;
		}
	    return true;
	}
	
	if(metric == MetricKind.HAMMINGMODIFIED){
	    ModifiedHammingDistance.RELATIV = relativ;
	    ModifiedHammingDistance.THRESHOLD = threshold;
	    ModifiedHammingDistance.SORTED = sorted;

	    float medium = 0.0F;

	    for (List<Encoder> path : set1){
		medium += ModifiedHammingDistance.findPathWithModifiedHammingDistance(path, set2);
		if(medium / set1.size() > threshold)
		   return false;
	    }
	    
	    if(medium / set1.size() <= threshold)
	        return true;
	    
	}
	
	if (metric == MetricKind.NW) {
	    return NW.compareSets(set1, set2);
	}
   
	Assertions.UNREACHABLE("Wrong metric.");
	
	return false;
    }
    
    

    String asString(){
	return Arrays.toString(encoding);
    }
    public final int[] getEncoding(){
	return encoding;
    }

    public int getNumberOfEncodings(){
	return number;
    }

}
