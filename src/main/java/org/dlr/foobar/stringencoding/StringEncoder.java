package org.dlr.foobar.stringencoding;
import fr.inria.controlflow.ControlFlowNode;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.code.*;
import java.lang.IllegalArgumentException;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.dlr.foobar.stringencoding.SpoonCodeElement;
import org.dlr.foobar.stringencoding.TokenSeparator;
import org.fsu.codeclones.Encoder;
import org.fsu.codeclones.CompletePathEncoder;
import org.fsu.codeclones.Code;



public class StringEncoder {

	TokenSeparator tokenSeparator;

	public StringEncoder(TokenSeparator tokenSeparator) {
		this.tokenSeparator = tokenSeparator;
	}

	// List<List<Encoder>>
	public List < List < Encoder >> encode(List < List < ControlFlowNode >> paths) {
		// i am using Java Streams here, you can also use the Java Collection API instead
		return paths.stream().map(list->list.stream().map(this::encode).flatMap(List::stream).collect(Collectors.toList())).collect(Collectors.toList());
	}

	List<Code> tokenMapper(Iterable<String> listOfString) {
		List<Code> result = new ArrayList<Code>();
		for (String s : listOfString) {	
			try { Double.valueOf(s); 
				try { Float.valueOf(s); 
					try { Long.valueOf(s); 
						try { Integer.valueOf(s); result.add(Code.INT); } catch (Exception e) {result.add(Code.INT);} } 
					catch (Exception e ) {result.add(Code.FLOAT);} }				 
				catch (Exception e) {result.add(Code.FLOAT);} }
			catch (Exception e) {
			 	if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false")) result.add(Code.BOOLEAN);
			 	
			 	else if (s.startsWith("++")) result.add(Code.PREINC);
				else if (s.endsWith("++")) result.add(Code.POSTINC);
				else if (s.startsWith("--")) result.add(Code.PREDEC);
				else if (s.endsWith("--")) result.add(Code.POSTDEC);
				else if (s.equals("+")) result.add(Code.PLUS);
				else if (s.equals("-")) result.add(Code.MINUS);
				else if (s.equals("[")) result.add(Code.LEFT_SB);
				else if (s.equals("]")) result.add(Code.RIGHT_SB);
				else if (s.equals("{")) result.add(Code.LEFT_CB);
				else if (s.equals("}")) result.add(Code.RIGHT_CB);
				else if (s.equals("(")) result.add(Code.LEFT_B);
				else if (s.equals(")")) result.add(Code.RIGHT_B);
				else if (s.equals(",")) result.add(Code.COMMA);
				else if (s.equals(";")) result.add(Code.SEM);
				else if (s.equals(":")) result.add(Code.COL);
				else if (s.equals(".")) result.add(Code.DOT);
				else if (s.equals("~")) result.add(Code.COMPL);
				else if (s.equals("#")) result.add(Code.HASH);
				else if (s.equals("'")) result.add(Code.SQM);
				else if (s.equals("&")) result.add(Code.BITAND);
				else if (s.equals("|")) result.add(Code.BITOR);
				else if (s.equals("*")) result.add(Code.MUL);
				else if (s.equals("/")) result.add(Code.DIV);
				else if (s.equals("%")) result.add(Code.MOD);
				else if (s.equals("^")) result.add(Code.BITXOR);
				else if (s.equals("!")) result.add(Code.NOT);
				else if (s.equals("=")) result.add(Code.ASSIGN);
				else if (s.equals("+=")) result.add(Code.AA);
				else if (s.equals("-=")) result.add(Code.SA);
				else if (s.equals("*=")) result.add(Code.MA);
				else if (s.equals("/=")) result.add(Code.DA);
				else if (s.equals("%=")) result.add(Code.MODA);
				else if (s.equals("==")) result.add(Code.EQ);
				else if (s.equals("!=")) result.add(Code.NE);
				else if (s.equals("<")) result.add(Code.LT);
				else if (s.equals(">")) result.add(Code.GT);
				else if (s.equals("<=")) result.add(Code.LE);
				else if (s.equals(">=")) result.add(Code.GE);
				else if (s.equals("<<")) result.add(Code.SL);
				else if (s.equals(">>")) result.add(Code.SR);
				else if (s.equals(">>>")) result.add(Code.USR);
				else if (s.equals("&&")) result.add(Code.AND);
				else if (s.equals("||")) result.add(Code.OR); 
				else if (s.equals("new")) result.add(Code.NEW);
				else if (s.equals("newarray")) result.add(Code.NEWARRAY);
				else if (s.equals("int")) result.add(Code.INT);
				else if (s.equals("float")) result.add(Code.FLOAT);
				else if (s.equals("long")) result.add(Code.FLOAT);
				else if (s.equals("double")) result.add(Code.FLOAT);
				else if (s.equals("string")) result.add(Code.STRING);
				else if (s.equals("boolean")) result.add(Code.BOOLEAN);
				else if (s.equals("return")) result.add(Code.RETURN);
				else if (s.equals("assign")) result.add(Code.ASSIGN);
				else if (s.equals("null")) result.add(Code.NULL);
			
			 	else result.add(Code.STRING);
			 } 			
			
		}
		return result;
	}

