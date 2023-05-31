package org.fsu.bytecode;

import com.ibm.wala.util.debug.Assertions;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.fsu.codeclones.Code;
import org.fsu.codeclones.Environment;
import soot.ResolutionFailedException;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.toolkits.graph.DominatorNode;

import java.util.*;

public class StandardEncoderRegisterCode {
    //static final Object monitorEncoder=new Object();
    static Map<Class,Integer> statementMap = new HashMap<>();
    static Map<Class,Integer> assignStatementMap = new HashMap<>();
    static {
        statementMap.put(JIdentityStmt.class,1);
        statementMap.put(JAssignStmt.class,2);
        statementMap.put(JInvokeStmt.class,3);
        statementMap.put(JReturnStmt.class,4);
        statementMap.put(JReturnVoidStmt.class,5);
        statementMap.put(JGotoStmt.class,6);
        statementMap.put(JThrowStmt.class,7);
        statementMap.put(JTableSwitchStmt.class,8);
        statementMap.put(JLookupSwitchStmt.class,8);
        statementMap.put(JIfStmt.class,9);
        statementMap.put(JEnterMonitorStmt.class,10);
        statementMap.put(JExitMonitorStmt.class,10);

        /*
        invoke Interface: class is part of an Interface. The entire Methode table has to be searched
        invoke Virtual: Static class > every Method has a static place there the bytecode is stored > Optimization possible
         */
        assignStatementMap.put(JArrayRef.class,1);
        assignStatementMap.put(JInstanceFieldRef.class,2);
        assignStatementMap.put(JimpleLocal.class,3);
        assignStatementMap.put(JVirtualInvokeExpr.class,4);
        assignStatementMap.put(JInterfaceInvokeExpr.class,5);
        assignStatementMap.put(JStaticInvokeExpr.class,6);
        assignStatementMap.put(JSpecialInvokeExpr.class,7);
        assignStatementMap.put(JNewExpr.class,8);
        assignStatementMap.put(JNewArrayExpr.class,9);
        assignStatementMap.put(NullConstant.class,10);
        assignStatementMap.put(JSubExpr.class,11);
        assignStatementMap.put(JAddExpr.class,12);
        assignStatementMap.put(JRemExpr.class,13);
        assignStatementMap.put(IntConstant.class,14);
        assignStatementMap.put(StringConstant.class,15);
        assignStatementMap.put(JAndExpr.class,16);
        assignStatementMap.put(JDivExpr.class,17);
        assignStatementMap.put(JMulExpr.class,18);
        assignStatementMap.put(JLengthExpr.class,19);
        assignStatementMap.put(JNewMultiArrayExpr.class,20);
        assignStatementMap.put(JInstanceOfExpr.class,21);
        assignStatementMap.put(JShlExpr.class,22);
        assignStatementMap.put(JShrExpr.class,23);
        assignStatementMap.put(JUshrExpr.class,24);
        assignStatementMap.put(JOrExpr.class,25);
        assignStatementMap.put(JCastExpr.class,26);
        assignStatementMap.put(JCmplExpr.class,27);
        assignStatementMap.put(StaticFieldRef.class,28);
        assignStatementMap.put(JCmpExpr.class,29);
        assignStatementMap.put(JNegExpr.class,30);
        assignStatementMap.put(JXorExpr.class,31);
        assignStatementMap.put(JCmpgExpr.class,32);
        assignStatementMap.put(DoubleConstant.class,33);
        assignStatementMap.put(FloatConstant.class,34);
        assignStatementMap.put(LongConstant.class,35);
        assignStatementMap.put(ClassConstant.class,36);
    }


    public static Code[] encodeUnit(Unit unit, FileBasedConfiguration config, List<String> functionNames)
    {
        switch (statementMap.get(unit.getClass())){
            case 1: return identityNode((IdentityStmt) unit,config);
            case 2: return assignNode((AssignStmt) unit,config,functionNames);
            case 3: return invokeNode((InvokeStmt) unit,config,functionNames);
            case 4: case 5: return returnNode(unit,config);
            case 6: return gotoNode((GotoStmt) unit,config);
            case 7: return throwNode((ThrowStmt) unit,config);
            case 8: return switchNode((SwitchStmt) unit,config);
            case 9: return ifNode((IfStmt) unit,config);
            case 10:return monitorNode((MonitorStmt) unit,config);
            default: Assertions.UNREACHABLE();
        }
        return null;
    }

