package org.fsu.codeclones;


import java.util.HashMap;
import com.ibm.wala.util.graph.AbstractGraph;
import com.ibm.wala.util.graph.EdgeManager;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.Assertions;
import fr.inria.controlflow.ControlFlowGraph;
import fr.inria.controlflow.BranchKind;
import fr.inria.controlflow.ControlFlowNode;
import fr.inria.controlflow.ControlFlowEdge;
import org.dlr.foobar.SpoonBigCloneBenchDriver;
import spoon.reflect.code.CtExpression;
import spoon.reflect.declaration.CtClass;
import spoon.support.reflect.code.CtConstructorCallImpl;
import spoon.support.reflect.code.CtInvocationImpl;
import spoon.support.reflect.code.CtNewClassImpl;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;

public class DominatorTree extends ControlFlowGraph{

    static HashMap<String,Integer> methodTable = new HashMap<String,Integer>();
    static int hashCounter = 0;
    private List<CtClass> anonymInnerClasses=new ArrayList<CtClass>();
	private List<Integer> anonymInnerClassesPathID=new ArrayList<Integer>();
	
    private List<List<ControlFlowNode>> descriptionSet;

    private List<List<Encoder>> encodedDescriptionSet;

    void setDescriptionSet(List<List<ControlFlowNode>> descriptionSet){
	this.descriptionSet = descriptionSet;
    }

    List<List<ControlFlowNode>> getDescriptionSet(){
	return this.descriptionSet;
    }

    void setEncodedDescriptionSet(List<List<Encoder>> encodedDescriptionSet){
	this.encodedDescriptionSet = encodedDescriptionSet;
    }

    List<List<Encoder>> getEncodedDescriptionSet(){
	return this.encodedDescriptionSet;
    }


    
    public DominatorTree(AbstractGraph<ControlFlowNode> graph) {
	ControlFlowGraph g = (ControlFlowGraph) graph.getNodeManager();
	EdgeManager<ControlFlowNode> edgeManager = graph.getEdgeManager();
	ControlFlowNode root = null;
	
	for(ControlFlowNode n : g.vertexSet()) {
	    if(n.getKind() != BranchKind.EXIT){
		addNode(n);
	    }
	    if(n.getKind() == BranchKind.BEGIN)
		root = n;
	}
	
	for (ControlFlowNode n : vertexSet()) {
	    
		for (ControlFlowNode m : Iterator2Iterable.make(edgeManager.getSuccNodes(n)))
		    if(m.getKind() != BranchKind.EXIT)
			addEdge(n,m);
		
		if(edgeManager.getPredNodes(n) != null){    
		    for (ControlFlowNode m : Iterator2Iterable.make(edgeManager.getPredNodes(n))) // Check this, do we need to calculate this. WA
			addEdge(m,n);
		}else
		    addEdge(root,n); // This is because some nodes that are not the root and which have no predecessor node. See also Dominators.java at Line 152
	}

        	
     }

 
      public boolean isLeafNode(ControlFlowNode n){
    	Set<ControlFlowEdge>  s = outgoingEdgesOf(n);
	return s.isEmpty();
    }

    
    public boolean isMergeNode(ControlFlowNode n){
    	Set<ControlFlowEdge>  s = outgoingEdgesOf(n);
	return s.size() > 1;
    }

    public List<ControlFlowNode> getLeafNodes(){
	List<ControlFlowNode>  s  = new ArrayList<ControlFlowNode>();
	for (ControlFlowNode n : vertexSet()){
	    if(isLeafNode(n))
		s.add(n);
	}
	return s;           
    }

    public List<ControlFlowNode> getMergeNodes(){
	List<ControlFlowNode>  s  = new ArrayList<ControlFlowNode>();
	for (ControlFlowNode n : vertexSet()){
	    if(isMergeNode(n))
		s.add(n);
	}
	return s;
    }

    List<ControlFlowNode> makePathToBegin(ControlFlowNode n){

	 List<ControlFlowNode>  s  = new ArrayList<ControlFlowNode>();
	 Iterator<ControlFlowNode> p;
	 s.add(n);
	 ControlFlowNode k = n;
	 while(k.getKind() != BranchKind.BEGIN){
	     p = getPredNodes(k);
		 for(ControlFlowNode m: Iterator2Iterable.make(p)){
		     s.add(m);
		     k = m;
		 }
	 }

	 Collections.reverse(s); // Invertiere die Liste, so dass die Pfade vom Startknoten beginnen.
	 return s;
	 
    }

     List<ControlFlowNode> makePathToBeginOrMerge(ControlFlowNode n){

	 List<ControlFlowNode>  s  = new ArrayList<ControlFlowNode>();
	 Iterator<ControlFlowNode> p;
	 s.add(n);
	 ControlFlowNode k = n;
	 while(k.getKind() != BranchKind.BRANCH &&
	       k.getKind() != BranchKind.BEGIN){
	     p = getPredNodes(k);
	     for(ControlFlowNode m: Iterator2Iterable.make(p)){
		 s.add(m);
		 k = m;
	     }
	     
	 }
	 Collections.reverse(s); // Invertiere die Liste, so dass die Pfade vom Startknoten beginnen.
	 return s;
    }
    
