package org.fsu.codeclones;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
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

public class AbstractEncoder extends Encoder{
    Code kind;
    int number = 0; // Number of code elements
    int [] abstractEncoding;
    
    

    public AbstractEncoder(){
      
    }
    AbstractEncoder(int a[]){
	this.abstractEncoding = a;
	this.number = numberOfCodingeElements(a);
    }
    
    private void encodeNode(ControlFlowNode n, int[] a){
	BranchKind branchKind = n.getKind();
	CtElement e = n.getStatement();
	
	if(branchKind == BranchKind.BRANCH){
	    a[AbstractCode.COND.ordinal()]++;
	
	    if(e == null || e.getClass() == null){
		System.out.println(e);
		return;
	    }
	    
	    if(ht.get(e.getClass().toString())==33){ // local var
		CtLocalVariable lvar = (CtLocalVariable)e;
		if(lvar.getDefaultExpression() != null){
		    a[AbstractCode.ASSIGN.ordinal()]++;
		     a[AbstractCode.VAR.ordinal()]++;
		    encodeOperators(lvar.getDefaultExpression(), a);
		}
		else
		  a[AbstractCode.VAR.ordinal()]++;
	    } else
		encodeOperators((CtExpression)e, a);
	 
	    return;
	}

	if(branchKind == BranchKind.TRY){
	    a[AbstractCode.TRY.ordinal()]++; 
	    return;
	}

	if(branchKind == BranchKind.FINALLY){
	    a[AbstractCode.FINALLY.ordinal()]++; 
	    return;
	}

	if(e == null || e.getClass() == null){
	    Assertions.UNREACHABLE(e);
	    return;
	}
	
	switch(ht.get(e.getClass().toString())){

	case 4: // assert statement
	    a[AbstractCode.ASSERT.ordinal()]++;
	    CtAssert ass = (CtAssert) e;
	    encodeOperators(ass.getAssertExpression(),a);
	    encodeOperators(ass.getExpression(),a);
	    break;
	    
	case 5: // assignment
	    a[AbstractCode.ASSIGN.ordinal()]++;
	    CtAssignment assign  = ((CtAssignment)e);
      	    encodeOperators(assign.getAssigned(), a);
	    encodeOperators(assign.getAssignment(), a);
	    break;

	case 6: // BinaryOperator
	    CtBinaryOperatorImpl binOp = ((CtBinaryOperatorImpl)e);
	    encodeOperators(binOp, a);
	    break;

	case 8: // break statement
	    a[AbstractCode.BREAK.ordinal()]++;
	    break;

	case 11: // catch statement
	    a[AbstractCode.CATCH.ordinal()]++;
	    break;

	    
	           
	case 17: // construktor call
	    a[AbstractCode.NEW.ordinal()]++;
	    CtConstructorCall cons  = (CtConstructorCall)e;
	    for(CtExpression m: (List<CtExpression>)(cons.getArguments()))
		encodeOperators(m, a);	    
	    break;
	    
	case 18: // continue statement
	    a[AbstractCode.CONTINUE.ordinal()]++;
	    break;

	case 23: //field read access
	    encodeOperators((CtExpression)e, a);
	    break;
	    
	case  28: // method invocation
	    CtInvocation inv = ((CtInvocation)e);
	    encodeOperators(inv,a);	
	    break;
	    
	case  32: //literal
	    CtLiteral lit = ((CtLiteral)e);
	    encodeOperators(lit,a);
	    break;
	case 33: // local variable definition
            CtLocalVariable lvar = (CtLocalVariable)e;
	    if(lvar.getDefaultExpression() != null){
		a[AbstractCode.ASSIGN.ordinal()]++;
			a[AbstractCode.VAR.ordinal()]++;
		encodeOperators(lvar.getDefaultExpression(), a);
	    }
	    else
	    	a[AbstractCode.VAR.ordinal()]++;
	    break;

	case  37: //operator assign statement
	    a[AbstractCode.ASSIGN.ordinal()]++;
	    assign  = ((CtAssignment)e);
	    encodeOperators(assign.getAssigned(), a);
	    binaryOperatorToAbstractCode(((CtOperatorAssignment)assign).getKind(),a);
	    encodeOperators(assign.getAssigned(), a);
	    encodeOperators(assign.getAssignment(), a);
	    break;
	    
	case  38: //return statement
	    a[AbstractCode.RETURN.ordinal()]++;
	    CtReturn ctReturn = (CtReturn)e;
	    if(ctReturn.getReturnedExpression() != null)
		encodeOperators(ctReturn.getReturnedExpression(), a);
	    break;

	case 47: // throw statement
	    a[AbstractCode.THROW.ordinal()]++;
	    break;

	case 51: // UnaryOperator
	    CtUnaryOperator unOp = ((CtUnaryOperator)e);
	    encodeOperators(unOp, a);	    
	    break;
	    
	case 53: // variable read
	    CtVariableRead varRead = ((CtVariableRead)e);
	    encodeOperators(varRead, a);
	    break;
	    
       case 57: // class definition 
	    break;
	    
	   
	default:
	    encodeOperators((CtExpression)e, a);
	}
    }


