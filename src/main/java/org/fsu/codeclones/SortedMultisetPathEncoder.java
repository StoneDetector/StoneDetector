package org.fsu.codeclones;

import com.ibm.wala.util.debug.Assertions;
import fr.inria.controlflow.BranchKind;
import fr.inria.controlflow.ControlFlowNode;
import org.apache.commons.io.output.FileWriterWithEncoding;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtElement;
import spoon.support.reflect.code.CtBinaryOperatorImpl;

import java.io.BufferedWriter;
import java.util.*;

import static java.lang.Integer.max;
import static java.lang.Integer.min;


import java.io.FileWriter;   // Import the FileWriter class
import java.io.File;  // Import the File class
import java.io.IOException;  // Import the IOException class to handle errors
import java.util.stream.Collectors;


public class SortedMultisetPathEncoder extends Encoder<ControlFlowNode> implements Comparable<SortedMultisetPathEncoder> {
    public List<SortedMultisetPathEncoder> content;

    List<Code> opKind = new ArrayList<Code>();
    int[] encoding;
    int number = 0; // Number of code elements
    int hashNumber; // Todo: That should been an IntList
    Integer hashValue;
    float ARGUMENTWEIGHT=1.0F-Environment.THRESHOLD;
    boolean PERFECTMATCH=true;
    boolean DEEPSEARCH = true;
    //--------------------------------------------------------------------------------------------------------------------------------------------------
    public final Code getKind() {

        return opKind.get(0);
    }

    public SortedMultisetPathEncoder() {
        content = new ArrayList<>();
    }


    // contructor used in StringEncoding
    public SortedMultisetPathEncoder(List<Code> opKind) {
        //this.kind = kind;
        this.opKind = opKind;
        number = opKind.size();
        setEncodingArray(opKind.size());
    }

    public SortedMultisetPathEncoder(ControlFlowNode n) {
        this.encodeNode(n);
    }

