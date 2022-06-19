package org.dlr.foobar.stringencoding;

import spoon.reflect.declaration.CtElement;
import spoon.support.reflect.code.*;
import java.util.HashMap;

public enum SpoonCodeElement {

  // corresponding enum element for each class of spoon.support.reflect.code
  AnnotationFieldAccess, ArrayAccess, ArrayRead, ArrayWrite, Assert,
  Assignment, BinaryOperator, Block, Break, Case, Catch, CatchVariable,
  CodeElement, CodeSnippetExpression, CodeSnippetStatement, Comment,
  Conditional, ConstructorCall, Continue, Do, ExecutableReferenceExpression,
  Expression, FieldAccess, FieldRead, FieldWrite, ForEach, For, If,
  Invocation, JavaDoc, JavaDocTag, Lambda, Literal, LocalVariable, Loop,
  NewArray, NewClass, OperatorAssignment, Return, Statement, StatementList,
  SuperAccess, SwitchExpression, Switch, Synchronized, TargetedExpression,
  ThisAccess, Throw, Try, TryWithResource, TypeAccess, UnaryOperator,
  VariableAccess, VariableRead, VariableWrite, While, Yield;

  public static SpoonCodeElement map(CtElement element) {
    return matcher.get(element.getClass());
  }

  // since Java does not support switch on class type, we do it by ourselves
  private static final HashMap<Class, SpoonCodeElement> matcher =
      new HashMap<Class, SpoonCodeElement>();
  static {
    matcher.put(CtAnnotationFieldAccessImpl.class, AnnotationFieldAccess);
    matcher.put(CtArrayAccessImpl.class, ArrayAccess);
    matcher.put(CtArrayReadImpl.class, ArrayRead);
    matcher.put(CtArrayWriteImpl.class, ArrayWrite);
    matcher.put(CtAssertImpl.class, Assert);
    matcher.put(CtAssignmentImpl.class, Assignment);
    matcher.put(CtBinaryOperatorImpl.class, BinaryOperator);
    matcher.put(CtBlockImpl.class, Block);
    matcher.put(CtBreakImpl.class, Break);
    matcher.put(CtCaseImpl.class, Case);
    matcher.put(CtCatchImpl.class, Catch);
    matcher.put(CtCatchVariableImpl.class, CatchVariable);
    matcher.put(CtCodeElementImpl.class, CodeElement);
    matcher.put(CtCodeSnippetExpressionImpl.class, CodeSnippetExpression);
    matcher.put(CtCodeSnippetStatementImpl.class, CodeSnippetStatement);
    matcher.put(CtCommentImpl.class, Comment);
    matcher.put(CtConditionalImpl.class, Conditional);
    matcher.put(CtConstructorCallImpl.class, ConstructorCall);
    matcher.put(CtContinueImpl.class, Continue);
    matcher.put(CtDoImpl.class, Do);
    matcher.put(CtExecutableReferenceExpressionImpl.class, ExecutableReferenceExpression);
    matcher.put(CtExpressionImpl.class, Expression);
    matcher.put(CtFieldAccessImpl.class, FieldAccess);
    matcher.put(CtFieldReadImpl.class, FieldRead);
    matcher.put(CtFieldWriteImpl.class, FieldWrite);
    matcher.put(CtForEachImpl.class, ForEach);
    matcher.put(CtForImpl.class, For);
    matcher.put(CtIfImpl.class, If);
    matcher.put(CtInvocationImpl.class, Invocation);
    matcher.put(CtJavaDocImpl.class, JavaDoc);
    matcher.put(CtJavaDocTagImpl.class, JavaDocTag);
    matcher.put(CtLambdaImpl.class, Lambda);
    matcher.put(CtLiteralImpl.class, Literal);
    matcher.put(CtLocalVariableImpl.class, LocalVariable);
    matcher.put(CtLoopImpl.class, Loop);
    matcher.put(CtNewArrayImpl.class, NewArray);
    matcher.put(CtNewClassImpl.class, NewClass);
    matcher.put(CtOperatorAssignmentImpl.class, OperatorAssignment);
    matcher.put(CtReturnImpl.class, Return);
    matcher.put(CtStatementImpl.class, Statement);
    matcher.put(CtStatementListImpl.class, StatementList);
    matcher.put(CtSuperAccessImpl.class, SuperAccess);
    matcher.put(CtSwitchExpressionImpl.class, SwitchExpression);
    matcher.put(CtSwitchImpl.class, Switch);
    matcher.put(CtSynchronizedImpl.class, Synchronized);
    matcher.put(CtTargetedExpressionImpl.class, TargetedExpression);
    matcher.put(CtThisAccessImpl.class, ThisAccess);
    matcher.put(CtThrowImpl.class, Throw);
    matcher.put(CtTryImpl.class, Try);
    matcher.put(CtTryWithResourceImpl.class, TryWithResource);
    matcher.put(CtTypeAccessImpl.class, TypeAccess);
    matcher.put(CtUnaryOperatorImpl.class, UnaryOperator);
    matcher.put(CtVariableAccessImpl.class, VariableAccess);
    matcher.put(CtVariableReadImpl.class, VariableRead);
    matcher.put(CtVariableWriteImpl.class, VariableWrite);
    matcher.put(CtWhileImpl.class, While);
    matcher.put(CtYieldStatementImpl.class, Yield);
  }

}
