package org.fsu.codeclones;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.inria.controlflow.BranchKind;
import fr.inria.controlflow.ControlFlowGraph;
import fr.inria.controlflow.ControlFlowNode;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.support.compiler.VirtualFile;
import spoon.reflect.code.CtCasePattern;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtReturn;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.filter.TypeFilter;

class ModernExpressionEncoderTest {

    @Test
    void blockLambdaBodiesAffectBothEncodings() {
        CtType<?> sample = model("""
                class Sample {
                    void firstCall() {}
                    void secondCall() {}

                    void declarations() {
                        Runnable first = () -> { firstCall(); };
                        Runnable second = () -> { secondCall(); };
                    }
                }
                """);

        Encodings first = encode(localVariable(sample, "first"));
        Encodings second = encode(localVariable(sample, "second"));

        assertDifferent(first, second, "block lambda bodies");
    }

    @Test
    void methodReferenceExecutableIdentityAffectsBothEncodings() {
        CtType<?> sample = model("""
                class Sample {
                    void declarations() {
                        java.util.function.Function<String, String> first = String::trim;
                        java.util.function.Function<String, String> second = String::toUpperCase;
                    }
                }
                """);
        boolean originalSupportCallNames = Environment.SUPPORTCALLNAMES;

        try {
            Environment.SUPPORTCALLNAMES = true;
            Encodings first = encode(localVariable(sample, "first"));
            Encodings second = encode(localVariable(sample, "second"));

            assertDifferent(first, second, "method-reference executable names");
        } finally {
            Environment.SUPPORTCALLNAMES = originalSupportCallNames;
        }
    }

    @Test
    void methodReferenceDeclaringTypeAffectsBothEncodings() {
        CtType<?> sample = model("""
                class Sample {
                    First first = new First();
                    Second second = new Second();

                    void declarations() {
                        java.util.function.Supplier<String> fromFirst = first::read;
                        java.util.function.Supplier<String> fromSecond = second::read;
                    }

                    static final class First { String read() { return "first"; } }
                    static final class Second { String read() { return "second"; } }
                }
                """);
        boolean originalSupportCallNames = Environment.SUPPORTCALLNAMES;

        try {
            Environment.SUPPORTCALLNAMES = true;

            assertDifferent(
                    encode(localVariable(sample, "fromFirst")),
                    encode(localVariable(sample, "fromSecond")),
                    "method-reference declaring type");
        } finally {
            Environment.SUPPORTCALLNAMES = originalSupportCallNames;
        }
    }

    @Test
    void boundMethodReferenceIdentitySurvivesInvokingReceiverEncoding() {
        CtType<?> sample = model("""
                class Sample {
                    Target target() { return new Target(); }

                    void declarations() {
                        java.util.function.Supplier<String> first = target()::first;
                        java.util.function.Supplier<String> second = target()::second;
                    }

                    static final class Target {
                        String first() { return "first"; }
                        String second() { return "second"; }
                    }
                }
                """);
        boolean originalSupportCallNames = Environment.SUPPORTCALLNAMES;

        try {
            Environment.SUPPORTCALLNAMES = true;

            assertDifferent(
                    encode(localVariable(sample, "first")),
                    encode(localVariable(sample, "second")),
                    "bound method-reference identity");
        } finally {
            Environment.SUPPORTCALLNAMES = originalSupportCallNames;
        }
    }

    @Test
    void overloadedMethodReferenceArityAffectsBothEncodings() {
        CtType<?> sample = model("""
                class Sample {
                    Target target = new Target();

                    void declarations() {
                        java.util.function.Supplier<String> noArgument = target::read;
                        java.util.function.Function<String, String> oneArgument = target::read;
                    }

                    static final class Target {
                        String read() { return "none"; }
                        String read(String value) { return value; }
                    }
                }
                """);
        boolean originalSupportCallNames = Environment.SUPPORTCALLNAMES;

        try {
            Environment.SUPPORTCALLNAMES = true;

            assertDifferent(
                    encode(localVariable(sample, "noArgument")),
                    encode(localVariable(sample, "oneArgument")),
                    "method-reference arity");
        } finally {
            Environment.SUPPORTCALLNAMES = originalSupportCallNames;
        }
    }