    private void encodeNode(ControlFlowNode n) {
        //System.out.println(n);
        BranchKind branchKind = n.getKind();
        CtElement e = n.getStatement();
        if (branchKind == BranchKind.BRANCH) {
            //kind = Code.COND;
            opKind.add(Code.COND);

            if (e == null || e.getClass() == null) {
                System.out.println(e);
                return;
            }
            if (ht.get(e.getClass().toString()) == 33) { // local var
                CtLocalVariable lvar = (CtLocalVariable) e;
                if (lvar.getDefaultExpression() != null) {
                    opKind.add(Code.ASSIGN);
                    opKind.add(Code.VAR);
                    getOperators(lvar.getDefaultExpression(), opKind);
                } else
                    opKind.add(Code.VAR);
            } else
                getOperators((CtExpression) e, opKind);
            number += opKind.size();
            //System.out.println(opKind);
            setEncodingArray(opKind.size());
            return;
        }

        if (branchKind == BranchKind.TRY) {
            opKind.add(Code.TRY);
            setEncodingArray(opKind.size());
            number += opKind.size();
            return;
        }

        if (branchKind == BranchKind.FINALLY) {
            opKind.add(Code.FINALLY);
            setEncodingArray(opKind.size());
            number += opKind.size();
            return;
        }

        if (e == null || e.getClass() == null) {
            System.out.println(e);
            return;
        }

        if (ht == null || ht.get(e.getClass().toString()) == null) {
            System.out.println("++++++++++++++++++++++++++++++++++++++++++" + ht.get(e.getClass().toString()));


            System.out.println("++++++++++++++++++++++++++++++++++++++++++" + e);
            System.out.println("++++++++++++++++++++++++++++++++++++++++++" + e.getClass());

            return;
        }

        //System.out.println("No. " + e.getClass().toString() + " "+ e);
        switch (ht.get(e.getClass().toString())) {

            case 4: // assert statement
                opKind.add(Code.ASSERT);
                CtAssert ass = (CtAssert) e;
                getOperators(ass.getAssertExpression(), opKind);
                getOperators(ass.getExpression(), opKind);
                break;

            case 5: // assignment
                opKind.add(Code.ASSIGN);
                CtAssignment assign = ((CtAssignment) e);
                getOperators(assign.getAssigned(), opKind);
                getOperators(assign.getAssignment(), opKind);
                break;

            case 6: // BinaryOperator
                CtBinaryOperatorImpl binOp = ((CtBinaryOperatorImpl) e);
                opKind.add(Code.BINARY); //brauchen wir das? WA
                getOperators(binOp, opKind);
                break;

            case 8: // break statement
                opKind.add(Code.BREAK);
                break;

            case 11: // catch statement
                opKind.add(Code.CATCH);
                break;

            case 17: // construktor call
                opKind.add(Code.NEW);
                CtConstructorCall cons = (CtConstructorCall) e; //Todo: Hashcode WA
                opKind.add(numberToCode(cons.getArguments().size()));
                for (CtExpression m : (List<CtExpression>) (cons.getArguments()))
                    getOperators(m, opKind);
                break;

            case 18: // continue statement
                opKind.add(Code.CONTINUE);
                break;

            case 23: //field read access
                getOperators((CtExpression) e, opKind);
                break;

            case 28: // method invocation
                CtInvocation inv = ((CtInvocation) e);
                getOperators(inv, opKind);
                break;

            case 32: //literal
                CtLiteral lit = ((CtLiteral) e);
                opKind.add(Code.VAR); // Redo: WA
                //getOperators(lit,opKind);
                break;

            case 33: // local variable definition
                CtLocalVariable lvar = (CtLocalVariable) e;
                if (lvar.getDefaultExpression() != null) {
                    opKind.add(Code.ASSIGN);
                    //opKind.add(Code.VAR);
                    getOperators(lvar.getDefaultExpression(), opKind);
                    ;
                } else
                    opKind.add(Code.VAR);
                break;

            case 37: //operator assign statement
                opKind.add(Code.ASSIGN);
                assign = ((CtAssignment) e);
                getOperators(assign.getAssigned(), opKind);
                opKind.add(binaryOperatorToCode(((CtOperatorAssignment) assign).getKind()));
                getOperators(assign.getAssigned(), opKind);
                getOperators(assign.getAssignment(), opKind);
                break;

            case 38: //return statement
                opKind.add(Code.RETURN);
                CtReturn ctReturn = (CtReturn) e;
                if (ctReturn.getReturnedExpression() != null)
                    getOperators(ctReturn.getReturnedExpression(), opKind);
                    //opKind.add(Code.VAR); //Todo:
                else
                    opKind.add(Code.VOID);
                break;

            case 47: // throw statement
                opKind.add(Code.THROW);
                break;

            case 51: // UnaryOperator
                CtUnaryOperator unOp = ((CtUnaryOperator) e);
                opKind.add(Code.UNARY);
                getOperators(unOp, opKind);
                break;

            case 53: // variable read
                CtVariableRead varRead = ((CtVariableRead) e);
                opKind.add(Code.EXPR); //Todo: Check thisn WA
                getOperators(varRead, opKind);
                break;
            case 57: // class definition ToDo
                opKind.add(Code.CLASSDEFINITION);
                break;

            default:
                opKind.add(Code.EXPR);
                //System.out.println(e);
                getOperators((CtExpression) e, opKind);
        }

        //System.out.println(opKind);
        setEncodingArray(opKind.size());
        number += opKind.size();

    }