    private static Code[] returnNode(Unit unit, FileBasedConfiguration config)
    {
        List<Code> codes = new ArrayList<>();
        codes.add(Code.RETURN);

        ReturnStmt ret;
        if (unit instanceof ReturnStmt) {
            ret = (ReturnStmt) unit;
            //return "return(" + ret.getOpBox().getValue().toString() + ")";
        }
        return codes.toArray(new Code[0]);
    }

    private static Code[] identityNode(IdentityStmt iStmnt, FileBasedConfiguration config)
    {
        /*
        this, exceptions, parameters (Assignment) f.e.:
        $r46 := @caughtexception,
        r0 := @this: _PrimeFactors.PrimeFactors
        r20 := @parameter0: java.lang.Object
         */
        List<Code> codes = new ArrayList<>();
        // only add Identity if it is not a Parameter since there is another flag for this
        // if it is a parameter we only add it if the parameterNodes flag is set to false
        if (config.getBoolean("atNodes") &&
                (!iStmnt.toString().contains("@parameter") || !config.getBoolean("parameterNodes"))){
            codes.add(Code.IDENTITY);
        }
        return codes.toArray(new Code[0]);
    }

    private static Code[] assignNode(AssignStmt aStmnt, FileBasedConfiguration config,List<String> functionNames)
    {
        /*
        Nodes with Assignment '=' (not identities)
         */
        List<Code> codes = new ArrayList<>();
        codes.add(Code.ASSIGN);

        int minCodesSize = 2;
        Value v = aStmnt.getLeftOp();
        if (v instanceof JArrayRef) {
            codes.add(Code.ARRAYWRITE);
            minCodesSize += 1;
        }
        // in some cases we have to get the left operation instead
        int getCurrentClass = assignStatementMap.get(aStmnt.getRightOp().getClass());
        Value assignStmt = aStmnt.getRightOp();
        // TODO
        if (config.getBoolean("checkRightAssignNode") && getCurrentClass == 3){
            getCurrentClass = assignStatementMap.get(aStmnt.getLeftOp().getClass());
            assignStmt = aStmnt.getLeftOp();
            if (getCurrentClass == 3) { return null; }
        }
        // If there is a function call/definition add it to the functionNames variable
        switch (getCurrentClass) {
            case 4: case 5: case 6: case 7: funcName((InvokeExpr) aStmnt.getRightOp(), functionNames, config);
        }
        switch (getCurrentClass) {
            case 1: codes.add(Code.ARRAYREAD); break;
            case 2: if (config.getBoolean("fieldread")) codes.add(Code.FIELDREAD); break; // TODO
            case 3: codes.add(Code.ASSIGN); break;
            case 4: codes.add(Code.VIRTUALCALL); codes.addAll(virtualInvoke((JVirtualInvokeExpr) assignStmt, config)); if (codes.size()==2) return null; break;
            case 5: codes.add(Code.INTERFACECALL); codes.addAll(interfaceInvoke((JInterfaceInvokeExpr) assignStmt,config)); break;
            case 6: codes.addAll(staticInvoke((JStaticInvokeExpr) assignStmt,config)); break;
            case 7: codes.add(Code.SPECIALCALL); codes.addAll(specialInvoke((JSpecialInvokeExpr) assignStmt,config)); if (codes.size()==2) return null; break;
            case 8: if (config.getBoolean("newCode")) codes.add(Code.NEW); break;
            case 9: codes.add(Code.NEWARRAY); break;
            case 10: codes.add(Code.NULL); break;
            case 11: codes.add(Code.MINUS); break;
            case 12: codes.add(Code.PLUS); break;
            case 13: codes.add(Code.MOD); break;
            case 14: if (config.getBoolean("constants")) codes.add(Code.INT); break;
            case 15: if (config.getBoolean("constants")) codes.add(Code.STRING); break;
            case 33: case 34: if (config.getBoolean("constants")) codes.add(Code.FLOAT); break;
            case 35: if (config.getBoolean("constants")) codes.add(Code.LONG); break;
            case 36: if (config.getBoolean("constants")) codes.add(Code.CLASS); break;
            case 16: codes.add(Code.AND); break;
            case 17: codes.add(Code.DIV); break;
            case 18: codes.add(Code.MUL); break;
            case 19: codes.add(Code.LENGTHOF); break;
            case 20: {
                int dim =((JNewMultiArrayExpr)aStmnt.getRightOp()).getBaseType().numDimensions;
                for (int i = 0;i<dim;i++)
                    codes.add(Code.NEWARRAY);
                break;
            }
            case 21: codes.add(Code.INSTANCEOF); break;
            case 22: codes.add(Code.SHIFTL); break;
            case 23: case 24: codes.add(Code.SHIFTR); break;
            case 25: codes.add(Code.OR); break;
            case 26: codes.add(Code.CAST); break;
            case 27: codes.add(Code.COMPL); break;
            case 28: codes.add(Code.STATICFIELD); break;
            case 29: codes.add(Code.COMP); break;
            case 30: codes.add(Code.NEG); break;
            case 31: codes.add(Code.BITXOR); break;
            case 32: codes.add(Code.COMPG); break;
            default:{
                break;
            }
        }

        if (codes.size() < minCodesSize)
            return null;
        return codes.toArray(new Code[0]);
    }