    @Test
    void localTypeBodiesDoNotBecomePartOfTheEnclosingLambdaEncoding() {
        CtType<?> sample = model("""
                class Sample {
                    void firstCall() {}
                    void secondCall() {}

                    void declarations() {
                        Runnable first = () -> {
                            class Local { void body() { firstCall(); } }
                        };
                        Runnable second = () -> {
                            class Local { void body() { secondCall(); } }
                        };
                    }
                }
                """);

        Encodings first = encode(localVariable(sample, "first"));
        Encodings second = encode(localVariable(sample, "second"));

        assertArrayEquals(first.complete(), second.complete());
        assertArrayEquals(first.multiset(), second.multiset());
        assertTrue(contains(first.complete(), Code.CLASSDEFINITION));
        assertTrue(contains(first.multiset(), Code.CLASSDEFINITION));
    }

    @Test
    void annotationsOnNestedDeclarationsAreNotEncodedAsRuntimeExpressions() {
        CtType<?> sample = model("""
                class Sample {
                    void declarations() {
                        Runnable annotated = () -> {
                            @SuppressWarnings("unused") int value = 1;
                        };
                    }
                }
                """);

        assertDoesNotThrow(() -> assertEncodes(localVariable(sample, "annotated")));
    }

    @Test
    void nestedLocalVariablesRespectEachEncoderVariablePolicy() {
        CtType<?> sample = model("""
                class Sample {
                    void declarations() {
                        int topInitialized = 1;
                        int topUninitialized;
                        Runnable empty = () -> {};
                        Runnable initialized = () -> { int value = 1; };
                        Runnable uninitialized = () -> { int value; };
                    }
                }
                """);
        PropertiesConfiguration withoutConstants = sourceConfiguration(false);
        PropertiesConfiguration withConstants = sourceConfiguration(true);

        int[] completeEmptyWithoutConstants =
                encodeComplete(localVariable(sample, "empty"), withoutConstants);
        int[] completeEmptyWithConstants =
                encodeComplete(localVariable(sample, "empty"), withConstants);
        int[] completeInitializedWithoutConstants =
                encodeComplete(localVariable(sample, "initialized"), withoutConstants);
        int[] completeUninitializedWithoutConstants =
                encodeComplete(localVariable(sample, "uninitialized"), withoutConstants);
        int[] completeInitializedWithConstants =
                encodeComplete(localVariable(sample, "initialized"), withConstants);
        int[] completeUninitializedWithConstants =
                encodeComplete(localVariable(sample, "uninitialized"), withConstants);
        int[] completeTopInitializedWithoutConstants =
                encodeComplete(localVariable(sample, "topInitialized"), withoutConstants);
        int[] completeTopInitializedWithConstants =
                encodeComplete(localVariable(sample, "topInitialized"), withConstants);
        int[] completeTopUninitializedWithConstants =
                encodeComplete(localVariable(sample, "topUninitialized"), withConstants);
        Encodings multisetEmpty = encode(localVariable(sample, "empty"));
        Encodings multisetInitialized = encode(localVariable(sample, "initialized"));
        Encodings multisetUninitialized = encode(localVariable(sample, "uninitialized"));
        Encodings multisetTopInitialized = encode(localVariable(sample, "topInitialized"));
        Encodings multisetTopUninitialized = encode(localVariable(sample, "topUninitialized"));

        assertTailEquals(
                completeInitializedWithoutConstants,
                completeEmptyWithoutConstants,
                completeTopInitializedWithoutConstants);
        assertArrayEquals(completeEmptyWithoutConstants, completeUninitializedWithoutConstants);
        assertTailEquals(
                completeInitializedWithConstants,
                completeEmptyWithConstants,
                completeTopInitializedWithConstants);
        assertTailEquals(
                completeUninitializedWithConstants,
                completeEmptyWithConstants,
                completeTopUninitializedWithConstants);
        assertTailEquals(
                multisetInitialized.multiset(),
                multisetEmpty.multiset(),
                multisetTopInitialized.multiset());
        assertTailEquals(
                multisetUninitialized.multiset(),
                multisetEmpty.multiset(),
                multisetTopUninitialized.multiset());
    }