    private void getOperators(CtExpression value, List<Code> list) {
        if (value == null)
            return;

        if (value instanceof CtAssignment) {
            list.add(Code.ASSIGN);
            CtAssignment assign = ((CtAssignment) value);
            getOperators(assign.getAssigned(), list);
            getOperators(assign.getAssignment(), list);
            return;
        }

        if (value instanceof CtConditional) {
            CtConditional cond = ((CtConditional) value);
            list.add(Code.COND);
            getOperators(cond.getCondition(), list);
            getOperators(cond.getThenExpression(), list);
            getOperators(cond.getElseExpression(), list);
            return;
        }

        if (value instanceof CtNewArray) {

            list.add(Code.NEWARRAY);
            // Redo this? WA
	    /*list.add(Code.HASHVALUE);
	    String clazz = ((CtNewArray)value).getType() + "" + ((CtNewArray)value).getElements();
	    //System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! " + clazz);

	    Integer number = DominatorTree.methodTable.get(clazz);
	    if (number == null ){
	    	number = DominatorTree.hashCounter++;
	    	DominatorTree.methodTable.put(clazz, number);
	    }

	    hashNumber = number.intValue();
	    //System.out.println(hashNumber);*/
            return;
        } else if (value instanceof CtLiteral) {
            list.add(Code.VAR);
            return;
	    /*if(((CtLiteral)value).getValue() instanceof String){
		list.add(Code.STRING);
		return;
	    }
	    if(((CtLiteral)value).getValue() instanceof Character){

		list.add(Code.CHAR);
		return;
	    }
	    if(((CtLiteral)value).getValue() instanceof Integer){
		list.add(Code.INT);
		return;
	    }
	    if(((CtLiteral)value).getValue() instanceof Long){
		list.add(Code.INT);
		return;
	    }
	    if(((CtLiteral)value).getValue() instanceof Float){
		list.add(Code.FLOAT);
		return;
	    }
	    if(((CtLiteral)value).getValue() instanceof Double){
		list.add(Code.FLOAT);
		return;
	    }
	    if(((CtLiteral)value).getValue() instanceof Boolean){
		list.add(Code.BOOLEAN);
		return;
	    }
	    else if(((CtLiteral)value).getValue() == null){
		list.add(Code.NULL);
		return;
		}*/
        } else if (value instanceof CtFieldRead) {
            list.add(Code.FIELDREAD);
            // Redo this? WA
	    /*list.add(Code.HASHVALUE);
	    String target = ((CtFieldAccess)value).getVariable().toString();
	    //System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! " + target);

	    Integer number = DominatorTree.methodTable.get(target);
	    if (number == null ){
	    	number = DominatorTree.hashCounter++;
	    	DominatorTree.methodTable.put(target, number);
	    }

	    hashNumber = number.intValue();
	    //System.out.println(hashNumber);*/
            return;
        } else if (value instanceof CtFieldWrite) {
            list.add(Code.FIELDWRITE);
            // Redo this? WA
	    /*list.add(Code.HASHVALUE);
	    String target = ((CtFieldAccess)value).getVariable().toString();
	    //System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! " + target);

	    Integer number = DominatorTree.methodTable.get(target);
	    if (number == null ){
	    	number = DominatorTree.hashCounter++;
	    	DominatorTree.methodTable.put(target, number);
	    }

	    hashNumber = number.intValue();
	    //System.out.println(hashNumber);
	    //System.out.println(value);*/

            return;
        } else if (value instanceof CtVariableRead || value instanceof CtVariableWrite) {
            list.add(Code.VAR);
            return;
        } else if (value instanceof CtInvocation) {
            CtInvocation inv = (CtInvocation) value;
            list.add(Code.CALL);
            if (Environment.SUPPORTCALLNAMES) {
                list.add(Code.HASHVALUE);
                String method = inv.getExecutable().toString().split("\\(")[0];
                //System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! " + method);

                Integer number = DominatorTree.methodTable.get(method);
                if (number == null) {
                    number = DominatorTree.hashCounter++;
                    DominatorTree.methodTable.put(method, number);
                }

                hashNumber = number.intValue();
            }
            list.add(numberToCode(inv.getArguments().size()));
            for (CtExpression m : (List<CtExpression>) inv.getArguments())
                getOperators(m, list);
            return;
        } else if (value instanceof CtConstructorCall) {
            list.add(Code.NEW);
            list.add(numberToCode(((CtConstructorCall) value).getArguments().size()));

            return;

        } else if (value instanceof CtArrayRead) {
            list.add(Code.ARRAYREAD); //Redo this. WA
            //getOperators(((CtArrayRead)value).getIndexExpression(), list);
            return;
        } else if (value instanceof CtArrayWrite) {
            list.add(Code.ARRAYWRITE);
            //getOperators(((CtArrayWrite)value).getIndexExpression(), list);
            return;
        } else if (value instanceof CtThisAccess) {

            //list.add(Code.THIS);
            return;
        } else if (value instanceof CtTypeAccess) {
            list.add(Code.TYPE);
            return;
        } else if (value instanceof CtBinaryOperator) {
            list.add(binaryOperatorToCode(((CtBinaryOperator) value).getKind()));
            getOperators(((CtBinaryOperator) value).getLeftHandOperand(), list);
            getOperators(((CtBinaryOperator) value).getRightHandOperand(), list);
            return;
        } else if (value instanceof CtUnaryOperator) {
            list.add(unaryOperatorToCode(((CtUnaryOperator) value).getKind()));
            getOperators(((CtUnaryOperator) value).getOperand(), list);
            return;
        }

        Assertions.UNREACHABLE("Cannot find operator");
        //System.out.println("Not knowm");

    }