    int numberOfCodingeElements(int[] a){
	int sum = 0;
	for(int i = 0; i<a.length;i++)
	    sum += a[i];
	return sum;
    }
   
    public List<List<Encoder>> encodeDescriptionSet(List<List<ControlFlowNode>> it){
  	List<List<Encoder>>  s  = new ArrayList<List<Encoder>>();
	int a[];	
	int i = 0;
	//System.out.println(i + ": !!!!!!!!!!!!!!!!!" );


	for (List<ControlFlowNode> m : it) {
	    if(m.size() > 3){
	
		List<Encoder>  l  = new ArrayList<Encoder>();
		
		a = new int[33];
		Arrays.fill( a, 0);
		for(ControlFlowNode k: m){
		    if(k.getStatement()!=null){
			encodeNode(k,a);
		    }
		}
		l.add(new AbstractEncoder(a));

		//System.out.println(i + ": " + m);
		//System.out.println(i++ + ": " + l.get(0));
		s.add(l);
		//System.out.println("Path " + EuclideanDistance.findPathWithEuclideanDistance(l,s));
	   
		}
	}

	//System.out.println(" Gleich: " + areTwoDescriptionSetsSimilar(s, s, MetricKind.EUCLIDEAN, true, 0F));

	//System.out.println("1 " + ": " + s.get(0));
	//System.out.println("s " + ": " + s);

	//System.out.println(EuclideanDistance.findPathWithEuclideanDistance(s.get(0), s));

	//System.out.println(HammingDistance.hammingDistanceForTwoPaths(s.get(0), s.get(1),true));
	    
	//System.out.println(s);
	return s;
    }



    private void binaryOperatorToAbstractCode(BinaryOperatorKind bkind, int [] a){
	switch(bkind){
	case OR:
	    a[AbstractCode.OR.ordinal()]++;
	    break;
	case AND:
	    a[AbstractCode.AND.ordinal()]++;
	    break;
	case BITOR:
	case BITXOR:
	case BITAND:
	    a[AbstractCode.BIT.ordinal()]++;
	    break;
	case EQ:
	case NE:
	    a[AbstractCode.EQNE.ordinal()]++;
	    break;
	case LT:
	case GT:
	case LE:
	case GE:
	    a[AbstractCode.GLTE.ordinal()]++;
	    break;
	case SL:
 	case SR:
	case USR:
	    a[AbstractCode.SHIFT.ordinal()]++;
	    break;
	case PLUS:
	    a[AbstractCode.PLUS.ordinal()]++;
	    break;
	case MINUS:
	    a[AbstractCode.MINUS.ordinal()]++;
	    break;
	case DIV:
	    a[AbstractCode.DIV.ordinal()]++;
	    break;
	case MUL:
	    a[AbstractCode.MUL.ordinal()]++;
	    break;
	case INSTANCEOF:
	    a[AbstractCode.INSTANCEOF.ordinal()]++;
	    break;
	case MOD:
	    a[AbstractCode.MOD.ordinal()]++;
	    break;
	default:
	    Assertions.UNREACHABLE(" Binary Operator " + bkind );
	}
    }

	
   private void unaryOperatorToCode(UnaryOperatorKind ukind, int [] a){
	switch(ukind){	
	case NOT:
	    a[AbstractCode.NOT.ordinal()]++;
	    break;
	case COMPL:
	    a[AbstractCode.BIT.ordinal()]++;
	    break;
	case PREINC:  
	case POSTINC:  
      	    a[AbstractCode.PLUS.ordinal()]++;
	    a[AbstractCode.ASSIGN.ordinal()]++; 
	    break;
	case PREDEC:  
	case POSTDEC:  
      	    a[AbstractCode.MINUS.ordinal()]++; 
	    a[AbstractCode.ASSIGN.ordinal()]++; 
	    break;
	case NEG:
	    a[AbstractCode.MINUS.ordinal()]++; 
	    break;
	case POS:
	    a[AbstractCode.PLUS.ordinal()]++; 
	    break;
	default:
	    Assertions.UNREACHABLE(" Unary Operator " + ukind );
	}
    }


