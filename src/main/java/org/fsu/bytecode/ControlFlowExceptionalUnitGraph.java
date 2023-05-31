package org.fsu.bytecode;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import org.apache.commons.configuration2.FileBasedConfiguration;
import soot.Body;
import soot.Unit;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.util.Chain;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class ControlFlowExceptionalUnitGraph extends ExceptionalUnitGraph implements Graph<Unit>
{
    public int exceptionCount;
    public Unit headNode;


    public ControlFlowExceptionalUnitGraph(Body body, FileBasedConfiguration configuration)
    {
        super(body);

        // count exceptions
        exceptionCount = 0;
        headNode = this.unitChain.getFirst();

        if (configuration.getBoolean("countExceptions")){
            for (Unit unit : this.unitChain) {
                if (getPredNodeCount(unit) == 0){
                    if (!unit.equals(headNode) && unit.toString().contains("@caughtexception")){
                        exceptionCount += 1;
                    }
                }
            }
        }
    }

    @Override
    public Iterator<Unit> getPredNodes(Unit n) {
        List<Unit> preds;
        preds=super.getPredsOf(n);
        return preds.iterator();
    }

    @Override
    public int getPredNodeCount(Unit n) {
        return super.getPredsOf(n).size();
    }

    @Override
    public Iterator<Unit> getSuccNodes(Unit n) {
        List<Unit> list;
        // TODO
        // add units super.unitChain
        list=super.getSuccsOf(n);
        return list.iterator();
    }

    @Override
    public int getSuccNodeCount(Unit n) {
        return super.getSuccsOf(n).size();
    }

    @Override
    public void removeAllIncidentEdges(Unit node) throws UnsupportedOperationException {
        Assertions.UNREACHABLE();
    }

    @Override
    public void removeIncomingEdges(Unit node) throws UnsupportedOperationException {
        Assertions.UNREACHABLE();
    }

    @Override
    public void removeOutgoingEdges(Unit node) throws UnsupportedOperationException {
        Assertions.UNREACHABLE();
    }

    @Override
    public boolean hasEdge(Unit src, Unit dst) {
        return super.getSuccsOf(src).contains(dst);
    }

    @Override
    public void removeNodeAndEdges(Unit n) throws UnsupportedOperationException {
        Assertions.UNREACHABLE();
    }

    @Override
    public Stream<Unit> stream() {
        Chain<Unit> chain=this.unitChain;
        return chain.stream();
    }

    @Override
    public int getNumberOfNodes() {
        Chain<Unit> chain=this.unitChain;
        return chain.size();
    }

    @Override
    public void addNode(Unit n) {
        Chain<Unit> chain=this.unitChain;
        chain.add(n);
    }

    @Override
    public void removeNode(Unit n) throws UnsupportedOperationException {
        Chain<Unit> chain=this.unitChain;
        chain.remove(n);
    }

    @Override
    public boolean containsNode(Unit n) {
        Chain<Unit> chain=this.unitChain;
        return chain.contains(n);
    }
}
