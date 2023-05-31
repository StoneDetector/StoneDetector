package org.fsu.bytecode;

import com.ibm.wala.util.debug.Assertions;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.eclipse.jdt.internal.compiler.impl.BooleanConstant;
import org.fsu.codeclones.Code;
import org.fsu.codeclones.Environment;
import soot.ResolutionFailedException;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.baf.MethodArgInst;
import soot.baf.internal.*;
import soot.jimple.*;
import soot.jimple.internal.*;

import java.util.*;

public class StandardEncoderByteCode {
    //static final Object monitorEncoder=new Object();
    static Map<Class,Integer> statementMap = new HashMap<>();
    static Map<Class,Integer> assignStatementMap = new HashMap<>();
    static {
        statementMap.put(BIdentityInst.class,1);

        statementMap.put(BLoadInst.class,2);

        statementMap.put(BFieldGetInst.class,3);
        statementMap.put(BVirtualInvokeInst.class,4);
        statementMap.put(BPopInst.class,5);
        statementMap.put(BIfEqInst.class,6);
        statementMap.put(BIfCmpNeInst.class,7);
        statementMap.put(BNewInst.class,8);
        statementMap.put(BDup1Inst.class,9);
        statementMap.put(BSpecialInvokeInst.class,10);
        statementMap.put(BReturnInst.class,11);
        statementMap.put(BInstanceOfInst.class,12);
        statementMap.put(BGotoInst.class,13);
        statementMap.put(BPushInst.class,14);
        statementMap.put(BStoreInst.class,15);
        statementMap.put(BIfNullInst.class,16);
        statementMap.put(BReturnVoidInst.class,17);
        statementMap.put(BIfNeInst.class,18);
        statementMap.put(BFieldPutInst.class,19);
        statementMap.put(BIfNonNullInst.class,20);
        statementMap.put(BStaticInvokeInst.class,21);
        statementMap.put(BNewArrayInst.class,22);
        statementMap.put(BArrayWriteInst.class,23);
        statementMap.put(BIfCmpEqInst.class,24);
        statementMap.put(BIfCmpGeInst.class,25);
        statementMap.put(BArrayReadInst.class,26);
        statementMap.put(BAddInst.class,27);
        statementMap.put(BMulInst.class,28);
        statementMap.put(BSubInst.class,29);
        statementMap.put(BDivInst.class,30);
        statementMap.put(BIfGeInst.class,31);
        statementMap.put(BIfGtInst.class,32);
        statementMap.put(BNewMultiArrayInst.class,33);
        statementMap.put(BIfCmpLtInst.class,34);
        statementMap.put(BRemInst.class,35);
        statementMap.put(BIfCmpLeInst.class,36);
        statementMap.put(BArrayLengthInst.class,37);
        statementMap.put(BIncInst.class,38);
        statementMap.put(BIfLeInst.class,39);
        statementMap.put(BOrInst.class,40);
        statementMap.put(BIfCmpGtInst.class,41);
        statementMap.put(BAndInst.class,42);
        statementMap.put(BShlInst.class,43);
        statementMap.put(BThrowInst.class,44);
        statementMap.put(BNegInst.class,45);
        statementMap.put(BLookupSwitchInst.class,46);
        statementMap.put(BStaticGetInst.class,47);
        statementMap.put(BXorInst.class,48);
        statementMap.put(BPrimitiveCastInst.class,49);
        statementMap.put(BEnterMonitorInst.class,50);
        statementMap.put(BExitMonitorInst.class,51);
        statementMap.put(BIfLtInst.class,52);
        statementMap.put(BInterfaceInvokeInst.class,53);
        statementMap.put(BTableSwitchInst.class,54);
        statementMap.put(BCmpgInst.class,55);
        statementMap.put(BCmplInst.class,56);
        statementMap.put(BCmpInst.class,57);
        statementMap.put(BShrInst.class,58);
        statementMap.put(BInstanceCastInst.class,59);
    }

