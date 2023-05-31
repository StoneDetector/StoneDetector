package org.fsu.codeclones;

import java.util.HashSet;
import java.util.Set;

/**
 * This enumeration defines all the kinds of binary operators.
 */
public enum Code {

    	/**
	 * assignment statement.
	 */
	ASSIGN,  
	/**
	 * return statement
	 */
	RETURN,
	/**
	 * call statement
	 */
	CALL,
	/**
	 *  expression
	 */
	EXPR,
	/**
	 * unary statement
	 */
	UNARY,
	/**
	 * binary expression
	 */
	BINARY,
	/**
	 * condition expression
	 */
	COND,
	/**
	 * break statement
	 */
	BREAK, //
	/**
	 * assert statement
	 */
	ASSERT, //
	/**
	 * continue statement
	 */
	CONTINUE, //
	/**
	 * throw statement
	 */
	THROW, //
	/**
	 * implicit var definition
	 */
	VAR,
	
	/* kind of subexpression => 
	 *  assigned to opKind
	 **************************/
	/**
	 * call statement, has been already defined 
	 */
	//CALL,
	/**
	 * var use, has been already defined above
	 */
	//VAR,
	
	/**
	 * new operator
	 */
	NEW, //
	/**
	 * new array operator
	 */
	NEWARRAY, //
	 /**
	 * null operand
	 */
	NULL, //
	/**
	 * this operand
	 */
	THIS, //
	/**
	 * type access
	 */
	TYPE, //
	/**
	 * int constant
	 */
	INT, 
	/**
	 * float constant
	 */
	FLOAT,
	/**
	 * boolean constant
	 */
	BOOLEAN,
	/**
	 * string constant
	 */
	STRING,
	/**
	 * char constant
	 */
	CHAR,
	/**
	 * Logical or
	 */
	OR, // ||
	/**
	 * Logical and.
	 */
	AND, // &&
	/**
	 * Bit to bit or.
	 */
	BITOR, // |
	/**
	 * Bit to bit xor.
	 */
	BITXOR, // ^
	/**
	 * Bit to bit and.
	 */
	BITAND, // &
	/**
	 * Equality.
	 */
	EQ, // ==
	/**
	 * Inequality.
	 */
	NE, // !=
	/**
	 * Lower than comparison.
	 */
	LT, // <
	/**
	 * Greater than comparison.
	 */
	GT, // >
	/**
	 * Lower or equal comparison.
	 */
	LE, // <=
	/**
	 * Greater or equal comparison.
	 */
	GE, // >=
	/**
	 * Shift left.
	 */
	SL, // <<
	/**
	 * Shift right.
	 */
	SR, // >>
	/**
	 * Unsigned shift right.
	 */
	USR, // >>>
	/**
	 * Addition.
	 */
	PLUS, // +
	/**
	 * Substraction.
	 */
	MINUS, // -
	/**
	 * Multiplication.
	 */
	MUL, // *
	/**
	 * Division.
	 */
	DIV, // /
	/**
	 * Modulo.
	 */
	MOD, // %

	/**
	 * Instanceof (OO specific).
	 */
	INSTANCEOF, // instanceof
	/**
	 * Positivation.
	 */
	POS, // +
	/**
	 * Negation.
	 */
	NEG, // -
	/**
	 * Logical inversion.
	 */
	NOT, // !
		/**
	 * Binary complement.
	 */
	COMPL, // ~
	
	/**
	 * Incrementation pre assignment.
	 */
	PREINC, // ++ _
	/**
	 * Decrementation pre assignment.
	 */
	PREDEC, // -- _
	/**
	 * Incrementation post assignment.
	 */
	POSTINC, // _ ++
	/**
	 * Decrementation post assignment.
	 */
	POSTDEC, // _ --
	
	/**
	 * read access to a field.
	 */
	FIELDREAD,
	/**
	 * write access to a field.
	 */
	FIELDWRITE,
	/**
	 * read access to an array
	 */
	ARRAYREAD,
	/**
	 * write access to a field
	 */
	ARRAYWRITE,

	/**
	 * constant numbers.
	 */
	VOID,
	ONE,
	TWO,
	THREE,
	FOUR,
	FIVE,
	SIX,
	SEVEN,
	EIGHT,
	NINE,
	TEN,
	 /**
	 * unkown element
	 ***/
	UNKNOWN,
	/**
	 * literal
	 ****/
	LIT,

	//Folgendes für StringEncoding eingefuegt:
	
	OP_ASSIGN, 
	/**
	OperatorAssignment
	*/
	
	RIGHT_SB, // ]
	/**
	 * right square bracket
	 */
	LEFT_SB, // [
	/**
	 * left square bracket
	 */
	
	RIGHT_CB, // }
	/**
	 * right curly bracket
	 */
	
	LEFT_CB, // {
	/**
	 * left curly bracket
	 */
	
	LEFT_B, // (
	/**
	 * left bracket
	 */
	 
	RIGHT_B, // )
	 /**
	 * right bracket
	 */
	COMMA, // ,
	 /**
	 * comma
	 */
	SEM, // ;
	  /**
	 * semicolon
	 */
	COL, // :
	  /**
	 * colon
	 */
	DOT, // .
	  /**
	 * dot
	 */
	HASH, // #
	 /**
	 * hash
	 */
	SQM, // '
	  /**
	 * single quotation mark
	 */
	AA, // +=
	  /**
	 * addition assignment
	 */
	SA, // -=
	  /**
	 * substraction assignment
	 */
	MA, // *=
	  /**
	 * multiplication assignment 
	 */
	DA, // /=
	  /**
	 * devision assignment
	 */
	MODA, // %=
	  /**
	 * modulo assignment
	 */
	CATCH,
	FINALLY,
	TRY,
	CLASSDEFINITION,
	HASHVALUE,

	GOTO,
	SWITCH,
	MONITOR,
	SPECIALCALL,
	STATICCALL,
	VIRTUALCALL,
	LENGTHOF,
	SHIFTL,
	SHIFTR,
	CAST,
	STATICFIELD,
	COMP,
	COMPG,
	LONG,
	INTERFACECALL,
	CLASS,
	IDENTITY,
	LOAD,
	STORE,
	FIELDGET,
	FIELDPUT,
	STATICGET,
	POP,
	DUP,
	PUSH,
	EXITMONITOR,
	INSTANCECAST,
	CMPG,
	CMPL,
	CMP
}