    @Test
    void bareReturnInBlockLambdaIncludesVoidInBothEncodings() {
        CtType<?> sample = model("""
                class Sample {
                    void declarations() {
                        Runnable empty = () -> {};
                        Runnable returning = () -> { return; };
                    }
                }
                """);

        Encodings returning = encode(localVariable(sample, "returning"));
        Encodings empty = encode(localVariable(sample, "empty"));

        assertTrue(contains(returning.complete(), Code.RETURN));
        assertTrue(count(returning.complete(), Code.VOID) > count(empty.complete(), Code.VOID));
        assertTrue(contains(returning.multiset(), Code.RETURN));
        assertTrue(count(returning.multiset(), Code.VOID) > count(empty.multiset(), Code.VOID));
    }

    @Test
    void listConstructorsRetainTheExistingDefaultHashValue() {
        CompletePathEncoder complete =
                new CompletePathEncoder(Code.HASHVALUE, List.of(Code.HASHVALUE));
        SortedMultisetPathEncoder multiset =
                new SortedMultisetPathEncoder(List.of(Code.HASHVALUE));

        assertArrayEquals(new int[] {0}, complete.getEncoding());
        assertArrayEquals(new int[] {0}, multiset.getEncoding());
    }

    @Test
    void compoundAssignmentInBlockLambdaRetainsItsOperator() {
        CtType<?> sample = model("""
                class Sample {
                    int value;

                    void declarations() {
                        Runnable plain = () -> { value = 1; };
                        Runnable compound = () -> { value += 1; };
                    }
                }
                """);

        assertDifferent(
                encode(localVariable(sample, "plain")),
                encode(localVariable(sample, "compound")),
                "compound-assignment operator");
    }

    @Test
    void compoundAssignmentInExpressionLambdaRetainsItsOperator() {
        CtType<?> sample = model("""
                class Sample {
                    int value;

                    void declarations() {
                        java.util.function.IntUnaryOperator plain = input -> value = input;
                        java.util.function.IntUnaryOperator compound = input -> value += input;
                    }
                }
                """);

        CtLocalVariable<?> plain = localVariable(sample, "plain");
        CtLocalVariable<?> compound = localVariable(sample, "compound");
        assertDifferent(
                encode(plain),
                encode(compound),
                "expression-lambda compound-assignment operator");

        PropertiesConfiguration registerConfiguration = sourceConfiguration(true);
        registerConfiguration.setProperty("encodeAsInRegistercode", true);
        assertFalse(Arrays.equals(
                encodeComplete(plain, registerConfiguration),
                encodeComplete(compound, registerConfiguration)));
    }

    @Test
    void nestedCompoundAssignmentRetainsItsOperator() {
        CtType<?> sample = model("""
                class Sample {
                    int value;

                    int consume(int input) { return input; }

                    void declarations() {
                        java.util.function.IntSupplier plain = () -> consume(value = 1);
                        java.util.function.IntSupplier compound = () -> consume(value += 1);
                    }
                }
                """);

        assertDifferent(
                encode(localVariable(sample, "plain")),
                encode(localVariable(sample, "compound")),
                "nested compound-assignment operator");
    }

    @Test
    void constructorArgumentsInExpressionLambdaAffectBothEncodings() {
        CtType<?> sample = model("""
                class Sample {
                    static final class Box {
                        Box(int value) {}
                    }

                    int first() { return 1; }
                    int second() { return 2; }

                    void declarations() {
                        java.util.function.Supplier<Box> firstBox = () -> new Box(first());
                        java.util.function.Supplier<Box> secondBox = () -> new Box(second());
                    }
                }
                """);

        assertDifferent(
                encode(localVariable(sample, "firstBox")),
                encode(localVariable(sample, "secondBox")),
                "constructor arguments in expression lambdas");
    }

    @Test
    void rootCompoundAssignmentInSwitchArmRetainsItsOperator() {
        CtType<?> sample = model("""
                class Sample {
                    int value;

                    int plain(int selector) {
                        return switch (selector) {
                            case 0:
                                value = 1;
                                yield value;
                            default:
                                yield value;
                        };
                    }

                    int compound(int selector) {
                        return switch (selector) {
                            case 0:
                                value += 1;
                                yield value;
                            default:
                                yield value;
                        };
                    }
                }
                """);

        assertDifferent(
                encode(returnStatement(sample, "plain")),
                encode(returnStatement(sample, "compound")),
                "root compound-assignment operator");
    }