    public static Code[] encodeUnit(Unit unit, FileBasedConfiguration config,List<String> functionNames)
    {
        Code[] codes = null;
	    try{
            switch (statementMap.get(unit.getClass())){
                case 1: if (config.getBoolean("parameterNodes")) codes= new Code[]{Code.IDENTITY}; break; //Identity
                case 2: if (config.getBoolean("LoadAndInit")) codes= new Code[]{Code.LOAD}; break;//if (config.getBoolean("LoadStore")) codes= new Code[]{Code.LOAD}; break; // Load
                case 15: if (config.getBoolean("Store")) codes= new Code[]{Code.STORE}; break; //Store
                case 3: if (config.getBoolean("FieldGetPutStaticGet")) if (config.getBoolean("fieldread")) codes= new Code[]{Code.FIELDGET}; break;// FieldGet
                case 19: if (config.getBoolean("FieldGetPutStaticGet")) codes= new Code[]{Code.FIELDPUT}; break;// FieldPut
                case 47: if (config.getBoolean("FieldGetPutStaticGet")) codes= new Code[]{Code.STATICGET}; break; // StaticGet
                case 5: if (config.getBoolean("PushPopDup")) codes= new Code[]{Code.POP}; break;// Pop
                case 9: if (config.getBoolean("PushPopDup")) codes= new Code[]{Code.DUP}; break;// DUP1
                case 14: if (config.getBoolean("PushPopDup")){
                    if (config.getBoolean("constants")) {
                        Constant constant = ((BPushInst) unit).getConstant();
                        codes = new Code[2];
                        codes[0] = Code.PUSH;
                        if (constant instanceof IntConstant)
                            codes[1] = Code.INT;
                        else if (constant instanceof StringConstant)
                            codes[1] = Code.STRING;
                        else if (constant instanceof NullConstant)
                            codes[1] = Code.NULL;
                        else if (constant instanceof FloatConstant || constant instanceof DoubleConstant)
                            codes[1] = Code.FLOAT;
                        else if (constant instanceof LongConstant)
                            codes[1] = Code.LONG;
                        else {
                            codes[1] = Code.PUSH;
                        }
                    }
                    else
                    {
                        codes= new Code[]{Code.PUSH};
                    }
                    break;
                } // Push
                case 49: if (config.getBoolean("InstanceOfCast")) codes= new Code[]{Code.CAST}; break; // PrimitiveCast

                case 12: if (config.getBoolean("InstanceOfCast")) codes= new Code[]{Code.INSTANCEOF}; break;// INSTANCEOF
                case 59: if (config.getBoolean("InstanceOfCast")) codes= new Code[]{Code.INSTANCECAST}; break; //InstaneCast
                case 51: if (config.getBoolean("Compare")) codes= new Code[]{Code.EXITMONITOR}; break; // ExitMonitor
                case 55: if (config.getBoolean("Compare")) codes= new Code[]{Code.CMPG}; break;// Cmpg
                case 56: if (config.getBoolean("Compare")) codes= new Code[]{Code.CMPL}; break;// CmpL
                case 57: if (config.getBoolean("Compare")) codes= new Code[]{Code.CMP}; break;// Cmp

                case 4: case 10: case 21: case 53: return invokeNode((MethodArgInst)unit, config,functionNames);
                case 8: return new Code[]{Code.NEW};
                case 11: case 17: return new Code[]{Code.RETURN};
                case 13: return new Code[]{Code.GOTO};
                case 22: case 33: return new Code[]{Code.NEWARRAY};
                case 23: return new Code[]{Code.ARRAYWRITE};
                case 26: return new Code[]{Code.ARRAYREAD};
                case 27: case 38: return new Code[]{Code.PLUS};
                case 28: return new Code[]{Code.MUL};
                case 29: return new Code[]{Code.MINUS};
                case 30: return new Code[]{Code.DIV};
                case 35: return new Code[]{Code.MOD}; // REMAINDER
                case 37: return new Code[]{Code.LENGTHOF};
                case 40: return new Code[]{Code.OR};
                case 42: return new Code[]{Code.AND};
                case 43: return new Code[]{Code.SHIFTL};
                case 58: return new Code[]{Code.SHIFTR};
                case 44: return new Code[]{Code.THROW};
                case 45: return new Code[]{Code.NEG};
                case 46: case 54: return new Code[]{Code.SWITCH};
                case 48: return new Code[]{Code.BITXOR};
                case 50: return new Code[]{Code.MONITOR};

                //case 7: return throwNode((ThrowStmt) unit);
                //case 8: return switchNode((SwitchStmt) unit);
                case 6: case 7: case 16: case 18: case 20: case 24: case 25: case 31: case 32: case 34: case 36: case 39: case 41: case 52:
                    return ifNode(unit,config);
                //case 10:return monitorNode((MonitorStmt) unit);
                //default: Assertions.UNREACHABLE();
            }
            return codes;
        }catch (Exception ex) {return null;}
    }