     public List<List<ControlFlowNode>> makePathToBeginSet(){
	List<ControlFlowNode> l = getLeafNodes();
	List<List<ControlFlowNode>>  s  = new ArrayList<List<ControlFlowNode>>();
	int count=0;
	for(ControlFlowNode n : l){
		if (n.getStatement() instanceof CtInvocationImpl && ( ((CtInvocationImpl)n.getStatement()).getArguments().size()!=0 ||
				((CtInvocationImpl)n.getStatement()).getTarget() instanceof CtConstructorCallImpl && ((CtConstructorCallImpl)((CtInvocationImpl)n.getStatement()).getTarget()).getArguments()!=null))
		{
			List<CtExpression> arguments=null;
			if (n.getStatement() instanceof CtInvocationImpl &&  ((CtInvocationImpl)n.getStatement()).getArguments().size()!=0 )
				arguments=((CtInvocationImpl)n.getStatement()).getArguments();
			else
				arguments=((CtConstructorCallImpl)((CtInvocationImpl)n.getStatement()).getTarget()).getArguments();
			for (int i=0;i<arguments.size();i++)
			{
				if (arguments.get(i) instanceof CtNewClassImpl && ((CtNewClassImpl)arguments.get(i)).getAnonymousClass()!=null) {
					anonymInnerClasses.add(((CtNewClassImpl) arguments.get(i)).getAnonymousClass());
					anonymInnerClassesPathID.add(count);
				}
			}
		}
	    List<ControlFlowNode> tmp = makePathToBegin(n);
	    // System.out.println(i++ + ": "+ tmp);
	    s.add(tmp);
	    count++;
	}
	descriptionSet = s;
	return s;
    }

    public List<List<ControlFlowNode>> makePathToBeginOrMergeSet(){
	List<ControlFlowNode> l = getLeafNodes();
	List<ControlFlowNode> m = getMergeNodes();
	Iterator<ControlFlowNode> p;
	List<List<ControlFlowNode>>  s  = new ArrayList<List<ControlFlowNode>>();
	int i = 0;
	for (ControlFlowNode n : m) {
	     p = getPredNodes(n);
		 for(ControlFlowNode k: Iterator2Iterable.make(p)){
		     List<ControlFlowNode> t = makePathToBeginOrMerge(k);
		     if(t.size() > 1){
			 s.add(t);
			 // System.out.println(i++ + ": " + t);
		     }

		 }
	}

	int count=0;
       	for(ControlFlowNode n : l){
			if (n.getStatement() instanceof CtInvocationImpl && ( ((CtInvocationImpl)n.getStatement()).getArguments().size()!=0 ||
					((CtInvocationImpl)n.getStatement()).getTarget() instanceof CtConstructorCallImpl && ((CtConstructorCallImpl)((CtInvocationImpl)n.getStatement()).getTarget()).getArguments()!=null))
			{
				List<CtExpression> arguments=null;
				if (n.getStatement() instanceof CtInvocationImpl &&  ((CtInvocationImpl)n.getStatement()).getArguments().size()!=0 )
					arguments=((CtInvocationImpl)n.getStatement()).getArguments();
				else
					arguments=((CtConstructorCallImpl)((CtInvocationImpl)n.getStatement()).getTarget()).getArguments();
				for (i=0;i<arguments.size();i++)
				{
					if (arguments.get(i) instanceof CtNewClassImpl && ((CtNewClassImpl)arguments.get(i)).getAnonymousClass()!=null) {
						anonymInnerClasses.add(((CtNewClassImpl) arguments.get(i)).getAnonymousClass());
						anonymInnerClassesPathID.add(count);
					}
				}
			}
	    List<ControlFlowNode> tmp = makePathToBeginOrMerge(n);
	    s.add(tmp);
	    count++;
	    // System.out.println(i++ + ": "+ tmp);
	}
	descriptionSet = s;
	//System.out.println("Vorher ");
	//System.out.println(s);
	return s;
    }

    public List<List<Encoder>> encodePathSet(EncoderKind split, EncoderKind kind, EncoderKind sorted){ 

	if(sorted == EncoderKind.SORTED)
	    HammingDistance.SORTED = true;
	
	if(split == EncoderKind.SPLITTING)
	    makePathToBeginOrMergeSet();
	else
	    makePathToBeginSet();

	//System.out.println(descriptionSet);
	if(kind == EncoderKind.ABSTRACT)
	    encodedDescriptionSet = (new AbstractEncoder()).encodeDescriptionSet(descriptionSet);
	else if(kind == EncoderKind.COMPLETEPATH)
	    encodedDescriptionSet = (new CompletePathEncoder()).encodeDescriptionSet(descriptionSet);
	else if(kind == EncoderKind.HASH)
	    encodedDescriptionSet = (new HashEncoder()).encodeDescriptionSet(descriptionSet);
	else
	    Assertions.UNREACHABLE("Argument is wrong.");

	for (int i=0;i<anonymInnerClasses.size();i++)
	{
		for (Object m : anonymInnerClasses.get(i).getTypeMembers()) {
			try {
				SpoonBigCloneBenchDriver spoonBigCloneBenchDriver=new SpoonBigCloneBenchDriver("");
				spoonBigCloneBenchDriver.setSkipClones(true);
				List<List<Encoder>> pathSetFromInnerAnonymousMethod=spoonBigCloneBenchDriver.extractGraphs(anonymInnerClasses.get(i), m, "");
				for (List<Encoder> listEncoder :pathSetFromInnerAnonymousMethod)
					for (Encoder e : listEncoder)
						encodedDescriptionSet.get(anonymInnerClassesPathID.get(i)).add(e);
			} catch (Throwable e) {
			}
		}
	}
	anonymInnerClasses=new ArrayList<CtClass>();;

	return encodedDescriptionSet;

    }

    
}