    @Test
    void stubberMethodReferenceSuffixIsNormalizedByBothEncoders() {
        CtType<?> sample = model("""
                class Sample {
                    Target target = new Target();

                    void declarations() {
                        java.util.function.Supplier<String> original = target::read;
                        java.util.function.Supplier<String> renamed = target::read_17;
                    }

                    static final class Target {
                        String read() { return "original"; }
                        String read_17() { return "renamed"; }
                    }
                }
                """);
        boolean originalSupportCallNames = Environment.SUPPORTCALLNAMES;
        boolean originalStubberProcessing = Environment.STUBBERPROCESSING;

        try {
            Environment.SUPPORTCALLNAMES = true;
            Environment.STUBBERPROCESSING = true;

            Encodings original = encode(localVariable(sample, "original"));
            Encodings renamed = encode(localVariable(sample, "renamed"));

            assertArrayEquals(original.complete(), renamed.complete());
            assertArrayEquals(original.multiset(), renamed.multiset());
        } finally {
            Environment.SUPPORTCALLNAMES = originalSupportCallNames;
            Environment.STUBBERPROCESSING = originalStubberProcessing;
        }
    }

    @Test
    void methodReferenceHonorsRegisterCodeCallNameRepresentation() {
        CtType<?> sample = model("""
                class Sample {
                    void declarations() {
                        java.util.function.Function<String, String> reference = String::trim;
                    }

                    String invoke(String value) { return value.trim(); }
                }
                """);
        PropertiesConfiguration configuration = sourceConfiguration(true);
        configuration.setProperty("encodeAsInRegistercode", true);
        boolean originalSupportCallNames = Environment.SUPPORTCALLNAMES;

        try {
            Environment.SUPPORTCALLNAMES = true;
            CompletePathEncoder encoder =
                    new CompletePathEncoder(node(localVariable(sample, "reference")), configuration);
            CtInvocation<?> trimInvocation = sample.getElements(new TypeFilter<>(CtInvocation.class)).stream()
                    .filter(invocation -> invocation.getExecutable().getSimpleName().equals("trim"))
                    .findFirst()
                    .orElseThrow();
            CompletePathEncoder invocation = new CompletePathEncoder(
                    node(trimInvocation), configuration);

            assertFalse(encoder.opKind.contains(Code.HASHVALUE));
            assertTrue(encoder.functionNames.contains("trim"));
            assertFalse(invocation.opKind.contains(Code.HASHVALUE));
            assertTrue(invocation.functionNames.contains("trim"));
            assertTrue(encoder.opKind.contains(Code.SPECIALCALL));
            assertFalse(encoder.opKind.contains(Code.CALL));
            assertTrue(invocation.opKind.contains(Code.SPECIALCALL));

            Environment.SUPPORTCALLNAMES = false;
            CompletePathEncoder withoutNames =
                    new CompletePathEncoder(node(localVariable(sample, "reference")), configuration);

            assertFalse(withoutNames.opKind.contains(Code.HASHVALUE));
            assertTrue(withoutNames.functionNames.isEmpty());
        } finally {
            Environment.SUPPORTCALLNAMES = originalSupportCallNames;
        }
    }