    private static void funcName(InvokeExpr invokeExpr, List<String> functionNames, FileBasedConfiguration config) {
        if (Environment.SUPPORTCALLNAMES) {
            String name=invokeExpr.getMethod().getName();
            if (Environment.STUBBERPROCESSING && name.contains("_")) {
                name=name.substring(0,name.lastIndexOf("_"));
            }
            functionNames.add(name);
        }
    }

    private static List<Code> fieldreadInvoke(JInstanceFieldRef fieldRef, FileBasedConfiguration config){
        List<Code> codes= new ArrayList<>();
        codes.add(Code.FIELDREAD);
        return codes;
    }

    private static List<Code> virtualInvoke(JVirtualInvokeExpr jVirtualInvokeExpr, FileBasedConfiguration config) {
        /*
        returns either an empty List or the amount of parameters needed for function call

        can be a StringBuilder or IntegerBuilder among other things
         */
        List<Code> codes= new ArrayList<>();

        if (!config.getBoolean("stringBuilder")) {
            if (jVirtualInvokeExpr.toString().contains("java.lang.StringBuilder append(") || jVirtualInvokeExpr.toString().contains("java.lang.StringBuilder: java.lang.String toString(")) {
                return codes;
            }
        }
        try {
            int paramCount = (jVirtualInvokeExpr).getMethod().getParameterCount();

            if (paramCount == 0) codes.add(Code.NULL);
            else if (paramCount == 1) codes.add(Code.ONE);
            else if (paramCount == 2) codes.add(Code.TWO);
            else if (paramCount == 3) codes.add(Code.THREE);
            else if (paramCount == 4) codes.add(Code.FOUR);
            else if (paramCount == 5) codes.add(Code.FIVE);
            else if (paramCount == 6) codes.add(Code.SIX);
            else if (paramCount == 7) codes.add(Code.SEVEN);
            else if (paramCount == 8) codes.add(Code.EIGHT);
            else if (paramCount == 9) codes.add(Code.NINE);
            else if (paramCount == 10) codes.add(Code.TEN);
            else codes.add(Code.INT);
        }
        catch (ResolutionFailedException ex) {}
        return codes;
    }

    public static Code[] encodeParameterCount(int paramCount) {
        /*
        gets an Integer with the count of parameters and return the Code
         */
        List<Code> codes= new ArrayList<>();
        if (paramCount == 0) codes.add(Code.NULL);
        else if (paramCount == 1) codes.add(Code.ONE);
        else if (paramCount == 2) codes.add(Code.TWO);
        else if (paramCount == 3) codes.add(Code.THREE);
        else if (paramCount == 4) codes.add(Code.FOUR);
        else if (paramCount == 5) codes.add(Code.FIVE);
        else if (paramCount == 6) codes.add(Code.SIX);
        else if (paramCount == 7) codes.add(Code.SEVEN);
        else if (paramCount == 8) codes.add(Code.EIGHT);
        else if (paramCount == 9) codes.add(Code.NINE);
        else if (paramCount == 10) codes.add(Code.TEN);
        else codes.add(Code.INT);
        return codes.toArray(new Code[0]);
    }

