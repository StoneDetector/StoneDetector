package fr.inria.controlflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.reflect.declaration.CtMethod;
import spoon.support.compiler.VirtualFile;

class ControlFlowBuilderTest {

    @Test
    void buildsControlFlowForEmptyEnhancedForBody() {
        ControlFlowGraph graph = buildGraph("for (String value : values);", "Iterable<String> values");

        assertEquals(1, graph.branchCount());
        assertEquals(1, graph.edgeSet().stream().filter(ControlFlowEdge::isBackEdge).count());
        assertTrue(hasPath(graph, graph.entry(), graph.findNodesOfKind(BranchKind.EXIT).get(0)));
    }

    @Test
    void buildsControlFlowForEmptyDoBody() {
        ControlFlowGraph graph = buildGraph("do; while (false);", "");

        assertEquals(1, graph.branchCount());
        assertEquals(1, graph.edgeSet().stream().filter(ControlFlowEdge::isBackEdge).count());
        assertTrue(hasPath(graph, graph.entry(), graph.findNodesOfKind(BranchKind.EXIT).get(0)));
    }

    private static ControlFlowGraph buildGraph(String statement, String parameters) {
        String source = """
                class EmptyLoop {
                    void loop(%s) {
                        %s
                    }
                }
                """.formatted(parameters, statement);
        Launcher launcher = new Launcher();
        launcher.getEnvironment().setComplianceLevel(25);
        launcher.addInputResource(new VirtualFile(source, "EmptyLoop.java"));
        launcher.buildModel();
        CtMethod<?> method = launcher.getFactory().Type().get("EmptyLoop").getMethodsByName("loop").get(0);

        return new ControlFlowBuilder().build(method);
    }

    private static boolean hasPath(
            ControlFlowGraph graph, ControlFlowNode source, ControlFlowNode destination) {
        Set<ControlFlowNode> visited = new HashSet<>();
        ArrayDeque<ControlFlowNode> pending = new ArrayDeque<>();
        pending.add(source);
        while (!pending.isEmpty()) {
            ControlFlowNode node = pending.removeFirst();
            if (node.equals(destination)) {
                return true;
            }
            if (visited.add(node)) {
                pending.addAll(node.next());
            }
        }
        return false;
    }
}
