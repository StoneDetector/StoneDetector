package org.fsu.codeclones;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.Assertions;
import fr.inria.controlflow.ControlFlowNode;

public abstract class Encoder<T>{

    public Code getKind(){
	return 	Code.UNKNOWN;
    }
    
    static HashMap<String,Integer> ht = new HashMap<String, Integer>();

    
    public abstract List<List<Encoder>> encodeDescriptionSet(List<List<T>> it);
    public abstract boolean isPathInDescriptionSet(List<Encoder> path, List<List<Encoder>> set,
					    MetricKind metric, boolean relativ, float threshold);

    public abstract boolean areTwoDescriptionSetsSimilar(List<List<Encoder>> set1, List<List<Encoder>> set2,
						  MetricKind metric, boolean sorted, boolean relativ, float threshold);
    
    public abstract int[] getEncoding();   // returns the encoding of a node => when using hamming metrics
                                    //             encoding of a path => when using euclidean metrics
    
    public abstract int getNumberOfEncodings(); // returns the number code elements from root to the node, that invokes the method
    
    static{
	ht.put("class spoon.support.reflect.code.CtArrayAccessImpl",1);
	ht.put("class spoon.support.reflect.code.CtArrayReadImpl",2);
	ht.put("class spoon.support.reflect.code.CtArrayWriteImpl",3);
	ht.put("class spoon.support.reflect.code.CtAssertImpl",4);
	ht.put("class spoon.support.reflect.code.CtAssignmentImpl",5);
	ht.put("class spoon.support.reflect.code.CtBinaryOperatorImpl",6);
	ht.put("class spoon.support.reflect.code.CtBlockImpl",7);
	ht.put("class spoon.support.reflect.code.CtBreakImpl",8);
	ht.put("class spoon.support.reflect.code.CtCaseImpl",9);
	ht.put("class spoon.support.reflect.code.CtCatchImpl",10);
	ht.put("class spoon.support.reflect.code.CtCatchVariableImpl",11);
	ht.put("class spoon.support.reflect.code.CtCodeElementImpl",12);
	ht.put("class spoon.support.reflect.code.CtCodeSnippetExpressionImpl",13);
	ht.put("class spoon.support.reflect.code.CtCodeSnippetStatementImpl",14);
	ht.put("class spoon.support.reflect.code.CtCommentImpl",15);
	ht.put("class spoon.support.reflect.code.CtConditionalImpl",16);
	ht.put("class spoon.support.reflect.code.CtConstructorCallImpl",17);
	ht.put("class spoon.support.reflect.code.CtContinueImpl",18);
	ht.put("class spoon.support.reflect.code.CtDoImpl",19);
	ht.put("class spoon.support.reflect.code.CtExecutableReferenceExpressionImpl",20);
	ht.put("class spoon.support.reflect.code.CtExpressionImpl",21);
	ht.put("class spoon.support.reflect.code.CtFieldAccessImpl",22);
	ht.put("class spoon.support.reflect.code.CtFieldReadImpl",23);
	ht.put("class spoon.support.reflect.code.CtFieldWriteImpl",24);
	ht.put("class spoon.support.reflect.code.CtForEachImpl",25);
	ht.put("class spoon.support.reflect.code.CtForImpl",26);
	ht.put("class spoon.support.reflect.code.CtIfImpl",27);
	ht.put("class spoon.support.reflect.code.CtInvocationImpl",28);
	ht.put("class spoon.support.reflect.code.CtJavaDocImpl",29);
	ht.put("class spoon.support.reflect.code.CtJavaDocTagImpl",30);
	ht.put("class spoon.support.reflect.code.CtLambdaImpl",31);
	ht.put("class spoon.support.reflect.code.CtLiteralImpl",32);
	ht.put("class spoon.support.reflect.code.CtLocalVariableImpl",33);
	ht.put("class spoon.support.reflect.code.CtLoopImpl",34);
	ht.put("class spoon.support.reflect.code.CtNewArrayImpl",35);
	ht.put("class spoon.support.reflect.code.CtNewClassImpl",36);
	ht.put("class spoon.support.reflect.code.CtOperatorAssignmentImpl",37);
	ht.put("class spoon.support.reflect.code.CtReturnImpl",38);
	ht.put("class spoon.support.reflect.code.CtStatementImpl",39);
	ht.put("class spoon.support.reflect.code.CtStatementListImpl",40);
	ht.put("class spoon.support.reflect.code.CtSuperAccessImpl",41);
	ht.put("class spoon.support.reflect.code.CtSwitchExpressionImpl",42);
	ht.put("class spoon.support.reflect.code.CtSwitchImpl",43);
	ht.put("class spoon.support.reflect.code.CtSynchronizedImpl",44);
	ht.put("class spoon.support.reflect.code.CtTargetedExpressionImpl",45);
	ht.put("class spoon.support.reflect.code.CtThisAccessImpl",46);
	ht.put("class spoon.support.reflect.code.CtThrowImpl",47);
	ht.put("class spoon.support.reflect.code.CtTryImpl",48);
	ht.put("class spoon.support.reflect.code.CtTryWithResourceImpl",49);
	ht.put("class spoon.support.reflect.code.CtTypeAccessImpl",50);
	ht.put("class spoon.support.reflect.code.CtUnaryOperatorImpl",51);
	ht.put("class spoon.support.reflect.code.CtVariableAccessImpl",52);
	ht.put("class spoon.support.reflect.code.CtVariableReadImpl",53);
	ht.put("class spoon.support.reflect.code.CtVariableWriteImpl",54);
	ht.put("class spoon.support.reflect.code.CtWhileImpl",55);
	ht.put("class spoon.support.reflect.code.CtYieldStatementImpl",56);
	ht.put("class spoon.support.reflect.declaration.CtClassImpl",57);
    }
}