    public static Code[] encodeParameterUnits(int paramCount) {
        /*
        gets an Integer with the count of parameters and return the Code
         */
        List<Code> codes= new ArrayList<>();
        for (int i = 0; i < paramCount; i++){
            codes.add(Code.IDENTITY);
        }
        return codes.toArray(new Code[0]);
    }


    private static List<Code> interfaceInvoke(JInterfaceInvokeExpr jInterfaceInvokeExpr, FileBasedConfiguration config)
    {
        /*
        returns the amount of parameters needed for function call
         */
        List<Code> codes= new ArrayList<>();
        try {
            int paramCount = (jInterfaceInvokeExpr).getMethod().getParameterCount();

            if (paramCount == 0) codes.add(Code.NULL);
            else if (paramCount == 1) codes.add(Code.ONE);
            else if (paramCount == 2) codes.add(Code.TWO);
            else if (paramCount == 3) codes.add(Code.THREE);
            else if (paramCount == 4) codes.add(Code.FOUR);
            else if (paramCount == 5) codes.add(Code.FIVE);
            else if (paramCount == 6) codes.add(Code.SIX);
            else if (paramCount == 7) codes.add(Code.SEVEN);
            else if (paramCount == 8) codes.add(Code.EIGHT);
            else if (paramCount == 9) codes.add(Code.NINE);
            else if (paramCount == 10) codes.add(Code.TEN);
            else codes.add(Code.INT);
        }
        catch (ResolutionFailedException ex) {}
        return codes;
    }


    private static List<Code> staticInvoke(JStaticInvokeExpr jStaticInvokeExpr, FileBasedConfiguration config)
    {
        /*
        invoke static method > can't change
        returns the amount of parameters needed for function call
         */
        List<Code> codes = new ArrayList<>();
        try {
            if (!config.getBoolean("staticCall")){
                return codes;
            }
            codes.add(Code.STATICCALL);

            int paramCount = jStaticInvokeExpr.getMethod().getParameterCount();

            if (paramCount == 0) codes.add(Code.NULL);
            else if (paramCount == 1) codes.add(Code.ONE);
            else if (paramCount == 2) codes.add(Code.TWO);
            else if (paramCount == 3) codes.add(Code.THREE);
            else if (paramCount == 4) codes.add(Code.FOUR);
            else if (paramCount == 5) codes.add(Code.FIVE);
            else if (paramCount == 6) codes.add(Code.SIX);
            else if (paramCount == 7) codes.add(Code.SEVEN);
            else if (paramCount == 8) codes.add(Code.EIGHT);
            else if (paramCount == 9) codes.add(Code.NINE);
            else if (paramCount == 10) codes.add(Code.TEN);
            else codes.add(Code.INT);
        }
        catch (ResolutionFailedException ex) {}
        return codes;
    }
    private static List<Code> specialInvoke(JSpecialInvokeExpr jSpecialInvokeExpr, FileBasedConfiguration config)
    {
        List<Code> codes= new ArrayList<>();
        if (!config.getBoolean("specialInvoke")) {
            if (jSpecialInvokeExpr.toString().contains("<init>")) {
                return codes;
            }
        }
        int paramCount = jSpecialInvokeExpr.getMethod().getParameterCount();

        if (paramCount == 0) codes.add(Code.NULL);
        else if (paramCount == 1) codes.add(Code.ONE);
        else if (paramCount == 2) codes.add(Code.TWO);
        else if (paramCount == 3) codes.add(Code.THREE);
        else if (paramCount == 4) codes.add(Code.FOUR);
        else if (paramCount == 5) codes.add(Code.FIVE);
        else if (paramCount == 6) codes.add(Code.SIX);
        else if (paramCount == 7) codes.add(Code.SEVEN);
        else if (paramCount == 8) codes.add(Code.EIGHT);
        else if (paramCount == 9) codes.add(Code.NINE);
        else if (paramCount == 10) codes.add(Code.TEN);
        else codes.add(Code.INT);
        return codes;
    }

