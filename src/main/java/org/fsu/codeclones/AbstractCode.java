/**
 * SPDX-License-Identifier: (MIT OR CECILL-C)
 *
 * Copyright (C) 2006-2019 INRIA and contributors
 *
 * Spoon is available either under the terms of the MIT License (see LICENSE-MIT.txt) of the Cecill-C License (see LICENSE-CECILL-C.txt). You as the user are entitled to choose the terms under which to adopt Spoon.
 */

package org.fsu.codeclones;




/**
 * This enumeration defines all the kinds of binary operators.
 */
public enum AbstractCode {
    	/**
	 * assert statement.
	 */
	ASSERT,
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
	 *  conditinal statement
	 */
	COND,
	/**
	 * break statement
	 */
	BREAK, //
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
	 * int, float nd boolean constant
	 */
	//NUMBER, 
	/**
	 * boolean constant
	 */
	//BOOLEAN,
	/**
	 * string and char constant
	 */
	//CHAR,
	/**
	 * Logical or
	 */
	OR, // ||
	/**
	 * Logical and.
	 */
	AND, // &&
	/**
	 * Bit operation.
	 */
	BIT, // |, ^, &, ~
	/**
	 * Equality.
	 */
	EQNE, // ==, !=
	/**
	 * Lower and lower comparison.
	 */
	GLTE, // <, >, <=,>=
	
	/**
	 * Shift operator
	 */
	SHIFT, 
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
	 * Logical inversion.
	 */
	NOT, // !
	/**
	 * Incrementation assignment.
	 */
	//INCDEC, // Assign and +,-
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
	 * type access
	 */
	TYPEACCESS,
	/**
	 * this operator
	 */
	THIS,
	CATCH,
	TRY,
	FINALLY
}