    private void encodeOperators(CtExpression value, int a[]){
	if(value == null)
	    return;
	if(value instanceof CtAssignment){
	    a[AbstractCode.ASSIGN.ordinal()]++;
	    CtAssignment assign  = ((CtAssignment) value);
	    encodeOperators(assign.getAssigned(), a);
	    encodeOperators(assign.getAssignment(), a);
	    return;
	}
	if(value instanceof CtConditional){
	    CtConditional cond  = ((CtConditional) value);
	     a[AbstractCode.COND.ordinal()]++;
	    encodeOperators(cond.getCondition(),a);
	    encodeOperators(cond.getThenExpression(),a);
	    encodeOperators(cond.getElseExpression(),a);
	    return;
	}
       	
	if (value instanceof CtNewArray) {
	    a[AbstractCode.NEWARRAY.ordinal()]++;
	    return;
	}

	if (value instanceof CtLiteral) {
	    a[AbstractCode.VAR.ordinal()]++;
	    return;
	    /*
	    Object v = ((CtLiteral)value).getValue();
	    if(v instanceof String || v instanceof Character){
		a[AbstractCode.CHAR.ordinal()]++;
		return;
	    }
	    if(v instanceof Boolean){
		a[AbstractCode.BOOLEAN.ordinal()]++;
		return;
	    }
	    else{
		a[AbstractCode.NUMBER.ordinal()]++;  
		return;
		}*/
	    
	}
	else if (value instanceof CtFieldRead) {
	    a[AbstractCode.FIELDREAD.ordinal()]++;
	    return;
        } else if (value instanceof CtFieldWrite) {
	    a[AbstractCode.FIELDWRITE.ordinal()]++;
	    return;
	} else if (value instanceof CtVariableRead || value instanceof CtVariableWrite) {
	    a[AbstractCode.VAR.ordinal()]++;
	    return;
	} else if (value instanceof CtInvocation) {
	    CtInvocation inv= (CtInvocation)value;
	    if(inv.toString().startsWith("new"))
		a[AbstractCode.NEW.ordinal()]++;
	    else{
		a[AbstractCode.CALL.ordinal()]++;
	    }
	    for(CtExpression m: (List<CtExpression>)inv.getArguments())
		    encodeOperators(m, a);
	    return;
	} else if (value instanceof CtConstructorCall) {
	    a[AbstractCode.NEW.ordinal()]++;
	    return;
	    
	}else if (value instanceof CtArrayRead) {
	    a[AbstractCode.ARRAYREAD.ordinal()]++; 
	    return;
	}else if (value instanceof CtArrayWrite) {
	    a[AbstractCode.ARRAYWRITE.ordinal()]++; 
	    return;
	}else if (value instanceof CtTypeAccess) {
	    a[AbstractCode.TYPEACCESS.ordinal()]++;
	    return;
	}else if (value instanceof CtBinaryOperator) {
	    binaryOperatorToAbstractCode(((CtBinaryOperator)value).getKind(),a);
	    encodeOperators(((CtBinaryOperator)value).getLeftHandOperand(), a);
	    encodeOperators(((CtBinaryOperator)value).getRightHandOperand(), a);
	    return;
	} else if (value instanceof CtUnaryOperator) {
	    unaryOperatorToCode(((CtUnaryOperator)value).getKind(), a);
	    encodeOperators(((CtUnaryOperator)value).getOperand(), a);
	    return;
	}else if (value instanceof CtThisAccess) {
	    //System.out.println("A " + AbstractCode.THIS.ordinal());
	     a[AbstractCode.THIS.ordinal()]++; 
	    return;
	}
	Assertions.UNREACHABLE(" Operator " + value );
	    
    }
    
    public String toString(){
	String s="";
	for(int i=0;i< abstractEncoding.length;i++)
	    s += AbstractCode.values()[i] + " " + abstractEncoding[i] + "\n";
	return s;
    }

      public boolean isPathInDescriptionSet(List<Encoder> path, List<List<Encoder>> set,
				   MetricKind metric, boolean relativ, float threshold){
	  if(metric != MetricKind.EUCLIDEAN)
	      Assertions.UNREACHABLE("Wrong metric"); 
	  EuclideanDistance.THRESHOLD = threshold;
	  return EuclideanDistance.findPathWithEuclideanDistance(path,set) <= threshold;
	
    }

     public boolean areTwoDescriptionSetsSimilar(List<List<Encoder>> set1, List<List<Encoder>> set2,
						 MetricKind metric, boolean sorted, boolean relativ, float threshold){
	 //System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +threshold);
	 EuclideanDistance.THRESHOLD = threshold;

	 if(set1.size()==0 || set2.size()==0){
	     //System.out.println("LLLLLLLLLLLLLLLLLLLEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEERRRRRRRRRRRRRRRRRRRR");
	    return false;
	 }

	 if(set1.size() > set2.size()){ // it will check if the smaller set is equivalent to the larger set
	   List<List<Encoder>> tmp = set1;
	   set1 = set2;
	   set2 = tmp;
	 }

	 //if(set2.size() > 5 && set1.size() * 1.5 < set2.size()) 
	 //return false;
      
	 if(metric == MetricKind.EUCLIDEAN){
	     double medium = 0.0F;
	     double tmp;
	     for (List<Encoder> path : set1){
		 tmp = EuclideanDistance.findPathWithEuclideanDistance(path, set2);
		 //System.out.println(" Euklische distanz " + tmp);
		 medium += tmp; // EuclideanDistance.findPathWithEuclideanDistance(path, set2);
		 if(medium / set1.size() > threshold)
		     return false;
	     }
	     
	     //System.out.println("Medium ;" + medium / set1.size()); 
	     if(medium / set1.size() <= threshold)
		 return true;
	     else
		 return false;
      
	 }
	 
	 return true;
    }
    

    public int [] getEncoding(){
	return abstractEncoding;
    }
    
    public int getNumberOfEncodings(){
	return number;
    }

    public int getNumber(){
	return number;
    }
}