    @Test
    void switchExpressionBlockArmsEncodeEveryStatementCategory() {
        CtType<?> sample = model("""
                class Sample {
                    void call() {}
                    void fallback() {}
                    void cleanup() {}

                    int baseline(int selector) {
                        return switch (selector) {
                            case 0 -> { yield selector; }
                            default -> selector;
                        };
                    }

                    int localVariable(int selector) {
                        return switch (selector) {
                            case 0 -> { int value = callAndReturn(); yield selector; }
                            default -> selector;
                        };
                    }

                    int statementExpression(int selector) {
                        return switch (selector) {
                            case 0 -> { call(); yield selector; }
                            default -> selector;
                        };
                    }

                    int conditional(int selector) {
                        return switch (selector) {
                            case 0 -> { if (selector > 0) { call(); } yield selector; }
                            default -> selector;
                        };
                    }

                    int forLoop(int selector) {
                        return switch (selector) {
                            case 0 -> { for (int i = 0; i < 1; i++) { call(); } yield selector; }
                            default -> selector;
                        };
                    }

                    int enhancedForLoop(int selector) {
                        return switch (selector) {
                            case 0 -> { for (int value : new int[] {selector}) { call(); } yield selector; }
                            default -> selector;
                        };
                    }

                    int whileLoop(int selector) {
                        return switch (selector) {
                            case 0 -> { while (selector > 0) { call(); break; } yield selector; }
                            default -> selector;
                        };
                    }

                    int doLoop(int selector) {
                        return switch (selector) {
                            case 0 -> { do { call(); } while (false); yield selector; }
                            default -> selector;
                        };
                    }

                    int continueLoop(int selector) {
                        return switch (selector) {
                            case 0 -> {
                                for (int i = 0; i < 1; i++) {
                                    if (i == 0) { continue; }
                                    call();
                                }
                                yield selector;
                            }
                            default -> selector;
                        };
                    }

                    int nestedTry(int selector) {
                        return switch (selector) {
                            case 0 -> {
                                try { call(); }
                                catch (RuntimeException ignored) { fallback(); }
                                finally { cleanup(); }
                                yield selector;
                            }
                            default -> selector;
                        };
                    }

                    int tryWithResource(int selector) {
                        return switch (selector) {
                            case 0 -> {
                                try (Resource ignored = new Resource()) { call(); }
                                yield selector;
                            }
                            default -> selector;
                        };
                    }

                    int synchronizedBlock(int selector) {
                        return switch (selector) {
                            case 0 -> { synchronized (this) { call(); } yield selector; }
                            default -> selector;
                        };
                    }

                    int assertion(int selector) {
                        return switch (selector) {
                            case 0 -> { assert selector > 0; yield selector; }
                            default -> selector;
                        };
                    }

                    int throwStatement(int selector) {
                        return switch (selector) {
                            case 0 -> {
                                if (selector < 0) { throw new IllegalStateException(); }
                                yield selector;
                            }
                            default -> selector;
                        };
                    }

                    int nestedSwitch(int selector) {
                        return switch (selector) {
                            case 0 -> {
                                switch (selector) { case 1 -> call(); default -> fallback(); }
                                yield selector;
                            }
                            default -> selector;
                        };
                    }

                    int nestedBlock(int selector) {
                        return switch (selector) {
                            case 0 -> { { call(); } yield selector; }
                            default -> selector;
                        };
                    }

                    int callAndReturn() { call(); return 1; }

                    static final class Resource implements AutoCloseable {
                        public void close() {}
                    }
                }
                """);
        Encodings baseline = encode(returnStatement(sample, "baseline"));

        assertAll(
                () -> assertDifferent(
                        baseline, encode(returnStatement(sample, "localVariable")), "local variable"),
                () -> assertDifferent(
                        baseline,
                        encode(returnStatement(sample, "statementExpression")),
                        "statement expression"),
                () -> assertDifferent(
                        baseline, encode(returnStatement(sample, "conditional")), "if statement"),
                () -> assertDifferent(
                        baseline, encode(returnStatement(sample, "forLoop")), "for loop"),
                () -> assertDifferent(
                        baseline,
                        encode(returnStatement(sample, "enhancedForLoop")),
                        "enhanced-for loop"),
                () -> assertDifferent(
                        baseline, encode(returnStatement(sample, "whileLoop")), "while loop"),
                () -> assertDifferent(
                        baseline, encode(returnStatement(sample, "doLoop")), "do loop"),
                () -> assertDifferent(
                        baseline, encode(returnStatement(sample, "continueLoop")), "continue statement"),
                () -> assertDifferent(
                        baseline, encode(returnStatement(sample, "nestedTry")), "try statement"),
                () -> assertDifferent(
                        baseline,
                        encode(returnStatement(sample, "tryWithResource")),
                        "try-with-resources statement"),
                () -> assertDifferent(
                        baseline,
                        encode(returnStatement(sample, "synchronizedBlock")),
                        "synchronized statement"),
                () -> assertDifferent(
                        baseline, encode(returnStatement(sample, "assertion")), "assert statement"),
                () -> assertDifferent(
                        baseline, encode(returnStatement(sample, "throwStatement")), "throw statement"),
                () -> assertDifferent(
                        baseline, encode(returnStatement(sample, "nestedSwitch")), "switch statement"),
                () -> assertDifferent(
                        baseline, encode(returnStatement(sample, "nestedBlock")), "nested block"));
    }