    private void setEncodingArray(int length) {
        encoding = new int[length];
        for (int i = 0; i < encoding.length; i++) {
            if (opKind.get(i) == Code.HASHVALUE)
                encoding[i] = hashNumber;
            else
                encoding[i] = opKind.get(i).ordinal();
        }
        hashValue= Arrays.toString(encoding).hashCode();
    }


    private final Code binaryOperatorToCode(BinaryOperatorKind bkind) {
        final int OFFSET = Code.OR.ordinal();
        return Code.values()[OFFSET + bkind.ordinal()];
    }

    private final Code unaryOperatorToCode(UnaryOperatorKind ukind) {
        final int OFFSET = Code.POS.ordinal();
        return Code.values()[OFFSET + ukind.ordinal()];
    }


    private final Code numberToCode(int n) {
        if (n > 10)
            n = 10;
        final int OFFSET = Code.VOID.ordinal();
        return Code.values()[OFFSET + n];
    }


    //--------------------------------------------------------------------------------------------------------------------------------------------------


    @Override
    public List<List<Encoder>> encodeDescriptionSet(List<List<ControlFlowNode>> it) {

        List<List<Encoder>> s = new ArrayList<List<Encoder>>();

        int i = 0;


        for (List<ControlFlowNode> m : it) {
            int number = 0;
            int cond = 0;
            if (m.size() > Environment.MINNODESNO) { //Modified-Unsorted Splitting 1 Modified-Unsorted Unsplitting 3, LCS Splitting > 1 LCS Unsplitting > 3 Levenshtein Spliiting > 1, Levenshtein Unsplitting > 2
                List<Encoder> l = new ArrayList<Encoder>();
                SortedMultisetPathEncoder tmp_list = new SortedMultisetPathEncoder();

                for (ControlFlowNode k : m) {

                    if (k.getStatement() != null || k.getKind() == BranchKind.TRY || k.getKind() == BranchKind.FINALLY) {
                        SortedMultisetPathEncoder tmp = new SortedMultisetPathEncoder(k);
                        tmp_list.add(tmp);

                        if (k.getKind() == BranchKind.TRY || k.getKind() == BranchKind.FINALLY || k.getKind() == BranchKind.CATCH ||
                                k.getKind() == BranchKind.BRANCH)
                            cond++;
                        number += tmp.number;
                    }


                }

                l.add(tmp_list);


                if (tmp_list.size() > 0) {

                    //((CompletePathEncoder)l.get(0)).number = number; // Store number of code elements in the first encoding node
                    //((CompletePathEncoder)l.get(0)).numberCond = cond;
                    s.add(l);
                }

            }
        }

        return s;
    }

    @Override
    public boolean isPathInDescriptionSet(List<Encoder> path, List<List<Encoder>> set, MetricKind metric, boolean relativ, float threshold) {
        if (Environment.TECHNIQUE == EncoderKind.MULTISET) {
            SortedMultisetPathEncoder path_multi = (SortedMultisetPathEncoder) path.get(0);
            List<SortedMultisetPathEncoder> set_multi = new ArrayList<>();
            for (List<Encoder> l : set) {
                set_multi.add((SortedMultisetPathEncoder) l.get(0));
            }

            for (SortedMultisetPathEncoder e : set_multi) {
                if (compare(path_multi, e) == -1) {
                    return true;
                }
            }
            return false;

        } else {
            return false;
        }
    }

    boolean isPathInDescriptionSet_alt(List<Encoder> path, List<List<Encoder>> set, MetricKind metric, boolean relativ, float threshold) {
        if (Environment.TECHNIQUE == EncoderKind.MULTISET) {
            SortedMultisetPathEncoder path_multi = (SortedMultisetPathEncoder) path.get(0);
            List<SortedMultisetPathEncoder> set_multi = new ArrayList<>();
            for (List<Encoder> l : set) {
                set_multi.add((SortedMultisetPathEncoder) l.get(0));
            }

            for (SortedMultisetPathEncoder e : set_multi) {
                if (similarity(path_multi, e) >= threshold) {
                    return true;
                }
            }
            return false;

        } else {
            return false;
        }
    }

