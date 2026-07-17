package org.fsu.codeclones;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.Assertions;
import fr.inria.controlflow.ControlFlowNode;
import spoon.reflect.code.CtAssert;
import spoon.reflect.code.CtBreak;
import spoon.reflect.code.CtCatch;
import spoon.reflect.code.CtContinue;
import spoon.reflect.code.CtDo;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtExecutableReferenceExpression;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtOperatorAssignment;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtSwitch;
import spoon.reflect.code.CtSynchronized;
import spoon.reflect.code.CtThrow;
import spoon.reflect.code.CtTry;
import spoon.reflect.code.CtTryWithResource;
import spoon.reflect.code.CtWhile;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.path.CtRole;
import spoon.reflect.visitor.CtScanner;
import spoon.reflect.reference.CtTypeReference;

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

    protected abstract static class StatementOperatorScanner extends CtScanner {
        private final List<Code> codes;
        private final boolean encodeVariableMarkers;

        protected StatementOperatorScanner(List<Code> codes, boolean encodeVariableMarkers) {
            this.codes = codes;
            this.encodeVariableMarkers = encodeVariableMarkers;
        }

        protected abstract void encodeExpression(CtExpression<?> expression);

        protected abstract void encodeOperatorAssignment(CtOperatorAssignment<?, ?> assignment);

        @Override
        public void scan(CtRole role, CtElement element) {
            if (role == CtRole.ANNOTATION) {
                return;
            }
            if (element instanceof CtOperatorAssignment<?, ?>) {
                encodeOperatorAssignment((CtOperatorAssignment<?, ?>) element);
                return;
            }
            if (element instanceof CtExpression<?>) {
                encodeExpression((CtExpression<?>) element);
                return;
            }
            super.scan(role, element);
        }

        @Override
        public <T> void visitCtLocalVariable(CtLocalVariable<T> localVariable) {
            if (localVariable.getDefaultExpression() != null) {
                codes.add(Code.ASSIGN);
            } else if (encodeVariableMarkers) {
                codes.add(Code.VAR);
            }
            super.visitCtLocalVariable(localVariable);
        }

        @Override
        public void visitCtIf(CtIf ifStatement) {
            codes.add(Code.COND);
            super.visitCtIf(ifStatement);
        }

        @Override
        public void visitCtFor(CtFor forLoop) {
            codes.add(Code.COND);
            super.visitCtFor(forLoop);
        }

        @Override
        public void visitCtForEach(CtForEach forEachLoop) {
            codes.add(Code.COND);
            super.visitCtForEach(forEachLoop);
        }

        @Override
        public void visitCtWhile(CtWhile whileLoop) {
            codes.add(Code.COND);
            super.visitCtWhile(whileLoop);
        }

        @Override
        public void visitCtDo(CtDo doLoop) {
            codes.add(Code.COND);
            super.visitCtDo(doLoop);
        }

        @Override
        public void visitCtTry(CtTry tryStatement) {
            codes.add(Code.TRY);
            if (tryStatement.getFinalizer() != null) {
                codes.add(Code.FINALLY);
            }
            super.visitCtTry(tryStatement);
        }

        @Override
        public void visitCtTryWithResource(CtTryWithResource tryStatement) {
            codes.add(Code.TRY);
            if (tryStatement.getFinalizer() != null) {
                codes.add(Code.FINALLY);
            }
            super.visitCtTryWithResource(tryStatement);
        }

        @Override
        public void visitCtCatch(CtCatch catchBlock) {
            codes.add(Code.CATCH);
            super.visitCtCatch(catchBlock);
        }

        @Override
        public void visitCtSynchronized(CtSynchronized synchronizedStatement) {
            codes.add(Code.MONITOR);
            super.visitCtSynchronized(synchronizedStatement);
        }

        @Override
        public <T> void visitCtAssert(CtAssert<T> asserted) {
            codes.add(Code.ASSERT);
            super.visitCtAssert(asserted);
        }

        @Override
        public <R> void visitCtReturn(CtReturn<R> returned) {
            codes.add(Code.RETURN);
            if (returned.getReturnedExpression() == null) {
                codes.add(Code.VOID);
            }
            super.visitCtReturn(returned);
        }

        @Override
        public void visitCtThrow(CtThrow thrown) {
            codes.add(Code.THROW);
            super.visitCtThrow(thrown);
        }

        @Override
        public void visitCtBreak(CtBreak breakStatement) {
            codes.add(Code.BREAK);
            super.visitCtBreak(breakStatement);
        }

        @Override
        public void visitCtContinue(CtContinue continueStatement) {
            codes.add(Code.CONTINUE);
            super.visitCtContinue(continueStatement);
        }

        @Override
        public <T> void visitCtClass(CtClass<T> declaredType) {
            codes.add(Code.CLASSDEFINITION);
        }

        @Override
        public <T> void visitCtInterface(CtInterface<T> declaredType) {
            codes.add(Code.CLASSDEFINITION);
        }

        @Override
        public <T extends Enum<?>> void visitCtEnum(CtEnum<T> declaredType) {
            codes.add(Code.CLASSDEFINITION);
        }

        @Override
        public <T extends java.lang.annotation.Annotation> void visitCtAnnotationType(
                CtAnnotationType<T> declaredType) {
            codes.add(Code.CLASSDEFINITION);
        }

        @Override
        public void visitCtRecord(CtRecord declaredType) {
            codes.add(Code.CLASSDEFINITION);
        }

        @Override
        public <S> void visitCtSwitch(CtSwitch<S> switchStatement) {
            codes.add(Code.SWITCH);
            super.visitCtSwitch(switchStatement);
        }

        protected final void scanStatement(CtStatement statement) {
            if (statement instanceof CtOperatorAssignment<?, ?>) {
                encodeOperatorAssignment((CtOperatorAssignment<?, ?>) statement);
            } else if (statement instanceof CtExpression<?>) {
                encodeExpression((CtExpression<?>) statement);
            } else if (statement != null) {
                scan(statement);
            }
        }
    }

    protected static String methodReferenceIdentity(
            CtExecutableReferenceExpression<?, ?> reference) {
        String name = methodReferenceName(reference);
        CtTypeReference<?> declaringType = reference.getExecutable().getDeclaringType();
        StringBuilder identity = new StringBuilder();
        if (declaringType != null) {
            identity.append(declaringType.getQualifiedName()).append('#');
        }
        identity.append(name).append('(');
        boolean first = true;
        for (CtTypeReference<?> parameter : reference.getExecutable().getParameters()) {
            if (!first) {
                identity.append(',');
            }
            identity.append(parameter.getQualifiedName());
            first = false;
        }
        return identity.append(')').toString();
    }

    protected static String methodReferenceName(
            CtExecutableReferenceExpression<?, ?> reference) {
        String name = reference.getExecutable().getSimpleName();
        if (Environment.STUBBERPROCESSING && name.contains("_")) {
            name = name.substring(0, name.lastIndexOf('_'));
        }
        return name;
    }
    
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
	ht.put("class spoon.support.reflect.code.CtTextBlockImpl",32);
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