    @Test
    void encodesModernExpressionsWithoutDroppingTheContainingMethod() {
        String source = """
                class Sample {
                    record Pair(String first, String second) {}

                    Object transform(Object candidate) {
                        java.util.function.Function<String, String> reference = String::trim;
                        java.util.function.Predicate<String> lambda = text -> text.isBlank();
                        return switch (candidate) {
                            case String text -> lambda.test(reference.apply(text));
                            case Pair(_, _) -> candidate;
                            default -> null;
                        };
                    }
                }
                """;
        CtType<?> sample = model(source);

        List<CtLocalVariable<?>> variables =
                sample.getElements(new TypeFilter<>(CtLocalVariable.class));
        CtReturn<?> returnStatement = sample.getElements(new TypeFilter<>(CtReturn.class)).get(0);
        List<CtCasePattern> casePatterns =
                sample.getElements(new TypeFilter<>(CtCasePattern.class));
        assertFalse(casePatterns.isEmpty());

        for (CtLocalVariable<?> variable : variables) {
            assertDoesNotThrow(() -> assertEncodes(variable));
        }
        assertDoesNotThrow(() -> assertEncodes(returnStatement));
        for (CtCasePattern casePattern : casePatterns) {
            assertDoesNotThrow(() -> assertEncodes(casePattern));
        }
    }

    private static void assertEncodes(spoon.reflect.declaration.CtElement statement) {
        Encodings encodings = encode(statement);

        assertTrue(encodings.complete().length > 0);
        assertTrue(encodings.multiset().length > 0);
    }

    private static Encodings encode(spoon.reflect.declaration.CtElement statement) {
        ControlFlowNode node =
                new ControlFlowNode(statement, new ControlFlowGraph(), BranchKind.STATEMENT);

        CompletePathEncoder complete = new CompletePathEncoder(node);
        SortedMultisetPathEncoder multiset = new SortedMultisetPathEncoder(node);

        return new Encodings(complete.getEncoding(), multiset.getEncoding());
    }

    private static int[] encodeComplete(
            spoon.reflect.declaration.CtElement statement, PropertiesConfiguration configuration) {
        return new CompletePathEncoder(node(statement), configuration).getEncoding();
    }

    private static ControlFlowNode node(spoon.reflect.declaration.CtElement statement) {
        return new ControlFlowNode(statement, new ControlFlowGraph(), BranchKind.STATEMENT);
    }

    private static PropertiesConfiguration sourceConfiguration(boolean constants) {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.setProperty("constants", constants);
        configuration.setProperty("newCode", true);
        configuration.setProperty("functionParameters", true);
        configuration.setProperty("ifNodeCompareOperator", true);
        configuration.setProperty("fieldread", true);
        configuration.setProperty("fieldwrite", true);
        configuration.setProperty("encodeAsInRegistercode", false);
        return configuration;
    }

    private static void assertDifferent(Encodings first, Encodings second, String description) {
        assertAll(
                () -> assertFalse(
                        Arrays.equals(first.complete(), second.complete()),
                        "CompletePathEncoder omitted " + description),
                () -> assertFalse(
                        Arrays.equals(first.multiset(), second.multiset()),
                        "SortedMultisetPathEncoder omitted " + description));
    }

    private static boolean contains(int[] encoding, Code code) {
        return Arrays.stream(encoding).anyMatch(value -> value == code.ordinal());
    }

    private static long count(int[] encoding, Code code) {
        return Arrays.stream(encoding).filter(value -> value == code.ordinal()).count();
    }

    private static void assertTailEquals(int[] actual, int[] prefix, int[] expectedTail) {
        assertArrayEquals(prefix, Arrays.copyOf(actual, prefix.length));
        assertArrayEquals(expectedTail, Arrays.copyOfRange(actual, prefix.length, actual.length));
    }

    private static CtType<?> model(String source) {
        Launcher launcher = new Launcher();
        launcher.getEnvironment().setComplianceLevel(25);
        launcher.addInputResource(new VirtualFile(source, "Sample.java"));
        launcher.buildModel();
        return launcher.getFactory().Type().get("Sample");
    }

    private static CtLocalVariable<?> localVariable(CtType<?> type, String name) {
        return type.getElements(new TypeFilter<>(CtLocalVariable.class)).stream()
                .filter(variable -> variable.getSimpleName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static CtReturn<?> returnStatement(CtType<?> type, String methodName) {
        return type.getMethodsByName(methodName).get(0).getElements(new TypeFilter<>(CtReturn.class)).get(0);
    }

    private record Encodings(int[] complete, int[] multiset) {}
}