    @Override
    public boolean areTwoDescriptionSetsSimilar(List<List<Encoder>> set1, List<List<Encoder>> set2, MetricKind metric, boolean sorted, boolean relativ, float threshold) {
        //------------------------------------------------------------------------------------------------------   Checks to skip Comparison if not neccessary
        if(set1.size()==0 || set2.size()==0){
            return false;
        }

        if(set1.size() > set2.size()){ // it will check if the smaller set is equivalent to the larger set
            List<List<Encoder>> tmp = set1;
            set1 = set2;
            set2 = tmp;
        }

        if(set2.size() > Environment.WIDTHLOWERNO && set1.size() *  Environment.WIDTHUPPERFAKTOR < set2.size()){
            return false;
        }
       //------------------------------------------------------------------------------------------------------

        if (Environment.TECHNIQUE == EncoderKind.MULTISET) {
            List<SortedMultisetPathEncoder> set_multi1 = new ArrayList<>();
            for (List<Encoder> l : set1) {
                set_multi1.add((SortedMultisetPathEncoder) l.get(0));
            }
            List<SortedMultisetPathEncoder> set_multi2 = new ArrayList<>();
            for (List<Encoder> l : set2) {
                set_multi2.add((SortedMultisetPathEncoder) l.get(0));
            }



            float total_similarity = 1;
            for (SortedMultisetPathEncoder s1 : set_multi1) {
                float max_similarity = 0;
                for (SortedMultisetPathEncoder s2 : set_multi2) {

                    if(Math.abs(s1.size()-s2.size())>Environment.MAXDIFFNODESNO){
                        continue;
                    }

                    float sim = similarity(s1, s2);
                    if (sim > max_similarity) {
                        max_similarity = sim;
                    }
                }
                total_similarity -= ((1-max_similarity)/set_multi1.size());
                if (total_similarity < 1 - threshold) {
                    return false;
                }
            }
            //total_similarity /= set_multi1.size();

            if (total_similarity >= 1 - threshold) {
                return true;
            }
            return false;

        } else {
            return false;
        }
    }


    @Override
    public int[] getEncoding() {
        return encoding;
    }

    @Override
    public int getNumberOfEncodings() {
        return number;
    }


    //-------------------------------------------------------------------------------------


    public void add(SortedMultisetPathEncoder element) {
        content.add(element);
        Collections.sort(content);
    }


    public void add(SortedMultisetPathEncoder element, int count) {
        for (int i = 0; i < count; i++) {
            content.add(element);
        }
        Collections.sort(content);
    }


    public void delete(SortedMultisetPathEncoder element) {
        this.delete(element, 1);
    }


    public void delete(SortedMultisetPathEncoder element, int count) {
        int deleted = 0;
        for (SortedMultisetPathEncoder i : content) {
            switch (i.compareTo(element)) {
                case 0:
                    content.remove(i);
                    deleted++;
                    if (deleted >= count) {
                        return;
                    }
                case 1:
                    return;
            }
        }
    }


    public int count(SortedMultisetPathEncoder element) {
        int count = 0;
        int Hash=element.hashValue;

        for (SortedMultisetPathEncoder i : content) { ;
            int currentHash=i.hashValue;
//            if(element.compareTo(i)==0){count++;}
//            else {
//                if(element.compareTo(i)==-1){
//                    return count;
//                }
//            }

            if(Hash==currentHash){
                count++;
                continue ;
            }
            if(Hash<currentHash){
                break;
            }

//            switch (element.compareTo(i)) {
//                case 0:
//                    count++;
//                    break;
//                case -1:
//                    return count;
//            }
        }
        return count;
    }


    public int size() {
        return content.size();
    }


    public Set<SortedMultisetPathEncoder> get_basicset() {
        Set<SortedMultisetPathEncoder> result = new HashSet<>();
        result.add(content.get(0));
        for (int i = 1; i < content.size(); i++) {
            if (content.get(i).compareTo(content.get(i - 1)) != 0) {
                result.add(content.get(i));
            }
        }
        return result;
    }


