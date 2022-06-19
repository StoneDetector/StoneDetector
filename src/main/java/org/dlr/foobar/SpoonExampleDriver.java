package org.dlr.foobar;

import spoon.Launcher;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtMethod;
import fr.inria.controlflow.ControlFlowBuilder;
import fr.inria.controlflow.ControlFlowGraph;

public class SpoonExampleDriver {
  public static void main(String args[]) {
    CtType type = Launcher.parseClass("public class C { void m(int i) { i=i+1; } }");
    for (Object m: type.getMethods()) {
      ControlFlowBuilder builder = new ControlFlowBuilder();
      ControlFlowGraph cfg = builder.build((CtMethod) m);
      System.out.println(cfg.toGraphVisText());
    }
  }
}