    private static Code[] invokeNode(InvokeStmt iStmnt, FileBasedConfiguration config,List<String> functionNames)
    {
        // check if it is a String builder
        if (!config.getBoolean("stringBuilder") && iStmnt.toString().contains("java.lang.StringBuilder")){
            return new Code[]{Code.ASSIGN, Code.STRING};
        }

        funcName(iStmnt.getInvokeExpr(), functionNames, config);
        List<Code> codes = new ArrayList<>();
        if (iStmnt instanceof JInvokeStmt) {
            if (iStmnt.getInvokeExpr() instanceof JSpecialInvokeExpr) {
                codes.add(Code.SPECIALCALL);
                codes.addAll(specialInvoke((JSpecialInvokeExpr) iStmnt.getInvokeExpr(),config));
		        if (codes.size()==1) { return null; }
            }
            else if (iStmnt.getInvokeExpr() instanceof JVirtualInvokeExpr) {
                codes.add(Code.SPECIALCALL);
                codes.addAll(virtualInvoke((JVirtualInvokeExpr) iStmnt.getInvokeExpr(),config));
                if (codes.size()==1) {return null; }
            }
            else if (iStmnt.getInvokeExpr() instanceof JStaticInvokeExpr) {
                codes.add(Code.SPECIALCALL);
                codes.addAll(staticInvoke((JStaticInvokeExpr) iStmnt.getInvokeExpr(),config));
                if (codes.size()==1) { return null; }
            }
            else if (iStmnt.getInvokeExpr() instanceof JInterfaceInvokeExpr) {
                codes.add(Code.SPECIALCALL);
                codes.addAll(interfaceInvoke((JInterfaceInvokeExpr) iStmnt.getInvokeExpr(),config));
                if (codes.size()==1) { return null; }
            }
        }
        return codes.toArray(new Code[0]);
    }

    private static Code[] ifNode(IfStmt iStmnt, FileBasedConfiguration config)
    {
        List<Code> codes = new ArrayList<>();
        codes.add(Code.COND);
        Value v=iStmnt.getCondition();

        // TODO
        if (!config.getBoolean("ifNodeCompareOperator")){
            codes.add(Code.COMP);
            return codes.toArray(new Code[0]);
        }

        if (v.toString().contains("==")) codes.add(Code.EQ);
        else if (v.toString().contains("<=")) codes.add(Code.LE);
        else if (v.toString().contains(">=")) codes.add(Code.GE);
        else if (v.toString().contains(">")) codes.add(Code.GT);
        else if (v.toString().contains("<")) codes.add(Code.LT);
        else if (v.toString().contains("!=")) codes.add(Code.NE);
        return codes.toArray(new Code[0]);
    }

    private static Code[] gotoNode(GotoStmt gotoStmnt, FileBasedConfiguration config)
    {
        List<Code> codes = new ArrayList<>();
        codes.add(Code.GOTO);
        return codes.toArray(new Code[0]);
    }

    private static Code[] throwNode(ThrowStmt throwStmnt, FileBasedConfiguration config)
    {
        List<Code> codes = new ArrayList<>();
        codes.add(Code.THROW);
        return codes.toArray(new Code[0]);
    }

    private static Code[] switchNode(SwitchStmt switchStmnt, FileBasedConfiguration config)
    {
        List<Code> codes = new ArrayList<>();
        codes.add(Code.SWITCH);
        return codes.toArray(new Code[0]);
    }

    private static Code[] monitorNode(MonitorStmt monitorStmt, FileBasedConfiguration config)
    {
        List<Code> codes = new ArrayList<>();
        codes.add(Code.MONITOR);
        return codes.toArray(new Code[0]);
    }

    public static Code[] encodeVirtualUnits(List<Unit> virtualNodes, FileBasedConfiguration config, List<String> functionNames) {
        List<Code> codes= new ArrayList<>();
        for (Unit unit : virtualNodes) {
            Class k = unit.getClass();
            // TODO
            if (statementMap.get(k) == null) { String s = ""; }
            else if (statementMap.get(k) == 2) {
                // search for string Builder beginning
                if (!config.getBoolean("stringBuilder") && unit.toString().contains("java.lang.String toString()")){
                    codes = new ArrayList<>();
                    functionNames = new ArrayList<>();
                }
                else {
                    // Assignment
                    Code[] code = assignNode((AssignStmt) unit, config, functionNames);
                    // ignore Code.ASSIGN
                    if (code!=null && code.length>=2) { codes.add(code[1]); }
                }
            } else {
                // TODO
                if (!unit.toString().contains("@caughtexception")){
                    System.out.println("UNIT: " + unit);
                    Assertions.UNREACHABLE();
                }
            }
        }
        if (functionNames.contains("<init>")){
            codes.add(0, Code.ASSIGN);
        }
        return codes.toArray(new Code[0]);
    }
}
