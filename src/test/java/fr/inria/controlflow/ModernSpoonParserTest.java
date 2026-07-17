package fr.inria.controlflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.support.compiler.VirtualFile;
import spoon.reflect.code.CtTextBlock;
import spoon.reflect.code.CtUnnamedPattern;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.filter.TypeFilter;

class ModernSpoonParserTest {

    @Test
    void parsesCurrentJavaSyntaxWithTheMaintainedSpoonDependency() {
        String source = """
                record Sample(String first, String second) {
                    String describe(Object value) {
                        return switch (value) {
                            case Sample(_, _) -> \"""
                                    modern Java
                                    \""";
                            default -> value.toString();
                        };
                    }
                }
                """;
        Launcher launcher = new Launcher();
        launcher.getEnvironment().setComplianceLevel(25);
        launcher.addInputResource(new VirtualFile(source, "Sample.java"));
        launcher.buildModel();
        CtType<?> sample = launcher.getFactory().Type().get("Sample");

        List<CtTextBlock> textBlocks = sample.getElements(new TypeFilter<>(CtTextBlock.class));
        List<CtUnnamedPattern> unnamedPatterns =
                sample.getElements(new TypeFilter<>(CtUnnamedPattern.class));

        assertEquals("Sample", sample.getSimpleName());
        assertFalse(textBlocks.isEmpty());
        assertFalse(unnamedPatterns.isEmpty());
    }
}