    private static Code[] invokeNode(MethodArgInst iStmnt,FileBasedConfiguration config,List<String> functionNames)
    {
        List<Code> codes = new ArrayList<>();
        int k = statementMap.get(iStmnt.getClass());
        SootMethod sm = iStmnt.getMethod();
        int paramCount=sm.getParameterCount();

        if (!config.getBoolean("staticCall") && iStmnt.getClass() == BStaticInvokeInst.class)
            return null;
        if (!config.getBoolean("specialInvoke") && iStmnt.toString().contains("<init>"))
            return null;

        if (!config.getBoolean("InitAppendToString")) {
            if (iStmnt.toString().contains("<init>") || iStmnt.toString().contains("java.lang.StringBuilder append(") ||
                    iStmnt.toString().contains("java.lang.StringBuilder: java.lang.String toString("))
                return null;
        }
        if (Environment.SUPPORTCALLNAMES)
        {
            String name=iStmnt.getMethod().getName();
            if (Environment.STUBBERPROCESSING && name.contains("_"))
            {
                name=name.substring(0,name.lastIndexOf("_"));
            }
            functionNames.add(name);
        }
        codes.add(Code.SPECIALCALL);
        if (paramCount==0) codes.add(Code.NULL);
        else if (paramCount==1) codes.add(Code.ONE);
        else if (paramCount==2) codes.add(Code.TWO);
        else if (paramCount==3) codes.add(Code.THREE);
        else if (paramCount==4) codes.add(Code.FOUR);
        else if (paramCount==5) codes.add(Code.FIVE);
        else if (paramCount==6) codes.add(Code.SIX);
        else if (paramCount==7) codes.add(Code.SEVEN);
        else if (paramCount==8) codes.add(Code.EIGHT);
        else if (paramCount==9) codes.add(Code.NINE);
        else if (paramCount==10) codes.add(Code.TEN);
        else codes.add(Code.INT);
        return codes.toArray(new Code[0]);
        /*if (iStmnt.getInvokeExpr() instanceof JSpecialInvokeExpr)
        {
            codes.add(Code.SPECIALCALL);
            codes.addAll(specialInvoke((JSpecialInvokeExpr) iStmnt.getInvokeExpr()));
            if (codes.size()==1)
                    return null;
        }
        else if (iStmnt.getInvokeExpr() instanceof JVirtualInvokeExpr)
        {
            codes.add(Code.SPECIALCALL);
            codes.addAll(virtualInvoke((JVirtualInvokeExpr) iStmnt.getInvokeExpr()));
            if (codes.size()==1)
                return null;
        }
        else if (iStmnt.getInvokeExpr() instanceof JStaticInvokeExpr)
        {
            codes.add(Code.SPECIALCALL);
            codes.addAll(staticInvoke((JStaticInvokeExpr) iStmnt.getInvokeExpr()));
            if (codes.size()==1)
                return null;
        }
        else if (iStmnt.getInvokeExpr() instanceof JInterfaceInvokeExpr) {
            codes.add(Code.SPECIALCALL);
            codes.addAll(interfaceInvoke((JInterfaceInvokeExpr) iStmnt.getInvokeExpr()));
            if (codes.size()==1)
                return null;
        }*/
    }

    private static Code[] ifNode(Unit iStmnt, FileBasedConfiguration config)
    {
        List<Code> codes = new ArrayList<>();
        codes.add(Code.COND);

        if (config.getBoolean("ifNodeCompareOperator")){
            codes.add(Code.COMP);
            return codes.toArray(new Code[0]);
        }

        int k=statementMap.get(iStmnt.getClass());
        if (k==6 || k==16 || k==24)  codes.add(Code.EQ);
        else if (k==36 || k==39) codes.add(Code.LE);
        else if (k==25 || k==31) codes.add(Code.GE);
        else if (k==32 || k==41) codes.add(Code.GT);
        else if (k==34 || k==52) codes.add(Code.LT);
        else if (k==7 || k==18 || k==20) codes.add(Code.NE);
        return codes.toArray(new Code[0]);
    }
}