    public int compare(SortedMultisetPathEncoder a, SortedMultisetPathEncoder b) {
        boolean check = true; // Variable zur Kontrolle ob die Bedingung für eine der Möglichen Ausgangsoptionen der compare Funktion verletzt wurde
        if (a.size() == b.size()) {

            for (SortedMultisetPathEncoder i : a.get_basicset()) {
                if (a.count(i) != b.count(i)) {

                    check = false;
                    break;
                }
            }
            if (check) {
                return 0;
            }
        } else {
            if (a.size() < b.size()) {
                for (SortedMultisetPathEncoder i : a.get_basicset()) {
                    if (a.count(i) > b.count(i)) {
                        check = false;
                        break;
                    }
                }
                if (check) {
                    return -1;
                }
            } else {
                for (SortedMultisetPathEncoder i : b.get_basicset()) {
                    if (b.count(i) > a.count(i)) {
                        check = false;
                        break;
                    }
                }
                if (check) {
                    return 1;
                }
            }
        }
        return 2;
        //2: ungleich 0: gleich 1:b ist in a enthalten -1: b enthält a
    }

    public float similarity(SortedMultisetPathEncoder a, SortedMultisetPathEncoder b) {
        if (b.size() == 0 || a.size() == 0) {
            return 0;
        }
        if (DEEPSEARCH == false || PERFECTMATCH) {

            float unionElementCount = 0;
            for (SortedMultisetPathEncoder i : a.get_basicset()) {
                unionElementCount += min(a.count(i), b.count(i));
            }

            return unionElementCount / (max(b.size(), a.size()));
        } else {

            float[][] SimilarityMatrix = new float[b.size()][a.size()];


            for (int x = 0; x < a.size(); x++) {
                for (int y = 0; y < b.size(); y++) {


                    List<Code> OpKindA = a.content.get(x).opKind;
                    List<Code> OpKindB = b.content.get(y).opKind;
                    float similarity = 0f;


                    if (OpKindA.get(0) == OpKindB.get(0)) {

                        similarity += 1 - ARGUMENTWEIGHT;
                        if (OpKindA.size() == 1 && OpKindB.size() == 1) {
                            similarity += ARGUMENTWEIGHT;

                        } else {

                            if (OpKindA.size() != 1 && OpKindB.size() != 1) {
                                int common = 0;


                                List<Integer> indizes = new ArrayList<>();
                                for (int index = 1; index < OpKindB.size(); index++) {
                                    indizes.add(index);
                                }

                                //---------------------------------------------------------------------------------------langsamer Part

                                for (Code code : OpKindA.subList(1, OpKindA.size())) {
                                    for (int index : indizes) {
                                        if (code == OpKindB.get(index)) {
                                            common++;
                                            indizes.remove(indizes.indexOf(index));
                                            break;
                                        }
                                    }

                                }

                                //------------------------------------------------------------------------------------
                                similarity += ARGUMENTWEIGHT * common / (max(OpKindA.size() - 1, OpKindB.size() - 1));
                            }

                        }
                    }


                    SimilarityMatrix[y][x] = similarity;
                }

            }


            return findBestSum(SimilarityMatrix) / max(b.size(), a.size());

        }

    }

