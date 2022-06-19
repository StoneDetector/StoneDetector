package org.dlr.foobar.stringencoding;

import spoon.reflect.visitor.DefaultJavaPrettyPrinter;
import spoon.reflect.visitor.DefaultTokenWriter;
import spoon.reflect.visitor.PrinterHelper;
import spoon.compiler.Environment;
import java.util.ArrayList;

public class TokenSeparator extends DefaultJavaPrettyPrinter {

  private MyTokenWriter tokenWriter;

  public TokenSeparator(Environment env) {
    super(env);
    tokenWriter = new MyTokenWriter(new PrinterHelper(env));
    setPrinterTokenWriter(tokenWriter);
  }

  public Iterable<String> pop() {
    return tokenWriter.pop();
  }

}

class MyTokenWriter extends DefaultTokenWriter {

  ArrayList<String> tokenList = new ArrayList<String>();

  MyTokenWriter(PrinterHelper printerHelper) {
    super(printerHelper);
  }

  Iterable<String> pop() {
    ArrayList<String> list = (ArrayList<String>) tokenList.clone();
    tokenList.clear();
    return list;
  }

  @Override
  public DefaultTokenWriter writeSeparator(String token) {
    super.writeSeparator(token);
    tokenList.add(token);
    return this;
  }
  @Override
  public DefaultTokenWriter writeOperator(String token) {
    super.writeOperator(token);
    tokenList.add(token);
    return this;
  }
  @Override
  public DefaultTokenWriter writeLiteral(String token) {
    super.writeLiteral(token);
    tokenList.add(token);
    return this;
  }
  @Override
  public DefaultTokenWriter writeKeyword(String token) {
    super.writeKeyword(token);
    tokenList.add(token);
    return this;
  }
  @Override
  public DefaultTokenWriter writeIdentifier(String token) {
    super.writeIdentifier(token);
    tokenList.add(token);
    return this;
  }
  @Override
  public DefaultTokenWriter writeCodeSnippet(String token) {
    super.writeCodeSnippet(token);
    tokenList.add(token);
    return this;
  }

}