	List<Encoder> encode(ControlFlowNode node) {
		CompletePathEncoder dummy = new CompletePathEncoder(Code.UNKNOWN, new ArrayList<Code>());
		CtElement element = node.getStatement();
		if (element != null) {
			switch (SpoonCodeElement.map(element)) {
				case ArrayRead:
					tokenSeparator.scan(element);
					Iterable<String> operands = tokenSeparator.pop();
					List<Code> operandEncoding = tokenMapper(operands);
					CompletePathEncoder arrayReadEncoder = new CompletePathEncoder(Code.ARRAYREAD, operandEncoding);
					return new ArrayList<Encoder>(Arrays.asList(arrayReadEncoder));		 

				case ArrayWrite:
					tokenSeparator.scan(element);
					Iterable<String> operands1 = tokenSeparator.pop();
					List<Code> operandEncoding1 = tokenMapper(operands1);
					CompletePathEncoder arrayWriteEncoder = new CompletePathEncoder(Code.ARRAYWRITE, operandEncoding1);
					return new ArrayList<Encoder>(Arrays.asList(arrayWriteEncoder));  

				case Assert:
					tokenSeparator.scan(element);
					Iterable<String> operands2 = tokenSeparator.pop();
					List<Code> operandEncoding2 = tokenMapper(operands2);
					CompletePathEncoder assertEncoder = new CompletePathEncoder(Code.ASSERT, operandEncoding2);
					return new ArrayList<Encoder>(Arrays.asList(assertEncoder));   	

				case Assignment:
					tokenSeparator.scan(element);
					Iterable<String> operands3 = tokenSeparator.pop();
					List<Code> operandEncoding3 = tokenMapper(operands3);
					CompletePathEncoder assignmentEncoder = new CompletePathEncoder(Code.ASSIGN, operandEncoding3);
					return new ArrayList<Encoder>(Arrays.asList(assignmentEncoder));			

				case BinaryOperator:
					tokenSeparator.scan(element);
					Iterable<String> operands4 = tokenSeparator.pop();
					List<Code> operandEncoding4 = tokenMapper(operands4);
					CompletePathEncoder binaryOperatorEncoding = new CompletePathEncoder(Code.BINARY, operandEncoding4);
					return new ArrayList<Encoder>(Arrays.asList(binaryOperatorEncoding)); 

				case Break:
					tokenSeparator.scan(element);
					Iterable<String> operands5 = tokenSeparator.pop();
					List<Code> operandEncoding5 = tokenMapper(operands5);
					CompletePathEncoder breakEncoder = new CompletePathEncoder(Code.ASSIGN, operandEncoding5);
					return new ArrayList<Encoder>(Arrays.asList(breakEncoder));   

				case Conditional:
					tokenSeparator.scan(element);
					Iterable<String> operands6 = tokenSeparator.pop();
					List<Code> operandEncoding6 = tokenMapper(operands6);
					CompletePathEncoder conditionalEncoder = new CompletePathEncoder(Code.COND, operandEncoding6);
					return new ArrayList<Encoder>(Arrays.asList(conditionalEncoder));   						

				case ConstructorCall:
					tokenSeparator.scan(element);
					Iterable<String> operands7 = tokenSeparator.pop();
					List<Code> operandEncoding7 = tokenMapper(operands7);
					CompletePathEncoder constructorCallEncoder = new CompletePathEncoder(Code.NEW, operandEncoding7);
					return new ArrayList<Encoder>(Arrays.asList(constructorCallEncoder));   						

				case Continue:
					tokenSeparator.scan(element);
					Iterable<String> operands8 = tokenSeparator.pop();
					List<Code> operandEncoding8 = tokenMapper(operands8);
					CompletePathEncoder continueEncoder = new CompletePathEncoder(Code.CONTINUE, operandEncoding8);
					return new ArrayList<Encoder>(Arrays.asList(continueEncoder));   

				case FieldRead:
					tokenSeparator.scan(element);
					Iterable<String> operands9 = tokenSeparator.pop();
					List<Code> operandEncoding9 = tokenMapper(operands9);
					CompletePathEncoder fieldReadEncoder = new CompletePathEncoder(Code.FIELDREAD, operandEncoding9);
					return new ArrayList<Encoder>(Arrays.asList(fieldReadEncoder)); 

				case FieldWrite:
					tokenSeparator.scan(element);
					Iterable<String> operands10 = tokenSeparator.pop();
					List<Code> operandEncoding10 = tokenMapper(operands10);
					CompletePathEncoder fieldWriteEncoder = new CompletePathEncoder(Code.FIELDWRITE, operandEncoding10);
					return new ArrayList<Encoder>(Arrays.asList(fieldWriteEncoder)); 			

				case Invocation:
					tokenSeparator.scan(element);
					Iterable<String> operands11 = tokenSeparator.pop();
					List<Code> operandEncoding11 = tokenMapper(operands11);
					CompletePathEncoder invocationEncoder = new CompletePathEncoder(Code.CALL, operandEncoding11);
					return new ArrayList<Encoder>(Arrays.asList(invocationEncoder)); 	

				case Literal:
					tokenSeparator.scan(element);
					Iterable<String> operands12 = tokenSeparator.pop();
					List<Code> operandEncoding12 = tokenMapper(operands12);
					CompletePathEncoder literalEncoder = new CompletePathEncoder(Code.EXPR, operandEncoding12);
					return new ArrayList<Encoder>(Arrays.asList(literalEncoder)); 

				case LocalVariable:
					tokenSeparator.scan(element);
					Iterable<String> operands13 = tokenSeparator.pop();
					List<Code> operandEncoding13 = tokenMapper(operands13);
					CompletePathEncoder localVariableEncoder = new CompletePathEncoder(Code.VAR, operandEncoding13);
					return new ArrayList<Encoder>(Arrays.asList(localVariableEncoder));

				case NewArray:
					tokenSeparator.scan(element);
					Iterable<String> operands14 = tokenSeparator.pop();
					List<Code> operandEncoding14 = tokenMapper(operands14);
					CompletePathEncoder newArrayEncoder = new CompletePathEncoder(Code.NEWARRAY, operandEncoding14);
					return new ArrayList<Encoder>(Arrays.asList(newArrayEncoder));		
					
	            		case OperatorAssignment:
					tokenSeparator.scan(element);
					Iterable<String> operands15 = tokenSeparator.pop();
					List<Code> operandEncoding15 = tokenMapper(operands15);
					CompletePathEncoder OperatorAssignmentEncoder = new CompletePathEncoder(Code.OP_ASSIGN, operandEncoding15);
					return new ArrayList<Encoder>(Arrays.asList(OperatorAssignmentEncoder));

				case Return:
					tokenSeparator.scan(element);
					Iterable<String> operands16 = tokenSeparator.pop();
					List<Code> operandEncoding16 = tokenMapper(operands16);
					CompletePathEncoder returnEncoder = new CompletePathEncoder(Code.RETURN, operandEncoding16);
					return new ArrayList<Encoder>(Arrays.asList(returnEncoder));		

				case ThisAccess:
					tokenSeparator.scan(element);
					Iterable<String> operands17 = tokenSeparator.pop();
					List<Code> operandEncoding17 = tokenMapper(operands17);
					CompletePathEncoder thisAccessEncoder = new CompletePathEncoder(Code.THIS, operandEncoding17);
					return new ArrayList<Encoder>(Arrays.asList(thisAccessEncoder));

				case Throw:
					tokenSeparator.scan(element);
					Iterable<String> operands18 = tokenSeparator.pop();
					List<Code> operandEncoding18 = tokenMapper(operands18);
					CompletePathEncoder throwEncoder = new CompletePathEncoder(Code.THROW, operandEncoding18);
					return new ArrayList<Encoder>(Arrays.asList(throwEncoder));	

				case TypeAccess:
					tokenSeparator.scan(element);
					Iterable<String> operands19 = tokenSeparator.pop();
					List<Code> operandEncoding19 = tokenMapper(operands19);
					CompletePathEncoder typeAccessEncoder = new CompletePathEncoder(Code.TYPE, operandEncoding19);
					return new ArrayList<Encoder>(Arrays.asList(typeAccessEncoder));	

				case UnaryOperator:
					tokenSeparator.scan(element);
					Iterable<String> operands20 = tokenSeparator.pop();
					List<Code> operandEncoding20 = tokenMapper(operands20);
					CompletePathEncoder unaryOperatorEncoder = new CompletePathEncoder(Code.UNARY, operandEncoding20);
					return new ArrayList<Encoder>(Arrays.asList(unaryOperatorEncoder));		

				case VariableRead:
					tokenSeparator.scan(element);
					Iterable<String> operands21 = tokenSeparator.pop();
					List<Code> operandEncoding21 = tokenMapper(operands21);
					CompletePathEncoder variableReadEncoder = new CompletePathEncoder(Code.VAR, operandEncoding21);
					return new ArrayList<Encoder>(Arrays.asList(variableReadEncoder));					

				default:
					return new ArrayList<Encoder> (Arrays.asList(dummy));
			}
		} 
		return new ArrayList<Encoder> (Arrays.asList(dummy));
	} 
}

