package org.fsu.bytecode;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.AbstractGraph;
import com.ibm.wala.util.graph.EdgeManager;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.NodeManager;
import fr.inria.controlflow.BranchKind;
import fr.inria.controlflow.ControlFlowNode;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.UnitPatchingChain;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.toolkits.graph.DominatorTree;
import soot.toolkits.graph.ExceptionalGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.util.Chain;
import soot.util.HashChain;
import soot.util.cfgcmd.CFGToDotGraph;
import soot.util.dot.DotGraph;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class DominatorTreeUnitGraph {

    private List<Chain<Unit>> domTrees=new ArrayList<>();

    // edge Manager List and node Manager List should only contain one element
    private List<EdgeManager<Unit>> edgeManagerList=new ArrayList<>();
    private List<NodeManager<Unit>> nodeManagerList=new ArrayList<>();
    private List<Unit> heads=new ArrayList<>();

    private String methodName="";

    private List<List<Unit>> fullPathes= new ArrayList<>();
    private List<List<Unit>> splitPathes= new ArrayList<>();


    public DominatorTreeUnitGraph(String methodName, AbstractGraph<Unit> dominatorTree, List<Unit> heads,SootMethod sm,boolean picture)
    {
        // heads contain the main Class and all Exceptions used in the method
        this.heads=heads;
        this.methodName = methodName; // this.methodName: selected,430580.java,49,79
        //for (AbstractGraph<Unit> dominatorTree:dominatorTrees) {

        EdgeManager<Unit> edgeManager = dominatorTree.getEdgeManager();
        NodeManager<Unit> nodeManager = dominatorTree.getNodeManager();
        edgeManagerList.add(edgeManager);
        nodeManagerList.add(nodeManager);

        if (picture) {
            createDotGraph();
        }
    }

    private void createDotGraph() {
        System.out.println("create Dominatortree dot Graph!");
        String graphName=this.methodName+"_dt.dot";
        DotGraph dotGraph=new DotGraph(graphName);
        EdgeManager<Unit> edgeManager = this.edgeManagerList.get(0);
        NodeManager<Unit> nodeManager = this.nodeManagerList.get(0);
        HashMap<Unit,String> mapping= new HashMap<>();
        int i=1;
        for(Unit n :Iterator2Iterable.make(nodeManager.iterator())) {
            String name=i+"_"+n.toString();
            dotGraph.drawNode(name);
            mapping.put(n,name);
            i++;
        }
        for (Unit n :mapping.keySet())
        {
            List<Unit> succs = new ArrayList<>();
            if (edgeManager.getSuccNodes(n)!=null) {
                for (Unit m : Iterator2Iterable.make(edgeManager.getSuccNodes(n))) {
                    succs.add(m);
                    dotGraph.drawEdge(mapping.get(n),mapping.get(m));
                }
            }
        }
        dotGraph.plot(graphName);
    }

    public String getMethodName()
    {
        return this.methodName;
    }

    public List<List<Unit>> getFullPathes()
    {
        /*
        For every Leaf Node add the unique Path to a list and update this.fullPaths at the end
         */
        if (this.fullPathes.size()>0)
            return this.fullPathes;
        List<List<Unit>> pathes= new ArrayList<>();
        /*
        Iterate over every edge > Although the manager list has always length 1 ??? TODO
         */

        for (int i=0;i<edgeManagerList.size();i++)
        {
            EdgeManager<Unit> edgeManager=edgeManagerList.get(i);
            // Contains all end Nodes in the ControllFlowGraph
            List<Unit> leafs=getLeafNodes(i);

            // iterate over every found leaf Node
            for (Unit leaf : leafs)
            {
                List<Unit> path = new ArrayList<>();
                Unit tmp=leaf;
                // Start at the leaf and go to the top in the CFG
                // check always for predecessor Nodes and the size
                // of the Iterator since if its 0 we are at the root
                while(edgeManager.getPredNodes(tmp)!=null &&Iterators.size(edgeManager.getPredNodes(tmp))>0)
                {
                    int ttt=edgeManager.getPredNodeCount(tmp);
                    path.add(tmp);
                    boolean set=false;
                    // this should only be run once in every while iteration
                    // tmp will be the Predecessor of the Node
                    // There can be only One Predecessor or the Dominator Tree is wrong
                    for (Unit pred : Iterator2Iterable.make(edgeManager.getPredNodes(tmp))) {
                        tmp = pred;
                        if (set)
                            Assertions.UNREACHABLE();
                        set = true;
                    }
                }
                path.add(tmp);
                if (path.size() > 1) { pathes.add(Lists.reverse(path)); }
            }
        }
        this.fullPathes=pathes;
        return this.fullPathes;
    }

    public List<List<Unit>> getSplitPathes()
    {
        /* Set this.splitPaths to paths there all splits are removed. Splits will be reviewed as single Path
         */
        if (this.splitPathes.size()>0)
            return this.splitPathes;
        List<List<Unit>> pathes= new ArrayList<>();
        // The Size of the edgeManagerList is always 1
        for (int i=0;i<edgeManagerList.size();i++) {
            EdgeManager<Unit> edgeManager = edgeManagerList.get(i);
            // all Nodes there the Predecessor has more then one Successor
            List<Unit> merge = getMergeNodeStarts(i);
            int count =-1;
            // Since i = 0 we start from the start Node of the function
            Unit tmp=this.heads.get(i);
            while (tmp!=null) {
                // var count counts the Nodes already used which split the path
                count++;
                List<Unit> path = new ArrayList<>();
                // while the path is strict we just go down
                while (Iterators.size(edgeManager.getSuccNodes(tmp))==1)
                {
                    path.add(tmp);
                    boolean set=false;
                    // TODO why is this working?
                    // This should fail then there is a split in the Graph
                    for (Unit pred:Iterator2Iterable.make(edgeManager.getSuccNodes(tmp)))
                    {
                        tmp=pred;
                        if (set)
                            Assertions.UNREACHABLE();
                        set=true;
                    }
                }
                path.add(tmp);
                this.splitPathes.add(path);
                if (merge.size()>count)
                    tmp=merge.get(count);
                else
                    tmp=null;
            }
        }
        return this.splitPathes;
    }

    private List<Unit> getMergeNodeStarts(int i)
    {
        /*
        always i = 0
        Iterate over every Node and if there are multiple Successors add them all in one List
         */
        List<Unit> merge= new ArrayList<>();
        NodeManager<Unit> nodeManager= nodeManagerList.get(i);
        EdgeManager<Unit> edgeManager= edgeManagerList.get(i);
        Iterator<Unit> it=nodeManager.iterator();
        // iterate over every Node and if there is more as one Successor add all Nodes to a List
        // if the Successor is clear this does nothing
        while (it.hasNext())
        {
            Unit u =it.next();
            if (edgeManager.getSuccNodeCount(u)>1)
            {
                for (Unit unit :Iterator2Iterable.make(edgeManager.getSuccNodes(u))) {
                    merge.add(unit);
                }
            }
        }
        return merge;
    }

    private List<Unit> getLeafNodes(int i)
    {
        /*
        i = 0 always (since there is only one entry in the manager)
        returns a list of all end nodes in the ControlFlowGraph
        Iterate over every Node and determine if it is an End Node
         */
        List<Unit> leafs= new ArrayList<>();
        NodeManager<Unit> nodeManager= nodeManagerList.get(i);
        EdgeManager<Unit> edgeManager= edgeManagerList.get(i);
        Iterator<Unit> it=nodeManager.iterator();
        while (it.hasNext())
        {
            Unit u =it.next();
            if (edgeManager.getSuccNodeCount(u)<=0)
            {
                leafs.add(u);
            }
        }
        return leafs;
    }

    private void CFGGraphPicture(String name, UnitGraph ug, List<Unit> colorUnits)
    {
        CFGToDotGraph cfgtodot = new CFGToDotGraph();
        //cfgtodot.setExceptionalControlFlowAttr("color","lightgray");
        DotGraph dg = cfgtodot.drawCFG((ExceptionalGraph<? extends Object>) ug);
        if (colorUnits!=null) {
            Iterator it = ug.iterator();
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
}