    private float findBestSum(float[][] M) {

//        List<Integer> columns=new ArrayList<>();
//        for (int x=0;x<M[0].length;x++){
//            columns.add(x);
//        }
//        float erg= findBestSumBacktracker(M,0,columns,0);


        float result = 0;
        if (M[0].length > M.length) {
            List<Integer> columns = new ArrayList<>();
            for (int i = 0; i < M[0].length; i++) {
                columns.add(i);
            }
//------------------------------------------------------------------------------------------------ Sorting Matrix according to highest Value in row


            Integer[] rows = new Integer[M.length];
            Float[] rowsMax = new Float[M.length];

            for (int i = 0; i < M.length; i++) {
                float max = 0;
                for (int j = 0; j < M[0].length; j++) {
                    if (max < M[i][j]) {
                        max = M[i][j];
                    }
                }
                rows[i] = i;
                rowsMax[i] = max;
            }

            for (int i = rowsMax.length - 2; i >= 0; i--) { //Insertion Sort
                for (int j = i; j < rowsMax.length - 1 && rowsMax[j + 1] > rowsMax[j]; j++) {
                    swap(rowsMax, j, j + 1);
                    swap(rows, j, j + 1);
                }
            }


//------------------------------------------------------------------------------------------------
            for (int i : rows) {
                float maxvalue = -1;
                int index = -1;
                for (int col : columns) {
                    if (M[i][col] > maxvalue) {
                        maxvalue = M[i][col];
                        index = col;
                    }
                }
                columns.remove(columns.indexOf(index));
                result += maxvalue;
            }
        } else {
            List<Integer> rows = new ArrayList<>();
            for (int i = 0; i < M.length; i++) {
                rows.add(i);
            }
            //------------------------------------------------------------------------------------------------ Sorting Matrix according to highest Value in column

            Integer[] columns = new Integer[M[0].length];
            Float[] columnsMax = new Float[M[0].length];

            for (int i = 0; i < M[0].length; i++) {
                float max = 0;
                for (int j = 0; j < M.length; j++) {
                    if (max < M[j][i]) {
                        max = M[j][i];
                    }
                }
                columns[i] = i;
                columnsMax[i] = max;
            }

            for (int i = columnsMax.length - 2; i >= 0; i--) { //Insertion Sort
                for (int j = i; j < columnsMax.length - 1 && columnsMax[j + 1] > columnsMax[j]; j++) {
                    swap(columnsMax, j, j + 1);
                    swap(columns, j, j + 1);
                }
            }

//------------------------------------------------------------------------------------------------
            for (int i : columns) {
                float maxvalue = -1;
                int index = -1;
                for (int row : rows) {
                    if (M[row][i] > maxvalue) {
                        maxvalue = M[row][i];
                        index = row;
                    }
                }
                rows.remove(rows.indexOf(index));
                result += maxvalue;
            }
        }


        return result;

    }


    private static final <T> void swap(T[] a, int i, int j) {
        T t = a[i];
        a[i] = a[j];
        a[j] = t;
    }


    private float findBestSumBacktracker(float[][] M, float sum, List<Integer> columns, int row) {
        if (row == M.length) {
            return sum;
        }
        float maxSum = 0;
        for (Integer column : columns) {
            List<Integer> columnsClone = new ArrayList<>(columns);
            columnsClone.remove(column);
            float sumTemp = findBestSumBacktracker(M, sum + M[row][column], columnsClone, row + 1);
            if (sumTemp > maxSum) {
                maxSum = sumTemp;
            }
        }
        List<Integer> columnsClone = new ArrayList<>(columns);
        float sumTemp = findBestSumBacktracker(M, sum, columnsClone, row + 1);
        if (sumTemp > maxSum) {
            maxSum = sumTemp;
        }
        return maxSum;
    }


    @Override
    public String toString() {
        if(content!=null){
            return content.toString();
        }
        return opKind.toString();
    }


    public SortedMultisetPathEncoder getFirstElement() {
        return content.get(0);
    }

    public SortedMultisetPathEncoder getElement(int i){
        if (content!=null){
            return content.get(i);
        }
        return null;
    }

    @Override
    public int compareTo(SortedMultisetPathEncoder encoder) {
        if(hashValue!=null){
            return this.hashValue.compareTo(encoder.hashValue);
        }
        return 0;

//        if (this.getKind().ordinal() < encoder.getKind().ordinal()) {
//            return -1;
//        }
//        if (this.getKind().ordinal() > encoder.getKind().ordinal()) {
//            return 1;
//        }
//        if (Environment.DEEPSEARCH == false) {
//            return 0;
//        }
//        if (this.opKind.size() > encoder.opKind.size()) {
//            return 1;
//        }
//
//        if (this.opKind.size() < encoder.opKind.size()) {
//            return -1;
//        }
//        for (int i = 1; i < this.opKind.size(); i++) {
//            if (this.opKind.get(i).ordinal() > encoder.opKind.get(i).ordinal()) {
//                return 1;
//            }
//            if (this.opKind.get(i).ordinal() < encoder.opKind.get(i).ordinal()) {
//                return -1;
//            }
//        }
//        return 0;
    }


}
